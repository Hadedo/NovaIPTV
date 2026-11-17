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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.dnsoverhttps.DnsOverHttps
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class SavedSubscription(
val id: String = UUID.randomUUID().toString(),
val label: String = "اشتراك رئيسي",
val type: String,
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
val contentType: String = "LIVE",
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

enum class DnsProvider(val label: String, val dohUrl: String, val ips: List<String>) {
SYSTEM("تلقائي (DNS النظام)", "", emptyList()),
CLOUDFLARE("Cloudflare (1.1.1.1)", "https://cloudflare-dns.com/dns-query", listOf("1.1.1.1", "1.0.0.1")),
GOOGLE("Google (8.8.8.8)", "https://dns.google/dns-query", listOf("8.8.8.8", "8.8.4.4")),
ADGUARD("AdGuard (حظر إعلانات)", "https://dns.adguard-dns.com/dns-query", listOf("94.140.14.14", "94.140.15.15"))
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

object DevkanVault {
private var cachedPrefs: SharedPreferences? = null

private fun getSecurePrefs(context: Context): SharedPreferences {
cachedPrefs?.let { return it }
val prefs = try {
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
cachedPrefs = prefs
return prefs
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

fun getUserAgent(context: Context): String =
getSecurePrefs(context).getString("custom_user_agent", "IPTVSmartersPro/3.1.5 (Linux; Android 14)")
?: "IPTVSmartersPro/3.1.5 (Linux; Android 14)"

fun setUserAgent(context: Context, ua: String) {
getSecurePrefs(context).edit().putString("custom_user_agent", ua).apply()
}

fun getDnsProvider(context: Context): DnsProvider {
val raw = getSecurePrefs(context).getString("custom_dns", DnsProvider.SYSTEM.name)
return try { DnsProvider.valueOf(raw ?: DnsProvider.SYSTEM.name) } catch (_: Exception) { DnsProvider.SYSTEM }
}

fun setDnsProvider(context: Context, provider: DnsProvider) {
getSecurePrefs(context).edit().putString("custom_dns", provider.name).apply()
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
return try { AutoSyncInterval.valueOf(raw ?: AutoSyncInterval.DAILY.name) } catch (_: Exception) { AutoSyncInterval.DAILY }
}

fun setAutoSyncInterval(context: Context, interval: AutoSyncInterval) {
getSecurePrefs(context).edit().putString("auto_sync_interval", interval.name).apply()
}

fun shouldRunBackgroundSync(context: Context): Boolean {
val interval = getAutoSyncInterval(context)
if (interval == AutoSyncInterval.MANUAL || interval.days <= 0) return false
val lastSync = getSecurePrefs(context).getLong("last_catalog_sync_ms", 0L)
return (System.currentTimeMillis() - lastSync) >= interval.days * 86400000L
}

fun markCatalogSyncedNow(context: Context) {
getSecurePrefs(context).edit().putLong("last_catalog_sync_ms", System.currentTimeMillis()).apply()
}

// ==========================================
// إدارة تعدد الاشتراكات + التعديل + التبديل
// ==========================================
fun getAllSubscriptions(context: Context): MutableList<SavedSubscription> {
val prefs = getSecurePrefs(context)
val raw = prefs.getString("multi_subs_json", null)
val list = mutableListOf<SavedSubscription>()
if (!raw.isNullOrEmpty()) {
try {
val arr = JSONArray(raw)
for (i in 0 until arr.length()) {
val o = arr.getJSONObject(i)
list.add(
SavedSubscription(
id = o.optString("id", UUID.randomUUID().toString()),
label = o.optString("label", "اشتراك ${i + 1}"),
type = o.optString("type", "XTREAM"),
host = o.optString("host"),
username = o.optString("username"),
password = o.optString("password"),
m3uUrl = o.optString("m3uUrl"),
macAddress = o.optString("macAddress"),
status = o.optString("status", "Active"),
expDateFormatted = o.optString("expDateFormatted", "Unlimited"),
allowedCountries = o.optString("allowedCountries", "Global / All"),
activeCons = o.optString("activeCons", "1"),
maxCons = o.optString("maxCons", "1")
)
)
}
} catch (_: Exception) { }
}
// ترحيل الاشتراك القديم تلقائياً إن وجد
if (list.isEmpty()) {
val oldType = prefs.getString("sub_type", null)
if (oldType != null) {
val migrated = SavedSubscription(
id = "default_sub",
label = prefs.getString("sub_user", "اشتراك 1")?.ifEmpty { "اشتراك 1" } ?: "اشتراك 1",
type = oldType,
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
list.add(migrated)
saveAllSubscriptions(context, list, migrated.id)
}
}
return list
}

private fun saveAllSubscriptions(context: Context, list: List<SavedSubscription>, activeId: String?) {
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
put("allowedCountries", s.allowedCountries)
put("activeCons", s.activeCons)
put("maxCons", s.maxCons)
})
}
val ed = getSecurePrefs(context).edit().putString("multi_subs_json", arr.toString())
if (activeId != null) ed.putString("active_sub_id", activeId)
ed.apply()
}

fun saveSubscription(context: Context, sub: SavedSubscription) {
val list = getAllSubscriptions(context)
val idx = list.indexOfFirst { it.id == sub.id }
if (idx != -1) list[idx] = sub else list.add(sub)
saveAllSubscriptions(context, list, sub.id)
}

fun switchActiveSubscription(context: Context, subId: String) {
getSecurePrefs(context).edit().putString("active_sub_id", subId).apply()
}

fun getSavedSubscription(context: Context): SavedSubscription? {
val list = getAllSubscriptions(context)
if (list.isEmpty()) return null
val activeId = getSecurePrefs(context).getString("active_sub_id", null)
return list.find { it.id == activeId } ?: list.first()
}

fun deleteSubscriptionById(context: Context, subId: String) {
val list = getAllSubscriptions(context).filterNot { it.id == subId }
val nextId = list.firstOrNull()?.id ?: ""
saveAllSubscriptions(context, list, nextId)
if (list.isEmpty()) {
try {
File(context.filesDir, "devkan_cached_playlist_v5.json").delete()
File(context.filesDir, "devkan_cached_sports_epg_v5.json").delete()
} catch (_: Exception) { }
}
}

fun clearSubscription(context: Context) {
val active = getSavedSubscription(context)
if (active != null) deleteSubscriptionById(context, active.id)
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
return try { EngineType.valueOf(raw ?: EngineType.AUTO_HYBRID.name) } catch (e: Exception) { EngineType.AUTO_HYBRID }
}

fun saveCachedItems(context: Context, items: List<MediaItemModel>) {
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
put("originTag", item.originTag)
put("genreTag", item.genreTag)
put("addedTimestamp", item.addedTimestamp)
put("is4k", item.is4k)
})
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
} catch (_: Exception) { emptyList() }
}

