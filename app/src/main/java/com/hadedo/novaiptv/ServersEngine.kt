package com.hadedo.novaiptv

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MediaItemModel(
    val id: String,
    val name: String,
    val url: String,
    val category: String,
    val logo: String = "",
    val type: String = "live"
)

data class XtreamCredentials(val host: String, val user: String, val pass: String)

data class AccountInfo(
    val status: String,
    val expDate: String,
    val activeCons: String,
    val maxCons: String
)

object ServersEngine {
    private val client = OkHttpClient.Builder().followRedirects(true).build()

    fun parseXtreamFromUrl(fullUrl: String): XtreamCredentials? {
        return try {
            val uri = Uri.parse(fullUrl.trim())
            val user = uri.getQueryParameter("username")
            val pass = uri.getQueryParameter("password")
            if (!user.isNullOrEmpty() && !pass.isNullOrEmpty() && uri.host != null) {
                val port = if (uri.port != -1) ":${uri.port}" else ""
                XtreamCredentials("${uri.scheme ?: "http"}://${uri.host}$port", user, pass)
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun getAccountInfo(host: String, user: String, pass: String): AccountInfo? = withContext(Dispatchers.IO) {
        try {
            val cleanHost = host.trim().removeSuffix("/")
            val url = "$cleanHost/player_api.php?username=$user&password=$pass"
            val body = client.newCall(Request.Builder().url(url).build()).execute().body?.string() ?: return@withContext null
            val info = JSONObject(body).getJSONObject("user_info")
            val status = info.optString("status", "Active")
            val expRaw = info.optString("exp_date", "")
            val expFormatted = if (expRaw.isNotEmpty() && expRaw != "null") {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                sdf.format(Date(expRaw.toLong() * 1000))
            } else "غير محدود"
            val active = info.optString("active_cons", "0")
            val max = info.optString("max_connections", "1")
            AccountInfo(status, expFormatted, active, max)
        } catch (e: Exception) { null }
    }

    suspend fun loadXtream(host: String, user: String, pass: String, type: String): List<MediaItemModel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItemModel>()
        try {
            val cleanHost = host.trim().removeSuffix("/")
            val catAction = when (type) { "vod" -> "get_vod_categories"; "series" -> "get_series_categories"; else -> "get_live_categories" }
            val streamAction = when (type) { "vod" -> "get_vod_streams"; "series" -> "get_series"; else -> "get_live_streams" }

            val catMap = mutableMapOf<String, String>()
            try {
                val catBody = client.newCall(Request.Builder().url("$cleanHost/player_api.php?username=$user&password=$pass&action=$catAction").build()).execute().body?.string()
                if (!catBody.isNullOrEmpty()) {
                    val catArr = JSONArray(catBody)
                    for (i in 0 until catArr.length()) {
                        val c = catArr.getJSONObject(i)
                        catMap[c.optString("category_id")] = c.optString("category_name", "عام")
                    }
                }
            } catch (_: Exception) {}

            val req = Request.Builder().url("$cleanHost/player_api.php?username=$user&password=$pass&action=$streamAction").header("User-Agent", "NovaIPTV/4.0").build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val jsonArray = JSONArray(body)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val streamId = obj.optString("stream_id", obj.optString("series_id", ""))
                val name = obj.optString("name", "بدون اسم")
                val catName = catMap[obj.optString("category_id", "")] ?: "باقة عامة"
                val icon = obj.optString("stream_icon", obj.optString("cover", ""))
                val ext = obj.optString("container_extension", "mp4")
                val streamUrl = when (type) {
                    "vod" -> "$cleanHost/movie/$user/$pass/$streamId.$ext"
                    "series" -> "$cleanHost/series/$user/$pass/$streamId.$ext"
                    else -> "$cleanHost/live/$user/$pass/$streamId.ts"
                }
                list.add(MediaItemModel(streamId, name, streamUrl, catName, icon, type))
            }
        } catch (e: Exception) { e.printStackTrace() }
        list
    }

    suspend fun loadStalker(portalUrl: String, macAddress: String): List<MediaItemModel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItemModel>()
        try {
            val baseUrl = portalUrl.trim().removeSuffix("/c").removeSuffix("/")
            val encodedMac = URLEncoder.encode(macAddress.trim(), "UTF-8")
            val cookie = "mac=$encodedMac; stb_lang=en; timezone=Asia/Baghdad"
            val hsBody = client.newCall(Request.Builder().url("$baseUrl/portal.php?type=stb&action=handshake&token=&JsHttpRequest=1-xml").header("Cookie", cookie).build()).execute().body?.string() ?: return@withContext emptyList()
            val token = JSONObject(hsBody).getJSONObject("js").getString("token")
            val chBody = client.newCall(Request.Builder().url("$baseUrl/portal.php?type=itv&action=get_all_channels&JsHttpRequest=1-xml").header("Cookie", cookie).header("Authorization", "Bearer $token").build()).execute().body?.string() ?: return@withContext emptyList()
            val dataArray = JSONObject(chBody).getJSONObject("js").getJSONArray("data")
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val id = item.optString("id")
                val name = item.optString("name", "قناة ستالكر")
                val cmd = item.optString("cmd").removePrefix("ffmpeg ").trim()
                val realUrl = if (cmd.startsWith("http")) cmd else "$baseUrl/play/live.php?mac=$encodedMac&stream=$id&extension=ts"
                list.add(MediaItemModel(id, name, realUrl, "Stalker Live", "", "live"))
            }
        } catch (e: Exception) { e.printStackTrace() }
        list
    }
}
// END_ENGINE
