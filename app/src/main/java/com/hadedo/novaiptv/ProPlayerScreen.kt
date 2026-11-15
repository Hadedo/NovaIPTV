package com.hadedo.novaiptv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.TrafficStats
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.util.TypedValue
import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
private fun IsolatedHardwareVideoSurface(
    exoPlayer: ExoPlayer,
    resizeMode: Int,
    subFontSize: Float,
    subColorYellow: Boolean
) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                this.resizeMode = resizeMode
                setKeepContentOnPlayerReset(true)
                setShutterBackgroundColor(AndroidColor.BLACK)
            }
        },
        update = { view ->
            if (view.resizeMode != resizeMode) {
                view.resizeMode = resizeMode
            }
            view.subtitleView?.apply {
                setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subFontSize)
                setStyle(
                    CaptionStyleCompat(
                        if (subColorYellow) AndroidColor.YELLOW else AndroidColor.WHITE,
                        AndroidColor.argb(155, 0, 0, 0),
                        AndroidColor.TRANSPARENT,
                        CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                        AndroidColor.BLACK,
                        null
                    )
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(UnstableApi::class)
@Composable
fun ProCinemaPlayerOverlay(
    exoPlayer: ExoPlayer,
    trackSelector: DefaultTrackSelector,
    currentMedia: MediaItemModel,
    currentEpg: EpgProgram?,
    videoResolution: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isFullScreen: Boolean,
    isInPipMode: Boolean,
    allLiveChannels: List<MediaItemModel>,
    activeHost: String,
    accentColor: Color,
    subFontSize: Float,
    subColorYellow: Boolean,
    gesturesEnabled: Boolean,
    onToggleFullScreen: () -> Unit,
    onClosePlayer: () -> Unit,
    onSelectChannel: (MediaItemModel) -> Unit,
    onZapChannel: (Int) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val haptics = LocalHapticFeedback.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val greenText = Color(0xFF4ADE80)

    var showOsd by remember { mutableStateOf(true) }
    var osdTick by remember { mutableIntStateOf(0) }
    var isLocked by remember { mutableStateOf(false) }
    var showChannelDrawer by remember { mutableStateOf(false) }
    var showTracksDialog by remember { mutableStateOf(false) }
    var gestureText by remember { mutableStateOf("") }

    var resizeIdx by remember { mutableIntStateOf(0) }
    val resizeModes = listOf(
        Pair(AspectRatioFrameLayout.RESIZE_MODE_FIT, "طبيعي"),
        Pair(AspectRatioFrameLayout.RESIZE_MODE_FILL, "تمدد"),
        Pair(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "سينمائي")
    )

    var posMs by remember { mutableLongStateOf(0L) }
    var durMs by remember { mutableLongStateOf(0L) }
    var netSpeedStr by remember { mutableStateOf("") }

    BackHandler(enabled = showChannelDrawer || isFullScreen) {
        if (showChannelDrawer) showChannelDrawer = false
        else if (isFullScreen) onToggleFullScreen()
    }

    LaunchedEffect(showOsd, osdTick, isPlaying, showChannelDrawer, isFullScreen) {
        if (isFullScreen && showOsd && isPlaying && !showChannelDrawer) {
            delay(4500L)
            showOsd = false
        }
    }

    // تحديث عداد الزمن والسرعة في وضع ملء الشاشة فقط لمنع أي استهلاك موارد في النافذة المصغرة
    LaunchedEffect(currentMedia, isFullScreen) {
        if (!isFullScreen) return@LaunchedEffect
        var lastBytes = TrafficStats.getTotalRxBytes()
        while (true) {
            delay(1000L)
            posMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durMs = exoPlayer.duration.coerceAtLeast(0L)
            val nowBytes = TrafficStats.getTotalRxBytes()
            if (lastBytes > 0 && nowBytes >= lastBytes) {
                val kbps = ((nowBytes - lastBytes) * 8) / 1000
                netSpeedStr = if (kbps >= 1000) String.format("%.1f Mbps", kbps / 1000f) else "$kbps Kbps"
            }
            lastBytes = nowBytes
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // حاوية الفيديو المعزولة (Zero-Recomposition Surface)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isFullScreen || isInPipMode) Modifier.fillMaxSize() else Modifier.height(210.dp))
                .background(Color.Black)
                .pointerInput(isFullScreen) {
                    detectTapGestures(
                        onTap = {
                            if (showChannelDrawer) {
                                showChannelDrawer = false
                            } else {
                                showOsd = !showOsd
                                osdTick++
                            }
                        },
                        onDoubleTap = {
                            if (!isLocked) onToggleFullScreen()
                        }
                    )
                }
                .pointerInput(gesturesEnabled, isLocked, isFullScreen) {
                    if (isFullScreen && gesturesEnabled && !isLocked) {
                        detectVerticalDragGestures(
                            onDragEnd = { gestureText = "" },
                            onVerticalDrag = { change, dragAmount ->
                                if (change.position.x > size.width / 2f) {
                                    val maxV = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val curV = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val newV = (curV + (if (dragAmount < -12) 1 else if (dragAmount > 12) -1 else 0)).coerceIn(0, maxV)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newV, 0)
                                    gestureText = "🔊 الصوت: ${(newV * 100) / maxV}%"
                                } else {
                                    activity?.window?.let { win ->
                                        val lp = win.attributes
                                        val curB = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
                                        val newB = (curB - dragAmount / 600f).coerceIn(0.05f, 1f)
                                        lp.screenBrightness = newB
                                        win.attributes = lp
                                        gestureText = "☀️ السطوع: ${(newB * 100).toInt()}%"
                                    }
                                }
                            }
                        )
                    }
                }
        ) {
            IsolatedHardwareVideoSurface(
                exoPlayer = exoPlayer,
                resizeMode = resizeModes[resizeIdx].first,
                subFontSize = subFontSize,
                subColorYellow = subColorYellow
            )

            if (isBuffering && !isInPipMode) {
                CircularProgressIndicator(color = accentColor, modifier = Modifier.align(Alignment.Center).size(38.dp))
            }

            if (gestureText.isNotEmpty() && isFullScreen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0xDD12121A), RoundedCornerShape(16.dp))
                        .border(1.dp, accentColor, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(gestureText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // واجهة التحكم في الوضع المصغر (نظيفة وخفيفة بدون أي عبء رسومي)
            if (!isFullScreen && !isInPipMode) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0x9914141C), CircleShape)
                                .clickable { onClosePlayer() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        if (videoResolution.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xAA14141C), RoundedCornerShape(8.dp))
                                    .border(1.dp, greenText.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(videoResolution.substringBefore("•").trim(), color = greenText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0x9914141C), CircleShape)
                                .clickable { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(accentColor, CircleShape)
                                .clickable { onToggleFullScreen() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Fullscreen, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // اسم القناة في أسفل الفيديو المصغر
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        currentMedia.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text("اضغط مرتين للتكبير", color = Color.LightGray, fontSize = 10.sp)
                }
            }

            // واجهة التحكم الزجاجية الكاملة في ملء الشاشة (mtivi Glass Deck)
            if (isFullScreen && !isInPipMode) {
                if (isLocked) {
                    IconButton(
                        onClick = { isLocked = false; showOsd = true },
                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp).background(Color(0xCC14141C), CircleShape)
                    ) {
                        Icon(Icons.Default.Lock, null, tint = accentColor)
                    }
                } else {
                    AnimatedVisibility(
                        visible = showOsd,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent, Color(0xE6000000))))
                        ) {
                            // الشريط العلوي
                            Row(
                                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { onToggleFullScreen() },
                                        modifier = Modifier.size(36.dp).background(Color(0xAA14141C), CircleShape)
                                    ) {
                                        Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    if (videoResolution.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xCC14141C), RoundedCornerShape(10.dp))
                                                .border(1.dp, greenText.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(videoResolution, color = greenText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (netSpeedStr.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xAA14141C), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("⚡ $netSpeedStr", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { isLocked = true },
                                    modifier = Modifier.size(36.dp).background(Color(0xAA14141C), CircleShape)
                                ) {
                                    Icon(Icons.Default.LockOpen, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }

                            // أزرار التحكم المركزية
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(32.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        osdTick++
                                        if (currentMedia.type == "live") onZapChannel(-1)
                                        else exoPlayer.seekTo((exoPlayer.currentPosition - 10000L).coerceAtLeast(0L))
                                    },
                                    modifier = Modifier.size(48.dp).background(Color(0xAA14141C), CircleShape).border(1.dp, Color(0x33FFFFFF), CircleShape)
                                ) {
                                    Icon(if (currentMedia.type == "live") Icons.Default.SkipNext else Icons.Default.Replay10, null, tint = Color.White)
                                }

                                IconButton(
                                    onClick = {
                                        osdTick++
                                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    },
                                    modifier = Modifier.size(64.dp).background(accentColor, CircleShape)
                                ) {
                                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp))
                                }

                                IconButton(
                                    onClick = {
                                        osdTick++
                                        if (currentMedia.type == "live") onZapChannel(1)
                                        else exoPlayer.seekTo(exoPlayer.currentPosition + 10000L)
                                    },
                                    modifier = Modifier.size(48.dp).background(Color(0xAA14141C), CircleShape).border(1.dp, Color(0x33FFFFFF), CircleShape)
                                ) {
                                    Icon(if (currentMedia.type == "live") Icons.Default.SkipPrevious else Icons.Default.Forward10, null, tint = Color.White)
                                }
                            }

                            // الشريط السفلي الزجاجي المستلهم من mtivi
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .background(Color(0xDE0E0E16), RoundedCornerShape(18.dp))
                                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                        if (currentMedia.logo.isNotBlank()) {
                                            AsyncImage(
                                                model = currentMedia.logo,
                                                contentDescription = currentMedia.name,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                                            )
                                        }
                                        Column {
                                            Text(currentMedia.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            val subInfo = if (currentMedia.type == "live") {
                                                currentEpg?.let { "الآن: ${it.nowTitle}" } ?: currentMedia.category
                                            } else currentMedia.category
                                            Text(subInfo, color = greenText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }

                                    // صف الأيقونات الزجاجية السريعة (mtivi Deck)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (currentMedia.type == "live" && allLiveChannels.isNotEmpty()) {
                                            OsdDeckChip(Icons.Default.List, "القنوات", accentColor) { showChannelDrawer = true; osdTick++ }
                                        }
                                        OsdDeckChip(Icons.Default.Subtitles, "CC", Color.White) { showTracksDialog = true; osdTick++ }
                                        OsdDeckChip(Icons.Default.AspectRatio, resizeModes[resizeIdx].second, Color.White) {
                                            resizeIdx = (resizeIdx + 1) % resizeModes.size; osdTick++
                                        }
                                        OsdDeckChip(Icons.Default.PictureInPictureAlt, "PiP", Color.White) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                activity?.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
                                            }
                                        }
                                        OsdDeckChip(Icons.Default.OpenInNew, "خارجي", Color(0xFFFBBF24)) {
                                            val resolved = ServersEngine.resolveStreamUrl(currentMedia.url, activeHost)
                                            launchPreferredExternalPlayer(context, resolved, currentMedia.name, "ask", ServersEngine.currentUserAgent)
                                        }
                                        OsdDeckChip(Icons.Default.FullscreenExit, "تصغير", greenText) { onToggleFullScreen() }
                                    }
                                }

                                if (currentMedia.type != "live" && durMs > 0L) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(formatDurationMs(posMs), color = Color.LightGray, fontSize = 11.sp)
                                        Slider(
                                            value = (posMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f),
                                            onValueChange = { ratio ->
                                                osdTick++
                                                exoPlayer.seekTo((ratio * durMs).toLong())
                                            },
                                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                                            modifier = Modifier.weight(1f).height(20.dp)
                                        )
                                        Text(formatDurationMs(durMs), color = Color.LightGray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // درج القنوات الجانبي الشفاف للتقليب داخل ملء الشاشة
            AnimatedVisibility(
                visible = showChannelDrawer,
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = slideOutHorizontally(targetOffsetX = { -it }),
                modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().width(290.dp)
            ) {
                var selectedDrawerCat by remember { mutableStateOf(currentMedia.category) }
                val categories = remember(allLiveChannels) { allLiveChannels.map { it.category }.distinct() }
                val catChannels = remember(allLiveChannels, selectedDrawerCat) {
                    allLiveChannels.filter { it.category == selectedDrawerCat }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xF00B0B12))
                        .border(1.dp, accentColor.copy(alpha = 0.35f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("القنوات المباشرة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        IconButton(onClick = { showChannelDrawer = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray)
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = cat == selectedDrawerCat,
                                onClick = { selectedDrawerCat = cat },
                                label = { Text(cat, fontSize = 10.sp, maxLines = 1) }
                            )
                        }
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                        items(catChannels) { ch ->
                            val isCurr = ch.id == currentMedia.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurr) accentColor.copy(alpha = 0.25f) else Color(0xFF151520))
                                    .clickable {
                                        onSelectChannel(ch)
                                        showChannelDrawer = false
                                    }
                                    .padding(9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AsyncImage(model = ch.logo, contentDescription = ch.name, modifier = Modifier.size(26.dp))
                                Text(
                                    ch.name,
                                    color = if (isCurr) accentColor else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurr) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isCurr) Text("▶", color = greenText, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // شريط الأدوات السريع أسفل المشغل المصغر (Kepler Floating Action Deck)
        if (!isFullScreen && !isInPipMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B0B11))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (currentMedia.type == "live" && allLiveChannels.isNotEmpty()) {
                    MiniDeckButton(Icons.Default.List, "القنوات", accentColor, Modifier.weight(1f)) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleFullScreen()
                        showChannelDrawer = true
                    }
                }
                MiniDeckButton(Icons.Default.Subtitles, "الصوت/CC", Color.White, Modifier.weight(1f)) {
                    showTracksDialog = true
                }
                MiniDeckButton(Icons.Default.AspectRatio, resizeModes[resizeIdx].second, Color.White, Modifier.weight(1f)) {
                    resizeIdx = (resizeIdx + 1) % resizeModes.size
                }
                MiniDeckButton(Icons.Default.PictureInPictureAlt, "عائمة", Color(0xFF38BDF8), Modifier.weight(1f)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        activity?.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
                    }
                }
                MiniDeckButton(Icons.Default.OpenInNew, "خارجي", Color(0xFFFBBF24), Modifier.weight(1f)) {
                    val resolved = ServersEngine.resolveStreamUrl(currentMedia.url, activeHost)
                    launchPreferredExternalPlayer(context, resolved, currentMedia.name, "ask", ServersEngine.currentUserAgent)
                }
            }
        }
    }

    if (showTracksDialog) {
        val tracks = exoPlayer.currentTracks
        AlertDialog(
            onDismissRequest = { showTracksDialog = false },
            containerColor = Color(0xFF14141C),
            title = { Text("مسارات الصوت والترجمة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مسارات الصوت:", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    var foundAudio = false
                    tracks.groups.forEach { group ->
                        if (group.type == C.TRACK_TYPE_AUDIO) {
                            for (tIdx in 0 until group.length) {
                                foundAudio = true
                                val fmt = group.getTrackFormat(tIdx)
                                val label = listOfNotNull(fmt.label, fmt.language?.uppercase(), "${fmt.channelCount}ch").joinToString(" • ").ifBlank { "مسار صوت ${tIdx + 1}" }
                                val isSelected = group.isTrackSelected(tIdx)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) accentColor.copy(alpha = 0.25f) else Color(0xFF0D0D12))
                                        .clickable {
                                            trackSelector.parameters = trackSelector.buildUponParameters()
                                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, tIdx))
                                                .build()
                                            showTracksDialog = false
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, color = Color.White, fontSize = 12.sp)
                                    if (isSelected) Text("✓", color = greenText, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    if (!foundAudio) Text("مسار صوت افتراضي واحد", color = Color.Gray, fontSize = 12.sp)

                    HorizontalDivider(color = Color(0x22FFFFFF))
                    Text("الترجمة:", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("إيقاف الترجمة", color = Color(0xFFF87171), fontSize = 12.sp, modifier = Modifier.fillMaxWidth().clickable {
                        trackSelector.parameters = trackSelector.buildUponParameters()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                        showTracksDialog = false
                    }.padding(6.dp))

                    tracks.groups.forEach { group ->
                        if (group.type == C.TRACK_TYPE_TEXT) {
                            for (tIdx in 0 until group.length) {
                                val fmt = group.getTrackFormat(tIdx)
                                val label = listOfNotNull(fmt.label, fmt.language?.uppercase()).joinToString(" • ").ifBlank { "ترجمة ${tIdx + 1}" }
                                Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().clickable {
                                    trackSelector.parameters = trackSelector.buildUponParameters()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, tIdx))
                                        .build()
                                    showTracksDialog = false
                                }.padding(6.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTracksDialog = false }) { Text("إغلاق", color = Color.Gray) } }
        )
    }
}

@Composable
private fun OsdDeckChip(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A26))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Text(label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MiniDeckButton(icon: ImageVector, label: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF14141E))
            .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000L
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

fun launchPreferredExternalPlayer(
    context: Context,
    streamUrl: String,
    title: String,
    playerPref: String,
    userAgent: String
): Boolean {
    val uri = Uri.parse(streamUrl)
    val mxPackages = listOf("com.mxtech.videoplayer.pro", "com.mxtech.videoplayer.ad")
    val vlcPackage = "org.videolan.vlc"

    fun tryStartPackage(pkg: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                setPackage(pkg)
                putExtra("title", title)
                putExtra("headers", arrayOf("User-Agent", userAgent))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    return when (playerPref) {
        "mx" -> mxPackages.any { tryStartPackage(it) } || tryStartPackage(vlcPackage)
        "vlc" -> tryStartPackage(vlcPackage)
        "ask" -> {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/*")
                    putExtra("title", title)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "اختر المشغل"))
                true
            } catch (_: Exception) {
                false
            }
        }
        else -> false
    }
}
