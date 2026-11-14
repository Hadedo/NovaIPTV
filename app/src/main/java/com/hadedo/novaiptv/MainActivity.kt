package com.hadedo.novaiptv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF38BDF8),
                    background = Color(0xFF090D16),
                    surface = Color(0xFF131B2E)
                )
            ) {
                NovaIptvModernApp()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun NovaIptvModernApp() {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("nova_prefs", Context.MODE_PRIVATE) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var serverMode by remember { mutableIntStateOf(0) }
    var isFullScreen by remember { mutableStateOf(false) }
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    val resizeModes = listOf(
        Pair(AspectRatioFrameLayout.RESIZE_MODE_FIT, "طبيعي"),
        Pair(AspectRatioFrameLayout.RESIZE_MODE_FILL, "ملء"),
        Pair(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "تكبير")
    )

    var xtHost by remember { mutableStateOf(prefs.getString("xt_host", "") ?: "") }
    var xtUser by remember { mutableStateOf(prefs.getString("xt_user", "") ?: "") }
    var xtPass by remember { mutableStateOf(prefs.getString("xt_pass", "") ?: "") }

    var stPortal by remember { mutableStateOf(prefs.getString("st_portal", "") ?: "") }
    var stMac by remember { mutableStateOf(prefs.getString("st_mac", "00:1A:79:") ?: "00:1A:79:") }

    var m3uUrl by remember { mutableStateOf(prefs.getString("m3u_url", "") ?: "") }

    var mediaList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }
    var currentMedia by remember { mutableStateOf<MediaItemModel?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(isFullScreen) {
        activity?.let { act ->
            val window = act.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullScreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = isFullScreen) {
        isFullScreen = false
    }

    fun playStream(item: MediaItemModel) {
        currentMedia = item
        exoPlayer.setMediaItem(MediaItem.fromUri(item.url))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
            )
        }
    }

    val categories = remember(mediaList) {
        listOf("الكل") + mediaList.map { it.category }.distinct()
    }

    val filteredList = remember(mediaList, selectedCategory, searchQuery) {
        mediaList.filter { item ->
            val matchCat = selectedCategory == "الكل" || item.category == selectedCategory
            val matchSearch = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
            matchCat && matchSearch
        }
    }

    Scaffold(
        bottomBar = {
            if (!isFullScreen) {
                NavigationBar(containerColor = Color(0xFF131B2E)) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("السيرفرات") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            selectedCategory = "الكل"
                            if (serverMode == 0 && xtHost.isNotEmpty()) {
                                scope.launch {
                                    isLoading = true
                                    mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "live")
                                    isLoading = false
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.LiveTv, null) },
                        label = { Text("مباشر") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            selectedCategory = "الكل"
                            if (serverMode == 0 && xtHost.isNotEmpty()) {
                                scope.launch {
                                    isLoading = true
                                    mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "vod")
                                    isLoading = false
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Movie, null) },
                        label = { Text("أفلام") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = {
                            selectedTab = 3
                            selectedCategory = "الكل"
                            if (serverMode == 0 && xtHost.isNotEmpty()) {
                                scope.launch {
                                    isLoading = true
                                    mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "series")
                                    isLoading = false
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.VideoLibrary, null) },
                        label = { Text("مسلسلات") }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreen) PaddingValues(0.dp) else padding)
                .background(Color(0xFF090D16))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isFullScreen) Modifier.fillMaxSize() else Modifier.height(230.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            resizeMode = resizeModes[resizeModeIndex].first
                        }
                    },
                    update = { view ->
                        view.resizeMode = resizeModes[resizeModeIndex].first
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(Color(0x99000000), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = resizeModes[resizeModeIndex].second,
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    IconButton(
                        onClick = { enterPip() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "نافذة عائمة",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { isFullScreen = !isFullScreen },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "ملء الشاشة",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!isFullScreen) {
                if (selectedTab == 0) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "إعدادات الاتصال والاشتراك",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Xtream كود", "Stalker ماك", "رابط M3U / أكستريم").forEachIndexed { idx, title ->
                                FilterChip(
                                    selected = serverMode == idx,
                                    onClick = { serverMode = idx },
                                    label = { Text(title, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (serverMode) {
                            0 -> {
                                OutlinedTextField(
                                    value = xtHost,
                                    onValueChange = { xtHost = it },
                                    label = { Text("رابط السيرفر (http://domain:port)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = xtUser,
                                    onValueChange = { xtUser = it },
                                    label = { Text("اسم المستخدم") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = xtPass,
                                    onValueChange = { xtPass = it },
                                    label = { Text("كلمة المرور") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        prefs.edit()
                                            .putString("xt_host", xtHost)
                                            .putString("xt_user", xtUser)
                                            .putString("xt_pass", xtPass)
                                            .apply()
                                        scope.launch {
                                            isLoading = true
                                            mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "live")
                                            isLoading = false
                                            if (mediaList.isNotEmpty()) selectedTab = 1
                                            else statusMessage = "تعذر الاتصال بالسيرفر"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("تشغيل وحفظ البيانات", fontWeight = FontWeight.Bold)
                                }
                            }

                            1 -> {
                                OutlinedTextField(
                                    value = stPortal,
                                    onValueChange = { stPortal = it },
                                    label = { Text("رابط البورتال (http://portal.com/c)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = stMac,
                                    onValueChange = { stMac = it },
                                    label = { Text("عنوان الماك (MAC Address)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        prefs.edit().putString("st_portal", stPortal).putString("st_mac", stMac).apply()
                                        scope.launch {
                                            isLoading = true
                                            mediaList = ServersEngine.loadStalker(stPortal, stMac)
                                            isLoading = false
                                            if (mediaList.isNotEmpty()) selectedTab = 1
                                            else statusMessage = "فشل الاتصال ببورتال Stalker"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("اتصال بـ Stalker", fontWeight = FontWeight.Bold)
                                }
                            }

                            2 -> {
                                OutlinedTextField(
                                    value = m3uUrl,
                                    onValueChange = { m3uUrl = it },
                                    label = { Text("ضع رابط M3U أو رابط Xtream الكامل هنا") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        prefs.edit().putString("m3u_url", m3uUrl).apply()
                                        scope.launch {
                                            isLoading = true
                                            val extracted = ServersEngine.parseXtreamFromUrl(m3uUrl)
                                            if (extracted != null) {
                                                xtHost = extracted.host
                                                xtUser = extracted.user
                                                xtPass = extracted.pass
                                                serverMode = 0
                                                prefs.edit()
                                                    .putString("xt_host", xtHost)
                                                    .putString("xt_user", xtUser)
                                                    .putString("xt_pass", xtPass)
                                                    .apply()
                                                mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "live")
                                            } else {
                                                val raw = M3uParser.loadFromUrl(m3uUrl)
                                                mediaList = raw.mapIndexed { idx, ch ->
                                                    MediaItemModel("$idx", ch.name, ch.url, ch.group)
                                                }
                                            }
                                            isLoading = false
                                            if (mediaList.isNotEmpty()) selectedTab = 1
                                            else statusMessage = "لم يتم العثور على قنوات في هذا الرابط"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("تحليل وتشغيل الرابط تلقائياً", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (isLoading) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else if (statusMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = statusMessage, color = Color(0xFF38BDF8), fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث في ${filteredList.size} قناة...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredList) { item ->
                                val isPlaying = currentMedia?.url == item.url
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isPlaying) Brush.horizontalGradient(
                                                listOf(Color(0xFF1D4ED8), Color(0xFF0284C7))
                                            ) else Brush.horizontalGradient(
                                                listOf(Color(0xFF131B2E), Color(0xFF1E293B))
                                            )
                                        )
                                        .clickable { playStream(item) }
                                        .padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x3338BDF8)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = item.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = item.category,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 12.sp
                               
Mode = resizeModes[resizeModeIndex].first
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // شريط أزرار التحكم السريع أعلى المشغل
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(Color(0x99000000), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // زر تغيير أبعاد الشاشة (طبيعي / ملء / تكبير)
                    Text(
                        text = resizeModes[resizeModeIndex].second,
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    // زر النافذة العائمة (PiP)
                    IconButton(
                        onClick = { enterPip() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "نافذة عائمة",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // زر ملء الشاشة وتدويرها
                    IconButton(
                        onClick = { isFullScreen = !isFullScreen },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "ملء الشاشة",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!isFullScreen) {
                if (selectedTab == 0) {
                    // واجهة السيرفرات العصرية
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "إعدادات الاتصال والاشتراك",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Xtream كود", "Stalker ماك", "رابط M3U / أكستريم").forEachIndexed { idx, title ->
                                FilterChip(
                                    selected = serverMode == idx,
                                    onClick = { serverMode = idx },
                                    label = { Text(title, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (serverMode) {
                            0 -> {
                                OutlinedTextField(
                                    value = xtHost,
                                    onValueChange = { xtHost = it },
                                    label = { Text("رابط السيرفر (http://domain:port)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = xtUser,
                                    onValueChange = { xtUser = it },
                                    label = { Text("اسم المستخدم") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = xtPass,
                                    onValueChange = { xtPass = it },
                                    label = { Text("كلمة المرور") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        prefs.edit()
                                            .putString("xt_host", xtHost)
                                            .putString("xt_user", xtUser)
                                            .putString("xt_pass", xtPass)
                                            .apply()
                                        scope.launch {
                                            isLoading = true
                                            mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "live")
                                            isLoading = false
                                            if (mediaList.isNotEmpty()) selectedTab = 1
                                            else statusMessage = "تعذر الاتصال بالسيرفر"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("تشغيل وحفظ البيانات", fontWeight = FontWeight.Bold)
                                }
                            }

                            1 -> {
                                OutlinedTextField(
                                    value = stPortal,
                                    onValueChange = { stPortal = it },
                                    label = { Text("رابط البورتال (http://portal.com/c)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = stMac,
                                    onValueChange = { stMac = it },
                                    label = { Text("عنوان الماك (MAC Address)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        prefs.edit().putString("st_portal", stPortal).putString("st_mac", stMac).apply()
                                        scope.launch {
                                            isLoading = true
                                            mediaList = ServersEngine.loadStalker(stPortal, stMac)
                                            isLoading = false
                                            if (mediaList.isNotEmpty()) selectedTab = 1
                                            else statusMessage = "فشل الاتصال ببورتال Stalker"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("اتصال بـ Stalker", fontWeight = FontWeight.Bold)
                                }
                            }

                            2 -> {
                                OutlinedTextField(
                                    value = m3uUrl,
                                    onValueChange = { m3uUrl = it },
                                    label = { Text("ضع رابط M3U أو رابط Xtream الكامل هنا") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        prefs.edit().putString("m3u_url", m3uUrl).apply()
                                        scope.launch {
                                            isLoading = true
                                            // فحص ذكي: هل الرابط هو رابط Xtream كامل؟
                                            val extracted = ServersEngine.parseXtreamFromUrl(m3uUrl)
                                            if (extracted != null) {
                                                xtHost = extracted.host
                                                xtUser = extracted.user
                                                xtPass = extracted.pass
                                                serverMode = 0
                                                prefs.edit()
                                                    .putString("xt_host", xtHost)
                                                    .putString("xt_user", xtUser)
                                                    .putString("xt_pass", xtPass)
                                                    .apply()
                                                mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "live")
                                            } else {
                                                val raw = M3uParser.loadFromUrl(m3uUrl)
                                                mediaList = raw.mapIndexed { idx, ch ->
                                                    MediaItemModel("$idx", ch.name, ch.url, ch.group)
                                                }
                                            }
                                            isLoading = false
                                            if (mediaList.isNotEmpty()) selectedTab = 1
                                            else statusMessage = "لم يتم العثور على قنوات في هذا الرابط"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("تحليل وتشغيل الرابط تلقائياً", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (isLoading) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else if (statusMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = statusMessage, color = Color(0xFF38BDF8), fontSize = 13.sp)
                        }
                    }
                } else {
                    // شريط تصنيف الباقات الأفقي العصري
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }

                    // شريط البحث
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث في ${filteredList.size} قناة...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredList) { item ->
                                val isPlaying = currentMedia?.url == item.url
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isPlaying) Brush.horizontalGradient(
                                                listOf(Color(0xFF1D4ED8), Color(0xFF0284C7))
                                            ) else Brush.horizontalGradient(
                                                listOf(Color(0xFF131B2E), Color(0xFF1E293B))
                                            )
                                        )
                                        .clickable { playStream(item) }
                                        .padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x3338BDF8)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = item.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = item.category,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 12.sp
                                            )
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
}
    .putString("xt_host", xtHost)
                                        .putString("xt_user", xtUser)
                                        .putString("xt_pass", xtPass)
                                        .apply()
                                    scope.launch {
                                        isLoading = true
                                        mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "live")
                                        isLoading = false
                                        if (mediaList.isNotEmpty()) selectedTab = 1
                                        else statusMessage = "فشل الاتصال بسيرفر Xtream، تأكد من البيانات"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ والاتصال بـ Xtream")
                            }
                        }

                        1 -> {
                            OutlinedTextField(
                                value = stPortal,
                                onValueChange = { stPortal = it },
                                label = { Text("رابط البورتال (http://portal.com/c)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = stMac,
                                onValueChange = { stMac = it },
                                label = { Text("عنوان الماك (00:1A:79:XX:XX:XX)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    prefs.edit()
                                        .putString("st_portal", stPortal)
                                        .putString("st_mac", stMac)
                                        .apply()
                                    scope.launch {
                                        isLoading = true
                                        mediaList = ServersEngine.loadStalker(stPortal, stMac)
                                        isLoading = false
                                        if (mediaList.isNotEmpty()) selectedTab = 1
                                        else statusMessage = "فشل الاتصال ببورتال Stalker"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ والاتصال بـ Stalker")
                            }
                        }

                        2 -> {
                            OutlinedTextField(
                                value = m3uUrl,
                                onValueChange = { m3uUrl = it },
                                label = { Text("رابط قائمة M3U / M3U8") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    prefs.edit().putString("m3u_url", m3uUrl).apply()
                                    scope.launch {
                                        isLoading = true
                                        val raw = M3uParser.loadFromUrl(m3uUrl)
                                        mediaList = raw.mapIndexed { idx, ch ->
                                            MediaItemModel("$idx", ch.name, ch.url, ch.group)
                                        }
                                        isLoading = false
                                        if (mediaList.isNotEmpty()) selectedTab = 1
                                        else statusMessage = "تعذر جلب قائمة M3U"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ وجلب القائمة")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        Text(text = statusMessage, color = Color(0xFF38BDF8), fontSize = 13.sp)
                    }
                }
            } else {
                // شاشة عرض القنوات / الأفلام / المسلسلات
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في (${filteredList.size}) عنصر...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredList) { item ->
                            val isPlaying = currentMedia?.url == item.url
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPlaying) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { playStream(item) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isPlaying) Color(0xFF38BDF8) else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = item.category,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp
                                        )
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