fun saveCachedSportsEpg(context: Context, programs: List<EpgProgram>) {
try {
val arr = JSONArray()
programs.forEach { p ->
arr.put(JSONObject().apply {
put("id", p.id)
put("channelId", p.channelId)
put("channelName", p.channelName)
put("channelLogo", p.channelLogo ?: "")
put("streamQuality", p.streamQuality ?: "")
put("title", p.title)
put("description", p.description ?: "")
put("startTime", p.startTime)
put("endTime", p.endTime)
put("category", p.category ?: "")
put("isSports", p.isSports)
put("homeTeam", p.homeTeam ?: "")
put("awayTeam", p.awayTeam ?: "")
put("competition", p.competition ?: "")
})
}
File(context.filesDir, "devkan_cached_sports_epg_v5.json").writeText(arr.toString())
} catch (_: Exception) { }
}

fun getCachedSportsEpg(context: Context): List<EpgProgram> {
return try {
val file = File(context.filesDir, "devkan_cached_sports_epg_v5.json")
if (!file.exists()) return emptyList()
val arr = JSONArray(file.readText())
val list = ArrayList<EpgProgram>(arr.length())
for (i in 0 until arr.length()) {
val obj = arr.getJSONObject(i)
list.add(
EpgProgram(
id = obj.optString("id"),
channelId = obj.optString("channelId"),
channelName = obj.optString("channelName"),
channelLogo = obj.optString("channelLogo").ifEmpty { null },
streamQuality = obj.optString("streamQuality").ifEmpty { null },
title = obj.optString("title"),
description = obj.optString("description").ifEmpty { null },
startTime = obj.optLong("startTime"),
endTime = obj.optLong("endTime"),
category = obj.optString("category").ifEmpty { null },
isSports = obj.optBoolean("isSports", false),
homeTeam = obj.optString("homeTeam").ifEmpty { null },
awayTeam = obj.optString("awayTeam").ifEmpty { null },
competition = obj.optString("competition").ifEmpty { null }
)
)
}
list
} catch (_: Exception) { emptyList() }
}
}

