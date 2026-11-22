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

// ═══════════════════════════════════════════════════════════════
//  قسم الرياضة: تصنيف قنوات الاشتراك (بدون أي اعتماد على بادئات أو رموز دول)
// ═══════════════════════════════════════════════════════════════

private const val Q_4K = 0
private const val Q_FHD = 1
private const val Q_HD = 2
private const val Q_SD = 3

private class SportsBrand(val id: String, val badge: String, val accent: Color, patterns: List<String>) {
    val regex: Regex = Regex(patterns.joinToString("|"))
}

private object SportsBrands {
    val OTHER = SportsBrand("OTHER", "SPORT", Color(0xFFD4AF37), listOf("(?!)"))

    // الأنماط مكتوبة على النص بعد التطبيع (حروف صغيرة، توحيد الألف والياء والتاء المربوطة)
    val list: List<SportsBrand> = listOf(
        SportsBrand("BEIN", "beIN", Color(0xFF8B5CF6), listOf("(?<![a-z])bein(?!g)", "بي\\s*ان", "بي\\s*اين", "بين\\s*سبورت")),
        SportsBrand("SSC", "SSC", Color(0xFF22C55E), listOf("(?<![a-z])ssc(?![a-z])", "اس\\s*اس\\s*سي")),
        SportsBrand("KASS", "KASS", Color(0xFFB91C1C), listOf("al[\\s_-]*kass", "(?<![a-z])kass(?![a-z])", "الكاس")),
        SportsBrand("AD", "AD", Color(0xFFF59E0B), listOf("abu[\\s_-]*dhabi[\\s_-]*sport", "(?<![a-z])ad[\\s_-]*sports?(?![a-z])", "ابو\\s*ظبي\\s*الرياض")),
        SportsBrand("DUBAI", "DXB", Color(0xFFEF4444), listOf("dubai[\\s_-]*sport", "دبي\\s*الرياض")),
        SportsBrand("RABIA", "4", Color(0xFF14B8A6), listOf("الرابعه", "(?<![a-z])al[\\s_-]*rabia", "alrabia")),
        SportsBrand("ESPN", "ESPN", Color(0xFFDC2626), listOf("(?<![a-z])espn")),
        SportsBrand("SKY", "SKY", Color(0xFF0EA5E9), listOf("(?<![a-z])sky[\\s_-]*sports?")),
        SportsBrand("TNT", "TNT", Color(0xFFF97316), listOf("(?<![a-z])(tnt|bt)[\\s_-]*sports?")),
        SportsBrand("FOX", "FOX", Color(0xFF3B82F6), listOf("fox[\\s_-]*sports?")),
        SportsBrand("DAZN", "DAZN", Color(0xFFFACC15), listOf("(?<![a-z])dazn")),
        SportsBrand("EURO", "EURO", Color(0xFF2563EB), listOf("eurosport", "يوروسبورت")),
        SportsBrand("STV", "STV", Color(0xFF10B981), listOf("(?<![a-z])sport[\\s_-]*tv(?![a-z])")),
        SportsBrand("CANAL", "C+", Color(0xFF94A3B8), listOf("canal[\\s_+-]*(sport|foot)")),
        SportsBrand("RMC", "RMC", Color(0xFFEC4899), listOf("(?<![a-z])rmc[\\s_-]*sport"))
    )

    private val map: Map<String, SportsBrand> = (list + OTHER).associateBy { it.id }
    fun byId(id: String): SportsBrand = map[id] ?: OTHER

    fun detect(text: String): SportsBrand? {
        if (text.isEmpty()) return null
        for (b in list) if (b.regex.containsMatchIn(text)) return b
        return null
    }
}

private data class SportsFolder(
    val key: String,
    val brand: String,
    val quality: Int,
    val channels: List<MediaItemModel>
)

private data class SportsCatalog(
    val all: List<MediaItemModel>,
    val folders: List<SportsFolder>
)

private fun normSports(s: String): String {
    val sb = StringBuilder(s.length)
    for (c in s.lowercase()) {
        when (c) {
            'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
            'ى' -> sb.append('ي')
            'ة' -> sb.append('ه')
            'ـ' -> { }
            else -> { if (c.code !in 0x064B..0x065F) sb.append(c) }
        }
    }
    return sb.toString()
}

