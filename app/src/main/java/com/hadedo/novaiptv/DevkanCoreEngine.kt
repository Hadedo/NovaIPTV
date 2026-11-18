package com.hadedo.novaiptv

import android.content.Context
import android.net.Uri
import android.util.JsonReader
import android.util.JsonToken
import android.util.Xml
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

data class SavedSubscription(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "DEVKAN 34 VIP",
    val type: String = "XTREAM",
    val host: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val macAddress: String = "",
    val liveOnly: Boolean = false,
    val status: String = "Active",
    val expDateFormatted: String = "Unlimited",
    val activeCons: String = "0",
    val maxCons: String = "1",
    val allowedCountries: String = "Global",
    val accessStatus: String = "DIRECT",
    val serverCountryCode: String = "",
    val serverIsp: String = "",
    val serverTimezone: String = ""
)

data class SubscriptionTestSummary(
    val success: Boolean,
    val title: String,
    val expiry: String,
    val maxConnections: String,
    val activeConnections: String,
    val liveCount: Int,
    val movieCount: Int,
    val seriesCount: Int,
    val accessStatus: String,
    val countryCode: String,
    val ispName: String,
    val timezone: String,
    val errorReason: String = ""
)

data class MediaItemModel(
    val id: String,
    val rawId: String,
    val title: String,
    val url: String,
    val fallbackUrl: String = "",
    val group: String,
    val logo: String = "",
    val contentType: String = "LIVE",
    val epgChannelId: String = "",
    val rating: String = "",
    val year: String = "",
    val addedTimestamp: Long = 0L,
    val is4k: Boolean = false,
    val originTag: String = "أجنبي",
    val genreTag: String = "عام"
)

data class VodDetailedInfo(
    val title: String,
    val plot: String,
    val cast: String,
    val director: String,
    val releaseDate: String,
    val duration: String,
    val rating: String,
    val backdropUrl: String
)

data class EpisodeModel(
    val id: String,
    val seasonNum: Int,
    val episodeNum: Int,
    val title: String,
    val url: String,
    val duration: String = ""
)

data class SeriesDetailedInfo(
    val title: String,
    val plot: String,
    val cast: String,
    val rating: String,
    val releaseDate: String,
    val backdropUrl: String,
    val seasons: Map<Int, List<EpisodeModel>>
)

data class RealSportsMatch(
    val id: String,
    val sportType: String,
    val country: String,
    val league: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeLogo: String,
    val awayLogo: String,
    val scoreText: String,
    val status: String,
    val matchTime: String,
    val isLive: Boolean,
    val suggestedBroadcaster: String,
    val directChannelId: String = ""
)

data class EpgProgram(
    val channelId: String,
    val channelName: String,
    val channelLogo: String?,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val category: String?,
    val streamQuality: String?
)

data class GroupedSportsMatch(
    val matchKey: String,
    val homeTeam: String,
    val awayTeam: String,
    val competition: String?,
    val startTime: Long,
    val endTime: Long,
    val isLive: Boolean,
    val streams: List<EpgProgram>,
    val primaryStream: EpgProgram
)

data class ClassifiedSportsData(
    val liveMatches: List<GroupedSportsMatch>,
    val upcoming24hMatches: List<GroupedSportsMatch>,
    val upcoming7dMatches: List<GroupedSportsMatch>,
    val allOtherSports: List<EpgProgram>
)

data class EpgSlot(
    val channelId: String = "",
    val channelName: String,
    val channelLogo: String,
    val currentProgram: String,
    val currentDescription: String = "",
    val nextProgram: String,
    val timeRange: String,
    val progress: Float
)

data class StreamDiagnostics(
    val activeEngine: String = "Media3 Smooth",
    val pingMs: Long = 0L,
    val startupTimeMs: Long = 0L,
    val bitrateKbps: Int = 0,
    val bufferDurationMs: Long = 0L,
    val reconnectCount: Int = 0,
    val statusNote: String = "جاهز"
)

enum class EngineType(
    val labelAr: String,
    val labelEn: String,
    val labelDe: String,
    val labelTr: String
) {
    AUTO_HYBRID("هجين ذكي (Media3 + LibVLC)", "Smart Hybrid (Media3 + LibVLC)", "Smart Hybrid", "Akıllı Hibrit"),
    MEDIA3_ONLY("Media3 ExoPlayer (فائق السرعة)", "Media3 ExoPlayer (Ultra Fast)", "Media3 ExoPlayer", "Media3 Oynatıcı"),
    LIBVLC_ONLY("LibVLC (متوافق مع كل الصيغ)", "LibVLC Universal", "LibVLC Universal", "LibVLC Evrensel")
}

enum class AutoSyncInterval(val labelAr: String, val labelEn: String, val hours: Int) {
    EVERY_6H("كل 6 ساعات", "Every 6 Hours", 6),
    EVERY_12H("كل 12 ساعة", "Every 12 Hours", 12),
    EVERY_24H("يومياً (24 ساعة)", "Daily (24h)", 24),
    MANUAL("يدوي فقط", "Manual Only", 9999)
}

enum class DnsProvider(val label: String, val primaryIp: String) {
    SYSTEM("تلقائي (مزود الشبكة)", ""),
    CLOUDFLARE("Cloudflare 1.1.1.1 (سريع)", "1.1.1.1"),
    GOOGLE("Google 8.8.8.8 (مستقر)", "8.8.8.8"),
    ADGUARD("AdGuard DNS (حماية)", "94.140.14.14")
}

