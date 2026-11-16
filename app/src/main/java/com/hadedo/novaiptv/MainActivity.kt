package com.hadedo.novaiptv

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DevkanAppRoot()
        }
    }
}

// ============================================================================
// الترجمة متعددة اللغات
// ============================================================================
data class AppStrings(
    val home: String,
    val live: String,
    val sports: String,
    val cinema: String,
    val settings: String,
    val continueWatching: String,
    val recommendedForYou: String,
    val searchHint: String,
    val all: String,
    val moviesTab: String,
    val seriesTab: String,
    val addConnection: String,
    val importFile: String,
    val epgGuide: String,
    val speedTest: String,
    val engineTitle: String,
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
    "en" -> AppStrings("Home", "Live TV", "Sports", "Cinema", "Settings", "Continue Watching", "Recommended For You", "Search channels, movies, series...", "All", "Movies", "Series", "Add Connection", "Import M3U File", "EPG Grid", "Server Speed & Ping", "Playback Engine", "App Lock PIN", "Subscription Details", "Status", "Expires", "Allowed Region", "Connections", "Ultra-Low Latency (Satellite Sync)", "Smart Geo-Bypass Headers", "Auto-Resume Last Channel")
    "de" -> AppStrings("Start", "Live-TV", "Sport", "Kino", "Optionen", "Weiterschauen", "Empfohlen für dich", "Kanäle, Filme, Serien suchen...", "Alle", "Filme", "Serien", "Verbindung hinzufügen", "M3U-Datei laden", "EPG-Plan", "Server-Speedtest", "Wiedergabe-Engine", "App-PIN-Sperre", "Abonnement-Details", "Status", "Ablaufdatum", "Region / Land", "Verbindungen", "Geringe Latenz (Satelliten-Sync)", "Geo-Bypass-Header", "Letzten Kanal fortsetzen")
    "tr" -> AppStrings("Ana Sayfa", "Canlı", "Spor", "Sinema", "Ayarlar", "Devam Et", "Sizin İçin Önerilenler", "Kanal, film, dizi ara...", "Tümü", "Filmler", "Diziler", "Bağlantı Ekle", "M3U Dosyası", "EPG Rehberi", "Sunucu Hız Testi", "Oynatıcı Motoru", "Uygulama PIN", "Abonelik Bilgileri", "Durum", "Bitiş Tarihi", "Ülke / Bölge", "Bağlantılar", "Düşük Gecikme Modu", "Bölge Kısıtlama Aşımı", "Son Kanalı Otomatik Aç")
    "fr" -> AppStrings("Accueil", "Direct", "Sports", "Cinéma", "Réglages", "Reprendre", "Recommandé", "Rechercher...", "Tout", "Films", "Séries", "Ajouter Connexion", "Fichier M3U", "Guide EPG", "Test Serveur", "Moteur Vidéo", "Code PIN App", "Abonnement", "Statut", "Expiration", "Pays Autorisé", "Connexions", "Faible Latence", "Contournement Géo", "Reprise Auto")
    "ku" -> AppStrings("سەرەکی", "ڕاستەوخۆ", "وەرزش", "سینەما", "ڕێکخستن", "بەردەوامبە", "پێشنیارکراو", "گەڕان...", "هەموو", "فیلم", "زنجیرە", "زیادکردنی بەشداری", "فایلی M3U", "خشتەی EPG", "پشکنینی خێرایی", "بزوێنەری کارپێکردن", "قفڵی بەرنامە", "زانیاری بەشداری", "دۆخ", "بەرواری بەسەرچوون", "وڵات", "پەیوەندییەکان", "کەمکردنەوەی دواکەوتن", "کردنەوەی بلۆکی وڵات", "کردنەوەی دوایین کەناڵ")
    else -> AppStrings("الرئيسية", "مباشر", "الرياضة", "السينما", "الإعدادات", "واصل المشاهدة", "ننصحك بمشاهدته اليوم", "ابحث عن قناة أو فيلم أو مسلسل...", "الكل", "الأفلام", "المسلسلات", "إضافة اتصال", "استيراد ملف M3U", "جدول EPG", "فحص سرعة السيرفر", "محرك التشغيل", "قفل التطبيق PIN", "بطاقة الاشتراك", "الحالة", "تاريخ الانتهاء", "الدولة / النطاق", "الاتصالات", "تقليل تأخير البث عن الستلايت", "تجاوز قيود السيرفر والدولة", "فتح آخر قناة تلقائياً عند التشغيل")
}

// ============================================================================
// الجذر الرئيسي للتطبيق
// ============================================================================
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

// ============================================================================
// الشعار الملكي الداخلي DEVKAN 34 VIP
// ============================================================================
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