private val natTokens = Regex("\\d+|\\D+")

private fun compareTokens(ta: List<String>, tb: List<String>): Int {
    val n = minOf(ta.size, tb.size)
    for (i in 0 until n) {
        val x = ta[i]
        val y = tb[i]
        val nx = x.toLongOrNull()
        val ny = y.toLongOrNull()
        val c = if (nx != null && ny != null) nx.compareTo(ny) else x.compareTo(y)
        if (c != 0) return c
    }
    return ta.size.compareTo(tb.size)
}

private fun sortNatural(list: List<MediaItemModel>): List<MediaItemModel> {
    return list
        .map { it to natTokens.findAll(it.title.lowercase()).map { m -> m.value }.toList() }
        .sortedWith { a, b -> compareTokens(a.second, b.second) }
        .map { it.first }
}

private object SportsClassifier {
    private val exclusions = listOf(
        "kitchen", "مطبخ", "gourmet", "cooking", "cook", "طبخ",
        "kids", "kidz", "اطفال", "cartoon", "كرتون", "junior", "جونيور",
        "movie", "film", "series", "drama", "دراما", "cinema", "سينما", "مسلسل", "افلام", "فيلم",
        "xxx", "adult"
    )

    private val generic = Regex(
        "(?<![a-z])sports?(?![a-z])|sport\\s*digital|رياضي(?!ات)|رياضه|سبورت|(?<![a-z])(nba|ufc|wwe|motogp)(?![a-z])"
    )

    private val q4k = Regex("(?<![a-z0-9])(4k|uhd|2160p?)(?![a-z0-9])")
    private val qFhd = Regex("(?<![a-z0-9])(fhd|1080[pi]?|full[\\s_-]*hd)(?![a-z0-9])")
    private val qHd = Regex("(?<![a-z])hd(?![a-z])|(?<![a-z0-9])720p?(?![a-z0-9])")
    private val qSd = Regex("(?<![a-z0-9])(sd|480p?|360p?)(?![a-z0-9])")

    private fun isExcluded(t: String): Boolean {
        for (w in exclusions) if (t.contains(w)) return true
        return false
    }

    private fun qualityOf(t: String): Int? = when {
        t.isEmpty() -> null
        q4k.containsMatchIn(t) -> Q_4K
        qFhd.containsMatchIn(t) -> Q_FHD
        qHd.containsMatchIn(t) -> Q_HD
        qSd.containsMatchIn(t) -> Q_SD
        else -> null
    }

    fun build(items: List<MediaItemModel>): SportsCatalog {
        val buckets = LinkedHashMap<String, ArrayList<MediaItemModel>>()
        val all = ArrayList<MediaItemModel>()
        for (ch in items) {
            val title = normSports(ch.title)
            val group = normSports(ch.group)
            if (isExcluded(title) || isExcluded(group)) continue

            val brand = SportsBrands.detect(title) ?: SportsBrands.detect(group)
            if (brand == null && !generic.containsMatchIn(title)) continue

            val brandId = brand?.id ?: SportsBrands.OTHER.id
            val quality = qualityOf(title) ?: qualityOf(group) ?: Q_SD
            buckets.getOrPut("$brandId|$quality") { ArrayList() }.add(ch)
            all.add(ch)
        }
        val folders = buckets.map { (key, list) ->
            val parts = key.split("|")
            SportsFolder(key, parts[0], parts[1].toInt(), sortNatural(list))
        }
        return SportsCatalog(all, folders)
    }
}

private fun sportsLangKey(lang: String): String = if (lang in listOf("en", "de", "tr", "fr", "ku")) lang else "ar"