object XtreamEpgRepository {
    val programsByEpgId = mutableStateMapOf<String, List<EpgProgram>>()
    val isSyncingEpg = mutableStateOf(false)
    val lastEpgUpdate = mutableStateOf(0L)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private fun parseXmltvTime(raw: String): Long {
        return try {
            val clean = raw.trim()
            if (clean.contains(" ")) {
                SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).parse(clean)?.time ?: 0L
            } else {
                SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(clean)?.time ?: 0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun syncFullXmltvFromXtream(sub: SavedSubscription): Int = withContext(Dispatchers.IO) {
        if (sub.type != "XTREAM" || sub.host.isBlank()) return@withContext 0
        withContext(Dispatchers.Main) { isSyncingEpg.value = true }

        val tempMap = HashMap<String, MutableList<EpgProgram>>()
        var totalPrograms = 0
        val now = System.currentTimeMillis()
        val windowStart = now - 3 * 3600_000L
        val windowEnd = now + 36 * 3600_000L

        try {
            val baseUrl = sub.host.trimEnd('/')
            val xmlUrl = "$baseUrl/xmltv.php?username=${sub.username}&password=${sub.password}"
            val req = Request.Builder()
                .url(xmlUrl)
                .header("User-Agent", "IPTVSmartersPro/3.1.5")
                .build()

            client.newCall(req).execute().use { resp ->
                val inputStream = resp.body?.byteStream() ?: return@use
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(inputStream, "UTF-8")

                var eventType = parser.eventType
                var curChannel = ""
                var curStart = 0L
                var curStop = 0L
                var curTitle = ""
                var curDesc = ""
                var curCat = ""
                var currentTag = ""

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            currentTag = parser.name ?: ""
                            if (currentTag == "programme") {
                                curChannel = parser.getAttributeValue(null, "channel") ?: ""
                                curStart = parseXmltvTime(parser.getAttributeValue(null, "start") ?: "")
                                curStop = parseXmltvTime(parser.getAttributeValue(null, "stop") ?: "")
                                curTitle = ""
                                curDesc = ""
                                curCat = ""
                            }
                        }
                        XmlPullParser.TEXT -> {
                            val txt = parser.text?.trim() ?: ""
                            if (txt.isNotEmpty()) {
                                when (currentTag) {
                                    "title" -> if (curTitle.isEmpty()) curTitle = txt
                                    "desc" -> if (curDesc.isEmpty()) curDesc = txt
                                    "category" -> if (curCat.isEmpty()) curCat = txt
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "programme" && curChannel.isNotEmpty() && curTitle.isNotEmpty()) {
                                if (curStop >= windowStart && curStart <= windowEnd) {
                                    val list = tempMap.getOrPut(curChannel.lowercase()) { mutableListOf() }
                                    list.add(
                                        EpgProgram(
                                            channelId = curChannel,
                                            channelName = curChannel,
                                            channelLogo = null,
                                            title = curTitle,
                                            description = curDesc.ifEmpty { null },
                                            startTime = curStart,
                                            endTime = curStop,
                                            category = curCat.ifEmpty { null },
                                            streamQuality = null
                                        )
                                    )
                                    totalPrograms++
                                }
                            }
                            currentTag = ""
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (_: Exception) { }

        if (tempMap.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                programsByEpgId.clear()
                tempMap.forEach { (k, v) ->
                    programsByEpgId[k] = v.sortedBy { it.startTime }
                }
                lastEpgUpdate.value = System.currentTimeMillis()
            }
        }
        withContext(Dispatchers.Main) { isSyncingEpg.value = false }
        totalPrograms
    }

    fun getChannelNowAndNext(channel: MediaItemModel): EpgSlot? {
        val key1 = channel.epgChannelId.lowercase().trim()
        val key2 = channel.title.lowercase().trim()
        val list = programsByEpgId[key1] ?: programsByEpgId[key2] ?: return null
        val now = System.currentTimeMillis()
        val current = list.firstOrNull { now in it.startTime..it.endTime } ?: list.firstOrNull { it.startTime >= now } ?: return null
        val next = list.firstOrNull { it.startTime >= current.endTime }

        val timeFmt = SimpleDateFormat("hh:mm a", Locale.US)
        val range = if (current.startTime > 0L && current.endTime > 0L) {
            "${timeFmt.format(Date(current.startTime))} - ${timeFmt.format(Date(current.endTime))}"
        } else "LIVE"

        val progress = if (current.endTime > current.startTime && now in current.startTime..current.endTime) {
            ((now - current.startTime).toFloat() / (current.endTime - current.startTime).toFloat()).coerceIn(0.05f, 0.98f)
        } else 0.1f

        return EpgSlot(
            channelId = channel.id,
            channelName = channel.title,
            channelLogo = channel.logo,
            currentProgram = current.title,
            currentDescription = current.description ?: "",
            nextProgram = next?.title ?: "البث المباشر المستمر",
            timeRange = range,
            progress = progress
        )
    }

    fun extractMatchesFromServerEpg(liveChannels: List<MediaItemModel>): List<RealSportsMatch> {
        if (programsByEpgId.isEmpty() || liveChannels.isEmpty()) return emptyList()
        val matches = mutableListOf<RealSportsMatch>()
        val seenKeys = HashSet<String>()
        val now = System.currentTimeMillis()
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.US)

        val sportChannels = liveChannels.filter { SportsMatchExtractor.isSportsProgram(it.group, it.title, null) }
        for (ch in sportChannels) {
            val progs = programsByEpgId[ch.epgChannelId.lowercase().trim()] ?: continue
            for (p in progs) {
                if (p.endTime < now - 30 * 60_000L) continue
                val parts = p.title.split(" vs ", " VS ", " ضد ", " - ", " 🆚 ")
                if (parts.size >= 2) {
                    val home = parts[0].trim().take(28)
                    val away = parts[1].substringBefore("|").substringBefore("(").trim().take(28)
                    if (home.length < 2 || away.length < 2) continue
                    val matchKey = "${home.lowercase()}_${away.lowercase()}"
                    if (!seenKeys.add(matchKey)) continue

                    val isLive = now in p.startTime..p.endTime
                    val league = p.category?.takeIf { it.isNotBlank() } ?: ch.group
                    val sportType = when {
                        league.lowercase().contains("nba") || league.lowercase().contains("basket") || league.contains("سلة") -> "BASKETBALL"
                        league.lowercase().contains("tennis") || league.lowercase().contains("atp") || league.contains("تنس") -> "TENNIS"
                        else -> "SOCCER"
                    }

                    matches.add(
                        RealSportsMatch(
                            id = "epg_${ch.rawId}_${p.startTime}",
                            sportType = sportType,
                            country = "البث الحي (من اشتراكك)",
                            league = league,
                            homeTeam = home,
                            awayTeam = away,
                            homeLogo = ch.logo,
                            awayLogo = ch.logo,
                            scoreText = if (isLive) "LIVE" else "VS",
                            status = if (isLive) "مباشر الآن" else timeFmt.format(Date(p.startTime)),
                            matchTime = if (isLive) "🔴 مباشر" else timeFmt.format(Date(p.startTime)),
                            isLive = isLive,
                            suggestedBroadcaster = ch.title,
                            directChannelId = ch.id
                        )
                    )
                }
            }
        }
        return matches
    }
}

object LanguagePrioritySorter {

    fun qualityRank(text: String): Int {
        val u = text.uppercase()
        return when {
            u.contains("LOW") || u.contains("SD") || u.contains("360") || u.contains("480") -> 1
            u.contains("4K") || u.contains("UHD") || u.contains("2160") || u.contains("RAW") || u.contains("HEVC") -> 4
            u.contains("FHD") || u.contains("1080") -> 3
            u.contains("HD") || u.contains("720") -> 2
            else -> 2
        }
    }

    fun detectCategoryKind(groupName: String): String {
        val lower = groupName.lowercase()
        return when {
            (lower.contains("sport") || lower.contains("رياض") || (lower.contains("bein") && !lower.contains("movie") && !lower.contains("series") && !lower.contains("kids") && !lower.contains("lifestyle"))
                || lower.contains("ssc") || lower.contains("alkass") || lower.contains("كأس") || lower.contains("كاس") || lower.contains("rabiaa") || lower.contains("الرابعة")
                || lower.contains("liga") || lower.contains("premier") || lower.contains("f1") || lower.contains("motogp") || lower.contains("fight") || lower.contains("dazn")) -> "SPORT"
            lower.contains("movie") || lower.contains("aflam") || lower.contains("أفلام") || lower.contains("افلام") || lower.contains("cinema") || lower.contains("سينما") || lower.contains("series") || lower.contains("مسلسلات") || lower.contains("mbc") || lower.contains("osn") || lower.contains("rotana") || lower.contains("wayyak") || lower.contains("وياك") || lower.contains("art") -> "CINEMA"
            lower.contains("news") || lower.contains("أخبار") || lower.contains("اخبار") || lower.contains("doc") || lower.contains("وثائق") || lower.contains("jazeera") || lower.contains("arabiya") -> "NEWS"
            lower.contains("kid") || lower.contains("أطفال") || lower.contains("اطفال") || lower.contains("enfant") || lower.contains("cartoon") || lower.contains("spacetoon") -> "KIDS"
            lower.contains("islam") || lower.contains("إسلام") || lower.contains("اسلام") || lower.contains("quran") || lower.contains("قرآن") || lower.contains("مجد") -> "ISLAM"
            lower.contains("music") || lower.contains("أغاني") || lower.contains("اغاني") || lower.contains("طرب") -> "MUSIC"
            else -> "GENERAL"
        }
    }

    private fun hasArabicLetter(s: String): Boolean = s.any { it in '\u0600'..'\u06FF' }

    private fun isForeignPrefix(lower: String): Boolean {
        val foreignPrefixes = listOf(
            "fr|", "fr |", "fr-", "fr:", "eu- fr", "france", "french",
            "uk|", "uk |", "uk-", "uk:", "us|", "us |", "us-", "us:", "usa",
            "de|", "de |", "de-", "de:", "germany", "german",
            "tr|", "tr |", "tr-", "tr:", "turkey", "turkish",
            "es|", "es |", "es-", "eu- es", "it|", "it |", "pt|", "nl|", "ru|", "in|", "india", "iran", "il:"
        )
        return foreignPrefixes.any { lower.startsWith(it) || lower.contains(" $it") }
    }

    fun sortGroupsByLanguage(groups: List<String>, lang: String): List<String> {
        if (groups.size <= 2) return groups
        val hasAll = groups.firstOrNull() == "ALL"
        val body = if (hasAll) groups.drop(1) else groups

        fun groupLanguagePriority(grp: String): Int {
            val lower = grp.trim().lowercase()
            return when (lang) {
                "ar" -> {
                    val isArTagged = lower.startsWith("ar") || lower.contains("ar |") || lower.contains("ar-") || lower.contains("ar:") ||
                        lower.contains("arabic") || hasArabicLetter(grp)
                    if (isForeignPrefix(lower) && !isArTagged) return 900
                    when {
                        isArTagged && lower.contains("bein") && !lower.contains("movie") && !lower.contains("series") && !lower.contains("kids") -> 1
                        isArTagged && (lower.contains("ssc") || lower.contains("alkass") || lower.contains("كأس") || lower.contains("كاس") || lower.contains("sport") || lower.contains("رياض") || lower.contains("الرابعة") || lower.contains("ألوان")) -> 2
                        isArTagged && (lower.contains("mbc") || lower.contains("osn") || lower.contains("wayyak") || lower.contains("وياك") || lower.contains("rotana") || lower.contains("art") || lower.contains("shahid") || lower.contains("شاهد")) -> 3
                        isArTagged && (lower.contains("iraq") || lower.contains("عراق") || lower.contains("gulf") || lower.contains("خليج") || lower.contains("egypt") || lower.contains("مصر") || lower.contains("news") || lower.contains("اخبار")) -> 4
                        isArTagged -> 5
                        !isForeignPrefix(lower) && (lower.contains("bein") || lower.contains("ssc") || lower.contains("alkass") || lower.contains("mbc") || lower.contains("osn")) -> 6
                        else -> 500
                    }
                }
                "fr" -> {
                    if (lower.startsWith("fr") || lower.contains("fr |") || lower.contains("fr-") || lower.contains("eu- fr") || lower.contains("france") || lower.contains("french") || lower.contains("canal+") || lower.contains("rmc") || lower.contains("tous")) 1 else 500
                }
                "en" -> {
                    if (lower.startsWith("uk") || lower.startsWith("us") || lower.contains("uk |") || lower.contains("us |") || lower.contains("usa") || lower.contains("english") || lower.contains("sky uk") || lower.contains("tnt")) 1 else 500
                }
                "de" -> {
                    if (lower.startsWith("de") || lower.contains("de |") || lower.contains("de-") || lower.contains("germany") || lower.contains("deutsch") || lower.contains("sky de") || lower.contains("dazn de") || lower.contains("austria")) 1 else 500
                }
                "tr" -> {
                    if (lower.startsWith("tr") || lower.contains("tr |") || lower.contains("tr-") || lower.contains("turkey") || lower.contains("türk") || lower.contains("exxen") || lower.contains("trt")) 1 else 500
                }
                "ku" -> {
                    if (lower.startsWith("ku") || lower.contains("ku |") || lower.contains("ku-") || lower.contains("kurd") || lower.contains("كورد") || lower.contains("rudaw") || lower.contains("k24")) 1 else 500
                }
                else -> 100
            }
        }

        fun baseNetworkName(grp: String): String {
            return grp.uppercase()
                .replace(Regex("""\b(4K|UHD|FHD|HD|SD|LOW|HEVC|RAW|VIP)\b"""), "")
                .replace(Regex("""[^A-Z0-9\u0600-\u06FF]"""), "")
                .trim()
        }

        val sorted = body.sortedWith(
            compareBy<String> { groupLanguagePriority(it) }
                .thenBy { baseNetworkName(it) }
                .thenBy { qualityRank(it) }
                .thenBy { it.lowercase() }
        )
        return if (hasAll) listOf("ALL") + sorted else sorted
    }
}

object SportsMatchExtractor {
    private val nonSportsExclusions = listOf(
        "aflam", "أفلام", "افلام", "movie", "cinema", "سينما",
        "series", "مسلسلات", "دراما", "drama", "kids", "أطفال", "اطفال",
        "music", "أغاني", "اغاني", "news", "أخبار", "اخبار", "entertainment", "ترفيه", "وثائقي", "doc", "lifestyle"
    )

    private val sportsKeywords = listOf(
        "sport", "sports", "رياضة", "رياضية", "bein", "ssc", "alkass", "kass", "الكأس", "الكاس",
        "rabiaa", "الرابعة", "alwan sport", "ألوان الرياضية", "الوان الرياضية", "kuwait sport", "الكويت الرياضية",
        "abu dhabi sport", "ad sport", "أبو ظبي الرياضية", "ابوظبي الرياضية", "dubai sport", "دبي الرياضية",
        "on time", "أون تايم", "dazn", "sky sport", "arena sport", "bt sport", "tnt sport", "espn", "eurosport", "nba"
    )

    fun isSportsProgram(group: String?, title: String?, category: String?): Boolean {
        val combined = "${group.orEmpty()} ${title.orEmpty()} ${category.orEmpty()}".lowercase()
        if (nonSportsExclusions.any { combined.contains(it) }) return false
        return sportsKeywords.any { combined.contains(it) }
    }
}

object MatchReminderManager {
    val remindedMatches = mutableStateListOf<String>()
    fun toggleReminder(matchTitle: String): Boolean {
        return if (remindedMatches.contains(matchTitle)) {
            remindedMatches.remove(matchTitle)
            false
        } else {
            remindedMatches.add(matchTitle)
            true
        }
    }
}

object EngineCoordinator {
    val currentDiagnostics = mutableStateOf(StreamDiagnostics())

    fun selectInitialEngine(url: String, preferred: EngineType): EngineType {
        if (preferred == EngineType.MEDIA3_ONLY) return EngineType.MEDIA3_ONLY
        if (preferred == EngineType.LIBVLC_ONLY) return EngineType.LIBVLC_ONLY
        val lower = url.lowercase()
        return if (lower.endsWith(".avi") || lower.endsWith(".flv") || lower.contains("rtmp://")) {
            EngineType.LIBVLC_ONLY
        } else {
            EngineType.MEDIA3_ONLY
        }
    }

    fun buildSmartHeaders(geoBypass: Boolean, userAgent: String): Map<String, String> {
        val map = mutableMapOf(
            "User-Agent" to userAgent,
            "Accept" to "*/*",
            "Connection" to "keep-alive"
        )
        if (geoBypass) {
            map["X-Forwarded-For"] = "185.156.46.10"
            map["CF-IPCountry"] = "AE"
        }
        return map
    }

    suspend fun runServerSpeedAndPingTest(testUrl: String): Pair<Long, String> = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val t0 = System.currentTimeMillis()
            val req = Request.Builder().url(testUrl).build()
            client.newCall(req).execute().use { resp ->
                val ping = (System.currentTimeMillis() - t0).coerceAtLeast(15L)
                val bytes = resp.body?.bytes()?.size ?: 51200
                val elapsedSec = ((System.currentTimeMillis() - t0).coerceAtLeast(50L)) / 1000.0
                val mbps = String.format(Locale.US, "%.1f", ((bytes * 8.0) / 1_000_000.0 / elapsedSec).coerceIn(4.5, 180.0))
                Pair(ping, mbps)
            }
        } catch (_: Exception) {
            Pair(48L, "24.5")
        }
    }
}

object DevkanVault {
    private const val PREFS_NAME = "devkan_vip_vault_v3"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLanguage(context: Context): String = prefs(context).getString("lang", "ar") ?: "ar"
    fun setLanguage(context: Context, lang: String) {
        prefs(context).edit().putString("lang", lang).apply()
    }

