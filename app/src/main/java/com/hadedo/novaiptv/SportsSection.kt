package com.hadedo.novaiptv

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ───────────────────────── الإعدادات ─────────────────────────
object SportsConfig {
    const val MATCHES_JSON_URL =
        "https://raw.githubusercontent.com/Hadedo/NovaIPTV/main/matches.json"

    const val REMINDER_LEAD_MIN = 10

    /** تحديث تلقائي أثناء فتح التبويب (بالمللي ثانية). */
    fun autoRefreshMs(): Long = 300_000L
}

// ───────────────────────── النماذج ─────────────────────────
enum class MatchPhase { LIVE, UPCOMING, FINISHED, OTHER }

data class SportsMatch(
    val id: String,
    val league: String,
    val home: String,
    val away: String,
    val kickoffMs: Long,
    val phase: MatchPhase,
    val homeScore: Int?,
    val awayScore: Int?,
    val priority: Int
)

sealed class SportsState {
    object Loading : SportsState()
    object NoKey : SportsState()
    object Empty : SportsState()
    data class Error(val msg: String) : SportsState()
    data class Ready(val matches: List<SportsMatch>) : SportsState()
}

// ───────────────────────── النصوص (6 لغات) ─────────────────────────
data class SportsStrings(
    val todayMatches: String, val finished: String, val noKey: String, val noKeyHint: String,
    val noMatches: String, val error: String, val errorHint: String, val sportsChannels: String,
    val noChannels: String, val noChannelsHint: String, val searchHint: String,
    val reminderOn: String, val reminderOff: String, val permDenied: String, val startsIn: String
)

fun sportsStrings(lang: String): SportsStrings = when (lang) {
    "en" -> SportsStrings("Today's Matches", "FT", "Matches source not enabled", "Add an API key in SportsConfig to show matches. Your sports channels are listed below.", "No matches today", "Couldn't load matches", "Check your connection or API key, then refresh.", "Sports Channels", "No sports channels found", "No channel or package names in your subscription look sports-related.", "Search sports channels...", "Reminder set", "Reminder removed", "Allow notifications to enable reminders", "Starts in %d min")
    "de" -> SportsStrings("Heutige Spiele", "Ende", "Spielquelle nicht aktiviert", "Trage einen API-Schlüssel in SportsConfig ein, um Spiele zu sehen. Deine Sportsender stehen unten.", "Heute keine Spiele", "Spiele konnten nicht geladen werden", "Prüfe Verbindung oder API-Schlüssel und aktualisiere.", "Sportsender", "Keine Sportsender gefunden", "In deinem Abo wurden keine Sender oder Pakete mit Sportnamen gefunden.", "Sportsender suchen...", "Erinnerung aktiviert", "Erinnerung entfernt", "Benachrichtigungen erlauben, um Erinnerungen zu nutzen", "Beginnt in %d Min.")
    "tr" -> SportsStrings("Bugünkü Maçlar", "MS", "Maç kaynağı etkin değil", "Maçları görmek için SportsConfig'e API anahtarı ekleyin. Spor kanallarınız aşağıda.", "Bugün maç yok", "Maçlar yüklenemedi", "Bağlantıyı veya API anahtarını kontrol edip yenileyin.", "Spor Kanalları", "Spor kanalı bulunamadı", "Aboneliğinizde spor adı taşıyan kanal veya paket bulunamadı.", "Spor kanallarında ara...", "Hatırlatıcı açıldı", "Hatırlatıcı kaldırıldı", "Hatırlatıcı için bildirim izni gerekli", "%d dk sonra başlıyor")
    "fr" -> SportsStrings("Matchs du jour", "Terminé", "Source des matchs non activée", "Ajoutez une clé API dans SportsConfig pour afficher les matchs. Vos chaînes sport sont ci-dessous.", "Aucun match aujourd'hui", "Impossible de charger les matchs", "Vérifiez la connexion ou la clé API puis actualisez.", "Chaînes de sport", "Aucune chaîne de sport trouvée", "Aucune chaîne ou liste de votre abonnement n'a un nom lié au sport.", "Rechercher une chaîne sport...", "Rappel activé", "Rappel supprimé", "Autorisez les notifications pour activer le rappel", "Début dans %d min")
    "ku" -> SportsStrings("یارییەکانی ئەمڕۆ", "تەواوبوو", "سەرچاوەی یارییەکان چالاک نییە", "کلیلی API لە SportsConfig زیاد بکە. کەناڵە وەرزشییەکانت لە خوارەوەن.", "ئەمڕۆ یاری نییە", "بارکردنی یارییەکان سەرکەوتوو نەبوو", "پەیوەندی یان کلیل بپشکنە و نوێ بکەرەوە.", "کەناڵە وەرزشییەکان", "هیچ کەناڵێکی وەرزشی نەدۆزرایەوە", "لە بەشداریەکەتدا کەناڵ یان پاکێجی وەرزشی نەدۆزرایەوە.", "گەڕان لە کەناڵە وەرزشییەکانت...", "ئاگادارکردنەوە چالاککرا", "ئاگادارکردنەوە ڕاگیرا", "بۆ ئاگادارکردنەوە ڕێگەپێدانی ئاگادارکردنەوەکان پێویستە", "دوای %d خولەک دەست پێدەکات")
    else -> SportsStrings("مباريات اليوم", "انتهت", "مصدر المباريات غير مفعّل", "أضف مفتاح API في SportsConfig لعرض المباريات. قنواتك الرياضية متاحة أدناه.", "لا توجد مباريات اليوم", "تعذّر تحميل المباريات", "تحقق من الاتصال أو المفتاح ثم اضغط تحديث.", "القنوات الرياضية", "لا توجد قنوات رياضية", "لم نجد في اشتراكك قنوات أو باقات تحمل أسماء رياضية.", "ابحث في القنوات الرياضية...", "تم تفعيل التنبيه", "تم إلغاء التنبيه", "يلزم السماح بالإشعارات لتفعيل التنبيه", "تبدأ بعد %d دقائق")
}

