package com.indianservers.hearingtest

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class TonePlaybackTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun originalMp3sDecodeAndCompleteWithoutErrors() {
        val context = instrumentation.targetContext
        val tones = context.assets.list("tones")!!.filter { it.endsWith(".mp3") }
        assertEquals(13, tones.size)
        for (asset in tones) {
            val ready = CountDownLatch(1)
            val complete = CountDownLatch(1)
            val failure = AtomicReference<String?>(null)
            lateinit var player: TonePlayer
            instrumentation.runOnMainSync {
                player = TonePlayer(context)
                // Silent integration check: validates the decoder, callbacks and completion.
                player.play(asset.removeSuffix(".mp3").toInt(), 0f, 1,
                    onReady = { ready.countDown() }, done = { failure.set(it); complete.countDown() })
            }
            try {
                assertTrue("$asset did not reach sustained playback", ready.await(10, TimeUnit.SECONDS))
                assertTrue("$asset did not complete", complete.await(10, TimeUnit.SECONDS))
                assertNull("$asset failed", failure.get())
            } finally { instrumentation.runOnMainSync { player.dispose() } }
        }
    }

    @Test fun immediateStopCancelsPendingPlaybackAndCallbacks() {
        val callback = CountDownLatch(1)
        lateinit var player: TonePlayer
        instrumentation.runOnMainSync {
            player = TonePlayer(instrumentation.targetContext)
            player.play(8000, 0f, 0, onReady = { callback.countDown() }, done = { callback.countDown() })
            player.stop()
        }
        try { assertFalse("Cancelled playback fired a stale callback", callback.await(1, TimeUnit.SECONDS)) }
        finally { instrumentation.runOnMainSync { player.dispose() } }
    }
}