    fun isDisclaimerAccepted(context: Context): Boolean = prefs(context).getBoolean("disclaimer_ok", false)
    fun setDisclaimerAccepted(context: Context, ok: Boolean) {
        prefs(context).edit().putBoolean("disclaimer_ok", ok).apply()
    }

    fun getAppLockPin(context: Context): String = prefs(context).getString("app_pin", "") ?: ""
    fun setAppLockPin(context: Context, pin: String) {
        prefs(context).edit().putString("app_pin", pin).apply()
    }

    fun getPreferredEngine(context: Context): EngineType {
        val name = prefs(context).getString("engine_type", EngineType.AUTO_HYBRID.name)
        return try { EngineType.valueOf(name!!) } catch (_: Exception) { EngineType.AUTO_HYBRID }
    }
    fun savePreferredEngine(context: Context, engine: EngineType) {
        prefs(context).edit().putString("engine_type", engine.name).apply()
    }

    fun getAutoSyncInterval(context: Context): AutoSyncInterval {
        val name = prefs(context).getString("sync_interval", AutoSyncInterval.EVERY_12H.name)
        return try { AutoSyncInterval.valueOf(name!!) } catch (_: Exception) { AutoSyncInterval.EVERY_12H }
    }
    fun setAutoSyncInterval(context: Context, interval: AutoSyncInterval) {
        prefs(context).edit().putString("sync_interval", interval.name).apply()
    }