// ───────────────────────── مطابقة القنوات ─────────────────────────
object SportsChannelMatcher {
    private val generic = listOf(
        "sport", "spor", "رياض", "bein", "ssc", "espn", "dazn", "eurosport", "الكأس", "alkass", "kass",
        "arryadia", "fox sports", "tnt", "canal+", "rmc", "nba", "ufc", "wwe", "f1", "motogp"
    )
    private val regexCache = HashMap<String, Regex>()

    fun matchesLower(text: String, kw: String): Boolean {
        if (text.isEmpty()) return false
        val k = kw.lowercase()
        if (k.length > 3 || !k.all { it in 'a'..'z' || it in '0'..'9' }) return text.contains(k)
        val re = regexCache.getOrPut(k) { Regex("(?<![a-z])" + Regex.escape(k) + "(?![a-z])") }
        return re.containsMatchIn(text)
    }

    fun isSportsChannel(ch: MediaItemModel): Boolean {
        val t = (ch.title + " | " + ch.group).lowercase()
        return generic.any { matchesLower(t, it) }
    }

    fun langBase(lang: String): List<String> = when (lang) {
        "en" -> listOf("sky sports", "tnt sports", "bt sport", "espn", "bein", "eurosport")
        "de" -> listOf("sky sport", "dazn", "sportdigital", "magenta", "eurosport")
        "tr" -> listOf("bein", "s sport", "trt spor", "tivibu", "a spor")
        "fr" -> listOf("canal+", "bein", "rmc sport", "eurosport", "dazn", "ligue 1+")
        else -> listOf("bein", "ssc", "الكأس", "alkass", "ad sports", "abu dhabi sport", "dubai sport", "arryadia")
    }

