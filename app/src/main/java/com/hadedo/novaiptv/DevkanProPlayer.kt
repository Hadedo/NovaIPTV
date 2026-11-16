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
import android.widget.FrameLayout
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

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

// ============================================================================
// 1. شاشة إخلاء المسؤولية القانونية
// ============================================================================
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

// ============================================================================
// 2. شاشة قفل التطبيق برمز PIN
// ============================================================================
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

// ============================================================================
// 3. المشغل السينمائي الهجين (تدوير بالعرض + تكبير الشاشة + تخطي المقدمة + حفظ النسبة)
// ============================================================================
@OptIn(UnstableApi::class)
@Composable
fun DevkanHybridPlayerScreen(
    streamUrl: String,
    fallbackUrl: String = "",
    streamTitle: String,
    isVodOrSeries: Boolean = false,
    streamId: String = "",
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

    var currentUrl by remember(streamUrl) { mutableStateOf(streamUrl) }
    var triedFallback by remember(streamUrl) { mutableStateOf(false) }
    var activeEngine by remember(streamUrl) {
        mutableStateOf(EngineCoordinator.selectInitialEngine(streamUrl, preferredEngine))
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var scaleIndex by remember { mutableIntStateOf(1) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var reconnectAttempts by remember { mutableIntStateOf(0) }
    var isReconnecting by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }

    val startTimestamp = remember { System.currentTimeMillis() }
    var startupMs by remember { mutableLongStateOf(0L) }

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

    LaunchedEffect(controlsVisible, isLocked) {
        if (controlsVisible && !isLocked) {
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

    val exoPlayer = remember(currentUrl, activeEngine) {
        if (activeEngine == EngineType.MEDIA3_ONLY) {
            try {
                val (minBuf, maxBuf, playBuf) = EngineCoordinator.getAdaptiveBufferMs(lowLatency)
                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(minBuf, maxBuf, playBuf, playBuf + 250)
                    .build()
                val headers = Engine
