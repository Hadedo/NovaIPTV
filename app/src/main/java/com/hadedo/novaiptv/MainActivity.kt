package com.hadedo.novaiptv

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DevkanAppRoot()
        }
    }
}

data class AppStrings(
    val home: String,
    val live: String,
    val sports: String,
    val moviesTab: String,
    val seriesTab: String,
    val settings: String,
    val continueWatching: String,
    val latestMovies: String,
    val featuredSeries: String,
    val movies4k: String,
    val searchHint: String,
    val all: String,
    val addConnection: String,
    val importFile: String,
    val epgGuide: String,
    val speedTest: String,
    val engineTitle: String,
    val autoSyncTitle: String,
    val appLockTitle: String,
    val subscriptionCard: String,
    val statusLabel: String,
    val expiryLabel: String,
    val countryLabel: String,
    val connectionsLabel: String,
    val lowLatencyTitle: String,
    val geoBypassTitle: String,
    val autoResumeTitle: String
)

fun getStrings(lang: String): AppStrings = when (lang) {
    "en" -> AppStrings("Home", "Live TV", "Sports", "Movies", "Series", "Settings", "Continue Watching", "Latest Releases", "Featured Series", "4K UHD Movies", "Search channels, movies, series...", "All", "Add Connection", "Import M3U File", "EPG Grid", "Server Speed & Ping", "Playback Engine", "Auto-Refresh Catalog", "App Lock PIN", "Subscription Details", "Status", "Expires", "Allowed Region", "Connections", "Ultra-Low Latency", "Smart Geo-Bypass", "Auto-Resume Last Channel")
    "de" -> AppStrings("Start", "Live-TV", "Sport", "Filme", "Serien", "Optionen", "Weiterschauen", "Neueste Filme", "Beliebte Serien", "4K-Filme", "Kanäle, Filme, Serien suchen...", "Alle", "Verbindung hinzufügen", "M3U-Datei laden", "EPG-Plan", "Server-Speedtest", "Wiedergabe-Engine", "Auto-Aktualisierung", "App-PIN-Sperre", "Abonnement-Details", "Status", "Ablaufdatum", "Region / Land", "Verbindungen", "Geringe Latenz", "Geo-Bypass-Header", "Letzten Kanal fortsetzen")
    "tr" -> AppStrings("Ana Sayfa", "Canlı", "Spor", "Filmler", "Diziler", "Ayarlar", "Devam Et", "Yeni Filmler", "Öne Çıkan Diziler", "4K Filmler", "Kanal, film, dizi ara...", "Tümü", "Bağlantı Ekle", "M3U Dosyası", "EPG Rehberi", "Sunucu Hız Testi", "Oynatıcı Motoru", "Otomatik Yenileme", "Uygulama PIN", "Abonelik Bilgileri", "Durum", "Bitiş Tarihi", "Ülke / Bölge", "Bağlantılar", "Düşük Gecikme Modu", "Bölge Kısıtlama Aşımı", "Son Kanalı Otomatik Aç")
    "fr" -> AppStrings("Accueil", "Direct", "Sports", "Films", "Séries", "Réglages", "Reprendre", "Derniers Films", "Séries en Vedette", "Films 4K", "Rechercher...", "Tout", "Ajouter Connexion", "Fichier M3U", "Guide EPG", "Test Serveur", "Moteur Vidéo", "Actualisation Auto", "Code PIN App", "Abonnement", "Statut", "Expiration", "Pays Autorisé", "Connexions", "Faible Latence", "Contournement Géo", "Reprise Auto")
    "ku" -> AppStrings("سەرەکی", "ڕاستەوخۆ", "وەرزش", "فیلم", "زنجیرە", "ڕێکخستن", "بەردەوامبە", "نوێترین فیلمەکان", "زنجیرە دیارەکان", "فیلمی 4K", "گەڕان...", "هەموو", "زیادکردنی بەشداری", "فایلی M3U", "خشتەی EPG", "پشکنینی خێرایی", "بزوێنەری کارپێکردن", "نوێکردنەوەی خۆکار", "قفڵی بەرنامە", "زانیاری بەشداری", "دۆخ", "بەرواری بەسەرچوون", "وڵات", "پەیوەندییەکان", "کەمکردنەوەی دواکەوتن", "کردنەوەی بلۆکی وڵات", "کردنەوەی دوایین کەناڵ")
    else -> AppStrings("الرئيسية", "مباشر", "الرياضة", "أفلام", "مسلسلات", "الإعدادات", "واصل المشاهدة", "أحدث الإصدارات", "المسلسلات المميزة", "أفلام 4K الحقيقية", "ابحث عن قناة أو فيلم أو مسلسل...", "الكل", "إضافة اتصال", "استيراد ملف M3U", "جدول EPG", "فحص سرعة السيرفر", "محرك التشغيل", "تحديث الواجهة والمكتبة تلقائياً", "قفل التطبيق PIN", "بطاقة الاشتراك", "الحالة", "تاريخ الانتهاء", "الدولة / النطاق", "الاتصالات", "تقليل تأخير البث عن الستلايت", "تجاوز قيود السيرفر والدولة", "فتح آخر قناة تلقائياً عند التشغيل")
}

