package com.hadedo.novaiptv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    var isInPipState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF38BDF8), background = Color(0xFF060913), surface = Color(0xFF0F172A))) {
                NovaIptvProEdition(isInPipMode = isInPipState)
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipState = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun NovaIptvProEdition(isInPipMode: Boolean) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("nova_prefs", Context.MODE_PRIVATE) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var selectedTab by remember { mutableIntStateOf(0) }
    var serverMode by remember { mutableIntStateOf(0) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(true) }
    var resizeIdx by remember { mutableIntStateOf(0) }
    val resizeModes = listOf(Pair(AspectRatioFrameLayout.RESIZE_MODE_FIT, "طبيعي"), Pair(AspectRatioFrameLayout.RESIZE_MODE_FILL, "ملء"), Pair(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "تكبير"))

    var xtHost by remember { mutableStateOf(prefs.getString("xt_host", "") ?: "") }
    var xtUser by remember { mutableStateOf(prefs.getString("xt_user", "") ?: "") }
    var xtPass by remember { mutableStateOf(prefs.getString("xt_pass", "") ?: "") }
    var stPortal by remember { mutableStateOf(prefs.getString("st_portal", "") ?: "") }
    var stMac by remember { mutableStateOf(prefs.getString("st_mac", "00:1A:79:") ?: "00:1A:79:") }
    var m3uUrl by remember { mutableStateOf(prefs.getString("m3u_url", "") ?: "") }

    var accountInfo by remember { mutableStateOf<AccountInfo?>(null) }
    var mediaList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var favoriteIds by remember { mutableStateOf(prefs.getStringSet("fav_ids", emptySet()) ?: emptySet()) }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }
    var currentMedia by remember { mutableStateOf<MediaItemModel?>(null) }
    var videoResolution by remember { mutableStateOf("") }
    var gestureIndicator by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0) {
                        val tag = when {
                            videoSize.height >= 2160 -> "4K UHD"
                            videoSize.height >= 1080 -> "1080p FHD"
                            videoSize.height >= 720 -> "720p HD"
                            else -> "${videoSize.height}p"
                        }
                        videoResolution = "$tag • LIVE"
                    }
                }
            })
        }
    }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    LaunchedEffect(isFullScreen) {
        activity?.let { act ->
            if (isFullScreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    act.window.insetsController?.let {
                        it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                        it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    act.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                }
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) act.window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                else @Suppress("DEPRECATION") act.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    BackHandler(enabled = isFullScreen) { isFullScreen = false }

    fun toggleFavorite(id: String) {
        val updated = if (favoriteIds.contains(id)) favoriteIds - id else favoriteIds + id
        favoriteIds = updated
        prefs.edit().putStringSet("fav_ids", updated).apply()
    }

    fun playStream(item: MediaItemModel) {
        currentMedia = item
        videoResolution = "جاري الاتصال..."
        exoPlayer.setMediaItem(MediaItem.fromUri(item.url))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
        }
    }

    val categories = remember(mediaList) { listOf("الكل", "★ المفضلة") + mediaList.map { it.category }.distinct() }
    val filteredList = remember(mediaList, selectedCategory, searchQuery, favoriteIds) {
        mediaList.filter { item ->
            val matchCat = when (selectedCategory) {
                "الكل" -> true
                "★ المفضلة" -> favoriteIds.contains(item.id)
                else -> item.category == selectedCategory
            }
            matchCat && (searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true))
        }
    }

    // صندوق المشغل مع إيماءات السحب للصوت والسطوع
    @Composable
    fun PlayerBox(modifier: Modifier) {
        Box(
            modifier = modifier
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = { gestureIndicator = "" },
                        onVerticalDrag = { change, dragAmount ->
                            val isRightSide = change.position.x > size.width / 2f
                            if (isRightSide) {
                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val delta = if (dragAmount < -12) 1 else if (dragAmount > 12) -1 else 0
                                val newVol = (curVol + delta).coerceIn(0, maxVol)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                gestureIndicator = "الصوت: ${(newVol * 100) / maxVol}%"
                            } else {
                                activity?.window?.let { win ->
                                    val lp = win.attributes
                                    val curB = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
                                    val newB = (curB - dragAmount / 600f).coerceIn(0.05f, 1f)
                                    lp.screenBrightness = newB
                                    win.attributes = lp
                                    gestureIndicator = "السطوع: ${(newB * 100).toInt()}%"
                                }
                            }
                        }
                    )
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = !isInPipMode
                        resizeMode = resizeModes[resizeIdx].first
                        setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { v -> showOverlay = (v == View.VISIBLE) })
                    }
                },
                update = { view ->
                    view.useController = !isInPipMode
                    view.resizeMode = resizeModes[resizeIdx].first
                },
                modifier = Modifier.fillMaxSize()
            )

            if (gestureIndicator.isNotEmpty() && !isInPipMode) {
                Box(modifier = Modifier.align(Alignment.Center).background(Color(0xCC0F172A), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(gestureIndicator, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            if (!isInPipMode && showOverlay) {
                // شارة الدقة الحية أعلى اليسار
                if (videoResolution.isNotEmpty()) {
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(10.dp).background(Color(0xBB0F172A), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(videoResolution, color = Color(0xFF4ADE80), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // أزرار التحكم أعلى اليمين
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).background(Color(0xCC0F172A), RoundedCornerShape(20.dp)).border(1.dp, Color(0x4438BDF8), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تحديث", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { currentMedia?.let { playStream(it) } })
                    Text(resizeModes[resizeIdx].second, color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { resizeIdx = (resizeIdx + 1) % resizeModes.size })
                    Text("عائمة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { enterPip() })
                    Text(if (isFullScreen) "تصغير" else "تكبير", color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { isFullScreen = !isFullScreen })
                }
            }
        }
    }

    if (isInPipMode || isFullScreen) {
        PlayerBox(Modifier.fillMaxSize())
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0B1120)) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("الحساب") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1; selectedCategory = "الكل"; if (serverMode == 0 && xtHost.isNotEmpty()) scope.launch { isLoading = true; mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "live"); isLoading = false } }, icon = { Icon(Icons.Default.PlayArrow, null) }, label = { Text("مباشر") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2; selectedCategory = "الكل"; if (serverMode == 0 && xtHost.isNotEmpty()) scope.launch { isLoading = true; mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "vod"); isLoading = false } }, icon = { Icon(Icons.Default.PlayArrow, null) }, label = { Text("أفلام") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3; selectedCategory = "الكل"; if (serverMode == 0 && xtHost.isNotEmpty()) scope.launch { isLoading = true; mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "series"); isLoading = false } }, icon = { Icon(Icons.Default.PlayArrow, null) }, label = { Text("مسلسلات") })
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFF060913))) {
            PlayerBox(Modifier.fillMaxWidth().height(225.dp))

            if (selectedTab == 0) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // بطاقة بيانات الاشتراك الاحترافية
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), border = BorderStroke(1.dp, Color(0x4438BDF8)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("NovaIPTV Pro", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(accountInfo?.status ?: if (mediaList.isNotEmpty()) "متصل" else "غير متصل", color = Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            if (accountInfo != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("انتهاء الاشتراك: ${accountInfo?.expDate}  |  الأجهزة: ${accountInfo?.activeCons}/${accountInfo?.maxCons}", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Xtream كود", "Stalker ماك", "رابط M3U / أكستريم").forEachIndexed { idx, title ->
                            FilterChip(selected = serverMode == idx, onClick = { serverMode = idx }, label = { Text(title, fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    when (serverMode) {
                        0 -> {
                            OutlinedTextField(value = xtHost, onValueChange = { xtHost = it }, label = { Text("رابط السيرفر (http://domain:port)") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = xtUser, onValueChange = { xtUser = it }, label = { Text("اسم المستخدم") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = xtPass, onValueChange = { xtPass = it }, label = { Text("كلمة المرور") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = {
                                prefs.edit().putString("xt_host", xtHost).putString("xt_user", xtUser).putString("xt_pass", xtPass).apply()
                                scope.launch {
                                    isLoading = true
                                    accountInfo = ServersEngine.getAccountInfo(xtHost, xtUser, xtPass)
                                    mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "live")
                                    isLoading = false
                                    if (mediaList.isNotEmpty()) selectedTab = 1 else statusMessage = "تعذر الاتصال بالسيرفر"
                                }
                            }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("اتصال وتشغيل البث", fontWeight = FontWeight.Bold) }
                        }
                        1 -> {
                            OutlinedTextField(value = stPortal, onValueChange = { stPortal = it }, label = { Text("رابط البورتال (http://portal.com/c)") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = stMac, onValueChange = { stMac = it }, label = { Text("عنوان الماك (MAC Address)") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = {
                                prefs.edit().putString("st_portal", stPortal).putString("st_mac", stMac).apply()
                                scope.launch {
                                    isLoading = true; mediaList = ServersEngine.loadStalker(stPortal, stMac); isLoading = false
                                    if (mediaList.isNotEmpty()) selectedTab = 1 else statusMessage = "فشل الاتصال ببورتال Stalker"
                                }
                            }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("اتصال بـ Stalker", fontWeight = FontWeight.Bold) }
                        }
                        2 -> {
                            OutlinedTextField(value = m3uUrl, onValueChange = { m3uUrl = it }, label = { Text("ضع رابط M3U أو رابط Xtream الكامل") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = {
                                prefs.edit().putString("m3u_url", m3uUrl).apply()
                                scope.launch {
                                    isLoading = true
                                    val ex = ServersEngine.parseXtreamFromUrl(m3uUrl)
                                    if (ex != null) {
                                        xtHost = ex.host; xtUser = ex.user; xtPass = ex.pass; serverMode = 0
                                        prefs.edit().putString("xt_host", xtHost).putString("xt_user", xtUser).putString("xt_pass", xtPass).apply()
                                        accountInfo = ServersEngine.getAccountInfo(xtHost, xtUser, xtPass)
                                        mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "live")
                                    } else {
                                        mediaList = M3uParser.loadFromUrl(m3uUrl).mapIndexed { idx, ch -> MediaItemModel("$idx", ch.name, ch.url, ch.group) }
                                    }
                                    isLoading = false
                                    if (mediaList.isNotEmpty()) selectedTab = 1 else statusMessage = "لم يتم العثور على قنوات"
                                }
                            }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("تحليل وتشغيل الرابط", fontWeight = FontWeight.Bold) }
                        }
                    }
                    if (isLoading) { Spacer(modifier = Modifier.height(12.dp)); CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally)) }
                    else if (statusMessage.isNotEmpty()) { Spacer(modifier = Modifier.height(8.dp)); Text(statusMessage, color = Color(0xFF38BDF8), fontSize = 13.sp) }
                }
            } else {
                LazyRow(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat -> FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat, fontSize = 12.sp) }) }
                }
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("ابحث في ${filteredList.size} عنصر...", fontSize = 13.sp) }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                Spacer(modifier = Modifier.height(6.dp))
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (selectedTab == 2 || selectedTab == 3) {
                    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                        items(filteredList) { item ->
                            val isPlaying = currentMedia?.url == item.url
                            val isFav = favoriteIds.contains(item.id)
                            Column(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (isPlaying) Color(0xFF1D4ED8) else Color(0xFF0F172A)).border(1.dp, if (isPlaying) Color(0xFF38BDF8) else Color(0x22FFFFFF), RoundedCornerShape(12.dp)).clickable { playStream(item) }) {
                                Box {
                                    AsyncImage(model = item.logo, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(145.dp).background(Color(0xFF1E293B)))
                                    Icon(imageVector = if (isFav) Icons.Default.Star else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isFav) Color(0xFFFBBF24) else Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clickable { toggleFavorite(item.id) })
                                }
                                Text(item.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(6.dp))
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredList) { item ->
                            val isPlaying = currentMedia?.url == item.url
                            val isFav = favoriteIds.contains(item.id)
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (isPlaying) Brush.horizontalGradient(listOf(Color(0xFF1D4ED8), Color(0xFF0284C7))) else Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))).border(1.dp, if (isPlaying) Color(0xFF38BDF8) else Color(0x1AFFFFFF), RoundedCornerShape(14.dp)).clickable { playStream(item) }.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = item.logo, contentDescription = item.name, contentScale = ContentScale.Fit, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E293B)).padding(4.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(item.category, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                    IconButton(onClick = { toggleFavorite(item.id) }) {
                                        Icon(imageVector = if (isFav) Icons.Default.Star else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isFav) Color(0xFFFBBF24) else Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
// END