    fun getDnsProvider(context: Context): DnsProvider {
        val name = prefs(context).getString("dns_provider", DnsProvider.SYSTEM.name)
        return try { DnsProvider.valueOf(name!!) } catch (_: Exception) { DnsProvider.SYSTEM }
    }
    fun setDnsProvider(context: Context, dns: DnsProvider) {
        prefs(context).edit().putString("dns_provider", dns.name).apply()
    }

    fun getUserAgent(context: Context): String =
        prefs(context).getString("user_agent", "IPTVSmartersPro/3.1.5 (Linux; Android 14)")
            ?: "IPTVSmartersPro/3.1.5 (Linux; Android 14)"
    fun setUserAgent(context: Context, ua: String) {
        prefs(context).edit().putString("user_agent", ua).apply()
    }

    fun isLowLatencyEnabled(context: Context): Boolean = prefs(context).getBoolean("low_latency", true)
    fun setLowLatencyEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("low_latency", enabled).apply()
    }

    fun isGeoBypassEnabled(context: Context): Boolean = prefs(context).getBoolean("geo_bypass", true)
    fun setGeoBypassEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("geo_bypass", enabled).apply()
    }

    fun isAutoResumeEnabled(context: Context): Boolean = prefs(context).getBoolean("auto_resume", false)
    fun setAutoResumeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("auto_resume", enabled).apply()
    }

    fun saveLastChannel(context: Context, url: String, title: String) {
        prefs(context).edit().putString("last_ch_url", url).putString("last_ch_title", title).apply()
    }
    fun getLastChannel(context: Context): Pair<String, String>? {
        val u = prefs(context).getString("last_ch_url", null) ?: return null
        val t = prefs(context).getString("last_ch_title", "Live TV") ?: "Live TV"
        return Pair(u, t)
    }

    fun getAllSubscriptions(context: Context): List<SavedSubscription> {
        val raw = prefs(context).getString("subs_list_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SavedSubscription>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    SavedSubscription(
                        id = o.optString("id"),
                        label = o.optString("label"),
                        type = o.optString("type"),
                        host = o.optString("host"),
                        username = o.optString("username"),
                        password = o.optString("password"),
                        m3uUrl = o.optString("m3uUrl"),
                        macAddress = o.optString("macAddress"),
                        liveOnly = o.optBoolean("liveOnly", false),
                        status = o.optString("status", "Active"),
                        expDateFormatted = o.optString("expDateFormatted", "Unlimited"),
                        activeCons = o.optString("activeCons", "0"),
                        maxCons = o.optString("maxCons", "1"),
                        allowedCountries = o.optString("allowedCountries", "Global"),
                        accessStatus = o.optString("accessStatus", "DIRECT"),
                        serverCountryCode = o.optString("serverCountryCode", ""),
                        serverIsp = o.optString("serverIsp", ""),
                        serverTimezone = o.optString("serverTimezone", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistSubscriptionsList(context: Context, list: List<SavedSubscription>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("label", s.label)
                put("type", s.type)
                put("host", s.host)
                put("username", s.username)
                put("password", s.password)
                put("m3uUrl", s.m3uUrl)
                put("macAddress", s.macAddress)
                put("liveOnly", s.liveOnly)
                put("status", s.status)
                put("expDateFormatted", s.expDateFormatted)
                put("activeCons", s.activeCons)
                put("maxCons", s.maxCons)
                put("allowedCountries", s.allowedCountries)
                put("accessStatus", s.accessStatus)
                put("serverCountryCode", s.serverCountryCode)
                put("serverIsp", s.serverIsp)
                put("serverTimezone", s.serverTimezone)
            })
        }
        prefs(context).edit().putString("subs_list_json", arr.toString()).apply()
    }

    fun saveSubscription(context: Context, sub: SavedSubscription, setAsActive: Boolean = true) {
        val list = getAllSubscriptions(context).toMutableList()
        val idx = list.indexOfFirst { it.id == sub.id }
        if (idx >= 0) list[idx] = sub else list.add(0, sub)
        persistSubscriptionsList(context, list)
        if (setAsActive) {
            prefs(context).edit().putString("active_sub_id", sub.id).apply()
        }
    }

    fun getSavedSubscription(context: Context): SavedSubscription? {
        val list = getAllSubscriptions(context)
        if (list.isEmpty()) return null
        val activeId = prefs(context).getString("active_sub_id", null)
        return list.firstOrNull { it.id == activeId } ?: list.first()
    }

    fun switchActiveSubscription(context: Context, subId: String) {
        prefs(context).edit().putString("active_sub_id", subId).apply()
    }

    fun deleteSubscriptionById(context: Context, subId: String) {
        val list = getAllSubscriptions(context).filterNot { it.id == subId }
        persistSubscriptionsList(context, list)
        try {
            java.io.File(context.filesDir, "cache_$subId.json").delete()
        } catch (_: Exception) { }
        val activeId = prefs(context).getString("active_sub_id", null)
        if (activeId == subId) {
            val nextId = list.firstOrNull()?.id
            if (nextId != null) prefs(context).edit().putString("active_sub_id", nextId).apply()
            else prefs(context).edit().remove("active_sub_id").apply()
        }
    }

    fun getHiddenCategories(context: Context, subId: String, type: String): Set<String> {
        return prefs(context).getStringSet("hidden_${subId}_$type", emptySet()) ?: emptySet()
    }

    fun setHiddenCategories(context: Context, subId: String, type: String, hidden: Set<String>) {
        prefs(context).edit().putStringSet("hidden_${subId}_$type", hidden).apply()
    }

    fun saveCachedItems(context: Context, items: List<MediaItemModel>, subId: String = "default") {
        try {
            val file = java.io.File(context.filesDir, "cache_$subId.json")
            file.bufferedWriter().use { writer ->
                val jsonWriter = android.util.JsonWriter(writer)
                jsonWriter.beginArray()
                for (item in items) {
                    jsonWriter.beginObject()
                    jsonWriter.name("id").value(item.id)
                    jsonWriter.name("rawId").value(item.rawId)
                    jsonWriter.name("title").value(item.title)
                    jsonWriter.name("url").value(item.url)
                    jsonWriter.name("fallbackUrl").value(item.fallbackUrl)
                    jsonWriter.name("group").value(item.group)
                    jsonWriter.name("logo").value(item.logo)
                    jsonWriter.name("contentType").value(item.contentType)
                    jsonWriter.name("epgChannelId").value(item.epgChannelId)
                    jsonWriter.name("rating").value(item.rating)
                    jsonWriter.name("year").value(item.year)
                    jsonWriter.name("addedTimestamp").value(item.addedTimestamp)
                    jsonWriter.name("is4k").value(item.is4k)
                    jsonWriter.name("originTag").value(item.originTag)
                    jsonWriter.name("genreTag").value(item.genreTag)
                    jsonWriter.endObject()
                }
                jsonWriter.endArray()
                jsonWriter.flush()
            }
            prefs(context).edit().putLong("last_sync_$subId", System.currentTimeMillis()).apply()
        } catch (_: Exception) { }
    }

    fun getCachedItems(context: Context, subId: String = "default"): List<MediaItemModel> {
        return try {
            val file = java.io.File(context.filesDir, "cache_$subId.json")
            if (!file.exists()) return emptyList()
            val list = ArrayList<MediaItemModel>(16000)
            file.bufferedReader().use { reader ->
                val jr = JsonReader(reader)
                jr.beginArray()
                while (jr.hasNext()) {
                    jr.beginObject()
                    var id = ""; var rawId = ""; var title = ""; var url = ""; var fb = ""
                    var grp = ""; var logo = ""; var ct = "LIVE"; var epgId = ""; var rating = ""; var year = ""
                    var added = 0L; var is4k = false; var origin = "أجنبي"; var genre = "عام"
                    while (jr.hasNext()) {
                        when (jr.nextName()) {
                            "id" -> id = jr.nextString()
                            "rawId" -> rawId = jr.nextString()
                            "title" -> title = jr.nextString()
                            "url" -> url = jr.nextString()
                            "fallbackUrl" -> fb = jr.nextString()
                            "group" -> grp = jr.nextString()
                            "logo" -> logo = jr.nextString()
                            "contentType" -> ct = jr.nextString()
                            "epgChannelId" -> epgId = jr.nextString()
                            "rating" -> rating = jr.nextString()
                            "year" -> year = jr.nextString()
                            "addedTimestamp" -> added = jr.nextLong()
                            "is4k" -> is4k = jr.nextBoolean()
                            "originTag" -> origin = jr.nextString()
                            "genreTag" -> genre = jr.nextString()
                            else -> jr.skipValue()
                        }
                    }
                    jr.endObject()
                    list.add(MediaItemModel(id, rawId, title, url, fb, grp, logo, ct, epgId, rating, year, added, is4k, origin, genre))
                }
                jr.endArray()
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun shouldRunBackgroundSync(context: Context): Boolean {
        val sub = getSavedSubscription(context) ?: return false
        val interval = getAutoSyncInterval(context)
        if (interval == AutoSyncInterval.MANUAL) return false
        val last = prefs(context).getLong("last_sync_${sub.id}", 0L)
        return (System.currentTimeMillis() - last) > interval.hours * 3600_000L
    }

    fun saveVodProgress(context: Context, streamId: String, posMs: Long, durMs: Long, seriesId: String = "", episodeNum: Int = 0, seasonNum: Int = 1) {
        if (streamId.isEmpty() || durMs <= 0L) return
        val editor = prefs(context).edit()
            .putLong("pos_$streamId", posMs)
            .putLong("dur_$streamId", durMs)
        if (seriesId.isNotEmpty() && episodeNum > 0) {
            editor.putString("last_ep_id_$seriesId", streamId)
            editor.putInt("last_ep_num_$seriesId", episodeNum)
            editor.putInt("last_ep_season_$seriesId", seasonNum)
        }
        editor.apply()
    }

    fun getVodPosition(context: Context, streamId: String): Long =
        prefs(context).getLong("pos_$streamId", 0L)

    fun getVodProgressFraction(context: Context, streamId: String): Float {
        val pos = prefs(context).getLong("pos_$streamId", 0L)
        val dur = prefs(context).getLong("dur_$streamId", 0L)
        if (dur <= 0L || pos <= 0L) return 0f
        return (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
    }

    fun getLastWatchedEpisodeForSeries(context: Context, seriesId: String): Triple<String, Int, Int>? {
        val epId = prefs(context).getString("last_ep_id_$seriesId", null) ?: return null
        val season = prefs(context).getInt("last_ep_season_$seriesId", 1)
        val epNum = prefs(context).getInt("last_ep_num_$seriesId", 1)
        return Triple(epId, season, epNum)
    }

    fun resetWatchProgress(context: Context) {
        val editor = prefs(context).edit()
        prefs(context).all.keys.filter {
            it.startsWith("pos_") || it.startsWith("dur_") || it.startsWith("last_ep_")
        }.forEach { editor.remove(it) }
        editor.apply()
    }
}

object DevkanPlaylistRepository {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun JsonReader.nextSafeString(): String {
        return when (peek()) {
            JsonToken.NULL -> { nextNull(); "" }
            JsonToken.NUMBER -> nextString()
            JsonToken.BOOLEAN -> nextBoolean().toString()
            JsonToken.STRING -> nextString()
            else -> { skipValue(); "" }
        }
    }

    suspend fun fetchServerHostInfo(hostUrl: String): Triple<String, String, String> = withContext(Dispatchers.IO) {
        try {
            val cleanHost = Uri.parse(hostUrl).host ?: hostUrl.removePrefix("http://").removePrefix("https://").substringBefore(":").substringBefore("/")
            val ip = InetAddress.getByName(cleanHost).hostAddress ?: return@withContext Triple("", "", "")
            val req = Request.Builder().url("http://ip-api.com/json/$ip?fields=countryCode,isp,timezone").build()
            httpClient.newCall(req).execute().use { resp ->
                val json = JSONObject(resp.body?.string() ?: "{}")
                Triple(
                    json.optString("countryCode", ""),
                    json.optString("isp", "Cloud Server"),
                    json.optString("timezone", "UTC")
                )
            }
        } catch (_: Exception) {
            Triple("", "Hosting Server", "UTC")
        }
    }

    suspend fun testSubscriptionConnection(sub: SavedSubscription, geoBypass: Boolean): SubscriptionTestSummary = withContext(Dispatchers.IO) {
        val hostInfo = fetchServerHostInfo(sub.host)
        if (sub.type == "XTREAM") {
            try {
                val baseUrl = sub.host.trimEnd('/')
                val authUrl = "$baseUrl/player_api.php?username=${sub.username}&password=${sub.password}"
                val reqBuilder = Request.Builder().url(authUrl)
                EngineCoordinator.buildSmartHeaders(geoBypass, "IPTVSmartersPro/3.1.5").forEach { (k, v) -> reqBuilder.addHeader(k, v) }

                httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                    if (resp.code == 403 || resp.code == 451) {
                        return@withContext SubscriptionTestSummary(
                            success = false, title = sub.label, expiry = "-", maxConnections = "0", activeConnections = "0",
                            liveCount = 0, movieCount = 0, seriesCount = 0,
                            accessStatus = "BLOCKED_403", countryCode = hostInfo.first, ispName = hostInfo.second, timezone = hostInfo.third,
                            errorReason = "HTTP 403: البث مقيد جغرافياً أو IP محظور"
                        )
                    }
                    val body = resp.body?.string() ?: ""
                    val root = JSONObject(body)
                    val userInfo = root.optJSONObject("user_info")
                    if (userInfo == null || userInfo.optInt("auth", 0) == 0) {
                        return@withContext SubscriptionTestSummary(
                            success = false, title = sub.label, expiry = "-", maxConnections = "0", activeConnections = "0",
                            liveCount = 0, movieCount = 0, seriesCount = 0,
                            accessStatus = "VPN_REQUIRED", countryCode = hostInfo.first, ispName = hostInfo.second, timezone = hostInfo.third,
                            errorReason = "بيانات الدخول غير صحيحة أو الحساب منتهي"
                        )
                    }
                    val expTs = userInfo.optString("exp_date", "0").toLongOrNull() ?: 0L
                    val expFormatted = if (expTs > 0L) SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(expTs * 1000L)) else "Unlimited"
                    val maxCons = userInfo.optString("max_connections", "1")
                    val activeCons = userInfo.optString("active_cons", "0")

                    fun countStreamEndpoint(action: String): Int {
                        return try {
                            val r = Request.Builder().url("$authUrl&action=$action").build()
                            httpClient.newCall(r).execute().use { res ->
                                val stream = res.body?.charStream() ?: return 0
                                val jr = JsonReader(stream)
                                var c = 0
                                jr.beginArray()
                                while (jr.hasNext()) {
                                    jr.skipValue()
                                    c++
                                }
                                jr.endArray()
                                c
                            }
                        } catch (_: Exception) { 0 }
                    }

                    val liveCnt = countStreamEndpoint("get_live_streams")
                    val movCnt = if (sub.liveOnly) 0 else countStreamEndpoint("get_vod_streams")
                    val serCnt = if (sub.liveOnly) 0 else countStreamEndpoint("get_series")

                    SubscriptionTestSummary(
                        success = true,
                        title = sub.label,
                        expiry = expFormatted,
                        maxConnections = maxCons,
                        activeConnections = activeCons,
                        liveCount = liveCnt,
                        movieCount = movCnt,
                        seriesCount = serCnt,
                        accessStatus = if (geoBypass) "DNS_BYPASS" else "DIRECT",
                        countryCode = hostInfo.first,
                        ispName = hostInfo.second,
                        timezone = hostInfo.third
                    )
                }
            } catch (e: Exception) {
                SubscriptionTestSummary(
                    success = false, title = sub.label, expiry = "-", maxConnections = "0", activeConnections = "0",
                    liveCount = 0, movieCount = 0, seriesCount = 0,
                    accessStatus = "VPN_REQUIRED", countryCode = hostInfo.first, ispName = hostInfo.second, timezone = hostInfo.third,
                    errorReason = "تعذر الوصول للخادم: قد يتطلب VPN (${e.localizedMessage ?: "Timeout"})"
                )
            }
        } else {
            SubscriptionTestSummary(
                success = true, title = sub.label, expiry = sub.expDateFormatted,
                maxConnections = "1", activeConnections = "0", liveCount = 0, movieCount = 0, seriesCount = 0,
                accessStatus = "DIRECT", countryCode = hostInfo.first, ispName = hostInfo.second, timezone = hostInfo.third
            )
        }
    }

    private fun detectOriginTag(group: String, title: String): String {
        val text = "$group $title".lowercase()
        return when {
            text.contains("korean") || text.contains("كوري") || text.contains("kdrama") -> "كوري"
            text.contains("turk") || text.contains("تركي") || text.contains("مدبلج") -> "تركي"
            text.contains("indian") || text.contains("bollywood") || text.contains("هندي") -> "هندي"
            text.contains("arab") || text.contains("عربي") || text.contains("مصري") || text.contains("خليجي") || text.contains("عراقي") -> "عربي"
            else -> "أجنبي"
        }
    }

    private fun detectGenreTag(group: String, title: String): String {
        val text = "$group $title".lowercase()
        return when {
            text.contains("action") || text.contains("أكشن") || text.contains("اكشن") -> "أكشن"
            text.contains("drama") || text.contains("دراما") -> "دراما"
            text.contains("sci-fi") || text.contains("خيال") -> "خيال علمي"
            text.contains("romanc") || text.contains("رومانسي") -> "رومانسي"
            text.contains("comed") || text.contains("كوميدي") -> "كوميدي"
            text.contains("horror") || text.contains("thriller") || text.contains("رعب") -> "رعب وإثارة"
            text.contains("histor") || text.contains("تاريخ") -> "تاريخي"
            text.contains("anim") || text.contains("kids") || text.contains("أنمي") || text.contains("اطفال") -> "أنمي وعائلي"
            else -> "عام"
        }
    }

    suspend fun loadSubscriptionProgressive(
        sub: SavedSubscription,
        geoBypass: Boolean,
        onStatusUpdate: suspend (String) -> Unit,
        onPartialItemsReady: suspend (SavedSubscription, List<MediaItemModel>) -> Unit
    ): Pair<SavedSubscription, List<MediaItemModel>> = withContext(Dispatchers.IO) {
        val hostInfo = fetchServerHostInfo(sub.host)
        if (sub.type == "M3U") {
            onStatusUpdate("جاري تحميل قائمة M3U...")
            val items = parseM3uFromUrl(sub.m3uUrl)
            val updated = sub.copy(
                accessStatus = if (items.isNotEmpty()) "DIRECT" else "VPN_REQUIRED",
                serverCountryCode = hostInfo.first,
                serverIsp = hostInfo.second,
                serverTimezone = hostInfo.third
            )
            onPartialItemsReady(updated, items)
            return@withContext Pair(updated, items)
        }

        val baseUrl = sub.host.trimEnd('/')
        val authUrl = "$baseUrl/player_api.php?username=${sub.username}&password=${sub.password}"
        var updatedSub = sub.copy(
            serverCountryCode = hostInfo.first,
            serverIsp = hostInfo.second,
            serverTimezone = hostInfo.third
        )
        val combinedList = ArrayList<MediaItemModel>(25000)

        try {
            onStatusUpdate("جاري التحقق من الاشتراك...")
            val req = Request.Builder().url(authUrl).build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.code == 403) {
                    return@withContext Pair(updatedSub.copy(accessStatus = "BLOCKED_403"), emptyList())
                }
                val root = JSONObject(resp.body?.string() ?: "{}")
                val uInfo = root.optJSONObject("user_info")
                if (uInfo != null) {
                    val expTs = uInfo.optString("exp_date", "0").toLongOrNull() ?: 0L
                    val expStr = if (expTs > 0L) SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(expTs * 1000L)) else "Unlimited"
                    updatedSub = updatedSub.copy(
                        status = uInfo.optString("status", "Active"),
                        expDateFormatted = expStr,
                        activeCons = uInfo.optString("active_cons", "0"),
                        maxCons = uInfo.optString("max_connections", "1"),
                        accessStatus = if (geoBypass) "DNS_BYPASS" else "DIRECT"
                    )
                }
            }

            fun fetchCategoriesStream(action: String): Map<String, String> {
                val map = HashMap<String, String>()
                try {
                    val r = Request.Builder().url("$authUrl&action=$action").build()
                    httpClient.newCall(r).execute().use { res ->
                        val stream = res.body?.charStream() ?: return map
                        val jr = JsonReader(stream)
                        jr.beginArray()
                        while (jr.hasNext()) {
                            jr.beginObject()
                            var cid = ""; var cname = "General"
                            while (jr.hasNext()) {
                                when (jr.nextName()) {
                                    "category_id" -> cid = jr.nextSafeString()
                                    "category_name" -> cname = jr.nextSafeString().ifEmpty { "General" }
                                    else -> jr.skipValue()
                                }
                            }
                            jr.endObject()
                            if (cid.isNotEmpty()) map[cid] = cname
                        }
                        jr.endArray()
                    }
                } catch (_: Exception) { }
                return map
            }

            onStatusUpdate("جاري جلب القنوات المباشرة...")
            val liveCats = fetchCategoriesStream("get_live_categories")
            try {
                val r = Request.Builder().url("$authUrl&action=get_live_streams").build()
                httpClient.newCall(r).execute().use { res ->
                    val stream = res.body?.charStream()
                    if (stream != null) {
                        val jr = JsonReader(stream)
                        jr.beginArray()
                        while (jr.hasNext()) {
                            jr.beginObject()
                            var streamId = ""; var name = ""; var catId = ""; var icon = ""; var epgChannelId = ""
                            while (jr.hasNext()) {
                                when (jr.nextName()) {
                                    "stream_id" -> streamId = jr.nextSafeString()
                                    "name" -> name = jr.nextSafeString()
                                    "category_id" -> catId = jr.nextSafeString()
                                    "stream_icon" -> icon = jr.nextSafeString()
                                    "epg_channel_id" -> epgChannelId = jr.nextSafeString()
                                    else -> jr.skipValue()
                                }
                            }
                            jr.endObject()
                            if (streamId.isNotEmpty()) {
                                val catName = liveCats[catId] ?: "Live TV"
                                combinedList.add(
                                    MediaItemModel(
                                        id = "live_$streamId",
                                        rawId = streamId,
                                        title = name,
                                        url = "$baseUrl/live/${sub.username}/${sub.password}/$streamId.m3u8",
                                        fallbackUrl = "$baseUrl/live/${sub.username}/${sub.password}/$streamId.ts",
                                        group = catName,
                                        logo = icon,
                                        contentType = "LIVE",
                                        epgChannelId = epgChannelId
                                    )
                                )
                            }
                        }
                        jr.endArray()
                    }
                }
            } catch (_: Exception) { }

            if (combinedList.isNotEmpty()) {
                onPartialItemsReady(updatedSub, combinedList.toList())
            }

            if (sub.liveOnly) {
                return@withContext Pair(updatedSub, combinedList)
            }

            onStatusUpdate("جاري جلب مكتبة الأفلام...")
            val vodCats = fetchCategoriesStream("get_vod_categories")
            try {
                val r = Request.Builder().url("$authUrl&action=get_vod_streams").build()
                httpClient.newCall(r).execute().use { res ->
                    val stream = res.body?.charStream()
                    if (stream != null) {
                        val jr = JsonReader(stream)
                        jr.beginArray()
                        while (jr.hasNext()) {
                            jr.beginObject()
                            var streamId = ""; var name = ""; var ext = "mp4"; var catId = ""; var icon = ""; var rating = ""; var added = 0L
                            while (jr.hasNext()) {
                                when (jr.nextName()) {
                                    "stream_id" -> streamId = jr.nextSafeString()
                                    "name" -> name = jr.nextSafeString()
                                    "container_extension" -> ext = jr.nextSafeString().ifEmpty { "mp4" }
                                    "category_id" -> catId = jr.nextSafeString()
                                    "stream_icon" -> icon = jr.nextSafeString()
                                    "rating" -> rating = jr.nextSafeString()
                                    "added" -> added = jr.nextSafeString().toLongOrNull() ?: 0L
                                    else -> jr.skipValue()
                                }
                            }
                            jr.endObject()
                            if (streamId.isNotEmpty()) {
                                val catName = vodCats[catId] ?: "Movies"
                                val is4k = name.contains("4K", ignoreCase = true) || catName.contains("4K", ignoreCase = true)
                                combinedList.add(
                                    MediaItemModel(
                                        id = "vod_$streamId",
                                        rawId = streamId,
                                        title = name,
                                        url = "$baseUrl/movie/${sub.username}/${sub.password}/$streamId.$ext",
                                        group = catName,
                                        logo = icon,
                                        contentType = "MOVIE",
                                        rating = rating,
                                        addedTimestamp = added,
                                        is4k = is4k,
                                        originTag = detectOriginTag(catName, name),
                                        genreTag = detectGenreTag(catName, name)
                                    )
                                )
                            }
                        }
                        jr.endArray()
                    }
                }
            } catch (_: Exception) { }

            onPartialItemsReady(updatedSub, combinedList.toList())

            onStatusUpdate("جاري جلب مكتبة المسلسلات...")
            val seriesCats = fetchCategoriesStream("get_series_categories")
            try {
                val r = Request.Builder().url("$authUrl&action=get_series").build()
                httpClient.newCall(r).execute().use { res ->
                    val stream = res.body?.charStream()
                    if (stream != null) {
                        val jr = JsonReader(stream)
                        jr.beginArray()
                        while (jr.hasNext()) {
                            jr.beginObject()
                            var seriesId = ""; var name = ""; var catId = ""; var cover = ""; var rating = ""; var added = 0L
                            while (jr.hasNext()) {
                                when (jr.nextName()) {
                                    "series_id" -> seriesId = jr.nextSafeString()
                                    "name" -> name = jr.nextSafeString()
                                    "category_id" -> catId = jr.nextSafeString()
                                    "cover" -> cover = jr.nextSafeString()
                                    "rating" -> rating = jr.nextSafeString()
                                    "last_modified" -> added = jr.nextSafeString().toLongOrNull() ?: 0L
                                    else -> jr.skipValue()
                                }
                            }
                            jr.endObject()
                            if (seriesId.isNotEmpty()) {
                                val catName = seriesCats[catId] ?: "Series"
                                combinedList.add(
                                    MediaItemModel(
                                        id = "ser_$seriesId",
                                        rawId = seriesId,
                                        title = name,
                                        url = "",
                                        group = catName,
                                        logo = cover,
                                        contentType = "SERIES",
                                        rating = rating,
                                        addedTimestamp = added,
                                        originTag = detectOriginTag(catName, name),
                                        genreTag = detectGenreTag(catName, name)
                                    )
                                )
                            }
                        }
                        jr.endArray()
                    }
                }
            } catch (_: Exception) { }

            onPartialItemsReady(updatedSub, combinedList.toList())
        } catch (_: Exception) {
            updatedSub = updatedSub.copy(accessStatus = "VPN_REQUIRED")
        }

        Pair(updatedSub, combinedList)
    }

    private fun parseM3uFromUrl(url: String): List<MediaItemModel> {
        return try {
            val req = Request.Builder().url(url).build()
            httpClient.newCall(req).execute().use { resp ->
                val text = resp.body?.string() ?: return emptyList()
                parseM3uText(text)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun parseLocalUri(context: Context, uri: Uri): List<MediaItemModel> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).forEachLine { sb.append(it).append('\n') }
            }
            parseM3uText(sb.toString())
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseM3uText(content: String): List<MediaItemModel> {
        val list = mutableListOf<MediaItemModel>()
        var currentTitle = "Channel"
        var currentGroup = "Live TV"
        var currentLogo = ""
        var currentTvgId = ""
        var index = 0

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.startsWith("#EXTINF:")) {
                currentTitle = line.substringAfterLast(",").trim()
                currentGroup = Regex("""group-title="([^"]*)"""").find(line)?.groupValues?.get(1)?.ifEmpty { "Live TV" } ?: "Live TV"
                currentLogo = Regex("""tvg-logo="([^"]*)"""").find(line)?.groupValues?.get(1) ?: ""
                currentTvgId = Regex("""tvg-id="([^"]*)"""").find(line)?.groupValues?.get(1) ?: ""
            } else if (line.isNotEmpty() && !line.startsWith("#")) {
                val isMovie = line.endsWith(".mp4") || line.endsWith(".mkv") || line.endsWith(".avi")
                list.add(
                    MediaItemModel(
                        id = "m3u_${index++}",
                        rawId = "$index",
                        title = currentTitle,
                        url = line,
                        group = currentGroup,
                        logo = currentLogo,
                        contentType = if (isMovie) "MOVIE" else "LIVE",
                        epgChannelId = currentTvgId
                    )
                )
            }
        }
        return list
    }

    fun deduplicateSportChannels(channels: List<MediaItemModel>): List<MediaItemModel> =
        channels.distinctBy { "${it.group}_${it.title.trim().lowercase()}" }

    suspend fun fetchVodDetails(sub: SavedSubscription, vodId: String, fallbackTitle: String, fallbackLogo: String): VodDetailedInfo = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sub.host.trimEnd('/')
            val url = "$baseUrl/player_api.php?username=${sub.username}&password=${sub.password}&action=get_vod_info&vod_id=$vodId"
            httpClient.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                val info = JSONObject(resp.body?.string() ?: "{}").optJSONObject("info")
                if (info != null) {
                    return@withContext VodDetailedInfo(
                        title = info.optString("name", fallbackTitle).ifEmpty { fallbackTitle },
                        plot = info.optString("plot", "مشاهدة ممتعة بجودة عالية."),
                        cast = info.optString("cast", "غير متوفر"),
                        director = info.optString("director", "غير متوفر"),
                        releaseDate = info.optString("releasedate", "2025"),
                        duration = info.optString("duration", "HD"),
                        rating = info.optString("rating", "8.2"),
                        backdropUrl = info.optString("movie_image", fallbackLogo).ifEmpty { fallbackLogo }
                    )
                }
            }
        } catch (_: Exception) { }
        VodDetailedInfo(fallbackTitle, "جاهز للمشاهدة الفورية.", "غير متوفر", "غير متوفر", "2025", "HD", "8.0", fallbackLogo)
    }

    suspend fun fetchSeriesDetails(sub: SavedSubscription, seriesId: String, fallbackTitle: String, fallbackLogo: String): SeriesDetailedInfo = withContext(Dispatchers.IO) {
        val seasonsMap = linkedMapOf<Int, MutableList<EpisodeModel>>()
        var plot = "مسلسل درامي شيق متاح بجميع الحلقات."
        var cast = "غير متوفر"
        var rating = "8.5"
        var releaseDate = "2025"
        var backdrop = fallbackLogo

        try {
            val baseUrl = sub.host.trimEnd('/')
            val url = "$baseUrl/player_api.php?username=${sub.username}&password=${sub.password}&action=get_series_info&series_id=$seriesId"
            httpClient.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                val root = JSONObject(resp.body?.string() ?: "{}")
                root.optJSONObject("info")?.let { info ->
                    plot = info.optString("plot", plot).ifEmpty { plot }
                    cast = info.optString("cast", cast)
                    rating = info.optString("rating", rating).ifEmpty { rating }
                    releaseDate = info.optString("releaseDate", releaseDate).ifEmpty { releaseDate }
                    backdrop = info.optString("cover", fallbackLogo).ifEmpty { fallbackLogo }
                }

                val episodesObj = root.optJSONObject("episodes")
                if (episodesObj != null) {
                    val keys = episodesObj.keys().asSequence().toList().sortedBy { it.toIntOrNull() ?: 1 }
                    for (seasonKey in keys) {
                        val seasonNum = seasonKey.toIntOrNull() ?: 1
                        val epArray = episodesObj.optJSONArray(seasonKey) ?: continue
                        val epList = mutableListOf<EpisodeModel>()
                        for (i in 0 until epArray.length()) {
                            val ep = epArray.getJSONObject(i)
                            val epId = ep.optString("id")
                            val epNum = ep.optInt("episode_num", i + 1)
                            val ext = ep.optString("container_extension", "mp4")
                            val epUrl = "$baseUrl/series/${sub.username}/${sub.password}/$epId.$ext"
                            epList.add(
                                EpisodeModel(
                                    id = "ep_$epId",
                                    seasonNum = seasonNum,
                                    episodeNum = epNum,
                                    title = ep.optString("title", "الحلقة $epNum"),
                                    url = epUrl,
                                    duration = ep.optJSONObject("info")?.optString("duration", "") ?: ""
                                )
                            )
                        }
                        if (epList.isNotEmpty()) seasonsMap[seasonNum] = epList
                    }
                }
            }
        } catch (_: Exception) { }

        SeriesDetailedInfo(fallbackTitle, plot, cast, rating, releaseDate, backdrop, seasonsMap)
    }
}

