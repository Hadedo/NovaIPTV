package com.hadedo.novaiptv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
fun DevkanSplashIntroScreen(onFinished: () -> Unit) {
    val goldLight = Color(0xFFFDE047)
    val goldMid = Color(0xFFD4AF37)
    val goldDark = Color(0xFFB45309)

    var startAnim by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.78f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "logoScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = LinearEasing),
        label = "contentAlpha"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(250L)
        onFinished()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1A150B), Color(0xFF0B0C14), Color(0xFF06070B)),
                        radius = 900f
                    )
                )
                .clickable { onFinished() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(contentAlpha)
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Brush.linearGradient(listOf(goldLight, goldMid, goldDark)))
                        .border(2.dp, goldLight.copy(alpha = 0.9f), RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher),
                        contentDescription = "DEVKAN 34 VIP",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = "DEVKAN 34 VIP",
                    color = goldLight,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun DevkanAppRoot() {
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    var lang by remember { mutableStateOf(DevkanVault.getLanguage(context)) }
    var disclaimerAccepted by remember { mutableStateOf(DevkanVault.isDisclaimerAccepted(context)) }
    val savedPin = remember { DevkanVault.getAppLockPin(context) }
    var isUnlocked by remember { mutableStateOf(savedPin.isEmpty()) }

    val layoutDirection = if (lang == "ar" || lang == "ku") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            when {
                showSplash -> {
                    DevkanSplashIntroScreen(onFinished = { showSplash = false })
                }
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
private fun HeaderIconButton(icon: ImageVector, desc: String, tint: Color, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(30.dp)
            .background(Color(0xFF161620), CircleShape)
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(16.dp))
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF16120A), Color(0xFF0A0A0F))))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(goldLight, goldMid, goldDark)))
                            .border(1.dp, goldLight.copy(alpha = 0.85f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher),
                            contentDescription = "DEVKAN 34 VIP",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("DEVKAN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 0.8.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Brush.horizontalGradient(listOf(goldLight, goldMid)))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("34 VIP", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 0.8.sp)
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF171723))
                        .border(1.dp, goldMid.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .clickable { onSwitchSubClick() }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = goldLight, modifier = Modifier.size(15.dp))
                    Text(
                        text = activeSubLabel.ifEmpty { strings.connectionsLabel },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 78.dp)
                    )
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = goldMid, modifier = Modifier.size(14.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HeaderIconButton(Icons.Default.Search, "Search", Color.White, onSearchClick)
                HeaderIconButton(Icons.Default.FolderOpen, strings.importFile, goldLight, onImportClick)
                HeaderIconButton(Icons.Default.Settings, strings.settings, goldMid, onSettingsClick)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, goldMid.copy(alpha = 0.7f), Color.Transparent)))
        )
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
                    contentDescription = "DEVKAN 34 VIP Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "DEVKAN 34 VIP",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "CINEMA & LIVE STREAM PLAYER",
                        color = goldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Text(
                text = "مشغل وسائط احترافي للبث المباشر والأفلام والمسلسلات بتنظيم واضح وسلاسة عالية.",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("إضافة اشتراك للبدء", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick = onImportM3uClick,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, goldMid.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
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
    var activeSeekMs by remember { mutableLongStateOf(0L) }
    var activeSeriesTracking by remember { mutableStateOf<Triple<String, Int, Int>?>(null) }

    var selectedMovieForDetails by remember { mutableStateOf<MediaItemModel?>(null) }
    var selectedSeriesForDetails by remember { mutableStateOf<MediaItemModel?>(null) }
    var showConnectionsManager by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var subToEdit by remember { mutableStateOf<SavedSubscription?>(null) }
    var showQuickSearch by remember { mutableStateOf(false) }
    var quickSearchQuery by remember { mutableStateOf("") }

    var openedLiveCategory by remember { mutableStateOf<String?>(null) }
    var liveSearchQuery by remember { mutableStateOf("") }

    var isSyncing by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf("") }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var categoriesRevision by remember { mutableIntStateOf(0) }
    var favoritesRevision by remember { mutableIntStateOf(0) }

    var movieServerGroup by remember { mutableStateOf("ALL") }
    var movieQuery by remember { mutableStateOf("") }
    var showMovieGroupsSheet by remember { mutableStateOf(false) }

    var seriesServerGroup by remember { mutableStateOf("ALL") }
    var seriesQuery by remember { mutableStateOf("") }
    var showSeriesGroupsSheet by remember { mutableStateOf(false) }

    val liveCategoriesListState = rememberLazyListState()
    val liveChannelsListState = rememberLazyListState()
    val moviesGridState = rememberLazyGridState()
    val seriesGridState = rememberLazyGridState()
    val homeScrollState = rememberScrollState()

    val initialSub = remember { DevkanVault.getSavedSubscription(context) }
    var currentSub by remember { mutableStateOf(initialSub) }

    val liveItemsList = remember { mutableStateListOf<MediaItemModel>() }
    val movieItemsList = remember { mutableStateListOf<MediaItemModel>() }
    val seriesItemsList = remember { mutableStateListOf<MediaItemModel>() }
    val watchlistIds = remember { mutableStateListOf<String>() }

    val favoriteIds = remember(currentSub?.id, favoritesRevision) {
        DevkanVault.getFavoriteChannelIds(context, currentSub?.id ?: "default")
    }

    var cachedLiveChannels by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedLiveGroupsWithCounts by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var cachedHomeLatestMovies by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedHome4kMovies by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedHomeTopSeries by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedMovieGroups by remember { mutableStateOf<List<String>>(listOf("ALL")) }
    var cachedSeriesGroups by remember { mutableStateOf<List<String>>(listOf("ALL")) }

    var filteredMoviesList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var filteredSeriesList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }

    var syncJob by remember { mutableStateOf<Job?>(null) }

    fun launchStream(
        item: MediaItemModel,
        seekMs: Long = 0L,
        tracking: Triple<String, Int, Int>? = null
    ) {
        val mode = DevkanVault.getExternalPlayerMode(context)
        val externalMode = mode.name.contains("EXT", ignoreCase = true)
        if (externalMode) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(item.url), "video/*")
                    putExtra("title", item.title)
                    putExtra("headers", arrayOf("User-Agent", DevkanVault.getUserAgent(context)))
                }
                context.startActivity(Intent.createChooser(intent, "فتح البث بواسطة"))
                return
            } catch (_: Exception) {
                Toast.makeText(context, "لا يوجد مشغل خارجي مثبت، سيتم التشغيل بالمشغل الداخلي", Toast.LENGTH_SHORT).show()
            }
        }

        activeSeekMs = seekMs.coerceAtLeast(0L)
        activeSeriesTracking = tracking
        activeStream = item
    }

    fun applyItemsImmediately(items: List<MediaItemModel>) {
        if (items.isEmpty()) return
        val newLive = ArrayList<MediaItemModel>()
        val newMov = ArrayList<MediaItemModel>()
        val newSer = ArrayList<MediaItemModel>()
        for (it in items) {
            when (it.contentType) {
                "LIVE" -> newLive.add(it)
                "MOVIE" -> newMov.add(it)
                "SERIES" -> newSer.add(it)
            }
        }
        if (newLive.isNotEmpty()) {
            liveItemsList.clear()
            liveItemsList.addAll(newLive)
        }
        if (newMov.isNotEmpty()) {
            movieItemsList.clear()
            movieItemsList.addAll(newMov)
        }
        if (newSer.isNotEmpty()) {
            seriesItemsList.clear()
            seriesItemsList.addAll(newSer)
        }
    }

    fun startSubscriptionLoad(targetSub: SavedSubscription, showToastOnDone: Boolean) {
        syncJob?.cancel()
        syncJob = scope.launch {
            isSyncing = true
            syncStatusText = "جاري الاتصال بالسيرفر..."
            val geoBypass = DevkanVault.isGeoBypassEnabled(context)

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
                        applyItemsImmediately(partialList)
                    }
                }
            )

            currentSub = updatedSub
            DevkanVault.saveSubscription(context, updatedSub)
            if (fullItems.isNotEmpty()) {
                applyItemsImmediately(fullItems)
                withContext(Dispatchers.IO) {
                    DevkanVault.saveCachedItems(context, fullItems, updatedSub.id)
                }
                if (showToastOnDone) {
                    Toast.makeText(context, "اكتمل التحميل (${fullItems.size} عنصر)", Toast.LENGTH_SHORT).show()
                }
            }
            isSyncing = false
            syncStatusText = ""
        }
    }

    fun switchSubscriptionFast(targetSub: SavedSubscription) {
        DevkanVault.switchActiveSubscription(context, targetSub.id)
        currentSub = targetSub
        openedLiveCategory = null
        movieServerGroup = "ALL"
        seriesServerGroup = "ALL"

        scope.launch {
            val cachedLive = withContext(Dispatchers.IO) { DevkanVault.getCachedItemsByType(context, targetSub.id, "LIVE") }
            val cachedMov = withContext(Dispatchers.IO) { DevkanVault.getCachedItemsByType(context, targetSub.id, "MOVIE") }
            val cachedSer = withContext(Dispatchers.IO) { DevkanVault.getCachedItemsByType(context, targetSub.id, "SERIES") }

            if (cachedLive.isNotEmpty()) { liveItemsList.clear(); liveItemsList.addAll(cachedLive) }
            if (cachedMov.isNotEmpty()) { movieItemsList.clear(); movieItemsList.addAll(cachedMov) }
            if (cachedSer.isNotEmpty()) { seriesItemsList.clear(); seriesItemsList.addAll(cachedSer) }

            startSubscriptionLoad(targetSub, showToastOnDone = true)
        }
    }

    LaunchedEffect(liveItemsList.size, currentSub?.id, categoriesRevision) {
        val snapshot = liveItemsList.toList()
        if (snapshot.isEmpty()) return@LaunchedEffect
        val subId = currentSub?.id ?: "default"
        val hiddenLive = DevkanVault.getHiddenCategories(context, subId, "LIVE")
        val visible = snapshot.filter { !hiddenLive.contains(it.group) }
        val countsMap = visible.groupingBy { it.group }.eachCount()
        cachedLiveChannels = visible
        cachedLiveGroupsWithCounts = countsMap.entries.map { it.key to it.value }
    }

    LaunchedEffect(movieItemsList.size, currentSub?.id, categoriesRevision) {
        val snapshot = movieItemsList.toList()
        if (snapshot.isEmpty()) return@LaunchedEffect
        val subId = currentSub?.id ?: "default"
        val hiddenMovie = DevkanVault.getHiddenCategories(context, subId, "MOVIE")
        val visible = snapshot.filter { !hiddenMovie.contains(it.group) }
        cachedHomeLatestMovies = visible.sortedByDescending { it.addedTimestamp }.take(12)
        cachedHome4kMovies = visible.filter { it.is4k }.take(12)
        cachedMovieGroups = listOf("ALL") + visible.map { it.group }.distinct()
    }

    LaunchedEffect(seriesItemsList.size, currentSub?.id, categoriesRevision) {
        val snapshot = seriesItemsList.toList()
        if (snapshot.isEmpty()) return@LaunchedEffect
        val subId = currentSub?.id ?: "default"
        val hiddenSeries = DevkanVault.getHiddenCategories(context, subId, "SERIES")
        val visible = snapshot.filter { !hiddenSeries.contains(it.group) }
        cachedHomeTopSeries = visible.take(12)
        cachedSeriesGroups = listOf("ALL") + visible.map { it.group }.distinct()
    }

    LaunchedEffect(selectedTab, movieItemsList.size, movieServerGroup, movieQuery, categoriesRevision) {
        if (selectedTab != 2) return@LaunchedEffect
        val snapshot = movieItemsList.toList()
        val subId = currentSub?.id ?: "default"
        val hiddenMovie = DevkanVault.getHiddenCategories(context, subId, "MOVIE")
        val q = movieQuery.trim()
        val visible = snapshot.filter { !hiddenMovie.contains(it.group) }
        val byGroup = if (movieServerGroup == "ALL") visible else visible.filter { it.group == movieServerGroup }
        filteredMoviesList = if (q.isEmpty()) byGroup else byGroup.filter { it.title.contains(q, ignoreCase = true) }
    }

    LaunchedEffect(selectedTab, seriesItemsList.size, seriesServerGroup, seriesQuery, categoriesRevision) {
        if (selectedTab != 3) return@LaunchedEffect
        val snapshot = seriesItemsList.toList()
        val subId = currentSub?.id ?: "default"
        val hiddenSeries = DevkanVault.getHiddenCategories(context, subId, "SERIES")
        val q = seriesQuery.trim()
        val visible = snapshot.filter { !hiddenSeries.contains(it.group) }
        val byGroup = if (seriesServerGroup == "ALL") visible else visible.filter { it.group == seriesServerGroup }
        filteredSeriesList = if (q.isEmpty()) byGroup else byGroup.filter { it.title.contains(q, ignoreCase = true) }
    }

    BackHandler(
        enabled = activeStream == null &&
            !showConnectionsManager &&
            selectedMovieForDetails == null &&
            selectedSeriesForDetails == null &&
            !showQuickSearch
    ) {
        if (selectedTab == 1 && openedLiveCategory != null) {
            openedLiveCategory = null
            liveSearchQuery = ""
        } else if (selectedTab != 0) {
            selectedTab = 0
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000L) {
                activity?.finish()
            } else {
                lastBackPressTime = now
                Toast.makeText(context, "اضغط رجوع مرة أخرى للخروج من DEVKAN 34 VIP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        val saved = initialSub
        if (saved != null) {
            val cachedLive = withContext(Dispatchers.IO) { DevkanVault.getCachedItemsByType(context, saved.id, "LIVE") }
            val cachedMov = withContext(Dispatchers.IO) { DevkanVault.getCachedItemsByType(context, saved.id, "MOVIE") }
            val cachedSer = withContext(Dispatchers.IO) { DevkanVault.getCachedItemsByType(context, saved.id, "SERIES") }

            if (cachedLive.isNotEmpty()) { liveItemsList.clear(); liveItemsList.addAll(cachedLive) }
            if (cachedMov.isNotEmpty()) { movieItemsList.clear(); movieItemsList.addAll(cachedMov) }
            if (cachedSer.isNotEmpty()) { seriesItemsList.clear(); seriesItemsList.addAll(cachedSer) }

            if (cachedLive.isEmpty() && cachedMov.isEmpty() && cachedSer.isEmpty()) {
                startSubscriptionLoad(saved, showToastOnDone = true)
            } else if (DevkanVault.shouldRunBackgroundSync(context)) {
                delay(1500L)
                startSubscriptionLoad(saved, showToastOnDone = false)
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
                    applyItemsImmediately(imported)
                    withContext(Dispatchers.IO) { DevkanVault.saveCachedItems(context, imported, localSub.id) }
                    Toast.makeText(context, "تم استيراد ${imported.size} عنصر", Toast.LENGTH_SHORT).show()
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
        activeSeekMs = 0L
        activeSeriesTracking = null
    }

    val totalItemsCount = liveItemsList.size + movieItemsList.size + seriesItemsList.size

    if (currentSub == null && totalItemsCount == 0 && !isSyncing && !showConnectionsManager) {
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
                    showAddDialog = false
                    subToEdit = null
                    currentSub = newSub
                    DevkanVault.saveSubscription(context, newSub)
                    startSubscriptionLoad(newSub, showToastOnDone = true)
                }
            )
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF07070A))) {
        Scaffold(
            containerColor = Color(0xFF07070A),
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DevkanBrandHeader(
                        activeSubLabel = currentSub?.label ?: strings.connectionsLabel,
                        onSwitchSubClick = { showConnectionsManager = true },
                        onSearchClick = { showQuickSearch = true },
                        onImportClick = { m3uFileLauncher.launch("*/*") },
                        onSettingsClick = { selectedTab = 4 },
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
                                    text = syncStatusText.ifEmpty { "جاري التحميل..." },
                                    color = goldLight,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$totalItemsCount عنصر",
                                    color = Color(0xFF4ADE80),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                color = gold,
                                trackColor = Color(0xFF252538),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(CircleShape)
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
                        Triple(strings.sports, Icons.Default.SportsSoccer, 5),
                        Triple(strings.moviesTab, Icons.Default.Movie, 2),
                        Triple(strings.seriesTab, Icons.Default.VideoLibrary, 3)
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
                        val homePills = remember(strings) {
                            listOf(
                                "ALL" to strings.all,
                                "LIVE" to strings.live,
                                "SPORTS" to strings.sports,
                                "MOVIES" to strings.moviesTab,
                                "SERIES" to strings.seriesTab
                            )
                        }

                        val heroFeatured = cachedHomeTopSeries.firstOrNull { it.logo.isNotEmpty() }
                            ?: cachedHomeLatestMovies.firstOrNull { it.logo.isNotEmpty() }
                            ?: cachedHomeTopSeries.firstOrNull()
                            ?: cachedHomeLatestMovies.firstOrNull()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(homeScrollState)
                                .padding(vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                homePills.forEach { (key, label) ->
                                    val isSelected = key == "ALL"
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) gold else Color(0xFF14141F))
                                            .border(1.dp, if (isSelected) gold else Color(0xFF28283A), RoundedCornerShape(50))
                                            .clickable {
                                                when (key) {
                                                    "LIVE" -> selectedTab = 1
                                                    "SPORTS" -> selectedTab = 5
                                                    "MOVIES" -> selectedTab = 2
                                                    "SERIES" -> selectedTab = 3
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            if (heroFeatured != null) {
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
                                    if (heroFeatured.logo.isNotEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(heroFeatured.logo)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = heroFeatured.title,
                                            contentScale = ContentScale.Crop,
                                            alignment = Alignment.TopCenter,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))))
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

                            if (lastCh != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF14141D))
                                        .border(1.dp, gold.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                        .clickable {
                                            launchStream(
                                                item = MediaItemModel("last", "0", lastCh.second, lastCh.first, "", "Live", "", "LIVE")
                                            )
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(strings.continueWatching, color = gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(lastCh.second, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = gold, modifier = Modifier.size(30.dp))
                                }
                            }

                            if (cachedHomeLatestMovies.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                                    CinemaHorizontalRow(
                                        title = strings.latestMovies,
                                        items = cachedHomeLatestMovies,
                                        onMoreClick = { selectedTab = 2 },
                                        onItemClick = { selectedMovieForDetails = it }
                                    )
                                }
                            }

                            if (cachedHomeTopSeries.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                                    CinemaHorizontalRow(
                                        title = strings.featuredSeries,
                                        items = cachedHomeTopSeries,
                                        onMoreClick = { selectedTab = 3 },
                                        onItemClick = { selectedSeriesForDetails = it }
                                    )
                                }
                            }

                            if (cachedHome4kMovies.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                                    CinemaHorizontalRow(
                                        title = strings.movies4k,
                                        items = cachedHome4kMovies,
                                        onMoreClick = { selectedTab = 2 },
                                        onItemClick = { selectedMovieForDetails = it }
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        val activeCategory = openedLiveCategory
                        if (activeCategory == null) {
                            val filteredGroupsList = remember(cachedLiveGroupsWithCounts, liveSearchQuery) {
                                val q = liveSearchQuery.trim()
                                if (q.isEmpty()) cachedLiveGroupsWithCounts
                                else cachedLiveGroupsWithCounts.filter { (grpName, _) ->
                                    grpName.contains(q, ignoreCase = true)
                                }
                            }

                            Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("باقات البث المباشر", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                        Text(
                                            text = "${cachedLiveGroupsWithCounts.size} باقة • ${cachedLiveChannels.size} قناة",
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    if (!isSyncing && currentSub != null) {
                                        IconButton(
                                            onClick = { startSubscriptionLoad(currentSub!!, showToastOnDone = true) },
                                            modifier = Modifier.size(34.dp).background(Color(0xFF171723), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = goldLight, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = liveSearchQuery,
                                    onValueChange = { liveSearchQuery = it },
                                    placeholder = { Text("ابحث عن باقة...", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gold, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        if (liveSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { liveSearchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                LazyColumn(
                                    state = liveCategoriesListState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item(key = "favorites_hub") {
                                        val favCount = cachedLiveChannels.count { favoriteIds.contains(it.id) }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Brush.horizontalGradient(listOf(Color(0xFF231C0D), Color(0xFF161522))))
                                                .border(1.dp, goldLight.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                                                .clickable {
                                                    liveSearchQuery = ""
                                                    openedLiveCategory = "FAVORITES"
                                                }
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(goldLight.copy(alpha = 0.22f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Star, contentDescription = null, tint = goldLight, modifier = Modifier.size(22.dp))
                                                }
                                                Text("قنواتي المفضلة", color = goldLight, fontWeight = FontWeight.Black, fontSize = 14.5.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .background(goldLight.copy(alpha = 0.22f))
                                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                                ) {
                                                    Text("$favCount", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = goldLight)
                                            }
                                        }
                                    }

                                    item(key = "all_channels_hub") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Brush.horizontalGradient(listOf(Color(0xFF1D180E), Color(0xFF141522))))
                                                .border(1.dp, gold.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                                                .clickable {
                                                    liveSearchQuery = ""
                                                    openedLiveCategory = "ALL"
                                                }
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(gold.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.LiveTv, contentDescription = null, tint = goldLight, modifier = Modifier.size(20.dp))
                                                }
                                                Text("جميع القنوات المباشرة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .background(gold.copy(alpha = 0.22f))
                                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                                ) {
                                                    Text("${cachedLiveChannels.size}", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = goldLight)
                                            }
                                        }
                                    }

                                    items(filteredGroupsList, key = { it.first }) { (grpName, count) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(15.dp))
                                                .background(Color(0xFF12141F))
                                                .border(1.dp, Color(0xFF202436), RoundedCornerShape(15.dp))
                                                .clickable {
                                                    liveSearchQuery = ""
                                                    openedLiveCategory = grpName
                                                }
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(RoundedCornerShape(11.dp))
                                                        .background(gold.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = gold, modifier = Modifier.size(20.dp))
                                                }
                                                Text(
                                                    text = grpName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .background(Color(0xFF1D2030))
                                                        .padding(horizontal = 9.dp, vertical = 3.dp)
                                                ) {
                                                    Text("$count", color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                                }
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF64748B))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val channelsInOpenedGroup = remember(cachedLiveChannels, activeCategory, liveSearchQuery, favoriteIds) {
                                val q = liveSearchQuery.trim()
                                cachedLiveChannels.filter { ch ->
                                    val catMatches = when (activeCategory) {
                                        "ALL" -> true
                                        "FAVORITES" -> favoriteIds.contains(ch.id)
                                        else -> ch.group == activeCategory
                                    }
                                    catMatches && (q.isEmpty() || ch.title.contains(q, ignoreCase = true))
                                }
                            }

                            Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = {
                                                openedLiveCategory = null
                                                liveSearchQuery = ""
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFF181A28), CircleShape)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = goldLight, modifier = Modifier.size(18.dp))
                                        }
                                        Column {
                                            Text(
                                                text = when (activeCategory) {
                                                    "ALL" -> "جميع القنوات"
                                                    "FAVORITES" -> "قنواتي المفضلة"
                                                    else -> activeCategory
                                                },
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text("${channelsInOpenedGroup.size} قناة", color = goldLight, fontSize = 11.sp)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = liveSearchQuery,
                                    onValueChange = { liveSearchQuery = it },
                                    placeholder = { Text("ابحث داخل هذه الباقة...", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gold, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        if (liveSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { liveSearchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                LazyColumn(state = liveChannelsListState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(items = channelsInOpenedGroup, key = { it.id }, contentType = { "live_channel" }) { ch ->
                                        val isFav = favoriteIds.contains(ch.id)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF131520))
                                                .border(1.dp, if (isFav) gold.copy(alpha = 0.45f) else Color(0xFF212436), RoundedCornerShape(14.dp))
                                                .clickable { launchStream(ch) }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                if (ch.logo.isNotEmpty()) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(context).data(ch.logo).build(),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(RoundedCornerShape(9.dp))
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(RoundedCornerShape(9.dp))
                                                            .background(Color(0xFF1D2030)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Tv, contentDescription = null, tint = gold, modifier = Modifier.size(22.dp))
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(ch.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(ch.group, color = Color(0xFF9CA3AF), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    val added = DevkanVault.toggleFavoriteChannel(context, currentSub?.id ?: "default", ch.id)
                                                    favoritesRevision++
                                                    Toast.makeText(context, if (added) "أضيفت للمفضلة" else "أزيلت من المفضلة", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = "Fav",
                                                    tint = if (isFav) goldLight else Color.Gray,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${strings.moviesTab} (${filteredMoviesList.size})", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                                OutlinedButton(
                                    onClick = { showMovieGroupsSheet = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    border = BorderStroke(1.dp, gold.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = goldLight, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (movieServerGroup == "ALL") "مجلدات السيرفر" else movieServerGroup,
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

                            LazyVerticalGrid(
                                state = moviesGridState,
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            ) {
                                items(items = filteredMoviesList, key = { it.id }, contentType = { "movie_poster" }) { item ->
                                    val prog = remember(item.id, activeStream) {
                                        DevkanVault.getVodProgressFraction(context, item.id)
                                    }
                                    MediaPosterCard(
                                        item = item,
                                        progress = prog,
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

                    3 -> {
                        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${strings.seriesTab} (${filteredSeriesList.size})", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                                OutlinedButton(
                                    onClick = { showSeriesGroupsSheet = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    border = BorderStroke(1.dp, gold.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = goldLight, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (seriesServerGroup == "ALL") "مجلدات السيرفر" else seriesServerGroup,
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

                            LazyVerticalGrid(
                                state = seriesGridState,
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            ) {
                                items(items = filteredSeriesList, key = { it.id }, contentType = { "series_poster" }) { item ->
                                    val lastEp = remember(item.id, activeStream) {
                                        DevkanVault.getLastWatchedEpisodeForSeries(context, item.id)
                                    }
                                    val prog = remember(lastEp) {
                                        lastEp?.let { DevkanVault.getVodProgressFraction(context, it.first) } ?: 0f
                                    }
                                    MediaPosterCard(
                                        item = item,
                                        progress = prog,
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

                    4 -> {
                        DevkanSettingsScreen(
                            strings = strings,
                            lang = lang,
                            onLanguageChange = onLanguageChange,
                            subscription = currentSub,
                            onOpenConnectionsManager = { showConnectionsManager = true },
                            onRefreshSub = {
                                val sub = currentSub ?: return@DevkanSettingsScreen
                                startSubscriptionLoad(sub, showToastOnDone = true)
                            },
                            onOpenAddDialog = {
                                subToEdit = null
                                showAddDialog = true
                            },
                            sampleTestUrl = liveItemsList.firstOrNull()?.url ?: "https://www.google.com"
                        )
                    }

                    5 -> {
                        val goldCard = Color(0xFF131520)
                        val borderCard = Color(0xFF212436)

                        fun isSportOrKassChannel(ch: MediaItemModel): Boolean {
                            val combined = "${ch.title} | ${ch.group}".lowercase(Locale.ROOT)
                            val hasSportWord = Regex("""(?<![a-z])sport(?![a-z])""").containsMatchIn(combined)
                            val keywords = listOf(
                                "رياض", "bein", "ssc", "espn", "dazn", "eurosport",
                                "الكأس", "alkass", "kass", "arryadia", "fox sports",
                                "tnt", "canal+", "rmc", "nba", "ufc", "wwe", "f1", "motogp",
                                "الرابعة", "rabiaa", "أبو ظبي", "ابوظبي", "ad sports", "dubai sport", "دبي الرياضية"
                            )
                            return hasSportWord || keywords.any { kw -> combined.contains(kw.lowercase(Locale.ROOT)) }
                        }

                        fun detectQualityTier(ch: MediaItemModel): String {
                            val t = ch.title.lowercase(Locale.ROOT)
                            return when {
                                Regex("""(?<![a-z0-9])(4k|uhd|2160p)(?![a-z0-9])""").containsMatchIn(t) -> "4K"
                                Regex("""(?<![a-z0-9])(fhd|1080p)(?![a-z0-9])""").containsMatchIn(t) -> "FHD"
                                Regex("""(?<![a-z0-9])(sd|576i|480p)(?![a-z0-9])""").containsMatchIn(t) -> "SD"
                                Regex("""(?<![a-z0-9])(hd|720p)(?![a-z0-9])""").containsMatchIn(t) -> "HD"
                                else -> "OTHER"
                            }
                        }

                        fun langPriority(ch: MediaItemModel, appLang: String): Int {
                            val txt = "${ch.group} | ${ch.title}".lowercase(Locale.ROOT)
                            val langKw = when (appLang) {
                                "en" -> listOf("sky", "tnt", "bt", "espn", "bein", "eurosport", "en", "uk", "us")
                                "de" -> listOf("sky", "dazn", "sportdigital", "magenta", "eurosport", "de")
                                "tr" -> listOf("bein", "s sport", "trt spor", "tivibu", "a spor", "tr")
                                "fr" -> listOf("canal+", "bein", "rmc", "eurosport", "dazn", "fr")
                                "ku" -> listOf("kurd", "كورد", "bein", "ssc", "الكأس", "alkass", "الرابعة", "ku")
                                else -> listOf("bein", "ssc", "الكأس", "alkass", "kass", "ad sports", "abu dhabi", "dubai", "دبي", "arryadia", "الرابعة", "ar", "عربي")
                            }
                            val idx = langKw.indexOfFirst { txt.contains(it) }
                            return if (idx >= 0) idx else 99
                        }

                        var selectedQuality by remember { mutableStateOf<String?>(null) }
                        var sportsQuery by remember { mutableStateOf("") }

                        val allSportChannels = remember(cachedLiveChannels, lang) {
                            cachedLiveChannels
                                .filter { isSportOrKassChannel(it) }
                                .sortedWith(compareBy({ langPriority(it, lang) }, { it.title.lowercase(Locale.ROOT) }))
                        }

                        val groupedByQuality = remember(allSportChannels) {
                            val map = linkedMapOf(
                                "SD" to mutableListOf<MediaItemModel>(),
                                "HD" to mutableListOf<MediaItemModel>(),
                                "FHD" to mutableListOf<MediaItemModel>(),
                                "4K" to mutableListOf<MediaItemModel>(),
                                "OTHER" to mutableListOf<MediaItemModel>()
                            )
                            for (ch in allSportChannels) {
                                map[detectQualityTier(ch)]?.add(ch)
                            }
                            map
                        }

                        val qualityBuckets = remember(groupedByQuality, lang) {
                            val labels = when (lang) {
                                "en" -> mapOf(
                                    "SD" to "SD Channels",
                                    "HD" to "HD Channels",
                                    "FHD" to "FHD Channels",
                                    "4K" to "4K Channels",
                                    "OTHER" to "Other Sports"
                                )
                                "de" -> mapOf(
                                    "SD" to "SD-Sender",
                                    "HD" to "HD-Sender",
                                    "FHD" to "FHD-Sender",
                                    "4K" to "4K-Sender",
                                    "OTHER" to "Weitere Sport"
                                )
                                "tr" -> mapOf(
                                    "SD" to "SD Kanallar",
                                    "HD" to "HD Kanallar",
                                    "FHD" to "FHD Kanallar",
                                    "4K" to "4K Kanallar",
                                    "OTHER" to "Diğer Spor"
                                )
                                "fr" -> mapOf(
                                    "SD" to "Chaînes SD",
                                    "HD" to "Chaînes HD",
                                    "FHD" to "Chaînes FHD",
                                    "4K" to "Chaînes 4K",
                                    "OTHER" to "Autres Sport"
                                )
                                "ku" -> mapOf(
                                    "SD" to "کەناڵەکانی SD",
                                    "HD" to "کەناڵەکانی HD",
                                    "FHD" to "کەناڵەکانی FHD",
                                    "4K" to "کەناڵەکانی 4K",
                                    "OTHER" to "وەرزشی تر"
                                )
                                else -> mapOf(
                                    "SD" to "قنوات SD",
                                    "HD" to "قنوات HD",
                                    "FHD" to "قنوات FHD",
                                    "4K" to "قنوات 4K",
                                    "OTHER" to "قنوات رياضية أخرى"
                                )
                            }
                            listOf("SD", "HD", "FHD", "4K", "OTHER").mapNotNull { tier ->
                                val list = groupedByQuality[tier].orEmpty()
                                if (list.isEmpty()) null else Triple(tier, labels[tier] ?: tier, list)
                            }
                        }

                        val visibleChannels = remember(allSportChannels, groupedByQuality, selectedQuality, sportsQuery) {
                            val baseList = when (selectedQuality) {
                                null -> allSportChannels
                                else -> groupedByQuality[selectedQuality].orEmpty()
                            }
                            val q = sportsQuery.trim()
                            if (q.isEmpty()) baseList
                            else baseList.filter {
                                it.title.contains(q, ignoreCase = true) || it.group.contains(q, ignoreCase = true)
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxSize().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(strings.sports, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                    Text(
                                        text = if (selectedQuality == null)
                                            "${allSportChannels.size} قناة رياضية"
                                        else
                                            "${visibleChannels.size} قناة ضمن $selectedQuality",
                                        color = goldLight,
                                        fontSize = 11.5.sp
                                    )
                                }
                                if (selectedQuality != null) {
                                    TextButton(onClick = { selectedQuality = null }) {
                                        Text(strings.all, color = goldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = sportsQuery,
                                onValueChange = { sportsQuery = it },
                                placeholder = { Text(strings.searchHint, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gold, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (sportsQuery.isNotEmpty()) {
                                        IconButton(onClick = { sportsQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (qualityBuckets.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item(key = "q_all") {
                                        val active = selectedQuality == null
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (active) gold else goldCard)
                                                .border(1.dp, if (active) goldLight else borderCard, RoundedCornerShape(14.dp))
                                                .clickable { selectedQuality = null }
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = "${strings.all} (${allSportChannels.size})",
                                                color = if (active) Color.Black else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            )
                                        }
                                    }

                                    items(qualityBuckets, key = { "q_${it.first}" }) { (tier, title, list) ->
                                        val active = selectedQuality == tier
                                        Column(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (active) gold.copy(alpha = 0.22f) else goldCard)
                                                .border(1.dp, if (active) goldLight else borderCard, RoundedCornerShape(14.dp))
                                                .clickable {
                                                    selectedQuality = if (active) null else tier
                                                }
                                                .padding(horizontal = 14.dp, vertical = 9.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = title,
                                                color = if (active) goldLight else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            )
                                            Text(
                                                text = "${list.size}",
                                                color = if (active) Color.White else Color(0xFF9CA3AF),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(visibleChannels, key = { "sp_${it.id}" }, contentType = { "sports_channel" }) { ch ->
                                    val tier = remember(ch.id, ch.title) { detectQualityTier(ch) }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(goldCard)
                                            .border(1.dp, borderCard, RoundedCornerShape(14.dp))
                                            .clickable { launchStream(ch) }
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
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context).data(ch.logo).build(),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.dp))
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(9.dp))
                                                        .background(Color(0xFF1D2030)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Tv, contentDescription = null, tint = gold, modifier = Modifier.size(22.dp))
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(ch.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(ch.group, color = Color(0xFF9CA3AF), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        if (tier != "OTHER") {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(gold.copy(alpha = 0.18f))
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(tier, color = goldLight, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
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
                onPlayMovie = { item, seekMs ->
                    selectedMovieForDetails = null
                    launchStream(item, seekMs = seekMs)
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
                onPlayEpisode = { epItem, seriesId, seasonNum, episodeNum, seekMs ->
                    DevkanVault.saveVodProgress(context, epItem.id, seekMs.coerceAtLeast(1000L), (seekMs + 60_000L).coerceAtLeast(60_000L), seriesId, episodeNum, seasonNum)
                    launchStream(epItem, seekMs = seekMs, tracking = Triple(seriesId, seasonNum, episodeNum))
                },
                onBack = { selectedSeriesForDetails = null }
            )
        }

        if (showConnectionsManager) {
            val combinedForCats = remember(liveItemsList.size, movieItemsList.size, seriesItemsList.size) {
                liveItemsList.toList() + movieItemsList.toList() + seriesItemsList.toList()
            }
            ConnectionsManagerScreen(
                strings = strings,
                activeSubId = currentSub?.id,
                allItemsForCategories = combinedForCats,
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
                            liveItemsList.clear()
                            movieItemsList.clear()
                            seriesItemsList.clear()
                            cachedLiveChannels = emptyList()
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

        if (activeStream != null) {
            val st = activeStream!!
            val subIdNow = currentSub?.id ?: "default"
            val tracking = activeSeriesTracking
            DevkanHybridPlayerScreen(
                streamUrl = st.url,
                fallbackUrl = st.fallbackUrl,
                streamTitle = st.title,
                isVodOrSeries = st.contentType != "LIVE",
                streamId = st.id,
                initialSeekMs = activeSeekMs,
                seriesId = tracking?.first ?: "",
                seasonNum = tracking?.second ?: 1,
                episodeNum = tracking?.third ?: 0,
                subId = subIdNow,
                isFavorite = favoriteIds.contains(st.id),
                onToggleFavorite = {
                    val added = DevkanVault.toggleFavoriteChannel(context, subIdNow, st.id)
                    favoritesRevision++
                    Toast.makeText(context, if (added) "أضيفت للمفضلة" else "أزيلت من المفضلة", Toast.LENGTH_SHORT).show()
                },
                quickChannels = cachedLiveChannels,
                onSwitchChannel = { nextCh ->
                    activeSeekMs = 0L
                    activeSeriesTracking = null
                    activeStream = nextCh
                },
                lang = lang,
                onBack = { handleClosePlayer() }
            )
        }
    }

    if (showQuickSearch) {
        val searchResults = remember(quickSearchQuery, liveItemsList.size, movieItemsList.size, seriesItemsList.size) {
            val q = quickSearchQuery.trim()
            if (q.length < 2) emptyList()
            else {
                val res = ArrayList<MediaItemModel>(25)
                for (it in liveItemsList) { if (it.title.contains(q, ignoreCase = true)) { res.add(it); if (res.size >= 25) break } }
                if (res.size < 25) for (it in movieItemsList) { if (it.title.contains(q, ignoreCase = true)) { res.add(it); if (res.size >= 25) break } }
                if (res.size < 25) for (it in seriesItemsList) { if (it.title.contains(q, ignoreCase = true)) { res.add(it); if (res.size >= 25) break } }
                res
            }
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
                                            else -> launchStream(item)
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

    if (showAddDialog) {
        SmartAddConnectionDialog(
            strings = strings,
            editingSub = subToEdit,
            onDismiss = {
                showAddDialog = false
                subToEdit = null
            },
            onSaveAndConnect = { newSub ->
                showAddDialog = false
                subToEdit = null
                showConnectionsManager = false
                currentSub = newSub
                DevkanVault.saveSubscription(context, newSub)
                startSubscriptionLoad(newSub, showToastOnDone = true)
            }
        )
    }
}