object EngineCoordinator {
val currentDiagnostics = mutableStateOf(StreamDiagnostics())

fun selectInitialEngine(
url: String,
preferred: EngineType,
isSportsSection: Boolean = false
): EngineType {
val manualOverride = when (preferred) {
EngineType.MEDIA3_ONLY -> PlayerEngine.MEDIA3_EXOPLAYER
EngineType.LIBVLC_ONLY -> PlayerEngine.LIB_VLC
EngineType.AUTO_HYBRID -> null
}
if (isSportsSection) {
val config = PlayerEngineRouter.resolvePlaybackConfig(
isSportsSection = true,
manualOverrideEngine = manualOverride
)
return if (config.engine == PlayerEngine.LIB_VLC) EngineType.LIBVLC_ONLY else EngineType.MEDIA3_ONLY
}
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

fun buildSmartHeaders(geoBypass: Boolean, customUserAgent: String = "IPTVSmartersPro/3.1.5 (Linux; Android 14)"): Map<String, String> {
val headers = mutableMapOf(
"User-Agent" to customUserAgent,
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
client.newCall(Request.Builder().url(cleanUrl).head().build()).execute().use { }
val ping = (System.currentTimeMillis() - startPing).coerceAtLeast(12L)
val startDl = System.currentTimeMillis()
var bytesRead = 0L
client.newCall(Request.Builder().url(cleanUrl).get().build()).execute().use { resp ->
val stream = resp.body?.byteStream()
val buf = ByteArray(8192)
var read = 0
while (stream != null && stream.read(buf).also { read = it } != -1 && bytesRead < 250_000) {
bytesRead += read
}
}
val elapsedSec = ((System.currentTimeMillis() - startDl).coerceAtLeast(50L)) / 1000.0
val mbps = ((bytesRead * 8.0) / 1_000_000.0) / elapsedSec
ping to String.format(Locale.US, "%.1f", mbps.coerceIn(1.5, 150.0)).toDouble()
} catch (e: Exception) {
999L to 0.0
}
}
}

// ============================================================================
// 3. مستورد الاشتراكات المتوازي (Parallel Fetching - يفتح بثواني بدل دقيقة)
// ============================================================================
object DevkanPlaylistRepository {
private fun buildClient(geoBypass: Boolean, userAgent: String = "IPTVSmartersPro/3.1.5 (Linux; Android 14)"): OkHttpClient {
return OkHttpClient.Builder()
.connectTimeout(10, TimeUnit.SECONDS)
.readTimeout(20, TimeUnit.SECONDS)
.addInterceptor { chain ->
val builder = chain.request().newBuilder()
.header("User-Agent", userAgent)
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

private fun detectStreamQuality(title: String): String {
val t = title.uppercase()
return when {
t.contains("4K") || t.contains("UHD") -> "4K"
t.contains("FHD") || t.contains("1080") -> "FHD"
t.contains("HD") || t.contains("720") -> "HD"
else -> "SD"
}
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

suspend fun syncAndClassifySportsEpg(
context: Context,
sub: SavedSubscription,
liveChannels: List<MediaItemModel>
): ClassifiedSportsData = withContext(Dispatchers.IO) {
if (sub.type != "XTREAM") {
val cached = DevkanVault.getCachedSportsEpg(context)
return@withContext EpgSportsPipeline.classifyAndGroupSports(cached)
}
val sportsChannels = liveChannels.filter {
it.contentType == "LIVE" && SportsMatchExtractor.isSportsProgram(it.group, it.title, null)
}.take(30)
val client = buildClient(true)
val cleanHost = sub.host.trimEnd('/')
val u = URLEncoder.encode(sub.username, "UTF-8")
val p = URLEncoder.encode(sub.password, "UTF-8")
val enrichedList = coroutineScope {
sportsChannels.filter { it.rawId.isNotBlank() }.map { ch ->
async {
val localList = mutableListOf<EpgProgram>()
try {
val url = "$cleanHost/player_api.php?username=$u&password=$p&action=get_short_epg&stream_id=${ch.rawId}&limit=4"
client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
val listings = JSONObject(resp.body?.string() ?: "{}").optJSONArray("epg_listings") ?: JSONArray()
for (i in 0 until listings.length()) {
val item = listings.getJSONObject(i)
val startMs = EpgSportsPipeline.resolveXtreamTimestamp(
unixSeconds = item.optLong("start_timestamp", 0L),
rawDateStr = item.optString("start", "")
)
val endMs = EpgSportsPipeline.resolveXtreamTimestamp(
unixSeconds = item.optLong("stop_timestamp", 0L),
rawDateStr = item.optString("end", "")
)
if (startMs > 0L && endMs > startMs) {
localList.add(
EpgSportsPipeline.enrichProgramForStorage(
EpgProgram(
id = item.optString("id", "${ch.rawId}_$i"),
channelId = ch.rawId,
channelName = ch.title,
channelLogo = ch.logo.ifEmpty { null },
streamQuality = detectStreamQuality(ch.title),
title = item.optString("title", ""),
description = item.optString("description", ""),
startTime = startMs,
endTime = endMs,
category = ch.group
)
)
)
}
}
}
} catch (_: Exception) { }
localList
}
}.awaitAll().flatten()
}
if (enrichedList.isNotEmpty()) {
DevkanVault.saveCachedSportsEpg(context, enrichedList)
return@withContext EpgSportsPipeline.classifyAndGroupSports(enrichedList)
}
EpgSportsPipeline.classifyAndGroupSports(DevkanVault.getCachedSportsEpg(context))
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
if (nowTitle.isNotBlank()) return@withContext (nowTitle.trim() to nextTitle.trim().ifEmpty { "البث المستمر" })
}
}
} catch (_: Exception) { }
"البث المباشر المستمر" to "البرنامج التالي على الجدول"
}

// =========================================================================
// تحميل متوازي فائق السرعة (7 طلبات بالتوازي بدل الانتظار التسلسلي)
// =========================================================================
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

coroutineScope {
val infoTask = async {
try {
client.newCall(Request.Builder().url(baseApi).build()).execute().use { resp ->
val userInfo = JSONObject(resp.body?.string() ?: "{}").optJSONObject("user_info")
if (userInfo != null) {
val status = userInfo.optString("status", "Active")
val expRaw = userInfo.optString("exp_date", "")
val expFormatted = if (expRaw.isNotEmpty() && expRaw != "null") {
val millis = expRaw.toLongOrNull()?.times(1000L) ?: 0L
if (millis > 0L) SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis)) else "Unlimited"
} else "Unlimited"
val allowedRaw = userInfo.optString("allowed_countries", "")
val countries = if (allowedRaw.isNotEmpty() && allowedRaw != "[]" && allowedRaw != "null") allowedRaw else "Global / All Countries"
updatedSub = updatedSub.copy(
status = status,
expDateFormatted = expFormatted,
allowedCountries = countries,
activeCons = userInfo.optString("active_cons", "1"),
maxCons = userInfo.optString("max_connections", "1")
)
}
}
} catch (_: Exception) { }
}

