package com.hadedo.novaiptv

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
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
import java.io.InputStreamReader
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ============================================================================
// نماذج البيانات الموحدة (اشتراك + قنوات + باقات حقيقية + أفلام ومسلسلات)
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
    val title: String,
    val url: String,
    val fallbackUrl: String = "",
    val group: String,
    val logo: String = "",
    val contentType: String = "LIVE" // "LIVE", "MOVIE", "SERIES"
)

enum class EngineType(val labelAr: String, val labelEn: String, val labelDe: String, val labelTr: String) {
    AUTO_HYBRID("تلقائي هجين (Media3 + LibVLC)", "Auto Hybrid (Media3 + LibVLC)", "Auto-Hybrid (Media3 + LibVLC)", "Otomatik Hibrit (Media3 + LibVLC)"),
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
// 1. طبقة الأمان والتخزين المشفر الدائم (Android Keystore)
// ============================================================================
object DevkanVault {
    private fun getSecurePrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "devkan_secure_vault_v3",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("devkan_fallback_prefs_v3", Context.MODE_PRIVATE)
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

    // حفظ واسترجاع الاشتراك تلقائياً عند إغلاق التطبيق وفتحه
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

    fun saveVodPosition(context: Context, streamId: String, positionMs: Long) {
        getSecurePrefs(context).edit().putLong("vod_pos_$streamId", positionMs).apply()
    }

    fun getVodPosition(context: Context, streamId: String): Long =
        getSecurePrefs(context).getLong("vod_pos_$streamId", 0L)

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
}

// ============================================================================
// 2. المنسق الهجين لمحركات التشغيل (Low Latency + Universal Stream Resolver)
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
        return if (lowLatency) {
            // تقليل التأخير عن الستلايت لأدنى حد ممكن (0.45 ثانية)
            Triple(1800, 6000, 450)
        } else {
            Triple(6000, 20000, 1200)
        }
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
// 3. مستورد الاشتراكات المتكامل (Xtream Real Categories + VOD + M3U + Stalker MAC)
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

                    // 1. جلب بيانات الاشتراك الحقيقية (تاريخ الانتهاء + الدولة + الاتصالات)
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

                                val countriesArr = userInfo.optJSONArray("allowed_output_formats")
                                val allowedCountriesRaw = userInfo.optString("allowed_countries", "")
                                val countriesDisplay = when {
                                    allowedCountriesRaw.isNotEmpty() && allowedCountriesRaw != "[]" && allowedCountriesRaw != "null" -> allowedCountriesRaw
                                    countriesArr != null -> "Global (${countriesArr.join(", ").replace("\"", "")})"
                                    else -> "Global / All Countries"
                                }
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

                    // 2. جلب أسماء الباقات الحقيقية (Live Categories) لمنع اختلاط القنوات
                    val liveCategories = mutableMapOf<String, String>()
                    try {
                        client.newCall(Request.Builder().url("$baseApi&action=get_live_categories").build()).execute().use { resp ->
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                liveCategories[obj.optString("category_id")] = obj.optString("category_name", "Live TV")
                            }
                        }
                    } catch (_: Exception) { }

                    // 3. جلب قنوات البث المباشر مع روابط بديلة (.m3u8 + .ts)
                    try {
                        client.newCall(Request.Builder().url("$baseApi&action=get_live_streams").build()).execute().use { resp ->
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val id = obj.optString("stream_id")
                                val name = obj.optString("name", "Channel $i")
                                val icon = obj.optString("stream_icon", "")
                                val catId = obj.optString("category_id")
                                val catName = liveCategories[catId] ?: "Live Channels"
                                val primaryUrl = "$cleanHost/live/${sub.username}/${sub.password}/$id.m3u8"
                                val fallbackTs = "$cleanHost/live/${sub.username}/${sub.password}/$id.ts"
                                items.add(MediaItemModel("live_$id", name, primaryUrl, fallbackTs, catName, icon, "LIVE"))
                            }
                        }
                    } catch (_: Exception) { }

                    // 4. جلب الأفلام الحقيقية من السيرفر (VOD)
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
                            for (i in 0 until minOf(arr.length(), 1500)) {
                                val obj = arr.getJSONObject(i)
                                val id = obj.optString("stream_id")
                                val name = obj.optString("name", "Movie $i")
                                val icon = obj.optString("stream_icon", "")
                                val ext = obj.optString("container_extension", "mp4").ifEmpty { "mp4" }
                                val catName = vodCategories[obj.optString("category_id")] ?: "Movies"
                                val movieUrl = "$cleanHost/movie/${sub.username}/${sub.password}/$id.$ext"
                                items.add(MediaItemModel("vod_$id", name, movieUrl, "", catName, icon, "MOVIE"))
                            }
                        }
                    } catch (_: Exception) { }
                }

                "M3U" -> {
                    client.newCall(Request.Builder().url(sub.m3uUrl).build()).execute().use { resp ->
                        val lines = (resp.body?.string() ?: "").lines()
                        items.addAll(parseM3uLines(lines))
                    }
                    updatedSub = updatedSub.copy(status = "Active (M3U)", expDateFormatted = "M3U Playlist")
                }

                "STALKER" -> {
                    val cleanPortal = sub.host.trimEnd('/')
                    val macEncoded = URLEncoder.encode(sub.macAddress.trim(), "UTF-8")
                    val handshakeUrl = "$cleanPortal/portal.php?type=stb&action=handshake&token=&JsHttpRequest=1-xml"
                    val req = Request.Builder()
                        .url(handshakeUrl)
                        .header("Cookie", "mac=$macEncoded; stb_lang=en; timezone=Europe/Berlin")
                        .build()
                    var token = ""
                    client.newCall(req).execute().use { resp ->
                        val json = JSONObject(resp.body?.string() ?: "{}")
                        token = json.optJSONObject("js")?.optString("token", "") ?: ""
                    }
                    if (token.isNotEmpty()) {
                        val channelsReq = Request.Builder()
                            .url("$cleanPortal/portal.php?type=itv&action=get_all_channels&JsHttpRequest=1-xml")
                            .header("Cookie", "mac=$macEncoded; stb_lang=en")
                            .header("Authorization", "Bearer $token")
                            .build()
                        client.newCall(channelsReq).execute().use { resp ->
                            val json = JSONObject(resp.body?.string() ?: "{}")
                            val dataArr = json.optJSONObject("js")?.optJSONArray("data") ?: JSONArray()
                            for (i in 0 until dataArr.length()) {
                                val ch = dataArr.getJSONObject(i)
                                val name = ch.optString("name", "Stalker CH $i")
                                val cmd = ch.optString("cmd", "").removePrefix("ffmpeg ").trim()
                                val logo = ch.optString("logo", "")
                                if (cmd.isNotEmpty()) {
                                    items.add(MediaItemModel("stk_$i", name, cmd, "", "Stalker TV", logo, "LIVE"))
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
                        title = name,
                        url = t,
                        group = group,
                        logo = logo,
                        contentType = if (isMovie) "MOVIE" else "LIVE"
                    )
                )
            }
        }
        return result
    }

    suspend fun parseLocalUri(context: Context, uri: Uri): List<MediaItemModel> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val lines = BufferedReader(InputStreamReader(input)).readLines()
                return@withContext parseM3uLines(lines)
            }
        } catch (_: Exception) { }
        emptyList()
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
