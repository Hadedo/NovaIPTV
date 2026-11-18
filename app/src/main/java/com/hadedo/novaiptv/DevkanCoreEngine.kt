package com.hadedo.novaiptv

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

data class SavedSubscription(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "DEVKAN VIP",
    val type: String = "XTREAM",
    val host: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val macAddress: String = "",
    val status: String = "Active",
    val expDateFormatted: String = "Unlimited",
    val activeCons: String = "0",
    val maxCons: String = "1",
    val allowedCountries: String = "Global",
    val accessStatus: String = "DIRECT", // DIRECT, DNS_BYPASS, VPN_REQUIRED, BLOCKED_403
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
    val sportType: String, // SOCCER, BASKETBALL, TENNIS
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
    val suggestedBroadcaster: String
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
    val channelName: String,
    val channelLogo: String,
    val currentProgram: String,
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

// ============================================================================
// محرك ترتيب التصنيفات الذكي حسب جميع اللغات الـ 6
// ============================================================================
object LanguagePrioritySorter {
    fun sortGroupsByLanguage(groups: List<String>, lang: String): List<String> {
        if (groups.size <= 2) return groups
        val hasAll = groups.firstOrNull() == "ALL"
        val body = if (hasAll) groups.drop(1) else groups

        val keywords = when (lang) {
            "ar" -> listOf(
                "bein", "ssc", "alkass", "kass", "الكأس", "الكاس", "rabiaa", "الرابعة",
                "alwan", "ألوان", "الوان", "kuwait", "الكويت", "abu dhabi", "ad sport", "أبو ظبي",
                "ar:", "ar |", "arabic", "عربي", "عربية", "mbc", "osn", "rotana", "روتانا",
                "iraq", "العراق", "عراق", "gulf", "خليج", "egypt", "مصر", "shahid", "شاهد", "tod", "news", "اخبار"
            )
            "fr" -> listOf(
                "fr:", "fr |", "france", "french", "francophone", "canal+", "canal plus",
                "bein fr", "rmc", "tf1", "m6", "tous", "arte", "france 2", "france 3", "eurosport fr"
            )
            "en" -> listOf(
                "uk:", "uk |", "us:", "us |", "usa", "english", "sky uk", "tnt sport", "bt sport",
                "nbc", "cbs", "abc", "fox", "espn", "hbo", "showtime", "premier", "bbc", "itv"
            )
            "de" -> listOf(
                "de:", "de |", "germany", "german", "deutsch", "sky de", "dazn de", "zdf",
                "ard", "rtl", "prosieben", "sat.1", "austria", "at:", "switzerland", "ch:"
            )
            "tr" -> listOf(
                "tr:", "tr |", "turkey", "turkish", "türk", "bein tr", "exxen", "trt",
                "atv", "show tv", "star tv", "kanal d", "tv8", "fox tr", "spor"
            )
            "ku" -> listOf(
                "ku:", "ku |", "kurd", "kurdish", "kurdistan", "كورد", "كردي",
                "rudaw", "k24", "kurdistan 24", "nrt", "kurdmax", "waar", "speda", "ava"
            )
            else -> emptyList()
        }

        if (keywords.isEmpty()) return groups

        val sorted = body.sortedWith(
            compareBy<String> { grp ->
                val lower = grp.lowercase()
                val idx = keywords.indexOfFirst { lower.contains(it) }
                if (idx >= 0) idx else 10_000
            }.thenBy { it.lowercase() }
        )
        return if (hasAll) listOf("ALL") + sorted else sorted
    }
}

object SportsMatchExtractor {
    private val sportsKeywords = listOf(
        "sport", "sports", "رياضة", "رياضية", "bein", "ssc", "alkass", "kass", "الكأس", "الكاس",
        "rabiaa", "الرابعة", "alwan", "ألوان", "الوان", "kuwait", "الكويت", "abu dhabi", "ad sport",
        "أبو ظبي", "ابوظبي", "dubai sport", "on time", "أون تايم", "dazn", "sky sport", "arena sport",
        "bt sport", "tnt sport", "espn", "eurosport", "supersport", "premier", "champions", "liga"
    )

    fun isSportsProgram(group: String?, title: String?, category: String?): Boolean {
        val combined = "${group.orEmpty()} ${title.orEmpty()} ${category.orEmpty()}".lowercase()
        return sportsKeywords.any { combined.contains(it) }
    }
}

object EpgSportsPipeline {
    fun classifyAndGroupSports(programs: List<EpgProgram>): ClassifiedSportsData {
        val now = System.currentTimeMillis()
        val in24h = now + 24 * 3600_000L
        val in7d = now + 7 * 24 * 3600_000L
        val liveList = mutableListOf<GroupedSportsMatch>()
        val next24 = mutableListOf<GroupedSportsMatch>()
        val next7 = mutableListOf<GroupedSportsMatch>()

        programs.groupBy { it.title.lowercase().trim() }.forEach { (key, list) ->
            val first = list.first()
            val parts = first.title.split(" vs ", " VS ", " - ", " 🆚 ")
            if (parts.size >= 2) {
                val isLive = now in first.startTime..first.endTime
                val gm = GroupedSportsMatch(
                    matchKey = key,
                    homeTeam = parts[0].trim(),
                    awayTeam = parts[1].trim(),
                    competition = first.category,
                    startTime = first.startTime,
                    endTime = first.endTime,
                    isLive = isLive,
                    streams = list,
                    primaryStream = first
                )
                when {
                    isLive -> liveList.add(gm)
                    first.startTime in now..in24h -> next24.add(gm)
                    first.startTime in in24h..in7d -> next7.add(gm)
                }
            }
        }
        return ClassifiedSportsData(liveList, next24, next7, programs)
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

// ============================================================================
// الخزنة المركزية (الاشتراكات المتعددة + الفئات المرئية + استكمال الحلقات)
// ============================================================================
object DevkanVault {
    private const val PREFS_NAME = "devkan_vip_vault_v2"

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

    // إدارة الاشتراكات المتعددة
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

    // حذف أي اشتراك محدد بالـ ID مباشرة دون الحاجة للدخول إليه
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

    fun clearSubscription(context: Context) {
        val current = getSavedSubscription(context) ?: return
        deleteSubscriptionById(context, current.id)
    }

    // إدارة الفئات المرئية / المخفية لكل اشتراك (Live / Movie / Series)
    fun getHiddenCategories(context: Context, subId: String, type: String): Set<String> {
        return prefs(context).getStringSet("hidden_${subId}_$type", emptySet()) ?: emptySet()
    }

    fun setHiddenCategories(context: Context, subId: String, type: String, hidden: Set<String>) {
        prefs(context).edit().putStringSet("hidden_${subId}_$type", hidden).apply()
    }

    // تخزين قائمة العناصر محلياً لكل اشتراك
    fun saveCachedItems(context: Context, items: List<MediaItemModel>, subId: String = "default") {
        try {
            val arr = JSONArray()
            items.forEach { item ->
                arr.put(JSONObject().apply {
                    put("id", item.id)
                    put("rawId", item.rawId)
                    put("title", item.title)
                    put("url", item.url)
                    put("fallbackUrl", item.fallbackUrl)
                    put("group", item.group)
                    put("logo", item.logo)
                    put("contentType", item.contentType)
                    put("rating", item.rating)
                    put("year", item.year)
                    put("addedTimestamp", item.addedTimestamp)
                    put("is4k", item.is4k)
                    put("originTag", item.originTag)
                    put("genreTag", item.genreTag)
                })
            }
            java.io.File(context.filesDir, "cache_$subId.json").writeText(arr.toString())
            prefs(context).edit().putLong("last_sync_$subId", System.currentTimeMillis()).apply()
        } catch (_: Exception) { }
    }

    fun getCachedItems(context: Context, subId: String = "default"): List<MediaItemModel> {
        return try {
            val file = java.io.File(context.filesDir, "cache_$subId.json")
            if (!file.exists()) return emptyList()
            val arr = JSONArray(file.readText())
            val list = ArrayList<MediaItemModel>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    MediaItemModel(
                        id = o.optString("id"),
                        rawId = o.optString("rawId"),
                        title = o.optString("title"),
                        url = o.optString("url"),
                        fallbackUrl = o.optString("fallbackUrl"),
                        group = o.optString("group"),
                        logo = o.optString("logo"),
                        contentType = o.optString("contentType", "LIVE"),
                        rating = o.optString("rating"),
                        year = o.optString("year"),
                        addedTimestamp = o.optLong("addedTimestamp", 0L),
                        is4k = o.optBoolean("is4k", false),
                        originTag = o.optString("originTag", "أجنبي"),
                        genreTag = o.optString("genreTag", "عام")
                    )
                )
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

    fun getCachedSportsEpg(context: Context): List<EpgProgram> = emptyList()

    // تتبع التقدم في الأفلام والحلقات + حفظ آخر حلقة تم تشغيلها للمسلسل
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

    // استرجاع آخر حلقة توقف عندها المستخدم في المسلسل (episodeId, seasonNum, episodeNum)
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

// ============================================================================
// مستودع البيانات وفحص السيرفرات وتصنيف الرياضة
// ============================================================================
object DevkanPlaylistRepository {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // جلب بلد الاستضافة واسم الشركة والمنطقة الزمنية للسيرفر
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

    // اختبار الاتصال الفوري بالسيرفر قبل تفعيله وعرض الإحصائيات وحالة الحظر
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

                    fun countEndpoint(action: String): Int {
                        return try {
                            val r = Request.Builder().url("$authUrl&action=$action").build()
                            httpClient.newCall(r).execute().use { res ->
                                JSONArray(res.body?.string() ?: "[]").length()
                            }
                        } catch (_: Exception) { 0 }
                    }

                    val liveCnt = countEndpoint("get_live_streams")
                    val movCnt = countEndpoint("get_vod_streams")
                    val serCnt = countEndpoint("get_series")

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

    suspend fun loadSubscriptionContent(sub: SavedSubscription, geoBypass: Boolean): Pair<SavedSubscription, List<MediaItemModel>> = withContext(Dispatchers.IO) {
        val hostInfo = fetchServerHostInfo(sub.host)
        if (sub.type == "M3U") {
            val items = parseM3uFromUrl(sub.m3uUrl)
            return@withContext Pair(
                sub.copy(
                    accessStatus = if (items.isNotEmpty()) "DIRECT" else "VPN_REQUIRED",
                    serverCountryCode = hostInfo.first,
                    serverIsp = hostInfo.second,
                    serverTimezone = hostInfo.third
                ),
                items
            )
        }

        val baseUrl = sub.host.trimEnd('/')
        val authUrl = "$baseUrl/player_api.php?username=${sub.username}&password=${sub.password}"
        var updatedSub = sub.copy(
            serverCountryCode = hostInfo.first,
            serverIsp = hostInfo.second,
            serverTimezone = hostInfo.third
        )
        val resultList = mutableListOf<MediaItemModel>()

        try {
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

            fun fetchCategories(action: String): Map<String, String> {
                val map = HashMap<String, String>()
                try {
                    val r = Request.Builder().url("$authUrl&action=$action").build()
                    httpClient.newCall(r).execute().use { res ->
                        val arr = JSONArray(res.body?.string() ?: "[]")
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            map[o.optString("category_id")] = o.optString("category_name", "General")
                        }
                    }
                } catch (_: Exception) { }
                return map
            }

            val liveCats = fetchCategories("get_live_categories")
            val vodCats = fetchCategories("get_vod_categories")
            val seriesCats = fetchCategories("get_series_categories")

            // 1. البث المباشر
            try {
                val r = Request.Builder().url("$authUrl&action=get_live_streams").build()
                httpClient.newCall(r).execute().use { res ->
                    val arr = JSONArray(res.body?.string() ?: "[]")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val streamId = o.optString("stream_id")
                        val name = o.optString("name")
                        val catName = liveCats[o.optString("category_id")] ?: "Live TV"
                        resultList.add(
                            MediaItemModel(
                                id = "live_$streamId",
                                rawId = streamId,
                                title = name,
                                url = "$baseUrl/live/${sub.username}/${sub.password}/$streamId.m3u8",
                                fallbackUrl = "$baseUrl/live/${sub.username}/${sub.password}/$streamId.ts",
                                group = catName,
                                logo = o.optString("stream_icon"),
                                contentType = "LIVE"
                            )
                        )
                    }
                }
            } catch (_: Exception) { }

            // 2. الأفلام
            try {
                val r = Request.Builder().url("$authUrl&action=get_vod_streams").build()
                httpClient.newCall(r).execute().use { res ->
                    val arr = JSONArray(res.body?.string() ?: "[]")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val streamId = o.optString("stream_id")
                        val name = o.optString("name")
                        val ext = o.optString("container_extension", "mp4")
                        val catName = vodCats[o.optString("category_id")] ?: "Movies"
                        val is4k = name.contains("4K", ignoreCase = true) || catName.contains("4K", ignoreCase = true)
                        resultList.add(
                            MediaItemModel(
                                id = "vod_$streamId",
                                rawId = streamId,
                                title = name,
                                url = "$baseUrl/movie/${sub.username}/${sub.password}/$streamId.$ext",
                                group = catName,
                                logo = o.optString("stream_icon"),
                                contentType = "MOVIE",
                                rating = o.optString("rating", ""),
                                addedTimestamp = o.optString("added", "0").toLongOrNull() ?: 0L,
                                is4k = is4k,
                                originTag = detectOriginTag(catName, name),
                                genreTag = detectGenreTag(catName, name)
                            )
                        )
                    }
                }
            } catch (_: Exception) { }

            // 3. المسلسلات
            try {
                val r = Request.Builder().url("$authUrl&action=get_series").build()
                httpClient.newCall(r).execute().use { res ->
                    val arr = JSONArray(res.body?.string() ?: "[]")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val seriesId = o.optString("series_id")
                        val name = o.optString("name")
                        val catName = seriesCats[o.optString("category_id")] ?: "Series"
                        resultList.add(
                            MediaItemModel(
                                id = "ser_$seriesId",
                                rawId = seriesId,
                                title = name,
                                url = "",
                                group = catName,
                                logo = o.optString("cover"),
                                contentType = "SERIES",
                                rating = o.optString("rating", ""),
                                addedTimestamp = o.optString("last_modified", "0").toLongOrNull() ?: 0L,
                                originTag = detectOriginTag(catName, name),
                                genreTag = detectGenreTag(catName, name)
                            )
                        )
                    }
                }
            } catch (_: Exception) { }
        } catch (_: Exception) {
            updatedSub = updatedSub.copy(accessStatus = "VPN_REQUIRED")
        }

        Pair(updatedSub, resultList)
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
        var index = 0

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.startsWith("#EXTINF:")) {
                currentTitle = line.substringAfterLast(",").trim()
                currentGroup = Regex("""group-title="([^"]*)"""").find(line)?.groupValues?.get(1)?.ifEmpty { "Live TV" } ?: "Live TV"
                currentLogo = Regex("""tvg-logo="([^"]*)"""").find(line)?.groupValues?.get(1) ?: ""
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
                        contentType = if (isMovie) "MOVIE" else "LIVE"
                    )
                )
            }
        }
        return list
    }

    fun deduplicateSportChannels(channels: List<MediaItemModel>): List<MediaItemModel> =
        channels.distinctBy { "${it.group}_${it.title.trim().lowercase()}" }

    suspend fun syncAndClassifySportsEpg(
        context: Context,
        sub: SavedSubscription,
        items: List<MediaItemModel>
    ): ClassifiedSportsData = ClassifiedSportsData(emptyList(), emptyList(), emptyList(), emptyList())

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