val liveCatTask = async {
val map = mutableMapOf<String, String>()
try {
client.newCall(Request.Builder().url("$baseApi&action=get_live_categories").build()).execute().use { resp ->
val arr = JSONArray(resp.body?.string() ?: "[]")
for (i in 0 until arr.length()) {
val obj = arr.getJSONObject(i)
map[obj.optString("category_id")] = obj.optString("category_name", "Live TV")
}
}
} catch (_: Exception) { }
map
}

val vodCatTask = async {
val map = mutableMapOf<String, String>()
try {
client.newCall(Request.Builder().url("$baseApi&action=get_vod_categories").build()).execute().use { resp ->
val arr = JSONArray(resp.body?.string() ?: "[]")
for (i in 0 until arr.length()) {
val obj = arr.getJSONObject(i)
map[obj.optString("category_id")] = obj.optString("category_name", "Movies")
}
}
} catch (_: Exception) { }
map
}

val seriesCatTask = async {
val map = mutableMapOf<String, String>()
try {
client.newCall(Request.Builder().url("$baseApi&action=get_series_categories").build()).execute().use { resp ->
val arr = JSONArray(resp.body?.string() ?: "[]")
for (i in 0 until arr.length()) {
val obj = arr.getJSONObject(i)
map[obj.optString("category_id")] = obj.optString("category_name", "Series")
}
}
} catch (_: Exception) { }
map
}