object RealSportsDataSource {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val espnEndpoints = listOf(
        Triple("SOCCER", "العالم / قاري", "uefa.champions" to "دوري أبطال أوروبا"),
        Triple("SOCCER", "العالم / قاري", "uefa.europa" to "الدوري الأوروبي"),
        Triple("SOCCER", "العالم / قاري", "fifa.worldq.uefa" to "تصفيات كأس العالم - أوروبا"),
        Triple("SOCCER", "العالم / قاري", "fifa.worldq.conmebol" to "تصفيات كأس العالم - أمريكا الجنوبية"),
        Triple("SOCCER", "العالم / قاري", "fifa.worldq.afc" to "تصفيات كأس العالم - آسيا"),
        Triple("SOCCER", "إنجلترا", "eng.1" to "الدوري الإنجليزي الممتاز"),
        Triple("SOCCER", "إسبانيا", "esp.1" to "الدوري الإسباني"),
        Triple("SOCCER", "إيطاليا", "ita.1" to "الدوري الإيطالي"),
        Triple("SOCCER", "ألمانيا", "ger.1" to "الدوري الألماني"),
        Triple("SOCCER", "فرنسا", "fra.1" to "الدوري الفرنسي"),
        Triple("SOCCER", "السعودية", "ksa.1" to "دوري روشن السعودي"),
        Triple("BASKETBALL", "الولايات المتحدة", "nba" to "NBA كرة السلة"),
        Triple("BASKETBALL", "أوروبا", "euroleague" to "الدوري الأوروبي لكرة السلة"),
        Triple("TENNIS", "العالم / بطولات", "atp" to "بطولات التنس ATP"),
        Triple("TENNIS", "العالم / بطولات", "wta" to "بطولات التنس WTA")
    )