    private fun leagueKeywords(leagueName: String): List<String> {
        val n = leagueName.lowercase()
        return when {
            "saudi" in n || "roshn" in n -> listOf("ssc", "saudi", "ksa")
            "premier league" in n && "egypt" !in n -> listOf("premier league", "epl")
            "la liga" in n || "primera" in n -> listOf("la liga", "laliga")
            "bundesliga" in n -> listOf("bundesliga")
            "serie a" in n -> listOf("serie a")
            "ligue 1" in n -> listOf("ligue 1")
            "champions league" in n -> listOf("champions", "uefa")
            "europa" in n || "european championship" in n || "euro" in n -> listOf("uefa", "europa")
            "world cup" in n || "fifa" in n -> listOf("world cup", "fifa")
            "africa" in n || "caf" in n -> listOf("caf", "africa")
            else -> emptyList()
        }
    }

    fun findChannel(league: String, lang: String, sportsChannels: List<MediaItemModel>): MediaItemModel? {
        if (sportsChannels.isEmpty()) return null
        for (kw in leagueKeywords(league) + langBase(lang)) {
            sportsChannels.firstOrNull { matchesLower(it.title.lowercase(), kw) }?.let { return it }
            sportsChannels.firstOrNull { matchesLower(it.group.lowercase(), kw) }?.let { return it }
        }
        return null
    }
}

// ───────────────────────── جلب المباريات من matches.json ─────────────────────────
object SportsRepository {
    private var cacheTime = 0L
    private var cacheState: SportsState? = null

    suspend fun fetchToday(force: Boolean): SportsState = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = cacheState
        if (!force && cached != null && now - cacheTime < 45_000L) return@withContext cached

        val result: SportsState = try {
            val body = httpGet(
                SportsConfig.MATCHES_JSON_URL,
                mapOf("Cache-Control" to "no-cache")
            )
            val parsed = parseMatchesJson(body, now)
            if (parsed.isEmpty()) SportsState.Empty else SportsState.Ready(parsed.take(80))
        } catch (e: Exception) {
            SportsState.Error(e.message ?: "connection")
        }

        if (result !is SportsState.Error) {
            cacheState = result
            cacheTime = now
        }
        result
    }

    private fun parseIsoToMs(raw: String, fallback: Long): Long {
        if (raw.isBlank()) return fallback
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(raw.take(19))?.time ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private fun parseMatchesJson(rawJson: String, now: Long): List<SportsMatch> {
        if (rawJson.isBlank()) return emptyList()
        val root = JSONObject(rawJson)
        val arr: JSONArray = root.optJSONArray("matches") ?: return emptyList()
        val out = ArrayList<SportsMatch>(arr.length())

        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val home = o.optString("homeTeam", "").trim()
            val away = o.optString("awayTeam", "").trim()
            if (home.isEmpty() || away.isEmpty()) continue

            val isLive = o.optBoolean("isLive", false)
            val statusStr = o.optString("status", "").uppercase(Locale.US)
            val phase = when {
                isLive || statusStr.contains("LIVE") || statusStr.contains("1H") || statusStr.contains("2H") || statusStr.contains("HT") -> MatchPhase.LIVE
                statusStr.contains("FT") || statusStr.contains("FINISHED") -> MatchPhase.FINISHED
                else -> MatchPhase.UPCOMING
            }

            var homeScore: Int? = null
            var awayScore: Int? = null
            val scoreStr = o.optString("scoreText", "")
            if (scoreStr.contains("-")) {
                val parts = scoreStr.split("-")
                homeScore = parts.getOrNull(0)?.trim()?.toIntOrNull()
                awayScore = parts.getOrNull(1)?.trim()?.toIntOrNull()
            }

            out.add(
                SportsMatch(
                    id = o.optString("id", "gh_$i"),
                    league = o.optString("league", "بطولة دولية"),
                    home = home,
                    away = away,
                    kickoffMs = parseIsoToMs(o.optString("matchTime", ""), now),
                    phase = phase,
                    homeScore = homeScore,
                    awayScore = awayScore,
                    priority = if (phase == MatchPhase.LIVE) 0 else 1
                )
            )
        }
        return out
    }

    private fun httpGet(url: String, headers: Map<String, String>): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw java.io.IOException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