val liveStreamsTask = async {
val list = mutableListOf<MediaItemModel>()
try {
client.newCall(Request.Builder().url("$baseApi&action=get_live_streams").build()).execute().use { resp ->
val arr = JSONArray(resp.body?.string() ?: "[]")
val cats = liveCatTask.await()
for (i in 0 until arr.length()) {
val obj = arr.getJSONObject(i)
val id = obj.optString("stream_id")
val name = obj.optString("name", "Channel $i")
val icon = obj.optString("stream_icon", "")
val catName = cats[obj.optString("category_id")] ?: "Live Channels"
list.add(
MediaItemModel(
id = "live_$id",
rawId = id,
title = name,
url = "$cleanHost/live/${sub.username}/${sub.password}/$id.m3u8",
fallbackUrl = "$cleanHost/live/${sub.username}/${sub.password}/$id.ts",
group = catName,
logo = icon,
contentType = "LIVE"
)
)
}
}
} catch (_: Exception) { }
list
}

val vodStreamsTask = async {
val list = mutableListOf<MediaItemModel>()
try {
client.newCall(Request.Builder().url("$baseApi&action=get_vod_streams").build()).execute().use { resp ->
val arr = JSONArray(resp.body?.string() ?: "[]")
val cats = vodCatTask.await()
for (i in 0 until minOf(arr.length(), 1800)) {
val obj = arr.getJSONObject(i)
val id = obj.optString("stream_id")
val name = obj.optString("name", "Movie $i")
val icon = obj.optString("stream_icon", "")
val rating = obj.optString("rating", "")
val addedTs = obj.optString("added", "0").toLongOrNull() ?: 0L
val ext = obj.optString("container_extension", "mp4").ifEmpty { "mp4" }
val catName = cats[obj.optString("category_id")] ?: "Movies"
val yearMatch = Regex("(19|20)\\d{2}").find(name)?.value ?: ""
list.add(
MediaItemModel(
id = "vod_$id",
rawId = id,
title = name,
url = "$cleanHost/movie/${sub.username}/${sub.password}/$id.$ext",
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
list
}

val seriesStreamsTask = async {
val list = mutableListOf<MediaItemModel>()
try {
client.newCall(Request.Builder().url("$baseApi&action=get_series").build()).execute().use { resp ->
val arr = JSONArray(resp.body?.string() ?: "[]")
val cats = seriesCatTask.await()
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
val catName = cats[obj.optString("category_id")] ?: "Series"
list.add(
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
list
}

infoTask.await()
items.addAll(liveStreamsTask.await())
items.addAll(vodStreamsTask.await())
items.addAll(seriesStreamsTask.await())
}
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
