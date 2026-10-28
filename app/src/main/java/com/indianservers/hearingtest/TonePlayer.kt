package com.indianservers.hearingtest

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

/** Original MP3 bytes go directly to the platform decoder. No pitch, rate or gain boost. */
internal class TonePlayer(
    private val context: Context,
    private val createPlayer: () -> MediaPlayer = { MediaPlayer() }
) {
    private val handler = Handler(Looper.getMainLooper())
    private var active: Session? = null
    private var generation = 0
    private var requestedVolume = .08f
    private var requestedEar = 1

    private class Session(val player: MediaPlayer, val token: Int, val done: (String?) -> Unit) {
        var left = 0f
        var right = 0f
        var ready = false
        var released = false
        var ending = false
        var ramp = 0
    }

    private fun release(session: Session) {
        if (session.released) return
        session.released = true
        handler.removeCallbacksAndMessages(session)
        session.player.setOnPreparedListener(null)
        session.player.setOnCompletionListener(null)
        session.player.setOnErrorListener(null)
        session.player.release()
        if (active === session) active = null
    }

    private fun fail(session: Session, cause: Exception? = null) {
        if (session.released) return
        Log.e("TonePlayer", "Tone playback failed", cause)
        release(session)
        if (session.token == generation) session.done("Unable to play this tone. No response was recorded.")
    }

    // Decoder failures can also occur after prepareAsync returns, in a posted callback.
    private fun safely(session: Session, action: () -> Unit) {
        if (session.released) return
        try { action() } catch (failure: Exception) { fail(session, failure) }
    }

    private fun later(session: Session, delay: Long, action: () -> Unit) {
        handler.postAtTime({ safely(session, action) }, session, SystemClock.uptimeMillis() + delay)
    }

    // Raised-cosine envelope avoids abrupt amplitude changes, including volume adjustments.
    private fun ramp(session: Session, left: Float, right: Float, complete: () -> Unit = {}) {
        val serial = ++session.ramp
        val fromLeft = session.left
        val fromRight = session.right
        val start = SystemClock.uptimeMillis()
        fun step() {
            if (session.released || serial != session.ramp) return
            val fraction = ((SystemClock.uptimeMillis() - start) / 45f).coerceIn(0f, 1f)
            val blend = ((1 - cos(Math.PI * fraction)) / 2).toFloat()
            session.left = fromLeft + (left - fromLeft) * blend
            session.right = fromRight + (right - fromRight) * blend
            safely(session) {
                session.player.setVolume(session.left, session.right)
                if (fraction < 1f) later(session, 5) { step() } else complete()
            }
        }
        step()
    }

    fun stop() {
        generation++
        active?.let { session ->
            active = null
            session.ending = true
            if (session.ready) ramp(session, 0f, 0f) { release(session) } else release(session)
        }
    }

    fun dispose() { stop() } // A pending fade owns and releases its player within 45 ms.

    fun volume(value: Float, ear: Int) {
        requestedVolume = value.coerceIn(0f, .5f)
        requestedEar = ear
        active?.takeIf { it.ready && !it.ending }?.let {
            ramp(it, if (ear == 2) 0f else requestedVolume, if (ear == 0) 0f else requestedVolume)
        }
    }

    fun play(hz: Int, value: Float, ear: Int, onReady: () -> Unit, done: (String?) -> Unit) {
        stop()
        val token = generation
        requestedVolume = value.coerceIn(0f, .5f)
        requestedEar = ear
        // Finish the previous fade before starting another tone.
        handler.postDelayed({
            if (token != generation) return@postDelayed
            val mediaPlayer = try { createPlayer() } catch (failure: Exception) {
                Log.e("TonePlayer", "Unable to create audio player", failure)
                done("Unable to play this tone. No response was recorded.")
                return@postDelayed
            }
            val session = Session(mediaPlayer, token, done)
            val next = session.player
            active = session
            try {
                next.setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                next.setVolume(0f, 0f)
                next.isLooping = false
                if (hz >= 8000) {
                    context.assets.openFd("tones/$hz.mp3").use {
                        next.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                    }
                } else next.setDataSource(lowTone(hz).absolutePath)
                next.setOnErrorListener { _, what, extra ->
                    fail(session, IllegalStateException("MediaPlayer error $what/$extra")); true
                }
                next.setOnCompletionListener {
                    if (session.released) return@setOnCompletionListener
                    release(session)
                    if (token == generation) done(null)
                }
                next.setOnPreparedListener {
                    if (session.released) return@setOnPreparedListener
                    if (token != generation) { release(session); return@setOnPreparedListener }
                    safely(session) {
                        session.ready = true
                        // Default playback speed and pitch remain 1.0. No PlaybackParams/effects.
                        next.start()
                        volume(requestedVolume, requestedEar)
                        later(session, 500) {
                            if (!session.released && token == generation && !session.ending) onReady()
                        }
                        later(session, (next.duration - 65).coerceAtLeast(500).toLong()) {
                            if (!session.released && token == generation) {
                                session.ending = true
                                ramp(session, 0f, 0f)
                            }
                        }
                    }
                }
                next.prepareAsync()
            } catch (failure: Exception) { fail(session, failure) }
        }, 50)
    }

    private fun lowTone(hz: Int): File {
        val file = File(context.cacheDir, "tone_v2_$hz.wav")
        if (!file.exists()) {
            val rate = 48000
            val count = rate * 3
            val data = ByteBuffer.allocate(44 + count * 2).order(ByteOrder.LITTLE_ENDIAN)
            data.put("RIFF".toByteArray()).putInt(36 + count * 2)
                .put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
                .putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
                .put("data".toByteArray()).putInt(count * 2)
            repeat(count) { i ->
                val edge = minOf(1.0, i / 2400.0, (count - 1 - i) / 2400.0)
                val envelope = (1 - cos(Math.PI * edge)) / 2
                data.putShort((sin(2 * Math.PI * hz * i / rate) * 16000 * envelope).toInt().toShort())
            }
            file.writeBytes(data.array())
        }
        return file
    }
}