private fun sportsBrandOrder(lang: String): List<String> {
    val order = when (sportsLangKey(lang)) {
        "en" -> listOf("SKY", "TNT", "ESPN", "BEIN", "DAZN", "EURO", "FOX", "SSC", "KASS", "AD", "DUBAI", "RABIA", "STV", "CANAL", "RMC")
        "de" -> listOf("SKY", "DAZN", "EURO", "ESPN", "BEIN", "TNT", "FOX", "SSC", "KASS", "AD", "DUBAI", "RABIA", "STV", "CANAL", "RMC")
        "tr" -> listOf("BEIN", "EURO", "DAZN", "ESPN", "SKY", "TNT", "FOX", "SSC", "KASS", "AD", "DUBAI", "RABIA", "STV", "CANAL", "RMC")
        "fr" -> listOf("BEIN", "CANAL", "RMC", "EURO", "DAZN", "ESPN", "SKY", "TNT", "FOX", "SSC", "KASS", "AD", "DUBAI", "RABIA", "STV")
        else -> listOf("BEIN", "SSC", "KASS", "AD", "DUBAI", "RABIA", "ESPN", "SKY", "TNT", "FOX", "DAZN", "EURO", "STV", "CANAL", "RMC")
    }
    val rest = SportsBrands.list.map { it.id }.filter { it !in order }
    return order + rest + "OTHER"
}

private fun sportsBrandName(id: String, lang: String): String {
    val l = sportsLangKey(lang)
    return when (id) {
        "BEIN" -> when (l) { "ar" -> "بي إن سبورتس"; "ku" -> "بی ئین سپۆرتس"; else -> "beIN SPORTS" }
        "SSC" -> when (l) { "ar" -> "SSC الرياضية"; "ku" -> "SSC وەرزشی"; "de" -> "SSC Sport"; "tr" -> "SSC Spor"; else -> "SSC Sports" }
        "KASS" -> when (l) { "ar" -> "الكأس"; "ku" -> "کاس"; else -> "Al Kass" }
        "AD" -> when (l) { "ar" -> "أبوظبي الرياضية"; "ku" -> "ئەبوزەبی وەرزشی"; "de" -> "Abu Dhabi Sport"; "tr" -> "Abu Dabi Spor"; else -> "Abu Dhabi Sports" }
        "DUBAI" -> when (l) { "ar" -> "دبي الرياضية"; "ku" -> "دوبەی وەرزشی"; "de" -> "Dubai Sport"; "tr" -> "Dubai Spor"; else -> "Dubai Sports" }
        "RABIA" -> when (l) { "ar", "ku" -> "الرابعة"; else -> "Al Rabia" }
        "ESPN" -> "ESPN"
        "SKY" -> when (l) { "ar" -> "سكاي سبورتس"; "de" -> "Sky Sport"; else -> "Sky Sports" }
        "TNT" -> "TNT / BT Sports"
        "FOX" -> "Fox Sports"
        "DAZN" -> "DAZN"
        "EURO" -> if (l == "ar") "يوروسبورت" else "Eurosport"
        "STV" -> "Sport TV"
        "CANAL" -> "Canal+ Sport"
        "RMC" -> "RMC Sport"
        else -> when (l) {
            "en" -> "More Sports Channels"
            "de" -> "Weitere Sportsender"
            "tr" -> "Diğer Spor Kanalları"
            "fr" -> "Autres chaînes sportives"
            "ku" -> "کەناڵە وەرزشییەکانی تر"
            else -> "قنوات رياضية أخرى"
        }
    }
}

private fun sportsQualityLabel(q: Int): String = when (q) {
    Q_4K -> "4K"
    Q_FHD -> "FHD"
    Q_HD -> "HD"
    else -> "SD"
}

private fun sportsQualityDesc(q: Int, lang: String): String {
    val l = sportsLangKey(lang)
    return when (q) {
        Q_4K -> when (l) { "ar" -> "فائقة الوضوح"; "ku" -> "ئەلترا HD"; else -> "Ultra HD" }
        Q_FHD -> when (l) { "ar" -> "دقة كاملة"; else -> "Full HD" }
        Q_HD -> when (l) { "ar" -> "عالية الدقة"; "ku" -> "وردی بەرز"; "de" -> "Hohe Auflösung"; "tr" -> "Yüksek Çözünürlük"; "fr" -> "Haute définition"; else -> "High Definition" }
        else -> when (l) { "ar" -> "دقة قياسية"; "ku" -> "ستانداردی"; "tr" -> "Standart"; else -> "Standard" }
    }
}