// ============================================================================
// مصدر المباريات الحية الشامل (كرة القدم + كرة السلة + التنس مصنفة بالدولة والدوري)
// ============================================================================
object RealSportsDataSource {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val endpoints = listOf(
        Triple("SOCCER", "العالم / قاري", "uefa.champions" to "دوري أبطال أوروبا"),
        Triple("SOCCER", "العالم / قاري", "fifa.worldq.uefa" to "تصفيات كأس العالم - أوروبا"),
        Triple("SOCCER", "العالم / قاري", "uefa.nations" to "دوري الأمم الأوروبية"),
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
        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        for ((sportType, country, leaguePair) in endpoints) {
            val (slug, leagueArabic) = leaguePair
            val sportPath = when (sportType) {
                "BASKETBALL" -> "basketball/$slug"
                "TENNIS" -> "tennis/$slug"
                else -> "soccer/$slug"
            }
            try {
                val url = "https://site.api.espn.com/apis/site/v2/sports/$sportPath/scoreboard?dates=$todayStr"
                client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val root = JSONObject(resp.body?.string() ?: "{}")
                    val events = root.optJSONArray("events") ?: return@use
                    for (i in 0 until events.length()) {
                        val ev = events.getJSONObject(i)
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
                            if (parsed != null) SimpleDateFormat("hh:mm a", Locale.US).format(parsed) else clock
                        } catch (_: Exception) { clock }

                        val s1 = c1.optString("score", "0").ifEmpty { "0" }
                        val s2 = c2.optString("score", "0").ifEmpty { "0" }
                        val scoreText = if (state == "pre") "VS" else "$s1 - $s2"

                        val broadcaster = when (slug) {
                            "ksa.1" -> "SSC Sports"
                            "ita.1" -> "AD Sports"
                            else -> "beIN SPORTS"
                        }

                        allMatches.add(
                            RealSportsMatch(
                                id = ev.optString("id", "$slug-$i"),
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
