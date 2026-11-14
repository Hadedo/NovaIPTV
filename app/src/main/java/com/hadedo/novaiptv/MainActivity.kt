package com.hadedo.novaiptv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Rational
import android.util.TypedValue
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    var isInPipState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            NovaIptvUltimateApp(isInPipMode = isInPipState)
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
        list.add(
            SavedServer(
                o.getString("id"),
                o.getString("title"),
                o.getString("type"),
                o.getString("host"),
                o.optString("user"),
                o.optString("pass"),
                o.optString("country", "🌐"),
                o.optLong("ping", 0)
            )
        )
    }
    return list
}

fun saveServersList(prefs: android.content.SharedPreferences, list: List<SavedServer>) {
    val arr = JSONArray()
    list.forEach { s ->
        arr.put(JSONObject().apply {
            put("id", s.id); put("title", s.title); put("type", s.type)
            put("host", s.host); put("user", s.user); put("pass", s.pass)
            put("country", s.countryCode); put("ping", s.pingMs)
        })
    }
    prefs.edit().putString("saved_servers_json", arr.toString()).apply()
}

data class AppStrings(
    val home: String, val live: String, val sports: String, val cinema: String, val hub: String,
    val continueWatching: String, val recentChannels: String, val myFolders: String, val platforms: String,
    val searchHint: String, val all: String, val movies: String, val series: String,
    val multiScreen: String, val kidsMode: String, val settings: String, val addConnection: String,
    val refreshLists: String, val seasons: String, val chooseQuality: String
)

fun getStrings(lang: String): AppStrings = when (lang) {
    "en" -> AppStrings("Home", "Live TV", "Sports", "Cinema", "Settings", "Continue Watching", "Recently Watched", "My Folders", "Platforms", "Search channels, movies...", "All", "Movies", "Series", "Multi-Screen (4)", "Kids Mode", "Settings", "Add Connection", "Sync Now", "Seasons & Episodes", "Select Quality")
    "fr" -> AppStrings("Accueil", "Direct", "Sports", "Cinéma", "Paramètres", "Reprendre", "Chaînes récentes", "Mes Dossiers", "Plateformes", "Rechercher...", "Tout", "Films", "Séries", "Multi-Écran (4)", "Mode Enfants", "Paramètres", "Ajouter", "Actualiser", "Saisons & Épisodes", "Choisir la qualité")
    "tr" -> AppStrings("Ana Sayfa", "Canlı TV", "Spor", "Sinema", "Ayarlar", "İzlemeye Devam Et", "Son İzlenenler", "Klasörlerim", "Platformlar", "Ara...", "Tümü", "Filmler", "Diziler", "Çoklu Ekran (4)", "Çocuk Modu", "Ayarlar", "Bağlantı Ekle", "Yenile", "Sezonlar ve Bölümler", "Kalite Seçin")
    "ku" -> AppStrings("سەرەکی", "ڕاستەوخۆ", "وەرزش", "سینەما", "ڕێکخستنەکان", "بەردەوامبە لە بینین", "دوایین کەناڵەکان", "فۆڵدەرەکانم", "پلاتفۆرمەکان", "گەڕان...", "هەموو", "فیلمەکان", "زنجیرەکان", "فرە شاشە (4)", "دۆخی منداڵان", "ڕێکخستنەکان", "زیادکردنی هەژمار", "نوێکردنەوە", "وەرز و ئەڵقەکان", "کوالێتی هەڵبژێرە")
    else -> AppStrings("الرئيسية", "مباشر", "الرياضة", "السينما", "الإعدادات", "واصل المشاهدة", "شاهدته مؤخراً", "مجلداتي", "المنصات", "ابحث في القنوات والأفلام...", "الكل", "أفلام", "مسلسلات", "تعدد الشاشات (4)", "وضع الأطفال", "الإعدادات", "إضافة اشتراك", "تحديث القوائم", "المواسم والحلقات", "اختر جودة البث")
}

