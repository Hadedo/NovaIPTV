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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

// ============================================================================
// 1. طبقة الأمان والتخزين المشفر (Android Keystore + EncryptedSharedPreferences)
// ============================================================================
object DevkanVault {
    private fun getSecurePrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "devkan_secure_vault",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("devkan_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    fun isDisclaimerAccepted(context: Context): Boolean =
        getSecurePrefs(context).getBoolean("disclaimer_accepted", false)

    fun setDisclaimerAccepted(context: Context, accepted: Boolean) {
        getSecurePrefs(context).edit().putBoolean("disclaimer_accepted", accepted).apply()
    }

    fun getAppLockPin(context: Context): String =
        getSecurePrefs(context).getString("app_lock_pin", "") ?: ""

    fun setAppLockPin(context: Context, pin: String) {
        getSecurePrefs(context).edit().putString("app_lock_pin", pin).apply()
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

    fun toggleWatchlist(context: Context, itemKey: String): Boolean {
        val prefs = getSecurePrefs(context)
        val current = prefs.getStringSet("watchlist_items", emptySet())?.toMutableSet() ?: mutableSetOf()
        val added = if (current.contains(itemKey)) {
            current.remove(itemKey)
            false
        } else {
            current.add(itemKey)
            true
        }
        prefs.edit().putStringSet("watchlist_items", current).apply()
        return added
    }

    fun getWatchlist(context: Context): Set<String> =
        getSecurePrefs(context).getStringSet("watchlist_items", emptySet()) ?: emptySet()

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
// 2. المنسق الهجين لمحركات التشغيل (Media3 ExoPlayer + LibVLC)
// ============================================================================
enum class EngineType(val labelAr: String, val labelEn: String, val labelDe: String) {
    AUTO_HYBRID("تلقائي هجين (Media3 + VLC)", "Auto Hybrid (Media3 + VLC)", "Auto-Hybrid (Media3 + VLC)"),
    MEDIA3_ONLY("Media3 ExoPlayer (فائق السرعة)", "Media3 ExoPlayer (Fast Zapping)", "Media3 ExoPlayer (Schnell)"),
    LIBVLC_ONLY("LibVLC (للترميزات الصعبة AC3/TS)", "LibVLC (Advanced Codecs)", "LibVLC (Erweiterte Codecs)")
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

object EngineCoordinator {
    val currentDiagnostics = mutableStateOf(StreamDiagnostics())

    fun selectInitialEngine(url: String, preferred: EngineType): EngineType {
        if (preferred != EngineType.AUTO_HYBRID) return preferred
        val lower = url.lowercase()
        // التوجيه الذكي: صيغ TS/MKV/AVI أو روابط البث الخام تُحوّل تلقائياً إلى LibVLC عند الحاجة
        return if (lower.endsWith(".avi") || lower.contains("mpegts") || lower.contains("rtsp://")) {
            EngineType.LIBVLC_ONLY
        } else {
            EngineType.MEDIA3_ONLY
        }
    }

    fun getAdaptiveBufferMs(pingMs: Long): Triple<Int, Int, Int> {
        // (minBufferMs, maxBufferMs, bufferForPlaybackMs) -> لتحقيق Fast Zapping بدون تقطيع
        return when {
            pingMs in 1..70 -> Triple(4000, 15000, 800)      // إنترنت سريع: فتح القناة في 0.8 ثانية
            pingMs in 71..180 -> Triple(8000, 25000, 1500)   // إنترنت متوسط
            else -> Triple(15000, 45000, 2500)               // إنترنت ضعيف: تخزين مؤقت أكبر لمنع التقطيع
        }
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

            // قياس سرعة الاستجابة التقريبية بالميجابت/ثانية
            val startDl = System.currentTimeMillis()
            val dlReq = Request.Builder().url(cleanUrl).get().build()
            var bytesRead = 0L
            client.newCall(dlReq).execute().use { resp ->
                val stream = resp.body?.byteStream()
                val buf = ByteArray(8192)
                var read: Int
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
// 3. مستورد ملفات M3U المحلية من ذاكرة الهاتف + بيانات جدول البرامج (EPG)
// ============================================================================
data class LocalM3uChannel(
    val name: String,
    val url: String,
    val group: String,
    val logo: String
)

data class EpgSlot(
    val channelName: String,
    val channelLogo: String,
    val currentProgram: String,
    val nextProgram: String,
    val timeRange: String,
    val progress: Float
)

object LocalPlaylistImporter {
    suspend fun parseUri(context: Context, uri: Uri): List<LocalM3uChannel> = withContext(Dispatchers.IO) {
        val result = mutableListOf<LocalM3uChannel>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                var currentName = "Channel"
                var currentGroup = "Local Playlist"
                var currentLogo = ""
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line!!.trim()
                    if (trimmed.startsWith("#EXTINF:")) {
                        currentName = trimmed.substringAfterLast(",").trim().ifEmpty { "Channel" }
                        currentGroup = Regex("""group-title="([^"]*)"""").find(trimmed)?.groupValues?.get(1)?.ifEmpty { "Local Playlist" } ?: "Local Playlist"
                        currentLogo = Regex("""tvg-logo="([^"]*)"""").find(trimmed)?.groupValues?.get(1) ?: ""
                    } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("rtsp://")) {
                        result.add(LocalM3uChannel(currentName, trimmed, currentGroup, currentLogo))
                    }
                }
            }
        } catch (_: Exception) {
        }
        result
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
