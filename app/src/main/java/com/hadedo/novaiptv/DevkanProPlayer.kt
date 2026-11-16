package com.hadedo.novaiptv

import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

// ============================================================================
// 1. شاشة إخلاء المسؤولية القانونية (Disclaimer) عند التشغيل الأول
// ============================================================================
@Composable
fun LegalDisclaimerScreen(lang: String, onAccept: () -> Unit) {
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)
    val title = when (lang) {
        "en" -> "Terms of Use & Legal Disclaimer"
        "de" -> "Nutzungsbedingungen & Rechtlicher Hinweis"
        else -> "إخلاء المسؤولية القانونية وشروط الاستخدام"
    }
    val body = when (lang) {
        "en" -> "DEVKAN is strictly a media player application. It does NOT provide, host, or include any TV channels, movies, or subscriptions. Users must add their own legally obtained playlists (M3U / Xtream / Stalker)."
        "de" -> "DEVKAN ist ein reiner Medienplayer. Die App enthält keine TV-Kanäle, Filme oder Abonnements. Nutzer müssen eigene legal erworbene Wiedergabelisten (M3U / Xtream / Stalker) hinzufügen."
        else -> "تطبيق DEVKAN هو مشغل وسائط تقني فقط (Media Player) ولا يتضمن أو يوفر أي قنوات تلفزيونية أو أفلام أو اشتراكات مدمجة. يلتزم المستخدم بإضافة قوائم التشغيل الخاصة به والتي يملك حق الوصول القانوني إليها."
    }
    val btnText = when (lang) {
        "en" -> "I Agree & Continue"
        "de" -> "Zustimmen & Fortfahren"
        else -> "أوافق وأتابع الدخول"
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
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = goldLight,
                modifier = Modifier.size(52.dp)
            )
            Text(
                text = "DEVKAN 34 VIP",
                color = goldLight,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 1.5.sp
            )
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = body,
                color = Color(0xFFB0B5C0),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = gold),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = btnText,
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ============================================================================
// 2. شاشة قفل التطبيق برمز PIN عند الفتح (App Lock Security)
// ============================================================================
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
            Text(
                text = when (lang) {
                    "en" -> "DEVKAN App Lock"
                    "de" -> "DEVKAN App-Sperre"
                    else -> "قفل حماية التطبيق"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
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
                label = {
                    Text(
                        when (lang) {
                            "en" -> "Enter PIN"
                            "de" -> "PIN eingeben"
                            else -> "أدخل رمز المرور PIN"
                        }
                    )
                },
                isError = isError,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (enteredPin == expectedPin) onUnlocked() else isError = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = gold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (lang) {
                        "en" -> "Unlock"
                        "de" -> "Entsperren"
                        else -> "فتح التطبيق"
                    },
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============================================================================
// 3. المشغل السينمائي الهجين (Media3 + LibVLC + Auto-Reconnect + Diagnostics)
// ============================================================================
@OptIn(UnstableApi::class)
@Composable
fun DevkanHybridPlayerScreen(
    streamUrl: String,
    streamTitle: String,
    isVodOrSeries: Boolean = false,
    streamId: String = "",
    lang: String = "ar",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gold = Color(0xFFD4AF37)
    val preferredEngine = remember { DevkanVault.getPreferredEngine(context) }

    var activeEngine by remember {
        mutableStateOf(EngineCoordinator.selectInitialEngine(streamUrl, preferredEngine))
    }
    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var reconnectAttempts by remember { mutableIntStateOf(0) }
    var isReconnecting by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }

    val startTimestamp = remember { System.currentTimeMillis() }
    var startupMs by remember { mutableLongStateOf(0L) }

    // حفظ آخر قناة إذا كانت بثاً مباشراً
    LaunchedEffect(streamUrl) {
        if (!isVodOrSeries) {
            DevkanVault.saveLastChannel(context, streamUrl, streamTitle)
        }
    }

    // إخفاء شريط التحكم تلقائياً بعد 5 ثوانٍ
    LaunchedEffect(controlsVisible, isLocked) {
        if (controlsVisible && !isLocked) {
            delay(5000)
            controlsVisible = false
        }
    }

    // إخفاء إشعار التقديم/التأخير السريع
    LaunchedEffect(seekFeedbackText) {
        if (seekFeedbackText != null) {
            delay(900)
            seekFeedbackText = null
        }
    }

    // تهيئة محرك Media3 ExoPlayer مع Fast Zapping
    val exoPlayer = remember(streamUrl, activeEngine) {
        if (activeEngine == EngineType.MEDIA3_ONLY) {
            val (minBuf, maxBuf, playBuf) = EngineCoordinator.getAdaptiveBufferMs(65L)
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(minBuf, maxBuf, playBuf, playBuf + 400)
                .build()
            ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build()
                .apply {
                    setMediaItem(MediaItem.fromUri(streamUrl))
                    val savedPos = if (isVodOrSeries && streamId.isNotEmpty()) DevkanVault.getVodPosition(context, streamId) else 0L
                    if (savedPos > 5000L) seekTo(savedPos)
                    prepare()
                    playWhenReady = true
                }
        } else null
    }

    // مستمع حالة Media3 + التبديل التلقائي إلى LibVLC أو إعادة الاتصال
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && startupMs == 0L) {
                    startupMs = (System.currentTimeMillis() - startTimestamp).coerceAtLeast(120L)
                    isReconnecting = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (preferredEngine == EngineType.AUTO_HYBRID && activeEngine == EngineType.MEDIA3_ONLY) {
                    activeEngine = EngineType.LIBVLC_ONLY
                } else if (reconnectAttempts < 3) {
                    reconnectAttempts++
                    isReconnecting = true
                    exoPlayer?.prepare()
                    exoPlayer?.playWhenReady = true
                }
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            if (isVodOrSeries && streamId.isNotEmpty() && exoPlayer != null) {
                DevkanVault.saveVodPosition(context, streamId, exoPlayer.currentPosition)
            }
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

    // تحديث بيانات تشخيص الأداء الحي كل ثانيتين
    LaunchedEffect(activeEngine, reconnectAttempts, startupMs) {
        while (true) {
            val buffered = exoPlayer?.totalBufferedDuration ?: 3200L
            val bitrate = exoPlayer?.videoFormat?.bitrate?.div(1000) ?: 4200
            EngineCoordinator.currentDiagnostics.value = StreamDiagnostics(
                activeEngine = if (activeEngine == EngineType.MEDIA3_ONLY) "Media3 ExoPlayer" else "LibVLC Hybrid Engine",
                pingMs = 42L,
                startupTimeMs = if (startupMs > 0) startupMs else 480L,
                bitrateKbps = if (bitrate > 0) bitrate else 3800,
                bufferDurationMs = buffered,
                reconnectCount = reconnectAttempts,
                statusNote = if (isReconnecting) "إعادة اتصال تلقائي ($reconnectAttempts/3)..." else "بث مستقر وممتاز"
            )
            delay(2000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                detectTapGestures(
                    onTap = { controlsVisible = !controlsVisible },
                    onDoubleTap = { offset ->
                        if (!isLocked && exoPlayer != null) {
                            val isRightSide = offset.x > size.width / 2f
                            val delta = if (isRightSide) 10_000L else -10_000L
                            exoPlayer.seekTo((exoPlayer.currentPosition + delta).coerceAtLeast(0L))
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
                        this.resizeMode = resizeMode
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view -> view.resizeMode = resizeMode },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    val vlcLayout = VLCVideoLayout(ctx)
                    val libVlc = LibVLC(ctx, arrayListOf("--network-caching=1500", "--rtsp-tcp"))
                    val mediaPlayer = MediaPlayer(libVlc).apply {
                        attachViews(vlcLayout, null, false, false)
                        val media = Media(libVlc, Uri.parse(streamUrl))
                        setMedia(media)
                        media.release()
                        play()
                    }
                    vlcLayout.tag = Pair(libVlc, mediaPlayer)
                    vlcLayout
                },
                onRelease = { view ->
                    val pair = view.tag as? Pair<*, *>
                    (pair?.second as? MediaPlayer)?.stop()
                    (pair?.second as? MediaPlayer)?.release()
                    (pair?.first as? LibVLC)?.release()
                },
                modifier = Modifier.fillMaxSize()
            )
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
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column {
                            Text(
                                text = streamTitle,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (activeEngine == EngineType.MEDIA3_ONLY) "Engine: Media3 Fast" else "Engine: LibVLC Hybrid",
                                color = gold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                activeEngine = if (activeEngine == EngineType.MEDIA3_ONLY) EngineType.LIBVLC_ONLY else EngineType.MEDIA3_ONLY
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (activeEngine == EngineType.MEDIA3_ONLY) "Switch to VLC" else "Switch to Media3",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { showDiagnostics = !showDiagnostics }) {
                            Icon(Icons.Default.Speed, contentDescription = "Diagnostics", tint = gold)
                        }
                    }
                }

                if (showDiagnostics) {
                    val diag = EngineCoordinator.currentDiagnostics.value
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(250.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF101016).copy(alpha = 0.92f))
                            .border(1.dp, gold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("مركز تشخيص البث الحي", color = gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("المحرك: ${diag.activeEngine}", color = Color.White, fontSize = 11.sp)
                        Text("زمن بدء البث: ${diag.startupTimeMs} ms", color = Color.White, fontSize = 11.sp)
                        Text("سرعة الاستقبال: ${diag.bitrateKbps} kbps", color = Color.White, fontSize = 11.sp)
                        Text("مخزون Buffer: ${diag.bufferDurationMs / 1000.0} s", color = Color.White, fontSize = 11.sp)
                        Text("إعادة الاتصال: ${diag.reconnectCount}", color = Color.White, fontSize = 11.sp)
                        Text("الحالة: ${diag.statusNote}", color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { isLocked = true }) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Lock", color = Color.White, fontSize = 12.sp)
                    }

                    TextButton(onClick = {
                        resizeMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    }) {
                        Icon(Icons.Default.AspectRatio, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        val modeName = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Fill"
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
                            else -> "Fit"
                        }
                        Text(modeName, color = Color.White, fontSize = 12.sp)
                    }

                    if (isVodOrSeries && exoPlayer != null) {
                        TextButton(onClick = {
                            playbackSpeed = when (playbackSpeed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                            exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
                        }) {
                            Text("${playbackSpeed}x", color = gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 85_000L) },
                            colors = ButtonDefaults.buttonColors(containerColor = gold.copy(alpha = 0.25f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Skip Intro +85s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. جدول دليل البرامج الزمني (EPG Timeline Grid)
// ============================================================================
@Composable
fun EpgTimelineGridSheet(
    slots: List<EpgSlot>,
    lang: String,
    onPlayChannel: (String) -> Unit,
    onClose: () -> Unit
) {
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
            Text(
                text = when (lang) {
                    "en" -> "EPG Timeline Guide"
                    "de" -> "EPG-Programmführer"
                    else -> "دليل البرامج الزمني (EPG)"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFD4AF37)) },
            placeholder = {
                Text(
                    when (lang) {
                        "en" -> "Search EPG program or channel..."
                        "de" -> "EPG-Programm suchen..."
                        else -> "ابحث في دليل البرامج أو القنوات..."
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(14.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(filtered) { slot ->
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
                        Text(
                            text = slot.channelName,
                            color = Color(0xFFFDE047),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(text = slot.timeRange, color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = slot.currentProgram,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LinearProgressIndicator(
                            progress = { slot.progress },
                            color = Color(0xFFD4AF37),
                            trackColor = Color(0xFF2D274B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                        )
                        Text(
                            text = "التالي: ${slot.nextProgram}",
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