private fun sportsQualityColor(q: Int): Color = when (q) {
    Q_4K -> Color(0xFFFDE047)
    Q_FHD -> Color(0xFF22C55E)
    Q_HD -> Color(0xFF3B82F6)
    else -> Color(0xFF64748B)
}

private data class SportsUi(
    val channelsWord: String,
    val foldersWord: String,
    val searchHint: String,
    val searchInFolder: String,
    val emptyTitle: String,
    val emptyHint: String,
    val noResults: String
)

private fun sportsUi(lang: String): SportsUi = when (sportsLangKey(lang)) {
    "en" -> SportsUi("channels", "folders", "Search sports channels...", "Search in this folder...", "No sports channels found", "No channel in your subscription matches known sports channels.", "No matching channels")
    "de" -> SportsUi("Sender", "Ordner", "Sportsender suchen...", "In diesem Ordner suchen...", "Keine Sportsender gefunden", "In deinem Abo wurde kein bekannter Sportsender gefunden.", "Keine passenden Sender")
    "tr" -> SportsUi("kanal", "klasör", "Spor kanallarında ara...", "Bu klasörde ara...", "Spor kanalı bulunamadı", "Aboneliğinizde bilinen bir spor kanalı bulunamadı.", "Eşleşen kanal yok")
    "fr" -> SportsUi("chaînes", "dossiers", "Rechercher une chaîne sport...", "Rechercher dans ce dossier...", "Aucune chaîne de sport trouvée", "Aucune chaîne sportive connue dans votre abonnement.", "Aucune chaîne correspondante")
    "ku" -> SportsUi("کەناڵ", "فۆڵدەر", "گەڕان لە کەناڵە وەرزشییەکان...", "گەڕان لەم فۆڵدەرەدا...", "هیچ کەناڵێکی وەرزشی نەدۆزرایەوە", "لە بەشداریەکەتدا کەناڵی وەرزشی ناسراو نەدۆزرایەوە.", "هیچ کەناڵێکی گونجاو نییە")
    else -> SportsUi("قناة", "مجلد", "ابحث في القنوات الرياضية...", "ابحث داخل هذا المجلد...", "لا توجد قنوات رياضية", "لم نجد في اشتراكك قنوات رياضية معروفة.", "لا توجد قنوات مطابقة")
}

@Composable
private fun SportsMessageCard(title: String, sub: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF12141F))
            .border(1.dp, Color(0xFF202436), RoundedCornerShape(16.dp))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(32.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (sub.isNotEmpty()) Text(sub, color = Color(0xFF9CA3AF), fontSize = 11.5.sp, textAlign = TextAlign.Center, lineHeight = 17.sp)
    }
}

