package com.hadedo.novaiptv

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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DevkanAppRoot()
        }
    }
}

// ============================================================================
// نماذج البيانات والترجمة متعددة اللغات (عربي + إنجليزي + ألماني + فرنسي + تركي + كردي)
// ============================================================================
data class AppStrings(
    val home: String,
    val live: String,
    val sports: String,
    val cinema: String,
    val settings: String,
    val continueWatching: String,
    val recentlyWatched: String,
    val myFolders: String,
    val searchHint: String,
    val all: String,
    val movies: String,
    val series: String,
    val addPlaylist: String,
    val importFile: String,
    val epgGuide: String,
    val speedTest: String,
    val engineTitle: String,
    val appLockTitle: String,
    val watchlist: String
)

fun getStrings(lang: String): AppStrings = when (lang) {
    "en" -> AppStrings("Home", "Live TV", "Sports", "Cinema", "Settings", "Continue Watching", "Recently Watched", "My Folders", "Search channels or movies...", "All", "Movies", "Series", "Add Playlist", "Import M3U File", "EPG Grid", "Server Speed & Ping", "Playback Engine", "App Lock PIN", "Watchlist")
    "de" -> AppStrings("Start", "Live-TV", "Sport", "Kino", "Optionen", "Weiterschauen", "Zuletzt gesehen", "Ordner", "Kanäle oder Filme suchen...", "Alle", "Filme", "Serien", "Abo hinzufügen", "M3U-Datei laden", "EPG-Plan", "Server-Speedtest", "Wiedergabe-Engine", "App-PIN-Sperre", "Merkliste")
    "fr" -> AppStrings("Accueil", "Direct", "Sports", "Cinéma", "Réglages", "Reprendre", "Récents", "Dossiers", "Rechercher...", "Tout", "Films", "Séries", "Ajouter Abo", "Fichier M3U", "Guide EPG", "Test Serveur", "Moteur Vidéo", "Code PIN App", "À voir")
    "tr" -> AppStrings("Ana Sayfa", "Canlı", "Spor", "Sinema", "Ayarlar", "Devam Et", "Son İzlenen", "Klasörler", "Kanal veya film ara...", "Tümü", "Filmler", "Diziler", "Abonelik Ekle", "M3U Dosyası", "EPG Rehberi", "Sunucu Hız Testi", "Oynatıcı Motoru", "Uygulama PIN", "İzleme Listesi")
    "ku" -> AppStrings("سەرەکی", "ڕاستەوخۆ", "وەرزش", "سینەما", "ڕێکخستن", "بەردەوامبە", "دوایینەکان", "فۆڵدەر", "گەڕان...", "هەموو", "فیلم", "زنجیرە", "زیادکردن", "فایلی M3U", "خشتەی EPG", "پشکنینی خێرایی", "بزوێنەری کارپێکردن", "قفڵی بەرنامە", "لیستی بینین")
    else -> AppStrings("الرئيسية", "مباشر", "الرياضة", "السينما", "الإعدادات", "واصل المشاهدة", "شاهدته مؤخراً", "مجلداتي", "ابحث عن قناة أو فيلم...", "الكل", "أفلام", "مسلسلات", "إضافة اشتراك", "استيراد ملف M3U", "جدول EPG", "فحص سرعة السيرفر", "محرك التشغيل", "قفل التطبيق PIN", "المشاهدة لاحقاً")
}

data class MediaItemModel(
    val id: String,
    val title: String,
    val url: String,
    val group: String,
    val logo: String = "",
    val isVod: Boolean = false
)

