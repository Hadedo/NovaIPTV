package com.hadedo.novaiptv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

data class MediaItemModel(
    val id: String,
    val name: String,
    val url: String,
    val category: String,
    val logo: String = "",
    val type: String = "live" // live, movie, series
)

object ServersEngine {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    // 1. محرك جلب قنوات وأفلام ومسلسلات Xtream Codes
    suspend fun loadXtream(
        host: String,
        user: String,
        pass: String,
        type: String // live, vod, series
    ): List<MediaItemModel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItemModel>()
        try {
            val cleanHost = host.trim().removeSuffix("/")
            val action = when (type) {
                "vod" -> "get_vod_streams"
                "series" -> "get_series"
                else -> "get_live_streams"
            }
            val apiUrl = "$cleanHost/player_api.php?username=$user&password=$pass&action=$action"
            val req = Request.Builder().url(apiUrl).header("User-Agent", "NovaIPTV/2.0").build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()

            val jsonArray = JSONArray(body)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val streamId = obj.optString("stream_id", obj.optString("series_id", ""))
                val name = obj.optString("name", "بدون اسم")
                val cat = obj.optString("category_name", "باقة عامة")
                val icon = obj.optString("stream_icon", obj.optString("cover", ""))
                val ext = obj.optString("container_extension", "mp4")

                val streamUrl = when (type) {
                    "vod" -> "$cleanHost/movie/$user/$pass/$streamId.$ext"
                    "series" -> "$cleanHost/series/$user/$pass/$streamId.$ext"
                    else -> "$cleanHost/live/$user/$pass/$streamId.ts"
                }

                list.add(MediaItemModel(streamId, name, streamUrl, cat, icon, type))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    // 2. محرك الاتصال بسيرفرات Stalker Portal / MAC
    suspend fun loadStalker(portalUrl: String, macAddress: String): List<MediaItemModel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItemModel>()
        try {
            val baseUrl = portalUrl.trim().removeSuffix("/c").removeSuffix("/")
            val encodedMac = URLEncoder.encode(macAddress.trim(), "UTF-8")
            val cookie = "mac=$encodedMac; stb_lang=en; timezone=Asia/Baghdad"

            // خطوة المصافحة (Handshake) للحصول على Token
            val handshakeUrl = "$baseUrl/portal.php?type=stb&action=handshake&token=&JsHttpRequest=1-xml"
            val hsReq = Request.Builder()
                .url(handshakeUrl)
                .header("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C) MAG200 stbapp ver: 2 rev: 250 Safari/533.1")
                .header("Cookie", cookie)
                .build()

            val hsBody = client.newCall(hsReq).execute().body?.string() ?: return@withContext emptyList()
            val token = JSONObject(hsBody).getJSONObject("js").getString("token")

            // جلب جميع القنوات من البورتال
            val channelsUrl = "$baseUrl/portal.php?type=itv&action=get_all_channels&JsHttpRequest=1-xml"
            val chReq = Request.Builder()
                .url(channelsUrl)
                .header("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C) MAG200 stbapp ver: 2 rev: 250 Safari/533.1")
                .header("Cookie", cookie)
                .header("Authorization", "Bearer $token")
                .build()

            val chBody = client.newCall(chReq).execute().body?.string() ?: return@withContext emptyList()
            val dataArray = JSONObject(chBody).getJSONObject("js").getJSONArray("data")

            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val id = item.optString("id")
                val name = item.optString("name", "قناة ستالكر")
                val cmd = item.optString("cmd").removePrefix("ffmpeg ").trim()
                val realUrl = if (cmd.startsWith("http")) cmd else "$baseUrl/play/live.php?mac=$encodedMac&stream=$id&extension=ts"

                list.add(MediaItemModel(id, name, realUrl, "Stalker Live", "", "live"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }
}
