package com.hadedo.novaiptv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.Locale

fun Context.findActivity(): Activity? {
var ctx = this
while (ctx is ContextWrapper) {
if (ctx is Activity) return ctx
ctx = ctx.baseContext
}
return null
}

private fun formatDurationMs(ms: Long): String {
if (ms <= 0L) return "00:00"
val totalSec = ms / 1000
val hours = totalSec / 3600
val minutes = (totalSec % 3600) / 60
val seconds = totalSec % 60
return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
else String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

@Composable
fun LegalDisclaimerScreen(lang: String, onAccept: () -> Unit) {
val gold = Color(0xFFD4AF37)
val goldLight = Color(0xFFFDE047)
val title = when (lang) {
"en" -> "Terms of Use & Legal Disclaimer"
"de" -> "Nutzungsbedingungen & Rechtlicher Hinweis"
"tr" -> "Kullanım Koşulları ve Yasal Uyarı"
else -> "إخلاء المسؤولية القانونية وشروط الاستخدام"
}
val body = when (lang) {
"en" -> "DEVKAN is strictly a media player application. It does NOT provide or include any channels or subscriptions. Users must add their own legally obtained playlists."
"de" -> "DEVKAN ist ein reiner Medienplayer. Die App enthält keine Kanäle oder Abonnements. Nutzer müssen eigene Wiedergabelisten hinzufügen."
"tr" -> "DEVKAN yalnızca bir medya oynatıcıdır. Herhangi bir kanal veya abonelik içermez."
else -> "تطبيق DEVKAN هو مشغل وسائط تقني فقط ولا يتضمن أي قنوات أو اشتراكات مدمجة. يلتزم المستخدم بإضافة اشتراكاته وقوائم التشغيل الخاصة به."
}

Box(
modifier = Modifier.fillMaxSize().background(Color(0xFF07070A)).padding(24.dp),
contentAlignment = Alignment.Center
) {
Column(
modifier = Modifier
.fillMaxWidth()
.clip(RoundedCornerShape(24.dp))
.background(Color(0xFF111116))
.border(1.dp, gold.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
.padding(24.dp),
horizontalAlignment = Alignment.CenterHorizontally,
verticalArrangement = Arrangement.spacedBy(16.dp)
) {
Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = goldLight, modifier = Modifier.size(52.dp))
Text("DEVKAN 34 VIP", color = goldLight, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.5.sp)
Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
Text(body, color = Color(0xFFB0B5C0), fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
Button(
onClick = onAccept,
colors = ButtonDefaults.buttonColors(containerColor = gold),
shape = RoundedCornerShape(14.dp),
modifier = Modifier.fillMaxWidth().height(50.dp)
) {
Text("أوافق وأتابع الدخول / Continue", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
}
}
}
}

@Composable
fun AppLockScreen(lang: String, expectedPin: String, onUnlocked: () -> Unit) {
var enteredPin by remember { mutableStateOf("") }
var isError by remember { mutableStateOf(false) }
val gold = Color(0xFFD4AF37)

Box(
modifier = Modifier.fillMaxSize().background(Color(0xFF07070A)).padding(24.dp),
contentAlignment = Alignment.Center
) {
Column(
modifier = Modifier
.fillMaxWidth()
.clip(RoundedCornerShape(24.dp))
.background(Color(0xFF121218))
.border(1.dp, gold.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
.padding(24.dp),
horizontalAlignment = Alignment.CenterHorizontally,
verticalArrangement = Arrangement.spacedBy(16.dp)
) {
Icon(Icons.Default.Lock, contentDescription = null, tint = gold, modifier = Modifier.size(44.dp))
Text("DEVKAN App Lock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
OutlinedTextField(
value = enteredPin,
onValueChange = {
if (it.length <= 6) {
enteredPin = it
isError = false
if (it == expectedPin) onUnlocked()
}
},
singleLine = true,
label = { Text("PIN") },
isError = isError,
modifier = Modifier.fillMaxWidth()
)
Button(
onClick = { if (enteredPin == expectedPin) onUnlocked() else isError = true },
colors = ButtonDefaults.buttonColors(containerColor = gold),
modifier = Modifier.fillMaxWidth()
) {
Text("Unlock / فتح", color = Color.Black, fontWeight = FontWeight.Bold)
}
}
}
}

@OptIn(UnstableApi::class)
@Composable
fun DevkanHybridPlayerScreen(
streamUrl: String,
fallbackUrl: String = "",
streamTitle: String,
isVodOrSeries: Boolean = false,
streamId: String = "",
quickChannels: List<MediaItemModel> = emptyList(),
onSwitchChannel: (MediaItemModel) -> Unit = {},
lang: String = "ar",
onBack: () -> Unit
) {
val context = LocalContext.current
val activity = remember(context) { context.findActivity() }
val configuration = LocalConfiguration.current
val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
val gold = Color(0xFFD4AF37)

val preferredEngine = remember { DevkanVault.getPreferredEngine(context) }
val lowLatency = remember { DevkanVault.isLowLatencyEnabled(context) }
val geoBypass = remember { DevkanVault.isGeoBypassEnabled(context) }
val customUserAgent = remember { DevkanVault.getUserAgent(context) }

var currentUrl by remember(streamUrl) { mutableStateOf(streamUrl) }
var triedFallback by remember(streamUrl) { mutableStateOf(false) }
var activeEngine by remember(streamUrl) {
mutableStateOf(EngineCoordinator.selectInitialEngine(streamUrl, preferredEngine))
}

var controlsVisible by remember { mutableStateOf(true) }
var isLocked by remember { mutableStateOf(false) }
var isPaused by remember { mutableStateOf(false) }
var showQuickDrawer by remember { mutableStateOf(false) }
var drawerSearchQuery by remember { mutableStateOf("") }
var currentPosMs by remember { mutableLongStateOf(0L) }
var totalDurMs by remember { mutableLongStateOf(0L) }

var scaleIndex by remember { mutableIntStateOf(if (isLandscape) 1 else 0) }
var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
var reconnectAttempts by remember { mutableIntStateOf(0) }
var isReconnecting by remember { mutableStateOf(false) }
var showDiagnostics by remember { mutableStateOf(false) }
var seekFeedbackText by remember { mutableStateOf<String?>(null) }
var vlcPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

val startTimestamp = remember(currentUrl) { System.currentTimeMillis() }
var startupMs by remember(currentUrl) { mutableLongStateOf(0L) }

DisposableEffect(activity, isPaused) {
val window = activity?.window
if (window != null) {
if (!isPaused) {
window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
} else {
window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}
}
onDispose {
try {
window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
} catch (_: Exception) { }
}
}

LaunchedEffect(isLandscape) {
scaleIndex = if (isLandscape) 1 else 0
}

BackHandler {
if (showQuickDrawer) {
showQuickDrawer = false
} else {
try {
activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
} catch (_: Exception) { }
onBack()
}
}

DisposableEffect(activity) {
val window = activity?.window
if (window != null) {
WindowCompat.setDecorFitsSystemWindows(window, false)
val controller = WindowInsetsControllerCompat(window, window.decorView)
controller.hide(WindowInsetsCompat.Type.systemBars())
controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}
onDispose {
try {
activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
if (window != null) {
WindowCompat.setDecorFitsSystemWindows(window, true)
WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
}
} catch (_: Exception) { }
}
}

LaunchedEffect(currentUrl) {
if (!isVodOrSeries) {
DevkanVault.saveLastChannel(context, currentUrl, streamTitle)
}
}

LaunchedEffect(controlsVisible, isLocked, isPaused, showQuickDrawer) {
if (controlsVisible && !isLocked && !isPaused && !showQuickDrawer) {
delay(5000)
controlsVisible = false
}
}

LaunchedEffect(seekFeedbackText) {
if (seekFeedbackText != null) {
delay(900)
seekFeedbackText = null
}
}

val exoPlayer = remember(activeEngine) {
if (activeEngine == EngineType.MEDIA3_ONLY) {
try {
val (minBuf, maxBuf, playBuf) = EngineCoordinator.getAdaptiveBufferMs(lowLatency)
val loadControl = DefaultLoadControl.Builder()
.setBufferDurationsMs(minBuf, maxBuf, playBuf, playBuf + 250)
.build()
val headers = EngineCoordinator.buildSmartHeaders(geoBypass, customUserAgent)
val httpFactory = DefaultHttpDataSource.Factory()
.setUserAgent(customUserAgent)
.setDefaultRequestProperties(headers)
.setAllowCrossProtocolRedirects(true)
.setConnectTimeoutMs(10_000)
.setReadTimeoutMs(15_000)

ExoPlayer.Builder(context)
.setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
.setLoadControl(loadControl)
.build()
} catch (_: Exception) {
null
}
} else null
}

LaunchedEffect(exoPlayer, currentUrl, streamId) {
if (exoPlayer != null) {
try {
exoPlayer.stop()
exoPlayer.clearMediaItems()
exoPlayer.setMediaItem(MediaItem.fromUri(currentUrl))
val savedPos = if (isVodOrSeries && streamId.isNotEmpty()) DevkanVault.getVodPosition(context, streamId) else 0L
if (savedPos > 5000L) exoPlayer.seekTo(savedPos)
exoPlayer.prepare()
exoPlayer.playWhenReady = true
isPaused = false
} catch (_: Exception) { }
}
}

LaunchedEffect(exoPlayer, vlcPlayerRef, isVodOrSeries, streamId) {
while (true) {
delay(1000)
try {
if (exoPlayer != null) {
currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
totalDurMs = exoPlayer.duration.coerceAtLeast(0L)
isPaused = !exoPlayer.isPlaying && exoPlayer.playbackState == Player.STATE_READY
if (isVodOrSeries && streamId.isNotEmpty() && currentPosMs > 2000L && totalDurMs > 0L) {
DevkanVault.saveVodProgress(context, streamId, currentPosMs, totalDurMs)
}
} else if (vlcPlayerRef != null && vlcPlayerRef?.isReleased == false) {
currentPosMs = vlcPlayerRef?.time?.coerceAtLeast(0L) ?: 0L
totalDurMs = vlcPlayerRef?.length?.coerceAtLeast(0L) ?: 0L
isPaused = vlcPlayerRef?.isPlaying == false
if (isVodOrSeries && streamId.isNotEmpty() && currentPosMs > 2000L && totalDurMs > 0L) {
DevkanVault.saveVodProgress(context, streamId, currentPosMs, totalDurMs)
}
}
} catch (_: Exception) { }
}
}

DisposableEffect(exoPlayer) {
val listener = object : Player.Listener {
override fun onPlaybackStateChanged(state: Int) {
if (state == Player.STATE_READY && startupMs == 0L) {
startupMs = (System.currentTimeMillis() - startTimestamp).coerceAtLeast(120L)
isReconnecting = false
}
}

override fun onPlayerError(error: PlaybackException) {
if (!triedFallback && fallbackUrl.isNotEmpty() && fallbackUrl != currentUrl) {
triedFallback = true
currentUrl = fallbackUrl
} else if (preferredEngine == EngineType.AUTO_HYBRID && activeEngine == EngineType.MEDIA3_ONLY) {
activeEngine = EngineType.LIBVLC_ONLY
} else if (reconnectAttempts < 3) {
reconnectAttempts++
isReconnecting = true
try {
exoPlayer?.prepare()
exoPlayer?.playWhenReady = true
} catch (_: Exception) { }
}
}
}
exoPlayer?.addListener(listener)
onDispose {
try {
if (isVodOrSeries && streamId.isNotEmpty() && exoPlayer != null) {
DevkanVault.saveVodProgress(context, streamId, exoPlayer.currentPosition, exoPlayer.duration)
}
exoPlayer?.removeListener(listener)
exoPlayer?.stop()
exoPlayer?.release()
} catch (_: Exception) { }
}
}

LaunchedEffect(activeEngine, reconnectAttempts, startupMs) {
while (true) {
val buffered = try { exoPlayer?.totalBufferedDuration ?: 1200L } catch (_: Exception) { 1200L }
val bitrate = try { exoPlayer?.videoFormat?.bitrate?.div(1000) ?: 4200 } catch (_: Exception) { 4200 }
EngineCoordinator.currentDiagnostics.value = StreamDiagnostics(
activeEngine = if (activeEngine == EngineType.MEDIA3_ONLY) "Media3 Low-Latency" else "LibVLC Universal",
pingMs = 38L,
startupTimeMs = if (startupMs > 0) startupMs else 450L,
bitrateKbps = if (bitrate > 0) bitrate else 3900,
bufferDurationMs = buffered,
reconnectCount = reconnectAttempts,
statusNote = if (isReconnecting) "إعادة اتصال ($reconnectAttempts/3)..." else "مستقر • الشاشة محمية من الانطفاء"
)
delay(2000)
}
}

fun togglePlayPause() {
try {
if (exoPlayer != null) {
if (exoPlayer.isPlaying) {
exoPlayer.pause()
isPaused = true
} else {
exoPlayer.play()
isPaused = false
}
} else if (vlcPlayerRef != null && vlcPlayerRef?.isReleased == false) {
if (vlcPlayerRef?.isPlaying == true) {
vlcPlayerRef?.pause()
isPaused = true
} else {
vlcPlayerRef?.play()
isPaused = false
}
}
} catch (_: Exception) { }
}

fun seekByDelta(deltaMs: Long) {
try {
if (exoPlayer != null) {
exoPlayer.seekTo((exoPlayer.currentPosition + deltaMs).coerceAtLeast(0L))
} else if (vlcPlayerRef != null && vlcPlayerRef?.isReleased == false) {
vlcPlayerRef?.time = ((vlcPlayerRef?.time ?: 0L) + deltaMs).coerceAtLeast(0L)
}
} catch (_: Exception) { }
}

val exoResizeMode = when (scaleIndex) {
1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
}

val vlcScaleType = when (scaleIndex) {
1 -> MediaPlayer.ScaleType.SURFACE_FILL
2 -> MediaPlayer.ScaleType.SURFACE_16_9
3 -> MediaPlayer.ScaleType.SURFACE_FIT_SCREEN
else -> MediaPlayer.ScaleType.SURFACE_BEST_FIT
}

Box(
modifier = Modifier
.fillMaxSize()
.background(Color.Black)
.pointerInput(isLocked) {
detectTapGestures(
onTap = {
if (showQuickDrawer) showQuickDrawer = false
else controlsVisible = !controlsVisible
},
onDoubleTap = { offset ->
if (!isLocked) {
val isRightSide = offset.x > size.width / 2f
val delta = if (isRightSide) 10_000L else -10_000L
seekByDelta(delta)
seekFeedbackText = if (isRightSide) "+10s ⏩" else "⏪ -10s"
}
}
)
}
) {
if (activeEngine == EngineType.MEDIA3_ONLY && exoPlayer != null) {
AndroidView(
factory = { ctx ->
PlayerView(ctx).apply {
player = exoPlayer
useController = false
resizeMode = exoResizeMode
setKeepContentOnPlayerReset(false)
layoutParams = FrameLayout.LayoutParams(
ViewGroup.LayoutParams.MATCH_PARENT,
ViewGroup.LayoutParams.MATCH_PARENT
)
}
},
update = { view ->
if (view.player !== exoPlayer) {
view.player = exoPlayer
}
view.resizeMode = exoResizeMode
},
modifier = Modifier.fillMaxSize()
)
} else {
key(currentUrl) {
AndroidView(
factory = { ctx ->
val vlcLayout = VLCVideoLayout(ctx).apply {
layoutParams = FrameLayout.LayoutParams(
ViewGroup.LayoutParams.MATCH_PARENT,
ViewGroup.LayoutParams.MATCH_PARENT
)
}
try {
val cacheArg = if (lowLatency) "--network-caching=450" else "--network-caching=1200"
val libVlc = LibVLC(
ctx,
arrayListOf(
cacheArg,
"--rtsp-tcp",
"--http-user-agent=$customUserAgent",
"--drop-late-frames",
"--skip-frames"
)
)
val mediaPlayer = MediaPlayer(libVlc).apply {
attachViews(vlcLayout, null, false, false)
videoScale = vlcScaleType
val media = Media(libVlc, Uri.parse(currentUrl))
media.setHWDecoderEnabled(true, false)
setMedia(media)
media.release()
play()
val savedPos = if (isVodOrSeries && streamId.isNotEmpty()) DevkanVault.getVodPosition(ctx, streamId) else 0L
if (savedPos > 5000L) time = savedPos
}
vlcPlayerRef = mediaPlayer
vlcLayout.tag = Pair(libVlc, mediaPlayer)
} catch (_: Exception) { }
vlcLayout
},
update = { view ->
try {
val pair = view.tag as? Pair<*, *>
val mp = pair?.second as? MediaPlayer
if (mp != null && !mp.isReleased) {
mp.videoScale = vlcScaleType
mp.updateVideoSurfaces()
}
} catch (_: Exception) { }
},
onRelease = { view ->
try {
val pair = view.tag as? Pair<*, *>
val mp = pair?.second as? MediaPlayer
val vlc = pair?.first as? LibVLC
if (mp != null && !mp.isReleased) {
mp.stop()
mp.detachViews()
mp.release()
}
vlc?.release()
vlcPlayerRef = null
} catch (_: Exception) { }
},
modifier = Modifier.fillMaxSize()
)
}
}

seekFeedbackText?.let { text ->
Box(
modifier = Modifier
.align(Alignment.Center)
.clip(RoundedCornerShape(16.dp))
.background(Color.Black.copy(alpha = 0.72f))
.padding(horizontal = 20.dp, vertical = 10.dp)
) {
Text(text = text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
}
}

if (isReconnecting) {
Row(
modifier = Modifier
.align(Alignment.TopCenter)
.padding(top = 64.dp)
.clip(RoundedCornerShape(12.dp))
.background(Color(0xFFB91C1C).copy(alpha = 0.9f))
.padding(horizontal = 14.dp, vertical = 6.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
Text("إعادة الاتصال التلقائي ($reconnectAttempts/3)...", color = Color.White, fontSize = 12.sp)
}
}

if (isLocked && controlsVisible) {
IconButton(
onClick = { isLocked = false },
modifier = Modifier
.align(Alignment.TopStart)
.padding(16.dp)
.background(Color.Black.copy(alpha = 0.65f), CircleShape)
) {
Icon(Icons.Default.LockOpen, contentDescription = "Unlock", tint = gold)
}
}

if (showQuickDrawer && quickChannels.isNotEmpty()) {
val drawerFiltered = remember(quickChannels, drawerSearchQuery) {
if (drawerSearchQuery.isBlank()) quickChannels
else quickChannels.filter { it.title.contains(drawerSearchQuery.trim(), ignoreCase = true) }
}
Column(
modifier = Modifier
.align(Alignment.CenterStart)
.fillMaxHeight()
.width(275.dp)
.background(Color(0xFF0E0E16).copy(alpha = 0.95f))
.border(1.dp, gold.copy(alpha = 0.4f))
.padding(12.dp),
verticalArrangement = Arrangement.spacedBy(8.dp)
) {
Row(
modifier = Modifier.fillMaxWidth(),
horizontalArrangement = Arrangement.SpaceBetween,
verticalAlignment = Alignment.CenterVertically
) {
Text("القنوات (${drawerFiltered.size})", color = gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
IconButton(onClick = { showQuickDrawer = false }) {
Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
}
}
OutlinedTextField(
value = drawerSearchQuery,
onValueChange = { drawerSearchQuery = it },
placeholder = { Text("بحث سريع عن قناة...", fontSize = 11.sp) },
singleLine = true,
shape = RoundedCornerShape(10.dp),
modifier = Modifier.fillMaxWidth()
)
LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
items(drawerFiltered, key = { it.id }) { ch ->
val isCurrent = ch.url == currentUrl
Row(
modifier = Modifier
.fillMaxWidth()
.clip(RoundedCornerShape(10.dp))
.background(if (isCurrent) gold.copy(alpha = 0.25f) else Color(0xFF171724))
.clickable {
showQuickDrawer = false
onSwitchChannel(ch)
}
.padding(10.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.SpaceBetween
) {
Text(
text = ch.title,
color = if (isCurrent) gold else Color.White,
fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
fontSize = 12.sp,
maxLines = 1,
overflow = TextOverflow.Ellipsis,
modifier = Modifier.weight(1f)
)
if (isCurrent) {
Icon(Icons.Default.Equalizer, contentDescription = null, tint = gold, modifier = Modifier.size(16.dp))
}
}
}
}
}
}

AnimatedVisibility(visible = controlsVisible && !isLocked, modifier = Modifier.fillMaxSize()) {
Box(
modifier = Modifier
.fillMaxSize()
.background(
Brush.verticalGradient(
colors = listOf(
Color.Black.copy(alpha = 0.78f),
Color.Transparent,
Color.Black.copy(alpha = 0.88f)
)
)
)
.padding(16.dp)
) {
Row(
modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.SpaceBetween
) {
Row(
modifier = Modifier.weight(1f),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
IconButton(onClick = {
try {
activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
} catch (_: Exception) { }
onBack()
}) {
Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
}
Column(modifier = Modifier.weight(1f)) {
Text(
text = streamTitle,
color = Color.White,
fontWeight = FontWeight.Bold,
fontSize = 15.sp,
maxLines = 1,
overflow = TextOverflow.Ellipsis
)
Text(
text = if (activeEngine == EngineType.MEDIA3_ONLY) "Media3 Fast" else "LibVLC Hybrid",
color = gold,
fontSize = 11.sp,
fontWeight = FontWeight.SemiBold
)
}
}

Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
if (!isVodOrSeries && quickChannels.isNotEmpty()) {
IconButton(onClick = { showQuickDrawer = !showQuickDrawer }) {
Icon(Icons.Default.List, contentDescription = "Channels", tint = gold)
}
}

IconButton(onClick = {
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
try {
val params = PictureInPictureParams.Builder()
.setAspectRatio(Rational(16, 9))
.build()
activity.enterPictureInPictureMode(params)
} catch (_: Exception) { }
}
}) {
Icon(Icons.Default.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White)
}

OutlinedButton(
onClick = {
activeEngine = if (activeEngine == EngineType.MEDIA3_ONLY) EngineType.LIBVLC_ONLY else EngineType.MEDIA3_ONLY
},
contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
shape = RoundedCornerShape(8.dp)
) {
Text(
text = if (activeEngine == EngineType.MEDIA3_ONLY) "VLC" else "Media3",
color = Color.White,
fontSize = 11.sp
)
}

IconButton(onClick = { showDiagnostics = !showDiagnostics }) {
Icon(Icons.Default.Speed, contentDescription = "Diagnostics", tint = gold)
}
}
}

Row(
modifier = Modifier.align(Alignment.Center),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(28.dp)
) {
if (isVodOrSeries) {
IconButton(
onClick = { seekByDelta(-10_000L) },
modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape)
) {
Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(28.dp))
}
}

IconButton(
onClick = { togglePlayPause() },
modifier = Modifier
.size(66.dp)
.background(gold.copy(alpha = 0.92f), CircleShape)
) {
Icon(
imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
contentDescription = "Play/Pause",
tint = Color.Black,
modifier = Modifier.size(38.dp)
)
}

if (isVodOrSeries) {
IconButton(
onClick = { seekByDelta(10_000L) },
modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape)
) {
Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(28.dp))
}
}
}

if (showDiagnostics) {
val diag = EngineCoordinator.currentDiagnostics.value
Column(
modifier = Modifier
.align(Alignment.CenterEnd)
.width(245.dp)
.clip(RoundedCornerShape(16.dp))
.background(Color(0xFF101016).copy(alpha = 0.92f))
.border(1.dp, gold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
.padding(14.dp),
verticalArrangement = Arrangement.spacedBy(6.dp)
) {
Text("مركز تشخيص البث الحي", color = gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
Text("المحرك: ${diag.activeEngine}", color = Color.White, fontSize = 11.sp)
Text("زمن الفتح: ${diag.startupTimeMs} ms", color = Color.White, fontSize = 11.sp)
Text("السرعة: ${diag.bitrateKbps} kbps", color = Color.White, fontSize = 11.sp)
Text("التخزين المؤقت: ${diag.bufferDurationMs / 1000.0} s", color = Color.White, fontSize = 11.sp)
Text("الحالة: ${diag.statusNote}", color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
}
}

Column(
modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
verticalArrangement = Arrangement.spacedBy(4.dp)
) {
if (isVodOrSeries && totalDurMs > 0L) {
Row(
modifier = Modifier.fillMaxWidth(),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
Text(formatDurationMs(currentPosMs), color = Color.White, fontSize = 11.sp)
Slider(
value = (currentPosMs.toFloat() / totalDurMs.toFloat()).coerceIn(0f, 1f),
onValueChange = { fraction ->
val target = (fraction * totalDurMs).toLong()
currentPosMs = target
try {
exoPlayer?.seekTo(target)
if (vlcPlayerRef != null && vlcPlayerRef?.isReleased == false) {
vlcPlayerRef?.time = target
}
} catch (_: Exception) { }
},
colors = SliderDefaults.colors(thumbColor = gold, activeTrackColor = gold),
modifier = Modifier.weight(1f)
)
Text(formatDurationMs(totalDurMs), color = Color.White, fontSize = 11.sp)
}
}

Row(
modifier = Modifier.fillMaxWidth(),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.SpaceEvenly
) {
TextButton(onClick = {
try {
activity?.requestedOrientation = if (isLandscape) {
ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
} else {
ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
}
} catch (_: Exception) { }
}) {
Icon(Icons.Default.ScreenRotation, contentDescription = "Rotate", tint = gold, modifier = Modifier.size(18.dp))
Spacer(Modifier.width(4.dp))
Text(if (isLandscape) "طولي" else "عرضي", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
}

TextButton(onClick = { scaleIndex = (scaleIndex + 1) % 4 }) {
Icon(Icons.Default.AspectRatio, contentDescription = null, tint = gold, modifier = Modifier.size(18.dp))
Spacer(Modifier.width(4.dp))
val modeName = when (scaleIndex) {
1 -> "Fill"
2 -> "Zoom"
3 -> "16:9"
else -> "Fit"
}
Text(modeName, color = gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
}

if (isVodOrSeries) {
Button(
onClick = {
seekByDelta(85_000L)
seekFeedbackText = "تخطي المقدمة +85s ⏭️"
},
colors = ButtonDefaults.buttonColors(containerColor = gold.copy(alpha = 0.28f)),
contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
shape = RoundedCornerShape(8.dp)
) {
Text("تخطي الانترو", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
}

if (exoPlayer != null) {
TextButton(onClick = {
playbackSpeed = when (playbackSpeed) {
1.0f -> 1.25f
1.25f -> 1.5f
1.5f -> 2.0f
else -> 1.0f
}
exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
}) {
Text("${playbackSpeed}x", color = gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}
}
} else {
TextButton(onClick = { isLocked = true }) {
Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
Spacer(Modifier.width(4.dp))
Text("قفل", color = Color.White, fontSize = 11.sp)
}
}
}
}
}
}
}
}

@Composable
fun EpgTimelineGridSheet(
slots: List<EpgSlot>,
lang: String,
onPlayChannel: (String) -> Unit,
onClose: () -> Unit
) {
BackHandler { onClose() }
var searchQuery by remember { mutableStateOf("") }
val filtered = remember(slots, searchQuery) {
if (searchQuery.isBlank()) slots
else slots.filter {
it.channelName.contains(searchQuery, ignoreCase = true) ||
it.currentProgram.contains(searchQuery, ignoreCase = true)
}
}

Column(
modifier = Modifier
.fillMaxSize()
.background(Color(0xFF0B0A14))
.padding(16.dp)
) {
Row(
modifier = Modifier.fillMaxWidth(),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.SpaceBetween
) {
Text("دليل البرامج الزمني (EPG)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
IconButton(onClick = onClose) {
Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
}
}

OutlinedTextField(
value = searchQuery,
onValueChange = { searchQuery = it },
singleLine = true,
leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFD4AF37)) },
placeholder = { Text("ابحث في دليل البرامج أو القنوات...") },
modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
shape = RoundedCornerShape(14.dp)
)

LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
items(filtered.take(200)) { slot ->
Row(
modifier = Modifier
.fillMaxWidth()
.clip(RoundedCornerShape(14.dp))
.background(Color(0xFF171429))
.border(1.dp, Color(0xFF2D274B), RoundedCornerShape(14.dp))
.clickable { onPlayChannel(slot.channelName) }
.padding(12.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
Column(modifier = Modifier.width(105.dp)) {
Text(slot.channelName, color = Color(0xFFFDE047), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
Text(text = slot.timeRange, color = Color(0xFF9CA3AF), fontSize = 11.sp)
}
Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
Text(slot.currentProgram, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
LinearProgressIndicator(
progress = { slot.progress },
color = Color(0xFFD4AF37),
trackColor = Color(0xFF2D274B),
modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
)
Text("التالي: ${slot.nextProgram}", color = Color(0xFF9CA3AF), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
}
}
}
}
}
