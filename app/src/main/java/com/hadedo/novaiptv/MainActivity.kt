package com.hadedo.novaiptv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Rational
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
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
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
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
    "en" -> AppStrings("Home", "Live TV", "Sports", "Cinema", "Settings", "Continue Watching", "Recently Watched", "My Folders", "Platforms", "Search channels or categories...", "All", "Movies", "Series", "Multi-Screen (4)", "Kids Mode", "Settings", "Add Connection", "Sync Now", "Seasons & Episodes", "Select Quality")
    "fr" -> AppStrings("Accueil", "Direct", "Sports", "Cinéma", "Paramètres", "Reprendre", "Chaînes récentes", "Mes Dossiers", "Plateformes", "Rechercher une chaîne...", "Tout", "Films", "Séries", "Multi-Écran (4)", "Mode Enfants", "Paramètres", "Ajouter", "Actualiser", "Saisons & Épisodes", "Choisir la qualité")
    "tr" -> AppStrings("Ana Sayfa", "Canlı TV", "Spor", "Sinema", "Ayarlar", "İzlemeye Devam Et", "Son İzlenenler", "Klasörlerim", "Platformlar", "Kanal veya kategori ara...", "Tümü", "Filmler", "Diziler", "Çoklu Ekran (4)", "Çocuk Modu", "Ayarlar", "Bağlantı Ekle", "Yenile", "Sezonlar ve Bölümler", "Kalite Seçin")
    "ku" -> AppStrings("سەرەکی", "ڕاستەوخۆ", "وەرزش", "سینەما", "ڕێکخستنەکان", "بەردەوامبە لە بینین", "دوایین کەناڵەکان", "فۆڵدەرەکانم", "پلاتفۆرمەکان", "گەڕان لە کەناڵەکان...", "هەموو", "فیلمەکان", "زنجیرەکان", "فرە شاشە (4)", "دۆخی منداڵان", "ڕێکخستنەکان", "زیادکردنی هەژمار", "نوێکردنەوە", "وەرز و ئەڵقەکان", "کوالێتی هەڵبژێرە")
    else -> AppStrings("الرئيسية", "مباشر", "الرياضة", "السينما", "الإعدادات", "واصل المشاهدة", "شاهدته مؤخراً", "مجلداتي", "المنصات", "ابحث عن قناة أو باقة...", "الكل", "أفلام", "مسلسلات", "تعدد الشاشات (4)", "وضع الأطفال", "الإعدادات", "إضافة اشتراك", "تحديث القوائم", "المواسم والحلقات", "اختر جودة البث")
}