// ============================================================================
// اللوحة الرئيسية للتطبيق
// ============================================================================
@Composable
fun DevkanMainDashboard(lang: String, onLanguageChange: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = remember(lang) { getStrings(lang) }
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    var selectedTab by remember { mutableIntStateOf(0) }
    var activeStream by remember { mutableStateOf<MediaItemModel?>(null) }
    var selectedMovieForDetails by remember { mutableStateOf<MediaItemModel?>(null) }
    var selectedSeriesForDetails by remember { mutableStateOf<MediaItemModel?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEpgSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedLiveCategory by remember { mutableStateOf("ALL") }
    var isSyncing by remember { mutableStateOf(false) }

    var currentSub by remember { mutableStateOf(DevkanVault.getSavedSubscription(context)) }
    val allItems = remember { mutableStateListOf<MediaItemModel>() }
    val todayRealMatches = remember { mutableStateListOf<RealSportsMatch>() }

    // تحميل فوري من الذاكرة المحلية في 0.1 ثانية + جلب جدول المباريات الحقيقي لليوم
    LaunchedEffect(Unit) {
        val cached = DevkanVault.getCachedItems(context)
        if (cached.isNotEmpty()) {
            allItems.clear()
            allItems.addAll(cached)
        } else {
            val saved = DevkanVault.getSavedSubscription(context)
            if (saved != null) {
                isSyncing = true
                val geoBypass = DevkanVault.isGeoBypassEnabled(context)
                val (updatedSub, loadedItems) = DevkanPlaylistRepository.loadSubscriptionContent(saved, geoBypass)
                currentSub = updatedSub
                DevkanVault.saveSubscription(context, updatedSub)
                if (loadedItems.isNotEmpty()) {
                    allItems.clear()
                    allItems.addAll(loadedItems)
                    DevkanVault.saveCachedItems(context, loadedItems)
                }
                isSyncing = false
            }
        }

        // جلب مباريات اليوم الحقيقية من المصدر الرياضي
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
                    DevkanVault.saveCachedItems(context, imported)
                    Toast.makeText(context, "تم استيراد وحفظ ${imported.size} عنصر!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 1. المشغل بملء الشاشة
    if (activeStream != null) {
        DevkanHybridPlayerScreen(
            streamUrl = activeStream!!.url,
            fallbackUrl = activeStream!!.fallbackUrl,
            streamTitle = activeStream!!.title,
            isVodOrSeries = activeStream!!.contentType != "LIVE",
            streamId = activeStream!!.id,
            lang = lang,
            onBack = { activeStream = null }
        )
        return
    }

    // 2. صفحة تفاصيل الفيلم قبل التشغيل (التقييم + سنة الإنتاج + القصة + الممثلين)
    if (selectedMovieForDetails != null) {
        MovieDetailsScreen(
            movie = selectedMovieForDetails!!,
            subscription = currentSub,
            onPlayMovie = { item ->
                selectedMovieForDetails = null
                activeStream = item
            },
            onBack = { selectedMovieForDetails = null }
        )
        return
    }

    // 3. صفحة تفاصيل المسلسل (المواسم + الحلقات + خط تقدم المشاهدة الذهبي)
    if (selectedSeriesForDetails != null) {
        SeriesDetailsScreen(
            series = selectedSeriesForDetails!!,
            subscription = currentSub,
            onPlayEpisode = { epItem ->
                activeStream = epItem
            },
            onBack = { selectedSeriesForDetails = null }
        )
        return
    }

    // 4. دليل البرامج الزمني EPG
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
                if (found != null) activeStream = found
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
                // 0: الشاشة الرئيسية الذكية (تنبيهات المباريات الحقيقية + ترشيحات الأفلام والمسلسلات)
                0 -> {
                    val lastCh = remember { DevkanVault.getLastChannel(context) }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // بطاقة تنبيه المباريات الحية القادمة من المصدر الرياضي الحقيقي
                        if (todayRealMatches.isNotEmpty()) {
                            val topMatch = todayRealMatches.first()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.horizontalGradient(listOf(Color(0xFF1A1429), Color(0xFF0F172A))))
                                    .border(1.dp, gold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                    .clickable { selectedTab = 2 }
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = goldLight, modifier = Modifier.size(18.dp))
                                        Text("تنبيه مباراة هامة اليوم • ${topMatch.league}", color = goldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(topMatch.matchTime, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text(
                                    text = "${topMatch.homeTeam}  🆚  ${topMatch.awayTeam}",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Text("اضغط هنا للانتقال لجدول المباريات وقنوات البث المباشر ←", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                            }
                        }

                        // بطاقة الاستئناف الفوري لآخر قناة
                        if (lastCh != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.horizontalGradient(listOf(Color(0xFF1F1A0D), Color(0xFF14141C))))
                                    .border(1.dp, gold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        activeStream = MediaItemModel("last", "0", lastCh.second, lastCh.first, "", "Live", "", "LIVE")
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(strings.continueWatching, color = gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(lastCh.second, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = gold, modifier = Modifier.size(34.dp))
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
                                Button(
                                    onClick = { showAddDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = gold)
                                ) {
                                    Text(strings.addConnection, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gold) },
                                placeholder = { Text(strings.searchHint, fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            // ترشيحات ذكية من الأفلام والمسلسلات مع تصنيف النوع (مسلسل تاريخي / دراما كورية / فيلم أكشن)
                            val recommendedVod = remember(allItems.size, searchQuery) {
                                allItems.filter {
                                    it.contentType != "LIVE" &&
                                        (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true))
                                }.take(20)
                            }
                            if (recommendedVod.isNotEmpty()) {
                                Text(strings.recommendedForYou, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(recommendedVod, key = { it.id }) { item ->
                                        val typeLabel = if (item.contentType == "SERIES") "مسلسل ${item.genreTag} • ${item.originTag}" else "فيلم ${item.genreTag} • ${item.originTag}"
                                        Column(
                                            modifier = Modifier
                                                .width(155.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF13131B))
                                                .border(1.dp, Color(0xFF232332), RoundedCornerShape(14.dp))
                                                .clickable {
                                                    if (item.contentType == "SERIES") selectedSeriesForDetails = item
                                                    else selectedMovieForDetails = item
                                                }
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (item.logo.isNotEmpty()) {
                                                AsyncImage(
                                                    model = item.logo,
                                                    contentDescription = item.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxWidth().height(125.dp).clip(RoundedCornerShape(10.dp))
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(gold.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(typeLabel, color = goldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            }
                                            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
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

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredLive, key = { it.id }) { ch ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF13131B))
                                        .clickable { activeStream = ch }
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

                // 2: الرياضة (جدول المباريات الحقيقي اليوم + القنوات الرياضية)
                2 -> {
                    val sportChannels = remember(allItems.size) {
                        allItems.filter {
                            it.contentType == "LIVE" && (
                                it.group.contains("sport", true) ||
                                    it.group.contains("bein", true) ||
                                    it.group.contains("رياض", true) ||
                                    it.title.contains("sport", true) ||
                                    it.title.contains("bein", true)
                                )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (todayRealMatches.isNotEmpty()) {
                            item {
                                Text("مباريات اليوم المباشرة (تحديث حي)", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            items(todayRealMatches, key = { it.id }) { match ->
                                val title = "${match.homeTeam} vs ${match.awayTeam}"
                                val isReminded = MatchReminderManager.remindedMatches.contains(title)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF151522))
                                        .border(1.dp, Color(0xFF28283E), RoundedCornerShape(14.dp))
                                        .clickable {
                                            val firstSport = sportChannels.firstOrNull()
                                            if (firstSport != null) {
                                                activeStream = firstSport.copy(title = "$title (${firstSport.title})")
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${match.league}  •  ${match.matchTime}", color = gold, fontSize = 11.sp)
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

                        item {
                            Text("القنوات الرياضية (${sportChannels.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        items(sportChannels, key = { it.id }) { ch ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF13131B))
                                    .clickable { activeStream = ch }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(ch.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = gold)
                            }
                        }
                    }
                }

                // 3: السينما (قسم الأفلام + قسم المسلسلات + تصنيف عربي / كوري / هندي / أجنبي)
                3 -> {
                    var cinemaSubTab by remember { mutableStateOf("MOVIE") } // "MOVIE" or "SERIES"
                    var selectedOrigin by remember { mutableStateOf("ALL") } // "ALL", "عربي", "كوري", "هندي", "أجنبي"

                    val cinemaItems = remember(allItems.size, cinemaSubTab, selectedOrigin) {
                        allItems.filter {
                            it.contentType == cinemaSubTab &&
                                (selectedOrigin == "ALL" || it.originTag == selectedOrigin)
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // أزرار التبديل بين قسم الأفلام وقسم المسلسلات
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

                        // فلاتر التصنيف: الكل / عربي / كوري / هندي / أجنبي
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
                                                modifier = Modifier.fillMaxWidth().height(155.dp).clip(RoundedCornerShape(12.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().height(145.dp).background(Color(0xFF1D1D29), RoundedCornerShape(12.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.MovieFilter, contentDescription = null, tint = gold, modifier = Modifier.size(36.dp))
                                            }
                                        }
                                        // شارة التصنيف (كوري / عربي / هندي) + التقييم
                                        Row(
                                            modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(item.originTag, color = goldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            if (item.rating.isNotEmpty() && item.rating != "0") {
                                                Box(
                                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("⭐ ${item.rating}", color = Color.White, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }

                                    // خط تقدم المشاهدة الذهبي إذا سبق وبدأت بمشاهدته
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
                                        text = if (item.year.isNotEmpty()) "${item.year} • ${item.group}" else item.group,
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
                                    DevkanVault.saveCachedItems(context, items)
                                }
                                isSyncing = false
                                Toast.makeText(context, "تم تحديث البيانات وحفظها محلياً!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteSub = {
                            DevkanVault.clearSubscription(context)
                            currentSub = null
                            allItems.clear()
                            Toast.makeText(context, "تم حذف الاشتراك", Toast.LENGTH_SHORT).show()
                        },
                        onOpenAddDialog = { showAddDialog = true },
                        sampleTestUrl = allItems.firstOrNull()?.url ?: "https://www.google.com"
                    )
                }
            }
        }
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
                        DevkanVault.saveCachedItems(context, loadedItems)
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

// ============================================================================
// صفحة تفاصيل الفيلم الاحترافية قبل التشغيل (التقييم + سنة الإنتاج + القصة + الممثلين)
// ============================================================================
@Composable
fun MovieDetailsScreen(
    movie: MediaItemModel,
    subscription: SavedSubscription?,
    onPlayMovie: (MediaItemModel) -> Unit,
    onBack: () -> Unit
) {
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
                    modifier = Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(18.dp))
                )
            }

            Text(info.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(gold.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("⭐ ${info.rating}", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text("سنة الإنتاج: ${info.releaseDate}", color = Color(0xFFB0B5C0), fontSize = 12.sp)
                Text("المدة: ${info.duration}", color = Color(0xFFB0B5C0), fontSize = 12.sp)
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

            Button(
                onClick = { onPlayMovie(movie) },
                colors = ButtonDefaults.buttonColors(containerColor = gold),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (savedProgress > 0.02f) "إكمال مشاهدة الفيلم" else "تشغيل الفيلم الآن",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            Text("القصة:", color = gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(info.plot, color = Color(0xFFD1D5DB), fontSize = 13.sp, lineHeight = 20.sp)

            Text("الممثلون: ${info.cast}", color = Color(0xFF9CA3AF), fontSize = 12.sp)
            Text("المخرج: ${info.director}", color = Color(0xFF9CA3AF), fontSize = 12.sp)
        }
    }
}

// ============================================================================
// صفحة تفاصيل المسلسل (تبديل المواسم بسلاسة + قائمة الحلقات المرقمة + خط المشاهدة)
// ============================================================================
@Composable
fun SeriesDetailsScreen(
    series: MediaItemModel,
    subscription: SavedSubscription?,
    onPlayEpisode: (MediaItemModel) -> Unit,
    onBack: () -> Unit
) {
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
                    Text("⭐ ${info.rating}  •  ${info.releaseDate}", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(info.plot, color = Color(0xFFD1D5DB), fontSize = 12.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    Text("الممثلون: ${info.cast}", color = Color(0xFF9CA3AF), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            // أزرار التبديل السلس بين المواسم (Season 1, Season 2...)
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

            // قائمة الحلقات المرقمة مع شريط تقدم المشاهدة الواضح لكل حلقة
            val episodes = info.seasons[selectedSeason] ?: emptyList()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(episodes, key = { it.id }) { ep ->
                    val progress = remember(ep.id) { DevkanVault.getVodProgressFraction(context, ep.id) }
                    val statusText = when {
                        progress >= 0.90f -> "مكتملة ✓"
                        progress >= 0.45f -> "وصلت إلى منتصف الحلقة (${(progress * 100).toInt()}%)"
                        progress >= 0.15f -> "وصلت إلى ربع الحلقة (${(progress * 100).toInt()}%)"
                        progress > 0.02f -> "بدأت المشاهدة (${(progress * 100).toInt()}%)"
                        else -> "لم تُشاهد بعد"
                    }

                    Column(
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
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).background(gold.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${ep.episodeNum}", color = goldLight, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ep.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(statusText, color = if (progress > 0.02f) goldLight else Color.Gray, fontSize = 11.sp)
                                }
                            }
                            Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = gold, modifier = Modifier.size(26.dp))
                        }

                        // خط تقدم المشاهدة الذهبي أسفل الحلقة
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

// ============================================================================
// شاشة الإعدادات المنظمة + نافذة إضافة الاتصال الذكية
// ============================================================================
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

    var selectedEngine by remember { mutableStateOf(DevkanVault.getPreferredEngine(context)) }
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
                        Text("تحديث", color = gold, fontSize = 12.sp)
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
