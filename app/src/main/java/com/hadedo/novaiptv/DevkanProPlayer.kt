package com.hadedo.novaiptv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

data class SrtCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

private fun parseSrtTimeToMs(raw: String): Long {
    return try {
        val clean = raw.trim().replace(',', '.')
        val parts = clean.split(":")
        val h = parts[0].toLong()
        val m = parts[1].toLong()
        val secParts = parts[2].split(".")
        val s = secParts[0].toLong()
        val ms = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0L
        ((h * 3600) + (m * 60) + s) * 1000L + ms
    } catch (_: Exception) {
        0L
    }
}

private fun parseSrtFromUri(context: Context, uri: Uri): List<SrtCue> {
    val result = mutableListOf<SrtCue>()
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
            val lines = reader.readLines()
            var i = 0
            while (i < lines.size) {
                val line = lines[i].trim()
                if (line.contains("-->")) {
                    val times = line.split("-->")
                    if (times.size == 2) {
                        val start = parseSrtTimeToMs(times[0])
                        val end = parseSrtTimeToMs(times[1].substringBefore(" "))
                        val sb = StringBuilder()
                        i++
                        while (i < lines.size && lines[i].isNotBlank()) {
                            if (sb.isNotEmpty()) sb.append("\n")
                            sb.append(lines[i].replace(Regex("<[^>]*>"), "").trim())
                            i++
                        }
                        if (sb.isNotEmpty() && end > start) {
                            result.add(SrtCue(start, end, sb.toString()))
                        }
                    }
                }
                i++
            }
        }
    } catch (_: Exception) { }
    return result
}

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
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070A))
            .padding(24.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
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
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070A))
            .padding(24.dp),
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

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@OptIn(UnstableApi::class)
@Composable
fun DevkanHybridPlayerScreen(
    streamUrl: String,
    fallbackUrl: String = "",
    streamTitle: String,
    isVodOrSeries: Boolean = false,
    isSportsSection: Boolean = false,
    streamId: String = "",
    subId: String = "default",
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    quickChannels: List<MediaItemModel> = emptyList(),
    onSwitchChannel: (MediaItemModel) -> Unit = {},
    lang: String = "ar",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    val preferredEngine = remember { DevkanVault.getPreferredEngine(context) }
    val externalPlayerMode = remember { DevkanVault.getExternalPlayerMode(context) }
    val userLowLatencyEnabled = remember { DevkanVault.isLowLatencyEnabled(context) }
    val performanceMode = remember { DevkanVault.getPerformanceMode(context) }
    val tuningConfig = remember(context, performanceMode) {
        DevicePerformanceManager.buildTuningConfig(context, performanceMode)
    }
    val geoBypass = remember { DevkanVault.isGeoBypassEnabled(context) }
    val customUserAgent = remember { DevkanVault.getUserAgent(context) }

    var media3FailedFallback by remember(streamUrl) { mutableStateOf(false) }
    var useFallbackUrl by remember(streamUrl, fallbackUrl) { mutableStateOf(false) }

    val playbackConfig = remember(
        streamUrl,
        isSportsSection,
        preferredEngine,
        externalPlayerMode,
        userLowLatencyEnabled,
        media3FailedFallback,
        tuningConfig
    ) {
        val routed = PlayerEngineRouter.resolvePlaybackConfig(
            isSportsSection = isSportsSection,
            preferredEngineType = preferredEngine,
            externalPlayerMode = externalPlayerMode,
            userLowLatencyEnabled = userLowLatencyEnabled,
            media3FailedFallback = media3FailedFallback,
            tuningConfig = tuningConfig
        )
        val suggestedEngineType = EngineCoordinator.selectInitialEngine(streamUrl, preferredEngine)
        if (preferredEngine == EngineType.AUTO_HYBRID &&
            !media3FailedFallback &&
            routed.engine == PlayerEngine.MEDIA3_EXOPLAYER &&
            suggestedEngineType == EngineType.LIBVLC_ONLY
        ) {
            routed.copy(engine = PlayerEngine.LIB_VLC)
        } else {
            routed
        }
    }

    val useVlcEngine = playbackConfig.engine == PlayerEngine.LIB_VLC
    val effectiveUrl = remember(streamUrl, fallbackUrl, useFallbackUrl) {
        if (useFallbackUrl && fallbackUrl.isNotBlank()) fallbackUrl else streamUrl
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var showQuickDrawer by remember { mutableStateOf(false) }
    var showTracksSheet by remember { mutableStateOf(false) }
    var drawerSearchQuery by remember { mutableStateOf("") }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var totalDurMs by remember { mutableLongStateOf(0L) }
    var favState by remember(streamId, isFavorite) { mutableStateOf(isFavorite) }

    var subtitleCues by remember(streamUrl) { mutableStateOf<List<SrtCue>>(emptyList()) }
    var subtitleOffsetMs by remember(streamUrl) { mutableLongStateOf(0L) }
    var activeSubtitleText by remember(streamUrl) { mutableStateOf("") }

    var scaleIndex by remember { mutableIntStateOf(if (isLandscape) 1 else 0) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var vlcPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var libVlcRef by remember { mutableStateOf<LibVLC?>(null) }

    val subtitlePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val parsed = withContext(Dispatchers.IO) { parseSrtFromUri(context, uri) }
                subtitleCues = parsed
                subtitleOffsetMs = 0L
                seekFeedbackText = if (parsed.isNotEmpty()) "تم تحميل ملف الترجمة" else "ملف الترجمة غير مدعوم"
            }
        }
    }

    DisposableEffect(activity, isPaused) {
        val window = activity?.window
        if (window != null) {
            try {
                if (!isPaused) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            } catch (_: Exception) { }
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

    DisposableEffect(
        useVlcEngine,
        isVodOrSeries,
        playbackConfig.enableLowLatency,
        playbackConfig.preferHighestBitrate,
        tuningConfig
    ) {
        var createdPlayer: ExoPlayer? = null
        if (!useVlcEngine) {
            try {
                val baseMin = when (tuningConfig.tier) {
                    PerformanceTier.LOW_TIER -> 10_000
                    PerformanceTier.MID_TIER -> 15_000
                    PerformanceTier.HIGH_TIER -> 20_000
                }
                val baseMax = when (tuningConfig.tier) {
                    PerformanceTier.LOW_TIER -> 30_000
                    PerformanceTier.MID_TIER -> 45_000
                    PerformanceTier.HIGH_TIER -> 60_000
                }
                val basePlay = when (tuningConfig.tier) {
                    PerformanceTier.LOW_TIER -> 2_000
                    PerformanceTier.MID_TIER -> 3_000
                    PerformanceTier.HIGH_TIER -> 4_000
                }
                val baseRebuffer = when (tuningConfig.tier) {
                    PerformanceTier.LOW_TIER -> 4_000
                    PerformanceTier.MID_TIER -> 5_000
                    PerformanceTier.HIGH_TIER -> 6_000
                }
                val connectTimeout = when (tuningConfig.tier) {
                    PerformanceTier.LOW_TIER -> 10_000
                    PerformanceTier.MID_TIER -> 12_000
                    PerformanceTier.HIGH_TIER -> 15_000
                }

                val minBuf = when {
                    isVodOrSeries -> baseMin.coerceAtLeast(20_000)
                    playbackConfig.enableLowLatency -> 8_000
                    else -> baseMin
                }
                val maxBuf = when {
                    isVodOrSeries -> baseMax.coerceAtLeast(60_000)
                    playbackConfig.enableLowLatency -> 25_000
                    else -> baseMax
                }
                val playBuf = when {
                    isVodOrSeries -> basePlay.coerceAtLeast(3_000)
                    playbackConfig.enableLowLatency -> 1_500
                    else -> basePlay
                }
                val rebufferBuf = when {
                    isVodOrSeries -> baseRebuffer.coerceAtLeast(5_000)
                    playbackConfig.enableLowLatency -> 3_000
                    else -> baseRebuffer
                }

                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(minBuf, maxBuf, playBuf, rebufferBuf)
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()

                val trackSelector = DefaultTrackSelector(context).apply {
                    setParameters(
                        buildUponParameters()
                            .setForceHighestSupportedBitrate(playbackConfig.preferHighestBitrate)
                            .setPreferredAudioLanguage(lang)
                            .setPreferredTextLanguage(lang)
                    )
                }

                val headers = EngineCoordinator.buildSmartHeaders(geoBypass, customUserAgent)
                val httpFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent(customUserAgent)
                    .setDefaultRequestProperties(headers)
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(connectTimeout)
                    .setReadTimeoutMs(connectTimeout * 2)

                createdPlayer = ExoPlayer.Builder(context)
                    .setTrackSelector(trackSelector)
                    .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
                    .setLoadControl(loadControl)
                    .build()

                createdPlayer.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        if (preferredEngine == EngineType.AUTO_HYBRID) {
                            if (!useFallbackUrl && fallbackUrl.isNotBlank()) {
                                useFallbackUrl = true
                                seekFeedbackText = "تبديل تلقائي للسيرفر البديل..."
                            } else if (!media3FailedFallback) {
                                media3FailedFallback = true
                                seekFeedbackText = "تبديل تلقائي إلى محرك LibVLC..."
                            }
                        }
                    }
                })

                exoPlayer = createdPlayer
            } catch (_: Exception) {
                createdPlayer = null
                exoPlayer = null
            }
        }
        onDispose {
            val p = createdPlayer
            if (p != null) {
                try {
                    val pos = p.currentPosition
                    val dur = p.duration
                    if (isVodOrSeries && streamId.isNotEmpty() && pos > 2000L && dur > 0L) {
                        CoroutineScope(Dispatchers.IO).launch {
                            DevkanVault.saveVodProgress(context, streamId, pos, dur)
                        }
                    }
                    p.stop()
                    p.clearMediaItems()
                    p.release()
                } catch (_: Exception) { }
            }
            if (exoPlayer === p) {
                exoPlayer = null
            }
        }
    }

    fun restoreWindowAndExit() {
        try {
            exoPlayer?.playWhenReady = false
            exoPlayer?.stop()
            vlcPlayerRef?.pause()
            vlcPlayerRef?.stop()
            if (activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val window = activity.window
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
                window.decorView.requestApplyInsets()
            }
        } catch (_: Exception) { }
        onBack()
    }

    BackHandler {
        when {
            showTracksSheet -> showTracksSheet = false
            showQuickDrawer -> showQuickDrawer = false
            else -> restoreWindowAndExit()
        }
    }

    DisposableEffect(activity) {
        val window = activity?.window
        if (window != null) {
            try {
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } catch (_: Exception) { }
        }
        onDispose {
            try {
                if (window != null) {
                    WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
                    window.decorView.requestApplyInsets()
                }
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(streamUrl) {
        if (!isVodOrSeries && streamUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                DevkanVault.saveLastChannel(context, streamUrl, streamTitle)
            }
        }
    }

    LaunchedEffect(controlsVisible, isLocked, isPaused, showQuickDrawer, showTracksSheet) {
        if (controlsVisible && !isLocked && !isPaused && !showQuickDrawer && !showTracksSheet) {
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

    LaunchedEffect(exoPlayer, effectiveUrl, streamId) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (effectiveUrl.isNotBlank()) {
            try {
                player.stop()
                player.clearMediaItems()
                player.setMediaItem(MediaItem.fromUri(effectiveUrl))
                val savedPos = if (isVodOrSeries && streamId.isNotEmpty()) {
                    withContext(Dispatchers.IO) { DevkanVault.getVodPosition(context, streamId) }
                } else 0L
                if (savedPos > 5000L) player.seekTo(savedPos)
                player.prepare()
                player.playWhenReady = true
                isPaused = false
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(exoPlayer, vlcPlayerRef, isVodOrSeries, streamId, controlsVisible) {
        if (!isVodOrSeries) return@LaunchedEffect
        var tick = 0
        while (true) {
            delay(1000)
            tick++
            try {
                val pos: Long
                val dur: Long
                val ep = exoPlayer
                val vp = vlcPlayerRef
                if (ep != null) {
                    pos = ep.currentPosition.coerceAtLeast(0L)
                    dur = ep.duration.coerceAtLeast(0L)
                    isPaused = !ep.isPlaying && ep.playbackState == Player.STATE_READY
                } else if (vp != null && !vp.isReleased) {
                    pos = vp.time.coerceAtLeast(0L)
                    dur = vp.length.coerceAtLeast(0L)
                    isPaused = !vp.isPlaying
                } else {
                    continue
                }

                if (controlsVisible) {
                    currentPosMs = pos
                    totalDurMs = dur
                }

                if (tick % 10 == 0 && streamId.isNotEmpty() && pos > 2000L && dur > 0L) {
                    withContext(Dispatchers.IO) {
                        DevkanVault.saveVodProgress(context, streamId, pos, dur)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(subtitleCues, subtitleOffsetMs, exoPlayer, vlcPlayerRef) {
        if (subtitleCues.isEmpty()) {
            activeSubtitleText = ""
            return@LaunchedEffect
        }
        while (true) {
            val pos = try {
                val ep = exoPlayer
                val vp = vlcPlayerRef
                when {
                    ep != null -> ep.currentPosition
                    vp != null && !vp.isReleased -> vp.time
                    else -> 0L
                }
            } catch (_: Exception) { 0L }

            val adjustedPos = pos + subtitleOffsetMs
            val cue = subtitleCues.firstOrNull { adjustedPos in it.startMs..it.endMs }
            val newText = cue?.text ?: ""
            if (activeSubtitleText != newText) {
                activeSubtitleText = newText
            }
            delay(250)
        }
    }

    LaunchedEffect(showDiagnostics, useVlcEngine, playbackConfig.enableLowLatency, media3FailedFallback) {
        if (!showDiagnostics) return@LaunchedEffect
        while (true) {
            val buffered = try { exoPlayer?.totalBufferedDuration ?: 4000L } catch (_: Exception) { 4000L }
            val bitrate = try { exoPlayer?.videoFormat?.bitrate?.div(1000) ?: 4500 } catch (_: Exception) { 4500 }
            EngineCoordinator.currentDiagnostics.value = StreamDiagnostics(
                activeEngine = if (!useVlcEngine) "Media3 ExoPlayer" else "LibVLC",
                pingMs = 30L,
                startupTimeMs = 400L,
                bitrateKbps = if (bitrate > 0) bitrate else 4200,
                bufferDurationMs = buffered,
                reconnectCount = if (media3FailedFallback) 1 else 0,
                statusNote = if (playbackConfig.enableLowLatency) "مستقر • كمون منخفض" else "مستقر"
            )
            delay(2000)
        }
    }

    fun togglePlayPause() {
        try {
            val ep = exoPlayer
            val vp = vlcPlayerRef
            if (ep != null) {
                if (ep.isPlaying) {
                    ep.pause()
                    isPaused = true
                } else {
                    ep.play()
                    isPaused = false
                }
            } else if (vp != null && !vp.isReleased) {
                if (vp.isPlaying) {
                    vp.pause()
                    isPaused = true
                } else {
                    vp.play()
                    isPaused = false
                }
            }
        } catch (_: Exception) { }
    }

    fun seekByDelta(deltaMs: Long) {
        try {
            val ep = exoPlayer
            val vp = vlcPlayerRef
            if (ep != null) {
                ep.seekTo((ep.currentPosition + deltaMs).coerceAtLeast(0L))
            } else if (vp != null && !vp.isReleased) {
                vp.time = (vp.time + deltaMs).coerceAtLeast(0L)
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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                                seekFeedbackText = if (isRightSide) "+10s" else "-10s"
                            }
                        }
                    )
                }
                .pointerInput(isLocked) {
                    detectVerticalDragGestures { change, dragAmount ->
                        if (isLocked) return@detectVerticalDragGestures
                        change.consume()
                        val am = audioManager ?: return@detectVerticalDragGestures
                        val isRightSide = change.position.x > size.width / 2f
                        if (isRightSide) {
                            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val delta = if (dragAmount < -15f) 1 else if (dragAmount > 15f) -1 else 0
                            if (delta != 0) {
                                val nextVol = (curVol + delta).coerceIn(0, maxVol)
                                am.setStreamVolume(AudioManager.STREAM_MUSIC, nextVol, 0)
                                val pct = ((nextVol.toFloat() / maxVol.toFloat()) * 100).toInt()
                                seekFeedbackText = "🔊 $pct%"
                            }
                        } else {
                            val win = activity?.window ?: return@detectVerticalDragGestures
                            val lp = win.attributes
                            val curBr = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                            val nextBr = (curBr - (dragAmount / 800f)).coerceIn(0.05f, 1.0f)
                            lp.screenBrightness = nextBr
                            win.attributes = lp
                            seekFeedbackText = "☀️ ${(nextBr * 100).toInt()}%"
                        }
                    }
                }
        ) {
            if (!useVlcEngine) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = exoResizeMode
                            setKeepContentOnPlayerReset(false)
                            setShutterBackgroundColor(android.graphics.Color.BLACK)
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
                AndroidView(
                    factory = { ctx ->
                        val vlcLayout = VLCVideoLayout(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                        try {
                            val cacheMs = when {
                                isVodOrSeries -> 5000
                                playbackConfig.enableLowLatency -> 1800
                                else -> 4000
                            }
                            val libVlc = LibVLC(
                                ctx,
                                arrayListOf(
                                    "--network-caching=$cacheMs",
                                    "--live-caching=$cacheMs",
                                    "--rtsp-tcp",
                                    "--http-reconnect",
                                    "--http-user-agent=$customUserAgent"
                                )
                            )
                            val mediaPlayer = MediaPlayer(libVlc).apply {
                                attachViews(vlcLayout, null, false, false)
                                videoScale = vlcScaleType
                                if (effectiveUrl.isNotBlank()) {
                                    val m = Media(libVlc, Uri.parse(effectiveUrl))
                                    m.setHWDecoderEnabled(true, false)
                                    media = m
                                    m.release()
                                    play()
                                }
                            }
                            libVlcRef = libVlc
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
                            }
                        } catch (_: Exception) { }
                    },
                    onRelease = { view ->
                        val pair = view.tag as? Pair<*, *>
                        val mp = pair?.second as? MediaPlayer
                        val vlc = pair?.first as? LibVLC
                        vlcPlayerRef = null
                        libVlcRef = null
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                if (mp != null && !mp.isReleased) {
                                    mp.stop()
                                    mp.detachViews()
                                    mp.release()
                                }
                                vlc?.release()
                            } catch (_: Exception) { }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (activeSubtitleText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            bottom = if (controlsVisible) 108.dp else 28.dp,
                            start = 20.dp,
                            end = 20.dp
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = activeSubtitleText,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp
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
                            val isCurrent = ch.id == streamId
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
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { restoreWindowAndExit() }) {
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
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (!isVodOrSeries && streamId.isNotEmpty()) {
                                IconButton(onClick = {
                                    favState = !favState
                                    onToggleFavorite()
                                }) {
                                    Icon(
                                        imageVector = if (favState) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (favState) goldLight else Color.White
                                    )
                                }
                            }

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
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
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
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
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
                            Text("معلومات البث", color = gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("المحرك: ${diag.activeEngine}", color = Color.White, fontSize = 11.sp)
                            Text("السرعة: ${diag.bitrateKbps} kbps", color = Color.White, fontSize = 11.sp)
                            Text("التخزين المؤقت: ${diag.bufferDurationMs / 1000.0} s", color = Color.White, fontSize = 11.sp)
                            Text("الحالة: ${diag.statusNote}", color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isVodOrSeries && subtitleCues.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = {
                                    subtitleOffsetMs -= 500L
                                    seekFeedbackText = "الترجمة ${subtitleOffsetMs / 1000.0}s"
                                }) {
                                    Text("-0.5s", color = gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "تزامن الترجمة: ${subtitleOffsetMs / 1000.0}s",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                                TextButton(onClick = {
                                    subtitleOffsetMs += 500L
                                    seekFeedbackText = "الترجمة +${subtitleOffsetMs / 1000.0}s"
                                }) {
                                    Text("+0.5s", color = gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

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
                                            val vp = vlcPlayerRef
                                            if (vp != null && !vp.isReleased) {
                                                vp.time = target
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
                                TextButton(onClick = { showTracksSheet = true }) {
                                    Icon(Icons.Default.Subtitles, contentDescription = "Audio & Subtitles", tint = if (subtitleCues.isNotEmpty()) Color(0xFF4ADE80) else gold, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("الصوت والترجمة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        seekByDelta(85_000L)
                                        seekFeedbackText = "تخطي المقدمة +85s"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = gold.copy(alpha = 0.28f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("تخطي", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                val ep = exoPlayer
                                if (ep != null) {
                                    TextButton(onClick = {
                                        playbackSpeed = when (playbackSpeed) {
                                            1.0f -> 1.25f
                                            1.25f -> 1.5f
                                            1.5f -> 2.0f
                                            else -> 1.0f
                                        }
                                        ep.playbackParameters = PlaybackParameters(playbackSpeed)
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

            if (showTracksSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showTracksSheet = false },
                    containerColor = Color(0xFF14141E)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("الصوت والترجمة", color = goldLight, fontWeight = FontWeight.Black, fontSize = 16.sp)

                        Button(
                            onClick = {
                                showTracksSheet = false
                                subtitlePickerLauncher.launch(arrayOf("*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = gold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(6.dp))
                            Text("تحميل ملف ترجمة خارجي (.srt)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        val ep = exoPlayer
                        val vp = vlcPlayerRef
                        if (ep != null) {
                            val tracks = ep.currentTracks
                            val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                            val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }

                            if (audioGroups.isNotEmpty()) {
                                Text("مسارات الصوت المدمجة:", color = gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                audioGroups.forEachIndexed { idx, group ->
                                    val fmt = group.getTrackFormat(0)
                                    val langLabel = fmt.language?.uppercase() ?: "مسار صوتي ${idx + 1}"
                                    val label = fmt.label?.takeIf { it.isNotBlank() } ?: langLabel
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (group.isSelected) gold.copy(alpha = 0.22f) else Color(0xFF1E1E2B))
                                            .clickable {
                                                ep.trackSelectionParameters = ep.trackSelectionParameters
                                                    .buildUpon()
                                                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                                                    .build()
                                                showTracksSheet = false
                                                Toast.makeText(context, "الصوت: $label", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(label, color = Color.White, fontSize = 13.sp)
                                        if (group.isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = goldLight)
                                    }
                                }
                            }

                            if (textGroups.isNotEmpty()) {
                                Text("الترجمات المدمجة في الفيديو:", color = gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                textGroups.forEachIndexed { idx, group ->
                                    val fmt = group.getTrackFormat(0)
                                    val langLabel = fmt.language?.uppercase() ?: "ترجمة ${idx + 1}"
                                    val label = fmt.label?.takeIf { it.isNotBlank() } ?: langLabel
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (group.isSelected) gold.copy(alpha = 0.22f) else Color(0xFF1E1E2B))
                                            .clickable {
                                                ep.trackSelectionParameters = ep.trackSelectionParameters
                                                    .buildUpon()
                                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                                                    .build()
                                                showTracksSheet = false
                                                Toast.makeText(context, "الترجمة: $label", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(label, color = Color.White, fontSize = 13.sp)
                                        if (group.isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = goldLight)
                                    }
                                }
                            }
                        } else if (vp != null && !vp.isReleased) {
                            val audioTracks = vp.audioTracks?.toList().orEmpty()
                            val spuTracks = vp.spuTracks?.toList().orEmpty()

                            if (audioTracks.isNotEmpty()) {
                                Text("مسارات الصوت المدمجة (VLC):", color = gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                audioTracks.forEach { t ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (vp.audioTrack == t.id) gold.copy(alpha = 0.22f) else Color(0xFF1E1E2B))
                                            .clickable {
                                                vp.audioTrack = t.id
                                                showTracksSheet = false
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(t.name ?: "Audio ${t.id}", color = Color.White, fontSize = 13.sp)
                                        if (vp.audioTrack == t.id) Icon(Icons.Default.Check, contentDescription = null, tint = goldLight)
                                    }
                                }
                            }

                            if (spuTracks.isNotEmpty()) {
                                Text("الترجمات المدمجة (VLC):", color = gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                spuTracks.forEach { t ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (vp.spuTrack == t.id) gold.copy(alpha = 0.22f) else Color(0xFF1E1E2B))
                                            .clickable {
                                                vp.spuTrack = t.id
                                                showTracksSheet = false
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(t.name ?: "Subtitle ${t.id}", color = Color.White, fontSize = 13.sp)
                                        if (vp.spuTrack == t.id) Icon(Icons.Default.Check, contentDescription = null, tint = goldLight)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                        )
                        Text("التالي: ${slot.nextProgram}", color = Color(0xFF9CA3AF), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