@OptIn(UnstableApi::class)
@Composable
fun NovaIptvUltimateApp(isInPipMode: Boolean) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val prefs = remember { context.getSharedPreferences("nova_neon_prefs", Context.MODE_PRIVATE) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var appLang by remember { mutableStateOf(prefs.getString("app_lang", "ar") ?: "ar") }
    val str = remember(appLang) { getStrings(appLang) }
    val layoutDir = if (appLang == "ar" || appLang == "ku") LayoutDirection.Rtl else LayoutDirection.Ltr

    var accentIdx by remember { mutableIntStateOf(prefs.getInt("accent_idx", 0)) }
    val accentOptions = listOf(
        Pair("بنفسجي نيون", Color(0xFF7C6DFA)),
        Pair("أزرق كهربائي", Color(0xFF3B82F6)),
        Pair("زمردي", Color(0xFF10B981)),
        Pair("عنبري ذهبي", Color(0xFFF59E0B)),
        Pair("أحمر سينمائي", Color(0xFFEF4444)),
        Pair("أرجواني", Color(0xFFEC4899))
    )
    val accentColor = accentOptions.getOrElse(accentIdx) { accentOptions[0] }.second

    var bgIdx by remember { mutableIntStateOf(prefs.getInt("bg_idx", 0)) }
    val bgOptions = listOf(
        Triple("AMOLED أسود", Color(0xFF000000), Color(0xFF0D0D12)),
        Triple("أسود عميق", Color(0xFF0A0A0E), Color(0xFF14141C)),
        Triple("أزرق ليلي", Color(0xFF090D16), Color(0xFF111827))
    )
    val bgColor = bgOptions.getOrElse(bgIdx) { bgOptions[0] }.second
    val cardColor = bgOptions.getOrElse(bgIdx) { bgOptions[0] }.third
    val greenText = Color(0xFF4ADE80)
    val greenBadge = Color(0xFF1E3A2F)

    var startupTab by remember { mutableIntStateOf(prefs.getInt("startup_tab", 0)) }
    var selectedTab by remember { mutableIntStateOf(startupTab) }
    var turboMode by remember { mutableStateOf(prefs.getBoolean("turbo_mode", false)) }
    var gesturesEnabled by remember { mutableStateOf(prefs.getBoolean("gestures_enabled", true)) }
    var autoplayNextEp by remember { mutableStateOf(prefs.getBoolean("autoplay_next_ep", true)) }
    var decoderMode by remember { mutableStateOf(prefs.getString("decoder_mode", "auto") ?: "auto") }
    var bufferProfile by remember { mutableStateOf(prefs.getString("buffer_profile", "balanced") ?: "balanced") }
    var streamFmt by remember { mutableStateOf(prefs.getString("stream_fmt", "ts") ?: "ts") }
    var sameHost by remember { mutableStateOf(prefs.getBoolean("same_host", false)) }
    var userAgent by remember { mutableStateOf(prefs.getString("user_agent", "IPTVSmartersPro") ?: "IPTVSmartersPro") }
    var subFontSize by remember { mutableFloatStateOf(prefs.getFloat("sub_font_size", 18f)) }
    var subColorYellow by remember { mutableStateOf(prefs.getBoolean("sub_yellow", false)) }

    ServersEngine.currentUserAgent = userAgent
    ServersEngine.streamFormat = streamFmt
    ServersEngine.sameHostProxy = sameHost

    var kidsMode by remember { mutableStateOf(false) }
    var parentalPin by remember { mutableStateOf(prefs.getString("parental_pin", "0000") ?: "0000") }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinActionCallback by remember { mutableStateOf<() -> Unit>({}) }
    var hiddenCategories by remember { mutableStateOf(prefs.getStringSet("hidden_cats", emptySet()) ?: emptySet()) }

    var servers by remember { mutableStateOf(loadSavedServers(prefs)) }
    var activeServerId by remember { mutableStateOf(prefs.getString("active_server_id", "") ?: "") }
    val activeServer = servers.find { it.id == activeServerId }
    var accountInfo by remember { mutableStateOf<AccountInfo?>(null) }

    var liveList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var vodList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var seriesList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var favoriteIds by remember { mutableStateOf(prefs.getStringSet("fav_ids", emptySet()) ?: emptySet()) }
    var watchHistory by remember { mutableStateOf(AppStorage.getWatchHistory(context)) }
    var recentChannels by remember { mutableStateOf(AppStorage.getRecentChannels(context)) }
    var customFolders by remember { mutableStateOf(AppStorage.getCustomFolders(context)) }

    var currentMedia by remember { mutableStateOf<MediaItemModel?>(null) }
    var currentEpg by remember { mutableStateOf<EpgProgram?>(null) }
    var videoResolution by remember { mutableStateOf("") }
    var isFullScreen by remember { mutableStateOf(false) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(true) }
    var gestureIndicator by remember { mutableStateOf("") }
    var sleepMinutesLeft by remember { mutableIntStateOf(0) }
    var resizeIdx by remember { mutableIntStateOf(0) }
    val resizeModes = listOf(
        Pair(AspectRatioFrameLayout.RESIZE_MODE_FIT, "طبيعي"),
        Pair(AspectRatioFrameLayout.RESIZE_MODE_FILL, "ملء"),
        Pair(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "تكبير")
    )

    var selectedQualityGroup by remember { mutableStateOf<GroupedSportsChannel?>(null) }
    var selectedSeriesItem by remember { mutableStateOf<MediaItemModel?>(null) }
    var loadedSeriesDetails by remember { mutableStateOf<SeriesDetails?>(null) }
    var activeSeasonNum by remember { mutableIntStateOf(1) }
    var activeEpisodesQueue by remember { mutableStateOf<List<EpisodeModel>>(emptyList()) }
    var currentEpisodeIdx by remember { mutableIntStateOf(-1) }
    var nextEpisodeCountdown by remember { mutableIntStateOf(0) }

    var isMultiScreenMode by remember { mutableStateOf(false) }
    var multiChannels by remember { mutableStateOf<List<MediaItemModel?>>(listOf(null, null, null, null)) }
    var activeAudioSlot by remember { mutableIntStateOf(0) }
    var pickingForSlot by remember { mutableIntStateOf(-1) }

    var showAccountMenu by remember { mutableStateOf(false) }
    var showAddServerDialog by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf<SavedServer?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderTargetItem by remember { mutableStateOf<MediaItemModel?>(null) }
    var showCategoriesManager by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    var cinemaSubTab by remember { mutableIntStateOf(0) }
    var sportsSubFilter by remember { mutableIntStateOf(0) }
    var selectedLangFilter by remember { mutableStateOf("الكل") }
    var selectedPlatformFilter by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) searchQuery = spoken
        }
    }

    val exoPlayer = remember(decoderMode, bufferProfile, userAgent) {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(
                when (decoderMode) {
                    "sw" -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                    "hw" -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                    else -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                }
            )
        }
        val (minBuf, maxBuf, playbackBuf) = when (bufferProfile) {
            "fast" -> Triple(1500, 5000, 800)
            "antifreeze" -> Triple(15000, 45000, 5000)
            else -> Triple(5000, 15000, 2000)
        }
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(minBuf, maxBuf, playbackBuf, playbackBuf)
            .build()
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)

        ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build().apply {
                playWhenReady = true
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
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

            override fun onPlayerError(error: PlaybackException) {
                videoResolution = "إعادة ربط البث..."
                exoPlayer.prepare()
                exoPlayer.play()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && autoplayNextEp && currentEpisodeIdx >= 0 && currentEpisodeIdx + 1 < activeEpisodesQueue.size) {
                    nextEpisodeCountdown = 5
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(currentMedia, sleepMinutesLeft, nextEpisodeCountdown) {
        while (true) {
            delay(1000L)
            val m = currentMedia
            if (m != null && m.type != "live" && exoPlayer.isPlaying && exoPlayer.duration > 0) {
                AppStorage.saveWatchProgress(
                    context,
                    WatchProgress(m.id, m.name, m.logo, m.url, m.type, exoPlayer.currentPosition, exoPlayer.duration, m.category)
                )
                watchHistory = AppStorage.getWatchHistory(context)
            }
            if (nextEpisodeCountdown > 0) {
                nextEpisodeCountdown -= 1
                if (nextEpisodeCountdown == 0 && currentEpisodeIdx + 1 < activeEpisodesQueue.size) {
                    val nextEp = activeEpisodesQueue[currentEpisodeIdx + 1]
                    currentEpisodeIdx += 1
                    val nextModel = MediaItemModel(nextEp.id, nextEp.title, nextEp.url, selectedSeriesItem?.name ?: "مسلسل", selectedSeriesItem?.logo ?: "", "series")
                    currentMedia = nextModel
                    exoPlayer.setMediaItem(MediaItem.fromUri(ServersEngine.resolveStreamUrl(nextModel.url, activeServer?.host ?: "")))
                    exoPlayer.prepare()
                    exoPlayer.play()
                }
            }
        }
    }

    LaunchedEffect(sleepMinutesLeft) {
        if (sleepMinutesLeft > 0) {
            delay(60_000L)
            sleepMinutesLeft -= 1
            if (sleepMinutesLeft == 0) {
                exoPlayer.pause()
                Toast.makeText(context, "تم إيقاف البث بواسطة مؤقت النوم", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun syncServerData(srv: SavedServer, forceRefresh: Boolean = false) {
        activeServerId = srv.id
        prefs.edit().putString("active_server_id", srv.id).apply()

        scope.launch {
            val cachedLive = if (!forceRefresh) AppStorage.loadMediaCache(context, srv.id, "live") else null
            val cachedVod = if (!forceRefresh) AppStorage.loadMediaCache(context, srv.id, "vod") else null
            val cachedSeries = if (!forceRefresh) AppStorage.loadMediaCache(context, srv.id, "series") else null

            if (cachedLive != null) {
                liveList = withContext(Dispatchers.Default) { ServersEngine.enrichCategoryQualities(cachedLive) }
            }
            if (cachedVod != null) vodList = cachedVod
            if (cachedSeries != null) seriesList = cachedSeries

            if (cachedLive == null || cachedVod == null || cachedSeries == null || forceRefresh) {
                isLoading = cachedLive == null
                val (country, ping) = ServersEngine.detectServerCountryAndPing(srv.host)
                val updatedServers = servers.map { if (it.id == srv.id) it.copy(countryCode = country, pingMs = ping) else it }
                servers = updatedServers
                saveServersList(prefs, updatedServers)

                if (srv.type == "xtream") {
                    accountInfo = ServersEngine.getAccountInfo(srv.host, srv.user, srv.pass)
                    val freshLive = ServersEngine.loadXtream(srv.host, srv.user, srv.pass, "live")
                    if (freshLive.isNotEmpty()) {
                        liveList = freshLive
                        AppStorage.saveMediaCache(context, srv.id, "live", freshLive)
                    }
                    isLoading = false

                    val freshVod = ServersEngine.loadXtream(srv.host, srv.user, srv.pass, "vod")
                    if (freshVod.isNotEmpty()) {
                        vodList = freshVod
                        AppStorage.saveMediaCache(context, srv.id, "vod", freshVod)
                    }
                    val freshSeries = ServersEngine.loadXtream(srv.host, srv.user, srv.pass, "series")
                    if (freshSeries.isNotEmpty()) {
                        seriesList = freshSeries
                        AppStorage.saveMediaCache(context, srv.id, "series", freshSeries)
                    }
                } else if (srv.type == "stalker") {
                    val freshStalker = ServersEngine.loadStalker(srv.host, srv.user)
                    liveList = freshStalker
                    AppStorage.saveMediaCache(context, srv.id, "live", freshStalker)
                    isLoading = false
                } else {
                    val rawM3u = M3uParser.loadFromUrl(srv.host)
                        .filter { !ServersEngine.isSeparatorChannel(it.name) }
                        .mapIndexed { idx, ch ->
                            MediaItemModel("$idx", ServersEngine.cleanMediaName(ch.name), ch.url, ch.group)
                        }
                    val freshM3u = withContext(Dispatchers.Default) { ServersEngine.enrichCategoryQualities(rawM3u) }
                    liveList = freshM3u
                    AppStorage.saveMediaCache(context, srv.id, "live", freshM3u)
                    isLoading = false
                }
            } else if (srv.type == "xtream") {
                accountInfo = ServersEngine.getAccountInfo(srv.host, srv.user, srv.pass)
            }
        }
    }

    LaunchedEffect(Unit) {
        activeServer?.let { syncServerData(it, false) }
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
                } else @Suppress("DEPRECATION") {
                    act.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                }
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    act.window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                } else @Suppress("DEPRECATION") {
                    act.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
    }

    BackHandler(enabled = isFullScreen || isMultiScreenMode) {
        if (isFullScreen) isFullScreen = false
        else if (isMultiScreenMode) isMultiScreenMode = false
    }

    fun startPlayback(item: MediaItemModel, resumeFromSaved: Boolean = true) {
        if (pickingForSlot in 0..3) {
            val updatedSlots = multiChannels.toMutableList()
            updatedSlots[pickingForSlot] = item
            multiChannels = updatedSlots
            pickingForSlot = -1
            isMultiScreenMode = true
            return
        }

        if (item.type == "series" && item.url.startsWith("series://")) {
            selectedSeriesItem = item
            loadedSeriesDetails = null
            scope.launch {
                val details = ServersEngine.getSeriesDetails(item.url)
                loadedSeriesDetails = details
                activeSeasonNum = details?.seasons?.keys?.firstOrNull() ?: 1
            }
            return
        }

        currentMedia = item
        currentEpg = null
        videoResolution = "جاري الاتصال..."
        val resolvedUrl = ServersEngine.resolveStreamUrl(item.url, activeServer?.host ?: "")
        exoPlayer.setMediaItem(MediaItem.fromUri(resolvedUrl))
        exoPlayer.prepare()
        if (resumeFromSaved && item.type != "live") {
            val savedPos = AppStorage.getSavedPositionForUrl(context, item.url)
            if (savedPos > 5000L) exoPlayer.seekTo(savedPos)
        }
        exoPlayer.play()

        if (item.type == "live") {
            AppStorage.addRecentChannel(context, item)
            recentChannels = AppStorage.getRecentChannels(context)
            activeServer?.let { srv ->
                if (srv.type == "xtream") {
                    scope.launch {
                        currentEpg = ServersEngine.getShortEpg(srv.host, srv.user, srv.pass, item.id)
                    }
                }
            }
        }
    }

    fun openExternalPlayer(url: String) {
        try {
            val resolved = ServersEngine.resolveStreamUrl(url, activeServer?.host ?: "")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(resolved), "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "اختر المشغل الخارجي"))
        } catch (_: Exception) {}
    }

    fun toggleFavorite(id: String) {
        val updated = if (favoriteIds.contains(id)) favoriteIds - id else favoriteIds + id
        favoriteIds = updated
        prefs.edit().putStringSet("fav_ids", updated).apply()
    }

    val safeLiveList by produceState(initialValue = emptyList<MediaItemModel>(), liveList, kidsMode, hiddenCategories) {
        value = withContext(Dispatchers.Default) {
            liveList.filter { item ->
                val notHidden = !hiddenCategories.contains(item.category) && !ServersEngine.isSeparatorChannel(item.name)
                if (kidsMode) {
                    val t = "${item.category} ${item.name}".lowercase()
                    notHidden && (t.contains("kids") || t.contains("cartoon") || t.contains("anime") || t.contains("spacetoon") || t.contains("mbc3") || t.contains("أطفال") || t.contains("كرتون") || t.contains("سبيستون"))
                } else notHidden
            }
        }
    }

    val safeVodList by produceState(initialValue = emptyList<MediaItemModel>(), vodList, kidsMode, hiddenCategories) {
        value = withContext(Dispatchers.Default) {
            vodList.filter { item ->
                val notHidden = !hiddenCategories.contains(item.category)
                if (kidsMode) notHidden && ServersEngine.detectContentLanguage(item) == "أنمي وكرتون" else notHidden
            }
        }
    }

    val safeSeriesList by produceState(initialValue = emptyList<MediaItemModel>(), seriesList, kidsMode, hiddenCategories) {
        value = withContext(Dispatchers.Default) {
            seriesList.filter { item ->
                val notHidden = !hiddenCategories.contains(item.category)
                if (kidsMode) notHidden && ServersEngine.detectContentLanguage(item) == "أنمي وكرتون" else notHidden
            }
        }
    }

    val groupedSports by produceState(initialValue = emptyList<GroupedSportsChannel>(), safeLiveList) {
        value = withContext(Dispatchers.Default) {
            ServersEngine.groupSportsChannels(safeLiveList)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        MaterialTheme(colorScheme = darkColorScheme(primary = accentColor, background = bgColor, surface = cardColor)) {

            if (isMultiScreenMode) {
                Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(cardColor).padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(str.multiScreen, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("إغلاق الكل", color = Color(0xFFF87171), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { isMultiScreenMode = false })
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        for (row in 0..1) {
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                for (col in 0..1) {
                                    val slotIdx = row * 2 + col
                                    val slotMedia = multiChannels[slotIdx]
                                    val isAudioActive = activeAudioSlot == slotIdx
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(2.dp, if (isAudioActive) accentColor else Color(0xFF22222E))
                                            .clickable { activeAudioSlot = slotIdx }
                                    ) {
                                        if (slotMedia != null) {
                                            val slotPlayer = remember(slotMedia.url) {
                                                ExoPlayer.Builder(context).build().apply {
                                                    setMediaItem(MediaItem.fromUri(ServersEngine.resolveStreamUrl(slotMedia.url, activeServer?.host ?: "")))
                                                    prepare()
                                                    playWhenReady = true
                                                }
                                            }
                                            LaunchedEffect(isAudioActive) {
                                                slotPlayer.volume = if (isAudioActive) 1f else 0f
                                            }
                                            DisposableEffect(slotMedia.url) {
                                                onDispose { slotPlayer.release() }
                                            }
                                            AndroidView(
                                                factory = { ctx -> PlayerView(ctx).apply { player = slotPlayer; useController = false } },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Row(
                                                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().background(Color(0x99000000)).padding(6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(slotMedia.name, color = Color.White, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                                Icon(
                                                    if (isAudioActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                                    null,
                                                    tint = if (isAudioActive) greenText else Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Default.Close,
                                                    null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp).clickable {
                                                        val upd = multiChannels.toMutableList()
                                                        upd[slotIdx] = null
                                                        multiChannels = upd
                                                    }
                                                )
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier.fillMaxSize().clickable {
                                                    pickingForSlot = slotIdx
                                                    isMultiScreenMode = false
                                                    selectedTab = 1
                                                    Toast.makeText(context, "اختر قناة للشاشة رقم ${slotIdx + 1}", Toast.LENGTH_SHORT).show()
                                                },
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Default.AddCircleOutline, null, tint = accentColor, modifier = Modifier.size(32.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("إضافة قناة ${slotIdx + 1}", color = Color.Gray, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return@MaterialTheme
            }

            @Composable
            fun ProPlayerViewport(modifier: Modifier) {
                Box(
                    modifier = modifier.background(Color.Black).pointerInput(gesturesEnabled, isScreenLocked) {
                        if (gesturesEnabled && !isScreenLocked) {
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
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = !isInPipMode && !isScreenLocked
                                resizeMode = resizeModes[resizeIdx].first
                                setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { v -> showOverlay = (v == View.VISIBLE) })
                            }
                        },
                        update = { view ->
                            view.useController = !isInPipMode && !isScreenLocked
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

                    if (gestureIndicator.isNotEmpty() && !isInPipMode) {
                        Box(modifier = Modifier.align(Alignment.Center).background(Color(0xCC14141C), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(gestureIndicator, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    if (nextEpisodeCountdown > 0 && !isInPipMode) {
                        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).background(cardColor, RoundedCornerShape(14.dp)).border(1.dp, accentColor, RoundedCornerShape(14.dp)).padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("الحلقة التالية خلال $nextEpisodeCountdown ث...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("إلغاء", color = Color(0xFFF87171), fontSize = 12.sp, modifier = Modifier.clickable { nextEpisodeCountdown = 0 })
                            }
                        }
                    }

                    if (!isInPipMode) {
                        if (isScreenLocked) {
                            IconButton(
                                onClick = { isScreenLocked = false },
                                modifier = Modifier.align(Alignment.TopStart).padding(12.dp).background(Color(0xCC14141C), CircleShape)
                            ) {
                                Icon(Icons.Default.Lock, null, tint = accentColor)
                            }
                        } else if (showOverlay) {
                            Column(modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) {
                                if (videoResolution.isNotEmpty()) {
                                    Box(modifier = Modifier.background(Color(0xBB14141C), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                        Text(videoResolution, color = greenText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                currentEpg?.let { epg ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.background(Color(0xCC14141C), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                        Text("الآن: ${epg.nowTitle}", color = Color.White, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .background(Color(0xCC14141C), RoundedCornerShape(20.dp))
                                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LockOpen, null, tint = Color.White, modifier = Modifier.size(16.dp).clickable { isScreenLocked = true })
                                currentMedia?.let { m ->
                                    Text("خارجي", color = Color(0xFFFBBF24), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { openExternalPlayer(m.url) })
                                }
                                Text(resizeModes[resizeIdx].second, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { resizeIdx = (resizeIdx + 1) % resizeModes.size })
                                Text("عائمة", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        activity?.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
                                    }
                                })
                                Text(if (isFullScreen) "تصغير" else "تكبير", color = greenText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { isFullScreen = !isFullScreen })
                            }
                        }
                    }
                }
            }

            if (isInPipMode || isFullScreen) {
                ProPlayerViewport(Modifier.fillMaxSize())
                return@MaterialTheme
            }

            Scaffold(
                bottomBar = {
                    NavigationBar(containerColor = cardColor) {
                        NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text(str.home, fontSize = 10.sp) })
                        NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Tv, null) }, label = { Text(str.live, fontSize = 10.sp) })
                        NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.SportsSoccer, null) }, label = { Text(str.sports, fontSize = 10.sp) })
                        NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Movie, null) }, label = { Text(str.cinema, fontSize = 10.sp) })
                        NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(str.hub, fontSize = 10.sp) })
                    }
                }
            ) { padding ->
                Column(modifier = Modifier.fillMaxSize().padding(padding).background(bgColor)) {

                    Row(
                        modifier = Modifier.fillMaxWidth().background(cardColor).padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bgColor)
                                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .clickable { showAccountMenu = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (activeServer != null) greenText else Color.Red))
                                Text(activeServer?.title ?: "اختر اشتراكاً", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = accentColor, modifier = Modifier.size(18.dp))
                            }

                            DropdownMenu(expanded = showAccountMenu, onDismissRequest = { showAccountMenu = false }) {
                                servers.forEach { srv ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(srv.title, fontWeight = if (srv.id == activeServerId) FontWeight.Bold else FontWeight.Normal)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(srv.countryCode, fontSize = 11.sp)
                                            }
                                        },
                                        onClick = {
                                            showAccountMenu = false
                                            syncServerData(srv, false)
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("+ ${str.addConnection}", color = accentColor, fontWeight = FontWeight.Bold) },
                                    onClick = { showAccountMenu = false; editingServer = null; showAddServerDialog = true }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (pickingForSlot in 0..3) {
                                Box(modifier = Modifier.background(accentColor, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("اختر لشاشة ${pickingForSlot + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(onClick = { isMultiScreenMode = true }) {
                                Icon(Icons.Default.GridView, null, tint = accentColor)
                            }
                            IconButton(onClick = {
                                if (kidsMode) {
                                    pinActionCallback = { kidsMode = false }
                                    showPinDialog = true
                                } else {
                                    kidsMode = true
                                    Toast.makeText(context, "تم تفعيل وضع الأطفال الآمن", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.ChildCare, null, tint = if (kidsMode) greenText else Color.Gray)
                            }
                            IconButton(onClick = { activeServer?.let { syncServerData(it, true) } }) {
                                Icon(Icons.Default.Refresh, null, tint = Color.White)
                            }
                        }
                    }

                    if (currentMedia != null) {
                        ProPlayerViewport(Modifier.fillMaxWidth().height(210.dp))
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accentColor)
                        }
                    } else {
                        when (selectedTab) {
                            // 0: الشاشة الرئيسية
                            0 -> {
                                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    if (accountInfo != null && activeServer != null) {
                                        Card(colors = CardDefaults.cardColors(containerColor = cardColor), border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)), shape = RoundedCornerShape(16.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column {
                                                    Text("${activeServer.countryCode} • ⚡ ${activeServer.pingMs}ms", color = greenText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text("ينتهي في: ${accountInfo?.expDate}", color = Color.LightGray, fontSize = 12.sp)
                                                }
                                                Text("المتصلين: ${accountInfo?.activeCons}/${accountInfo?.maxCons}", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    } else if (servers.isEmpty()) {
                                        Button(onClick = { editingServer = null; showAddServerDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = accentColor), modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                                            Icon(Icons.Default.AddCircleOutline, null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(str.addConnection, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }

                                    Text(str.platforms, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val platforms = listOf("Netflix", "Shahid", "Prime Video", "Disney+", "HBO", "Apple TV")
                                        items(platforms) { plat ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(cardColor)
                                                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        selectedPlatformFilter = plat
                                                        selectedTab = 3
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                            ) {
                                                Text(plat, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }

                                    if (watchHistory.isNotEmpty()) {
                                        Text(str.continueWatching, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            items(watchHistory) { prog ->
                                                val ratio = (prog.positionMs.toFloat() / prog.durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0.05f, 1f)
                                                Column(
                                                    modifier = Modifier
                                                        .width(135.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(cardColor)
                                                        .clickable {
                                                            startPlayback(MediaItemModel(prog.id, prog.title, prog.url, prog.subtitleInfo, prog.logo, prog.type), true)
                                                        }
                                                        .padding(8.dp)
                                                ) {
                                                    Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                                                        AsyncImage(model = prog.logo, contentDescription = prog.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
                                                        Icon(
                                                            Icons.Default.Close,
                                                            null,
                                                            tint = Color.White,
                                                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).background(Color(0xAA000000), CircleShape).clickable {
                                                                AppStorage.removeWatchProgress(context, prog.id)
                                                                watchHistory = AppStorage.getWatchHistory(context)
                                                            }
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    LinearProgressIndicator(progress = { ratio }, color = accentColor, trackColor = Color.DarkGray, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape))
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(prog.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }

                                    if (recentChannels.isNotEmpty()) {
                                        Text(str.recentChannels, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            items(recentChannels) { rCh ->
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .width(95.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(cardColor)
                                                        .clickable { startPlayback(rCh) }
                                                        .padding(8.dp)
                                                ) {
                                                    Box(modifier = Modifier.fillMaxWidth().height(55.dp)) {
                                                        AsyncImage(model = rCh.logo, contentDescription = rCh.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                                                        Icon(
                                                            Icons.Default.Close,
                                                            null,
                                                            tint = Color.Gray,
                                                            modifier = Modifier.align(Alignment.TopEnd).size(15.dp).clickable {
                                                                AppStorage.removeRecentChannel(context, rCh.url)
                                                                recentChannels = AppStorage.getRecentChannels(context)
                                                            }
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(rCh.name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(str.myFolders, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("+ مجلد جديد", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { showCreateFolderDialog = true })
                                    }
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(customFolders) { folder ->
                                            Card(colors = CardDefaults.cardColors(containerColor = cardColor), border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)), shape = RoundedCornerShape(14.dp)) {
                                                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Icon(Icons.Default.FolderSpecial, null, tint = accentColor)
                                                    Column {
                                                        Text(folder.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text("${folder.itemIds.size} عنصر", color = Color.Gray, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 1: البث المباشر (قائمة باقات عمودية سريعة مع عداد القنوات وسهم الدخول)
                            1 -> {
                                var openedLiveCategory by remember { mutableStateOf<String?>(null) }

                                BackHandler(enabled = openedLiveCategory != null) {
                                    openedLiveCategory = null
                                    searchQuery = ""
                                }

                                val categoryStats by produceState(
                                    initialValue = emptyList<Pair<String, Int>>(),
                                    safeLiveList, favoriteIds
                                ) {
                                    value = withContext(Dispatchers.Default) {
                                        val counts = safeLiveList.groupingBy { it.category }.eachCount()
                                        val result = ArrayList<Pair<String, Int>>(counts.size + 2)
                                        result.add(str.all to safeLiveList.size)
                                        result.add("المفضلة" to safeLiveList.count { favoriteIds.contains(it.id) })
                                        counts.forEach { (cat, count) -> result.add(cat to count) }
                                        result
                                    }
                                }

                                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text(str.searchHint, fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.Search, null, tint = accentColor) },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                                                }
                                            } else {
                                                IconButton(onClick = {
                                                    try {
                                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-IQ")
                                                        }
                                                        voiceLauncher.launch(intent)
                                                    } catch (_: Exception) {}
                                                }) { Icon(Icons.Default.Mic, null, tint = accentColor) }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                    )

                                    if (openedLiveCategory == null && searchQuery.isBlank()) {
                                        Text("الباقات (${categoryStats.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp))
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            contentPadding = PaddingValues(bottom = 12.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(categoryStats, key = { it.first }) { (catName, count) ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(cardColor)
                                                        .clickable { openedLiveCategory = catName }
                                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Icon(
                                                            if (catName == "المفضلة") Icons.Default.Favorite else Icons.Default.LiveTv,
                                                            null,
                                                            tint = accentColor,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(
                                                            catName,
                                                            color = Color.White,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("$count", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Icon(Icons.Default.KeyboardArrowLeft, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(cardColor)
                                                .clickable {
                                                    openedLiveCategory = null
                                                    searchQuery = ""
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.ArrowForward, null, tint = accentColor, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                if (searchQuery.isNotBlank()) "نتائج البحث عن: $searchQuery" else (openedLiveCategory ?: ""),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text("كل الباقات", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        val groupedLive by produceState(
                                            initialValue = emptyList<GroupedSportsChannel>(),
                                            safeLiveList, openedLiveCategory, searchQuery, favoriteIds
                                        ) {
                                            value = withContext(Dispatchers.Default) {
                                                val filtered = safeLiveList.filter { item ->
                                                    val matchCat = when {
                                                        searchQuery.isNotBlank() -> true
                                                        openedLiveCategory == str.all -> true
                                                        openedLiveCategory == "المفضلة" -> favoriteIds.contains(item.id)
                                                        else -> item.category == openedLiveCategory
                                                    }
                                                    matchCat && (searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true))
                                                }
                                                ServersEngine.groupChannelsByQuality(filtered.take(1000))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(3),
                                            contentPadding = PaddingValues(bottom = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(groupedLive) { grp ->
                                                val primaryItem = grp.variants.first().item
                                                val isFav = grp.variants.any { favoriteIds.contains(it.item.id) }
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(cardColor)
                                                        .border(1.dp, if (grp.variants.any { it.item.url == currentMedia?.url }) accentColor else Color(0x18FFFFFF), RoundedCornerShape(14.dp))
                                                        .clickable {
                                                            if (grp.variants.size == 1) startPlayback(primaryItem)
                                                            else selectedQualityGroup = grp
                                                        }
                                                        .padding(8.dp)
                                                ) {
                                                    Box(modifier = Modifier.fillMaxWidth().height(58.dp)) {
                                                        AsyncImage(model = grp.logo, contentDescription = grp.cleanName, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                                                        Icon(
                                                            if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                            null,
                                                            tint = if (isFav) accentColor else Color.Gray,
                                                            modifier = Modifier.align(Alignment.TopEnd).size(17.dp).clickable { toggleFavorite(primaryItem.id) }
                                                        )
                                                        Icon(
                                                            Icons.Default.CreateNewFolder,
                                                            null,
                                                            tint = Color.Gray,
                                                            modifier = Modifier.align(Alignment.TopStart).size(16.dp).clickable { folderTargetItem = primaryItem }
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(grp.cleanName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    if (grp.variants.size > 1) {
                                                        Box(modifier = Modifier.background(greenBadge, RoundedCornerShape(10.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                            Text("${grp.variants.size} جودات", color = greenText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    } else {
                                                        Text(grp.variants.first().qualityTag.substringBefore(" "), color = Color.Gray, fontSize = 9.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2: مركز الرياضة الاحترافي (تمرير عمودي موحّد بدون حجب + ربط ذكي بالمباريات)
                            2 -> {
                                var todayMatches by remember { mutableStateOf(listOf<LiveMatchInfo>()) }
                                var isLoadingMatches by remember { mutableStateOf(false) }
                                var selectedMatchForChannels by remember { mutableStateOf<LiveMatchInfo?>(null) }
                                var liveEpgMap by remember { mutableStateOf(mapOf("" to "")) }

                                LaunchedEffect(appLang) {
                                    isLoadingMatches = true
                                    todayMatches = ServersEngine.fetchTodayMatches(appLang)
                                    isLoadingMatches = false
                                }

                                LaunchedEffect(groupedSports, activeServer) {
                                    val srv = activeServer
                                    if (srv != null && srv.type == "xtream" && groupedSports.isNotEmpty() && liveEpgMap.size <= 1) {
                                        liveEpgMap = ServersEngine.fetchLiveMatchesForSports(srv.host, srv.user, srv.pass, groupedSports)
                                    }
                                }

                                val displaySports = remember(groupedSports, sportsSubFilter, searchQuery) {
                                    groupedSports.filter { grp ->
                                        val matchFilter = when (sportsSubFilter) {
                                            1 -> grp.regionTag == "arab"
                                            2 -> grp.regionTag == "global"
                                            else -> true
                                        }
                                        matchFilter && (searchQuery.isBlank() || grp.cleanName.contains(searchQuery, ignoreCase = true))
                                    }
                                }

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    item(span = { GridItemSpan(2) }) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                item {
                                                    FilterChip(
                                                        selected = sportsSubFilter == 0,
                                                        onClick = { sportsSubFilter = 0 },
                                                        label = { Text("الكل (${groupedSports.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                                    )
                                                }
                                                item {
                                                    FilterChip(
                                                        selected = sportsSubFilter == 1,
                                                        onClick = { sportsSubFilter = 1 },
                                                        label = { Text("الرياضة العربية (beIN / SSC)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                                    )
                                                }
                                                item {
                                                    FilterChip(
                                                        selected = sportsSubFilter == 2,
                                                        onClick = { sportsSubFilter = 2 },
                                                        label = { Text("الرياضة العالمية (Sky / DAZN)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                                    )
                                                }
                                            }

                                            OutlinedTextField(
                                                value = searchQuery,
                                                onValueChange = { searchQuery = it },
                                                placeholder = { Text("ابحث عن مباراة أو قناة (ريال، برشلونة، beIN 2)...", fontSize = 12.sp) },
                                                leadingIcon = { Icon(Icons.Default.SportsSoccer, null, tint = accentColor) },
                                                trailingIcon = {
                                                    if (searchQuery.isNotEmpty()) {
                                                        IconButton(onClick = { searchQuery = "" }) {
                                                            Icon(Icons.Default.Close, null, tint = Color.Gray)
                                                        }
                                                    }
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            if (todayMatches.isNotEmpty() && searchQuery.isBlank()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("مباريات اليوم (${todayMatches.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text("اضغط لاختيار القناة الناقلة", color = greenText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    items(todayMatches) { match ->
                                                        val isLiveNow = match.statusState == "in"
                                                        Card(
                                                            colors = CardDefaults.cardColors(containerColor = cardColor),
                                                            border = BorderStroke(1.dp, if (isLiveNow) greenText else accentColor.copy(alpha = 0.4f)),
                                                            shape = RoundedCornerShape(16.dp),
                                                            modifier = Modifier.width(235.dp).clickable {
                                                                val epgHit = groupedSports.firstOrNull { grp ->
                                                                    val prog = liveEpgMap[grp.cleanName]?.lowercase() ?: ""
                                                                    prog.isNotBlank() && (
                                                                        prog.contains(match.homeTeam.lowercase()) ||
                                                                        prog.contains(match.awayTeam.lowercase())
                                                                    )
                                                                }
                                                                if (epgHit != null) {
                                                                    selectedQualityGroup = epgHit
                                                                } else {
                                                                    selectedMatchForChannels = match
                                                                }
                                                            }
                                                        ) {
                                                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(match.leagueName, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .background(if (isLiveNow) Color(0x33EF4444) else greenBadge, RoundedCornerShape(8.dp))
                                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    ) {
                                                                        Text(match.statusText, color = if (isLiveNow) Color(0xFFF87171) else greenText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                                    }
                                                                }

                                                                Spacer(modifier = Modifier.height(6.dp))

                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                                        AsyncImage(model = match.homeLogo, contentDescription = match.homeTeam, modifier = Modifier.size(28.dp))
                                                                        Spacer(modifier = Modifier.height(3.dp))
                                                                        Text(match.homeTeam, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                                    }

                                                                    Text(
                                                                        if (match.statusState == "pre") "VS" else "${match.homeScore} - ${match.awayScore}",
                                                                        color = Color.White,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        fontSize = 14.sp,
                                                                        modifier = Modifier.padding(horizontal = 6.dp)
                                                                    )

                                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                                        AsyncImage(model = match.awayLogo, contentDescription = match.awayTeam, modifier = Modifier.size(28.dp))
                                                                        Spacer(modifier = Modifier.height(3.dp))
                                                                        Text(match.awayTeam, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                                    }
                                                                }

                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Text("📺 ${match.suggestedBroadcaster}", color = Color.LightGray, fontSize = 10.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                            } else if (isLoadingMatches) {
                                                LinearProgressIndicator(color = accentColor, modifier = Modifier.fillMaxWidth())
                                            }

                                            Text("القنوات الرياضية (${displaySports.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }

                                    items(displaySports) { grp ->
                                        val nowProg = liveEpgMap[grp.cleanName] ?: ""
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = cardColor),
                                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                if (grp.variants.size == 1) startPlayback(grp.variants[0].item)
                                                else selectedQualityGroup = grp
                                            }
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                AsyncImage(model = grp.logo, contentDescription = grp.cleanName, contentScale = ContentScale.Fit, modifier = Modifier.height(44.dp).fillMaxWidth())
                                                Spacer(modifier = Modifier.height(5.dp))
                                                Text(grp.cleanName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                if (nowProg.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(nowProg, color = Color.LightGray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                                Spacer(modifier = Modifier.height(5.dp))
                                                Box(modifier = Modifier.background(greenBadge, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                                                    Text("${grp.variants.size} جودات متاحة", color = greenText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                selectedMatchForChannels?.let { m ->
                                    val suggestedList = remember(m, groupedSports) {
                                        val keywords = m.suggestedBroadcaster.split("/").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                                        val matched = groupedSports.filter { grp ->
                                            val n = grp.cleanName.lowercase()
                                            keywords.any { kw -> n.contains(kw) } && !n.contains("haber") && !n.contains("news")
                                        }
                                        matched.ifEmpty { groupedSports }.take(20)
                                    }

                                    AlertDialog(
                                        onDismissRequest = { selectedMatchForChannels = null },
                                        containerColor = cardColor,
                                        title = {
                                            Column {
                                                Text("${m.homeTeam} × ${m.awayTeam}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Text("${m.leagueName} • اختر القناة الناقلة:", color = accentColor, fontSize = 11.sp)
                                            }
                                        },
                                        text = {
                                            Column(
                                                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                suggestedList.forEach { grp ->
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = bgColor),
                                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                                                        modifier = Modifier.fillMaxWidth().clickable {
                                                            selectedMatchForChannels = null
                                                            if (grp.variants.size == 1) startPlayback(grp.variants[0].item)
                                                            else selectedQualityGroup = grp
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(10.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(grp.cleanName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                                val epgNow = liveEpgMap[grp.cleanName]
                                                                if (!epgNow.isNullOrBlank()) {
                                                                    Text(epgNow, color = greenText, fontSize = 10.sp, maxLines = 1)
                                                                }
                                                            }
                                                            Text("${grp.variants.size} جودات", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = { selectedMatchForChannels = null }) {
                                                Text("إغلاق", color = Color.Gray)
                                            }
                                        }
                                    )
                                }
                            }

                            // 3: السينما المنظمة
                            3 -> {
                                val baseCinema = if (cinemaSubTab == 0) safeVodList else safeSeriesList
                                val langFilters = listOf("الكل", "عربي", "أجنبي مترجم", "كوري وآسيوي", "أنمي وكرتون")
                                val filteredCinema = remember(baseCinema, selectedLangFilter, selectedPlatformFilter, searchQuery) {
                                    baseCinema.filter { item ->
                                        val matchLang = selectedLangFilter == "الكل" || ServersEngine.detectContentLanguage(item) == selectedLangFilter
                                        val matchPlat = selectedPlatformFilter.isBlank() || "${item.category} ${item.name}".contains(selectedPlatformFilter, ignoreCase = true)
                                        matchLang && matchPlat && (searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true))
                                    }
                                }

                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(selected = cinemaSubTab == 0, onClick = { cinemaSubTab = 0 }, label = { Text("${str.movies} (${safeVodList.size})", fontWeight = FontWeight.Bold) })
                                        FilterChip(selected = cinemaSubTab == 1, onClick = { cinemaSubTab = 1 }, label = { Text("${str.series} (${safeSeriesList.size})", fontWeight = FontWeight.Bold) })
                                        if (selectedPlatformFilter.isNotBlank()) {
                                            FilterChip(selected = true, onClick = { selectedPlatformFilter = "" }, label = { Text("$selectedPlatformFilter ×") })
                                        }
                                    }
                                    LazyRow(contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(langFilters) { lf ->
                                            FilterChip(selected = selectedLangFilter == lf, onClick = { selectedLangFilter = lf }, label = { Text(lf, fontSize = 11.sp) })
                                        }
                                    }
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text(str.searchHint, fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.Search, null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(filteredCinema) { item ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(cardColor)
                                                    .clickable { startPlayback(item) }
                                                    .padding(6.dp)
                                            ) {
                                                AsyncImage(model = item.logo, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(10.dp)))
                                                Spacer(modifier = Modifier.height(5.dp))
                                                Text(item.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }

                            // 4: الإعدادات والاشتراكات المتكاملة (Settings)
                            4 -> {
                                var speedResult by remember { mutableStateOf("") }
                                var customUaInput by remember { mutableStateOf(userAgent) }

                                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("الاشتراكات المحفوظة (${servers.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("+ ${str.addConnection}", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.clickable { editingServer = null; showAddServerDialog = true })
                                    }

                                    servers.forEach { srv ->
                                        val isAct = srv.id == activeServerId
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = cardColor),
                                            border = BorderStroke(1.dp, if (isAct) accentColor else Color(0x1FFFFFFF)),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth().clickable { syncServerData(srv, false); selectedTab = 1 }
                                        ) {
                                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(srv.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text("${srv.countryCode} • ${srv.type.uppercase()} • ${srv.pingMs}ms", color = greenText, fontSize = 11.sp)
                                                }
                                                IconButton(onClick = { editingServer = srv; showAddServerDialog = true }) {
                                                    Icon(Icons.Default.Edit, null, tint = Color.LightGray)
                                                }
                                                IconButton(onClick = {
                                                    val upd = servers.filter { it.id != srv.id }
                                                    servers = upd; saveServersList(prefs, upd)
                                                }) {
                                                    Icon(Icons.Default.Delete, null, tint = Color(0xFFF87171))
                                                }
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { showCategoriesManager = true }, modifier = Modifier.weight(1f)) {
                                            Text("إخفاء الباقات", fontSize = 12.sp)
                                        }
                                        OutlinedButton(onClick = { showBackupDialog = true }, modifier = Modifier.weight(1f)) {
                                            Text("نسخ احتياطي", fontSize = 12.sp)
                                        }
                                    }

                                    HorizontalDivider(color = Color(0x22FFFFFF))

                                    Text("لغة التطبيق (Language):", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val langs = listOf("ar" to "العربية", "en" to "English", "fr" to "Français", "tr" to "Türkçe", "ku" to "کوردی")
                                        items(langs) { (code, label) ->
                                            FilterChip(selected = appLang == code, onClick = {
                                                appLang = code; prefs.edit().putString("app_lang", code).apply()
                                            }, label = { Text(label, fontSize = 11.sp) })
                                        }
                                    }

                                    Text("لون التمييز والخلفية:", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(accentOptions.indices.toList()) { idx ->
                                            FilterChip(selected = accentIdx == idx, onClick = {
                                                accentIdx = idx; prefs.edit().putInt("accent_idx", idx).apply()
                                            }, label = { Text(accentOptions[idx].first, fontSize = 10.sp) })
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        bgOptions.indices.forEach { idx ->
                                            FilterChip(selected = bgIdx == idx, onClick = {
                                                bgIdx = idx; prefs.edit().putInt("bg_idx", idx).apply()
                                            }, label = { Text(bgOptions[idx].first, fontSize = 10.sp) })
                                        }
                                    }

                                    Text("نوع فك الترميز (Decoder):", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("auto" to "تلقائي", "hw" to "Hardware (4K)", "sw" to "Software").forEach { (k, lbl) ->
                                            FilterChip(selected = decoderMode == k, onClick = {
                                                decoderMode = k; prefs.edit().putString("decoder_mode", k).apply()
                                            }, label = { Text(lbl, fontSize = 10.sp) })
                                        }
                                    }

                                    Text("ملف التخزين المؤقت (منع التقطيع):", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("fast" to "سريع", "balanced" to "متوازن", "antifreeze" to "مضاد للتقطيع").forEach { (k, lbl) ->
                                            FilterChip(selected = bufferProfile == k, onClick = {
                                                bufferProfile = k; prefs.edit().putString("buffer_profile", k).apply()
                                            }, label = { Text(lbl, fontSize = 10.sp) })
                                        }
                                    }

                                    Text("صيغة البث وتوحيد الهوست:", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(selected = streamFmt == "ts", onClick = { streamFmt = "ts"; prefs.edit().putString("stream_fmt", "ts").apply() }, label = { Text("MPEG-TS") })
                                        FilterChip(selected = streamFmt == "m3u8", onClick = { streamFmt = "m3u8"; prefs.edit().putString("stream_fmt", "m3u8").apply() }, label = { Text("HLS / M3U8") })
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("توحيد رابط البث (Same Host Proxy)", color = Color.White, fontSize = 12.sp)
                                        Switch(checked = sameHost, onCheckedChange = { sameHost = it; prefs.edit().putBoolean("same_host", it).apply() })
                                    }

                                    Text("User-Agent (هوية المشغل):", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val agents = listOf("IPTVSmartersPro", "TiviMate/4.7.0", "VLC/3.0.18", "VU IPTV Player", "Mozilla/5.0")
                                        items(agents) { ua ->
                                            FilterChip(selected = userAgent == ua, onClick = {
                                                userAgent = ua; customUaInput = ua; prefs.edit().putString("user_agent", ua).apply()
                                            }, label = { Text(ua, fontSize = 10.sp) })
                                        }
                                    }
                                    OutlinedTextField(
                                        value = customUaInput,
                                        onValueChange = { customUaInput = it; userAgent = it; prefs.edit().putString("user_agent", it).apply() },
                                        label = { Text("User-Agent مخصص", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Text("مؤقت النوم (Sleep Timer): ${if (sleepMinutesLeft > 0) "$sleepMinutesLeft د" else "متوقف"}", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(0 to "إيقاف", 15 to "15د", 30 to "30د", 60 to "60د").forEach { (mins, lbl) ->
                                            FilterChip(selected = sleepMinutesLeft == mins, onClick = { sleepMinutesLeft = mins }, label = { Text(lbl, fontSize = 10.sp) })
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("تشغيل الحلقة التالية تلقائياً", color = Color.White, fontSize = 12.sp)
                                        Switch(checked = autoplayNextEp, onCheckedChange = { autoplayNextEp = it; prefs.edit().putBoolean("autoplay_next_ep", it).apply() })
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("لون الترجمة أصفر سينمائي", color = Color.White, fontSize = 12.sp)
                                        Switch(checked = subColorYellow, onCheckedChange = { subColorYellow = it; prefs.edit().putBoolean("sub_yellow", it).apply() })
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = {
                                            activeServer?.let { srv ->
                                                scope.launch { speedResult = ServersEngine.runSpeedTest(srv.host) }
                                            }
                                        }, modifier = Modifier.weight(1f)) { Text("فحص السيرفر", fontSize = 11.sp) }

                                        OutlinedButton(onClick = {
                                            AppStorage.clearAllMediaCache(context)
                                            Toast.makeText(context, "تم تنظيف الذاكرة المؤقتة", Toast.LENGTH_SHORT).show()
                                        }, modifier = Modifier.weight(1f)) { Text("مسح الكاش (${AppStorage.getCacheSizeMb(context)})", fontSize = 10.sp) }
                                    }
                                    if (speedResult.isNotBlank()) {
                                        Text(speedResult, color = greenText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // نافذة اختيار جودة القناة
            selectedQualityGroup?.let { grp ->
                AlertDialog(
                    onDismissRequest = { selectedQualityGroup = null },
                    containerColor = cardColor,
                    title = { Text("${str.chooseQuality}: ${grp.cleanName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    text = {
                        Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            grp.variants.forEach { variant ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = bgColor),
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedQualityGroup = null
                                        startPlayback(variant.item)
                                    }
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(variant.qualityTag, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(variant.rawName, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Icon(Icons.Default.PlayArrow, null, tint = greenText)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { selectedQualityGroup = null }) { Text("إغلاق", color = Color.Gray) } }
                )
            }

            // نافذة المواسم والحلقات الكاملة للمسلسلات
            selectedSeriesItem?.let { sItem ->
                AlertDialog(
                    onDismissRequest = { selectedSeriesItem = null },
                    containerColor = cardColor,
                    title = { Text(sItem.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    text = {
                        val det = loadedSeriesDetails
                        if (det == null) {
                            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = accentColor)
                            }
                        } else {
                            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("⭐ ${det.rating} • ${det.plot}", color = Color.LightGray, fontSize = 11.sp, maxLines = 3)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(det.seasons.keys.toList()) { sNum ->
                                        FilterChip(selected = activeSeasonNum == sNum, onClick = { activeSeasonNum = sNum }, label = { Text("الموسم $sNum") })
                                    }
                                }
                                val episodes = det.seasons[activeSeasonNum] ?: emptyList()
                                episodes.forEachIndexed { idx, ep ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = bgColor),
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            activeEpisodesQueue = episodes
                                            currentEpisodeIdx = idx
                                            selectedSeriesItem = null
                                            startPlayback(MediaItemModel(ep.id, "${det.title} - م$activeSeasonNum ح${ep.episodeNum}", ep.url, det.title, det.cover, "series"))
                                        }
                                    ) {
                                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("الحلقة ${ep.episodeNum}: ${ep.title}", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Icon(Icons.Default.PlayCircleOutline, null, tint = accentColor)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { selectedSeriesItem = null }) { Text("إغلاق", color = Color.Gray) } }
                )
            }

            // نافذة إضافة / تعديل اشتراك
            if (showAddServerDialog) {
                var titleIn by remember { mutableStateOf(editingServer?.title ?: "") }
                var typeIn by remember { mutableStateOf(editingServer?.type ?: "xtream") }
                var hostIn by remember { mutableStateOf(editingServer?.host ?: "") }
                var userIn by remember { mutableStateOf(editingServer?.user ?: "") }
                var passIn by remember { mutableStateOf(editingServer?.pass ?: "") }

                AlertDialog(
                    onDismissRequest = { showAddServerDialog = false },
                    containerColor = cardColor,
                    title = { Text(if (editingServer == null) str.addConnection else "تعديل الاشتراك", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("xtream" to "Xtream", "stalker" to "Stalker", "m3u" to "M3U").forEach { (k, lbl) ->
                                    FilterChip(selected = typeIn == k, onClick = { typeIn = k }, label = { Text(lbl, fontSize = 11.sp) })
                                }
                            }
                            OutlinedTextField(value = titleIn, onValueChange = { titleIn = it }, label = { Text("اسم الاشتراك") }, singleLine = true)
                            OutlinedTextField(value = hostIn, onValueChange = { hostIn = it }, label = { Text("رابط السيرفر أو M3U") }, singleLine = true)
                            if (typeIn != "m3u") {
                                OutlinedTextField(value = userIn, onValueChange = { userIn = it }, label = { Text(if (typeIn == "stalker") "MAC Address" else "اسم المستخدم") }, singleLine = true)
                            }
                            if (typeIn == "xtream") {
                                OutlinedTextField(value = passIn, onValueChange = { passIn = it }, label = { Text("كلمة المرور") }, singleLine = true)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val extracted = if (typeIn == "m3u") ServersEngine.parseXtreamFromUrl(hostIn) else null
                            val fType = if (extracted != null) "xtream" else typeIn
                            val fHost = extracted?.first ?: hostIn.trim()
                            val fUser = extracted?.second ?: userIn.trim()
                            val fPass = extracted?.third ?: passIn.trim()
                            val fTitle = titleIn.ifBlank { Uri.parse(fHost).host ?: "اشتراك جديد" }

                            val newSrv = SavedServer(editingServer?.id ?: System.currentTimeMillis().toString(), fTitle, fType, fHost, fUser, fPass)
                            val upd = if (editingServer == null) servers + newSrv else servers.map { if (it.id == newSrv.id) newSrv else it }
                            servers = upd; saveServersList(prefs, upd)
                            showAddServerDialog = false
                            syncServerData(newSrv, true)
                        }) { Text("حفظ وتحديث") }
                    },
                    dismissButton = { TextButton(onClick = { showAddServerDialog = false }) { Text("إلغاء", color = Color.Gray) } }
                )
            }

            // نافذة إنشاء مجلد شخصي
            if (showCreateFolderDialog) {
                var folderName by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreateFolderDialog = false },
                    containerColor = cardColor,
                    title = { Text("إنشاء مجلد شخصي جديد", color = Color.White) },
                    text = { OutlinedTextField(value = folderName, onValueChange = { folderName = it }, label = { Text("اسم المجلد (مثلاً: مبارياتي)") }, singleLine = true) },
                    confirmButton = {
                        Button(onClick = {
                            if (folderName.isNotBlank()) {
                                val upd = customFolders + CustomFolder(System.currentTimeMillis().toString(), folderName.trim(), emptyList())
                                customFolders = upd; AppStorage.saveCustomFolders(context, upd)
                            }
                            showCreateFolderDialog = false
                        }) { Text("إنشاء") }
                    }
                )
            }

            // نافذة إضافة قناة إلى مجلد
            folderTargetItem?.let { target ->
                AlertDialog(
                    onDismissRequest = { folderTargetItem = null },
                    containerColor = cardColor,
                    title = { Text("إضافة إلى مجلد: ${target.name}", color = Color.White, fontSize = 14.sp) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (customFolders.isEmpty()) Text("أنشئ مجلداً من الرئيسية أولاً.", color = Color.Gray)
                            customFolders.forEach { f ->
                                Text("📁 ${f.name}", color = accentColor, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().clickable {
                                    val upd = customFolders.map { if (it.id == f.id) it.copy(itemIds = (it.itemIds + target.id).distinct()) else it }
                                    customFolders = upd; AppStorage.saveCustomFolders(context, upd)
                                    folderTargetItem = null
                                    Toast.makeText(context, "تمت الإضافة إلى ${f.name}", Toast.LENGTH_SHORT).show()
                                }.padding(8.dp))
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { folderTargetItem = null }) { Text("إغلاق") } }
                )
            }

            // نافذة إخفاء الباقات غير المرغوبة
            if (showCategoriesManager) {
                val allCats = remember(liveList, vodList) { (liveList.map { it.category } + vodList.map { it.category }).distinct() }
                AlertDialog(
                    onDismissRequest = { showCategoriesManager = false },
                    containerColor = cardColor,
                    title = { Text("إخفاء / إظهار الباقات", color = Color.White) },
                    text = {
                        Column(modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                            allCats.forEach { c ->
                                val isHid = hiddenCategories.contains(c)
                                Row(modifier = Modifier.fillMaxWidth().clickable {
                                    val upd = if (isHid) hiddenCategories - c else hiddenCategories + c
                                    hiddenCategories = upd; prefs.edit().putStringSet("hidden_cats", upd).apply()
                                }.padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(c, color = if (isHid) Color.Gray else Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    Text(if (isHid) "مخفية" else "ظاهرة", color = if (isHid) Color.Red else greenText, fontSize = 11.sp)
                                }
                            }
                        }
                    },
                    confirmButton = { Button(onClick = { showCategoriesManager = false }) { Text("تم") } }
                )
            }

            // نافذة النسخ الاحتياطي السريع
            if (showBackupDialog) {
                var codeIn by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showBackupDialog = false },
                    containerColor = cardColor,
                    title = { Text("النسخ الاحتياطي والاستعادة", color = Color.White) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val exported = AppStorage.exportBackupCode(context)
                                clipboard.setText(AnnotatedString(exported))
                                Toast.makeText(context, "تم نسخ كود النسخة الاحتياطية للحافظة!", Toast.LENGTH_LONG).show()
                            }, modifier = Modifier.fillMaxWidth()) { Text("نسخ كود النسخة الاحتياطية") }
                            OutlinedTextField(value = codeIn, onValueChange = { codeIn = it }, label = { Text("الصق كود الاستعادة هنا") })
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (AppStorage.importBackupCode(context, codeIn)) {
                                servers = loadSavedServers(prefs)
                                customFolders = AppStorage.getCustomFolders(context)
                                Toast.makeText(context, "تمت الاستعادة بنجاح!", Toast.LENGTH_SHORT).show()
                                showBackupDialog = false
                            }
                        }) { Text("استعادة") }
                    }
                )
            }

            // نافذة الرمز السري للخروج من وضع الأطفال (PIN)
            if (showPinDialog) {
                var pinInput by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showPinDialog = false },
                    containerColor = cardColor,
                    title = { Text("أدخل الرمز السري (الافتراضي 0000)", color = Color.White, fontSize = 14.sp) },
                    text = { OutlinedTextField(value = pinInput, onValueChange = { pinInput = it }, label = { Text("PIN") }, singleLine = true) },
                    confirmButton = {
                        Button(onClick = {
                            if (pinInput == parentalPin || pinInput == "0000") {
                                showPinDialog = false
                                pinActionCallback()
                            } else {
                                Toast.makeText(context, "الرمز غير صحيح", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("تأكيد") }
                    }
                )
            }
        }
    }
}
// END_MAIN
