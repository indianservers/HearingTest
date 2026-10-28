package com.indianservers.hearingtest

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

private val Blue = Color(0xFF1674FF)
private val Ink = Color(0xFF111B30)
private val Muted = Color(0xFF64748B)
private val Green = Color(0xFF139B46)
private val frequencies = listOf(250,500,1000,2000,3000,4000,6000,8000,10000,12000,14000,15000,16000,17000,17400,18000,19000,20000,21000,22000)
private val groupNames = listOf("Kids", "Teens", "Adults", "Seniors")
private val groupColors = listOf(Color(0xFFE8F3FF),Color(0xFFE9FBF1),Color(0xFFF1EBFF),Color(0xFFFFF1E2))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary=Blue, secondary=Blue, secondaryContainer=Color(0xFFE8F3FF), onSecondaryContainer=Blue, primaryContainer=Color(0xFFE8F3FF), background=Color(0xFFF8FAFD), surface=Color.White, onSurface=Ink)) { HearingApp() } }
    }
}

@Composable private fun HearingApp() {
    val context=LocalContext.current
    val prefs=remember { context.getSharedPreferences("hearing",0) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var group by rememberSaveable { mutableIntStateOf(0) }
    var hz by rememberSaveable { mutableIntStateOf(8000) }
    var ear by rememberSaveable { mutableIntStateOf(1) }
    var volume by rememberSaveable { mutableFloatStateOf(.08f) }
    var playing by remember { mutableStateOf(false) }
    var played by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var answers by remember { mutableStateOf(validResponses(prefs.all)) }
    val player=remember { TonePlayer(context) }
    fun stop() { player.stop(); playing=false }
    fun record(heard: Boolean) { val key="$ear:$hz"; val value=if(heard) volume.toString() else "no"; answers=answers+(key to value); prefs.edit().putString(key,value).apply(); stop(); played=false }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) { val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_STOP) stop() }; lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer); player.dispose() } }
    val scrollState = rememberScrollState()
    LaunchedEffect(page) { scrollState.scrollTo(0) }
    Scaffold(bottomBar={ NavigationBar(containerColor=Color.White) { listOf("Hearing Test","Results","About").forEachIndexed { index,label -> NavigationBarItem(selected=page==index,onClick={stop();page=index},icon={Text(listOf("♬","▥","ⓘ")[index],fontSize=25.sp)},label={Text(label)}) } } }) { padding ->
        Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFD)).padding(padding).verticalScroll(scrollState).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            if(page==0) {
                Row(verticalAlignment=Alignment.CenterVertically) { Text("♬",fontSize=48.sp,color=Blue,modifier=Modifier.padding(end=16.dp)); Column(Modifier.weight(1f)) { Text("Check Your",fontSize=19.sp,fontWeight=FontWeight.Bold); Text("Hearing Range",fontSize=29.sp,fontWeight=FontWeight.Bold) }; TextButton(onClick={stop();page=2}) { Text("ⓘ",fontSize=25.sp) } }
                Text("Play different tones to find the quietest sound you can hear.",color=Muted,fontSize=15.sp)
                Canvas(Modifier.fillMaxWidth().height(44.dp)) { repeat(57) { i -> val height=(sin(i*.63)*sin(i*.19)*.8+.18).toFloat()*size.height; val x=size.width*i/56; drawLine(Blue.copy(alpha=.22f),Offset(x,size.height/2-height/2),Offset(x,size.height/2+height/2),3.dp.toPx()) } }
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) { groupNames.forEachIndexed { i,name ->
                    Column(Modifier.weight(1f).background(groupColors[i],RoundedCornerShape(16.dp)).border(if(group==i) 1.5.dp else 0.dp,if(group==i) Blue else Color.Transparent,RoundedCornerShape(16.dp)).clickable { group=i }.padding(vertical=14.dp,horizontal=2.dp),horizontalAlignment=Alignment.CenterHorizontally) { Text(listOf("🧒","👦","👨","👴")[i],fontSize=29.sp); Spacer(Modifier.height(7.dp)); Text(name,fontWeight=FontWeight.Bold,fontSize=14.sp); Text(listOf("250–8k Hz","250–12k Hz","250–8k Hz","250–6k Hz")[i],fontSize=10.sp,color=Muted) }
                } }
                Panel {
                    Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("%,d Hz".format(hz),fontWeight=FontWeight.Bold,fontSize=30.sp); Text(if(hz>=8000) "High pitch · ${hz/1000f} kHz" else "${if(hz<1000) "Low" else "Mid"} pitch",color=Muted) }; Text("${groupNames[group]}\nrange",color=Blue,textAlign=TextAlign.End,fontSize=13.sp) }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) { Text("♩",color=Muted,fontSize=24.sp); Slider(value=volume,onValueChange={volume=it;if(!playing) played=false;player.volume(it,ear)},valueRange=0f..0.5f,modifier=Modifier.weight(1f)); Text("♫",color=Muted,fontSize=24.sp) }
                    Text("Volume ${(volume*200).toInt()}% · start very low",color=Muted,fontSize=12.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) { listOf("Left Ear","Both Ears","Right Ear").forEachIndexed { i,name -> val color=listOf(Color(0xFFDD2362),Blue,Green)[i]; Column(Modifier.weight(1f).background(color.copy(alpha=.07f),RoundedCornerShape(14.dp)).border(1.dp,if(ear==i) color else Color.Transparent,RoundedCornerShape(14.dp)).clickable { stop();ear=i;played=false }.padding(vertical=15.dp),horizontalAlignment=Alignment.CenterHorizontally) { Text(if(i==1) "♬" else "◖",color=color,fontSize=32.sp); Text(name,color=color,fontSize=12.sp,fontWeight=FontWeight.Bold) } } }
                    Spacer(Modifier.height(16.dp))
                    Text("Standard range · 250–8,000 Hz",fontSize=12.sp,color=Muted)
                    (frequencies.filter { it<=8000 }.chunked(4)+frequencies.filter { it>8000 }.chunked(4)).forEachIndexed { rowIndex,row -> if(rowIndex==2) Text("Extended tones · above 8,000 Hz",fontSize=12.sp,color=Muted); Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.padding(bottom=7.dp)) { row.forEach { f -> val selected=f==hz; Column(Modifier.weight(1f).background(if(selected) Blue else Color.White,RoundedCornerShape(10.dp)).border(1.dp,if(selected) Blue else Color(0xFFE5EAF2),RoundedCornerShape(10.dp)).clickable { stop();hz=f;played=false }.padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally) { Text(if(f>=1000) "${f/1000f}k" else "$f",fontSize=12.sp,color=if(selected) Color.White else Ink); Text("Hz",fontSize=10.sp,color=if(selected) Color.White else Muted) } }; repeat(4-row.size) { Spacer(Modifier.weight(1f)) } } }
                    Button(onClick={ if(playing) stop() else { playing=true;played=false;error=null;player.play(hz,volume,ear,onReady={played=true}) { failure -> playing=false; if(failure!=null) { error=failure;played=false } } } },modifier=Modifier.fillMaxWidth()) { Text(if(playing) "■  Stop tone" else "▶  Play tone") }
                    answers["$ear:$hz"]?.let { response -> Text("Recorded: ${if(response=="no") "Not heard" else "Heard"} · ${listOf("Left ear","Both ears","Right ear")[ear]}",color=if(response=="no") Muted else Green,fontSize=12.sp) }
                    error?.let { Text(it,color=MaterialTheme.colorScheme.error,fontSize=12.sp) }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedButton(enabled=played,onClick={record(false)},modifier=Modifier.weight(1f)) { Text("Not heard",fontSize=12.sp) }; Button(enabled=played,onClick={record(true)},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=Green)) { Text("Heard",fontSize=12.sp) } }
                    Text("Mark Heard only for a sustained tone, not a tap, click, hiss or buzz. Responses unlock after 0.5 seconds. Above 8,000 Hz: extended frequency exploration.",fontSize=11.sp,color=Muted)
                }
                Panel(Color(0xFFEAF3FF)) { Text("♧  A little preparation",color=Blue,fontWeight=FontWeight.Bold); Text("Use headphones in a quiet room. Start at a very low volume and stop if a tone feels uncomfortable.",fontSize=13.sp,color=Muted) }
                TextButton(onClick={stop();page=1},modifier=Modifier.fillMaxWidth()) { Text("View my results →") }
            } else if(page==1) {
                Row(verticalAlignment=Alignment.CenterVertically) { Text("Hearing Test Result",fontSize=23.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f)); TextButton(onClick={ val count=answers.count { it.value!="no" }; context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain";putExtra(Intent.EXTRA_TEXT,"My hearing tone check: $count detected responses across ${answers.size} checks. An informal, uncalibrated check; not a clinical hearing test.") },"Share results")) }) { Text("Share") } }
                val detected=answers.filterValues { it!="no" }.keys.map { it.substringAfter(":").toInt() }
                Panel(Color(0xFFE9FBF0)) { Text("Your Hearing Range",fontWeight=FontWeight.Bold,fontSize=18.sp); Text(if(detected.isEmpty()) "Let's find out" else "Up to %,d Hz".format(detected.max()),color=Green,fontSize=30.sp,fontWeight=FontWeight.Bold); Text(if(answers.isEmpty()) "Play a tone and record what you can hear to build your results." else "${detected.size} detected / ${answers.size} responses recorded",color=Muted) }
                Panel {
                    Text("What You Can Hear",fontSize=20.sp,fontWeight=FontWeight.Bold)
                    Text("Green: heard · gray: not heard · pale: untested",fontSize=11.sp,color=Muted)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) { listOf("Left","Both","Right").forEachIndexed { i,label -> FilterChip(selected=ear==i,onClick={ear=i},label={Text(label)}) } }
                    frequencies.chunked(10).forEach { row -> Row(Modifier.fillMaxWidth().height(142.dp),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.Bottom) { row.forEach { f -> val value=answers["$ear:$f"]; Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom) { Box(Modifier.fillMaxWidth().height(if(value==null) 12.dp else if(value=="no") 22.dp else (35+65*(1-value.toFloat()/.5f)).dp).background(if(value==null) Color(0xFFEAF0F7) else if(value=="no") Color(0xFFBFC8D4) else Color(0xFF4CCD76),RoundedCornerShape(topStart=3.dp,topEnd=3.dp))); Text(if(f>=1000) "${f/1000f}k" else "$f",fontSize=8.sp,color=Muted) } } } }
                    Text("Frequency (Hz) · bar height reflects selected volume",color=Muted,fontSize=10.sp)
                }
                Panel { Text("Recorded responses",fontWeight=FontWeight.Bold,fontSize=20.sp); Text("${listOf("Left ear","Both ears","Right ear")[ear]} · playback volume is not calibrated dB HL",color=Muted,fontSize=11.sp); val recorded=frequencies.filter { answers.containsKey("$ear:$it") }; if(recorded.isEmpty()) Text("No responses for this ear yet.",color=Muted); recorded.forEach { f -> val response=answers["$ear:$f"]!!; Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("%,d Hz".format(f),fontSize=13.sp); Text(if(response=="no") "Not heard" else "Heard · ${(response.toFloat()*200).toInt()}% volume",fontSize=13.sp,color=if(response=="no") Muted else Green) } } }
                Panel { Text("Hearing Range Comparison",fontWeight=FontWeight.Bold,fontSize=20.sp); Text("Reference illustration",color=Muted,fontSize=12.sp); groupNames.forEachIndexed { i,name -> Row(Modifier.padding(top=14.dp),verticalAlignment=Alignment.CenterVertically) { Text(listOf("🧒","👦","👨","👴")[i],fontSize=27.sp); Column(Modifier.width(90.dp).padding(start=8.dp)) { Text(name,fontWeight=FontWeight.Bold,fontSize=14.sp); Text(listOf("250 Hz–8 kHz","250 Hz–12 kHz","250 Hz–8 kHz","250 Hz–6 kHz")[i],fontSize=10.sp,color=Muted) }; Box(Modifier.weight(1f).height(11.dp).background(Color(0xFFF0F2F6),RoundedCornerShape(3.dp))) { Box(Modifier.fillMaxWidth(listOf(.5f,.75f,.5f,.375f)[i]).fillMaxHeight().background(listOf(Blue,Green,Color(0xFF9866EF),Color(0xFFFFA34D))[i],RoundedCornerShape(3.dp))) } } } }
                Panel(Color(0xFFEAF3FF)) { Text("What this means",color=Blue,fontWeight=FontWeight.Bold,fontSize=18.sp); Text("These are your reported responses at the selected volume. Headphones and device output affect results. This check cannot determine hearing age or diagnose hearing loss.",color=Muted,fontSize=14.sp) }
                OutlinedButton(onClick={prefs.edit().clear().apply();answers=emptyMap()},modifier=Modifier.fillMaxWidth()) { Text("Clear results & start again") }
            } else {
                Text("About Hearing Test",fontSize=27.sp,fontWeight=FontWeight.Bold)
                Panel { Text("Get to know your hearing",fontSize=21.sp,fontWeight=FontWeight.Bold); Text("Explore tones from 250 Hz to 22,000 Hz, choose an ear, and record what you hear. Use stereo headphones for separate ear checks.",color=Muted); Spacer(Modifier.height(12.dp)); Text("How to use",fontWeight=FontWeight.Bold); Text("1. Find a quiet place and put on headphones.\n2. Start with a low volume and select a tone.\n3. Tap Play, then record whether you hear it.\n4. Repeat across frequencies and ears.\n5. Open Results to see your responses.",color=Muted) }
                Panel(Color(0xFFEAF3FF)) { Text("An informal sound check",fontWeight=FontWeight.Bold,color=Blue); Text("This app is uncalibrated and does not measure clinical hearing thresholds or hearing age. Group ranges are illustrative. Your device may not reproduce very high frequencies. If you have concerns about your hearing, consult a qualified hearing professional.",color=Muted) }
                Text("Your results stay on this device.\nAudio: bundled supplied MP3 tones at 8–22 kHz; generated PCM tones at lower frequencies.",fontSize=12.sp,color=Muted)
            }
        }
    }
}

@Composable private fun Panel(color: Color=Color.White, content: @Composable ColumnScope.() -> Unit) {
    Surface(color=color,shape=RoundedCornerShape(20.dp),shadowElevation=1.dp,modifier=Modifier.fillMaxWidth()) { Column(Modifier.padding(17.dp),verticalArrangement=Arrangement.spacedBy(7.dp),content=content) }
}