// ============================================================================
// الجذر الرئيسي للتطبيق (Disclaimer -> App Lock -> Main App / Hybrid Player)
// ============================================================================
@Composable
fun DevkanAppRoot() {
    val context = LocalContext.current
    var lang by remember { mutableStateOf("ar") }
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
                    DevkanMainDashboard(lang = lang, onLanguageChange = { lang = it })
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
                Text(
                    text = "DEVKAN 34",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .background(goldMid.copy(alpha = 0.22f), RoundedCornerShape(5.dp))
                        .border(1.dp, goldMid.copy(alpha = 0.85f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(text = "VIP", color = goldLight, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(
                onClick = onImportClick,
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFF161620), CircleShape)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = strings.importFile, tint = goldLight, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(34.dp)
                    .background(goldMid, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.addPlaylist, tint = Color.Black, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ============================================================================
// اللوحة الرئيسية للتطبيق (5 تبويبات + المشغل الهجين + EPG + الإعدادات المتقدمة)
// ============================================================================
@Composable
fun DevkanMainDashboard(lang: String, onLanguageChange: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = remember(lang) { getStrings(lang) }
    val gold = Color(0xFFD4AF37)

    var selectedTab by remember { mutableIntStateOf(0) }
    var activeStream by remember { mutableStateOf<MediaItemModel?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEpgSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }

    // قائمة القنوات والأفلام المحملة
    val channelsList = remember {
        mutableStateListOf(
            MediaItemModel("ch_1", "beIN SPORTS 1 HD", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "Sports"),
            MediaItemModel("ch_2", "beIN SPORTS 2 HD", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "Sports"),
            MediaItemModel("ch_3", "MBC 1 HD", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "Arabic"),
            MediaItemModel("ch_4", "National Geographic", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "Documentary"),
            MediaItemModel("vod_1", "Dune: Part Two (4K)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "Movies", isVod = true),
            MediaItemModel("vod_2", "House of the Dragon S02", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", "Series", isVod = true)
        )
    }

    // مستورد ملفات M3U المحلية من الهاتف
    val m3uFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val imported = LocalPlaylistImporter.parseUri(context, uri)
                if (imported.isNotEmpty()) {
                    channelsList.clear()
                    imported.forEachIndexed { idx, item ->
                        channelsList.add(
                            MediaItemModel(
                                id = "local_$idx",
                                title = item.name,
                                url = item.url,
                                group = item.group,
                                logo = item.logo,
                                isVod = item.url.endsWith(".mp4") || item.url.endsWith(".mkv")
                            )
                        )
                    }
                    Toast.makeText(context, "تم استيراد ${imported.size} قناة بنجاح!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "تعذر قراءة الملف أو الملف فارغ", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // فتح المشغل الهجين بملء الشاشة عند اختيار أي قناة أو فيلم
    if (activeStream != null) {
        DevkanHybridPlayerScreen(
            streamUrl = activeStream!!.url,
            streamTitle = activeStream!!.title,
            isVodOrSeries = activeStream!!.isVod,
            streamId = activeStream!!.id,
            lang = lang,
            onBack = { activeStream = null }
        )
        return
    }

    // عرض جدول البرامج الزمني (EPG Timeline Grid)
    if (showEpgSheet) {
        val sampleEpg = channelsList.filter { !it.isVod }.mapIndexed { idx, ch ->
            EpgSlot(
                channelName = ch.title,
                channelLogo = ch.logo,
                currentProgram = if (idx % 2 == 0) "البث المباشر - الاستوديو التحليلي" else "نشرة الأخبار والتغطية الخاصة",
                nextProgram = "البرنامج المسائي المباشر",
                timeRange = "20:00 - 22:00",
                progress = 0.65f
            )
        }
        EpgTimelineGridSheet(
            slots = sampleEpg,
            lang = lang,
            onPlayChannel = { chName ->
                val found = channelsList.firstOrNull { it.title == chName }
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
            // شريط تنقل سفلي محسن بسطر واحد لا ينكسر أبداً في أي شاشة
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                // 0: الرئيسية (متابعة آخر قناة + شريط البحث + القنوات المميزة)
                0 -> {
                    val lastCh = remember { DevkanVault.getLastChannel(context) }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // بطاقة الاستئناف الفوري لآخر قناة
                        if (lastCh != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.horizontalGradient(listOf(Color(0xFF1F1A0D), Color(0xFF14141C))))
                                    .border(1.dp, gold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        activeStream = MediaItemModel("last", lastCh.second, lastCh.first, "Last")
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(strings.continueWatching, color = gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(lastCh.second, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = gold, modifier = Modifier.size(36.dp))
                            }
                        }

                        // شريط البحث السريع
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gold) },
                            placeholder = { Text(strings.searchHint, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Text(strings.recentlyWatched, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(channelsList.filter { it.title.contains(searchQuery, ignoreCase = true) }) { item ->
                                Column(
                                    modifier = Modifier
                                        .width(145.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF13131B))
                                        .border(1.dp, Color(0xFF232332), RoundedCornerShape(14.dp))
                                        .clickable { activeStream = item }
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isVod) Icons.Default.Movie else Icons.Default.LiveTv,
                                        contentDescription = null,
                                        tint = gold
                                    )
                                    Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.group, color = Color(0xFF8E929F), fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // 1: البث المباشر (مع زر جدول البرامج EPG Grid وفلترة الباقات)
                1 -> {
                    val liveChannels = channelsList.filter { !it.isVod }
                    val groups = listOf("ALL") + liveChannels.map { it.group }.distinct()
                    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.live, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
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

                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            groups.forEach { grp ->
                                FilterChip(
                                    selected = selectedCategory == grp,
                                    onClick = { selectedCategory = grp },
                                    label = { Text(if (grp == "ALL") strings.all else grp, fontSize = 12.sp) }
                                )
                            }
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val filtered = liveChannels.filter { selectedCategory == "ALL" || it.group == selectedCategory }
                            items(filtered) { ch ->
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
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (ch.logo.isNotEmpty()) {
                                            AsyncImage(model = ch.logo, contentDescription = null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)))
                                        } else {
                                            Icon(Icons.Default.Tv, contentDescription = null, tint = gold, modifier = Modifier.size(30.dp))
                                        }
                                        Column {
                                            Text(ch.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(ch.group, color = Color(0xFF8E929F), fontSize = 11.sp)
                                        }
                                    }
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = gold)
                                }
                            }
                        }
                    }
                }

                // 2: مركز المباريات المباشر مع زر التذكير (🔔)
                2 -> {
                    val matches = listOf(
                        Triple("Real Madrid vs Barcelona", "22:00 - LaLiga", "beIN SPORTS 1 HD"),
                        Triple("Liverpool vs Arsenal", "20:30 - Premier League", "beIN SPORTS 2 HD"),
                        Triple("Iraq vs Jordan", "19:15 - World Cup Qualifiers", "beIN SPORTS 1 HD")
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(strings.sports, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                        items(matches) { (matchTitle, timeInfo, channelName) ->
                            val isReminded = MatchReminderManager.remindedMatches.contains(matchTitle)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF13131B))
                                    .border(1.dp, Color(0xFF252536), RoundedCornerShape(16.dp))
                                    .clickable {
                                        val ch = channelsList.firstOrNull { it.title.contains("beIN") } ?: channelsList.first()
                                        activeStream = ch.copy(title = "$matchTitle ($channelName)")
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(matchTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("$timeInfo • $channelName", color = gold, fontSize = 12.sp)
                                }
                                IconButton(onClick = {
                                    val added = MatchReminderManager.toggleReminder(matchTitle)
                                    Toast.makeText(
                                        context,
                                        if (added) "تم تفعيل التذكير للمباراة 🔔" else "تم إلغاء التذكير",
                                        Toast.LENGTH_SHORT
                                    ).show()
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
                }

                // 3: السينما (الأفلام والمسلسلات + قائمة المشاهدة لاحقاً Watchlist)
                3 -> {
                    val vodItems = channelsList.filter { it.isVod }
                    var watchlistState by remember { mutableStateOf(DevkanVault.getWatchlist(context)) }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(vodItems) { item ->
                            val inWatchlist = watchlistState.contains(item.id)
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF13131B))
                                    .border(1.dp, Color(0xFF232332), RoundedCornerShape(16.dp))
                                    .clickable { activeStream = item }
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Icon(Icons.Default.MovieFilter, contentDescription = null, tint = gold)
                                    IconButton(
                                        onClick = {
                                            DevkanVault.toggleWatchlist(context, item.id)
                                            watchlistState = DevkanVault.getWatchlist(context)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (inWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = strings.watchlist,
                                            tint = gold
                                        )
                                    }
                                }
                                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2)
                                Text(item.group, color = Color(0xFF8E929F), fontSize = 11.sp)
                            }
                        }
                    }
                }

                // 4: الإعدادات المتقدمة (اللغة الألمانية + المحرك الهجين + فحص سرعة السيرفر + قفل PIN)
                4 -> {
                    var selectedEngine by remember { mutableStateOf(DevkanVault.getPreferredEngine(context)) }
                    var pinInput by remember { mutableStateOf(DevkanVault.getAppLockPin(context)) }
                    var speedResult by remember { mutableStateOf<String?>(null) }
                    var testingSpeed by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // اختيار اللغة (بما فيها الألمانية Deutsch)
                        Text("اللغة / Language / Sprache", color = gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ar" to "العربية", "en" to "English", "de" to "Deutsch", "fr" to "Français", "tr" to "Türkçe", "ku" to "کوردی").forEach { (code, name) ->
                                FilterChip(
                                    selected = lang == code,
                                    onClick = { onLanguageChange(code) },
                                    label = { Text(name) }
                                )
                            }
                        }

                        // اختيار محرك التشغيل (Hybrid Media3 + LibVLC)
                        Text(strings.engineTitle, color = gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        EngineType.entries.forEach { engine ->
                            val label = when (lang) {
                                "en" -> engine.labelEn
                                "de" -> engine.labelDe
                                else -> engine.labelAr
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF13131B))
                                    .clickable {
                                        selectedEngine = engine
                                        DevkanVault.savePreferredEngine(context, engine)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, color = Color.White, fontSize = 13.sp)
                                RadioButton(selected = selectedEngine == engine, onClick = null)
                            }
                        }

                        // أداة فحص سرعة السيرفر والـ Ping الفعلي
                        Button(
                            onClick = {
                                testingSpeed = true
                                scope.launch {
                                    val testUrl = channelsList.firstOrNull()?.url ?: "https://www.google.com"
                                    val (ping, mbps) = EngineCoordinator.runServerSpeedAndPingTest(testUrl)
                                    speedResult = "Ping: ${ping} ms  |  Speed: ${mbps} Mbps"
                                    testingSpeed = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = gold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (testingSpeed) "جاري فحص السيرفر..." else strings.speedTest,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        speedResult?.let {
                            Text(it, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // إعداد قفل التطبيق برمز PIN
                        Text(strings.appLockTitle, color = gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { if (it.length <= 6) pinInput = it },
                                placeholder = { Text("4-6 digits (فارغ = معطل)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    DevkanVault.setAppLockPin(context, pinInput)
                                    Toast.makeText(context, if (pinInput.isEmpty()) "تم إيقاف القفل" else "تم حفظ رمز القفل بنجاح", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = gold)
                            ) {
                                Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // نافذة إضافة اشتراك (Xtream Codes أو رابط M3U)
    if (showAddDialog) {
        var host by remember { mutableStateOf("") }
        var user by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }
        var m3uDirect by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.addPlaylist, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Server URL (http://...)") }, singleLine = true)
                    OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, singleLine = true)
                    OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, singleLine = true)
                    HorizontalDivider()
                    OutlinedTextField(value = m3uDirect, onValueChange = { m3uDirect = it }, label = { Text("أو رابط M3U مباشر") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    enabled = !isLoading,
                    onClick = {
                        isLoading = true
                        scope.launch {
                            val loaded = fetchPlaylistFromNetwork(host.trim(), user.trim(), pass.trim(), m3uDirect.trim())
                            if (loaded.isNotEmpty()) {
                                channelsList.clear()
                                channelsList.addAll(loaded)
                                Toast.makeText(context, "تم تحميل ${loaded.size} قناة!", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                            } else {
                                Toast.makeText(context, "تعذر الاتصال بالسيرفر، تأكد من البيانات", Toast.LENGTH_SHORT).show()
                            }
                            isLoading = false
                        }
                    }
                ) {
                    Text(if (isLoading) "جاري التحميل..." else "اتصال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

// دالة جلب القنوات من Xtream Codes API أو رابط M3U
suspend fun fetchPlaylistFromNetwork(
    host: String,
    user: String,
    pass: String,
    m3uUrl: String
): List<MediaItemModel> = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    val result = mutableListOf<MediaItemModel>()
    try {
        if (host.isNotEmpty() && user.isNotEmpty()) {
            val cleanHost = host.trimEnd('/')
            val api = "$cleanHost/player_api.php?username=${URLEncoder.encode(user, "UTF-8")}&password=${URLEncoder.encode(pass, "UTF-8")}&action=get_live_streams"
            val req = Request.Builder().url(api).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: "[]"
                val arr = JSONArray(body)
                for (i in 0 until minOf(arr.length(), 1500)) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("stream_id")
                    val name = obj.optString("name", "Channel $i")
                    val icon = obj.optString("stream_icon", "")
                    val cat = obj.optString("category_id", "Live")
                    val streamUrl = "$cleanHost/live/$user/$pass/$id.m3u8"
                    result.add(MediaItemModel(id, name, streamUrl, "Group $cat", icon))
                }
            }
        } else if (m3uUrl.isNotEmpty()) {
            val req = Request.Builder().url(m3uUrl).build()
            client.newCall(req).execute().use { resp ->
                val lines = (resp.body?.string() ?: "").lines()
                var name = "Channel"
                var group = "M3U"
                var logo = ""
                lines.forEachIndexed { idx, line ->
                    val t = line.trim()
                    if (t.startsWith("#EXTINF:")) {
                        name = t.substringAfterLast(",").trim()
                        group = Regex("""group-title="([^"]*)"""").find(t)?.groupValues?.get(1)?.ifEmpty { "M3U" } ?: "M3U"
                        logo = Regex("""tvg-logo="([^"]*)"""").find(t)?.groupValues?.get(1) ?: ""
                    } else if (t.startsWith("http")) {
                        result.add(MediaItemModel("m3u_$idx", name, t, group, logo))
                    }
                }
            }
        }
    } catch (_: Exception) {
    }
    result
}
