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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

private fun isSmartSportChannel(group: String, title: String): Boolean {
    if (SportsMatchExtractor.isSportsProgram(group, title, null)) return true
    val text = "$group $title".lowercase()
    val extraKeywords = listOf(
        "alwan", "ألوان", "الوان",
        "alkass", "kass", "الكأس", "الكاس", "كاس",
        "rabiaa", "الرابعة", "رابعة",
        "kuwait", "ktv sport", "الكويت",
        "bein", "ssc", "abu dhabi", "ad sport", "أبو ظبي", "ابوظبي",
        "dubai sport", "دبي الرياضية", "on time", "أون تايم", "اون تايم",
        "shahid sport", "شاهد رياضة", "dazn", "sky sport", "canal+ sport", "arena sport",
        "espn", "bt sport", "tnt sport", "euro sport", "eurosport", "fox sport", "cbs sport",
        "super sport", "supersport", "premier sport", "viaplay", "eleven sport", "movistar",
        "starzplay", "tod", "رياضة", "رياضية", "sport", "match", "liga", "premier"
    )
    return extraKeywords.any { text.contains(it) }
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
                text = activeSubLabel.ifEmpty { "الاتصالات" },
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

@Composable
fun DevkanWelcomeOnboardingScreen(
    onAddSubscriptionClick: () -> Unit,
    onImportM3uClick: () -> Unit
) {
    val goldLight = Color(0xFFFDE047)
    val goldMid = Color(0xFFD4AF37)
    val goldDark = Color(0xFFB45309)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0A10), Color(0xFF12101A), Color(0xFF07070A))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF12121A).copy(alpha = 0.92f))
                .border(1.dp, goldMid.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(goldLight, goldMid, goldDark)))
                    .border(2.dp, goldLight, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = "DEVKAN Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "DEVKAN",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    letterSpacing = 3.sp
                )
                Text(
                    text = "34 VIP CINEMA & SPORTS PLAYER",
                    color = goldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp
                )
            }

            Text(
                text = "مشغل وسائط احترافي فائق السرعة للبث المباشر والمباريات الحية والأفلام والمسلسلات بجودة 4K.",
                color = Color(0xFFA1A1AA),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onAddSubscriptionClick,
                colors = ButtonDefaults.buttonColors(containerColor = goldMid),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("إضافة اشتراك للبدء", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick = onImportM3uClick,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, goldMid.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = goldLight, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("استيراد ملف M3U من الجهاز", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
    var activeStream by remember { mutableStateOf<MediaItemModel?>(null) }
    var activeSeriesTracking by remember { mutableStateOf<Triple<String, Int, Int>?>(null) }
    var isSportsPlayback by remember { mutableStateOf(false) }

    var selectedMovieForDetails by remember { mutableStateOf<MediaItemModel?>(null) }
    var selectedSeriesForDetails by remember { mutableStateOf<MediaItemModel?>(null) }
    var selectedMatchForChannelPicker by remember { mutableStateOf<RealSportsMatch?>(null) }
    var showConnectionsManager by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var subToEdit by remember { mutableStateOf<SavedSubscription?>(null) }
    var showEpgSheet by remember { mutableStateOf(false) }
    var showQuickSearch by remember { mutableStateOf(false) }
    var quickSearchQuery by remember { mutableStateOf("") }

    var selectedLiveCategory by remember { mutableStateOf("ALL") }
    var liveFilterQuery by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf("") }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var categoriesRevision by remember { mutableIntStateOf(0) }

    var selectedSportDiscipline by remember { mutableStateOf("SOCCER") }
    var selectedSportCountry by remember { mutableStateOf("ALL") }
    var selectedSportLeague by remember { mutableStateOf("ALL") }
    var sportSearchQuery by remember { mutableStateOf("") }
    var showCountryDropdown by remember { mutableStateOf(false) }
    var showLeagueDropdown by remember { mutableStateOf(false) }

    var movieSmartFilter by remember { mutableStateOf("ALL") }
    var movieServerGroup by remember { mutableStateOf("ALL") }
    var movieQuery by remember { mutableStateOf("") }
    var showMovieGroupsSheet by remember { mutableStateOf(false) }

    var seriesSmartFilter by remember { mutableStateOf("ALL") }
    var seriesServerGroup by remember { mutableStateOf("ALL") }
    var seriesQuery by remember { mutableStateOf("") }
    var showSeriesGroupsSheet by remember { mutableStateOf(false) }

    val liveCategoryRowState = rememberLazyListState()
    val liveListState = rememberLazyListState()
    val moviesGridState = rememberLazyGridState()
    val seriesGridState = rememberLazyGridState()
    val sportsListState = rememberLazyListState()
    val homeScrollState = rememberScrollState()

    var currentSub by remember { mutableStateOf(DevkanVault.getSavedSubscription(context)) }
    val allItems = remember { mutableStateListOf<MediaItemModel>() }
    val todayRealMatches = remember { mutableStateListOf<RealSportsMatch>() }
    val watchlistIds = remember { mutableStateListOf<String>() }

    var cachedSportsChannels by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedLiveChannels by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedLiveGroups by remember { mutableStateOf<List<String>>(listOf("ALL")) }
    var cachedHomeLatestMovies by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedHome4kMovies by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedHomeTopSeries by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedMovieGroups by remember { mutableStateOf<List<String>>(listOf("ALL")) }
    var cachedMovieFilters by remember { mutableStateOf<List<Pair<String, String>>>(listOf("ALL" to "الكل")) }
    var cachedSeriesGroups by remember { mutableStateOf<List<String>>(listOf("ALL")) }
    var cachedSeriesFilters by remember { mutableStateOf<List<Pair<String, String>>>(listOf("ALL" to "الكل")) }

    var filteredLiveList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var filteredMoviesList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var filteredSeriesList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var vodProgressCache by remember { mutableStateOf(mapOf("" to 0f)) }

    // دالة تحميل متدرجة سريعة وآمنة للذاكرة
    fun startProgressiveSubscriptionLoad(targetSub: SavedSubscription, isNewConnection: Boolean) {
        scope.launch {
            isSyncing = true
            syncStatusText = "جاري الاتصال بالسيرفر..."
            val geoBypass = DevkanVault.isGeoBypassEnabled(context)
            var liveShownToast = false

            val (updatedSub, fullItems) = DevkanPlaylistRepository.loadSubscriptionProgressive(
                sub = targetSub,
                geoBypass = geoBypass,
                onStatusUpdate = { msg ->
                    withContext(Dispatchers.Main) {
                        syncStatusText = msg
                    }
                },
                onPartialItemsReady = { subSnapshot, partialList ->
                    withContext(Dispatchers.Main) {
                        currentSub = subSnapshot
                        allItems.clear()
                        allItems.addAll(partialList)
                        if (isNewConnection && !liveShownToast && partialList.isNotEmpty()) {
                            liveShownToast = true
                            Toast.makeText(context, "تم تحميل ${partialList.size} قناة مباشرة ✓", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            currentSub = updatedSub
            DevkanVault.saveSubscription(context, updatedSub)
            if (fullItems.isNotEmpty()) {
                allItems.clear()
                allItems.addAll(fullItems)
                withContext(Dispatchers.IO) {
                    DevkanVault.saveCachedItems(context, fullItems, updatedSub.id)
                }
                if (todayRealMatches.isEmpty()) {
                    val realMatches = RealSportsDataSource.fetchTodayRealMatches()
                    if (realMatches.isNotEmpty()) {
                        todayRealMatches.clear()
                        todayRealMatches.addAll(realMatches)
                    }
                }
                Toast.makeText(context, "اكتمل التحديث (${fullItems.size} عنصر)!", Toast.LENGTH_SHORT).show()
            } else if (isNewConnection) {
                Toast.makeText(context, "تعذر جلب القنوات، تأكد من الرابط أو VPN", Toast.LENGTH_LONG).show()
            }
            isSyncing = false
            syncStatusText = ""
        }
    }

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
            startProgressiveSubscriptionLoad(targetSub, isNewConnection = subCache.isEmpty())
        }
    }

    LaunchedEffect(allItems.size, lang, currentSub?.id, categoriesRevision) {
        if (allItems.isEmpty()) {
            cachedLiveChannels = emptyList()
            cachedLiveGroups = listOf("ALL")
            cachedSportsChannels = emptyList()
            cachedHomeLatestMovies = emptyList()
            cachedHome4kMovies = emptyList()
            cachedHomeTopSeries = emptyList()
            cachedMovieGroups = listOf("ALL")
            cachedMovieFilters = listOf("ALL" to "الكل")
            cachedSeriesGroups = listOf("ALL")
            cachedSeriesFilters = listOf("ALL" to "الكل")
        } else {
            val snapshot = allItems.toList()
            val subId = currentSub?.id ?: "default"
            val hiddenLive = DevkanVault.getHiddenCategories(context, subId, "LIVE")
            val hiddenMovie = DevkanVault.getHiddenCategories(context, subId, "MOVIE")
            val hiddenSeries = DevkanVault.getHiddenCategories(context, subId, "SERIES")

            withContext(Dispatchers.Default) {
                val live = snapshot.filter { it.contentType == "LIVE" && !hiddenLive.contains(it.group) }
                val rawLiveGroups = listOf("ALL") + live.map { it.group }.distinct()
                val sortedLiveGroups = LanguagePrioritySorter.sortGroupsByLanguage(rawLiveGroups, lang)

                val rawSports = live.filter { isSmartSportChannel(it.group, it.title) }
                val dedupedSports = DevkanPlaylistRepository.deduplicateSportChannels(rawSports)

                val movies = snapshot.filter { it.contentType == "MOVIE" && !hiddenMovie.contains(it.group) }
                val latestMov = movies.sortedWith(
                    compareByDescending<MediaItemModel> { it.addedTimestamp }
                        .thenByDescending { it.year.toIntOrNull() ?: 0 }
                ).take(15)
                val mov4k = movies.filter { it.is4k }.sortedByDescending { it.addedTimestamp }.take(15)
                val movGrps = LanguagePrioritySorter.sortGroupsByLanguage(listOf("ALL") + movies.map { it.group }.distinct(), lang)
                val movFilters = mutableListOf("ALL" to "الكل", "RECENT" to "مضاف حديثاً", "TOP" to "أعلى تقييماً ⭐").apply {
                    if (movies.any { it.is4k }) add("4K" to "4K UHD")
                    listOf("دراما", "أكشن", "خيال علمي", "رومانسي", "كوميدي", "رعب وإثارة", "تاريخي", "أنمي وعائلي").forEach { g ->
                        if (movies.any { it.genreTag == g }) add(g to g)
                    }
                    listOf("عربي", "كوري", "تركي", "هندي", "أجنبي").forEach { o ->
                        if (movies.any { it.originTag == o }) add(o to "أفلام $o")
                    }
                }

                val series = snapshot.filter { it.contentType == "SERIES" && !hiddenSeries.contains(it.group) }
                val topSer = series.sortedWith(
                    compareByDescending<MediaItemModel> { it.rating.toDoubleOrNull() ?: 0.0 }
                        .thenByDescending { it.addedTimestamp }
                ).take(15)
                val serGrps = LanguagePrioritySorter.sortGroupsByLanguage(listOf("ALL") + series.map { it.group }.distinct(), lang)
                val serFilters = mutableListOf("ALL" to "الكل", "RECENT" to "مضاف حديثاً", "TOP" to "أعلى تقييماً ⭐").apply {
                    listOf("عربي", "كوري", "تركي", "هندي", "أجنبي").forEach { o ->
                        if (series.any { it.originTag == o }) add(o to "مسلسلات $o")
                    }
                    listOf("دراما", "أكشن", "خيال علمي", "رومانسي", "تاريخي", "كوميدي", "رعب وإثارة").forEach { g ->
                        if (series.any { it.genreTag == g }) add(g to g)
                    }
                }

                withContext(Dispatchers.Main) {
                    cachedLiveChannels = live
                    cachedLiveGroups = sortedLiveGroups
                    cachedSportsChannels = dedupedSports
                    cachedHomeLatestMovies = latestMov
                    cachedHome4kMovies = mov4k
                    cachedHomeTopSeries = topSer
                    cachedMovieGroups = movGrps
                    cachedMovieFilters = movFilters
                    cachedSeriesGroups = serGrps
                    cachedSeriesFilters = serFilters
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

    LaunchedEffect(allItems.size, movieSmartFilter, movieServerGroup, movieQuery, activeStream, categoriesRevision) {
        if (allItems.isEmpty()) {
            filteredMoviesList = emptyList()
        } else {
            val snapshot = allItems.toList()
            val subId = currentSub?.id ?: "default"
            val hiddenMovie = DevkanVault.getHiddenCategories(context, subId, "MOVIE")
            val q = movieQuery.trim()
            val result = withContext(Dispatchers.Default) {
                val moviesOnly = snapshot.filter { it.contentType == "MOVIE" && !hiddenMovie.contains(it.group) }
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

    LaunchedEffect(allItems.size, seriesSmartFilter, seriesServerGroup, seriesQuery, activeStream, categoriesRevision) {
        if (allItems.isEmpty()) {
            filteredSeriesList = emptyList()
        } else {
            val snapshot = allItems.toList()
            val subId = currentSub?.id ?: "default"
            val hiddenSeries = DevkanVault.getHiddenCategories(context, subId, "SERIES")
            val q = seriesQuery.trim()
            val result = withContext(Dispatchers.Default) {
                val seriesOnly = snapshot.filter { it.contentType == "SERIES" && !hiddenSeries.contains(it.group) }
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
            !showConnectionsManager &&
            selectedMovieForDetails == null &&
            selectedSeriesForDetails == null &&
            !showEpgSheet &&
            !showQuickSearch &&
            selectedMatchForChannelPicker == null
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
        currentSub = saved
        if (saved != null) {
            val cached = withContext(Dispatchers.IO) { DevkanVault.getCachedItems(context, saved.id) }
            if (cached.isNotEmpty()) {
                allItems.clear()
                allItems.addAll(cached)
            }

            if (cached.isEmpty() || DevkanVault.shouldRunBackgroundSync(context)) {
                startProgressiveSubscriptionLoad(saved, isNewConnection = cached.isEmpty())
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
    }

    val m3uFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                isSyncing = true
                syncStatusText = "جاري قراءة ملف M3U..."
                val imported = DevkanPlaylistRepository.parseLocalUri(context, uri)
                if (imported.isNotEmpty()) {
                    val localSub = SavedSubscription(
                        id = "local_m3u",
                        label = "ملف M3U محلي",
                        type = "M3U",
                        host = uri.toString(),
                        m3uUrl = uri.toString()
                    )
                    DevkanVault.saveSubscription(context, localSub)
                    currentSub = localSub
                    allItems.clear()
                    allItems.addAll(imported)
                    withContext(Dispatchers.IO) { DevkanVault.saveCachedItems(context, imported, localSub.id) }
                    val realMatches = RealSportsDataSource.fetchTodayRealMatches()
                    if (realMatches.isNotEmpty()) {
                        todayRealMatches.clear()
                        todayRealMatches.addAll(realMatches)
                    }
                    Toast.makeText(context, "تم استيراد وحفظ ${imported.size} عنصر!", Toast.LENGTH_SHORT).show()
                }
                isSyncing = false
                syncStatusText = ""
            }
        }
    }

    fun handleClosePlayer() {
        val stream = activeStream
        val tracking = activeSeriesTracking
        if (stream != null && tracking != null) {
            val pos = DevkanVault.getVodPosition(context, stream.id)
            if (pos > 0L) {
                DevkanVault.saveVodProgress(
                    context = context,
                    streamId = stream.id,
                    posMs = pos,
                    durMs = pos + 60_000L,
                    seriesId = tracking.first,
                    seasonNum = tracking.second,
                    episodeNum = tracking.third
                )
            }
        }
        activeStream = null
        activeSeriesTracking = null
        isSportsPlayback = false
    }

    if (currentSub == null && allItems.isEmpty() && !isSyncing && !showConnectionsManager) {
        DevkanWelcomeOnboardingScreen(
            onAddSubscriptionClick = {
                subToEdit = null
                showAddDialog = true
            },
            onImportM3uClick = { m3uFileLauncher.launch("*/*") }
        )
        if (showAddDialog) {
            SmartAddConnectionDialog(
                strings = strings,
                editingSub = subToEdit,
                onDismiss = {
                    showAddDialog = false
                    subToEdit = null
                },
                onSaveAndConnect = { newSub ->
                    // إغلاق النافذة فوراً والبدء بالتحميل المتدرج
                    showAddDialog = false
                    subToEdit = null
                    currentSub = newSub
                    DevkanVault.saveSubscription(context, newSub)
                    startProgressiveSubscriptionLoad(newSub, isNewConnection = true)
                }
            )
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFF07070A),
            topBar = {
                Column {
                    DevkanBrandHeader(
                        activeSubLabel = currentSub?.label ?: "الاتصالات",
                        onSwitchSubClick = { showConnectionsManager = true },
                        onSearchClick = { showQuickSearch = true },
                        onImportClick = { m3uFileLauncher.launch("*/*") },
                        onSettingsClick = { selectedTab = 5 },
                        strings = strings
                    )
                    if (isSyncing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF13131E))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = syncStatusText.ifEmpty { "جاري تحديث المحتوى..." },
                                    color = goldLight,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${allItems.size} عنصر",
                                    color = Color(0xFF4ADE80),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                color = gold,
                                trackColor = Color(0xFF252538),
                                modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape)
                            )
                        }
                    }
                }
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
                when (selectedTab) {
                    0 -> {
                        val lastCh = remember(activeStream) { DevkanVault.getLastChannel(context) }
                        val homePills = remember { listOf("الكل", "مباشر", "رياضة", "أفلام", "مسلسلات", "EPG") }

                        val heroFeatured = cachedHomeTopSeries.firstOrNull { it.logo.isNotEmpty() }
                            ?: cachedHomeLatestMovies.firstOrNull { it.logo.isNotEmpty() }

                        val fallbackRealMatch = todayRealMatches.firstOrNull()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(homeScrollState)
                                .padding(vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(homePills, key = { it }) { pill ->
                                    val isSelected = pill == "الكل"
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) gold else Color(0xFF14141F))
                                            .border(1.dp, if (isSelected) gold else Color(0xFF28283A), RoundedCornerShape(50))
                                            .clickable {
                                                when (pill) {
                                                    "مباشر" -> selectedTab = 1
                                                    "رياضة" -> selectedTab = 2
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

                            if (fallbackRealMatch != null) {
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
                                                Text("اختر القناة الناقلة للمباراة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
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

                            val liveCount = todayRealMatches.count { it.isLive }
                            val totalCount = todayRealMatches.size
                            if (totalCount > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF111916))
                                        .border(1.dp, Color(0xFF16A34A).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                        .clickable { selectedTab = 2 }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(24.dp))
                                        Column {
                                            Text("رياضة • جدول المباريات والقنوات الناقلة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("🔴 $liveCount مباشر   •   ⚽ $totalCount مباراة اليوم", color = Color(0xFF4ADE80), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF4ADE80))
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
                                            activeSeriesTracking = null
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

                            if (cachedHomeLatestMovies.isNotEmpty()) {
                                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                    CinemaHorizontalRow(
                                        title = strings.latestMovies,
                                        items = cachedHomeLatestMovies,
                                        onMoreClick = { selectedTab = 3 },
                                        onItemClick = { selectedMovieForDetails = it }
                                    )
                                }
                            }

                            if (cachedHomeTopSeries.isNotEmpty()) {
                                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                    CinemaHorizontalRow(
                                        title = strings.featuredSeries,
                                        items = cachedHomeTopSeries,
                                        onMoreClick = { selectedTab = 4 },
                                        onItemClick = { selectedSeriesForDetails = it }
                                    )
                                }
                            }

                            if (cachedHome4kMovies.isNotEmpty()) {
                                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                    CinemaHorizontalRow(
                                        title = strings.movies4k,
                                        items = cachedHome4kMovies,
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

                            LazyRow(state = liveCategoryRowState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                isSportsPlayback = isSmartSportChannel(ch.group, ch.title)
                                                activeSeriesTracking = null
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
                        val soccerCount = remember(todayRealMatches.size) { todayRealMatches.count { it.sportType == "SOCCER" } }
                        val basketCount = remember(todayRealMatches.size) { todayRealMatches.count { it.sportType == "BASKETBALL" } }
                        val tennisCount = remember(todayRealMatches.size) { todayRealMatches.count { it.sportType == "TENNIS" } }
                        val liveTotal = remember(todayRealMatches.size) { todayRealMatches.count { it.isLive } }

                        val matchesBySport = remember(todayRealMatches.size, selectedSportDiscipline) {
                            todayRealMatches.filter { it.sportType == selectedSportDiscipline }
                        }

                        val availableCountries = remember(matchesBySport) {
                            listOf("ALL") + matchesBySport.map { it.country }.distinct()
                        }

                        val availableLeagues = remember(matchesBySport, selectedSportCountry) {
                            val filteredByCountry = if (selectedSportCountry == "ALL") matchesBySport
                            else matchesBySport.filter { it.country == selectedSportCountry }
                            listOf("ALL") + filteredByCountry.map { it.league }.distinct()
                        }

                        val finalMatches = remember(matchesBySport, selectedSportCountry, selectedSportLeague, sportSearchQuery) {
                            val q = sportSearchQuery.trim()
                            matchesBySport.filter { m ->
                                (selectedSportCountry == "ALL" || m.country == selectedSportCountry) &&
                                    (selectedSportLeague == "ALL" || m.league == selectedSportLeague) &&
                                    (q.isEmpty() || m.homeTeam.contains(q, ignoreCase = true) || m.awayTeam.contains(q, ignoreCase = true) || m.league.contains(q, ignoreCase = true))
                            }
                        }

                        val matchesGroupedByLeague = remember(finalMatches) {
                            finalMatches.groupBy { it.league }
                        }

                        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                    listOf(
                                        Triple("SOCCER", "⚽ كرة القدم ($soccerCount)", true),
                                        Triple("BASKETBALL", "🏀 السلة ($basketCount)", false),
                                        Triple("TENNIS", "🎾 التنس ($tennisCount)", false)
                                    ).forEach { (code, label, _) ->
                                        val active = selectedSportDiscipline == code
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (active) Color(0xFF2E264F) else Color(0xFF14141E))
                                                .border(1.dp, if (active) Color(0xFFA78BFA) else Color.Transparent, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    selectedSportDiscipline = code
                                                    selectedSportCountry = "ALL"
                                                    selectedSportLeague = "ALL"
                                                }
                                                .padding(horizontal = 9.dp, vertical = 7.dp)
                                        ) {
                                            Text(label, color = if (active) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF7F1D1D).copy(alpha = 0.55f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text("🔴 $liveTotal مباشر", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            OutlinedTextField(
                                value = sportSearchQuery,
                                onValueChange = { sportSearchQuery = it },
                                placeholder = { Text("ابحث عن فريق أو بطولة...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gold, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (sportSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { sportSearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0xFF171724))
                                            .border(1.dp, Color(0xFF2D2D42), RoundedCornerShape(50))
                                            .clickable { showCountryDropdown = true }
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🌐 الدولة: ${if (selectedSportCountry == "ALL") "الكل" else selectedSportCountry}",
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = goldLight, modifier = Modifier.size(16.dp))
                                    }
                                    DropdownMenu(
                                        expanded = showCountryDropdown,
                                        onDismissRequest = { showCountryDropdown = false }
                                    ) {
                                        availableCountries.forEach { c ->
                                            DropdownMenuItem(
                                                text = { Text(if (c == "ALL") "الكل" else c, color = Color.White, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedSportCountry = c
                                                    selectedSportLeague = "ALL"
                                                    showCountryDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0xFF171724))
                                            .border(1.dp, Color(0xFF2D2D42), RoundedCornerShape(50))
                                            .clickable { showLeagueDropdown = true }
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🏆 الدوري: ${if (selectedSportLeague == "ALL") "الكل" else selectedSportLeague}",
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = goldLight, modifier = Modifier.size(16.dp))
                                    }
                                    DropdownMenu(
                                        expanded = showLeagueDropdown,
                                        onDismissRequest = { showLeagueDropdown = false }
                                    ) {
                                        availableLeagues.forEach { l ->
                                            DropdownMenuItem(
                                                text = { Text(if (l == "ALL") "الكل" else l, color = Color.White, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedSportLeague = l
                                                    showLeagueDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            LazyColumn(state = sportsListState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                matchesGroupedByLeague.forEach { (leagueName, leagueMatches) ->
                                    item(key = "league_hdr_$leagueName") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🏆 $leagueName", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                            Text("${leagueMatches.size}", color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }

                                    itemsIndexed(leagueMatches, key = { idx, it -> "maya_${leagueName}_${idx}_${it.id}" }) { _, match ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF14141F))
                                                .border(
                                                    1.dp,
                                                    if (match.isLive) Color(0xFF16A34A).copy(alpha = 0.55f) else Color(0xFF252536),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedMatchForChannelPicker = match }
                                                .padding(horizontal = 10.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF16A34A).copy(alpha = 0.18f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.LiveTv, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(16.dp))
                                            }

                                            Spacer(Modifier.width(8.dp))

                                            Text(
                                                text = match.matchTime,
                                                color = if (match.isLive) Color(0xFF4ADE80) else Color(0xFF9CA3AF),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.width(56.dp)
                                            )

                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(match.homeTeam, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                if (match.homeLogo.isNotEmpty()) {
                                                    Spacer(Modifier.width(5.dp))
                                                    AsyncImage(model = match.homeLogo, contentDescription = null, modifier = Modifier.size(20.dp))
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 8.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (match.isLive) Color(0xFF14532D) else Color(0xFF222232))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = match.scoreText,
                                                    color = if (match.isLive) Color(0xFF4ADE80) else goldLight,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 12.sp
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Start
                                            ) {
                                                if (match.awayLogo.isNotEmpty()) {
                                                    AsyncImage(model = match.awayLogo, contentDescription = null, modifier = Modifier.size(20.dp))
                                                    Spacer(Modifier.width(5.dp))
                                                }
                                                Text(match.awayTeam, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
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
                                items(cachedMovieFilters, key = { it.first }) { (code, label) ->
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
                                        items(cachedMovieGroups, key = { it }) { grp ->
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
                                                Text(if (grp == "ALL") "كل مجلدات الأفلام" else grp, color = Color.White, fontSize = 13.sp)
                                                if (movieServerGroup == grp) Icon(Icons.Default.Check, contentDescription = null, tint = goldLight)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
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
                                items(cachedSeriesFilters, key = { it.first }) { (code, label) ->
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
                                        items(cachedSeriesGroups, key = { it }) { grp ->
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
                                                Text(if (grp == "ALL") "كل مجلدات المسلسلات" else grp, color = Color.White, fontSize = 13.sp)
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
                            onOpenConnectionsManager = { showConnectionsManager = true },
                            onRefreshSub = {
                                val sub = currentSub ?: return@DevkanSettingsScreen
                                startProgressiveSubscriptionLoad(sub, isNewConnection = false)
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
                    activeSeriesTracking = null
                    activeStream = item
                },
                onBack = { selectedMovieForDetails = null }
            )
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
                onPlayEpisode = { epItem, seriesId, seasonNum, episodeNum ->
                    isSportsPlayback = false
                    activeSeriesTracking = Triple(seriesId, seasonNum, episodeNum)
                    DevkanVault.saveVodProgress(context, epItem.id, 1000L, 60_000L, seriesId, episodeNum, seasonNum)
                    activeStream = epItem
                },
                onBack = { selectedSeriesForDetails = null }
            )
        }

        if (showConnectionsManager) {
            ConnectionsManagerScreen(
                strings = strings,
                activeSubId = currentSub?.id,
                allItemsForCategories = allItems.toList(),
                onActivateSub = { sub ->
                    showConnectionsManager = false
                    switchSubscriptionFast(sub)
                },
                onEditSub = { sub ->
                    subToEdit = sub
                    showAddDialog = true
                },
                onDeleteSubById = { subId ->
                    val wasCurrent = subId == currentSub?.id
                    DevkanVault.deleteSubscriptionById(context, subId)
                    if (wasCurrent) {
                        val nextSub = DevkanVault.getSavedSubscription(context)
                        currentSub = nextSub
                        if (nextSub == null) {
                            allItems.clear()
                            cachedLiveChannels = emptyList()
                            cachedSportsChannels = emptyList()
                            todayRealMatches.clear()
                            showConnectionsManager = false
                        } else {
                            switchSubscriptionFast(nextSub)
                        }
                    }
                },
                onOpenAddDialog = {
                    subToEdit = null
                    showAddDialog = true
                },
                onCategoriesChanged = { categoriesRevision++ },
                onBack = { showConnectionsManager = false }
            )
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
                        isSportsPlayback = isSmartSportChannel(found.group, found.title)
                        activeSeriesTracking = null
                        activeStream = found
                    }
                },
                onClose = { showEpgSheet = false }
            )
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
                    isSportsPlayback = isSmartSportChannel(nextCh.group, nextCh.title)
                    activeSeriesTracking = null
                    activeStream = nextCh
                },
                lang = lang,
                onBack = { handleClosePlayer() }
            )
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
                                                isSportsPlayback = isSmartSportChannel(item.group, item.title)
                                                activeSeriesTracking = null
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

    selectedMatchForChannelPicker?.let { match ->
        var pickerQuery by remember(match.id) { mutableStateOf("") }
        val matchingChannels = remember(cachedSportsChannels, match.id, pickerQuery) {
            val q = pickerQuery.trim()
            if (q.isNotEmpty()) {
                cachedSportsChannels.filter {
                    it.title.contains(q, ignoreCase = true) || it.group.contains(q, ignoreCase = true)
                }.take(25)
            } else {
                val broadcasterKeyword = match.suggestedBroadcaster.substringBefore(" ").lowercase()
                val alwanList = cachedSportsChannels.filter {
                    val t = "${it.group} ${it.title}".lowercase()
                    t.contains("alwan") || t.contains("ألوان") || t.contains("الوان")
                }
                val primaryList = cachedSportsChannels.filter {
                    val t = "${it.group} ${it.title}".lowercase()
                    t.contains(broadcasterKeyword)
                }
                (alwanList + primaryList + cachedSportsChannels).distinctBy { it.id }.take(20)
            }
        }

        AlertDialog(
            onDismissRequest = { selectedMatchForChannelPicker = null },
            containerColor = Color(0xFF14141D),
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${match.homeTeam} vs ${match.awayTeam}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    OutlinedTextField(
                        value = pickerQuery,
                        onValueChange = { pickerQuery = it },
                        placeholder = { Text("ابحث عن القناة الناقلة في اشتراكك...", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            text = {
                if (matchingChannels.isEmpty()) {
                    Text("لم يتم العثور على قنوات مطابقة في اشتراكك.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(matchingChannels, key = { it.id }) { ch ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E1E2B))
                                    .clickable {
                                        selectedMatchForChannelPicker = null
                                        isSportsPlayback = true
                                        activeSeriesTracking = null
                                        activeStream = ch.copy(title = "${match.homeTeam} vs ${match.awayTeam} • ${ch.title}")
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ch.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(ch.group, color = Color.Gray, fontSize = 10.5.sp, maxLines = 1)
                                }
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
                // إغلاق النافذة فوراً والانتقال للواجهة الرئيسية مع شريط التقدم المتدرج
                showAddDialog = false
                subToEdit = null
                showConnectionsManager = false
                currentSub = newSub
                DevkanVault.saveSubscription(context, newSub)
                startProgressiveSubscriptionLoad(newSub, isNewConnection = true)
            }
        )
    }
}
