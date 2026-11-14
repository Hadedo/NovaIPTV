package com.hadedo.novaiptv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import org.json.JSONArray
import org.json.JSONObject

private val NeonPurple = Color(0xFF7C6DFA)
private val DeepBlack = Color(0xFF0A0A0E)
private val CardDark = Color(0xFF14141C)
private val GreenBadge = Color(0xFF1E3A2F)
private val GreenText = Color(0xFF4ADE80)

class MainActivity : ComponentActivity() {
    var isInPipState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = NeonPurple, background = DeepBlack, surface = CardDark)) {
                NovaIptvNeonApp(isInPipMode = isInPipState)
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

fun loadSavedServers(prefs: android.content.SharedPreferences): List<SavedServer> {
    val raw = prefs.getString("saved_servers_json", "[]") ?: "[]"
    val arr = JSONArray(raw)
    val list = mutableListOf<SavedServer>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        list.add(SavedServer(o.getString("id"), o.getString("title"), o.getString("type"), o.getString("host"), o.optString("user"), o.optString("pass"), o.optString("country", "🌐"), o.optLong("ping", 0)))
    }
    return list
}

fun saveServersList(prefs: android.content.SharedPreferences, list: List<SavedServer>) {
    val arr = JSONArray()
    list.forEach { s ->
        arr.put(JSONObject().apply {
            put("id", s.id); put("title", s.title); put("type", s.type); put("host", s.host); put("user", s.user); put("pass", s.pass); put("country", s.countryCode); put("ping", s.pingMs)
        })
    }
    prefs.edit().putString("saved_servers_json", arr.toString()).apply()
}

