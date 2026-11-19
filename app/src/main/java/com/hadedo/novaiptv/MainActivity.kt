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
import androidx.compose.ui.geometry.Offset
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
import kotlinx.coroutines.delay
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
    return SportsMatchExtractor.isSportsProgram(group, title, null)
}

private fun formatCleanLeagueName(raw: String): String {
    val cleaned = raw
        .replace(Regex("""[|┃┆┇┊┋︱︲∣║‖•/_\\]+"""), " ")
        .replace(Regex("""(?i)^\s*(fr|uk|us|ar|de|tr|es|be|nl|it|pt)\b\s*"""), "")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()
    return cleaned.ifEmpty { "بطولة دولية" }
}

@Composable
fun DevkanSplashIntroScreen(onFinished: () -> Unit) {
    val goldLight = Color(0xFFFDE047)
    val goldMid = Color(0xFFD4AF37)
    val goldDark = Color(0xFFB45309)

    var startAnim by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.78f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "logoScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        label = "contentAlpha"
    )
    val lineFraction by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 300, delayMillis = 60, easing = FastOutSlowInEasing),
        label = "lineFraction"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(350L)
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
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(contentAlpha)
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(listOf(goldLight, goldMid, goldDark)))
                        .border(2.dp, goldLight.copy(alpha = 0.9f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher),
                        contentDescription = "DEVKAN 34 VIP",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val shimmerBrush = Brush.linearGradient(
                        colors = listOf(goldMid, Color.White, goldLight, goldMid),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 280f, 120f)
                    )

                    Text(
                        text = "DEVKAN 34 VIP",
                        style = LocalTextStyle.current.copy(
                            brush = shimmerBrush,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            letterSpacing = 2.5.sp
                        )
                    )

                    Box(
                        modifier = Modifier
                            .width((190 * lineFraction).dp)
                            .height(2.5.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, goldLight, Color.Transparent)))
                    )

                    Text(
                        text = "ULTRA CINEMA & LIVE SPORTS",
                        color = Color(0xFFA1A1AA),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp
                    )
                }
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

    LaunchedEffect(Unit) {
        DevkanVault.getDnsProvider(context)
    }

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
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                Column {
                    Text("DEVKAN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 0.6.sp)
                    Text("34 VIP", color = goldLight, fontWeight = FontWeight.ExtraBold, fontSize = 9.5.sp, letterSpacing = 1.sp)
                }
            }
        }

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
                text = activeSubLabel.ifEmpty { "الاتصالات" },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 95.dp)
            )
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = goldMid, modifier = Modifier.size(14.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(31.dp)
                    .background(Color(0xFF161620), CircleShape)
            ) {
                Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            IconButton(
                onClick = onImportClick,
                modifier = Modifier
                    .size(31.dp)
                    .background(Color(0xFF161620), CircleShape)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = strings.importFile, tint = goldLight, modifier = Modifier.size(16.dp))
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(31.dp)
                    .background(Color(0xFF161620), CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = strings.settings, tint = goldMid, modifier = Modifier.size(16.dp))
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
                        text = "CINEMA & SPORTS MEDIA PLAYER",
                        color = goldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp
                    )
                }
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

    var openedLiveCategory by remember { mutableStateOf<String?>(null) }
    var liveCategoryKindFilter by remember { mutableStateOf("ALL") }
    var liveSearchQuery by remember { mutableStateOf("") }

    var isSyncing by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf("") }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var categoriesRevision by remember { mutableIntStateOf(0) }
    var layoutResetKey by remember { mutableIntStateOf(0) }
    var favoritesRevision by remember { mutableIntStateOf(0) }

    var isRefreshingSports by remember { mutableStateOf(false) }

    var movieSmartFilter by remember { mutableStateOf("ALL") }
    var movieServerGroup by remember { mutableStateOf("ALL") }
    var movieQuery by remember { mutableStateOf("") }
    var showMovieGroupsSheet by remember { mutableStateOf(false) }

    var seriesSmartFilter by remember { mutableStateOf("ALL") }
    var seriesServerGroup by remember { mutableStateOf("ALL") }
    var seriesQuery by remember { mutableStateOf("") }
    var showSeriesGroupsSheet by remember { mutableStateOf(false) }

    val liveCategoriesGridState = rememberLazyListState()
    val liveChannelsListState = rememberLazyListState()
    val moviesGridState = rememberLazyGridState()
    val seriesGridState = rememberLazyGridState()
    val homeScrollState = rememberScrollState()

    val initialSub = remember { DevkanVault.getSavedSubscription(context) }
    var currentSub by remember { mutableStateOf(initialSub) }

    val liveItemsList = remember { mutableStateListOf<MediaItemModel>() }
    val movieItemsList = remember { mutableStateListOf<MediaItemModel>() }
    val seriesItemsList = remember { mutableStateListOf<MediaItemModel>() }
    val apiRealMatches = remember { mutableStateListOf<RealSportsMatch>() }
    val watchlistIds = remember { mutableStateListOf<String>() }

    val favoriteIds = remember(currentSub?.id, favoritesRevision) {
        DevkanVault.getFavoriteChannelIds(context, currentSub?.id ?: "default")
    }

    var cachedSportsChannels by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedLiveChannels by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedLiveGroupsWithCounts by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var cachedHomeLatestMovies by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedHome4kMovies by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedHomeTopSeries by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var cachedMovieGroups by remember { mutableStateOf<List<String>>(listOf("ALL")) }
    var cachedMovieFilters by remember { mutableStateOf<List<Pair<String, String>>>(listOf("ALL" to "الكل")) }
    var cachedSeriesGroups by remember { mutableStateOf<List<String>>(listOf("ALL")) }
    var cachedSeriesFilters by remember { mutableStateOf<List<Pair<String, String>>>(listOf("ALL" to "الكل")) }

    var filteredMoviesList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var filteredSeriesList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var vodProgressCache by remember { mutableStateOf(mapOf("" to 0f)) }

    val combinedRealMatches = remember(apiRealMatches.size, cachedLiveChannels.size, XtreamEpgRepository.lastEpgUpdate.value) {
        val fromServerEpg = XtreamEpgRepository.extractMatchesFromServerEpg(cachedLiveChannels)
        (fromServerEpg + apiRealMatches).distinctBy { "${it.homeTeam.lowercase()}_${it.awayTeam.lowercase()}" }
    }

    fun launchStream(
        item: MediaItemModel,
        isSports: Boolean = isSmartSportChannel(item.group, item.title),
        tracking: Triple<String, Int, Int>? = null
    ) {
        val isVodOrSeries = item.contentType != "LIVE"
        val perfMode = DevkanVault.getPerformanceMode(context)
        val lowLatencySetting = DevkanVault.isLowLatencyEnabled(context)
        val tuning = DevicePerformanceManager.buildTuningConfig(
            context = context,
            isVodOrSeries = isVodOrSeries,
            performanceMode = perfMode,
            lowLatency = lowLatencySetting
        )
        val config = PlayerEngineRouter.resolvePlaybackConfig(
            isSportsSection = isSports,
            preferredEngineType = DevkanVault.getPreferredEngine(context),
            externalPlayerMode = DevkanVault.getExternalPlayerMode(context),
            userLowLatencyEnabled = lowLatencySetting,
            tuningConfig = tuning
        )

        if (config.engine == PlayerEngine.NATIVE_EXTERNAL) {
            try {
                val formatMode = DevkanVault.getStreamFormatMode(context)
                val targetUrl = if (item.contentType == "LIVE") {
                    EngineCoordinator.resolveLiveStreamUrls(item.url, item.fallbackUrl, formatMode).first
                } else item.url

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(targetUrl), "video/*")
                    putExtra("title", item.title)
                    putExtra("headers", arrayOf("User-Agent", DevkanVault.getUserAgent(context)))
                }
                context.startActivity(Intent.createChooser(intent, "فتح البث بواسطة"))
                return
            } catch (_: Exception) {
                Toast.makeText(context, "لا يوجد مشغل خارجي مثبت، سيتم التشغيل بالمشغل الداخلي", Toast.LENGTH_SHORT).show()
            }
        }

        isSportsPlayback = isSports
        activeSeriesTracking = tracking
        activeStream = item
    }

    fun distributeItemsToLists(items: List<MediaItemModel>) {
        liveItemsList.clear()
        movieItemsList.clear()
        seriesItemsList.clear()
        for (it in items) {
            when (it.contentType) {
                "LIVE" -> liveItemsList.add(it)
                "MOVIE" -> movieItemsList.add(it)
                "SERIES" -> seriesItemsList.add(it)
            }
        }
    }

    fun refreshSportsAndEpg(targetSub: SavedSubscription?) {
        scope.launch {
            isRefreshingSports = true
            launch(Dispatchers.IO) {
                val matches = RealSportsDataSource.fetchTodayRealMatches()
                if (matches.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        apiRealMatches.clear()
                        apiRealMatches.addAll(matches)
                    }
                }
            }
            if (targetSub != null && targetSub.type == "XTREAM") {
                launch(Dispatchers.IO) {
                    XtreamEpgRepository.syncFullXmltvFromXtream(targetSub)
                }
            }
            delay(800L)
            isRefreshingSports = false
        }
    }

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
                        distributeItemsToLists(partialList)
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
                distributeItemsToLists(fullItems)
                withContext(Dispatchers.IO) {
                    DevkanVault.saveCachedItems(context, fullItems, updatedSub.id)
                }
                Toast.makeText(context, "اكتمل التحديث (${fullItems.size} عنصر)!", Toast.LENGTH_SHORT).show()
                refreshSportsAndEpg(updatedSub)
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
        openedLiveCategory = null
        movieSmartFilter = "ALL"
        movieServerGroup = "ALL"
        seriesSmartFilter = "ALL"
        seriesServerGroup = "ALL"

        scope.launch {
            val cachedLive = withContext(Dispatchers.IO) { DevkanVault.getCachedItemsByType(context, targetSub.id, "LIVE") }
            liveItemsList.clear()
            liveItemsList.addAll(cachedLive)

            if (cachedLive.isNotEmpty()) {
                Toast.makeText(context, "تم التبديل إلى: ${targetSub.label}", Toast.LENGTH_SHORT).show()
                launch(Dispatchers.IO) {
                    val mov = DevkanVault.getCachedItemsByType(context, targetSub.id, "MOVIE")
                    val ser = DevkanVault.getCachedItemsByType(context, targetSub.id, "SERIES")
                    withContext(Dispatchers.Main) {
                        movieItemsList.clear(); movieItemsList.addAll(mov)
                        seriesItemsList.clear(); seriesItemsList.addAll(ser)
                    }
                }
                refreshSportsAndEpg(targetSub)
            }
            startProgressiveSubscriptionLoad(targetSub, isNewConnection = cachedLive.isEmpty())
        }
    }

    LaunchedEffect(liveItemsList.size, lang, currentSub?.id, categoriesRevision) {
        if (liveItemsList.isEmpty()) {
            cachedLiveChannels = emptyList()
            cachedLiveGroupsWithCounts = emptyList()
            cachedSportsChannels = emptyList()
        } else {
            val snapshot = liveItemsList.toList()
            val subId = currentSub?.id ?: "default"
            val hiddenLive = DevkanVault.getHiddenCategories(context, subId, "LIVE")

            withContext(Dispatchers.Default) {
                val live = snapshot.filter { !hiddenLive.contains(it.group) }
                    .sortedWith(compareByDescending<MediaItemModel> { LanguagePrioritySorter.qualityRank(it.title) }.thenBy { it.title })

                val countsMap = live.groupingBy { it.group }.eachCount()
                val sortedGroupNames = LanguagePrioritySorter.sortGroupsByLanguage(countsMap.keys.toList(), lang)
                val groupsWithCounts = sortedGroupNames.map { it to (countsMap[it] ?: 0) }

                val rawSports = live.filter { isSmartSportChannel(it.group, it.title) }
                val dedupedSports = DevkanPlaylistRepository.deduplicateSportChannels(rawSports)
                    .sortedWith(compareByDescending<MediaItemModel> { LanguagePrioritySorter.qualityRank(it.title) }.thenBy { it.title })

                withContext(Dispatchers.Main) {
                    cachedLiveChannels = live
                    cachedLiveGroupsWithCounts = groupsWithCounts
                    cachedSportsChannels = dedupedSports
                }
            }
        }
    }

    LaunchedEffect(movieItemsList.size, lang, currentSub?.id, categoriesRevision) {
        if (movieItemsList.isEmpty()) {
            cachedHomeLatestMovies = emptyList()
            cachedHome4kMovies = emptyList()
            cachedMovieGroups = listOf("ALL")
            cachedMovieFilters = listOf("ALL" to "الكل")
        } else {
            val snapshot = movieItemsList.toList()
            val subId = currentSub?.id ?: "default"
            val hiddenMovie = DevkanVault.getHiddenCategories(context, subId, "MOVIE")

            withContext(Dispatchers.Default) {
                val movies = snapshot.filter { !hiddenMovie.contains(it.group) }
                val latestMov = movies.sortedWith(
                    compareByDescending<MediaItemModel> { it.addedTimestamp }
                        .thenByDescending { it.year.toIntOrNull() ?: 0 }
                ).take(15)
                val mov4k = movies.filter { it.is4k }.sortedByDescending { it.addedTimestamp }.take(15)
                val movGrps = LanguagePrioritySorter.sortGroupsByLanguage(listOf("ALL") + movies.map { it.group }.distinct(), lang)
                val movFilters = mutableListOf(
                    "ALL" to "الكل",
                    "SUBBED" to "مترجم / مدبلج ✓",
                    "RECENT" to "مضاف حديثاً",
                    "TOP" to "أعلى تقييماً ⭐"
                ).apply {
                    if (movies.any { it.is4k }) add("4K" to "4K UHD")
                    listOf("دراما", "أكشن", "خيال علمي", "رومانسي", "كوميدي", "رعب وإثارة", "تاريخي", "أنمي وعائلي").forEach { g ->
                        if (movies.any { it.genreTag == g }) add(g to g)
                    }
                    listOf("عربي", "كوري", "تركي", "هندي", "أجنبي").forEach { o ->
                        if (movies.any { it.originTag == o }) add(o to "أفلام $o")
                    }
                }

                withContext(Dispatchers.Main) {
                    cachedHomeLatestMovies = latestMov
                    cachedHome4kMovies = mov4k
                    cachedMovieGroups = movGrps
                    cachedMovieFilters = movFilters
                }
            }
        }
    }

    LaunchedEffect(seriesItemsList.size, lang, currentSub?.id, categoriesRevision) {
        if (seriesItemsList.isEmpty()) {
            cachedHomeTopSeries = emptyList()
            cachedSeriesGroups = listOf("ALL")
            cachedSeriesFilters = listOf("ALL" to "الكل")
        } else {
            val snapshot = seriesItemsList.toList()
            val subId = currentSub?.id ?: "default"
            val hiddenSeries = DevkanVault.getHiddenCategories(context, subId, "SERIES")

            withContext(Dispatchers.Default) {
                val series = snapshot.filter { !hiddenSeries.contains(it.group) }
                val topSer = series.sortedWith(
                    compareByDescending<MediaItemModel> { it.rating.toDoubleOrNull() ?: 0.0 }
                        .thenByDescending { it.addedTimestamp }
                ).take(15)
                val serGrps = LanguagePrioritySorter.sortGroupsByLanguage(listOf("ALL") + series.map { it.group }.distinct(), lang)
                val serFilters = mutableListOf(
                    "ALL" to "الكل",
                    "SUBBED" to "مترجم / مدبلج ✓",
                    "RECENT" to "مضاف حديثاً",
                    "TOP" to "أعلى تقييماً ⭐"
                ).apply {
                    listOf("عربي", "كوري", "تركي", "هندي", "أجنبي").forEach { o ->
                        if (series.any { it.originTag == o }) add(o to "مسلسلات $o")
                    }
                    listOf("دراما", "أكشن", "خيال علمي", "رومانسي", "تاريخي", "كوميدي", "رعب وإثارة").forEach { g ->
                        if (series.any { it.genreTag == g }) add(g to g)
                    }
                }

                withContext(Dispatchers.Main) {
                    cachedHomeTopSeries = topSer
                    cachedSeriesGroups = serGrps
                    cachedSeriesFilters = serFilters
                }
            }
        }
    }

    LaunchedEffect(selectedTab, movieItemsList.size, movieSmartFilter, movieServerGroup, movieQuery, activeStream, categoriesRevision) {
        if (selectedTab != 3) return@LaunchedEffect
        if (movieItemsList.isEmpty()) {
            filteredMoviesList = emptyList()
        } else {
            val snapshot = movieItemsList.toList()
            val subId = currentSub?.id ?: "default"
            val hiddenMovie = DevkanVault.getHiddenCategories(context, subId, "MOVIE")
            val q = movieQuery.trim()
            val result = withContext(Dispatchers.Default) {
                val moviesOnly = snapshot.filter { !hiddenMovie.contains(it.group) }
                val byGroup = if (movieServerGroup == "ALL") moviesOnly else moviesOnly.filter { it.group == movieServerGroup }
                val byFilter = when (movieSmartFilter) {
                    "ALL" -> byGroup
                    "SUBBED" -> byGroup.filter { it.subLangTag.isNotEmpty() || DevkanPlaylistRepository.detectSubLangTag(it.group, it.title).isNotEmpty() }
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

    LaunchedEffect(selectedTab, seriesItemsList.size, seriesSmartFilter, seriesServerGroup, seriesQuery, activeStream, categoriesRevision) {
        if (selectedTab != 4) return@LaunchedEffect
        if (seriesItemsList.isEmpty()) {
            filteredSeriesList = emptyList()
        } else {
            val snapshot = seriesItemsList.toList()
            val subId = currentSub?.id ?: "default"
            val hiddenSeries = DevkanVault.getHiddenCategories(context, subId, "SERIES")
            val q = seriesQuery.trim()
            val result = withContext(Dispatchers.Default) {
                val seriesOnly = snapshot.filter { !hiddenSeries.contains(it.group) }
                val byGroup = if (seriesServerGroup == "ALL") seriesOnly else seriesOnly.filter { it.group == seriesServerGroup }
                val byFilter = when (seriesSmartFilter) {
                    "ALL" -> byGroup
                    "SUBBED" -> byGroup.filter { it.subLangTag.isNotEmpty() || DevkanPlaylistRepository.detectSubLangTag(it.group, it.title).isNotEmpty() }
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
            if (cachedLive.isNotEmpty()) {
                liveItemsList.clear()
                liveItemsList.addAll(cachedLive)
            }

            if (DevkanVault.isAutoResumeEnabled(context)) {
                val last = DevkanVault.getLastChannel(context)
                if (last != null) {
                    launchStream(
                        item = MediaItemModel("resume", "0", last.second, last.first, "", "Live", "", "LIVE"),
                        isSports = isSmartSportChannel("Live", last.second)
                    )
                }
            }

            if (cachedLive.isEmpty()) {
                startProgressiveSubscriptionLoad(saved, isNewConnection = true)
            } else {
                launch(Dispatchers.IO) {
                    val mov = DevkanVault.getCachedItemsByType(context, saved.id, "MOVIE")
                    val ser = DevkanVault.getCachedItemsByType(context, saved.id, "SERIES")
                    withContext(Dispatchers.Main) {
                        movieItemsList.clear(); movieItemsList.addAll(mov)
                        seriesItemsList.clear(); seriesItemsList.addAll(ser)
                    }
                }
                delay(2500L)
                if (DevkanVault.shouldRunBackgroundSync(context)) {
                    startProgressiveSubscriptionLoad(saved, isNewConnection = false)
                } else {
                    refreshSportsAndEpg(saved)
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
                    distributeItemsToLists(imported)
                    withContext(Dispatchers.IO) { DevkanVault.saveCachedItems(context, imported, localSub.id) }
                    refreshSportsAndEpg(localSub)
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
        layoutResetKey++
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
                    startProgressiveSubscriptionLoad(newSub, isNewConnection = true)
                }
            )
        }
        return
    }

    key(layoutResetKey) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF07070A))) {
            Scaffold(
                containerColor = Color(0xFF07070A),
                topBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
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

                            val fallbackRealMatch = remember(combinedRealMatches.size) {
                                combinedRealMatches.firstOrNull { it.isLive } ?: combinedRealMatches.firstOrNull()
                            }

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
                                    homePills.forEach { pill ->
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
                                                .padding(horizontal = 14.dp, vertical = 7.dp)
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
                                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "🏆 ${formatCleanLeagueName(fallbackRealMatch.league)}",
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
                                                    Text(fallbackRealMatch.homeTeam, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFF222232), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(fallbackRealMatch.scoreText, color = goldLight, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                                }
                                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                                                    Text(fallbackRealMatch.awayTeam, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    if (fallbackRealMatch.awayLogo.isNotEmpty()) {
                                                        Spacer(Modifier.width(6.dp))
                                                        AsyncImage(model = fallbackRealMatch.awayLogo, contentDescription = null, modifier = Modifier.size(28.dp))
                                                    }
                                                }
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
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

                                val liveCount = combinedRealMatches.count { it.isLive }
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
                                            Text("رياضة • استوديو المباريات المباشرة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                text = if (liveCount > 0) "🔴 $liveCount مباراة جارية الآن • اختيار ذكي للقناة" else "⚽ تصفح المباريات المباشرة والقنوات الرياضية",
                                                color = Color(0xFF4ADE80),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF4ADE80))
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
                                                    item = MediaItemModel("last", "0", lastCh.second, lastCh.first, "", "Live", "", "LIVE"),
                                                    isSports = isSmartSportChannel("Live", lastCh.second)
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
                                            onMoreClick = { selectedTab = 3 },
                                            onItemClick = { selectedMovieForDetails = it }
                                        )
                                    }
                                }

                                if (cachedHomeTopSeries.isNotEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                                        CinemaHorizontalRow(
                                            title = strings.featuredSeries,
                                            items = cachedHomeTopSeries,
                                            onMoreClick = { selectedTab = 4 },
                                            onItemClick = { selectedSeriesForDetails = it }
                                        )
                                    }
                                }

                                if (cachedHome4kMovies.isNotEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
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
                            val activeCategory = openedLiveCategory
                            if (activeCategory == null) {
                                val filteredGroupsList = remember(cachedLiveGroupsWithCounts, liveCategoryKindFilter, liveSearchQuery) {
                                    val q = liveSearchQuery.trim()
                                    cachedLiveGroupsWithCounts.filter { (grpName, _) ->
                                        val kindMatches = liveCategoryKindFilter == "ALL" ||
                                            LanguagePrioritySorter.detectCategoryKind(grpName) == liveCategoryKindFilter
                                        val queryMatches = q.isEmpty() || grpName.contains(q, ignoreCase = true)
                                        kindMatches && queryMatches
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
                                                text = if (XtreamEpgRepository.programsByEpgId.isNotEmpty())
                                                    "${cachedLiveGroupsWithCounts.size} باقة • دليل EPG حي مفعّل ✓"
                                                else "${cachedLiveGroupsWithCounts.size} باقة • ${cachedLiveChannels.size} قناة",
                                                color = if (XtreamEpgRepository.programsByEpgId.isNotEmpty()) Color(0xFF4ADE80) else Color(0xFF9CA3AF),
                                                fontSize = 11.5.sp
                                            )
                                        }
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
                                        value = liveSearchQuery,
                                        onValueChange = { liveSearchQuery = it },
                                        placeholder = { Text("ابحث عن باقة (مثل beIN, MBC, الكأس, العراق)...", fontSize = 12.sp) },
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

                                    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val kinds = listOf(
                                            "ALL" to "الكل",
                                            "SPORT" to "🏆 رياضة",
                                            "CINEMA" to "🎬 أفلام ومسلسلات",
                                            "NEWS" to "📰 أخبار ووثائقي",
                                            "KIDS" to "🧸 أطفال"
                                        )
                                        items(kinds, key = { it.first }) { (code, title) ->
                                            val selected = liveCategoryKindFilter == code
                                            FilterChip(
                                                selected = selected,
                                                onClick = { liveCategoryKindFilter = code },
                                                label = { Text(title, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                                            )
                                        }
                                    }

                                    LazyColumn(state = liveCategoriesGridState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                    Text("⭐ قنواتي المفضلة", color = goldLight, fontWeight = FontWeight.Black, fontSize = 14.5.sp)
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
                                            val kind = LanguagePrioritySorter.detectCategoryKind(grpName)
                                            val (catIcon, catTint) = when (kind) {
                                                "SPORT" -> Icons.Default.EmojiEvents to Color(0xFFFBBF24)
                                                "CINEMA" -> Icons.Default.MovieFilter to Color(0xFFA78BFA)
                                                "NEWS" -> Icons.Default.Newspaper to Color(0xFF38BDF8)
                                                "KIDS" -> Icons.Default.ChildCare to Color(0xFF34D399)
                                                "ISLAM" -> Icons.Default.Mosque to Color(0xFF4ADE80)
                                                "MUSIC" -> Icons.Default.MusicNote to Color(0xFFF472B6)
                                                else -> Icons.Default.FolderSpecial to gold
                                            }

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
                                                            .background(catTint.copy(alpha = 0.15f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(catIcon, contentDescription = null, tint = catTint, modifier = Modifier.size(20.dp))
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
                                                        "FAVORITES" -> "⭐ قنواتي المفضلة"
                                                        else -> activeCategory
                                                    },
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text("${channelsInOpenedGroup.size} قناة (مرتبة حسب الجودة)", color = goldLight, fontSize = 11.sp)
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
                                            val qRank = LanguagePrioritySorter.qualityRank(ch.title)
                                            val qBadge = when (qRank) {
                                                4 -> "4K"
                                                3 -> "FHD"
                                                1 -> "SD"
                                                else -> "HD"
                                            }
                                            val qColor = when (qRank) {
                                                4 -> goldLight
                                                3 -> Color(0xFF4ADE80)
                                                1 -> Color(0xFF94A3B8)
                                                else -> Color(0xFF38BDF8)
                                            }
                                            val liveProgram = remember(ch.id, XtreamEpgRepository.lastEpgUpdate.value) {
                                                XtreamEpgRepository.getChannelNowAndNext(ch)
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(Color(0xFF131520))
                                                    .border(1.dp, if (isFav) gold.copy(alpha = 0.45f) else Color(0xFF212436), RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        launchStream(ch, isSports = isSmartSportChannel(ch.group, ch.title))
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
                                                        if (liveProgram != null) {
                                                            Text(
                                                                text = "🔴 الآن: ${liveProgram.currentProgram}",
                                                                color = Color(0xFF4ADE80),
                                                                fontSize = 11.5.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        } else {
                                                            Text(ch.group, color = Color(0xFF9CA3AF), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                    }
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(qColor.copy(alpha = 0.16f))
                                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(qBadge, color = qColor, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            val added = DevkanVault.toggleFavoriteChannel(context, currentSub?.id ?: "default", ch.id)
                                                            favoritesRevision++
                                                            Toast.makeText(context, if (added) "أضيفت للمفضلة ⭐" else "أزيلت من المفضلة", Toast.LENGTH_SHORT).show()
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
                        }

                        2 -> {
                            DevkanSportsStudioScreen(
                                matches = combinedRealMatches,
                                quickSportsChannels = cachedSportsChannels,
                                lang = lang,
                                isRefreshing = isRefreshingSports,
                                onRefresh = { refreshSportsAndEpg(currentSub) },
                                onSmartPlayChannel = { match, ch ->
                                    launchStream(ch.copy(title = "${match.homeTeam} vs ${match.awayTeam} • ${ch.title}"), isSports = true)
                                },
                                onOpenChannelPicker = { match ->
                                    selectedMatchForChannelPicker = match
                                },
                                onQuickChannelClick = { ch ->
                                    launchStream(ch, isSports = true)
                                }
                            )
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

                                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                sampleTestUrl = liveItemsList.firstOrNull()?.url ?: "https://www.google.com"
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
                        launchStream(item)
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
                        DevkanVault.saveVodProgress(context, epItem.id, 1000L, 60_000L, seriesId, episodeNum, seasonNum)
                        launchStream(epItem, tracking = Triple(seriesId, seasonNum, episodeNum))
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
                                cachedSportsChannels = emptyList()
                                apiRealMatches.clear()
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
                val realEpgSlots = remember(cachedLiveChannels.size, XtreamEpgRepository.lastEpgUpdate.value) {
                    cachedLiveChannels.take(150).map { ch ->
                        XtreamEpgRepository.getChannelNowAndNext(ch) ?: EpgSlot(
                            channelId = ch.id,
                            channelName = ch.title,
                            channelLogo = ch.logo,
                            currentProgram = "البث المباشر (${ch.group})",
                            nextProgram = "متابعة البث المستمر",
                            timeRange = "LIVE",
                            progress = 0.5f
                        )
                    }
                }
                EpgTimelineGridSheet(
                    slots = realEpgSlots,
                    lang = lang,
                    onPlayChannel = { chName ->
                        val found = cachedLiveChannels.firstOrNull { it.title == chName }
                        showEpgSheet = false
                        if (found != null) {
                            launchStream(found, isSports = isSmartSportChannel(found.group, found.title))
                        }
                    },
                    onClose = { showEpgSheet = false }
                )
            }

            if (activeStream != null) {
                val st = activeStream!!
                val subIdNow = currentSub?.id ?: "default"
                DevkanHybridPlayerScreen(
                    streamUrl = st.url,
                    fallbackUrl = st.fallbackUrl,
                    streamTitle = st.title,
                    isVodOrSeries = st.contentType != "LIVE",
                    streamId = st.id,
                    subId = subIdNow,
                    isFavorite = favoriteIds.contains(st.id),
                    onToggleFavorite = {
                        val added = DevkanVault.toggleFavoriteChannel(context, subIdNow, st.id)
                        favoritesRevision++
                        Toast.makeText(context, if (added) "أضيفت للمفضلة ⭐" else "أزيلت من المفضلة", Toast.LENGTH_SHORT).show()
                    },
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
                                            else -> launchStream(item, isSports = isSmartSportChannel(item.group, item.title))
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
                }.take(30)
            } else {
                val directCh = if (match.directChannelId.isNotEmpty()) {
                    cachedSportsChannels.filter { it.id == match.directChannelId }
                } else emptyList()

                val keywords = listOf(
                    match.suggestedBroadcaster.substringBefore(" ").lowercase(),
                    "bein", "ssc", "alkass", "كأس", "كاس", "alwan sport", "ألوان الرياضية", "الوان الرياضية",
                    "ad sport", "أبو ظبي", "rabiaa", "الرابعة", "kuwait sport"
                )
                val prioritized = cachedSportsChannels.filter { ch ->
                    val combined = "${ch.group} ${ch.title}".lowercase()
                    keywords.any { combined.contains(it) }
                }
                (directCh + prioritized + cachedSportsChannels).distinctBy { it.id }.take(25)
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
                        placeholder = { Text("ابحث عن القناة الرياضية الناقلة...", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            text = {
                if (matchingChannels.isEmpty()) {
                    Text("لم يتم العثور على قنوات رياضية مطابقة في اشتراكك.", color = Color.Gray, fontSize = 13.sp)
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
                                        launchStream(ch.copy(title = "${match.homeTeam} vs ${match.awayTeam} • ${ch.title}"), isSports = true)
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
