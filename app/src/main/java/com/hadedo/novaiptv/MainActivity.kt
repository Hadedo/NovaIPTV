package com.hadedo.novaiptv

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    val cinema: String,
    val settings: String,
    val continueWatching: String,
    val latestMovies: String,
    val featuredSeries: String,
    val movies4k: String,
    val searchHint: String,
    val all: String,
    val moviesTab: String,
    val seriesTab: String,
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
    "en" -> AppStrings("Home", "Live TV", "Sports", "Cinema", "Settings", "Continue Watching", "Latest Releases", "Featured Series", "4K UHD Movies", "Search channels, movies, series...", "All", "Movies", "Series", "Add Connection", "Import M3U File", "EPG Grid", "Server Speed & Ping", "Playback Engine", "Auto-Refresh Catalog", "App Lock PIN", "Subscription Details", "Status", "Expires", "Allowed Region", "Connections", "Ultra-Low Latency (Satellite Sync)", "Smart Geo-Bypass Headers", "Auto-Resume Last Channel")
    "de" -> AppStrings("Start", "Live-TV", "Sport", "Kino", "Optionen", "Weiterschauen", "Neueste Filme", "Beliebte Serien", "4K-Filme", "Kanäle, Filme, Serien suchen...", "Alle", "Filme", "Serien", "Verbindung hinzufügen", "M3U-Datei laden", "EPG-Plan", "Server-Speedtest", "Wiedergabe-Engine", "Auto-Aktualisierung", "App-PIN-Sperre", "Abonnement-Details", "Status", "Ablaufdatum", "Region / Land", "Verbindungen", "Geringe Latenz (Satelliten-Sync)", "Geo-Bypass-Header", "Letzten Kanal fortsetzen")
    "tr" -> AppStrings("Ana Sayfa", "Canlı", "Spor", "Sinema", "Ayarlar", "Devam Et", "Yeni Filmler", "Öne Çıkan Diziler", "4K Filmler", "Kanal, film, dizi ara...", "Tümü", "Filmler", "Diziler", "Bağlantı Ekle", "M3U Dosyası", "EPG Rehberi", "Sunucu Hız Testi", "Oynatıcı Motoru", "Otomatik Yenileme", "Uygulama PIN", "Abonelik Bilgileri", "Durum", "Bitiş Tarihi", "Ülke / Bölge", "Bağlantılar", "Düşük Gecikme Modu", "Bölge Kısıtlama Aşımı", "Son Kanalı Otomatik Aç")
    "fr" -> AppStrings("Accueil", "Direct", "Sports", "Cinéma", "Réglages", "Reprendre", "Derniers Films", "Séries en Vedette", "Films 4K", "Rechercher...", "Tout", "Films", "Séries", "Ajouter Connexion", "Fichier M3U", "Guide EPG", "Test Serveur", "Moteur Vidéo", "Actualisation Auto", "Code PIN App", "Abonnement", "Statut", "Expiration", "Pays Autorisé", "Connexions", "Faible Latence", "Contournement Géo", "Reprise Auto")
    "ku" -> AppStrings("سەرەکی", "ڕاستەوخۆ", "وەرزش", "سینەما", "ڕێکخستن", "بەردەوامبە", "نوێترین فیلمەکان", "زنجیرە دیارەکان", "فیلمی 4K", "گەڕان...", "هەموو", "فیلم", "زنجیرە", "زیادکردنی بەشداری", "فایلی M3U", "خشتەی EPG", "پشکنینی خێرایی", "بزوێنەری کارپێکردن", "نوێکردنەوەی خۆکار", "قفڵی بەرنامە", "زانیاری بەشداری", "دۆخ", "بەرواری بەسەرچوون", "وڵات", "پەیوەندییەکان", "کەمکردنەوەی دواکەوتن", "کردنەوەی بلۆکی وڵات", "کردنەوەی دوایین کەناڵ")
    else -> AppStrings("الرئيسية", "مباشر", "الرياضة", "السينما", "الإعدادات", "واصل المشاهدة", "أحدث الإصدارات", "المسلسلات المميزة", "أفلام 4K الحقيقية", "ابحث عن قناة أو فيلم أو مسلسل...", "الكل", "الأفلام", "المسلسلات", "إضافة اتصال", "استيراد ملف M3U", "جدول EPG", "فحص سرعة السيرفر", "محرك التشغيل", "تحديث الواجهة والمكتبة تلقائياً", "قفل التطبيق PIN", "بطاقة الاشتراك", "الحالة", "تاريخ الانتهاء", "الدولة / النطاق", "الاتصالات", "تقليل تأخير البث عن الستلايت", "تجاوز قيود السيرفر والدولة", "فتح آخر قناة تلقائياً عند التشغيل")
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
fun DevkanBrandHeader(onAddClick: () -> Unit, onImportClick: () -> Unit, strings: AppStrings) {
    val goldLight = Color(0xFFFDE047)
    val goldMid = Color(0xFFD4AF37)
    val goldDark = Color(0xFFB45309)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0F))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(goldLight, goldMid, goldDark)))
                    .border(1.dp, goldLight.copy(alpha = 0.8f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = "DEVKAN 34 VIP",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("DEVKAN 34", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
                Box(
                    modifier = Modifier
                        .background(goldMid.copy(alpha = 0.22f), RoundedCornerShape(5.dp))
                        .border(1.dp, goldMid.copy(alpha = 0.85f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text("VIP", color = goldLight, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(
                onClick = onImportClick,
                modifier = Modifier.size(34.dp).background(Color(0xFF161620), CircleShape)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = strings.importFile, tint = goldLight, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.size(34.dp).background(goldMid, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.addConnection, tint = Color.Black, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun DevkanMainDashboard(lang: String, onLanguageChange: (String) -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val strings = remember(lang) { getStrings(lang) }
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    var selectedTab by remember { mutableIntStateOf(0) }
    var activeStream by remember { mutableStateOf<MediaItemModel?>(null) }
    var isSportsPlayback by remember { mutableStateOf(false) }
    var selectedMovieForDetails by remember { mutableStateOf<MediaItemModel?>(null) }
    var selectedSeriesForDetails by remember { mutableStateOf<MediaItemModel?>(null) }
    var selectedMatchForChannelPicker by remember { mutableStateOf<RealSportsMatch?>(null) }
    var selectedGroupedMatchForQuality by remember { mutableStateOf<GroupedSportsMatch?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEpgSheet by remember { mutableStateOf(false) }
    var selectedLiveCategory by remember { mutableStateOf("ALL") }
    var isSyncing by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    var cinemaSubTab by remember { mutableStateOf("MOVIE") }
    var selectedOrigin by remember { mutableStateOf("ALL") }
    val moviesGridState = rememberLazyGridState()
    val seriesGridState = rememberLazyGridState()
    val liveListState = rememberLazyListState()
    val sportsListState = rememberLazyListState()

    var currentSub by remember { mutableStateOf(DevkanVault.getSavedSubscription(context)) }
    val allItems = remember { mutableStateListOf<MediaItemModel>() }
    val todayRealMatches = remember { mutableStateListOf<RealSportsMatch>() }
    val watchlistIds = remember { mutableStateListOf<String>() }

    // قوائم جاهزة مسبقاً في الخلفية لمنع أي تجميد للواجهة
    var cachedSportsChannels by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var classifiedSports by remember {
        mutableStateOf(
            ClassifiedSportsData(
                liveMatches = emptyList(),
                upcoming24hMatches = emptyList(),
                upcomingLaterMatches = emptyList(),
                livePrograms = emptyList()
            )
        )
    }

    // معالجة وتصفية القنوات الرياضية في الخلفية (Dispatchers.Default) بعيداً عن Main Thread
    LaunchedEffect(allItems.size) {
        if (allItems.isEmpty()) {
            cachedSportsChannels = emptyList()
        } else {
            val snapshot = allItems.toList()
            cachedSportsChannels = withContext(Dispatchers.Default) {
                val raw = snapshot.filter { item ->
                    item.contentType == "LIVE" &&
                        SportsMatchExtractor.isSportsProgram(item.group, item.title, null)
                }
                DevkanPlaylistRepository.deduplicateSportChannels(raw)
            }
        }
    }

    BackHandler(
        enabled = activeStream == null &&
            selectedMovieForDetails == null &&
            selectedSeriesForDetails == null &&
            !showEpgSheet &&
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

    // الفتح الفوري في 0.1 ثانية + المزامنة الصامتة في الخلفية لـ Catalog و Sports EPG
    LaunchedEffect(Unit) {
        val cached = withContext(Dispatchers.IO) { DevkanVault.getCachedItems(context) }
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

        val saved = DevkanVault.getSavedSubscription(context)
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
                    withContext(Dispatchers.IO) { DevkanVault.saveCachedItems(context, loadedItems) }
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

    val liveChannelsForDrawer = remember(allItems.size) {
        allItems.filter { it.contentType == "LIVE" }
    }

    fun launchGroupedMatchStream(match: GroupedSportsMatch, targetProgram: EpgProgram = match.primaryStream) {
        val matchedChannel = allItems.firstOrNull {
            it.contentType == "LIVE" && (it.rawId == targetProgram.channelId || it.title.equals(targetProgram.channelName, ignoreCase = true))
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
            quickChannels = liveChannelsForDrawer,
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
                Toast.makeText(context, if (watchlistIds.contains(m.id)) "تمت الإضافة للمشاهدة لاحقاً ✓" else "تمت الإزالة", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, if (watchlistIds.contains(s.id)) "تمت الإضافة للمشاهدة لاحقاً ✓" else "تمت الإزالة", Toast.LENGTH_SHORT).show()
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
        val sampleEpg = allItems.filter { it.contentType == "LIVE" }.take(120).mapIndexed { idx, ch ->
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
                val found = allItems.firstOrNull { it.title == chName }
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
                onAddClick = { showAddDialog = true },
                onImportClick = { m3uFileLauncher.launch("*/*") },
                strings = strings
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0E0E14), tonalElevation = 8.dp) {
                val navItems = listOf(
                    Triple(strings.home, Icons.Default.Home, 0),
                    Triple(strings.live, Icons.Default.LiveTv, 1),
                    Triple(strings.sports, Icons.Default.SportsSoccer, 2),
                    Triple(strings.cinema, Icons.Default.Movie, 3),
                    Triple(strings.settings, Icons.Default.Settings, 4)
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
                                fontSize = 9.5.sp,
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
                // 0: الشاشة الرئيسية الهجينة
                0 -> {
                    val lastCh = remember { DevkanVault.getLastChannel(context) }

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

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val epgTopMatch = classifiedSports.liveMatches.firstOrNull()
                            ?: classifiedSports.upcoming24hMatches.firstOrNull()

                        if (epgTopMatch != null) {
                            SmartEpgMatchCard(
                                match = epgTopMatch,
                                onPlayPrimary = { launchGroupedMatchStream(epgTopMatch) },
                                onSelectQuality = {
                                    if (epgTopMatch.streams.size > 1) {
                                        selectedGroupedMatchForQuality = epgTopMatch
                                    } else {
                                        launchGroupedMatchStream(epgTopMatch)
                                    }
                                }
                            )
                        } else if (todayRealMatches.isNotEmpty()) {
                            val topMatch = todayRealMatches.first()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.horizontalGradient(listOf(Color(0xFF1A1429), Color(0xFF0F172A))))
                                    .border(1.dp, gold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                    .clickable { selectedMatchForChannelPicker = topMatch }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("⚽ ${topMatch.league} • الناقل: ${topMatch.suggestedBroadcaster}", color = goldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("${topMatch.homeTeam}  🆚  ${topMatch.awayTeam}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                }
                                Box(
                                    modifier = Modifier.background(Color(0xFF16A34A).copy(alpha = 0.25f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(topMatch.matchTime, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        if (heroFeatured != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF14141F))
                                    .clickable {
                                        if (heroFeatured.contentType == "SERIES") selectedSeriesForDetails = heroFeatured
                                        else selectedMovieForDetails = heroFeatured
                                    }
                            ) {
                                AsyncImage(
                                    model = heroFeatured.logo,
                                    contentDescription = heroFeatured.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.background(gold, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text("TOP PICKS • VIP", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                        }
                                        Text("${heroFeatured.originTag} • ${heroFeatured.genreTag}", color = goldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(heroFeatured.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Button(
                                        onClick = {
                                            if (heroFeatured.contentType == "SERIES") selectedSeriesForDetails = heroFeatured
                                            else selectedMovieForDetails = heroFeatured
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("شاهد الآن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        if (lastCh != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF12121A))
                                    .border(1.dp, gold.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.AddLink, contentDescription = null, tint = gold, modifier = Modifier.size(44.dp))
                                Text("أضف اشتراكك للبدء", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Button(onClick = { showAddDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = gold)) {
                                    Text(strings.addConnection, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            if (latestMoviesSorted.isNotEmpty()) {
                                CinemaHorizontalRow(
                                    title = strings.latestMovies,
                                    items = latestMoviesSorted,
                                    onMoreClick = {
                                        cinemaSubTab = "MOVIE"
                                        selectedTab = 3
                                    },
                                    onItemClick = { selectedMovieForDetails = it }
                                )
                            }

                            if (topSeriesSorted.isNotEmpty()) {
                                CinemaHorizontalRow(
                                    title = strings.featuredSeries,
                                    items = topSeriesSorted,
                                    onMoreClick = {
                                        cinemaSubTab = "SERIES"
                                        selectedTab = 3
                                    },
                                    onItemClick = { selectedSeriesForDetails = it }
                                )
                            }

                            if (true4kMovies.isNotEmpty()) {
                                CinemaHorizontalRow(
                                    title = strings.movies4k,
                                    items = true4kMovies,
                                    onMoreClick = {
                                        cinemaSubTab = "MOVIE"
                                        selectedTab = 3
                                    },
                                    onItemClick = { selectedMovieForDetails = it }
                                )
                            }
                        }
                    }
                }

                // 1: البث المباشر
                1 -> {
                    val liveChannels = remember(allItems.size) { allItems.filter { it.contentType == "LIVE" } }
                    val groups = remember(liveChannels.size) { listOf("ALL") + liveChannels.map { it.group }.distinct() }
                    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${strings.live} (${liveChannels.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
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

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(groups) { grp ->
                                FilterChip(
                                    selected = selectedLiveCategory == grp,
                                    onClick = { selectedLiveCategory = grp },
                                    label = { Text(if (grp == "ALL") strings.all else grp, fontSize = 12.sp) }
                                )
                            }
                        }

                        val filteredLive = remember(liveChannels.size, selectedLiveCategory) {
                            if (selectedLiveCategory == "ALL") liveChannels else liveChannels.filter { it.group == selectedLiveCategory }
                        }

                        LazyColumn(state = liveListState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredLive, key = { it.id }) { ch ->
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
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (ch.logo.isNotEmpty()) {
                                            AsyncImage(model = ch.logo, contentDescription = null, modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)))
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

                // 2: مركز الرياضة الذكي (فوري 0ms بفضل المعالجة المسبقة في الخلفية)
                2 -> {
                    var selectedSportNetwork by remember { mutableStateOf("ALL") }
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
                            if (classifiedSports.liveMatches.isNotEmpty() && selectedSportNetwork == "ALL") {
                                item(key = "header_live_epg") {
                                    Text("🔴 جارية الآن على قنواتك (${classifiedSports.liveMatches.size})", color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                itemsIndexed(classifiedSports.liveMatches, key = { idx, it -> "live_${idx}_${it.matchKey}" }) { _, match ->
                                    SmartEpgMatchCard(
                                        match = match,
                                        onPlayPrimary = { launchGroupedMatchStream(match) },
                                        onSelectQuality = {
                                            if (match.streams.size > 1) selectedGroupedMatchForQuality = match else launchGroupedMatchStream(match)
                                        }
                                    )
                                }
                            }

                            if (classifiedSports.upcoming24hMatches.isNotEmpty() && selectedSportNetwork == "ALL") {
                                item(key = "header_upcoming_epg") {
                                    Text("🟢 قادمة خلال 24 ساعة (${classifiedSports.upcoming24hMatches.size})", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                itemsIndexed(classifiedSports.upcoming24hMatches, key = { idx, it -> "up_${idx}_${it.matchKey}" }) { _, match ->
                                    SmartEpgMatchCard(
                                        match = match,
                                        onPlayPrimary = { launchGroupedMatchStream(match) },
                                        onSelectQuality = {
                                            if (match.streams.size > 1) selectedGroupedMatchForQuality = match else launchGroupedMatchStream(match)
                                        }
                                    )
                                }
                            }

                            if (classifiedSports.liveMatches.isEmpty() && classifiedSports.upcoming24hMatches.isEmpty() && todayRealMatches.isNotEmpty() && selectedSportNetwork == "ALL") {
                                item(key = "header_today_matches") {
                                    Text("🏆 مباريات اليوم والقنوات الناقلة", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                itemsIndexed(todayRealMatches.take(10), key = { idx, it -> "real_${idx}_${it.id}" }) { _, match ->
                                    val title = "${match.homeTeam} vs ${match.awayTeam}"
                                    val isReminded = MatchReminderManager.remindedMatches.contains(title)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF151522))
                                            .border(1.dp, Color(0xFF28283E), RoundedCornerShape(14.dp))
                                            .clickable { selectedMatchForChannelPicker = match }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("${match.league}  •  ${match.matchTime}", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                                            Text("📺 القناة الناقلة: ${match.suggestedBroadcaster}", color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (ch.logo.isNotEmpty()) {
                                            AsyncImage(model = ch.logo, contentDescription = null, modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)))
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

                // 3: السينما
                3 -> {
                    val cinemaItems = remember(allItems.size, cinemaSubTab, selectedOrigin) {
                        allItems.filter {
                            it.contentType == cinemaSubTab &&
                                (selectedOrigin == "ALL" || it.originTag == selectedOrigin)
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MOVIE" to strings.moviesTab, "SERIES" to strings.seriesTab).forEach { (type, label) ->
                                val active = cinemaSubTab == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (active) gold else Color(0xFF14141E))
                                        .clickable { cinemaSubTab = type }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (active) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("ALL" to strings.all, "عربي" to "عربي", "كوري" to "كوري", "هندي" to "هندي", "أجنبي" to "أجنبي")) { (tag, label) ->
                                FilterChip(
                                    selected = selectedOrigin == tag,
                                    onClick = { selectedOrigin = tag },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }

                        LazyVerticalGrid(
                            state = if (cinemaSubTab == "MOVIE") moviesGridState else seriesGridState,
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(cinemaItems, key = { it.id }) { item ->
                                val progress = remember(item.id) { DevkanVault.getVodProgressFraction(context, item.id) }
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF13131B))
                                        .border(1.dp, Color(0xFF232332), RoundedCornerShape(16.dp))
                                        .clickable {
                                            if (item.contentType == "SERIES") selectedSeriesForDetails = item
                                            else selectedMovieForDetails = item
                                        }
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box {
                                        if (item.logo.isNotEmpty()) {
                                            AsyncImage(
                                                model = item.logo,
                                                contentDescription = item.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().height(150.dp).background(Color(0xFF1D1D29), RoundedCornerShape(12.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.MovieFilter, contentDescription = null, tint = gold, modifier = Modifier.size(36.dp))
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(item.originTag, color = goldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            if (item.rating.isNotEmpty() && item.rating != "0") {
                                                Box(
                                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("IMDb ${item.rating}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    if (progress > 0.02f) {
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            color = gold,
                                            trackColor = Color(0xFF2B2B3C),
                                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                                        )
                                    }

                                    Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = if (item.year.isNotEmpty()) "${item.year} • ${item.genreTag}" else item.genreTag,
                                        color = Color(0xFF8E929F),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // 4: الإعدادات
                4 -> {
                    DevkanSettingsScreen(
                        strings = strings,
                        lang = lang,
                        onLanguageChange = onLanguageChange,
                        subscription = currentSub,
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
                                    withContext(Dispatchers.IO) { DevkanVault.saveCachedItems(context, items) }
                                    classifiedSports = DevkanPlaylistRepository.syncAndClassifySportsEpg(context, updated, items)
                                }
                                isSyncing = false
                                Toast.makeText(context, "تم تحديث البيانات والجدول الرياضي بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteSub = {
                            DevkanVault.clearSubscription(context)
                            currentSub = null
                            allItems.clear()
                            cachedSportsChannels = emptyList()
                            classifiedSports = ClassifiedSportsData(emptyList(), emptyList(), emptyList(), emptyList())
                            Toast.makeText(context, "تم حذف الاشتراك", Toast.LENGTH_SHORT).show()
                        },
                        onOpenAddDialog = { showAddDialog = true },
                        sampleTestUrl = allItems.firstOrNull()?.url ?: "https://www.google.com"
                    )
                }
            }
        }
    }

    // نافذة اختيار الجودة عند وجود أكثر من قناة ناقلة لنفس المباراة
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

    // نافذة اختيار القناة الناقلة السريعة للمباريات العامة
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
            onDismiss = { showAddDialog = false },
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
                        withContext(Dispatchers.IO) { DevkanVault.saveCachedItems(context, loadedItems) }
                        classifiedSports = DevkanPlaylistRepository.syncAndClassifySportsEpg(context, updatedSub, loadedItems)
                        Toast.makeText(context, "تم الاتصال وحفظ ${loadedItems.size} قناة وفيلم ومسلسل!", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    } else {
                        Toast.makeText(context, "تعذر جلب القنوات، تأكد من البيانات", Toast.LENGTH_LONG).show()
                    }
                    isSyncing = false
                }
            }
        )
    }
}

@Composable
fun SmartEpgMatchCard(
    match: GroupedSportsMatch,
    onPlayPrimary: () -> Unit,
    onSelectQuality: () -> Unit
) {
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)
    val timeFormatted = remember(match.startTime) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(match.startTime))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF171626), Color(0xFF101522))))
            .border(1.dp, if (match.isLive) Color(0xFFEF4444).copy(alpha = 0.55f) else gold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable { onPlayPrimary() }
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = match.competition ?: "بث رياضي",
                color = goldLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .background(
                        if (match.isLive) Color(0xFFEF4444).copy(alpha = 0.22f) else Color(0xFF16A34A).copy(alpha = 0.22f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (match.isLive) "🔴 مباشر الآن" else "🟢 $timeFormatted",
                    color = if (match.isLive) Color(0xFFF87171) else Color(0xFF4ADE80),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = "${match.homeTeam}   🆚   ${match.awayTeam}",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📺 ${match.primaryStream.channelName}",
                color = Color(0xFF93C5FD),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (match.streams.size > 1) {
                Text(
                    text = "+${match.streams.size - 1} جودات ▾",
                    color = goldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(gold.copy(alpha = 0.16f))
                        .clickable { onSelectQuality() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun CinemaHorizontalRow(
    title: String,
    items: List<MediaItemModel>,
    onMoreClick: () -> Unit,
    onItemClick: (MediaItemModel) -> Unit
) {
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                text = "المزيد",
                color = goldLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onMoreClick() }.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                Column(
                    modifier = Modifier
                        .width(135.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF13131B))
                        .clickable { onItemClick(item) }
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box {
                        if (item.logo.isNotEmpty()) {
                            AsyncImage(
                                model = item.logo,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(165.dp).clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(165.dp).background(Color(0xFF1D1D29), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Movie, contentDescription = null, tint = gold)
                            }
                        }
                        if (item.rating.isNotEmpty() && item.rating != "0") {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(5.dp)
                                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(5.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text("IMDb ${item.rating}", color = goldLight, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = if (item.year.isNotEmpty()) "${item.year} • ${item.genreTag}" else item.genreTag,
                        color = Color(0xFF9CA3AF),
                        fontSize = 10.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun MovieDetailsScreen(
    movie: MediaItemModel,
    subscription: SavedSubscription?,
    isInWatchlist: Boolean,
    onToggleWatchlist: () -> Unit,
    onPlayMovie: (MediaItemModel) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)
    var details by remember { mutableStateOf<VodDetailedInfo?>(null) }
    val savedProgress = remember { DevkanVault.getVodProgressFraction(context, movie.id) }

    LaunchedEffect(movie.id) {
        if (subscription != null && subscription.type == "XTREAM" && movie.rawId.isNotEmpty()) {
            details = DevkanPlaylistRepository.fetchVodDetails(subscription, movie.rawId, movie.title, movie.logo)
        } else {
            details = VodDetailedInfo(movie.title, "جاهز للمشاهدة الفورية بجودة عالية.", "غير متوفر", "غير متوفر", movie.year.ifEmpty { "2025" }, "HD", movie.rating.ifEmpty { "8.0" }, movie.logo)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070A))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        val info = details
        if (info == null) {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = gold)
            }
        } else {
            if (info.backdropUrl.isNotEmpty()) {
                AsyncImage(
                    model = info.backdropUrl,
                    contentDescription = info.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(18.dp))
                )
            }

            Text(info.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(gold.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("IMDb ⭐ ${info.rating}", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text("${info.releaseDate} • ${info.duration}", color = Color(0xFFB0B5C0), fontSize = 12.sp)
            }

            if (savedProgress > 0.02f) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("وصلت إلى ${(savedProgress * 100).toInt()}% من الفيلم", color = goldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { savedProgress },
                        color = gold,
                        trackColor = Color(0xFF252536),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onPlayMovie(movie) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (savedProgress > 0.02f) "إكمال المشاهدة" else "شاهد الآن",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }

                OutlinedButton(
                    onClick = onToggleWatchlist,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(if (isInWatchlist) Icons.Default.Check else Icons.Default.Add, contentDescription = null, tint = goldLight)
                    Spacer(Modifier.width(4.dp))
                    Text(if (isInWatchlist) "في القائمة" else "لاحقاً", color = Color.White, fontSize = 12.sp)
                }
            }

            Text(info.plot, color = Color(0xFFD1D5DB), fontSize = 13.sp, lineHeight = 20.sp)
            Text("الممثلون: ${info.cast}", color = Color(0xFF9CA3AF), fontSize = 12.sp)
            Text("المخرج: ${info.director}", color = Color(0xFF9CA3AF), fontSize = 12.sp)
        }
    }
}

@Composable
fun SeriesDetailsScreen(
    series: MediaItemModel,
    subscription: SavedSubscription?,
    isInWatchlist: Boolean,
    onToggleWatchlist: () -> Unit,
    onPlayEpisode: (MediaItemModel) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    var details by remember { mutableStateOf<SeriesDetailedInfo?>(null) }
    var selectedSeason by remember { mutableIntStateOf(1) }

    LaunchedEffect(series.id) {
        if (subscription != null && subscription.type == "XTREAM" && series.rawId.isNotEmpty()) {
            val fetched = DevkanPlaylistRepository.fetchSeriesDetails(subscription, series.rawId, series.title, series.logo)
            details = fetched
            if (fetched.seasons.isNotEmpty()) {
                selectedSeason = fetched.seasons.keys.first()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(series.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        val info = details
        if (info == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = gold)
            }
        } else {
            val episodes = info.seasons[selectedSeason] ?: emptyList()

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (info.backdropUrl.isNotEmpty()) {
                    AsyncImage(
                        model = info.backdropUrl,
                        contentDescription = info.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(110.dp).height(155.dp).clip(RoundedCornerShape(14.dp))
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("IMDb ⭐ ${info.rating}  •  ${info.releaseDate}", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(info.plot, color = Color(0xFFD1D5DB), fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val firstEp = episodes.firstOrNull()
                                if (firstEp != null) {
                                    onPlayEpisode(
                                        MediaItemModel(firstEp.id, firstEp.id, "${series.title} - م${firstEp.seasonNum} ح${firstEp.episodeNum}", firstEp.url, "", series.group, series.logo, "SERIES")
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("شاهد الآن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = onToggleWatchlist,
                            modifier = Modifier.size(38.dp).background(Color(0xFF1A1A26), CircleShape)
                        ) {
                            Icon(if (isInWatchlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isInWatchlist) Color(0xFFEF4444) else Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (info.seasons.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(info.seasons.keys.toList()) { seasonNum ->
                        val active = selectedSeason == seasonNum
                        FilterChip(
                            selected = active,
                            onClick = { selectedSeason = seasonNum },
                            label = {
                                Text(
                                    text = "الموسم $seasonNum",
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(episodes, key = { it.id }) { ep ->
                    val progress = remember(ep.id) { DevkanVault.getVodProgressFraction(context, ep.id) }
                    val statusText = when {
                        progress >= 0.90f -> "مكتملة ✓"
                        progress >= 0.45f -> "نصف الحلقة (${(progress * 100).toInt()}%)"
                        progress >= 0.15f -> "ربع الحلقة (${(progress * 100).toInt()}%)"
                        progress > 0.02f -> "قيد المشاهدة (${(progress * 100).toInt()}%)"
                        else -> ep.duration.ifEmpty { "جاهزة للمشاهدة" }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF13131B))
                            .border(1.dp, if (progress > 0.02f) gold.copy(alpha = 0.45f) else Color(0xFF232332), RoundedCornerShape(14.dp))
                            .clickable {
                                onPlayEpisode(
                                    MediaItemModel(
                                        id = ep.id,
                                        rawId = ep.id,
                                        title = "${series.title} - م${ep.seasonNum} ح${ep.episodeNum}",
                                        url = ep.url,
                                        group = series.group,
                                        logo = series.logo,
                                        contentType = "SERIES"
                                    )
                                )
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.width(105.dp).height(64.dp).clip(RoundedCornerShape(10.dp))) {
                            if (series.logo.isNotEmpty()) {
                                AsyncImage(model = series.logo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                            Box(
                                modifier = Modifier.align(Alignment.Center).size(28.dp).background(Color(0xFFDC2626).copy(alpha = 0.9f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${series.title} • الحلقة ${ep.episodeNum}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(statusText, color = if (progress > 0.02f) goldLight else Color.Gray, fontSize = 11.sp)
                            if (progress > 0.02f) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = gold,
                                    trackColor = Color(0xFF2B2B3C),
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DevkanSettingsScreen(
    strings: AppStrings,
    lang: String,
    onLanguageChange: (String) -> Unit,
    subscription: SavedSubscription?,
    onRefreshSub: () -> Unit,
    onDeleteSub: () -> Unit,
    onOpenAddDialog: () -> Unit,
    sampleTestUrl: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    var langMenuExpanded by remember { mutableStateOf(false) }
    var engineMenuExpanded by remember { mutableStateOf(false) }
    var syncMenuExpanded by remember { mutableStateOf(false) }

    var selectedEngine by remember { mutableStateOf(DevkanVault.getPreferredEngine(context)) }
    var selectedSyncInterval by remember { mutableStateOf(DevkanVault.getAutoSyncInterval(context)) }
    var lowLatency by remember { mutableStateOf(DevkanVault.isLowLatencyEnabled(context)) }
    var geoBypass by remember { mutableStateOf(DevkanVault.isGeoBypassEnabled(context)) }
    var autoResume by remember { mutableStateOf(DevkanVault.isAutoResumeEnabled(context)) }

    var pinInput by remember { mutableStateOf(DevkanVault.getAppLockPin(context)) }
    var speedResult by remember { mutableStateOf<String?>(null) }
    var testingSpeed by remember { mutableStateOf(false) }

    val languagesList = listOf(
        "ar" to "العربية (Arabic)",
        "en" to "English (الإنجليزية)",
        "de" to "Deutsch (الألمانية)",
        "tr" to "Türkçe (التركية)",
        "fr" to "Français (الفرنسية)",
        "ku" to "کوردی (الكردية)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1B170E), Color(0xFF111118))))
                .border(1.dp, gold.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = goldLight)
                    Text(strings.subscriptionCard, color = goldLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                if (subscription != null) {
                    Box(
                        modifier = Modifier.background(Color(0xFF16A34A).copy(alpha = 0.22f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(subscription.status, color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (subscription != null) {
                Text(
                    text = when (subscription.type) {
                        "XTREAM" -> "Xtream Codes • ${subscription.username}"
                        "STALKER" -> "Stalker MAC • ${subscription.macAddress}"
                        else -> "M3U Playlist"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.expiryLabel, color = Color.Gray, fontSize = 12.sp)
                    Text(subscription.expDateFormatted, color = goldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.countryLabel, color = Color.Gray, fontSize = 12.sp)
                    Text(subscription.allowedCountries, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.connectionsLabel, color = Color.Gray, fontSize = 12.sp)
                    Text("${subscription.activeCons} / ${subscription.maxCons}", color = Color.White, fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRefreshSub, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = gold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تحديث الآن", color = gold, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = onDeleteSub, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("حذف", color = Color(0xFFF87171), fontSize = 12.sp)
                    }
                }
            } else {
                Text("لا يوجد اشتراك محفوظ حالياً", color = Color.Gray, fontSize = 12.sp)
                Button(onClick = onOpenAddDialog, colors = ButtonDefaults.buttonColors(containerColor = gold), modifier = Modifier.fillMaxWidth()) {
                    Text(strings.addConnection, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF13131B))
                .clickable { syncMenuExpanded = true }
                .padding(14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, tint = gold)
                    Column {
                        Text(strings.autoSyncTitle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (lang == "ar" || lang == "ku") selectedSyncInterval.labelAr else selectedSyncInterval.labelEn,
                            color = goldLight,
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = gold)
            }
            DropdownMenu(expanded = syncMenuExpanded, onDismissRequest = { syncMenuExpanded = false }) {
                AutoSyncInterval.entries.forEach { interval ->
                    DropdownMenuItem(
                        text = { Text(if (lang == "ar" || lang == "ku") interval.labelAr else interval.labelEn) },
                        onClick = {
                            selectedSyncInterval = interval
                            DevkanVault.setAutoSyncInterval(context, interval)
                            syncMenuExpanded = false
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF13131B))
                .clickable { langMenuExpanded = true }
                .padding(14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = gold)
                    Text("اللغة / Language / Sprache", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val currentLabel = languagesList.firstOrNull { it.first == lang }?.second?.substringBefore(" ") ?: "العربية"
                    Text(currentLabel, color = goldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = gold)
                }
            }
            DropdownMenu(expanded = langMenuExpanded, onDismissRequest = { langMenuExpanded = false }) {
                languagesList.forEach { (code, title) ->
                    DropdownMenuItem(
                        text = { Text(title, fontWeight = if (lang == code) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            onLanguageChange(code)
                            langMenuExpanded = false
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF13131B))
                .clickable { engineMenuExpanded = true }
                .padding(14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.SettingsInputComponent, contentDescription = null, tint = gold)
                    Column {
                        Text(strings.engineTitle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        val engineName = when (lang) {
                            "en" -> selectedEngine.labelEn
                            "de" -> selectedEngine.labelDe
                            "tr" -> selectedEngine.labelTr
                            else -> selectedEngine.labelAr
                        }
                        Text(engineName, color = gold, fontSize = 11.sp)
                    }
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = gold)
            }
            DropdownMenu(expanded = engineMenuExpanded, onDismissRequest = { engineMenuExpanded = false }) {
                EngineType.entries.forEach { eng ->
                    val label = when (lang) {
                        "en" -> eng.labelEn
                        "de" -> eng.labelDe
                        "tr" -> eng.labelTr
                        else -> eng.labelAr
                    }
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            selectedEngine = eng
                            DevkanVault.savePreferredEngine(context, eng)
                            engineMenuExpanded = false
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF13131B))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(strings.lowLatencyTitle, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = lowLatency, onCheckedChange = {
                    lowLatency = it
                    DevkanVault.setLowLatencyEnabled(context, it)
                })
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(strings.geoBypassTitle, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = geoBypass, onCheckedChange = {
                    geoBypass = it
                    DevkanVault.setGeoBypassEnabled(context, it)
                })
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(strings.autoResumeTitle, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = autoResume, onCheckedChange = {
                    autoResume = it
                    DevkanVault.setAutoResumeEnabled(context, it)
                })
            }
        }

        Button(
            onClick = {
                testingSpeed = true
                scope.launch {
                    val (ping, mbps) = EngineCoordinator.runServerSpeedAndPingTest(sampleTestUrl)
                    speedResult = "Ping: ${ping} ms   •   Speed: ${mbps} Mbps"
                    testingSpeed = false
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = gold),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Text(if (testingSpeed) "جاري الفحص..." else strings.speedTest, color = Color.Black, fontWeight = FontWeight.Bold)
        }
        speedResult?.let {
            Text(it, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF13131B))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(strings.appLockTitle, color = gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6) pinInput = it },
                    placeholder = { Text("4-6 digits (فارغ = معطل)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        DevkanVault.setAppLockPin(context, pinInput)
                        Toast.makeText(context, if (pinInput.isEmpty()) "تم إيقاف القفل" else "تم حفظ رمز القفل", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = gold)
                ) {
                    Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SmartAddConnectionDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSaveAndConnect: (SavedSubscription) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val gold = Color(0xFFD4AF37)

    var selectedMode by remember { mutableStateOf("XTREAM") }
    var smartPasteInput by remember { mutableStateOf("") }
    var autoDetectedBanner by remember { mutableStateOf(false) }

    var host by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var m3uUrl by remember { mutableStateOf("") }
    var macAddress by remember { mutableStateOf("00:1A:79:") }

    fun parseSmartUrl(raw: String) {
        smartPasteInput = raw
        try {
            val uri = Uri.parse(raw.trim())
            val u = uri.getQueryParameter("username")
            val p = uri.getQueryParameter("password")
            val scheme = uri.scheme
            val auth = uri.authority
            if (!u.isNullOrEmpty() && !p.isNullOrEmpty() && !scheme.isNullOrEmpty() && !auth.isNullOrEmpty()) {
                host = "$scheme://$auth"
                username = u
                password = p
                selectedMode = "XTREAM"
                autoDetectedBanner = true
            } else if (raw.trim().startsWith("http")) {
                m3uUrl = raw.trim()
                autoDetectedBanner = false
            }
        } catch (_: Exception) { }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141D),
        title = { Text(strings.addConnection, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("XTREAM" to "Xtream Codes", "M3U" to "قائمة M3U", "STALKER" to "Stalker MAC").forEach { (mode, label) ->
                        val active = selectedMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) gold else Color(0xFF1F1F2C))
                                .clickable { selectedMode = mode }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (active) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                OutlinedTextField(
                    value = smartPasteInput,
                    onValueChange = { parseSmartUrl(it) },
                    singleLine = true,
                    label = { Text("رابط M3U / Xtream (الصقه للتعبئة التلقائية)", fontSize = 11.sp) },
                    trailingIcon = {
                        IconButton(onClick = {
                            val clip = clipboardManager.getText()?.text ?: ""
                            if (clip.isNotEmpty()) parseSmartUrl(clip)
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = gold)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (autoDetectedBanner) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(16.dp))
                        Text("تم اكتشاف بيانات الاعتماد تلقائياً", color = Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                when (selectedMode) {
                    "XTREAM" -> {
                        OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("رابط الخادم (http://server:port)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("اسم المستخدم") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة المرور") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "M3U" -> {
                        OutlinedTextField(value = m3uUrl, onValueChange = { m3uUrl = it }, label = { Text("رابط قائمة التشغيل M3U / M3U8") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    "STALKER" -> {
                        OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("رابط البورتال (http://portal.com/c)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = macAddress, onValueChange = { macAddress = it }, label = { Text("عنوان MAC (00:1A:79:...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = gold),
                onClick = {
                    val sub = when (selectedMode) {
                        "XTREAM" -> SavedSubscription("XTREAM", host.trim(), username.trim(), password.trim())
                        "M3U" -> SavedSubscription("M3U", m3uUrl.trim(), m3uUrl = m3uUrl.trim())
                        else -> SavedSubscription("STALKER", host.trim(), macAddress = macAddress.trim())
                    }
                    onSaveAndConnect(sub)
                }
            ) {
                Text("حفظ واتصال", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = Color.Gray) }
        }
    )
}