@Composable
private fun SportsQualityChip(q: Int) {
    val c = sportsQualityColor(q)
    Box(
        modifier = Modifier
            .width(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(c.copy(alpha = 0.18f))
            .border(1.dp, c.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(sportsQualityLabel(q), color = c, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SportsBrandBadge(brand: SportsBrand, size: Int = 36) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(brand.accent.copy(alpha = 0.18f))
            .border(1.dp, brand.accent.copy(alpha = 0.7f), RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(brand.badge, color = brand.accent, fontWeight = FontWeight.Black, fontSize = 9.5.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SportsChannelRow(
    ch: MediaItemModel,
    isFav: Boolean,
    onPlay: () -> Unit,
    onFav: () -> Unit
) {
    val context = LocalContext.current
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131520))
            .border(1.dp, if (isFav) gold.copy(alpha = 0.45f) else Color(0xFF212436), RoundedCornerShape(14.dp))
            .clickable { onPlay() }
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
        IconButton(onClick = onFav, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Fav",
                tint = if (isFav) goldLight else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SportsSearchField(value: String, hint: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(hint, fontSize = 12.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SportsFolderView(
    folder: SportsFolder,
    lang: String,
    ui: SportsUi,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (MediaItemModel) -> Unit,
    onPlay: (MediaItemModel) -> Unit,
    modifier: Modifier
) {
    val goldLight = Color(0xFFFDE047)
    val brand = SportsBrands.byId(folder.brand)
    val q = query.trim()
    val shown = remember(folder, q) {
        if (q.isEmpty()) folder.channels else folder.channels.filter { it.title.contains(q, ignoreCase = true) }
    }

    Column(modifier = modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF181A28), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = goldLight, modifier = Modifier.size(18.dp))
            }
            SportsBrandBadge(brand, 36)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${sportsBrandName(folder.brand, lang)} • ${sportsQualityLabel(folder.quality)}",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("${shown.size} ${ui.channelsWord} • ${sportsQualityDesc(folder.quality, lang)}", color = goldLight, fontSize = 11.sp)
            }
            SportsQualityChip(folder.quality)
        }

        SportsSearchField(value = query, hint = ui.searchInFolder, onChange = onQueryChange)

        if (shown.isEmpty()) {
            SportsMessageCard(ui.noResults, "")
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(shown, key = { i, ch -> "${ch.id}#$i" }, contentType = { _, _ -> "sports_channel" }) { _, ch ->
                    SportsChannelRow(
                        ch = ch,
                        isFav = isFavorite(ch.id),
                        onPlay = { onPlay(ch) },
                        onFav = { onToggleFavorite(ch) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SportsMainView(
    title: String,
    lang: String,
    ui: SportsUi,
    catalog: SportsCatalog,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (MediaItemModel) -> Unit,
    onPlay: (MediaItemModel) -> Unit,
    modifier: Modifier
) {
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)
    val q = query.trim()

    val sections = remember(catalog, lang) {
        val byBrand = catalog.folders.groupBy { it.brand }
        sportsBrandOrder(lang)
            .filter { byBrand.containsKey(it) }
            .map { id -> id to byBrand.getValue(id).sortedBy { it.quality } }
    }

    val results = remember(catalog, q) {
        if (q.isEmpty()) emptyList() else catalog.all.filter { it.title.contains(q, ignoreCase = true) }.take(150)
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "sports_header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(goldLight.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = goldLight, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(
                        text = "${catalog.all.size} ${ui.channelsWord} • ${catalog.folders.size} ${ui.foldersWord}",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        item(key = "sports_search") {
            SportsSearchField(value = query, hint = ui.searchHint, onChange = onQueryChange)
        }

        if (catalog.all.isEmpty()) {
            item(key = "sports_empty") { SportsMessageCard(ui.emptyTitle, ui.emptyHint) }
        } else if (q.isNotEmpty()) {
            if (results.isEmpty()) {
                item(key = "sports_no_results") { SportsMessageCard(ui.noResults, "") }
            } else {
                itemsIndexed(results, key = { i, ch -> "r_${ch.id}#$i" }, contentType = { _, _ -> "sports_channel" }) { _, ch ->
                    SportsChannelRow(
                        ch = ch,
                        isFav = isFavorite(ch.id),
                        onPlay = { onPlay(ch) },
                        onFav = { onToggleFavorite(ch) }
                    )
                }
            }
        } else {
            items(sections, key = { "sec_${it.first}" }) { (brandId, folders) ->
                val brand = SportsBrands.byId(brandId)
                val total = folders.sumOf { it.channels.size }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.verticalGradient(listOf(brand.accent.copy(alpha = 0.10f), Color(0xFF12141F))))
                        .border(1.dp, brand.accent.copy(alpha = 0.40f), RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SportsBrandBadge(brand, 38)
                        Text(
                            text = sportsBrandName(brandId, lang),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(brand.accent.copy(alpha = 0.22f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("$total", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    folders.forEach { f ->
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1E2133)))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenFolder(f.key) }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SportsQualityChip(f.quality)
                            Text(
                                text = sportsQualityDesc(f.quality, lang),
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF1D2030))
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text("${f.channels.size}", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = gold.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
        item(key = "sports_bottom_space") { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SportsHub(
    title: String,
    lang: String,
    catalog: SportsCatalog?,
    openedFolderKey: String?,
    onOpenFolder: (String?) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (MediaItemModel) -> Unit,
    onPlay: (MediaItemModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val ui = remember(lang) { sportsUi(lang) }

    if (catalog == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFD4AF37), strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
        }
        return
    }

    val folder = if (openedFolderKey != null) catalog.folders.firstOrNull { it.key == openedFolderKey } else null

    if (openedFolderKey != null && folder == null) {
        LaunchedEffect(openedFolderKey) { onOpenFolder(null) }
    }

    if (folder != null) {
        SportsFolderView(
            folder = folder,
            lang = lang,
            ui = ui,
            query = query,
            onQueryChange = onQueryChange,
            onBack = {
                onQueryChange("")
                onOpenFolder(null)
            },
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onPlay = onPlay,
            modifier = modifier
        )
    } else {
        SportsMainView(
            title = title,
            lang = lang,
            ui = ui,
            catalog = catalog,
            query = query,
            onQueryChange = onQueryChange,
            onOpenFolder = { key ->
                onQueryChange("")
                onOpenFolder(key)
            },
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onPlay = onPlay,
            modifier = modifier
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  بقية التطبيق
// ═══════════════════════════════════════════════════════════════

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
    var homeFilter by remember { mutableStateOf("ALL") }

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

    var openedSportsFolder by remember { mutableStateOf<String?>(null) }
    var sportsQuery by remember { mutableStateOf("") }
    var sportsCatalog by remember { mutableStateOf<SportsCatalog?>(null) }
    var sportsSource by remember { mutableStateOf<List<MediaItemModel>?>(null) }

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
        openedSportsFolder = null
        sportsQuery = ""
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

    // تصنيف القنوات الرياضية: يعمل فقط عند فتح "الرياضة" ويُعاد عند تغيّر قنوات الاشتراك
    LaunchedEffect(cachedLiveChannels, homeFilter) {
        if (homeFilter != "SPORTS") return@LaunchedEffect
        if (sportsSource === cachedLiveChannels && sportsCatalog != null) return@LaunchedEffect
        val src = cachedLiveChannels
        val built = withContext(Dispatchers.Default) { SportsClassifier.build(src) }
        sportsCatalog = built
        sportsSource = src
    }

    BackHandler(
        enabled = activeStream == null &&
            !showConnectionsManager &&
            selectedMovieForDetails == null &&
            selectedSeriesForDetails == null &&
            !showQuickSearch
    ) {
        if (selectedTab == 0 && homeFilter == "SPORTS") {
            if (openedSportsFolder != null) {
                openedSportsFolder = null
                sportsQuery = ""
            } else {
                homeFilter = "ALL"
                sportsQuery = ""
            }
        } else if (selectedTab == 1 && openedLiveCategory != null) {
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
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                homePills.forEach { (key, label) ->
                                    val isSelected = key == homeFilter
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) gold else Color(0xFF14141F))
                                            .border(1.dp, if (isSelected) gold else Color(0xFF28283A), RoundedCornerShape(50))
                                            .clickable {
                                                when (key) {
                                                    "ALL" -> {
                                                        homeFilter = "ALL"
                                                        openedSportsFolder = null
                                                        sportsQuery = ""
                                                    }
                                                    "SPORTS" -> homeFilter = "SPORTS"
                                                    "LIVE" -> { homeFilter = "ALL"; selectedTab = 1 }
                                                    "MOVIES" -> { homeFilter = "ALL"; selectedTab = 2 }
                                                    "SERIES" -> { homeFilter = "ALL"; selectedTab = 3 }
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

                            if (homeFilter == "SPORTS") {
                                SportsHub(
                                    title = strings.sports,
                                    lang = lang,
                                    catalog = sportsCatalog,
                                    openedFolderKey = openedSportsFolder,
                                    onOpenFolder = { openedSportsFolder = it },
                                    query = sportsQuery,
                                    onQueryChange = { sportsQuery = it },
                                    isFavorite = { id -> favoriteIds.contains(id) },
                                    onToggleFavorite = { ch ->
                                        val added = DevkanVault.toggleFavoriteChannel(context, currentSub?.id ?: "default", ch.id)
                                        favoritesRevision++
                                        Toast.makeText(context, if (added) "أضيفت للمفضلة" else "أزيلت من المفضلة", Toast.LENGTH_SHORT).show()
                                    },
                                    onPlay = { launchStream(it) },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .verticalScroll(homeScrollState)
                                        .padding(bottom = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
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
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = goldLight, modifier = Modifier.siz
