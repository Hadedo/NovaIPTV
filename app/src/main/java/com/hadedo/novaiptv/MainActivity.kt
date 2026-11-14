package com.hadedo.novaiptv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
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
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF38BDF8),
                    background = Color(0xFF060913),
                    surface = Color(0xFF0F172A)
                )
            ) {
                NovaIptvUltraApp(isInPipMode = isInPipState)
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipState = isInPictureInPictureMode
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
fun NovaIptvUltraApp(isInPipMode: Boolean) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("nova_prefs", Context.MODE_PRIVATE) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var serverMode by remember { mutableIntStateOf(0) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showOverlayControls by remember { mutableStateOf(true) }
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
            if (isFullScreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    act.window.insetsController?.let {
                        it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                        it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    act.window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
                }
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    act.window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                } else {
                    @Suppress("DEPRECATION")
                    act.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
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

    if (isInPipMode || isFullScreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = !isInPipMode
                        resizeMode = resizeModes[resizeModeIndex].first
                        setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                            showOverlayControls = (visibility == View.VISIBLE)
                        })
                    }
                },
                update = { view ->
                    view.useController = !isInPipMode
                    view.resizeMode = resizeModes[resizeModeIndex].first
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!isInPipMode && showOverlayControls) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .background(Color(0xCC0F172A), RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "أبعاد: ${resizeModes[resizeModeIndex].second}",
                        color = Color(0xFF38BDF8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                        }
                    )
                    Text(
                        text = "نافذة عائمة",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { enterPip() }
                    )
                    Text(
                        text = "تصغير",
                        color = Color(0xFF4ADE80),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { isFullScreen = false }
                    )
                }
            }
        }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0B1120),
                tonalElevation = 8.dp
            ) {
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
                    icon = { Icon(Icons.Default.PlayArrow, null) },
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
                    icon = { Icon(Icons.Default.PlayArrow, null) },
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
                    icon = { Icon(Icons.Default.PlayArrow, null) },
                    label = { Text("مسلسلات") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF060913))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            resizeMode = resizeModes[resizeModeIndex].first
                            setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                                showOverlayControls = (visibility == View.VISIBLE)
                            })
                        }
                    },
                    update = { view ->
                        view.resizeMode = resizeModes[resizeModeIndex].first
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (showOverlayControls) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .background(Color(0xBB0F172A), RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "أبعاد: ${resizeModes[resizeModeIndex].second}",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                            }
                        )
                        Text(
                            text = "نافذة عائمة",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { enterPip() }
                        )
                        Text(
                            text = "ملء الشاشة",
                            color = Color(0xFF4ADE80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { isFullScreen = true }
                        )
                    }
                }
            }

            if (selectedTab == 0) {
