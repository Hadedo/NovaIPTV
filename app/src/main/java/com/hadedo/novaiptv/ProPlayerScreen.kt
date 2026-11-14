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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
        Pair(AspectRatioFrameLayout.RESIZE_MODE_FILL, "ملء الشاشة"),
        Pair(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "تكبير سينمائي")
    )

    var posMs by remember { mutableLongStateOf(0L) }
    var durMs by remember { mutableLongStateOf(0L) }
    var netSpeedStr by remember { mutableStateOf("") }

    BackHandler(enabled = showChannelDrawer || isFullScreen) {
        if (showChannelDrawer) showChannelDrawer = false
        else if (isFullScreen) onToggleFullScreen()
    }

    LaunchedEffect(showOsd, osdTick, isPlaying, showChannelDrawer) {
        if (showOsd && isPlaying && !showChannelDrawer) {
            delay(4500L)
            showOsd = false
        }
    }

    // تحديث عداد الزمن وسرعة الإنترنت المسحوبة حياً
    LaunchedEffect(currentMedia) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
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
            .pointerInput(gesturesEnabled, isLocked) {
                if (gesturesEnabled && !isLocked) {
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
        // سطح العرض النقي (SurfaceView GPU)
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = resizeModes[resizeIdx].first
                    setKeepContentOnPlayerReset(true)
                }
            },
            update = { view ->
                view.resizeMode = resizeModes[resizeIdx].first
                view.subtitleView?.apply {
                    setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subFontSize)
                    setStyle(
                        CaptionStyleCompat(
                            if (subColorYellow) AndroidColor.YELLOW else AndroidColor.WHITE,
                            AndroidColor.argb(150, 0, 0, 0),
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

        if (isBuffering && !isInPipMode) {
            CircularProgressIndicator(color = accentColor, modifier = Modifier.align(Alignment.Center).size(42.dp))
        }

        if (gestureText.isNotEmpty() && !isInPipMode) {
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

        if (!isInPipMode) {
            if (isLocked) {
                IconButton(
                    onClick = { isLocked = false; showOsd = true },
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp).background(Color(0xCC14141C), CircleShape)
                ) {
                    Icon(Icons.Default.Lock, null, tint = accentColor)
                }
            } else {
                // واجهة التحكم الزجاجية (Nova Glass OSD)
                AnimatedVisibility(
                    visible = showOsd,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xD9000000), Color.Transparent, Color(0xE6000000))
                                )
                            )
                    ) {
                        // 1. الشريط العلوي (معلومات البث الحية + أزرار الأدوات)
                        Row(
                            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { if (isFullScreen) onToggleFullScreen() else onClosePlayer() },
                                    modifier = Modifier.size(34.dp).background(Color(0xAA14141C), CircleShape)
                                ) {
                                    Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }

                                if (videoResolution.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xCC14141C), RoundedCornerShape(10.dp))
                                            .border(1.dp, greenText.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(videoResolution, color = greenText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (netSpeedStr.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xAA14141C), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("⚡ $netSpeedStr", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .background(Color(0xCC14141C), RoundedCornerShape(20.dp))
                                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (currentMedia.type == "live" && allLiveChannels.isNotEmpty()) {
                                    Text(
                                        "📺 القنوات",
                                        color = accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { showChannelDrawer = true; osdTick++ }
                                    )
                                }
                                Text(
                                    "الصوت/الترجمة",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { showTracksDialog = true; osdTick++ }
                                )
                                Icon(Icons.Default.LockOpen, null, tint = Color.White, modifier = Modifier.size(16.dp).clickable { isLocked = true })
                                Text(
                                    resizeModes[resizeIdx].second,
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    modifier = Modifier.clickable { resizeIdx = (resizeIdx + 1) % resizeModes.size; osdTick++ }
                                )
                                Text(
                                    "خارجي",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        try {
                                            val resolved = ServersEngine.resolveStreamUrl(currentMedia.url, activeHost)
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(Uri.parse(resolved), "video/*")
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "اختر المشغل الخارجي"))
                                        } catch (_: Exception) {}
                                    }
                                )
                                Text(
                                    "عائمة",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.clickable {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            activity?.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
                                        }
                                    }
                                )
                                Text(
                                    if (isFullScreen) "تصغير" else "ملء الشاشة",
                                    color = greenText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onToggleFullScreen() }
                                )
                            }
                        }

                        // 2. أزرار التحكم المركزية المتوهجة
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    osdTick++
                                    if (currentMedia.type == "live") onZapChannel(-1)
                                    else exoPlayer.seekTo((exoPlayer.currentPosition - 10000L).coerceAtLeast(0L))
                                },
                                modifier = Modifier.size(46.dp).background(Color(0xAA14141C), CircleShape).border(1.dp, Color(0x33FFFFFF), CircleShape)
                            ) {
                                Icon(if (currentMedia.type == "live") Icons.Default.SkipNext else Icons.Default.Replay10, null, tint = Color.White)
                            }

                            IconButton(
                                onClick = {
                                    osdTick++
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                                modifier = Modifier.size(60.dp).background(accentColor, CircleShape)
                            ) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(34.dp))
                            }

                            IconButton(
                                onClick = {
                                    osdTick++
                                    if (currentMedia.type == "live") onZapChannel(1)
                                    else exoPlayer.seekTo(exoPlayer.currentPosition + 10000L)
                                },
                                modifier = Modifier.size(46.dp).background(Color(0xAA14141C), CircleShape).border(1.dp, Color(0x33FFFFFF), CircleShape)
                            ) {
                                Icon(if (currentMedia.type == "live") Icons.Default.SkipPrevious else Icons.Default.Forward10, null, tint = Color.White)
                            }
                        }

                        // 3. بطاقة معلومات القناة والبرنامج الحي في الأسفل
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .background(Color(0xD9101018), RoundedCornerShape(16.dp))
                                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (currentMedia.logo.isNotBlank()) {
                                    AsyncImage(
                                        model = currentMedia.logo,
                                        contentDescription = currentMedia.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(currentMedia.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (currentMedia.type == "live") {
                                        val epgText = currentEpg?.let { "الآن: ${it.nowTitle}" + if (it.nextTitle.isNotBlank()) " • التالي: ${it.nextTitle}" else "" } ?: currentMedia.category
                                        Text(epgText, color = greenText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    } else {
                                        Text(currentMedia.category, color = Color.LightGray, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                                Text(
                                    if (currentMedia.type == "live") "🔴 مباشر" else "${formatDurationMs(posMs)} / ${formatDurationMs(durMs)}",
                                    color = if (currentMedia.type == "live") Color(0xFFF87171) else accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (currentMedia.type != "live" && durMs > 0L) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Slider(
                                    value = (posMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f),
                                    onValueChange = { ratio ->
                                        osdTick++
                                        exoPlayer.seekTo((ratio * durMs).toLong())
                                    },
                                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                                    modifier = Modifier.height(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. درج القنوات الجانبي الشفاف (In-Player Channel Zapper Drawer مثل StreamVault و TiviMate)
            AnimatedVisibility(
                visible = showChannelDrawer,
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = slideOutHorizontally(targetOffsetX = { -it }),
                modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().width(280.dp)
            ) {
                var selectedDrawerCat by remember { mutableStateOf(currentMedia.category) }
                val categories = remember(allLiveChannels) { allLiveChannels.map { it.category }.distinct() }
                val catChannels = remember(allLiveChannels, selectedDrawerCat) {
                    allLiveChannels.filter { it.category == selectedDrawerCat }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xEB0D0D14))
                        .border(1.dp, accentColor.copy(alpha = 0.3f))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("دليل القنوات السريع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                    .background(if (isCurr) accentColor.copy(alpha = 0.25f) else Color(0xFF161622))
                                    .clickable {
                                        onSelectChannel(ch)
                                        showChannelDrawer = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AsyncImage(model = ch.logo, contentDescription = ch.name, modifier = Modifier.size(28.dp))
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
    }

    // نافذة اختيار مسارات الصوت والترجمة
    if (showTracksDialog) {
        val tracks = exoPlayer.currentTracks
        AlertDialog(
            onDismissRequest = { showTracksDialog = false },
            containerColor = Color(0xFF14141C),
            title = { Text("مسارات الصوت والترجمة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مسارات الصوت المتاحة:", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                    Text("الترجمة (Subtitles):", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

private fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000L
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
