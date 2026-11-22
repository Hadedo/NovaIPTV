package com.hadedo.novaiptv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

// ---------------------------------------------------------------------------
// إعدادات المشغّل الموحّدة (مصدر واحد للحقيقة بدل القيم المبعثرة داخل الدالة)
// ---------------------------------------------------------------------------
private const val DVK_CONNECT_TIMEOUT_MS = 12_000
private const val DVK_READ_TIMEOUT_MS = 30_000
private const val DVK_LIVE_STALL_TIMEOUT_MS = 20_000L
private const val DVK_LIVE_MAX_RETRIES = 5
private const val DVK_VOD_MAX_RETRIES = 3
private const val DVK_MAX_BACKOFF_MS = 5_000L
private const val DVK_DEFAULT_USER_AGENT = "IPTVSmartersPro/3.1.5"

private data class DvkBufferProfile(
    val minMs: Int,
    val maxMs: Int,
    val startMs: Int,
    val rebufferMs: Int
)

/**
 * أولوية واحدة واضحة: VOD ثم Low Latency ثم ECO/LOW ثم الافتراضي.
 * الحد الأدنى للبث المباشر 10 ثوانٍ، وللـ VOD 20 ثانية.
 */
private fun dvkBufferProfile(
    isVod: Boolean,
    lowLatency: Boolean,
    performanceName: String
): DvkBufferProfile = when {
    isVod -> DvkBufferProfile(20_000, 60_000, 2_500, 5_000)
    lowLatency -> DvkBufferProfile(10_000, 30_000, 2_000, 4_000)
    performanceName.contains("ECO", ignoreCase = true) ||
        performanceName.contains("LOW", ignoreCase = true) -> DvkBufferProfile(10_000, 28_000, 2_000, 4_000)
    else -> DvkBufferProfile(15_000, 45_000, 2_500, 5_000)
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun formatCleanTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
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
            .statusBarsPadding()
            .navigationBarsPadding()
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
    val maxLen = maxOf(6, expectedPin.length)
    val numericPin = remember(expectedPin) { expectedPin.all { it.isDigit() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070A))
            .statusBarsPadding()
            .navigationBarsPadding()
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
                    if (it.length <= maxLen) {
                        enteredPin = it
                        isError = false
                        if (it == expectedPin) onUnlocked()
                    }
                },
                singleLine = true,
                label = { Text("PIN") },
                isError = isError,
                supportingText = { if (isError) Text("PIN خاطئ / Wrong PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (numericPin) KeyboardType.NumberPassword else KeyboardType.Password
                ),
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
    initialSeekMs: Long = 0L,
    seriesId: String = "",
    seasonNum: Int = 1,
    episodeNum: Int = 0,
    subId: String = "default",
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    quickChannels: List<MediaItemModel> = emptyList(),
    onSwitchChannel: (MediaItemModel) -> Unit = {},
    onNextEpisode: (() -> Unit)? = null,
    lang: String = "ar",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val accent = Color(0xFFEAB308)
    val surfaceDark = Color(0xFF101218)

    val maxRetries = if (isVodOrSeries) DVK_VOD_MAX_RETRIES else DVK_LIVE_MAX_RETRIES

    val latestOnNextEpisode by rememberUpdatedState(onNextEpisode)
    val latestOnBack by rememberUpdatedState(onBack)

    val userAgent = remember(context) { DevkanVault.getUserAgent(context) }
    val formatMode = remember(context) { DevkanVault.getStreamFormatMode(context) }
    val performanceMode = remember(context) { DevkanVault.getPerformanceMode(context) }
    val lowLatency = remember(context) { DevkanVault.isLowLatencyEnabled(context) }

    val (resolvedPrimary, resolvedFallback) = remember(streamUrl, fallbackUrl, formatMode, isVodOrSeries) {
        if (isVodOrSeries) Pair(streamUrl.trim(), fallbackUrl.trim())
        else EngineCoordinator.resolveLiveStreamUrls(streamUrl.trim(), fallbackUrl.trim(), formatMode)
    }

    var currentUrl by remember(resolvedPrimary, streamId) { mutableStateOf(resolvedPrimary) }
    var fallbackUsed by remember(resolvedPrimary, streamId) { mutableStateOf(false) }
    var retryAttempts by remember(resolvedPrimary, streamId) { mutableIntStateOf(0) }
    var reloadTick by remember(resolvedPrimary, streamId) { mutableIntStateOf(0) }
    var resumeFromMs by remember(resolvedPrimary, streamId) { mutableLongStateOf(0L) }
    var seekApplied by remember(streamId, initialSeekMs) { mutableStateOf(false) }

    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var showChannelDrawer by remember { mutableStateOf(false) }
    var resizeIndex by remember { mutableIntStateOf(0) }
    var statusBadge by remember { mutableStateOf<String?>(null) }

    var positionMs by remember(streamId) { mutableLongStateOf(initialSeekMs.coerceAtLeast(0L)) }
    var durationMs by remember(streamId) { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val drawerListState = rememberLazyListState()

    val effectiveUa = remember(userAgent) {
        userAgent.trim().ifBlank { DVK_DEFAULT_USER_AGENT }
    }

    val loadControl = remember(isVodOrSeries, performanceMode.name, lowLatency) {
        val p = dvkBufferProfile(isVodOrSeries, lowLatency, performanceMode.name)
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(p.minMs, p.maxMs, p.startMs, p.rebufferMs)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    val exoPlayer = remember(effectiveUa, loadControl) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(effectiveUa)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(DVK_CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(DVK_READ_TIMEOUT_MS)

        val extractorsFactory = DefaultExtractorsFactory()
            .setTsExtractorFlags(
                DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                    DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
            )

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory))
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    fun persistCurrentVodProgress() {
        if (!isVodOrSeries || streamId.isBlank()) return
        try {
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration
            if (pos > 3_000L && dur != C.TIME_UNSET && dur > 0L) {
                DevkanVault.saveVodProgress(
                    context = context,
                    streamId = streamId,
                    posMs = pos,
                    durMs = dur,
                    seriesId = seriesId,
                    episodeNum = episodeNum,
                    seasonNum = seasonNum
                )
            }
        } catch (_: Exception) { }
    }
    val persistProgress by rememberUpdatedState { persistCurrentVodProgress() }

    fun setPortraitOrientationKeepPlaying() {
        try {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (_: Exception) { }
    }

    fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val act = activity ?: return
        try {
            if (!act.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
            val vs = exoPlayer.videoSize
            val ratioValue = if (vs.width > 0 && vs.height > 0) {
                (vs.width.toFloat() / vs.height.toFloat()).coerceIn(0.42f, 2.39f)
            } else 16f / 9f
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational((ratioValue * 1000).toInt(), 1000))
                .build()
            controlsVisible = false
            act.enterPictureInPictureMode(params)
        } catch (_: Exception) { }
    }

    fun reloadStream(urlToLoad: String, startPositionMs: Long = 0L) {
        if (urlToLoad.isBlank()) return
        try {
            val builder = MediaItem.Builder().setUri(urlToLoad)
            if (!isVodOrSeries) {
                builder.setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setMinPlaybackSpeed(1f)
                        .setMaxPlaybackSpeed(1f)
                        .build()
                )
            }
            val item = builder.build()
            if (startPositionMs > 0L) exoPlayer.setMediaItem(item, startPositionMs)
            else exoPlayer.setMediaItem(item)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (_: Exception) { }
    }

    fun seekByMs(deltaMs: Long) {
        val dur = exoPlayer.duration
        val raw = exoPlayer.currentPosition + deltaMs
        val target = if (dur != C.TIME_UNSET && dur > 0L) raw.coerceIn(0L, dur) else raw.coerceAtLeast(0L)
        exoPlayer.seekTo(target)
        statusBadge = if (deltaMs >= 0) "+${deltaMs / 1000}s" else "${deltaMs / 1000}s"
    }

    fun exitPlayerCleanly() {
        persistCurrentVodProgress()
        try {
            exoPlayer.playWhenReady = false
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            val act = activity
            if (act != null) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val win = act.window
                WindowInsetsControllerCompat(win, win.decorView).show(WindowInsetsCompat.Type.systemBars())
                win.decorView.requestApplyInsets()
            }
        } catch (_: Exception) { }
        latestOnBack()
    }

    BackHandler {
        when {
            isLocked -> {
                controlsVisible = true
                statusBadge = "🔒"
            }
            showChannelDrawer -> showChannelDrawer = false
            isLandscape -> setPortraitOrientationKeepPlaying()
            else -> exitPlayerCleanly()
        }
    }

    LaunchedEffect(showChannelDrawer, streamId, quickChannels) {
        if (showChannelDrawer && quickChannels.isNotEmpty()) {
            val idx = quickChannels.indexOfFirst { it.id == streamId }.coerceAtLeast(0)
            try {
                drawerListState.scrollToItem(idx)
            } catch (_: Exception) { }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            persistProgress()
            try {
                exoPlayer.release()
            } catch (_: Exception) { }
        }
    }

    DisposableEffect(exoPlayer, streamId) {
        onDispose {
            persistCurrentVodProgress()
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
            } catch (_: Exception) { }
        }
    }

    DisposableEffect(activity) {
        val act = activity
        val win = act?.window
        val originalBrightness = win?.attributes?.screenBrightness
            ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        if (win != null) {
            try {
                win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val controller = WindowInsetsControllerCompat(win, win.decorView)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } catch (_: Exception) { }
        }
        onDispose {
            try {
                if (win != null) {
                    win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    val lp = win.attributes
                    lp.screenBrightness = originalBrightness
                    win.attributes = lp
                    WindowInsetsControllerCompat(win, win.decorView).show(WindowInsetsCompat.Type.systemBars())
                    win.decorView.requestApplyInsets()
                }
            } catch (_: Exception) { }
        }
    }

    DisposableEffect(activity, exoPlayer, isVodOrSeries) {
        val owner = activity as? LifecycleOwner
        var pausedByLifecycle = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    persistProgress()
                    val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                        activity?.isInPictureInPictureMode == true
                    if (!inPip && exoPlayer.playWhenReady) {
                        exoPlayer.pause()
                        pausedByLifecycle = true
                    }
                }
                Lifecycle.Event.ON_START -> {
                    if (pausedByLifecycle) {
                        pausedByLifecycle = false
                        if (!isVodOrSeries) exoPlayer.seekToDefaultPosition()
                        exoPlayer.play()
                    }
                }
                else -> Unit
            }
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }

    DisposableEffect(context, exoPlayer, isVodOrSeries) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val mainHandler = Handler(Looper.getMainLooper())
        var disposed = false
        var callback: ConnectivityManager.NetworkCallback? = null
        if (cm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    mainHandler.post {
                        if (disposed || errorMessage == null) return@post
                        if (isVodOrSeries) {
                            resumeFromMs = try {
                                exoPlayer.currentPosition.coerceAtLeast(0L)
                            } catch (_: Exception) { 0L }
                        }
                        errorMessage = null
                        retryAttempts = 0
                        isBuffering = true
                        reloadTick++
                    }
                }
            }
            try {
                cm.registerDefaultNetworkCallback(callback)
            } catch (_: Exception) {
                callback = null
            }
        }
        onDispose {
            disposed = true
            mainHandler.removeCallbacksAndMessages(null)
            try {
                val cb = callback
                if (cm != null && cb != null) cm.unregisterNetworkCallback(cb)
            } catch (_: Exception) { }
        }
    }

    DisposableEffect(exoPlayer, currentUrl, resolvedFallback, initialSeekMs, isVodOrSeries, maxRetries) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        errorMessage = null
                        retryAttempts = 0
                        if (isVodOrSeries && initialSeekMs > 3_000L && !seekApplied) {
                            seekApplied = true
                            val dur = exoPlayer.duration
                            val target = if (dur != C.TIME_UNSET && dur > 0L) {
                                initialSeekMs.coerceAtMost((dur - 2_000L).coerceAtLeast(0L))
                            } else initialSeekMs
                            exoPlayer.seekTo(target)
                        }
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        persistProgress()
                        val next = latestOnNextEpisode
                        if (isVodOrSeries && next != null) {
                            next()
                        } else if (isVodOrSeries) {
                            controlsVisible = true
                        } else if (currentUrl.isNotBlank() && retryAttempts < maxRetries) {
                            retryAttempts++
                            reloadTick++
                        } else if (!isVodOrSeries) {
                            errorMessage = "انقطع البث. تحقق من الرابط أو الشبكة."
                        }
                    }
                    Player.STATE_IDLE -> isBuffering = false
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    isBuffering = false
                    errorMessage = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    try {
                        exoPlayer.seekToDefaultPosition()
                        exoPlayer.prepare()
                    } catch (_: Exception) { }
                    return
                }
                if (isVodOrSeries) {
                    resumeFromMs = try {
                        exoPlayer.currentPosition.coerceAtLeast(0L)
                    } catch (_: Exception) { 0L }
                }
                if (!fallbackUsed && resolvedFallback.isNotBlank() && resolvedFallback != currentUrl) {
                    fallbackUsed = true
                    retryAttempts = 0
                    errorMessage = null
                    statusBadge = "تحويل للرابط البديل..."
                    currentUrl = resolvedFallback
                } else if (retryAttempts < maxRetries && currentUrl.isNotBlank()) {
                    retryAttempts++
                    errorMessage = null
                    reloadTick++
                } else {
                    errorMessage = "تعذر تشغيل البث حالياً. تحقق من الرابط أو الشبكة."
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(currentUrl, reloadTick) {
        if (currentUrl.isBlank()) {
            isBuffering = false
            errorMessage = "رابط البث فارغ."
            return@LaunchedEffect
        }
        if (reloadTick > 0 && retryAttempts > 0) {
            delay((1_000L * retryAttempts).coerceAtMost(DVK_MAX_BACKOFF_MS))
        }
        errorMessage = null
        isBuffering = true
        val resume = if (isVodOrSeries) resumeFromMs else 0L
        resumeFromMs = 0L
        if (resume > 3_000L) seekApplied = true
        reloadStream(currentUrl, resume)
    }

    LaunchedEffect(isBuffering, reloadTick, currentUrl, isVodOrSeries) {
        if (isVodOrSeries || !isBuffering || errorMessage != null || currentUrl.isBlank()) {
            return@LaunchedEffect
        }
        delay(DVK_LIVE_STALL_TIMEOUT_MS)
        if (!isBuffering || errorMessage != null) return@LaunchedEffect
        if (retryAttempts < maxRetries) {
            retryAttempts++
            reloadTick++
        } else {
            isBuffering = false
            errorMessage = "تعذر تشغيل البث حالياً. تحقق من الرابط أو الشبكة."
        }
    }

    LaunchedEffect(controlsVisible, isLocked, isPlaying, showChannelDrawer, errorMessage, isDragging) {
        if (controlsVisible && isPlaying && !showChannelDrawer && errorMessage == null && !isDragging) {
            delay(if (isLocked) 2_500L else 4_500L)
            controlsVisible = false
        }
    }

    LaunchedEffect(statusBadge) {
        if (statusBadge != null) {
            delay(1500)
            statusBadge = null
        }
    }

    LaunchedEffect(isVodOrSeries, streamId) {
        if (!isVodOrSeries) return@LaunchedEffect
        var ticks = 0
        while (true) {
            delay(500)
            try {
                val dur = exoPlayer.duration
                if (dur != C.TIME_UNSET && dur > 0L && dur != durationMs) durationMs = dur
                if (controlsVisible && !isDragging) {
                    positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                }
                ticks++
                if (ticks >= 20) {
                    ticks = 0
                    persistProgress()
                }
            } catch (_: Exception) { }
        }
    }

    val videoResizeMode = when (resizeIndex) {
        1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    val resizeLabel = when (resizeIndex) {
        1 -> "ملء"
        2 -> "تكبير"
        else -> "ملائم"
    }

    val showCenterPlaybackControls = controlsVisible && !isLocked && !showChannelDrawer && errorMessage == null && !isBuffering

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(isLocked) {
                    detectTapGestures(
                        onTap = {
                            if (showChannelDrawer) showChannelDrawer = false
                            else controlsVisible = !controlsVisible
                        },
                        onDoubleTap = { offset ->
                            if (!isLocked && isVodOrSeries) {
                                val forward = offset.x > size.width / 2f
                                seekByMs(if (forward) 10_000L else -10_000L)
                            }
                        }
                    )
                }
                .pointerInput(isLocked) {
                    var volAccum = 0f
                    detectVerticalDragGestures(
                        onDragStart = { volAccum = 0f },
                        onDragEnd = { },
                        onDragCancel = { }
                    ) { change, dragAmount ->
                        if (isLocked) return@detectVerticalDragGestures
                        change.consume()
                        val rightSide = change.position.x > size.width / 2f
                        if (rightSide) {
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            volAccum += -dragAmount
                            val perStep = size.height / (maxVol.coerceAtLeast(1) * 1.2f)
                            val steps = (volAccum / perStep).toInt()
                            if (steps != 0) {
                                volAccum -= steps * perStep
                                val nextVol = (curVol + steps).coerceIn(0, maxVol)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nextVol, 0)
                                val pct = ((nextVol.toFloat() / maxVol.coerceAtLeast(1).toFloat()) * 100).toInt()
                                statusBadge = "🔊 $pct%"
                            }
                        } else {
                            val win = activity?.window ?: return@detectVerticalDragGestures
                            val lp = win.attributes
                            val curBr = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                            val nextBr = (curBr - (dragAmount / size.height.toFloat())).coerceIn(0.05f, 1.0f)
                            lp.screenBrightness = nextBr
                            win.attributes = lp
                            statusBadge = "☀️ ${(nextBr * 100).toInt()}%"
                        }
                    }
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        this.resizeMode = videoResizeMode
                        setKeepContentOnPlayerReset(false)
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) view.player = exoPlayer
                    if (view.resizeMode != videoResizeMode) view.resizeMode = videoResizeMode
                },
                onRelease = { view -> view.player = null },
                modifier = Modifier.fillMaxSize()
            )

            if (isBuffering && errorMessage == null && !showChannelDrawer) {
                CircularProgressIndicator(
                    color = accent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            statusBadge?.let { badge ->
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text = badge, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            errorMessage?.let { msg ->
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(surfaceDark.copy(alpha = 0.96f))
                        .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = accent, modifier = Modifier.size(36.dp))
                    Text(msg, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                errorMessage = null
                                retryAttempts = 0
                                isBuffering = true
                                if (isVodOrSeries) {
                                    resumeFromMs = try {
                                        exoPlayer.currentPosition.coerceAtLeast(0L)
                                    } catch (_: Exception) { 0L }
                                }
                                reloadTick++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("إعادة المحاولة", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { exitPlayerCleanly() }, shape = RoundedCornerShape(10.dp)) {
                            Text("رجوع", color = Color.White)
                        }
                    }
                }
            }

            if (isLocked && controlsVisible) {
                IconButton(
                    onClick = { isLocked = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = "Unlock", tint = accent)
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
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(onClick = {
                                if (showChannelDrawer) showChannelDrawer = false
                                else if (isLandscape) setPortraitOrientationKeepPlaying()
                                else exitPlayerCleanly()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Text(
                                text = streamTitle,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                IconButton(onClick = { enterPictureInPicture() }) {
                                    Icon(Icons.Default.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White)
                                }
                            }
                            if (!isVodOrSeries && quickChannels.isNotEmpty()) {
                                IconButton(onClick = { showChannelDrawer = !showChannelDrawer }) {
                                    Icon(Icons.Default.List, contentDescription = "Channels", tint = accent)
                                }
                            }
                            IconButton(onClick = { isLocked = true }) {
                                Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color.White)
                            }
                        }
                    }

                    if (showCenterPlaybackControls) {
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            if (isVodOrSeries) {
                                IconButton(
                                    onClick = { seekByMs(-10_000L) },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White)
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (exoPlayer.playWhenReady && exoPlayer.playbackState != Player.STATE_ENDED) {
                                        exoPlayer.pause()
                                    } else {
                                        if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekToDefaultPosition()
                                        exoPlayer.play()
                                    }
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(accent, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            if (isVodOrSeries) {
                                IconButton(
                                    onClick = { seekByMs(10_000L) },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White)
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isVodOrSeries && durationMs > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(formatCleanTime(positionMs), color = Color.White, fontSize = 11.sp)
                                Slider(
                                    value = if (isDragging) dragFraction
                                    else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
                                    onValueChange = { fraction ->
                                        isDragging = true
                                        dragFraction = fraction
                                        positionMs = (fraction * durationMs).toLong()
                                    },
                                    onValueChangeFinished = {
                                        exoPlayer.seekTo((dragFraction * durationMs).toLong())
                                        isDragging = false
                                    },
                                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(formatCleanTime(durationMs), color = Color.White, fontSize = 11.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
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
                                Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isLandscape) "طولي" else "عرضي", color = Color.White, fontSize = 12.sp)
                            }

                            TextButton(onClick = { resizeIndex = (resizeIndex + 1) % 3 }) {
                                Icon(Icons.Default.AspectRatio, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(resizeLabel, color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (showChannelDrawer && quickChannels.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(275.dp)
                        .background(surfaceDark.copy(alpha = 0.97f))
                        .border(1.dp, accent.copy(alpha = 0.35f))
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("القنوات", color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        IconButton(onClick = { showChannelDrawer = false }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    LazyColumn(
                        state = drawerListState,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(quickChannels, key = { _, ch -> ch.id }) { _, ch ->
                            val selected = ch.id == streamId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) accent.copy(alpha = 0.22f) else Color(0xFF1A1D26))
                                    .clickable {
                                        showChannelDrawer = false
                                        onSwitchChannel(ch)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ch.title,
                                    color = if (selected) accent else Color.White,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
    var filtered by remember { mutableStateOf(slots.take(200)) }

    LaunchedEffect(slots, searchQuery) {
        if (searchQuery.isBlank()) {
            filtered = withContext(Dispatchers.Default) { slots.take(200) }
        } else {
            delay(250)
            val q = searchQuery
            filtered = withContext(Dispatchers.Default) {
                slots.asSequence()
                    .filter {
                        it.channelName.contains(q, ignoreCase = true) ||
                            it.currentProgram.contains(q, ignoreCase = true)
                    }
                    .take(200)
                    .toList()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0A14))
            .statusBarsPadding()
            .navigationBarsPadding()
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
            itemsIndexed(filtered) { _, slot ->
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
                            progress = { slot.progress.coerceIn(0f, 1f) },
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
