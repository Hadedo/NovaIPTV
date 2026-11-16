package com.hadedo.novaiptv

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ============================================================================
// نماذج البيانات الموحدة (v5.0 - Smart Catalog + Hybrid EPG + Sports Matcher)
// ============================================================================
data class SavedSubscription(
    val type: String, // "XTREAM", "M3U", "STALKER"
    val host: String,
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val macAddress: String = "",
    val status: String = "Active",
    val expDateFormatted: String = "Unlimited",
    val allowedCountries: String = "Global / All",
    val activeCons: String = "1",
    val maxCons: String = "1"
)

data class MediaItemModel(
    val id: String,
    val rawId: String = "",
    val title: String,
    val url: String,
    val fallbackUrl: String = "",
    val group: String,
    val logo: String = "",
    val contentType: String = "LIVE", // "LIVE", "MOVIE", "SERIES"
    val rating: String = "",
    val year: String = "",
    val originTag: String = "أجنبي",
    val genreTag: String = "دراما",
    val addedTimestamp: Long = 0L,
    val is4k: Boolean = false
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

data class SeriesEpisodeModel(
    val id: String,
    val episodeNum: Int,
    val seasonNum: Int,
    val title: String,
    val url: String,
    val duration: String = ""
)

data class SeriesDetailedInfo(
    val title: String,
    val plot: String,
    val cast: String,
    val releaseDate: String,
    val rating: String,
    val backdropUrl: String,
    val seasons: Map<Int, List<SeriesEpisodeModel>>
)

data class RealSportsMatch(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val league: String,
    val matchTime: String,
    val status: String,
    val suggestedBroadcaster: String = "beIN SPORTS"
)

enum class AutoSyncInterval(val days: Int, val labelAr: String, val labelEn: String) {
    DAILY(1, "كل يوم (24 ساعة)", "Every Day (24h)"),
    WEEKLY(7, "كل أسبوع (7 أيام)", "Every Week (7d)"),
    MONTHLY(30, "كل شهر (30 يوماً)", "Every Month (30d)"),
    MANUAL(0, "يدوي فقط", "Manual Only")
}

enum class EngineType(val labelAr: String, val labelEn: String, val labelDe: String, val labelTr: String) {
    AUTO_HYBRID("تلقائي هجين (Media3 + LibVLC)", "Auto Hybrid (Media3 + VLC)", "Auto-Hybrid (Media3 + LibVLC)", "Otomatik Hibrit (Media3 + LibVLC)"),
    MEDIA3_ONLY("Media3 ExoPlayer (فائق السرعة)", "Media3 ExoPlayer (Fast Zapping)", "Media3 ExoPlayer (Schnell)", "Media3 ExoPlayer (Hızlı)"),
    LIBVLC_ONLY("LibVLC (للترميزات الصعبة AC3/TS)", "LibVLC (Advanced Codecs)", "LibVLC (Erweiterte Codecs)", "LibVLC (Gelişmiş Kodekler)")
}

data class StreamDiagnostics(
    val activeEngine: String = "Media3 ExoPlayer",
    val pingMs: Long = 0L,
    val startupTimeMs: Long = 0L,
    val bitrateKbps: Int = 0,
    val bufferDurationMs: Long = 0L,
    val reconnectCount: Int = 0,
    val statusNote: String = "مستقر"
)

// ============================================================================
// 1. طبقة الأمان والتخزين المشفر + جدولة المزامنة التلقائية
// ============================================================================
object DevkanVault {
    private fun getSecurePrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "devkan_secure_vault_v5",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("devkan_fallback_prefs_v5", Context.MODE_PRIVATE)
        }
    }

    fun isDisclaimerAccepted(context: Context): Boolean =
        getSecurePrefs(context).getBoolean("disclaimer_accepted", false)

    fun setDisclaimerAccepted(context: Context, accepted: Boolean) {
        getSecurePrefs(context).edit().putBoolean("disclaimer_accepted", accepted).apply()
    }

    fun getLanguage(context: Context): String =
        getSecurePrefs(context).getString("app_lang", "ar") ?: "ar"

    fun setLanguage(context: Context, lang: String) {
        getSecurePrefs(context).edit().putString("app_lang", lang).apply()
    }

    fun getAppLockPin(context: Context): String =
        getSecurePrefs(context).getString("app_lock_pin", "") ?: ""

    fun setAppLockPin(context: Context, pin: String) {
        getSecurePrefs(context).edit().putString("app_lock_pin", pin).apply()
    }

    fun isLowLatencyEnabled(context: Context): Boolean =
        getSecurePrefs(context).getBoolean("low_latency_mode", true)

    fun setLowLatencyEnabled(context: Context, enabled: Boolean) {
        getSecurePrefs(context).edit().putBoolean("low_latency_mode", enabled).apply()
    }

    fun isGeoBypassEnabled(context: Context): Boolean =
        getSecurePrefs(context).getBoolean("geo_bypass_headers", true)

    fun setGeoBypassEnabled(context: Context, enabled: Boolean) {
        getSecurePrefs(context).edit().putBoolean("geo_bypass_headers", enabled).apply()
    }

    fun isAutoResumeEnabled(context: Context): Boolean =
        getSecurePrefs(context).getBoolean("auto_resume_last", false)

    fun setAutoResumeEnabled(context: Context, enabled: Boolean) {
        getSecurePrefs(context).edit().putBoolean("auto_resume_last", enabled).apply()
    }

    fun getAutoSyncInterval(context: Context): AutoSyncInterval {
        val raw = getSecurePrefs(context).getString("auto_sync_interval", AutoSyncInterval.DAILY.name)
        return try {
            AutoSyncInterval.valueOf(raw ?: AutoSyncInterval.DAILY.name)
        } catch (_: Exception) {
            AutoSyncInterval.DAILY
        }
    }

    fun setAutoSyncInterval(context: Context, interval: AutoSyncInterval) {
        getSecurePrefs(context).edit().putString("auto_sync_interval", interval.name).apply()
    }

    fun shouldRunBackgroundSync(context: Context): Boolean {
        val interval = getAutoSyncInterval(context)
        if (interval == AutoSyncInterval.MANUAL || interval.days <= 0) return false
        val lastSync = getSecurePrefs(context).getLong("last_catalog_sync_ms", 0L)
        val elapsedMs = System.currentTimeMillis() - lastSync
        val thresholdMs = interval.days * 24L * 60L * 60L * 1000L
        return elapsedMs >= thresholdMs
    }

    fun markCatalogSyncedNow(context: Context) {
        getSecurePrefs(context).edit().putLong("last_catalog_sync_ms", System.currentTimeMillis()).apply()
    }

    fun saveSubscription(context: Context, sub: SavedSubscription) {
        getSecurePrefs(context).edit()
            .putString("sub_type", sub.type)
            .putString("sub_host", sub.host)
            .putString("sub_user", sub.username)
            .putString("sub_pass", sub.password)
            .putString("sub_m3u", sub.m3uUrl)
            .putString("sub_mac", sub.macAddress)
            .putString("sub_status", sub.status)
            .putString("sub_exp", sub.expDateFormatted)
            .putString("sub_countries", sub.allowedCountries)
            .putString("sub_active_cons", sub.activeCons)
            .putString("sub_max_cons", sub.maxCons)
            .apply()
    }

    fun getSavedSubscription(context: Context): SavedSubscription? {
        val prefs = getSecurePrefs(context)
        val type = prefs.getString("sub_type", null) ?: return null
        return SavedSubscription(
            type = type,
            host = prefs.getString("sub_host", "") ?: "",
            username = prefs.getString("sub_user", "") ?: "",
            password = prefs.getString("sub_pass", "") ?: "",
            m3uUrl = prefs.getString("sub_m3u", "") ?: "",
            macAddress = prefs.getString("sub_mac", "") ?: "",
            status = prefs.getString("sub_status", "Active") ?: "Active",
            expDateFormatted = prefs.getString("sub_exp", "Unlimited") ?: "Unlimited",
            allowedCountries = prefs.getString("sub_countries", "Global") ?: "Global",
            activeCons = prefs.getString("sub_active_cons", "1") ?: "1",
            maxCons = prefs.getString("sub_max_cons", "1") ?: "1"
        )
    }

    fun clearSubscription(context: Context) {
        getSecurePrefs(context).edit()
            .remove("sub_type")
            .remove("sub_host")
            .remove("sub_user")
            .remove("sub_pass")
            .remove("sub_m3u")
            .remove("sub_mac")
            .apply()
        try {
            File(context.filesDir, "devkan_cached_playlist_v5.json").delete()
        } catch (_: Exception) { }
    }

    fun saveLastChannel(context: Context, url: String, title: String) {
        getSecurePrefs(context).edit()
            .putString("last_channel_url", url)
            .putString("last_channel_title", title)
            .apply()
    }

    fun getLastChannel(context: Context): Pair<String, String>? {
        val prefs = getSecurePrefs(context)
        val url = prefs.getString("last_channel_url", null) ?: return null
        val title = prefs.getString("last_channel_title", "Last Channel") ?: "Last Channel"
        return url to title
    }

    fun saveVodProgress(context: Context, streamId: String, positionMs: Long, durationMs: Long) {
        val progressFraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
        getSecurePrefs(context).edit()
            .putLong("vod_pos_$streamId", positionMs)
            .putFloat("vod_frac_$streamId", progressFraction)
            .apply()
    }

    fun getVodPosition(context: Context, streamId: String): Long =
        getSecurePrefs(context).getLong("vod_pos_$streamId", 0L)

    fun getVodProgressFraction(context: Context, streamId: String): Float =
        getSecurePrefs(context).getFloat("vod_frac_$streamId", 0f)

    fun savePreferredEngine(context: Context, engine: EngineType) {
        getSecurePrefs(context).edit().putString("preferred_engine", engine.name).apply()
    }

    fun getPreferredEngine(context: Context): EngineType {
        val raw = getSecurePrefs(context).getString("preferred_engine", EngineType.AUTO_HYBRID.name)
        return try {
            EngineType.valueOf(raw ?: EngineType.AUTO_HYBRID.name)
        } catch (e: Exception) {
            EngineType.AUTO_HYBRID
        }
    }

    fun saveCachedItems(context: Context, items: List<MediaItemModel>) {
        try {
            val arr = JSONArray()
            items.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("rawId", item.rawId)
                obj.put("title", item.title)
                obj.put("url", item.url)
                obj.put("fallbackUrl", item.fallbackUrl)
                obj.put("group", item.group)
                obj.put("logo", item.logo)
                obj.put("contentType", item.contentType)
                obj.put("rating", item.rating)
                obj.put("year", item.year)
                obj.put("originTag", item.originTag)
                obj.put("genreTag", item.genreTag)
                obj.put("addedTimestamp", item.addedTimestamp)
                obj.put("is4k", item.is4k)
                arr.put(obj)
            }
            File(context.filesDir, "devkan_cached_playlist_v5.json").writeText(arr.toString())
            markCatalogSyncedNow(context)
        } catch (_: Exception) { }
    }

    fun getCachedItems(context: Context): List<MediaItemModel> {
        return try {
            val file = File(context.filesDir, "devkan_cached_playlist_v5.json")
            if (!file.exists()) return emptyList()
            val arr = JSONArray(file.readText())
            val list = ArrayList<MediaItemModel>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    MediaItemModel(
                        id = obj.optString("id"),
                        rawId = obj.optString("rawId"),
                        title = obj.optString("title"),
                        url = obj.optString("url"),
                        fallbackUrl = obj.optString("fallbackUrl"),
                        group = obj.optString("group"),
                        logo = obj.optString("logo"),
                        contentType = obj.optString("contentType", "LIVE"),
                        rating = obj.optString("rating"),
                        year = obj.optString("year"),
                        originTag = obj.optString("originTag", "أجنبي"),
                        genreTag = obj.optString("genreTag", "دراما"),
                        addedTimestamp = obj.optLong("addedTimestamp", 0L),
                        is4k = obj.optBoolean("is4k", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}

// ============================================================================
// 2. المنسق الهجين لمحركات التشغيل
// ============================================================================
object EngineCoordinator {
    val currentDiagnostics = mutableStateOf(StreamDiagnostics())

    fun selectInitialEngine(url: String, preferred: EngineType): EngineType {
        if (preferred != EngineType.AUTO_HYBRID) return preferred
        val lower = url.lowercase()
        return if (lower.endsWith(".ts") || lower.endsWith(".avi") || lower.contains("mpegts") || lower.contains("rtsp://")) {
            EngineType.LIBVLC_ONLY
        } else {
            EngineType.MEDIA3_ONLY
        }
    }

    fun getAdaptiveBufferMs(lowLatency: Boolean): Triple<Int, Int, Int> {
        return if (lowLatency) Triple(1800, 6000, 450) else Triple(6000, 20000, 1200)
    }

    fun buildSmartHeaders(geoBypass: Boolean): Map<String, String> {
        val headers = mutableMapOf(
            "User-Agent" to "IPTVSmartersPro/3.1.5 (Linux; Android 14) Lavf/58.76.100",
            "Accept" to "*/*",
            "Connection" to "keep-alive"
        )
        if (geoBypass) {
            headers["X-Forwarded-For"] = "104.28.14.12"
            headers["CF-IPCountry"] = "US"
        }
        return headers
    }

    suspend fun runServerSpeedAndPingTest(serverUrl: String): Pair<Long, Double> = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
        val cleanUrl = if (serverUrl.startsWith("http")) serverUrl else "https://www.google.com"
        return@withContext try {
            val startPing = System.currentTimeMillis()
            val req = Request.Builder().url(cleanUrl).head().build()
            client.newCall(req).execute().use { }
            val ping = (System.currentTimeMillis() - startPing).coerceAtLeast(12L)

            val startDl = System.currentTimeMillis()
            val dlReq = Request.Builder().url(cleanUrl).get().build()
            var bytesRead = 0L
            client.newCall(dlReq).execute().use { resp ->
                val stream = resp.body?.byteStream()
                val buf = ByteArray(8192)
                var read = 0
                while (stream != null && stream.read(buf).also { read = it } != -1 && bytesRead < 250_000) {
                    bytesRead += read
                }
            }
            val elapsedSec = ((System.currentTimeMillis() - startDl).coerceAtLeast(50L)) / 1000.0
            val mbps = ((bytesRead * 8.0) / 1_000_000.0) / elapsedSec
            ping to String.format("%.1f", mbps.coerceIn(1.5, 150.0)).toDouble()
        } catch (e: Exception) {
            999L to 0.0
        }
    }
}

// ============================================================================
// 3. مستورد الاشتراكات الذكي + الدليل الهجين (Hybrid EPG) + دمج القنوات المكررة
// ============================================================================
object DevkanPlaylistRepository {
    private fun buildClient(geoBypass: Boolean): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                    .header("User-Agent", "IPTVSmartersPro/3.1.5 (Linux; Android 14)")
                if (geoBypass) {
                    builder.header("X-Forwarded-For", "104.28.14.12")
                    builder.header("CF-IPCountry", "US")
                }
                chain.proceed(builder.build())
            }
            .build()
    }

    private fun detectOriginTag(group: String, title: String): String {
        val text = "$group $title".lowercase()
        return when {
            text.contains("korean") || text.contains("كوري") || text.contains("korea") || text.contains("kdrama") -> "كوري"
            text.contains("indian") || text.contains("bollywood") || text.contains("هندي") || text.contains("hindi") -> "هندي"
            text.contains("arab") || text.contains("عربي") || text.contains("egypt") || text.contains("مصري") || text.contains("خليجي") || text.contains("سوري") || text.contains("عراقي") || Regex("[\\u0600-\\u06FF]").containsMatchIn(title) -> "عربي"
            else -> "أجنبي"
        }
    }

    private fun detectGenreTag(group: String, title: String): String {
        val text = "$group $title".lowercase()
        return when {
            text.contains("history") || text.contains("historical") || text.contains("تاريخ") -> "تاريخي"
            text.contains("action") || text.contains("أكشن") || text.contains("اكشن") || text.contains("war") -> "أكشن"
            text.contains("comedy") || text.contains("كوميد") -> "كوميدي"
            else -> "دراما"
        }
    }

    private fun checkIs4K(group: String, title: String): Boolean {
        val text = "$group $title".uppercase()
        return text.contains("4K") || text.contains("UHD") || text.contains("2160P")
    }

    fun deduplicateSportChannels(channels: List<MediaItemModel>): List<MediaItemModel> {
        val seenNormalized = mutableSetOf<String>()
        val result = mutableListOf<MediaItemModel>()
        val sorted = channels.sortedByDescending { ch ->
            val t = ch.title.uppercase()
            when {
                t.contains("4K") || t.contains("UHD") -> 4
                t.contains("FHD") || t.contains("1080") -> 3
                t.contains("HD") -> 2
                else -> 1
            }
        }
        for (ch in sorted) {
            val cleanKey = ch.title.uppercase()
                .replace(Regex("\\b(4K|UHD|FHD|HD|SD|LOW|HEVC|H265|VIP|BACKUP|BK|1080P|720P)\\b"), "")
                .replace(Regex("[^A-Z0-9\\u0600-\\u06FF]"), "")
                .trim()
            if (cleanKey.isNotEmpty() && !seenNormalized.contains(cleanKey)) {
                seenNormalized.add(cleanKey)
                result.add(ch)
            }
        }
        return result
    }

    suspend fun fetchChannelShortEpg(sub: SavedSubscription, streamRawId: String): Pair<String, String> = withContext(Dispatchers.IO) {
        if (sub.type != "XTREAM" || streamRawId.isEmpty()) return@withContext ("البث المباشر المستمر" to "البرنامج التالي")
        val client = buildClient(true)
        try {
            val cleanHost = sub.host.trimEnd('/')
            val url = "$cleanHost/player_api.php?username=${URLEncoder.encode(sub.username, "UTF-8")}&password=${URLEncoder.encode(sub.password, "UTF-8")}&action=get_short_epg&stream_id=$streamRawId&limit=2"
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                val listings = JSONObject(resp.body?.string() ?: "{}").optJSONArray("epg_listings") ?: JSONArray()
                if (listings.length() > 0) {
                    val nowRaw = listings.getJSONObject(0).optString("title", "")
                    val nowTitle = try { String(Base64.decode(nowRaw, Base64.DEFAULT)) } catch (_: Exception) { nowRaw }
                    val nextRaw = if (listings.length() > 1) listings.getJSONObject(1).optString("title", "") else ""
                    val nextTitle = try { String(Base64.decode(nextRaw, Base64.DEFAULT)) } catch (_: Exception) { nextRaw }
                    if (nowTitle.isNotBlank()) {
                        return@withContext (nowTitle.trim() to nextTitle.trim().ifEmpty { "البث المستمر" })
                    }
                }
            }
        } catch (_: Exception) { }
        "البث المباشر المستمر" to "البرنامج التالي على الجدول"
    }

    suspend fun loadSubscriptionContent(
        sub: SavedSubscription,
        geoBypass: Boolean = true
    ): Pair<SavedSubscription, List<MediaItemModel>> = withContext(Dispatchers.IO) {
        val client = buildClient(geoBypass)
        val items = mutableListOf<MediaItemModel>()
        var updatedSub = sub

        try {
            when (sub.type) {
                "XTREAM" -> {
                    val cleanHost = sub.host.trimEnd('/')
                    val u = URLEncoder.encode(sub.username, "UTF-8")
                    val p = URLEncoder.encode(sub.password, "UTF-8")
                    val baseApi = "$cleanHost/player_api.php?username=$u&password=$p"

                    try {
                        client.newCall(Request.Builder().url(baseApi).build()).execute().use { resp ->
                            val root = JSONObject(resp.body?.string() ?: "{}")
                            val userInfo = root.optJSONObject("user_info")
                            if (userInfo != null) {
                                val status = userInfo.optString("status", "Active")
                                val expRaw = userInfo.optString("exp_date", "")
                                val expFormatted = if (expRaw.isNotEmpty() && expRaw != "null") {
                                    val millis = expRaw.toLongOrNull()?.times(1000L) ?: 0L
                                    if (millis > 0L) SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis)) else "Unlimited"
                                } else "Unlimited"
                                val allowedCountriesRaw = userInfo.optString("allowed_countries", "")
                                val countriesDisplay = if (allowedCountriesRaw.isNotEmpty() && allowedCountriesRaw != "[]" && allowedCountriesRaw != "null") allowedCountriesRaw else "Global / All Countries"
                                updatedSub = updatedSub.copy(
                                    status = status,
                                    expDateFormatted = expFormatted,
                                    allowedCountries = countriesDisplay,
                                    activeCons = userInfo.optString("active_cons", "1"),
                                    maxCons = userInfo.optString("max_connections", "1")
                                )
                            }
                        }
                    } catch (_: Exception) { }

                    val liveCategories = mutableMapOf<String, String>()
                    try {
                        client.newCall(Request.Builder().url("$baseApi&action=get_live_categories").build()).execute().use { resp ->
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                liveCategories[obj.optString("category_id")] = obj.optString("category_name", "Live TV")
                            }
                        }
                        client.newCall(Request.Builder().url("$baseApi&action=get_live_streams").build()).execute().use { resp ->
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val id = obj.optString("stream_id")
                                val name = obj.optString("name", "Channel $i")
                                val icon = obj.optString("stream_icon", "")
                                val catName = liveCategories[obj.optString("category_id")] ?: "Live Channels"
                                val primaryUrl = "$cleanHost/live/${sub.username}/${sub.password}/$id.m3u8"
                                val fallbackTs = "$cleanHost/live/${sub.username}/${sub.password}/$id.ts"
                                items.add(MediaItemModel("live_$id", id, name, primaryUrl, fallbackTs, catName, icon, "LIVE"))
                            }
                        }
                    } catch (_: Exception) { }

                    val vodCategories = mutableMapOf<String, String>()
                    try {
                        client.newCall(Request.Builder().url("$baseApi&action=get_vod_categories").build()).execute().use { resp ->
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                vodCategories[obj.optString("category_id")] = obj.optString("category_name", "Movies")
                            }
                        }
                        client.newCall(Request.Builder().url("$baseApi&action=get_vod_streams").build()).execute().use { resp ->
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until minOf(arr.length(), 1800)) {
                                val obj = arr.getJSONObject(i)
                                val id = obj.optString("stream_id")
                                val name = obj.optString("name", "Movie $i")
                                val icon = obj.optString("stream_icon", "")
                                val rating = obj.optString("rating", "")
                                val addedTs = obj.optString("added", "0").toLongOrNull() ?: 0L
                                val ext = obj.optString("container_extension", "mp4").ifEmpty { "mp4" }
                                val catName = vodCategories[obj.optString("category_id")] ?: "Movies"
                                val movieUrl = "$cleanHost/movie/${sub.username}/${sub.password}/$id.$ext"
                                val yearMatch = Regex("(19|20)\\d{2}").find(name)?.value ?: ""
                                items.add(
                                    MediaItemModel(
                                        id = "vod_$id",
                                        rawId = id,
                                        title = name,
                                        url = movieUrl,
                                        group = catName,
                                        logo = icon,
                                        contentType = "MOVIE",
                                        rating = rating,
                                        year = yearMatch,
                                        originTag = detectOriginTag(catName, name),
                                        genreTag = detectGenreTag(catName, name),
                                        addedTimestamp = addedTs,
                                        is4k = checkIs4K(catName, name)
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) { }

                    val seriesCategories = mutableMapOf<String, String>()
                    try {
                        client.newCall(Request.Builder().url("$baseApi&action=get_series_categories").build()).execute().use { resp ->
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                seriesCategories[obj.optString("category_id")] = obj.optString("category_name", "Series")
                            }
                        }
                        client.newCall(Request.Builder().url("$baseApi&action=get_series").build()).execute().use { resp ->
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until minOf(arr.length(), 1800)) {
                                val obj = arr.getJSONObject(i)
                                val id = obj.optString("series_id")
                                val name = obj.optString("name", "Series $i")
                                val cover = obj.optString("cover", "")
                                val rating = obj.optString("rating", "")
                                val lastMod = obj.optString("last_modified", "0").toLongOrNull() ?: 0L
                                val release = obj.optString("releaseDate", "").take(4).ifEmpty {
                                    Regex("(19|20)\\d{2}").find(name)?.value ?: ""
                                }
                                val catName = seriesCategories[obj.optString("category_id")] ?: "Series"
                                items.add(
                                    MediaItemModel(
                                        id = "ser_$id",
                                        rawId = id,
                                        title = name,
                                        url = "",
                                        group = catName,
                                        logo = cover,
                                        contentType = "SERIES",
                                        rating = rating,
                                        year = release,
                                        originTag = detectOriginTag(catName, name),
                                        genreTag = detectGenreTag(catName, name),
                                        addedTimestamp = lastMod,
                                        is4k = checkIs4K(catName, name)
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) { }
                }

                "M3U" -> {
                    client.newCall(Request.Builder().url(sub.m3uUrl).build()).execute().use { resp ->
                        items.addAll(parseM3uLines((resp.body?.string() ?: "").lines()))
                    }
                    updatedSub = updatedSub.copy(status = "Active (M3U)", expDateFormatted = "M3U Playlist")
                }

                "STALKER" -> {
                    val cleanPortal = sub.host.trimEnd('/')
                    val macEncoded = URLEncoder.encode(sub.macAddress.trim(), "UTF-8")
                    val req = Request.Builder()
                        .url("$cleanPortal/portal.php?type=stb&action=handshake&token=&JsHttpRequest=1-xml")
                        .header("Cookie", "mac=$macEncoded; stb_lang=en; timezone=Europe/Berlin")
                        .build()
                    var token = ""
                    client.newCall(req).execute().use { resp ->
                        token = JSONObject(resp.body?.string() ?: "{}").optJSONObject("js")?.optString("token", "") ?: ""
                    }
                    if (token.isNotEmpty()) {
                        val channelsReq = Request.Builder()
                            .url("$cleanPortal/portal.php?type=itv&action=get_all_channels&JsHttpRequest=1-xml")
                            .header("Cookie", "mac=$macEncoded; stb_lang=en")
                            .header("Authorization", "Bearer $token")
                            .build()
                        client.newCall(channelsReq).execute().use { resp ->
                            val dataArr = JSONObject(resp.body?.string() ?: "{}").optJSONObject("js")?.optJSONArray("data") ?: JSONArray()
                            for (i in 0 until dataArr.length()) {
                                val ch = dataArr.getJSONObject(i)
                                val name = ch.optString("name", "Stalker CH $i")
                                val cmd = ch.optString("cmd", "").removePrefix("ffmpeg ").trim()
                                val logo = ch.optString("logo", "")
                                if (cmd.isNotEmpty()) {
                                    items.add(MediaItemModel("stk_$i", "$i", name, cmd, "", "Stalker TV", logo, "LIVE"))
                                }
                            }
                        }
                        updatedSub = updatedSub.copy(status = "Active (Stalker MAC)", expDateFormatted = "Portal Connected")
                    }
                }
            }
        } catch (_: Exception) { }

        updatedSub to items
    }

    suspend fun fetchVodDetails(sub: SavedSubscription, vodRawId: String, fallbackTitle: String, fallbackPoster: String): VodDetailedInfo = withContext(Dispatchers.IO) {
        val client = buildClient(true)
        try {
            val cleanHost = sub.host.trimEnd('/')
            val url = "$cleanHost/player_api.php?username=${URLEncoder.encode(sub.username, "UTF-8")}&password=${URLEncoder.encode(sub.password, "UTF-8")}&action=get_vod_info&vod_id=$vodRawId"
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                val info = JSONObject(resp.body?.string() ?: "{}").optJSONObject("info")
                if (info != null) {
                    val backdrops = info.optJSONArray("backdrop_path")
                    val bg = if (backdrops != null && backdrops.length() > 0) backdrops.optString(0) else fallbackPoster
                    return@withContext VodDetailedInfo(
                        title = info.optString("name", fallbackTitle).ifEmpty { fallbackTitle },
                        plot = info.optString("plot", "لا يتوفر وصف تفصيلي لهذا الفيلم حالياً.").ifEmpty { "لا يتوفر وصف تفصيلي لهذا الفيلم حالياً." },
                        cast = info.optString("cast", "غير متوفر"),
                        director = info.optString("director", "غير متوفر"),
                        releaseDate = info.optString("releasedate", "2025"),
                        duration = info.optString("duration", "01:45:00"),
                        rating = info.optString("rating", "7.8"),
                        backdropUrl = bg.ifEmpty { fallbackPoster }
                    )
                }
            }
        } catch (_: Exception) { }
        VodDetailedInfo(fallbackTitle, "جاهز للمشاهدة بجودة عالية.", "غير متوفر", "غير متوفر", "2025", "HD", "8.0", fallbackPoster)
    }

    suspend fun fetchSeriesDetails(sub: SavedSubscription, seriesRawId: String, fallbackTitle: String, fallbackPoster: String): SeriesDetailedInfo = withContext(Dispatchers.IO) {
        val client = buildClient(true)
        val seasonsMap = sortedMapOf<Int, MutableList<SeriesEpisodeModel>>()
        try {
            val cleanHost = sub.host.trimEnd('/')
            val url = "$cleanHost/player_api.php?username=${URLEncoder.encode(sub.username, "UTF-8")}&password=${URLEncoder.encode(sub.password, "UTF-8")}&action=get_series_info&series_id=$seriesRawId"
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                val root = JSONObject(resp.body?.string() ?: "{}")
                val info = root.optJSONObject("info")
                val episodesObj = root.optJSONObject("episodes")
                if (episodesObj != null) {
                    val keys = episodesObj.keys()
                    while (keys.hasNext()) {
                        val seasonKey = keys.next()
                        val sNum = seasonKey.toIntOrNull() ?: 1
                        val epArr = episodesObj.optJSONArray(seasonKey) ?: continue
                        val list = seasonsMap.getOrPut(sNum) { mutableListOf() }
                        for (i in 0 until epArr.length()) {
                            val ep = epArr.getJSONObject(i)
                            val epId = ep.optString("id")
                            val epNum = ep.optInt("episode_num", i + 1)
                            val epTitle = ep.optString("title", "الحلقة $epNum")
                            val ext = ep.optString("container_extension", "mp4").ifEmpty { "mp4" }
                            val dur = ep.optJSONObject("info")?.optString("duration", "") ?: ""
                            val epUrl = "$cleanHost/series/${sub.username}/${sub.password}/$epId.$ext"
                            list.add(SeriesEpisodeModel("ep_$epId", epNum, sNum, epTitle, epUrl, dur))
                        }
                    }
                }
                if (info != null) {
                    return@withContext SeriesDetailedInfo(
                        title = info.optString("name", fallbackTitle).ifEmpty { fallbackTitle },
                        plot = info.optString("plot", "مسلسل متكامل المواسم والحلقات.").ifEmpty { "مسلسل متكامل المواسم والحلقات." },
                        cast = info.optString("cast", "غير متوفر"),
                        releaseDate = info.optString("releaseDate", "2025"),
                        rating = info.optString("rating", "8.4"),
                        backdropUrl = info.optString("cover", fallbackPoster).ifEmpty { fallbackPoster },
                        seasons = seasonsMap
                    )
                }
            }
        } catch (_: Exception) { }
        SeriesDetailedInfo(fallbackTitle, "اختر الموسم والحلقة للمشاهدة.", "غير متوفر", "2025", "8.2", fallbackPoster, seasonsMap)
    }

    fun parseM3uLines(lines: List<String>): List<MediaItemModel> {
        val result = mutableListOf<MediaItemModel>()
        var name = "Channel"
        var group = "General"
        var logo = ""
        lines.forEachIndexed { idx, line ->
            val t = line.trim()
            if (t.startsWith("#EXTINF:")) {
                name = t.substringAfterLast(",").trim().ifEmpty { "Channel $idx" }
                group = Regex("""group-title="([^"]*)"""").find(t)?.groupValues?.get(1)?.ifEmpty { "General" } ?: "General"
                logo = Regex("""tvg-logo="([^"]*)"""").find(t)?.groupValues?.get(1) ?: ""
            } else if (t.startsWith("http://") || t.startsWith("https://") || t.startsWith("rtsp://")) {
                val isMovie = t.endsWith(".mp4", true) || t.endsWith(".mkv", true) || t.endsWith(".avi", true)
                result.add(
                    MediaItemModel(
                        id = "m3u_$idx",
                        rawId = "$idx",
                        title = name,
                        url = t,
                        group = group,
                        logo = logo,
                        contentType = if (isMovie) "MOVIE" else "LIVE",
                        originTag = detectOriginTag(group, name),
                        genreTag = detectGenreTag(group, name),
                        is4k = checkIs4K(group, name)
                    )
                )
            }
        }
        return result
    }

    suspend fun parseLocalUri(context: Context, uri: Uri): List<MediaItemModel> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                return@withContext parseM3uLines(BufferedReader(InputStreamReader(input)).readLines())
            }
        } catch (_: Exception) { }
        emptyList()
    }
}