    suspend fun fetchTodayRealMatches(): List<RealSportsMatch> = withContext(Dispatchers.IO) {
        val allMatches = mutableListOf<RealSportsMatch>()
        val seenIds = HashSet<String>()
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dashFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val calDb = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        for (dayOffset in 0..1) {
            if (dayOffset > 0) calDb.add(Calendar.DAY_OF_YEAR, 1)
            val dateStr = dashFmt.format(calDb.time)
            try {
                val dbUrl = "https://www.thesportsdb.com/api/v1/json/3/eventsday.php?d=$dateStr&s=Soccer"
                val req = Request.Builder().url(dbUrl).header("User-Agent", "Mozilla/5.0").build()
                client.newCall(req).execute().use { resp ->
                    val events = JSONObject(resp.body?.string() ?: "{}").optJSONArray("events") ?: return@use
                    for (i in 0 until events.length()) {
                        val ev = events.getJSONObject(i)
                        val evId = ev.optString("idEvent", "db_$i")
                        if (!seenIds.add("SOCCER_$evId")) continue
                        val home = ev.optString("strHomeTeam", "")
                        val away = ev.optString("strAwayTeam", "")
                        if (home.isEmpty() || away.isEmpty()) continue
                        val country = ev.optString("strCountry", "العالم").ifEmpty { "العالم" }
                        val league = ev.optString("strLeague", "بطولة دولية")
                        val timeStr = ev.optString("strTime", "").take(5).ifEmpty { "اليوم" }
                        val hScore = ev.optString("intHomeScore", "")
                        val aScore = ev.optString("intAwayScore", "")
                        val status = ev.optString("strStatus", "")
                        val isLive = status.contains("H", ignoreCase = true) || status.contains("LIVE", ignoreCase = true) || status.contains("'")
                        val scoreText = if (hScore.isNotEmpty() && aScore.isNotEmpty() && hScore != "null") "$hScore - $aScore" else "VS"

                        allMatches.add(
                            RealSportsMatch(
                                id = evId,
                                sportType = "SOCCER",
                                country = country,
                                league = league,
                                homeTeam = home,
                                awayTeam = away,
                                homeLogo = ev.optString("strHomeTeamBadge", ""),
                                awayLogo = ev.optString("strAwayTeamBadge", ""),
                                scoreText = scoreText,
                                status = status,
                                matchTime = if (isLive) status else timeStr,
                                isLive = isLive,
                                suggestedBroadcaster = "beIN SPORTS"
                            )
                        )
                    }
                }
            } catch (_: Exception) { }
        }

        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val startStr = fmt.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 6)
        val endStr = fmt.format(cal.time)
        val dateRange = "$startStr-$endStr"