@Composable
fun VeloxBrandWordmark(accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF09090F))))
                .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(16.dp)) {
                val w = size.width
                val h = size.height
                val vPath = Path().apply {
                    moveTo(w * 0.1f, h * 0.15f)
                    lineTo(w * 0.34f, h * 0.15f)
                    lineTo(w * 0.5f, h * 0.68f)
                    lineTo(w * 0.66f, h * 0.15f)
                    lineTo(w * 0.9f, h * 0.15f)
                    lineTo(w * 0.58f, h * 0.88f)
                    lineTo(w * 0.42f, h * 0.88f)
                    close()
                }
                drawPath(
                    path = vPath,
                    brush = Brush.linearGradient(listOf(Color(0xFF38BDF8), accentColor))
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("VELOX", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
            Box(
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.22f), RoundedCornerShape(5.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text("ULTRA", color = Color(0xFF38BDF8), fontWeight = FontWeight.ExtraBold, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun ChannelMonogramBadge(name: String, logoUrl: String, accentColor: Color, modifier: Modifier = Modifier) {
    val initials = remember(name) {
        name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "TV" }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF161622))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = accentColor, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        if (logoUrl.isNotBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(3.dp)
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NovaIptvUltimateApp(isInPipMode: Boolean) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("nova_neon_prefs", Context.MODE_PRIVATE) }

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
    var liveGridMode by remember { mutableStateOf(prefs.getBoolean("live_grid_mode", false)) }
    var gesturesEnabled by remember { mutableStateOf(prefs.getBoolean("gestures_enabled", true)) }
    var autoplayNextEp by remember { mutableStateOf(prefs.getBoolean("autoplay_next_ep", true)) }
    var defaultPlayer by remember { mutableStateOf(prefs.getString("default_player", "internal") ?: "internal") }
    var decoderMode by remember { mutableStateOf(prefs.getString("decoder_mode", "hw") ?: "hw") }
    var bufferProfile by remember { mutableStateOf(prefs.getString("buffer_profile", "zerodelay") ?: "zerodelay") }
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

    var liveList by remember { mutableStateOf(listOf<MediaItemModel>()) }
    var vodList by remember { mutableStateOf(listOf<MediaItemModel>()) }
    var seriesList by remember { mutableStateOf(listOf<MediaItemModel>()) }
    var homeMatches by remember { mutableStateOf(listOf<LiveMatchInfo>()) }
    var isLoading by remember { mutableStateOf(false) }

    var favoriteIds by remember { mutableStateOf(prefs.getStringSet("fav_ids", emptySet()) ?: emptySet()) }
    var watchHistory by remember { mutableStateOf(AppStorage.getWatchHistory(context)) }
    var recentChannels by remember { mutableStateOf(AppStorage.getRecentChannels(context)) }
    var customFolders by remember { mutableStateOf(AppStorage.getCustomFolders(context)) }

    var currentMedia by remember { mutableStateOf<MediaItemModel?>(null) }
    var currentEpg by remember { mutableStateOf<EpgProgram?>(null) }
    var videoResolution by remember { mutableStateOf("") }
    var isPlayingState by remember { mutableStateOf(true) }
    var isBufferingState by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var sleepMinutesLeft by remember { mutableIntStateOf(0) }

    var selectedQualityGroup by remember { mutableStateOf<GroupedSportsChannel?>(null) }
    var selectedSeriesItem by remember { mutableStateOf<MediaItemModel?>(null) }
    var loadedSeriesDetails by remember { mutableStateOf<SeriesDetails?>(null) }
    var activeSeasonNum by remember { mutableIntStateOf(1) }
    var activeEpisodesQueue by remember { mutableStateOf(listOf<EpisodeModel>()) }
    var currentEpisodeIdx by remember { mutableIntStateOf(-1) }
    var nextEpisodeCountdown by remember { mutableIntStateOf(0) }

    var isMultiScreenMode by remember { mutableStateOf(false) }
    var multiChannels by remember { mutableStateOf<List<MediaItemModel?>>(listOf(null, null, null, null)) }
    var activeAudioSlot by remember { mutableIntStateOf(0) }
    var pickingForSlot by remember { mutableIntStateOf(-1) }

    var showServerSwitcherSheet by remember { mutableStateOf(false) }
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

    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .clearVideoSizeConstraints()
                    .setForceHighestSupportedBitrate(true)
                    .setPreferredAudioLanguage("ar")
            )
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
            setEnableDecoderFallback(true)
        }

        val (minBuf, maxBuf, playbackBuf, rebufferMs) = when (bufferProfile) {
            "zerodelay" -> listOf(1000, 8000, 400, 800)
            "fast" -> listOf(1500, 12000, 500, 1000)
            "antifreeze" -> listOf(12000, 45000, 3000, 4000)
            else -> listOf(3500, 20000, 900, 1500)
        }
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(minBuf, maxBuf, playbackBuf, rebufferMs)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(10000)
            .setAllowCrossProtocolRedirects(true)

        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
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
                    videoResolution = "$tag • 60FPS"
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayingState = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                val currUri = exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString() ?: ""
                if (currUri.endsWith(".ts", true)) {
                    videoResolution = "تجربة مسار بديل..."
                    exoPlayer.setMediaItem(MediaItem.fromUri(currUri.dropLast(3) + ".m3u8"))
                    exoPlayer.prepare()
                    exoPlayer.play()
                } else {
                    isBufferingState = false
                    videoResolution = "القناة متوقفة من المصدر"
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBufferingState = (playbackState == Player.STATE_BUFFERING)
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
        val isSwitchingServer = (activeServerId != srv.id)
        activeServerId = srv.id
        prefs.edit().putString("active_server_id", srv.id).apply()

        val loadMovies = prefs.getBoolean("sync_vod_${srv.id}", true)
        val loadSeries = prefs.getBoolean("sync_series_${srv.id}", true)

        if (isSwitchingServer) {
            exoPlayer.clearMediaItems()
            exoPlayer.stop()
            currentMedia = null
            accountInfo = null
            liveList = emptyList()
            vodList = emptyList()
            seriesList = emptyList()
        }

        scope.launch {
            val cachedLive = if (!forceRefresh) AppStorage.loadMediaCache(context, srv.id, "live") else null
            val cachedVod = if (!forceRefresh && loadMovies) AppStorage.loadMediaCache(context, srv.id, "vod") else null
            val cachedSeries = if (!forceRefresh && loadSeries) AppStorage.loadMediaCache(context, srv.id, "series") else null

            if (cachedLive != null) {
                liveList = withContext(Dispatchers.Default) { ServersEngine.enrichCategoryQualities(cachedLive) }
            }
            if (cachedVod != null) vodList = cachedVod
            if (cachedSeries != null) seriesList = cachedSeries

            val needsVod = loadMovies && (cachedVod == null || forceRefresh)
            val needsSeries = loadSeries && (cachedSeries == null || forceRefresh)

            if (cachedLive == null || needsVod || needsSeries || forceRefresh) {
                isLoading = (cachedLive == null)
                val (country, ping) = ServersEngine.detectServerCountryAndPing(srv.host)
                val updatedServers = servers.map { if (it.id == srv.id) it.copy(countryCode = country, pingMs = ping) else it }
                servers = updatedServers
                saveServersList(prefs, updatedServers)

                if (srv.type == "xtream") {
                    accountInfo = ServersEngine.getAccountInfo(srv.host, srv.user, srv.pass)
                    if (cachedLive == null || forceRefresh) {
                        val freshLive = ServersEngine.loadXtream(srv.host, srv.user, srv.pass, "live")
                        if (freshLive.isNotEmpty()) {
                            liveList = freshLive
                            AppStorage.saveMediaCache(context, srv.id, "live", freshLive)
                        }
                    }
                    isLoading = false

                    if (needsVod) {
                        val freshVod = ServersEngine.loadXtream(srv.host, srv.user, srv.pass, "vod")
                        if (freshVod.isNotEmpty()) {
                            vodList = freshVod
                            AppStorage.saveMediaCache(context, srv.id, "vod", freshVod)
                        }
                    }
                    if (needsSeries) {
                        val freshSeries = ServersEngine.loadXtream(srv.host, srv.user, srv.pass, "series")
                        if (freshSeries.isNotEmpty()) {
                            seriesList = freshSeries
                            AppStorage.saveMediaCache(context, srv.id, "series", freshSeries)
                        }
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
        homeMatches = ServersEngine.fetchTodayMatches(appLang)
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

        val resolvedUrl = ServersEngine.resolveStreamUrl(item.url, activeServer?.host ?: "")

        if (defaultPlayer != "internal") {
            val launched = launchPreferredExternalPlayer(context, resolvedUrl, item.name, defaultPlayer, userAgent)
            if (launched) {
                if (item.type == "live") {
                    AppStorage.addRecentChannel(context, item)
                    recentChannels = AppStorage.getRecentChannels(context)
                }
                return
            } else {
                Toast.makeText(context, "المشغل المختار غير مثبت، تم التشغيل في VELOX الداخلي", Toast.LENGTH_SHORT).show()
            }
        }

        currentMedia = item
        currentEpg = null
        videoResolution = "جاري الربط..."

        val mediaItemBuilder = MediaItem.Builder().setUri(resolvedUrl)
        if (item.type == "live" && bufferProfile == "zerodelay") {
            mediaItemBuilder.setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(1500L)
                    .setMinOffsetMs(500L)
                    .setMaxOffsetMs(4000L)
                    .setMinPlaybackSpeed(0.98f)
                    .setMaxPlaybackSpeed(1.04f)
                    .build()
            )
        }

        exoPlayer.setMediaItem(mediaItemBuilder.build())
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

    fun zapChannel(step: Int) {
        val curr = currentMedia ?: return
        if (curr.type != "live" || liveList.isEmpty()) return
        val sameCat = liveList.filter { it.category == curr.category }.ifEmpty { liveList }
        val idx = sameCat.indexOfFirst { it.id == curr.id }
        val nextIdx = if (idx == -1) 0 else (idx + step + sameCat.size) % sameCat.size
        startPlayback(sameCat[nextIdx], false)
    }

    fun toggleFavorite(id: String) {
        val updated = if (favoriteIds.contains(id)) favoriteIds - id else favoriteIds + id
        favoriteIds = updated
        prefs.edit().putStringSet("fav_ids", updated).apply()
    }

    val safeLiveList by produceState(initialValue = listOf<MediaItemModel>(), liveList, kidsMode, hiddenCategories) {
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

    val safeVodList by produceState(initialValue = listOf<MediaItemModel>(), vodList, kidsMode, hiddenCategories) {
        value = withContext(Dispatchers.Default) {
            vodList.filter { item ->
                val notHidden = !hiddenCategories.contains(item.category)
                if (kidsMode) notHidden && ServersEngine.detectContentLanguage(item) == "أنمي وكرتون" else notHidden
            }
        }
    }

    val safeSeriesList by produceState(initialValue = listOf<MediaItemModel>(), seriesList, kidsMode, hiddenCategories) {
        value = withContext(Dispatchers.Default) {
            seriesList.filter { item ->
                val notHidden = !hiddenCategories.contains(item.category)
                if (kidsMode) notHidden && ServersEngine.detectContentLanguage(item) == "أنمي وكرتون" else notHidden
            }
        }
    }

    val groupedSports by produceState(initialValue = listOf<GroupedSportsChannel>(), safeLiveList) {
        value = withContext(Dispatchers.Default) {
            ServersEngine.groupSportsChannels(safeLiveList)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        MaterialTheme(colorScheme = darkColorScheme(primary = accentColor, background = bgColor, surface = cardColor)) {

            if (isMultiScreenMode) {
                BackHandler { isMultiScreenMode = false }
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
                                            LaunchedEffect(isAudioActive) { slotPlayer.volume = if (isAudioActive) 1f else 0f }
                                            DisposableEffect(slotMedia.url) { onDispose { slotPlayer.release() } }
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
                                                Icon(if (isAudioActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff, null, tint = if (isAudioActive) greenText else Color.Gray, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp).clickable {
                                                    val upd = multiChannels.toMutableList(); upd[slotIdx] = null; multiChannels = upd
                                                })
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier.fillMaxSize().clickable {
                                                    pickingForSlot = slotIdx; isMultiScreenMode = false; selectedTab = 1
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

            val activePlayerItem = currentMedia
            if ((isInPipMode || isFullScreen) && activePlayerItem != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ProCinemaPlayerOverlay(
                        exoPlayer = exoPlayer,
                        trackSelector = trackSelector,
                        currentMedia = activePlayerItem,
                        currentEpg = currentEpg,
                        videoResolution = videoResolution,
                        isPlaying = isPlayingState,
                        isBuffering = isBufferingState,
                        isFullScreen = isFullScreen,
                        isInPipMode = isInPipMode,
                        allLiveChannels = safeLiveList,
                        activeHost = activeServer?.host ?: "",
                        accentColor = accentColor,
                        subFontSize = subFontSize,
                        subColorYellow = subColorYellow,
                        gesturesEnabled = gesturesEnabled,
                        onToggleFullScreen = { isFullScreen = !isFullScreen },
                        onClosePlayer = {
                            exoPlayer.clearMediaItems()
                            exoPlayer.stop()
                            currentMedia = null
                            isFullScreen = false
                        },
                        onSelectChannel = { startPlayback(it, false) },
                        onZapChannel = { zapChannel(it) }
                    )
                }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardColor)
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VeloxBrandWordmark(accentColor = accentColor)

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(bgColor)
                                .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showServerSwitcherSheet = true
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (activeServer != null) greenText else Color(0xFFF87171)))
                            Text(
                                activeServer?.title ?: "اختر اشتراكاً",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 95.dp)
                            )
                            Icon(Icons.Default.SwapVert, null, tint = accentColor, modifier = Modifier.size(15.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isMultiScreenMode = true }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.GridView, null, tint = accentColor, modifier = Modifier.size(19.dp))
                            }
                            IconButton(
                                onClick = {
                                    if (kidsMode) {
                                        pinActionCallback = { kidsMode = false }
                                        showPinDialog = true
                                    } else {
                                        kidsMode = true
                                        Toast.makeText(context, "تم تفعيل وضع الأطفال", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.ChildCare, null, tint = if (kidsMode) greenText else Color.Gray, modifier = Modifier.size(19.dp))
                            }
                            IconButton(onClick = { activeServer?.let { syncServerData(it, true) } }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(19.dp))
                            }
                        }
                    }

                    if (isLoading) {
                        LinearProgressIndicator(color = accentColor, modifier = Modifier.fillMaxWidth().height(2.dp))
                    }

                    if (activePlayerItem != null) {
                        ProCinemaPlayerOverlay(
                            exoPlayer = exoPlayer,
                            trackSelector = trackSelector,
                            currentMedia = activePlayerItem,
                            currentEpg = currentEpg,
                            videoResolution = videoResolution,
                            isPlaying = isPlayingState,
                            isBuffering = isBufferingState,
                            isFullScreen = false,
                            isInPipMode = false,
                            allLiveChannels = safeLiveList,
                            activeHost = activeServer?.host ?: "",
                            accentColor = accentColor,
                            subFontSize = subFontSize,
                            subColorYellow = subColorYellow,
                            gesturesEnabled = gesturesEnabled,
                            onToggleFullScreen = { isFullScreen = true },
                            onClosePlayer = {
                                exoPlayer.clearMediaItems()
                                exoPlayer.stop()
                                currentMedia = null
                            },
                            onSelectChannel = { startPlayback(it, false) },
                            onZapChannel = { zapChannel(it) }
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedTab) {
                            0 -> {
                                val heroItems = remember(safeVodList, safeSeriesList, safeLiveList) {
                                    (safeVodList.filter { it.logo.isNotBlank() }.take(5) +
                                        safeSeriesList.filter { it.logo.isNotBlank() }.take(4)).ifEmpty {
                                        safeLiveList.filter { it.logo.isNotBlank() }.take(6)
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (heroItems.isNotEmpty()) {
                                        val pagerState = rememberPagerState(pageCount = { heroItems.size })
                                        LaunchedEffect(heroItems.size) {
                                            while (heroItems.size > 1) {
                                                delay(5500L)
                                                val next = (pagerState.currentPage + 1) % heroItems.size
                                                pagerState.animateScrollToPage(next)
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(235.dp)
                                                .clip(RoundedCornerShape(24.dp))
                                                .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                                        ) {
                                            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                                                val item = heroItems[page]
                                                Box(modifier = Modifier.fillMaxSize().clickable { startPlayback(item) }) {
                                                    AsyncImage(
                                                        model = item.logo,
                                                        contentDescription = item.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(
                                                                Brush.verticalGradient(
                                                                    colors = listOf(Color(0x22000000), Color(0x9908080D), Color(0xF5050508))
                                                                )
                                                            )
                                                    )
                                                    Column(
                                                        modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(accentColor, RoundedCornerShape(8.dp))
                                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                                            ) {
                                                                Text(
                                                                    if (item.type == "live") "🔴 بث مباشر" else "✨ مختار لك",
                                                                    color = Color.White,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.ExtraBold
                                                                )
                                                            }
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(Color(0x9914141C), RoundedCornerShape(8.dp))
                                                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                                            ) {
                                                                Text("4K ULTRA", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }

                                                        Text(item.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

                                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            Button(
                                                                onClick = { startPlayback(item) },
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                                                shape = RoundedCornerShape(12.dp),
                                                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                                                                modifier = Modifier.height(36.dp)
                                                            ) {
                                                                Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("شاهد الآن", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                                            }

                                                            OutlinedButton(
                                                                onClick = { toggleFavorite(item.id) },
                                                                border = BorderStroke(1.dp, Color(0x55FFFFFF)),
                                                                shape = RoundedCornerShape(12.dp),
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                                modifier = Modifier.height(36.dp)
                                                            ) {
                                                                val isF = favoriteIds.contains(item.id)
                                                                Icon(if (isF) Icons.Default.Favorite else Icons.Default.Add, null, tint = if (isF) accentColor else Color.White, modifier = Modifier.size(16.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("المفضلة", color = Color.White, fontSize = 12.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (servers.isEmpty()) {
                                        Button(
                                            onClick = { editingServer = null; showAddServerDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(Icons.Default.AddCircleOutline, null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(str.addConnection, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }

                                    if (activeServer != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(cardColor)
                                                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(18.dp))
                                                .clickable { showServerSwitcherSheet = true }
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("${activeServer.countryCode} ${activeServer.title}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(
                                                    accountInfo?.let { "ينتهي في: ${it.expDate} • المتصلون: ${it.activeCons}/${it.maxCons}" }
                                                        ?: "⚡ استجابة السيرفر: ${activeServer.pingMs}ms",
                                                    color = greenText,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                HomeStatPill("مباشر", "${safeLiveList.size}", accentColor) { selectedTab = 1 }
                                                HomeStatPill("أفلام", "${safeVodList.size}", Color(0xFF38BDF8)) { cinemaSubTab = 0; selectedTab = 3 }
                                                HomeStatPill("مسلسلات", "${safeSeriesList.size}", greenText) { cinemaSubTab = 1; selectedTab = 3 }
                                            }
                                        }
                                    }

                                    if (homeMatches.isNotEmpty()) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("⚽ مباريات اليوم", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                            Text("عرض الكل ›", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { selectedTab = 2 })
                                        }
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            items(homeMatches.take(8)) { m ->
                                                val isLiveNow = m.statusState == "in"
                                                Column(
                                                    modifier = Modifier
                                                        .width(210.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(cardColor)
                                                        .border(1.dp, if (isLiveNow) greenText.copy(alpha = 0.5f) else Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
                                                        .clickable { selectedTab = 2 }
                                                        .padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text(m.leagueName, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                                                        Text(m.statusText, color = if (isLiveNow) Color(0xFFF87171) else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                            AsyncImage(model = m.awayLogo, contentDescription = m.awayTeam, modifier = Modifier.size(22.dp))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(m.awayTeam, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                        Text(if (m.statusState == "pre") "VS" else "${m.awayScore}-${m.homeScore}", color = greenText, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                                    }
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                            AsyncImage(model = m.homeLogo, contentDescription = m.homeTeam, modifier = Modifier.size(22.dp))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(m.homeTeam, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                        Text("📺 ${m.suggestedBroadcaster.substringBefore("/")}", color = Color.Gray, fontSize = 9.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Text(str.platforms, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val platforms = listOf("Netflix", "Shahid", "Prime Video", "Disney+", "HBO", "Apple TV")
                                        items(platforms) { plat ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(cardColor)
                                                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                                    .clickable { selectedPlatformFilter = plat; selectedTab = 3 }
                                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                            ) {
                                                Text(plat, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }

                                    if (watchHistory.isNotEmpty()) {
                                        Text(str.continueWatching, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            items(watchHistory) { prog ->
                                                val ratio = (prog.positionMs.toFloat() / prog.durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0.05f, 1f)
                                                Column(
                                                    modifier = Modifier
                                                        .width(135.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(cardColor)
                                                        .clickable { startPlayback(MediaItemModel(prog.id, prog.title, prog.url, prog.subtitleInfo, prog.logo, prog.type), true) }
                                                        .padding(8.dp)
                                                ) {
                                                    Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                                                        AsyncImage(model = prog.logo, contentDescription = prog.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
                                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).background(Color(0xAA000000), CircleShape).clickable {
                                                            AppStorage.removeWatchProgress(context, prog.id)
                                                            watchHistory = AppStorage.getWatchHistory(context)
                                                        })
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
                                        Text(str.recentChannels, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            items(recentChannels) { rCh ->
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.width(95.dp).clip(RoundedCornerShape(14.dp)).background(cardColor).clickable { startPlayback(rCh) }.padding(8.dp)
                                                ) {
                                                    Box(modifier = Modifier.fillMaxWidth().height(55.dp)) {
                                                        ChannelMonogramBadge(rCh.name, rCh.logo, accentColor, Modifier.fillMaxSize())
                                                        Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.align(Alignment.TopEnd).size(15.dp).clickable {
                                                            AppStorage.removeRecentChannel(context, rCh.url)
                                                            recentChannels = AppStorage.getRecentChannels(context)
                                                        })
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(rCh.name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(str.myFolders, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
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

                            1 -> {
                                var openedLiveCategory by remember { mutableStateOf<String?>(null) }
                                val catsListState = rememberLazyListState()

                                BackHandler(enabled = openedLiveCategory != null) {
                                    openedLiveCategory = null
                                    searchQuery = ""
                                }

                                val categoryStats by produceState(initialValue = listOf<Pair<String, Int>>(), safeLiveList, favoriteIds, searchQuery) {
                                    value = withContext(Dispatchers.Default) {
                                        val counts = safeLiveList.groupingBy { it.category }.eachCount()
                                        val result = ArrayList<Pair<String, Int>>(counts.size + 2)
                                        result.add(str.all to safeLiveList.size)
                                        result.add("المفضلة" to safeLiveList.count { favoriteIds.contains(it.id) })
                                        counts.forEach { (cat, count) ->
                                            if (searchQuery.isBlank() || cat.contains(searchQuery, ignoreCase = true)) {
                                                result.add(cat to count)
                                            }
                                        }
                                        result
                                    }
                                }

                                val alphabetLetters = remember { listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "K", "L", "M", "N", "O", "P", "R", "S", "T", "U", "V", "W") }

                                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text(str.searchHint, fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.Search, null, tint = accentColor) },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
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
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("المجموعات (${categoryStats.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("A-Z فهرس سريع", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Row(modifier = Modifier.fillMaxSize()) {
                                            LazyColumn(
                                                state = catsListState,
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                contentPadding = PaddingValues(bottom = 12.dp),
                                                modifier = Modifier.weight(1f)
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
                                                            Icon(if (catName == "المفضلة") Icons.Default.Favorite else Icons.Default.LiveTv, null, tint = accentColor, modifier = Modifier.size(19.dp))
                                                            Spacer(modifier = Modifier.width(12.dp))
                                                            Text(catName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("$count", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Icon(Icons.Default.KeyboardArrowLeft, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                                        }
                                                    }
                                                }
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .padding(start = 4.dp)
                                                    .fillMaxHeight()
                                                    .verticalScroll(rememberScrollState()),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                alphabetLetters.forEach { letter ->
                                                    Text(
                                                        letter,
                                                        color = Color.Gray,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier
                                                            .clickable {
                                                                val targetIdx = categoryStats.indexOfFirst { it.first.uppercase().contains(letter) }
                                                                if (targetIdx >= 0) {
                                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    scope.launch { catsListState.scrollToItem(targetIdx) }
                                                                }
                                                            }
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
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
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f).clickable { openedLiveCategory = null; searchQuery = "" },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.ArrowForward, null, tint = accentColor, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    if (searchQuery.isNotBlank()) "نتائج البحث" else (openedLiveCategory ?: ""),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    liveGridMode = !liveGridMode
                                                    prefs.edit().putBoolean("live_grid_mode", liveGridMode).apply()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    if (liveGridMode) Icons.Default.ViewList else Icons.Default.GridView,
                                                    null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        val groupedLive by produceState(initialValue = listOf<GroupedSportsChannel>(), safeLiveList, openedLiveCategory, searchQuery, favoriteIds) {
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
                                                ServersEngine.groupChannelsByQuality(filtered.take(500))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (liveGridMode) {
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
                                                        Box(modifier = Modifier.fillMaxWidth().height(54.dp)) {
                                                            ChannelMonogramBadge(grp.cleanName, grp.logo, accentColor, Modifier.fillMaxSize())
                                                            Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isFav) accentColor else Color.Gray, modifier = Modifier.align(Alignment.TopEnd).size(17.dp).clickable { toggleFavorite(primaryItem.id) })
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(grp.cleanName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        if (grp.variants.size > 1) {
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Box(modifier = Modifier.background(greenBadge, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
                                                                Text("${grp.variants.size} جودات", color = greenText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            LazyColumn(
                                                contentPadding = PaddingValues(bottom = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                itemsIndexed(groupedLive) { index, grp ->
                                                    val primaryItem = grp.variants.first().item
                                                    val isFav = grp.variants.any { favoriteIds.contains(it.item.id) }
                                                    val isPlayingThis = grp.variants.any { it.item.url == currentMedia?.url }

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .background(if (isPlayingThis) accentColor.copy(alpha = 0.16f) else cardColor)
                                                            .border(1.dp, if (isPlayingThis) accentColor else Color(0x14FFFFFF), RoundedCornerShape(14.dp))
                                                            .clickable {
                                                                if (grp.variants.size == 1) startPlayback(primaryItem)
                                                                else selectedQualityGroup = grp
                                                            }
                                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Text(
                                                            String.format("%03d", index + 1),
                                                            color = Color.Gray,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        ChannelMonogramBadge(grp.cleanName, grp.logo, accentColor, Modifier.size(36.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                grp.cleanName,
                                                                color = if (isPlayingThis) accentColor else Color.White,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            if (grp.variants.size > 1) {
                                                                Text("${grp.variants.size} جودات متوفرة", color = greenText, fontSize = 10.sp)
                                                            }
                                                        }
                                                        Icon(
                                                            Icons.Default.CreateNewFolder,
                                                            null,
                                                            tint = Color.Gray,
                                                            modifier = Modifier.size(18.dp).clickable { folderTargetItem = primaryItem }
                                                        )
                                                        Icon(
                                                            if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                                            null,
                                                            tint = if (isFav) Color(0xFFFBBF24) else Color.Gray,
                                                            modifier = Modifier.size(20.dp).clickable { toggleFavorite(primaryItem.id) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                var todayMatches by remember { mutableStateOf(listOf<LiveMatchInfo>()) }
                                var isLoadingMatches by remember { mutableStateOf(false) }
                                var selectedMatchSheet by remember { mutableStateOf<LiveMatchInfo?>(null) }
                                var liveEpgMap by remember { mutableStateOf(mapOf("" to "")) }
                                var liveOnlyFilter by remember { mutableStateOf(false) }

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

                                val filteredMatches = remember(todayMatches, liveOnlyFilter, searchQuery) {
                                    todayMatches.filter { m ->
                                        (!liveOnlyFilter || m.statusState == "in") &&
                                            (searchQuery.isBlank() || m.homeTeam.contains(searchQuery, true) || m.awayTeam.contains(searchQuery, true) || m.leagueName.contains(searchQuery, true))
                                    }
                                }
                                val groupedByLeague = remember(filteredMatches) {
                                    filteredMatches.groupBy { it.leagueName }
                                }
                                val liveMatchesCount = remember(todayMatches) { todayMatches.count { it.statusState == "in" } }

                                LazyColumn(
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    item {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                item {
                                                    FilterChip(
                                                        selected = liveOnlyFilter,
                                                        onClick = { liveOnlyFilter = !liveOnlyFilter },
                                                        label = { Text("🔴 مباشر ($liveMatchesCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                                    )
                                                }
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
                                                        label = { Text("القنوات العربية", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                                    )
                                                }
                                                item {
                                                    FilterChip(
                                                        selected = sportsSubFilter == 2,
                                                        onClick = { sportsSubFilter = 2 },
                                                        label = { Text("القنوات العالمية", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                                    )
                                                }
                                            }

                                            OutlinedTextField(
                                                value = searchQuery,
                                                onValueChange = { searchQuery = it },
                                                placeholder = { Text("ابحث عن مباراة، بطولة، أو قناة...", fontSize = 12.sp) },
                                                leadingIcon = { Icon(Icons.Default.SportsSoccer, null, tint = accentColor) },
                                                trailingIcon = {
                                                    if (searchQuery.isNotEmpty()) {
                                                        IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                                                    }
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    if (displaySports.isNotEmpty()) {
                                        item {
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(displaySports) { grp ->
                                                    Row(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .background(cardColor)
                                                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                                            .clickable {
                                                                if (grp.variants.size == 1) startPlayback(grp.variants[0].item)
                                                                else selectedQualityGroup = grp
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        ChannelMonogramBadge(grp.cleanName, grp.logo, accentColor, Modifier.size(30.dp))
                                                        Column {
                                                            Text(grp.cleanName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                                            if (grp.variants.size > 1) {
                                                                Text("${grp.variants.size} جودات", color = greenText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (isLoadingMatches) {
                                        item { LinearProgressIndicator(color = accentColor, modifier = Modifier.fillMaxWidth()) }
                                    } else {
                                        groupedByLeague.forEach { (leagueTitle, leagueMatches) ->
                                            item(key = "league_$leagueTitle") {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("🏆 $leagueTitle", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("${leagueMatches.size}", color = Color.Gray, fontSize = 11.sp)
                                                }
                                            }

                                            items(leagueMatches, key = { it.id }) { match ->
                                                val isLiveNow = match.statusState == "in"
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = cardColor),
                                                    border = BorderStroke(1.dp, if (isLiveNow) greenText.copy(alpha = 0.6f) else Color(0x1FFFFFFF)),
                                                    shape = RoundedCornerShape(16.dp),
                                                    modifier = Modifier.fillMaxWidth().clickable { selectedMatchSheet = match }
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                                AsyncImage(model = match.awayLogo, contentDescription = match.awayTeam, modifier = Modifier.size(26.dp))
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text(match.awayTeam, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                            }

                                                            Box(
                                                                modifier = Modifier
                                                                    .padding(horizontal = 8.dp)
                                                                    .background(if (isLiveNow) greenBadge else Color(0xFF1A1A26), RoundedCornerShape(10.dp))
                                                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                                            ) {
                                                                Text(
                                                                    if (match.statusState == "pre") "VS" else "${match.awayScore} - ${match.homeScore}",
                                                                    color = if (isLiveNow) greenText else Color.White,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    fontSize = 14.sp
                                                                )
                                                            }

                                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.weight(1f)) {
                                                                Text(match.homeTeam, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                AsyncImage(model = match.homeLogo, contentDescription = match.homeTeam, modifier = Modifier.size(26.dp))
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                match.statusText,
                                                                color = if (isLiveNow) Color(0xFFF87171) else Color.Gray,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                "📺 ${match.suggestedBroadcaster}  •  التفاصيل والقناة ›",
                                                                color = accentColor,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                selectedMatchSheet?.let { m ->
                                    val (matchingChannels, isEpgVerified) = remember(m, groupedSports, liveEpgMap) {
                                        ServersEngine.findExactChannelsForMatch(m, groupedSports, liveEpgMap)
                                    }

                                    NovaMatchCenterBottomSheet(
                                        match = m,
                                        matchingChannels = matchingChannels,
                                        isEpgVerified = isEpgVerified,
                                        liveEpgMap = liveEpgMap,
                                        accentColor = accentColor,
                                        cardColor = cardColor,
                                        bgColor = bgColor,
                                        onDismiss = { selectedMatchSheet = null },
                                        onPlayChannelItem = { startPlayback(it) },
                                        onOpenChannelVariants = { selectedQualityGroup = it }
                                    )
                                }
                            }

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
                                        placeholder = { Text("ابحث في الأفلام والمسلسلات...", fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.Search, null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(filteredCinema) { item ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(cardColor).clickable { startPlayback(item) }.padding(6.dp)
                                            ) {
                                                AsyncImage(model = item.logo, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(10.dp)))
                                                Spacer(modifier = Modifier.height(5.dp))
                                                Text(item.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }

                            4 -> {
                                AdvancedSettingsScreen(
                                    prefs = prefs,
                                    servers = servers,
                                    activeServerId = activeServerId,
                                    activeServer = activeServer,
                                    accountInfo = accountInfo,
                                    appLang = appLang,
                                    onAppLangChange = { langVal: String -> appLang = langVal; prefs.edit().putString("app_lang", langVal).apply() },
                                    accentIdx = accentIdx,
                                    accentOptions = accentOptions,
                                    onAccentChange = { idxVal: Int -> accentIdx = idxVal; prefs.edit().putInt("accent_idx", idxVal).apply() },
                                    bgIdx = bgIdx,
                                    bgOptions = bgOptions,
                                    onBgChange = { bgVal: Int -> bgIdx = bgVal; prefs.edit().putInt("bg_idx", bgVal).apply() },
                                    startupTab = startupTab,
                                    onStartupTabChange = { tabVal: Int -> startupTab = tabVal; prefs.edit().putInt("startup_tab", tabVal).apply() },
                                    defaultPlayer = defaultPlayer,
                                    onDefaultPlayerChange = { plVal: String -> defaultPlayer = plVal; prefs.edit().putString("default_player", plVal).apply() },
                                    decoderMode = decoderMode,
                                    onDecoderChange = { decVal: String -> decoderMode = decVal; prefs.edit().putString("decoder_mode", decVal).apply() },
                                    bufferProfile = bufferProfile,
                                    onBufferChange = { bufVal: String -> bufferProfile = bufVal; prefs.edit().putString("buffer_profile", bufVal).apply() },
                                    streamFmt = streamFmt,
                                    onStreamFmtChange = { fmtVal: String -> streamFmt = fmtVal; prefs.edit().putString("stream_fmt", fmtVal).apply() },
                                    sameHost = sameHost,
                                    onSameHostChange = { hostBool: Boolean -> sameHost = hostBool; prefs.edit().putBoolean("same_host", hostBool).apply() },
                                    userAgent = userAgent,
                                    onUserAgentChange = { uaVal: String -> userAgent = uaVal; prefs.edit().putString("user_agent", uaVal).apply() },
                                    gesturesEnabled = gesturesEnabled,
                                    onGesturesChange = { gestBool: Boolean -> gesturesEnabled = gestBool; prefs.edit().putBoolean("gestures_enabled", gestBool).apply() },
                                    autoplayNextEp = autoplayNextEp,
                                    onAutoplayChange = { autoBool: Boolean -> autoplayNextEp = autoBool; prefs.edit().putBoolean("autoplay_next_ep", autoBool).apply() },
                                    subFontSize = subFontSize,
                                    onSubFontSizeChange = { sizeVal: Float -> subFontSize = sizeVal; prefs.edit().putFloat("sub_font_size", sizeVal).apply() },
                                    subColorYellow = subColorYellow,
                                    onSubColorYellowChange = { yelBool: Boolean -> subColorYellow = yelBool; prefs.edit().putBoolean("sub_yellow", yelBool).apply() },
                                    sleepMinutesLeft = sleepMinutesLeft,
                                    onSleepMinutesChange = { minVal: Int -> sleepMinutesLeft = minVal },
                                    parentalPin = parentalPin,
                                    onParentalPinChange = { pinVal: String -> parentalPin = pinVal },
                                    accentColor = accentColor,
                                    cardColor = cardColor,
                                    onSelectServer = { srvItem: SavedServer -> syncServerData(srvItem, false); selectedTab = 1 },
                                    onAddServerClick = { editingServer = null; showAddServerDialog = true },
                                    onEditServerClick = { srvItem: SavedServer -> editingServer = srvItem; showAddServerDialog = true },
                                    onDeleteServerClick = { srvItem: SavedServer ->
                                        val upd = servers.filter { s -> s.id != srvItem.id }
                                        servers = upd; saveServersList(prefs, upd)
                                    },
                                    onOpenCategoriesManager = { showCategoriesManager = true },
                                    onOpenBackupDialog = { showBackupDialog = true }
                                )
                            }
                        }
                    }
                }
            }

            if (showServerSwitcherSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showServerSwitcherSheet = false },
                    containerColor = cardColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الاشتراكات المحفوظة (${servers.size})", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text(
                                "+ إضافة جديد",
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable {
                                    showServerSwitcherSheet = false
                                    editingServer = null
                                    showAddServerDialog = true
                                }
                            )
                        }

                        if (servers.isEmpty()) {
                            Text("لا توجد اشتراكات مضافة حالياً.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                        } else {
                            servers.forEach { srv ->
                                val isAct = srv.id == activeServerId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isAct) accentColor.copy(alpha = 0.16f) else bgColor)
                                        .border(1.dp, if (isAct) accentColor else Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showServerSwitcherSheet = false
                                            syncServerData(srv, false)
                                            Toast.makeText(context, "تم التبديل إلى: ${srv.title}", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isAct) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        null,
                                        tint = if (isAct) greenText else Color.Gray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(srv.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("${srv.countryCode} • ${srv.type.uppercase()} • ⚡ ${srv.pingMs}ms", color = if (isAct) greenText else Color.Gray, fontSize = 11.sp, maxLines = 1)
                                    }
                                    if (isAct) {
                                        Box(modifier = Modifier.background(greenBadge, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                            Text("نشط", color = greenText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }
            }

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

            if (showAddServerDialog) {
                var titleIn by remember { mutableStateOf(editingServer?.title ?: "") }
                var typeIn by remember { mutableStateOf(editingServer?.type ?: "xtream") }
                var hostIn by remember { mutableStateOf(editingServer?.host ?: "") }
                var userIn by remember { mutableStateOf(editingServer?.user ?: "") }
                var passIn by remember { mutableStateOf(editingServer?.pass ?: "") }
                val existingId = editingServer?.id ?: ""
                var syncMoviesToggle by remember { mutableStateOf(if (existingId.isNotEmpty()) prefs.getBoolean("sync_vod_$existingId", true) else true) }
                var syncSeriesToggle by remember { mutableStateOf(if (existingId.isNotEmpty()) prefs.getBoolean("sync_series_$existingId", true) else true) }

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
                                HorizontalDivider(color = Color(0x1FFFFFFF))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("تحميل الأفلام", color = Color.White, fontSize = 13.sp)
                                    Switch(checked = syncMoviesToggle, onCheckedChange = { syncMoviesToggle = it })
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("تحميل المسلسلات", color = Color.White, fontSize = 13.sp)
                                    Switch(checked = syncSeriesToggle, onCheckedChange = { syncSeriesToggle = it })
                                }
                                Text("عند الإيقاف يُحرر الكاش ويبقى البث المباشر فقط", color = Color.Gray, fontSize = 10.sp)
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
                            val fTitle = titleIn.ifBlank { Uri.parse(fHost).host ?: "اشتراك" }
                            val finalId = editingServer?.id ?: System.currentTimeMillis().toString()

                            prefs.edit()
                                .putBoolean("sync_vod_$finalId", syncMoviesToggle)
                                .putBoolean("sync_series_$finalId", syncSeriesToggle)
                                .apply()

                            if (!syncMoviesToggle) vodList = emptyList()
                            if (!syncSeriesToggle) seriesList = emptyList()

                            val newSrv = SavedServer(finalId, fTitle, fType, fHost, fUser, fPass)
                            val upd = if (editingServer == null) servers + newSrv else servers.map { if (it.id == newSrv.id) newSrv else it }
                            servers = upd; saveServersList(prefs, upd)
                            showAddServerDialog = false
                            syncServerData(newSrv, true)
                        }) { Text("حفظ التغييرات") }
                    },
                    dismissButton = { TextButton(onClick = { showAddServerDialog = false }) { Text("إلغاء", color = Color.Gray) } }
                )
            }

            if (showCreateFolderDialog) {
                var folderName by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreateFolderDialog = false },
                    containerColor = cardColor,
                    title = { Text("إنشاء مجلد شخصي جديد", color = Color.White) },
                    text = { OutlinedTextField(value = folderName, onValueChange = { folderName = it }, label = { Text("اسم المجلد") }, singleLine = true) },
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
                                Toast.makeText(context, "تم نسخ كود النسخة الاحتياطية", Toast.LENGTH_LONG).show()
                            }, modifier = Modifier.fillMaxWidth()) { Text("نسخ كود النسخة الاحتياطية") }
                            OutlinedTextField(value = codeIn, onValueChange = { codeIn = it }, label = { Text("الصق كود الاستعادة هنا") })
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (AppStorage.importBackupCode(context, codeIn)) {
                                servers = loadSavedServers(prefs)
                                customFolders = AppStorage.getCustomFolders(context)
                                Toast.makeText(context, "تمت الاستعادة بنجاح", Toast.LENGTH_SHORT).show()
                                showBackupDialog = false
                            }
                        }) { Text("استعادة") }
                    }
                )
            }

            if (showPinDialog) {
                var pinInput by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showPinDialog = false },
                    containerColor = cardColor,
                    title = { Text("أدخل الرمز السري", color = Color.White, fontSize = 14.sp) },
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

@Composable
private fun HomeStatPill(label: String, count: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(count, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        Text(label, color = Color.LightGray, fontSize = 9.sp)
    }
}
// END_MAIN