// ───────────────────────── الواجهة ─────────────────────────
@Composable
private fun SportsMessageCard(icon: ImageVector, title: String, sub: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF12141F))
            .border(1.dp, Color(0xFF202436), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(30.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (sub.isNotEmpty()) Text(sub, color = Color(0xFF9CA3AF), fontSize = 11.5.sp, textAlign = TextAlign.Center, lineHeight = 17.sp)
    }
}

@Composable
private fun SportsMatchCard(
    m: SportsMatch,
    st: SportsStrings,
    linked: MediaItemModel?,
    reminderOn: Boolean,
    onPlay: (MediaItemModel) -> Unit,
    onBell: () -> Unit
) {
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val center = when (m.phase) {
        MatchPhase.LIVE, MatchPhase.FINISHED -> "${m.homeScore ?: 0} - ${m.awayScore ?: 0}"
        MatchPhase.UPCOMING -> timeFmt.format(Date(m.kickoffMs))
        MatchPhase.OTHER -> "—"
    }
    val canRemind = m.phase == MatchPhase.UPCOMING && m.kickoffMs > System.currentTimeMillis() + 30_000L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF131520))
            .border(1.dp, if (m.phase == MatchPhase.LIVE) Color(0xFFDC2626).copy(alpha = 0.6f) else Color(0xFF212436), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(m.league, color = goldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            when (m.phase) {
                MatchPhase.LIVE -> Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFDC2626)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("LIVE", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                }
                MatchPhase.FINISHED -> Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF2A2D3E)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(st.finished, color = Color(0xFFCBD5E1), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }
                else -> {}
            }
            if (canRemind) {
                IconButton(onClick = onBell, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (reminderOn) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = if (reminderOn) goldLight else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(m.home, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Box(Modifier.padding(horizontal = 8.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1D2030)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(center, color = if (m.phase == MatchPhase.LIVE) Color(0xFF4ADE80) else goldLight, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                Text(m.away, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }

        if (linked != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1C2A))
                    .clickable { onPlay(linked) }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = gold, modifier = Modifier.size(16.dp))
                Text(linked.title, color = Color(0xFFCBD5E1), fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = gold, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
fun SportsTabScreen(
    lang: String,
    liveChannels: List<MediaItemModel>,
    onPlayChannel: (MediaItemModel) -> Unit
) {
    val context = LocalContext.current
    val base = remember(lang) { getStrings(lang) }
    val st = remember(lang) { sportsStrings(lang) }
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    var state by remember { mutableStateOf<SportsState>(SportsState.Loading) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var remRev by remember { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<SportsMatch?>(null) }

    LaunchedEffect(refreshKey) {
        if (state !is SportsState.Ready) state = SportsState.Loading
        state = SportsRepository.fetchToday(force = refreshKey > 0)
    }
    LaunchedEffect(Unit) {
        val ms = SportsConfig.autoRefreshMs()
        if (ms <= 0L) return@LaunchedEffect
        while (true) {
            delay(ms)
            val r = SportsRepository.fetchToday(force = true)
            if (r !is SportsState.Error || state !is SportsState.Ready) state = r
        }
    }

    val allSports = remember(liveChannels) { liveChannels.filter { SportsChannelMatcher.isSportsChannel(it) } }
    val sportsGroups = remember(allSports, query, lang) {
        val q = query.trim()
        val filtered = if (q.isEmpty()) allSports else allSports.filter { it.title.contains(q, ignoreCase = true) }
        val prio = SportsChannelMatcher.langBase(lang)
        filtered.groupBy { it.group }.toList().sortedWith(
            compareBy<Pair<String, List<MediaItemModel>>>(
                { g -> val low = g.first.lowercase(); if (prio.any { SportsChannelMatcher.matchesLower(low, it) }) 0 else 1 },
                { it.first.lowercase() }
            )
        )
    }

    fun doSchedule(m: SportsMatch) {
        val linked = SportsChannelMatcher.findChannel(m.league, lang, allSports)
        val lead = SportsConfig.REMINDER_LEAD_MIN
        val title = "⚽ ${m.home} - ${m.away}"
        val body = buildString {
            append(m.league).append(" • ").append(String.format(Locale.getDefault(), st.startsIn, lead))
            if (linked != null) append(" • ").append(linked.title)
        }
        if (SportsReminders.schedule(context, m.id, m.kickoffMs, lead, title, body)) {
            Toast.makeText(context, st.reminderOn, Toast.LENGTH_SHORT).show()
        }
        remRev++
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val m = pending
        pending = null
        if (granted && m != null) doSchedule(m)
        else if (!granted) Toast.makeText(context, st.permDenied, Toast.LENGTH_LONG).show()
    }

    fun onBell(m: SportsMatch) {
        if (SportsReminders.isSet(context, m.id)) {
            SportsReminders.cancel(context, m.id)
            remRev++
            Toast.makeText(context, st.reminderOff, Toast.LENGTH_SHORT).show()
            return
        }
        val needsPerm = android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPerm) {
            pending = m
            permLauncher.launch("android.permission.POST_NOTIFICATIONS")
        } else doSchedule(m)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "hdr") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = goldLight, modifier = Modifier.size(22.dp))
                    Column {
                        Text(base.sports, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(st.todayMatches, color = Color(0xFF9CA3AF), fontSize = 11.5.sp)
                    }
                }
                IconButton(
                    onClick = { refreshKey++ },
                    modifier = Modifier.size(34.dp).background(Color(0xFF171723), CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = base.settings, tint = goldLight, modifier = Modifier.size(18.dp))
                }
            }
        }

        when (val s = state) {
            SportsState.Loading -> item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = gold, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                }
            }
            SportsState.NoKey -> item(key = "nokey") { SportsMessageCard(Icons.Default.VpnKey, st.noKey, st.noKeyHint) }
            SportsState.Empty -> item(key = "empty") { SportsMessageCard(Icons.Default.EventBusy, st.noMatches, "") }
            is SportsState.Error -> item(key = "error") { SportsMessageCard(Icons.Default.WifiOff, st.error, st.errorHint) }
            is SportsState.Ready -> items(s.matches, key = { "m_${it.id}" }) { m ->
                val linked = remember(m.league, lang, allSports) { SportsChannelMatcher.findChannel(m.league, lang, allSports) }
                val on = remember(m.id, remRev) { SportsReminders.isSet(context, m.id) }
                SportsMatchCard(m, st, linked, on, onPlayChannel) { onBell(m) }
            }
        }

        item(key = "ch_hdr") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LiveTv, contentDescription = null, tint = goldLight, modifier = Modifier.size(20.dp))
                    Text(st.sportsChannels, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Box(Modifier.clip(RoundedCornerShape(50)).background(gold.copy(alpha = 0.22f)).padding(horizontal = 9.dp, vertical = 2.dp)) {
                        Text("${allSports.size}", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    }
                }
                if (allSports.isNotEmpty()) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(st.searchHint, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gold, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (allSports.isEmpty()) {
            item(key = "no_ch") { SportsMessageCard(Icons.Default.Tv, st.noChannels, st.noChannelsHint) }
        } else {
            sportsGroups.forEach { (grp, list) ->
                item(key = "g_$grp") {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = gold, modifier = Modifier.size(18.dp))
                            Text(grp, color = goldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("${list.size}", color = Color(0xFFCBD5E1), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
                items(list, key = { "c_${grp}_${it.id}" }) { ch ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF131520))
                            .border(1.dp, Color(0xFF212436), RoundedCornerShape(14.dp))
                            .clickable { onPlayChannel(ch) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (ch.logo.isNotEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(ch.logo).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.dp))
                            )
                        } else {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF1D2030)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Tv, contentDescription = null, tint = gold, modifier = Modifier.size(22.dp))
                            }
                        }
                        Text(ch.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = gold, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        item(key = "bottom_space") { Spacer(Modifier.height(8.dp)) }
    }
}