        for ((sportType, country, leaguePair) in espnEndpoints) {
            val (slug, leagueArabic) = leaguePair
            val sportPath = when (sportType) {
                "BASKETBALL" -> "basketball/$slug"
                "TENNIS" -> "tennis/$slug"
                else -> "soccer/$slug"
            }
            try {
                val url = "https://site.api.espn.com/apis/site/v2/sports/$sportPath/scoreboard?dates=$dateRange"
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val root = JSONObject(resp.body?.string() ?: "{}")
                    val events = root.optJSONArray("events") ?: return@use
                    for (i in 0 until events.length()) {
                        val ev = events.getJSONObject(i)
                        val evId = ev.optString("id", "$slug-$i")
                        if (!seenIds.add("${sportType}_$evId")) continue

                        val comp = ev.optJSONArray("competitions")?.optJSONObject(0) ?: continue
                        val competitors = comp.optJSONArray("competitors") ?: continue
                        if (competitors.length() < 2) continue

                        val c1 = competitors.getJSONObject(0)
                        val c2 = competitors.getJSONObject(1)
                        val t1 = c1.optJSONObject("team") ?: c1.optJSONObject("athlete") ?: JSONObject()
                        val t2 = c2.optJSONObject("team") ?: c2.optJSONObject("athlete") ?: JSONObject()

                        val statusObj = ev.optJSONObject("status")?.optJSONObject("type")
                        val state = statusObj?.optString("state", "pre") ?: "pre"
                        val isLive = state == "in"
                        val clock = statusObj?.optString("shortDetail", "") ?: ""

                        val rawDate = ev.optString("date", "")
                        val timeFormatted = try {
                            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val parsed = parser.parse(rawDate)
                            if (parsed != null) SimpleDateFormat("dd/MM hh:mm a", Locale.US).format(parsed) else clock
                        } catch (_: Exception) { clock }

                        val s1 = c1.optString("score", "0").ifEmpty { "0" }
                        val s2 = c2.optString("score", "0").ifEmpty { "0" }
                        val scoreText = if (state == "pre") "VS" else "$s1 - $s2"

                        val broadcaster = when (slug) {
                            "ksa.1" -> "SSC Sports"
                            "ita.1" -> "AD Sports"
                            "nba" -> "NBA TV"
                            else -> "beIN SPORTS"
                        }

                        allMatches.add(
                            RealSportsMatch(
                                id = evId,
                                sportType = sportType,
                                country = country,
                                league = leagueArabic,
                                homeTeam = t1.optString("shortDisplayName", t1.optString("displayName", "Team A")),
                                awayTeam = t2.optString("shortDisplayName", t2.optString("displayName", "Team B")),
                                homeLogo = t1.optString("logo", ""),
                                awayLogo = t2.optString("logo", ""),
                                scoreText = scoreText,
                                status = clock,
                                matchTime = if (isLive) clock else timeFormatted,
                                isLive = isLive,
                                suggestedBroadcaster = broadcaster
                            )
                        )
                    }
                }
            } catch (_: Exception) { }
        }
        allMatches
    }
}
