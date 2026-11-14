package com.hadedo.novaiptv

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class WatchProgress(
    val id: String,
    val title: String,
    val logo: String,
    val url: String,
    val type: String, // "vod" or "series"
    val positionMs: Long,
    val durationMs: Long,
    val subtitleInfo: String = ""
)

data class CustomFolder(
    val id: String,
    val name: String,
    val itemIds: List<String>
)

object AppStorage {
    private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 ساعة

    // 1. حفظ القوائم في ذاكرة الهاتف (Zero-Wait Loading)
    fun saveMediaCache(context: Context, serverId: String, type: String, list: List<MediaItemModel>) {
        try {
            val arr = JSONArray()
            list.forEach { m ->
                arr.put(JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("url", m.url)
                    put("cat", m.category)
                    put("logo", m.logo)
                    put("type", m.type)
                })
            }
            val wrapper = JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("items", arr)
            }
            File(context.filesDir, "nova_cache_${serverId}_$type.json").writeText(wrapper.toString())
        } catch (_: Exception) {}
    }

    fun loadMediaCache(
        context: Context,
        serverId: String,
        type: String,
        ignoreExpiry: Boolean = false
    ): List<MediaItemModel>? {
        return try {
            val file = File(context.filesDir, "nova_cache_${serverId}_$type.json")
            if (!file.exists()) return null
            val wrapper = JSONObject(file.readText())
            val ts = wrapper.optLong("timestamp", 0L)
            if (!ignoreExpiry && (System.currentTimeMillis() - ts > CACHE_TTL_MS)) return null

            val arr = wrapper.getJSONArray("items")
            val list = mutableListOf<MediaItemModel>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    MediaItemModel(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        url = o.getString("url"),
                        category = o.getString("cat"),
                        logo = o.optString("logo", ""),
                        type = o.optString("type", type)
                    )
                )
            }
            list.ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    fun getCacheSizeMb(context: Context): String {
        val bytes = context.filesDir.listFiles()
            ?.filter { it.name.startsWith("nova_cache_") }
            ?.sumOf { it.length() } ?: 0L
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    }

    fun clearAllMediaCache(context: Context) {
        context.filesDir.listFiles()?.filter { it.name.startsWith("nova_cache_") }?.forEach { it.delete() }
    }

    // 2. حفظ واسترجاع موضع التوقف (Continue Watching)
    fun saveWatchProgress(context: Context, item: WatchProgress) {
        try {
            val prefs = context.getSharedPreferences("nova_watch_history", Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString("history_json", "[]") ?: "[]")
            val newArr = JSONArray()

            val isFinished = item.durationMs > 0 && (item.positionMs.toFloat() / item.durationMs.toFloat()) > 0.95f
            if (!isFinished && item.positionMs > 10_000L) {
                newArr.put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("logo", item.logo)
                    put("url", item.url)
                    put("type", item.type)
                    put("pos", item.positionMs)
                    put("dur", item.durationMs)
                    put("sub", item.subtitleInfo)
                })
            }

            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.getString("id") != item.id && newArr.length() < 25) {
                    newArr.put(o)
                }
            }
            prefs.edit().putString("history_json", newArr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getWatchHistory(context: Context): List<WatchProgress> {
        val list = mutableListOf<WatchProgress>()
        try {
            val prefs = context.getSharedPreferences("nova_watch_history", Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString("history_json", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    WatchProgress(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        logo = o.optString("logo", ""),
                        url = o.getString("url"),
                        type = o.optString("type", "vod"),
                        positionMs = o.optLong("pos", 0L),
                        durationMs = o.optLong("dur", 1L),
                        subtitleInfo = o.optString("sub", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun getSavedPositionForUrl(context: Context, url: String): Long {
        return getWatchHistory(context).find { it.url == url }?.positionMs ?: 0L
    }

    fun removeWatchProgress(context: Context, id: String) {
        try {
            val prefs = context.getSharedPreferences("nova_watch_history", Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString("history_json", "[]") ?: "[]")
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.getString("id") != id) newArr.put(o)
            }
            prefs.edit().putString("history_json", newArr.toString()).apply()
        } catch (_: Exception) {}
    }

    // 3. سجل القنوات المشاهدة مؤخراً (Recently Watched Channels)
    fun addRecentChannel(context: Context, item: MediaItemModel) {
        try {
            val prefs = context.getSharedPreferences("nova_recent_channels", Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString("recent_json", "[]") ?: "[]")
            val newArr = JSONArray()
            newArr.put(JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("url", item.url)
                put("cat", item.category)
                put("logo", item.logo)
            })
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.getString("url") != item.url && newArr.length() < 15) {
                    newArr.put(o)
                }
            }
            prefs.edit().putString("recent_json", newArr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getRecentChannels(context: Context): List<MediaItemModel> {
        val list = mutableListOf<MediaItemModel>()
        try {
            val prefs = context.getSharedPreferences("nova_recent_channels", Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString("recent_json", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    MediaItemModel(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        url = o.getString("url"),
                        category = o.optString("cat", "مباشر"),
                        logo = o.optString("logo", ""),
                        type = "live"
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun removeRecentChannel(context: Context, url: String) {
        try {
            val prefs = context.getSharedPreferences("nova_recent_channels", Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString("recent_json", "[]") ?: "[]")
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.getString("url") != url) newArr.put(o)
            }
            prefs.edit().putString("recent_json", newArr.toString()).apply()
        } catch (_: Exception) {}
    }

    // 4. إدارة المجلدات الشخصية المخصصة (Custom Folders)
    fun getCustomFolders(context: Context): List<CustomFolder> {
        val list = mutableListOf<CustomFolder>()
        try {
            val prefs = context.getSharedPreferences("nova_folders_prefs", Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString("folders_json", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val idsArr = o.getJSONArray("ids")
                val ids = mutableListOf<String>()
                for (j in 0 until idsArr.length()) ids.add(idsArr.getString(j))
                list.add(CustomFolder(o.getString("id"), o.getString("name"), ids))
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveCustomFolders(context: Context, folders: List<CustomFolder>) {
        val arr = JSONArray()
        folders.forEach { f ->
            val idsArr = JSONArray()
            f.itemIds.forEach { idsArr.put(it) }
            arr.put(JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("ids", idsArr)
            })
        }
        context.getSharedPreferences("nova_folders_prefs", Context.MODE_PRIVATE)
            .edit().putString("folders_json", arr.toString()).apply()
    }

    // 5. النسخ الاحتياطي السريع (Backup & Restore)
    fun exportBackupCode(context: Context): String {
        val prefs = context.getSharedPreferences("nova_neon_prefs", Context.MODE_PRIVATE)
        val foldersPrefs = context.getSharedPreferences("nova_folders_prefs", Context.MODE_PRIVATE)
        val backup = JSONObject().apply {
            put("servers", prefs.getString("saved_servers_json", "[]"))
            put("favs", JSONArray(prefs.getStringSet("fav_ids", emptySet())?.toList() ?: emptyList<String>()))
            put("folders", foldersPrefs.getString("folders_json", "[]"))
        }
        return Base64.encodeToString(backup.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    fun importBackupCode(context: Context, code: String): Boolean {
        return try {
            val jsonStr = String(Base64.decode(code.trim(), Base64.NO_WRAP), Charsets.UTF_8)
            val obj = JSONObject(jsonStr)
            val prefs = context.getSharedPreferences("nova_neon_prefs", Context.MODE_PRIVATE)
            val favArr = obj.getJSONArray("favs")
            val favSet = mutableSetOf<String>()
            for (i in 0 until favArr.length()) favSet.add(favArr.getString(i))
            prefs.edit()
                .putString("saved_servers_json", obj.getString("servers"))
                .putStringSet("fav_ids", favSet)
                .apply()
            if (obj.has("folders")) {
                context.getSharedPreferences("nova_folders_prefs", Context.MODE_PRIVATE)
                    .edit().putString("folders_json", obj.getString("folders")).apply()
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
// END_STORAGE
