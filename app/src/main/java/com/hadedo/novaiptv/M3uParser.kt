package com.hadedo.novaiptv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class Channel(
    val name: String,
    val url: String,
    val group: String = "عام"
)

object M3uParser {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    suspend fun loadFromUrl(playlistUrl: String): List<Channel> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<Channel>()
        try {
            val request = Request.Builder()
                .url(playlistUrl.trim())
                .header("User-Agent", "NovaIPTV/1.0")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()

            var currentName = ""
            var currentGroup = "عام"

            body.lineSequence().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.startsWith("#EXTINF:")) {
                    currentName = line.substringAfterLast(",").trim().ifEmpty { "قناة غير معروفة" }
                    val groupMatch = Regex("""group-title="([^"]*)"""").find(line)
                    currentGroup = groupMatch?.groupValues?.get(1)?.ifEmpty { "عام" } ?: "عام"
                } else if (line.isNotEmpty() && !line.startsWith("#")) {
                    if (currentName.isNotEmpty()) {
                        channels.add(Channel(currentName, line, currentGroup))
                        currentName = ""
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        channels
    }
}