// ============================================================================
// 4. مصدر رياضي حقيقي لجدول المباريات + خوارزمية تحديد القناة الناقلة الفعلية
// ============================================================================
object RealSportsDataSource {
    private fun mapLeagueToBroadcaster(league: String): String {
        val l = league.lowercase()
        return when {
            l.contains("saudi") || l.contains("roshn") -> "SSC Sports"
            l.contains("iraq") || l.contains("afc") || l.contains("asian") -> "الرابعة الرياضية / الكأس"
            l.contains("serie a") || l.contains("italian") -> "AD SPORTS Premium"
            l.contains("premier") || l.contains("champions") || l.contains("liga") || l.contains("bundesliga") || l.contains("ligue 1") -> "beIN SPORTS"
            else -> "beIN SPORTS / القنوات الرياضية"
        }
    }

    suspend fun fetchTodayRealMatches(): List<RealSportsMatch> = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val matches = mutableListOf<RealSportsMatch>()
        try {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val url = "https://www.thesportsdb.com/api/v1/json/3/eventsday.php?d=$todayStr&s=Soccer"
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                val root = JSONObject(resp.body?.string() ?: "{}")
                val events = root.optJSONArray("events") ?: JSONArray()
                for (i in 0 until minOf(events.length(), 25)) {
                    val ev = events.getJSONObject(i)
                    val id = ev.optString("idEvent", "$i")
                    val home = ev.optString("strHomeTeam", "")
                    val away = ev.optString("strAwayTeam", "")
                    val league = ev.optString("strLeague", "Football")
                    val time = ev.optString("strTime", "20:00").take(5)
                    val status = ev.optString("strStatus", "Scheduled").ifEmpty { "اليوم" }
                    if (home.isNotEmpty() && away.isNotEmpty()) {
                        matches.add(
                            RealSportsMatch(
                                id = id,
                                homeTeam = home,
                                awayTeam = away,
                                league = league,
                                matchTime = time,
                                status = status,
                                suggestedBroadcaster = mapLeagueToBroadcaster(league)
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) { }
        matches
    }
}

data class EpgSlot(
    val channelName: String,
    val channelLogo: String,
    val currentProgram: String,
    val nextProgram: String,
    val timeRange: String,
    val progress: Float
)

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