@OptIn(UnstableApi::class)
@Composable
fun NovaIptvNeonApp(isInPipMode: Boolean) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("nova_neon_prefs", Context.MODE_PRIVATE) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf<SavedServer?>(null) }

    var servers by remember { mutableStateOf<List<SavedServer>>(loadSavedServers(prefs)) }
    var activeServerId by remember { mutableStateOf(prefs.getString("active_server_id", "") ?: "") }
    val activeServer = servers.find { it.id == activeServerId }

    var gesturesEnabled by remember { mutableStateOf(prefs.getBoolean("gestures_enabled", true)) }
    var expiryAlertEnabled by remember { mutableStateOf(prefs.getBoolean("expiry_alert", true)) }
    var userAgent by remember { mutableStateOf(prefs.getString("user_agent", "IPTVSmartersPro") ?: "IPTVSmartersPro") }
    ServersEngine.currentUserAgent = userAgent

    var isFullScreen by remember { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(true) }
    var resizeIdx by remember { mutableIntStateOf(0) }
    val resizeModes = listOf(Pair(AspectRatioFrameLayout.RESIZE_MODE_FIT, "طبيعي"), Pair(AspectRatioFrameLayout.RESIZE_MODE_FILL, "ملء"), Pair(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "تكبير"))

    var accountInfo by remember { mutableStateOf<AccountInfo?>(null) }
    var mediaList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var favoriteIds by remember { mutableStateOf(prefs.getStringSet("fav_ids", emptySet()) ?: emptySet()) }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }
    var currentMedia by remember { mutableStateOf<MediaItemModel?>(null) }
    var videoResolution by remember { mutableStateOf("") }
    var gestureIndicator by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0) {
                        val tag = when { videoSize.height >= 2160 -> "4K HDR"; videoSize.height >= 1080 -> "1080p FHD"; videoSize.height >= 720 -> "720p HD"; else -> "${videoSize.height}p" }
                        videoResolution = "$tag • LIVE"
                    }
                }
            })
        }
    }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    fun connectToServer(srv: SavedServer, contentType: String = "live", switchTab: Boolean = true) {
        activeServerId = srv.id
        prefs.edit().putString("active_server_id", srv.id).apply()
        scope.launch {
            isLoading = true
            val (country, ping) = ServersEngine.detectServerCountryAndPing(srv.host)
            val updatedList = servers.map { if (it.id == srv.id) it.copy(countryCode = country, pingMs = ping) else it }
            servers = updatedList
            saveServersList(prefs, updatedList)

            if (srv.type == "xtream") {
                accountInfo = ServersEngine.getAccountInfo(srv.host, srv.user, srv.pass)
                mediaList = ServersEngine.loadXtream(srv.host, srv.user, srv.pass, contentType)
            } else if (srv.type == "stalker") {
                accountInfo = null
                mediaList = ServersEngine.loadStalker(srv.host, srv.user)
            } else {
                accountInfo = null
                mediaList = M3uParser.loadFromUrl(srv.host).mapIndexed { idx, ch -> MediaItemModel("$idx", ch.name, ch.url, ch.group) }
            }
            isLoading = false
            if (switchTab && mediaList.isNotEmpty()) selectedTab = 1
        }
    }

    LaunchedEffect(Unit) {
        activeServer?.let { connectToServer(it, "live", false) }
    }

    LaunchedEffect(isFullScreen) {
        activity?.let { act ->
            if (isFullScreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    act.window.insetsController?.let { it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()); it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE }
                } else @Suppress("DEPRECATION") { act.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY }
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) act.window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                else @Suppress("DEPRECATION") { act.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE }
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

    fun openExternalPlayer(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.parse(url), "video/*"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
        }
    }

    val categories = remember(mediaList) { listOf("الكل") + mediaList.map { it.category }.distinct() }
    val filteredList = remember(mediaList, selectedCategory, searchQuery, selectedTab, favoriteIds) {
        mediaList.filter { item ->
            val matchFav = if (selectedTab == 4) favoriteIds.contains(item.id) else true
            val matchCat = if (selectedTab == 4) true else (selectedCategory == "الكل" || item.category == selectedCategory)
            matchFav && matchCat && (searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true))
        }
    }

    @Composable
    fun PlayerViewport(modifier: Modifier) {
        Box(
            modifier = modifier.background(Color.Black).pointerInput(gesturesEnabled) {
                if (gesturesEnabled) {
                    detectVerticalDragGestures(
                        onDragEnd = { gestureIndicator = "" },
                        onVerticalDrag = { change, dragAmount ->
                            if (change.position.x > size.width / 2f) {
                                val maxV = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val curV = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val newV = (curV + (if (dragAmount < -12) 1 else if (dragAmount > 12) -1 else 0)).coerceIn(0, maxV)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newV, 0)
                                gestureIndicator = "الصوت: ${(newV * 100) / maxV}%"
                            } else {
                                activity?.window?.let { win ->
                                    val lp = win.attributes
                                    val curB = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
                                    val newB = (curB - dragAmount / 600f).coerceIn(0.05f, 1f)
                                    lp.screenBrightness = newB; win.attributes = lp
                                    gestureIndicator = "السطوع: ${(newB * 100).toInt()}%"
                                }
                            }
                        }
                    )
                }
            }
        ) {
            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = !isInPipMode; resizeMode = resizeModes[resizeIdx].first; setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { v -> showOverlay = (v == View.VISIBLE) }) } },
                update = { view -> view.useController = !isInPipMode; view.resizeMode = resizeModes[resizeIdx].first },
                modifier = Modifier.fillMaxSize()
            )
            if (gestureIndicator.isNotEmpty() && !isInPipMode) {
                Box(modifier = Modifier.align(Alignment.Center).background(Color(0xCC14141C), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(gestureIndicator, color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
            if (!isInPipMode && showOverlay) {
                if (videoResolution.isNotEmpty()) {
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(10.dp).background(Color(0xBB14141C), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(videoResolution, color = GreenText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).background(Color(0xCC14141C), RoundedCornerShape(20.dp)).border(1.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    currentMedia?.let { m -> Text("خارجي", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { openExternalPlayer(m.url) }) }
                    Text(resizeModes[resizeIdx].second, color = NeonPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { resizeIdx = (resizeIdx + 1) % resizeModes.size })
                    Text("عائمة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { enterPip() })
                    Text(if (isFullScreen) "تصغير" else "تكبير", color = GreenText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { isFullScreen = !isFullScreen })
                }
            }
        }
    }

    if (isInPipMode || isFullScreen) {
        PlayerViewport(Modifier.fillMaxSize())
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0E0E14)) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("الرئيسية", fontSize = 11.sp) })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1; selectedCategory = "الكل"; activeServer?.let { connectToServer(it, "live", false) } }, icon = { Icon(Icons.Default.Tv, null) }, label = { Text("البث المباشر", fontSize = 11.sp) })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2; selectedCategory = "الكل"; activeServer?.let { connectToServer(it, "vod", false) } }, icon = { Icon(Icons.Default.Movie, null) }, label = { Text("أفلام", fontSize = 11.sp) })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3; selectedCategory = "الكل"; activeServer?.let { connectToServer(it, "series", false) } }, icon = { Icon(Icons.Default.VideoLibrary, null) }, label = { Text("مسلسلات", fontSize = 11.sp) })
                NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4 }, icon = { Icon(Icons.Default.FavoriteBorder, null) }, label = { Text("المفضلة", fontSize = 11.sp) })
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(DeepBlack)) {
            if (currentMedia != null || selectedTab != 0) {
                PlayerViewport(Modifier.fillMaxWidth().height(215.dp))
            }

            if (selectedTab == 0) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showSettingsSheet = true }) { Icon(Icons.Default.Settings, null, tint = Color.White) }
                        Text("الاتصالات", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    if (accountInfo != null && expiryAlertEnabled) {
                        Card(colors = CardDefaults.cardColors(containerColor = CardDark), border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.4f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("انتهاء الاشتراك: ${accountInfo?.expDate}", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                Text("الأجهزة: ${accountInfo?.activeCons}/${accountInfo?.maxCons}", color = GreenText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = { editingServer = null; showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة اتصال", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                        items(servers) { srv ->
                            val isActive = srv.id == activeServerId
                            var menuExpanded by remember { mutableStateOf(false) }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardDark),
                                border = BorderStroke(1.dp, if (isActive) NeonPurple else Color(0x1FFFFFFF)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().clickable { connectToServer(srv, "live", true) }
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box {
                                        IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, null, tint = Color.LightGray) }
                                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                            DropdownMenuItem(text = { Text("تشخيص الشبكة / فحص الدولة") }, onClick = { menuExpanded = false; connectToServer(srv, "live", false) })
                                            DropdownMenuItem(text = { Text("تعديل") }, onClick = { menuExpanded = false; editingServer = srv; showAddDialog = true })
                                            DropdownMenuItem(text = { Text("حذف", color = Color(0xFFF87171)) }, onClick = {
                                                menuExpanded = false
                                                val updated = servers.filter { it.id != srv.id }
                                                servers = updated; saveServersList(prefs, updated)
                                            })
                                        }
                                    }

                                    if (isActive) {
                                        Box(modifier = Modifier.background(GreenBadge, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                            Text("نشط", color = GreenText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(srv.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(if (srv.type == "xtream") "Xtream Codes" else if (srv.type == "stalker") "Stalker Portal" else "M3U Playlist", color = Color.Gray, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(modifier = Modifier.background(Color(0xFF22222E), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                                Text(srv.countryCode, color = Color.White, fontSize = 11.sp)
                                            }
                                            Box(modifier = Modifier.background(GreenBadge, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                                Text(if (srv.pingMs > 0) "⚡ وصول مباشر (${srv.pingMs}ms)" else "⚡ وصول مباشر", color = GreenText, fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))
                                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(NeonPurple.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Storage, null, tint = NeonPurple)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (selectedTab != 4) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat, fontSize = 12.sp) })
                        }
                    }
                }
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("ابحث في ${filteredList.size} عنصر...", fontSize = 13.sp) }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                Spacer(modifier = Modifier.height(6.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NeonPurple) }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredList) { item ->
                            val isPlaying = currentMedia?.url == item.url
                            val isFav = favoriteIds.contains(item.id)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isPlaying) NeonPurple.copy(alpha = 0.25f) else CardDark)
                                    .border(1.dp, if (isPlaying) NeonPurple else Color(0x18FFFFFF), RoundedCornerShape(14.dp))
                                    .clickable { playStream(item) }
                                    .padding(8.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().height(if (selectedTab == 1) 75.dp else 135.dp)) {
                                    AsyncImage(
                                        model = item.logo,
                                        contentDescription = item.name,
                                        contentScale = if (selectedTab == 1) ContentScale.Fit else ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                                    )
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isFav) NeonPurple else Color.Gray,
                                        modifier = Modifier.align(Alignment.TopEnd).size(20.dp).clickable { toggleFavorite(item.id) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(item.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var titleInput by remember { mutableStateOf(editingServer?.title ?: "") }
        var typeInput by remember { mutableStateOf(editingServer?.type ?: "xtream") }
        var hostInput by remember { mutableStateOf(editingServer?.host ?: "") }
        var userInput by remember { mutableStateOf(editingServer?.user ?: "") }
        var passInput by remember { mutableStateOf(editingServer?.pass ?: "") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CardDark,
            title = { Text(if (editingServer == null) "إضافة اتصال جديد" else "تعديل الاتصال", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("xtream" to "Xtream", "stalker" to "Stalker", "m3u" to "رابط").forEach { (k, label) ->
                            FilterChip(selected = typeInput == k, onClick = { typeInput = k }, label = { Text(label, fontSize = 11.sp) })
                        }
                    }
                    OutlinedTextField(value = titleInput, onValueChange = { titleInput = it }, label = { Text("اسم الاشتراك (مثلاً: ماارفيل)") }, singleLine = true)
                    OutlinedTextField(value = hostInput, onValueChange = { hostInput = it }, label = { Text(if (typeInput == "stalker") "رابط البورتال" else "رابط السيرفر أو M3U") }, singleLine = true)
                    if (typeInput != "m3u") {
                        OutlinedTextField(value = userInput, onValueChange = { userInput = it }, label = { Text(if (typeInput == "stalker") "عنوان الماك (MAC)" else "اسم المستخدم") }, singleLine = true)
                    }
                    if (typeInput == "xtream") {
                        OutlinedTextField(value = passInput, onValueChange = { passInput = it }, label = { Text("كلمة المرور") }, singleLine = true)
                    }
                }
            },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = NeonPurple), onClick = {
                    val extracted = if (typeInput == "m3u") ServersEngine.parseXtreamFromUrl(hostInput) else null
                    val finalType = if (extracted != null) "xtream" else typeInput
                    val finalHost = extracted?.first ?: hostInput.trim()
                    val finalUser = extracted?.second ?: userInput.trim()
                    val finalPass = extracted?.third ?: passInput.trim()
                    val finalTitle = titleInput.ifBlank { Uri.parse(finalHost).host ?: "سيرفر جديد" }

                    val newSrv = SavedServer(editingServer?.id ?: System.currentTimeMillis().toString(), finalTitle, finalType, finalHost, finalUser, finalPass)
                    val updated = if (editingServer == null) servers + newSrv else servers.map { if (it.id == newSrv.id) newSrv else it }
                    servers = updated; saveServersList(prefs, updated)
                    showAddDialog = false
                    connectToServer(newSrv, "live", true)
                }) { Text("حفظ واتصال", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("إلغاء", color = Color.Gray) } }
        )
    }

    if (showSettingsSheet) {
        AlertDialog(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = CardDark,
            title = { Text("الإعدادات المتقدمة", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("التحكم بالإيماءات (الصوت/السطوع)", color = Color.White, fontSize = 13.sp)
                        Switch(checked = gesturesEnabled, onCheckedChange = { gesturesEnabled = it; prefs.edit().putBoolean("gestures_enabled", it).apply() })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("تنبيه انتهاء الاشتراك", color = Color.White, fontSize = 13.sp)
                        Switch(checked = expiryAlertEnabled, onCheckedChange = { expiryAlertEnabled = it; prefs.edit().putBoolean("expiry_alert", it).apply() })
                    }
                    Text("User-Agent (تبديل الهوية):", color = NeonPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("IPTVSmartersPro", "VLC/3.0", "TiviMate").forEach { ua ->
                            FilterChip(selected = userAgent == ua, onClick = {
                                userAgent = ua; ServersEngine.currentUserAgent = ua; prefs.edit().putString("user_agent", ua).apply()
                            }, label = { Text(ua, fontSize = 10.sp) })
                        }
                    }
                }
            },
            confirmButton = { Button(colors = ButtonDefaults.buttonColors(containerColor = NeonPurple), onClick = { showSettingsSheet = false }) { Text("تم", color = Color.White) } }
        )
    }
}
// END