@Composable
fun DevkanAppRoot() {
    val context = LocalContext.current
    var lang by remember { mutableStateOf(DevkanVault.getLanguage(context)) }
    var disclaimerAccepted by remember { mutableStateOf(DevkanVault.isDisclaimerAccepted(context)) }
    val savedPin = remember { DevkanVault.getAppLockPin(context) }
    var isUnlocked by remember { mutableStateOf(savedPin.isEmpty()) }

    val layoutDirection = if (lang == "ar" || lang == "ku") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            when {
                !disclaimerAccepted -> {
                    LegalDisclaimerScreen(
                        lang = lang,
                        onAccept = {
                            DevkanVault.setDisclaimerAccepted(context, true)
                            disclaimerAccepted = true
                        }
                    )
                }
                !isUnlocked && savedPin.isNotEmpty() -> {
                    AppLockScreen(
                        lang = lang,
                        expectedPin = savedPin,
                        onUnlocked = { isUnlocked = true }
                    )
                }
                else -> {
                    DevkanMainDashboard(
                        lang = lang,
                        onLanguageChange = { newLang ->
                            lang = newLang
                            DevkanVault.setLanguage(context, newLang)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DevkanBrandHeader(
    activeSubLabel: String,
    onSwitchSubClick: () -> Unit,
    onSearchClick: () -> Unit,
    onImportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    strings: AppStrings
) {
    val goldLight = Color(0xFFFDE047)
    val goldMid = Color(0xFFD4AF37)
    val goldDark = Color(0xFFB45309)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0F))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Brush.linearGradient(listOf(goldLight, goldMid, goldDark)))
                    .border(1.dp, goldLight.copy(alpha = 0.8f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = "DEVKAN 34 VIP",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text("DEVKAN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 0.8.sp)
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF171723))
                .border(1.dp, goldMid.copy(alpha = 0.45f), RoundedCornerShape(50))
                .clickable { onSwitchSubClick() }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = goldLight, modifier = Modifier.size(16.dp))
            Text(
                text = activeSubLabel.ifEmpty { "إضافة اشتراك" },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 105.dp)
            )
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = goldMid, modifier = Modifier.size(15.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(32.dp).background(Color(0xFF161620), CircleShape)
            ) {
                Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color.White, modifier = Modifier.size(17.dp))
            }
            IconButton(
                onClick = onImportClick,
                modifier = Modifier.size(32.dp).background(Color(0xFF161620), CircleShape)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = strings.importFile, tint = goldLight, modifier = Modifier.size(17.dp))
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(32.dp).background(Color(0xFF161620), CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = strings.settings, tint = goldMid, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevkanMainDashboard(lang: String, onLanguageChange: (String) -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val strings = remember(lang) { getStrings(lang) }
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedSportNetwork by remember { mutableStateOf("ALL") }
    var activeStream by remember { mutableStateOf<MediaItemModel?>(null) }
    var isSportsPlayback by remember { mutableStateOf(false) }
    var selectedMovieForDetails by remember { mutableStateOf<MediaItemModel?>(null) }
    var selectedSeriesForDetails by remember { mutableStateOf<MediaItemModel?>(null) }
    var selectedMatchForChannelPicker by remember { mutableStateOf<RealSportsMatch?>(null) }
    var selectedGroupedMatchForQuality by remember { mutableStateOf<GroupedSportsMatch?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showQuickSubSwitcher by remember { mutableStateOf(false) }
    var subToEdit by remember { mutableStateOf<SavedSubscription?>(null) }
    var showEpgSheet by remember { mutableStateOf(false) }
    var showQuickSearch by remember { mutableStateOf(false) }
    var quickSearchQuery by remember { mutableStateOf("") }
    var selectedLiveCategory by remember { mutableStateOf("ALL") }
    var liveFilterQuery by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    var movieSmartFilter by remember { mutableStateOf("ALL") }
    var movieServerGroup by remember { mutableStateOf("ALL") }
    var movieQuery by remember { mutableStateOf("") }
    var showMovieGroupsSheet by remember { mutableStateOf(false) }

    var seriesSmartFilter by remember { mutableStateOf("ALL") }
    var seriesServerGroup by remember { mutableStateOf("ALL") }
    var seriesQuery by remember { mutableStateOf("") }
    var showSeriesGroupsSheet by remember { mutableStateOf(false) }

    val moviesGridState = rememberLazyGridState()
    val seriesGridState = rememberLazyGridState()
    val liveListState = rememberLazyListState()
    val sportsListState = rememberLazyListState()

    var currentSub by remember { mutableStateOf(DevkanVault.getSavedSubscription(context)) }
    val allItems = remember { mutableStateListOf<MediaItemModel>() }
    val todayRealMatches = remember { mutableStateListOf<RealSportsMatch>() }
    val watchlistIds = remember { mutableStateListOf<String>() }

    var cachedSportsChannels by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedLiveChannels by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedLiveGroups by remember { mutableStateOf<List<String>>(listOf("ALL")) }
    var filteredLiveList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var filteredMoviesList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var filteredSeriesList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var vodProgressCache by remember { mutableStateOf(mapOf("" to 0f)) }
    val emptySports = ClassifiedSportsData(emptyList(), emptyList(), emptyList(), emptyList())
    var classifiedSports by remember { mutableStateOf(emptySports) }

    fun switchSubscriptionFast(targetSub: SavedSubscription) {
        DevkanVault.switchActiveSubscription(context, targetSub.id)
        currentSub = targetSub
        selectedLiveCategory = "ALL"
        movieSmartFilter = "ALL"
        movieServerGroup = "ALL"
        seriesSmartFilter = "ALL"
        seriesServerGroup = "ALL"

        scope.launch {
            val subCache = withContext(Dispatchers.IO) { DevkanVault.getCachedItems(context, targetSub.id) }
            if (subCache.isNotEmpty()) {
                allItems.clear()
                allItems.addAll(subCache)
                Toast.makeText(context, "تم التبديل إلى: ${targetSub.label}", Toast.LENGTH_SHORT).show()
            }
            isSyncing = true
            val (updated, freshItems) = DevkanPlaylistRepository.loadSubscriptionContent(targetSub, DevkanVault.isGeoBypassEnabled(context))
            currentSub = updated
            DevkanVault.saveSubscription(context, updated)
            if (freshItems.isNotEmpty()) {
                allItems.clear()
                allItems.addAll(freshItems)
                withContext(Dispatchers.IO) { DevkanVault.saveCachedItems(context, freshItems, updated.id) }
                classifiedSports = DevkanPlaylistRepository.syncAndClassifySportsEpg(context, updated, freshItems)
                if (subCache.isEmpty()) {
                    Toast.makeText(context, "تم الاتصال بـ: ${updated.label}", Toast.LENGTH_SHORT).show()
                }
            }
            isSyncing = false
        }
    }

    LaunchedEffect(allItems.size) {
        if (allItems.isEmpty()) {
            cachedLiveChannels = emptyList()
            cachedLiveGroups = listOf("ALL")
            cachedSportsChannels = emptyList()
        } else {
            val snapshot = allItems.toList()
            withContext(Dispatchers.Default) {
                val live = snapshot.filter { it.contentType == "LIVE" }
                val groups = listOf("ALL") + live.map { it.group }.distinct()
                val rawSports = live.filter { SportsMatchExtractor.isSportsProgram(it.group, it.title, null) }
                val dedupedSports = DevkanPlaylistRepository.deduplicateSportChannels(rawSports)

                withContext(Dispatchers.Main) {
                    cachedLiveChannels = live
                    cachedLiveGroups = groups
                    cachedSportsChannels = dedupedSports
                }
            }
        }
    }

    LaunchedEffect(cachedLiveChannels, selectedLiveCategory, liveFilterQuery) {
        val q = liveFilterQuery.trim()
        filteredLiveList = withContext(Dispatchers.Default) {
            cachedLiveChannels.filter { ch ->
                (selectedLiveCategory == "ALL" || ch.group == selectedLiveCategory) &&
                    (q.isEmpty() || ch.title.contains(q, ignoreCase = true))
            }
        }
    }

    LaunchedEffect(allItems.size, movieSmartFilter, movieServerGroup, movieQuery, activeStream) {
        if (allItems.isEmpty()) {
            filteredMoviesList = emptyList()
        } else {
            val snapshot = allItems.toList()
            val q = movieQuery.trim()
            val result = withContext(Dispatchers.Default) {
                val moviesOnly = snapshot.filter { it.contentType == "MOVIE" }
                val byGroup = if (movieServerGroup == "ALL") moviesOnly else moviesOnly.filter { it.group == movieServerGroup }
                val byFilter = when (movieSmartFilter) {
                    "ALL" -> byGroup
                    "RECENT" -> byGroup.sortedByDescending { it.addedTimestamp }
                    "4K" -> byGroup.filter { it.is4k }
                    "TOP" -> byGroup.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
                    "عربي", "كوري", "تركي", "هندي", "أجنبي" -> byGroup.filter { it.originTag == movieSmartFilter }
                    else -> byGroup.filter { it.genreTag == movieSmartFilter }
                }
                if (q.isEmpty()) byFilter else byFilter.filter { it.title.contains(q, ignoreCase = true) }
            }
            filteredMoviesList = result

            val progressMap = withContext(Dispatchers.IO) {
                val map = HashMap<String, Float>()
                for (item in result.take(250)) {
                    val p = DevkanVault.getVodProgressFraction(context, item.id)
                    if (p > 0.02f) map[item.id] = p
                }
                map
            }
            vodProgressCache = progressMap
        }
    }

    LaunchedEffect(allItems.size, seriesSmartFilter, seriesServerGroup, seriesQuery, activeStream) {
        if (allItems.isEmpty()) {
            filteredSeriesList = emptyList()
        } else {
            val snapshot = allItems.toList()
            val q = seriesQuery.trim()
            val result = withContext(Dispatchers.Default) {
                val seriesOnly = snapshot.filter { it.contentType == "SERIES" }
                val byGroup = if (seriesServerGroup == "ALL") seriesOnly else seriesOnly.filter { it.group == seriesServerGroup }
                val byFilter = when (seriesSmartFilter) {
                    "ALL" -> byGroup
                    "RECENT" -> byGroup.sortedByDescending { it.addedTimestamp }
                    "TOP" -> byGroup.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
                    "عربي", "كوري", "تركي", "هندي", "أجنبي" -> byGroup.filter { it.originTag == seriesSmartFilter }
                    else -> byGroup.filter { it.genreTag == seriesSmartFilter }
                }
                if (q.isEmpty()) byFilter else byFilter.filter { it.title.contains(q, ignoreCase = true) }
            }
            filteredSeriesList = result
        }
    }

    BackHandler(
        enabled = activeStream == null &&
            selectedMovieForDetails == null &&
            selectedSeriesForDetails == null &&
            !showEpgSheet &&
            !showQuickSearch &&
            selectedMatchForChannelPicker == null &&
            selectedGroupedMatchForQuality == null
    ) {
        if (selectedTab != 0) {
            selectedTab = 0
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000L) {
                activity?.finish()
            } else {
                lastBackPressTime = now
                Toast.makeText(context, "اضغط رجوع مرة أخرى للخروج من DEVKAN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        val saved = DevkanVault.getSavedSubscription(context)
        val cached = withContext(Dispatchers.IO) { DevkanVault.getCachedItems(context, saved?.id ?: "default") }
        if (cached.isNotEmpty()) {
            allItems.clear()
            allItems.addAll(cached)
        }

        val cachedEpg = withContext(Dispatchers.IO) { DevkanVault.getCachedSportsEpg(context) }
        if (cachedEpg.isNotEmpty()) {
            classifiedSports = withContext(Dispatchers.Default) {
                EpgSportsPipeline.classifyAndGroupSports(cachedEpg)
            }
        }

        if (saved != null && (cached.isEmpty() || DevkanVault.shouldRunBackgroundSync(context))) {
            launch {
                if (cached.isEmpty()) isSyncing = true
                val geoBypass = DevkanVault.isGeoBypassEnabled(context)
                val (updatedSub, loadedItems) = DevkanPlaylistRepository.loadSubscriptionContent(saved, geoBypass)
                currentSub = updatedSub
                DevkanVault.saveSubscription(context, updatedSub)
                if (loadedItems.isNotEmpty()) {
                    allItems.clear()
                    allItems.addAll(loadedItems)
                    withContext(Dispatchers.IO) { DevkanVault.saveCachedItems(context, loadedItems, updatedSub.id) }
                    classifiedSports = DevkanPlaylistRepository.syncAndClassifySportsEpg(context, updatedSub, loadedItems)
                }
                isSyncing = false
            }
        } else if (saved != null && cachedEpg.isEmpty() && cached.isNotEmpty()) {
            launch {
                classifiedSports = DevkanPlaylistRepository.syncAndClassifySportsEpg(context, saved, cached)
            }
        }

        launch {
            val realMatches = RealSportsDataSource.fetchTodayRealMatches()
            if (realMatches.isNotEmpty()) {
                todayRealMatches.clear()
                todayRealMatches.addAll(realMatches)
            }
        }

        if (DevkanVault.isAutoResumeEnabled(context)) {
            val last = DevkanVault.getLastChannel(context)
            if (last != null) {
                isSportsPlayback = false
                activeStream = MediaItemModel("resume", "0", last.second, last.first, "", "Live", "", "LIVE")
            }
        }
    }

    val m3uFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val imported = DevkanPlaylistRepository.parseLocalUri(context, uri)
                if (imported.isNotEmpty()) {
                    allItems.clear()
                    allItems.addAll(imported)
                    withContext(Dispatchers.IO) { DevkanVault.saveCachedItems(context, imported) }
                    Toast.makeText(context, "تم استيراد وحفظ ${imported.size} عنصر!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun launchGroupedMatchStream(match: GroupedSportsMatch, targetProgram: EpgProgram = match.primaryStream) {
        val matchedChannel = cachedLiveChannels.firstOrNull {
            it.rawId == targetProgram.channelId || it.title.equals(targetProgram.channelName, ignoreCase = true)
        }
        isSportsPlayback = true
        if (matchedChannel != null) {
            activeStream = matchedChannel.copy(
                title = "${match.homeTeam} vs ${match.awayTeam} • ${matchedChannel.title}"
            )
        } else if (currentSub?.type == "XTREAM" && targetProgram.channelId.isNotBlank()) {
            val sub = currentSub!!
            val cleanHost = sub.host.trimEnd('/')
            val url = "$cleanHost/live/${sub.username}/${sub.password}/${targetProgram.channelId}.m3u8"
            val tsFallback = "$cleanHost/live/${sub.username}/${sub.password}/${targetProgram.channelId}.ts"
            activeStream = MediaItemModel(
                id = "live_${targetProgram.channelId}",
                rawId = targetProgram.channelId,
                title = "${match.homeTeam} vs ${match.awayTeam} • ${targetProgram.channelName}",
                url = url,
                fallbackUrl = tsFallback,
                group = targetProgram.category ?: "Sports",
                logo = targetProgram.channelLogo.orEmpty(),
                contentType = "LIVE"
            )
        }
    }

    if (activeStream != null) {
        DevkanHybridPlayerScreen(
            streamUrl = activeStream!!.url,
            fallbackUrl = activeStream!!.fallbackUrl,
            streamTitle = activeStream!!.title,
            isVodOrSeries = activeStream!!.contentType != "LIVE",
            streamId = activeStream!!.id,
            quickChannels = cachedLiveChannels,
            onSwitchChannel = { nextCh ->
                isSportsPlayback = SportsMatchExtractor.isSportsProgram(nextCh.group, nextCh.title, null)
                activeStream = nextCh
            },
            lang = lang,
            onBack = {
                activeStream = null
                isSportsPlayback = false
            }
        )
        return
    }

    if (selectedMovieForDetails != null) {
        val m = selectedMovieForDetails!!
        MovieDetailsScreen(
            movie = m,
            subscription = currentSub,
            isInWatchlist = watchlistIds.contains(m.id),
            onToggleWatchlist = {
                if (watchlistIds.contains(m.id)) watchlistIds.remove(m.id) else watchlistIds.add(m.id)
                Toast.makeText(context, if (watchlistIds.contains(m.id)) "تمت الإضافة للمفضلة ✓" else "تمت الإزالة", Toast.LENGTH_SHORT).show()
            },
            onPlayMovie = { item ->
                selectedMovieForDetails = null
                isSportsPlayback = false
                activeStream = item
            },
            onBack = { selectedMovieForDetails = null }
        )
        return
    }

    if (selectedSeriesForDetails != null) {
        val s = selectedSeriesForDetails!!
        SeriesDetailsScreen(
            series = s,
            subscription = currentSub,
            isInWatchlist = watchlistIds.contains(s.id),
            onToggleWatchlist = {
                if (watchlistIds.contains(s.id)) watchlistIds.remove(s.id) else watchlistIds.add(s.id)
                Toast.makeText(context, if (watchlistIds.contains(s.id)) "تمت الإضافة للمفضلة ✓" else "تمت الإزالة", Toast.LENGTH_SHORT).show()
            },
            onPlayEpisode = { epItem ->
                isSportsPlayback = false
                activeStream = epItem
            },
            onBack = { selectedSeriesForDetails = null }
        )
        return
    }

    if (showEpgSheet) {
        val sampleEpg = cachedLiveChannels.take(120).mapIndexed { idx, ch ->
            EpgSlot(
                channelName = ch.title,
                channelLogo = ch.logo,
                currentProgram = if (idx % 2 == 0) "البث المباشر الحالي (${ch.group})" else "التغطية المباشرة المستمرة",
                nextProgram = "البرنامج التالي على الجدول",
                timeRange = "LIVE • HD",
                progress = 0.6f
            )
        }
        EpgTimelineGridSheet(
            slots = sampleEpg,
            lang = lang,
            onPlayChannel = { chName ->
                val found = cachedLiveChannels.firstOrNull { it.title == chName }
                showEpgSheet = false
                if (found != null) {
                    isSportsPlayback = SportsMatchExtractor.isSportsProgram(found.group, found.title, null)
                    activeStream = found
                }
            },
            onClose = { showEpgSheet = false }
        )
        return
    }

    Scaffold(
        containerColor = Color(0xFF07070A),
        topBar = {
            DevkanBrandHeader(
                activeSubLabel = currentSub?.label ?: "إضافة اتصال",
                onSwitchSubClick = { showQuickSubSwitcher = true },
                onSearchClick = { showQuickSearch = true },
                onImportClick = { m3uFileLauncher.launch("*/*") },
                onSettingsClick = { selectedTab = 5 },
                strings = strings
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0E0E14), tonalElevation = 8.dp) {
                val navItems = listOf(
                    Triple(strings.home, Icons.Default.Home, 0),
                    Triple(strings.live, Icons.Default.LiveTv, 1),
                    Triple(strings.sports, Icons.Default.SportsSoccer, 2),
                    Triple(strings.moviesTab, Icons.Default.Movie, 3),
                    Triple(strings.seriesTab, Icons.Default.VideoLibrary, 4)
                )
                navItems.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (selectedTab == index) gold else Color(0xFF8E929F),
                                modifier = Modifier.size(21.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) gold else Color(0xFF8E929F)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = gold.copy(alpha = 0.16f))
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isSyncing) {
                LinearProgressIndicator(color = gold, modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }

            when (selectedTab) {
                0 -> {
                    val lastCh = remember { DevkanVault.getLastChannel(context) }
                    var selectedHomePill by remember { mutableStateOf("الكل") }
                    val homePills = listOf("الكل", "مباشر", "رياضة", "أفلام", "مسلسلات", "EPG")

                    val latestMoviesSorted = remember(allItems.size) {
                        allItems.filter { it.contentType == "MOVIE" }
                            .sortedWith(
                                compareByDescending<MediaItemModel> { it.addedTimestamp }
                                    .thenByDescending { it.year.toIntOrNull() ?: 0 }
                            )
                            .take(15)
                    }

                    val true4kMovies = remember(allItems.size) {
                        allItems.filter { it.contentType == "MOVIE" && it.is4k }
                            .sortedByDescending { it.addedTimestamp }
                            .take(15)
                    }

                    val topSeriesSorted = remember(allItems.size) {
                        allItems.filter { it.contentType == "SERIES" }
                            .sortedWith(
                                compareByDescending<MediaItemModel> { it.rating.toDoubleOrNull() ?: 0.0 }
                                    .thenByDescending { it.addedTimestamp }
                            )
                            .take(15)
                    }

                    val heroFeatured = topSeriesSorted.firstOrNull { it.logo.isNotEmpty() }
                        ?: latestMoviesSorted.firstOrNull { it.logo.isNotEmpty() }

                    val featuredMatch = classifiedSports.liveMatches.firstOrNull()
                        ?: classifiedSports.upcoming24hMatches.firstOrNull()
                    val fallbackRealMatch = todayRealMatches.firstOrNull()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(homePills, key = { it }) { pill ->
                                val isSelected = pill == selectedHomePill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSelected) gold else Color(0xFF14141F))
                                        .border(1.dp, if (isSelected) gold else Color(0xFF28283A), RoundedCornerShape(50))
                                        .clickable {
                                            selectedHomePill = pill
                                            when (pill) {
                                                "مباشر" -> selectedTab = 1
                                                "رياضة" -> {
                                                    selectedSportNetwork = "ALL"
                                                    selectedTab = 2
                                                }
                                                "أفلام" -> selectedTab = 3
                                                "مسلسلات" -> selectedTab = 4
                                                "EPG" -> showEpgSheet = true
                                            }
                                        }
                                        .padding(horizontal = 15.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = pill,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        if (featuredMatch != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF18192C), Color(0xFF111625), Color(0xFF0A0A0F))
                                        )
                                    )
                                    .border(1.dp, gold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🏆 ${featuredMatch.competition ?: "قمة مباريات اليوم"}",
                                            color = goldLight,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val timeTxt = remember(featuredMatch.startTime) {
                                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(featuredMatch.startTime))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (featuredMatch.isLive) Color(0xFFEF4444).copy(alpha = 0.22f)
                                                    else Color(0xFF16A34A).copy(alpha = 0.22f)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (featuredMatch.isLive) "🔴 مباشر الآن" else "🟢 $timeTxt",
                                                color = if (featuredMatch.isLive) Color(0xFFF87171) else Color(0xFF4ADE80),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(featuredMatch.homeTeam, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("VS", color = gold, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 10.dp))
                                        Text(featuredMatch.awayTeam, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { launchGroupedMatchStream(featuredMatch) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("مشاهدة البث (${featuredMatch.streams.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                if (featuredMatch.streams.size > 1) selectedGroupedMatchForQuality = featuredMatch else launchGroupedMatchStream(featuredMatch)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, gold.copy(alpha = 0.5f))
                                        ) {
                                            Text("الجودات", color = goldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else if (fallbackRealMatch != null) {
                            val matchTitle = "${fallbackRealMatch.homeTeam} vs ${fallbackRealMatch.awayTeam}"
                            val isReminded = MatchReminderManager.remindedMatches.contains(matchTitle)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF18192C), Color(0xFF111625), Color(0xFF0A0A0F))
                                        )
                                    )
                                    .border(1.dp, if (fallbackRealMatch.isLive) Color(0xFFEF4444).copy(alpha = 0.65f) else gold.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🏆 ${fallbackRealMatch.league}",
                                            color = goldLight,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (fallbackRealMatch.isLive) Color(0xFFEF4444).copy(alpha = 0.24f) else Color(0xFF16A34A).copy(alpha = 0.22f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (fallbackRealMatch.isLive) "🔴 ${fallbackRealMatch.status}" else "⏰ ${fallbackRealMatch.matchTime}",
                                                color = if (fallbackRealMatch.isLive) Color(0xFFF87171) else Color(0xFF4ADE80),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (fallbackRealMatch.homeLogo.isNotEmpty()) {
                                                AsyncImage(model = fallbackRealMatch.homeLogo, contentDescription = null, modifier = Modifier.size(28.dp))
                                            }
                                            Text(fallbackRealMatch.homeTeam, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF222232), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(fallbackRealMatch.scoreText, color = goldLight, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                        }
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                                            Text(fallbackRealMatch.awayTeam, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (fallbackRealMatch.awayLogo.isNotEmpty()) {
                                                Spacer(Modifier.width(6.dp))
                                                AsyncImage(model = fallbackRealMatch.awayLogo, contentDescription = null, modifier = Modifier.size(28.dp))
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Button(
                                            onClick = { selectedMatchForChannelPicker = fallbackRealMatch },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("شاهد عبر ${fallbackRealMatch.suggestedBroadcaster}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                        }
                                        IconButton(
                                            onClick = {
                                                val added = MatchReminderManager.toggleReminder(matchTitle)
                                                Toast.makeText(context, if (added) "تم تفعيل إشعار المباراة 🔔" else "تم إلغاء الإشعار", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.background(Color(0xFF1E1E2D), RoundedCornerShape(12.dp))
                                        ) {
                                            Icon(
                                                imageVector = if (isReminded) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                                contentDescription = "Reminder",
                                                tint = if (isReminded) goldLight else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (heroFeatured != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF14141F))
                                    .clickable {
                                        if (heroFeatured.contentType == "SERIES") selectedSeriesForDetails = heroFeatured
                                        else selectedMovieForDetails = heroFeatured
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(heroFeatured.logo).size(640, 420).crossfade(true).build(),
                                    contentDescription = heroFeatured.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))))
                                )
                                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(heroFeatured.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Button(
                                        onClick = {
                                            if (heroFeatured.contentType == "SERIES") selectedSeriesForDetails = heroFeatured
                                            else selectedMovieForDetails = heroFeatured
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("شاهد الآن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        val liveCount = classifiedSports.liveMatches.size + todayRealMatches.count { it.isLive }
                        val totalCount = classifiedSports.liveMatches.size + classifiedSports.upcoming24hMatches.size + todayRealMatches.size
                        if (totalCount > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF111916))
                                    .border(1.dp, Color(0xFF16A34A).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedSportNetwork = "ALL"
                                        selectedTab = 2
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(24.dp))
                                    Column {
                                        Text("رياضة • جدول المباريات الحية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("🔴 $liveCount مباشر   •   ⚽ $totalCount مباراة اليوم", color = Color(0xFF4ADE80), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF4ADE80))
                            }
                        }

                        if (todayRealMatches.size > 1) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⚽ مباريات اليوم المباشرة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("عرض الكل", color = goldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { selectedTab = 2 })
                                }
                                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    itemsIndexed(todayRealMatches.drop(1).take(12), key = { idx, it -> "home_espn_${idx}_${it.id}" }) { _, match ->
                                        Column(
                                            modifier = Modifier
                                                .width(245.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF151522))
                                                .border(1.dp, if (match.isLive) Color(0xFFEF4444).copy(alpha = 0.5f) else gold.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                                .clickable { selectedMatchForChannelPicker = match }
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(match.league, color = goldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                                                Text(if (match.isLive) "🔴 ${match.matchTime}" else match.matchTime, color = if (match.isLive) Color(0xFFF87171) else Color(0xFF4ADE80), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text("${match.homeTeam}  (${match.scoreText})  ${match.awayTeam}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("📺 ${match.suggestedBroadcaster}", color = Color(0xFF93C5FD), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        if (lastCh != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF14141D))
                                    .border(1.dp, gold.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        isSportsPlayback = false
                                        activeStream = MediaItemModel("last", "0", lastCh.second, lastCh.first, "", "Live", "", "LIVE")
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(strings.continueWatching, color = gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(lastCh.second, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = gold, modifier = Modifier.size(30.dp))
                            }
                        }

                        if (allItems.isEmpty() && !isSyncing) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF12121A))
                                    .border(1.dp, gold.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.AddLink, contentDescription = null, tint = gold, modifier = Modifier.size(44.dp))
                                Text("أضف اشتراكك للبدء", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Button(onClick = {
                                    subToEdit = null
                                    showAddDialog = true
                                }, colors = ButtonDefaults.buttonColors(containerColor = gold)) {
                                    Text(strings.addConnection, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            if (latestMoviesSorted.isNotEmpty()) {
                                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                    CinemaHorizontalRow(
                                        title = strings.latestMovies,
                                        items = latestMoviesSorted,
                                        onMoreClick = { selectedTab = 3 },
                                        onItemClick = { selectedMovieForDetails = it }
                                    )
                                }
                            }

                            if (topSeriesSorted.isNotEmpty()) {
                                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                    CinemaHorizontalRow(
                                        title = strings.featuredSeries,
                                        items = topSeriesSorted,
                                        onMoreClick = { selectedTab = 4 },
                                        onItemClick = { selectedSeriesForDetails = it }
                                    )
                                }
                            }

                            if (true4kMovies.isNotEmpty()) {
                                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                    CinemaHorizontalRow(
                                        title = strings.movies4k,
                                        items = true4kMovies,
                                        onMoreClick = {
                                            movieSmartFilter = "4K"
                                            selectedTab = 3
                                        },
                                        onItemClick = { selectedMovieForDetails = it }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${strings.live} (${filteredLiveList.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Button(
                                onClick = { showEpgSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = gold.copy(alpha = 0.2f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CalendarViewWeek, contentDescription = null, tint = gold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.epgGuide, color = gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(
                            value = liveFilterQuery,
                            onValueChange = { liveFilterQuery = it },
                            placeholder = { Text("تصفية القنوات بالاسم...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gold, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (liveFilterQuery.isNotEmpty()) {
                                    IconButton(onClick = { liveFilterQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(cachedLiveGroups, key = { it }) { grp ->
                                FilterChip(
                                    selected = selectedLiveCategory == grp,
                                    onClick = { selectedLiveCategory = grp },
                                    label = { Text(if (grp == "ALL") strings.all else grp, fontSize = 12.sp) }
                                )
                            }
                        }

                        LazyColumn(state = liveListState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(items = filteredLiveList, key = { it.id }, contentType = { "live_channel" }) { ch ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF13131B))
                                        .clickable {
                                            isSportsPlayback = SportsMatchExtractor.isSportsProgram(ch.group, ch.title, null)
                                            activeStream = ch
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (ch.logo.isNotEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context).data(ch.logo).size(120, 120).build(),
                                                contentDescription = null,
                                                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                                            )
                                        } else {
                                            Icon(Icons.Default.Tv, contentDescription = null, tint = gold, modifier = Modifier.size(32.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ch.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(ch.group, color = gold.copy(alpha = 0.85f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = gold)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    val networks = listOf("ALL" to "الكل", "BEIN" to "beIN SPORTS", "SSC" to "SSC", "ALKASS" to "الكأس", "AD" to "أبو ظبي", "RABIAA" to "الرابعة")

                    val deduplicatedSports = remember(cachedSportsChannels, selectedSportNetwork) {
                        if (selectedSportNetwork == "ALL") {
                            cachedSportsChannels
                        } else {
                            cachedSportsChannels.filter { item ->
                                val text = "${item.group} ${item.title}".lowercase()
                                when (selectedSportNetwork) {
                                    "BEIN" -> text.contains("bein")
                                    "SSC" -> text.contains("ssc")
                                    "ALKASS" -> text.contains("kass") || text.contains("كأس") || text.contains("الكاس")
                                    "AD" -> text.contains("abu dhabi") || text.contains("ad sport") || text.contains("ابوظبي") || text.contains("أبوظبي")
                                    "RABIAA" -> text.contains("rabiaa") || text.contains("الرابعة") || text.contains("iraq")
                                    else -> true
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(networks, key = { it.first }) { (code, label) ->
                                FilterChip(
                                    selected = selectedSportNetwork == code,
                                    onClick = { selectedSportNetwork = code },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        LazyColumn(state = sportsListState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (todayRealMatches.isNotEmpty() && selectedSportNetwork == "ALL") {
                                item(key = "header_today_matches") {
                                    Text("🏆 مباريات اليوم والنتائج الحية (${todayRealMatches.size})", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                itemsIndexed(todayRealMatches, key = { idx, it -> "real_${idx}_${it.id}" }) { _, match ->
                                    val title = "${match.homeTeam} vs ${match.awayTeam}"
                                    val isReminded = MatchReminderManager.remindedMatches.contains(title)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF151522))
                                            .border(1.dp, if (match.isLive) Color(0xFFEF4444).copy(alpha = 0.55f) else Color(0xFF28283E), RoundedCornerShape(14.dp))
                                            .clickable { selectedMatchForChannelPicker = match }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = if (match.isLive) "🔴 ${match.matchTime}" else "⏰ ${match.matchTime}",
                                                    color = if (match.isLive) Color(0xFFF87171) else Color(0xFF4ADE80),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text("• ${match.league}", color = Color(0xFF9CA3AF), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Text("${match.homeTeam}   (${match.scoreText})   ${match.awayTeam}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                            Text("📺 القناة الناقلة: ${match.suggestedBroadcaster}", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(onClick = {
                                            val added = MatchReminderManager.toggleReminder(title)
                                            Toast.makeText(context, if (added) "تم تفعيل إشعار المباراة 🔔" else "تم إلغاء التذكير", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(
                                                imageVector = if (isReminded) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                                contentDescription = "Reminder",
                                                tint = if (isReminded) gold else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }

                            item(key = "header_sports_channels") {
                                Text("📺 القنوات الرياضية الموحدة (${deduplicatedSports.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            itemsIndexed(deduplicatedSports, key = { idx, it -> "sp_${idx}_${it.id}" }) { _, ch ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF13131B))
                                        .clickable {
                                            isSportsPlayback = true
                                            activeStream = ch
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (ch.logo.isNotEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context).data(ch.logo).size(120, 120).build(),
                                                contentDescription = null,
                                                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                                            )
                                        } else {
                                            Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = gold)
                                        }
                                        Column {
                                            Text(ch.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                            Text(ch.group, color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = gold)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    val allMovies = remember(allItems.size) { allItems.filter { it.contentType == "MOVIE" } }
                    val movieGroups = remember(allMovies.size) { listOf("ALL") + allMovies.map { it.group }.distinct() }

                    val availableMovieFilters = remember(allMovies.size) {
                        val list = mutableListOf("ALL" to "الكل", "RECENT" to "مضاف حديثاً", "TOP" to "أعلى تقييماً ⭐")
                        if (allMovies.any { it.is4k }) list.add("4K" to "4K UHD")
                        listOf("دراما", "أكشن", "خيال علمي", "رومانسي", "كوميدي", "رعب وإثارة", "تاريخي", "أنمي وعائلي").forEach { g ->
                            if (allMovies.any { it.genreTag == g }) list.add(g to g)
                        }
                        listOf("عربي", "كوري", "تركي", "هندي", "أجنبي").forEach { o ->
                            if (allMovies.any { it.originTag == o }) list.add(o to "أفلام $o")
                        }
                        list
                    }

                    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎬 ${strings.moviesTab} (${filteredMoviesList.size})", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                            OutlinedButton(
                                onClick = { showMovieGroupsSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, gold.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = goldLight, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (movieServerGroup == "ALL") "أقسام السيرفر" else movieServerGroup,
                                    color = goldLight,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 120.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = movieQuery,
                            onValueChange = { movieQuery = it },
                            placeholder = { Text("ابحث في مكتبة الأفلام...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gold, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (movieQuery.isNotEmpty()) {
                                    IconButton(onClick = { movieQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableMovieFilters, key = { it.first }) { (code, label) ->
                                FilterChip(
                                    selected = movieSmartFilter == code,
                                    onClick = { movieSmartFilter = code },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = if (movieSmartFilter == code) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        LazyVerticalGrid(
                            state = moviesGridState,
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items = filteredMoviesList, key = { it.id }, contentType = { "movie_poster" }) { item ->
                                val progress = vodProgressCache[item.id] ?: 0f
                                MediaPosterCard(
                                    item = item,
                                    progress = progress,
                                    onClick = { selectedMovieForDetails = item }
                                )
                            }
                        }
                    }

                    if (showMovieGroupsSheet) {
                        ModalBottomSheet(onDismissRequest = { showMovieGroupsSheet = false }, containerColor = Color(0xFF14141E)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("مجلدات الأفلام في السيرفر", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                LazyColumn(modifier = Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(movieGroups, key = { it }) { grp ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (movieServerGroup == grp) gold.copy(alpha = 0.2f) else Color(0xFF1C1C28))
                                                .clickable {
                                                    movieServerGroup = grp
                                                    showMovieGroupsSheet = false
                                                }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(if (grp == "ALL") "كل مجلدات الأفلام (${allMovies.size})" else grp, color = Color.White, fontSize = 13.sp)
                                            if (movieServerGroup == grp) Icon(Icons.Default.Check, contentDescription = null, tint = goldLight)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    val allSeries = remember(allItems.size) { allItems.filter { it.contentType == "SERIES" } }
                    val seriesGroups = remember(allSeries.size) { listOf("ALL") + allSeries.map { it.group }.distinct() }

                    val availableSeriesFilters = remember(allSeries.size) {
                        val list = mutableListOf("ALL" to "الكل", "RECENT" to "مضاف حديثاً", "TOP" to "أعلى تقييماً ⭐")
                        listOf("عربي", "كوري", "تركي", "هندي", "أجنبي").forEach { o ->
                            if (allSeries.any { it.originTag == o }) list.add(o to "مسلسلات $o")
                        }
                        listOf("دراما", "أكشن", "خيال علمي", "رومانسي", "تاريخي", "كوميدي", "رعب وإثارة").forEach { g ->
                            if (allSeries.any { it.genreTag == g }) list.add(g to g)
                        }
                        list
                    }

                    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📺 ${strings.seriesTab} (${filteredSeriesList.size})", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                            OutlinedButton(
                                onClick = { showSeriesGroupsSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, gold.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = goldLight, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (seriesServerGroup == "ALL") "أقسام السيرفر" else seriesServerGroup,
                                    color = goldLight,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 120.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = seriesQuery,
                            onValueChange = { seriesQuery = it },
                            placeholder = { Text("ابحث في مكتبة المسلسلات...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gold, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (seriesQuery.isNotEmpty()) {
                                    IconButton(onClick = { seriesQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableSeriesFilters, key = { it.first }) { (code, label) ->
                                FilterChip(
                                    selected = seriesSmartFilter == code,
                                    onClick = { seriesSmartFilter = code },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = if (seriesSmartFilter == code) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        LazyVerticalGrid(
                            state = seriesGridState,
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items = filteredSeriesList, key = { it.id }, contentType = { "series_poster" }) { item ->
                                MediaPosterCard(
                                    item = item,
                                    progress = 0f,
                                    onClick = { selectedSeriesForDetails = item }
                                )
                            }
                        }
                    }

                    if (showSeriesGroupsSheet) {
                        ModalBottomSheet(onDismissRequest = { showSeriesGroupsSheet = false }, containerColor = Color(0xFF14141E)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("مجلدات المسلسلات في السيرفر", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                LazyColumn(modifier = Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(seriesGroups, key = { it }) { grp ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (seriesServerGroup == grp) gold.copy(alpha = 0.2f) else Color(0xFF1C1C28))
                                                .clickable {
                                                    seriesServerGroup = grp
                                                    showSeriesGroupsSheet = false
                                                }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(if (grp == "ALL") "كل مجلدات المسلسلات (${allSeries.size})" else grp, color = Color.White, fontSize = 13.sp)
                                            if (seriesServerGroup == grp) Icon(Icons.Default.Check, contentDescription = null, tint = goldLight)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                5 -> {
                    DevkanSettingsScreen(
                        strings = strings,
                        lang = lang,
                        onLanguageChange = onLanguageChange,
                        subscription = currentSub,
                        onSwitchSub = { selectedSub -> switchSubscriptionFast(selectedSub) },
                        onEditSub = { sub ->
                            subToEdit = sub
                            showAddDialog = true
                        },
                        onRefreshSub = {
                            val sub = currentSub ?: return@DevkanSettingsScreen
                            scope.launch {
                                isSyncing = true
                                val (updated, items) = DevkanPlaylistRepository.loadSubscriptionContent(sub, DevkanVault.isGeoBypassEnabled(context))
                                currentSub = updated
                                DevkanVault.saveSubscription(context, updated)
                                if (items.isNotEmpty()) {
                                    allItems.clear()
                                    allItems.addAll(items)
                                    withContext(Dispatchers.IO) { DevkanVault.saveCachedItems(context, items, updated.id) }
                                    classifiedSports = DevkanPlaylistRepository.syncAndClassifySportsEpg(context, updated, items)
                                }
                                isSyncing = false
                                Toast.makeText(context, "تم تحديث البيانات والجدول الرياضي بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteSub = {
                            DevkanVault.clearSubscription(context)
                            val nextSub = DevkanVault.getSavedSubscription(context)
                            currentSub = nextSub
                            if (nextSub == null) {
                                allItems.clear()
                                cachedLiveChannels = emptyList()
                                cachedSportsChannels = emptyList()
                                classifiedSports = emptySports
                            } else {
                                switchSubscriptionFast(nextSub)
                            }
                            Toast.makeText(context, "تم حذف الاشتراك", Toast.LENGTH_SHORT).show()
                        },
                        onOpenAddDialog = {
                            subToEdit = null
                            showAddDialog = true
                        },
                        sampleTestUrl = allItems.firstOrNull()?.url ?: "https://www.google.com"
                    )
                }
            }
        }
    }

    if (showQuickSubSwitcher) {
        val subsList = remember(currentSub?.id, currentSub?.label) { DevkanVault.getAllSubscriptions(context) }
        ModalBottomSheet(
            onDismissRequest = { showQuickSubSwitcher = false },
            containerColor = Color(0xFF14141E)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("تبديل الاتصال", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                if (subsList.isEmpty()) {
                    Text("لا توجد اشتراكات محفوظة حالياً", color = Color.Gray, fontSize = 13.sp)
                } else {
                    subsList.forEach { subItem ->
                        val isSelected = subItem.id == currentSub?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) gold.copy(alpha = 0.18f) else Color(0xFF1B1B27))
                                .clickable {
                                    showQuickSubSwitcher = false
                                    if (!isSelected) switchSubscriptionFast(subItem)
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RadioButton(selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = goldLight))
                                Column {
                                    Text(subItem.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${subItem.type} • ${subItem.expDateFormatted}", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = {
                                showQuickSubSwitcher = false
                                subToEdit = subItem
                                showAddDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(gold.copy(alpha = 0.15f))
                        .clickable {
                            showQuickSubSwitcher = false
                            subToEdit = null
                            showAddDialog = true
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = goldLight)
                    Spacer(Modifier.width(8.dp))
                    Text("إضافة اتصال جديد", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showQuickSearch) {
        val searchResults = remember(quickSearchQuery, allItems.size) {
            if (quickSearchQuery.trim().length < 2) emptyList()
            else allItems.filter { it.title.contains(quickSearchQuery.trim(), ignoreCase = true) }.take(25)
        }
        AlertDialog(
            onDismissRequest = {
                showQuickSearch = false
                quickSearchQuery = ""
            },
            containerColor = Color(0xFF14141D),
            title = {
                OutlinedTextField(
                    value = quickSearchQuery,
                    onValueChange = { quickSearchQuery = it },
                    placeholder = { Text(strings.searchHint, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                if (searchResults.isEmpty()) {
                    Text("اكتب حرفين على الأقل للبحث في القنوات والأفلام والمسلسلات", color = Color.Gray, fontSize = 12.sp)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(searchResults, key = { it.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E1E2B))
                                    .clickable {
                                        showQuickSearch = false
                                        quickSearchQuery = ""
                                        when (item.contentType) {
                                            "MOVIE" -> selectedMovieForDetails = item
                                            "SERIES" -> selectedSeriesForDetails = item
                                            else -> {
                                                isSportsPlayback = SportsMatchExtractor.isSportsProgram(item.group, item.title, null)
                                                activeStream = item
                                            }
                                        }
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${item.contentType} • ${item.group}", color = goldLight, fontSize = 11.sp, maxLines = 1)
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = gold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showQuickSearch = false
                    quickSearchQuery = ""
                }) {
                    Text("إغلاق", color = gold)
                }
            }
        )
    }

    selectedGroupedMatchForQuality?.let { match ->
        AlertDialog(
            onDismissRequest = { selectedGroupedMatchForQuality = null },
            containerColor = Color(0xFF14141D),
            title = {
                Column {
                    Text("${match.homeTeam} vs ${match.awayTeam}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("اختر القناة أو الجودة المناسبة:", color = goldLight, fontSize = 12.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    match.streams.forEach { prog ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1E2B))
                                .clickable {
                                    selectedGroupedMatchForQuality = null
                                    launchGroupedMatchStream(match, prog)
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(prog.channelName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier.background(gold.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(prog.streamQuality ?: "HD", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedGroupedMatchForQuality = null }) {
                    Text("إغلاق", color = gold)
                }
            }
        )
    }

    selectedMatchForChannelPicker?.let { match ->
        val matchingChannels = remember(cachedSportsChannels, match.id) {
            val broadcasterKeyword = match.suggestedBroadcaster.substringBefore(" ").lowercase()
            val priority = cachedSportsChannels.filter { it.title.lowercase().contains(broadcasterKeyword) || it.group.lowercase().contains(broadcasterKeyword) }
            if (priority.isNotEmpty()) priority.take(8) else cachedSportsChannels.take(8)
        }

        AlertDialog(
            onDismissRequest = { selectedMatchForChannelPicker = null },
            containerColor = Color(0xFF14141D),
            title = {
                Column {
                    Text("${match.homeTeam} vs ${match.awayTeam}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("اختر القناة الناقلة من اشتراكك:", color = goldLight, fontSize = 12.sp)
                }
            },
            text = {
                if (matchingChannels.isEmpty()) {
                    Text("لم يتم العثور على قنوات رياضية في اشتراكك الحالي.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        matchingChannels.forEach { ch ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E1E2B))
                                    .clickable {
                                        selectedMatchForChannelPicker = null
                                        isSportsPlayback = true
                                        activeStream = ch.copy(title = "${match.homeTeam} vs ${match.awayTeam} • ${ch.title}")
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(ch.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = gold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMatchForChannelPicker = null }) {
                    Text("إغلاق", color = gold)
                }
            }
        )
    }

    if (showAddDialog) {
        SmartAddConnectionDialog(
            strings = strings,
            editingSub = subToEdit,
            onDismiss = {
                showAddDialog = false
                subToEdit = null
            },
            onSaveAndConnect = { newSub ->
                scope.launch {
                    isSyncing = true
                    DevkanVault.saveSubscription(context, newSub)
                    val (updatedSub, loadedItems) = DevkanPlaylistRepository.loadSubscriptionContent(
                        newSub,
                        DevkanVault.isGeoBypassEnabled(context)
                    )
                    currentSub = updatedSub
                    DevkanVault.saveSubscription(context, updatedSub)
                    if (loadedItems.isNotEmpty()) {
                        allItems.clear()
                        allItems.addAll(loadedItems)
                        withContext(Dispatchers.IO) { DevkanVault.saveCachedItems(context, loadedItems, updatedSub.id) }
                        classifiedSports = DevkanPlaylistRepository.syncAndClassifySportsEpg(context, updatedSub, loadedItems)
                        Toast.makeText(context, "تم الحفظ والاتصال (${loadedItems.size} عنصر)!", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                        subToEdit = null
                    } else {
                        Toast.makeText(context, "تعذر جلب القنوات، تأكد من البيانات", Toast.LENGTH_LONG).show()
                    }
                    isSyncing = false
                }
            }
        )
    }
}
