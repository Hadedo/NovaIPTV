package com.hadedo.novaiptv

import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.URLEncoder

data class SavedServer(
    val id: String,
    val title: String,
    val type: String, // "xtream", "stalker", "m3u"
    val host: String,
    val user: String = "",
    val pass: String = "",
    val countryCode: String = "🌐",
    val pingMs: Long = 0
)

data class AccountInfo(
    val status: String,
    val expDate: String,
    val activeCons: String,
    val maxCons: String
)

data class MediaItemModel(
    val id: String,
    val name: String,
    val url: String,
    val category: String,
    val logo: String = "",
    val type: String = "live" // "live", "vod", "series"
)

data class EpisodeModel(
    val id: String,
    val episodeNum: Int,
    val title: String,
    val url: String,
    val duration: String = "",
    val plot: String = ""
)

data class SeriesDetails(
    val title: String,
    val plot: String,
    val rating: String,
    val cover: String,
    val seasons: Map<Int, List<EpisodeModel>>
)

data class QualityVariant(
    val qualityTag: String, // "4K UHD", "1080p FHD", "720p HD", "SD / LOW", "بث قياسي"
    val priority: Int,
    val rawName: String,
    val item: MediaItemModel
)

data class GroupedSportsChannel(
    val cleanName: String,
    val category: String,
    val logo: String,
    val variants: List<QualityVariant>,
    val isLiveEvent: Boolean = false
)

data class EpgProgram(
    val nowTitle: String,
    val nextTitle: String
)

object ServersEngine {
    var currentUserAgent: String = "IPTVSmartersPro"
    var streamFormat: String = "ts" // "ts" or "m3u8"
    var sameHostProxy: Boolean = false

    private fun fetchJson(urlStr: String, headers: Map<String, String> = emptyMap()): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 15000
            setRequestProperty("User-Agent", currentUserAgent)
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    // فحص دولة السيرفر وسرعة الاستجابة (Ping)
    suspend fun detectServerCountryAndPing(hostUrl: String): Pair<String, Long> = withContext(Dispatchers.IO) {
        try {
            val cleanHost = Uri.parse(if (hostUrl.startsWith("http")) hostUrl else "http://$hostUrl").host ?: return@withContext Pair("🌐 غير معروف", 0L)
            val start = System.currentTimeMillis()
            val ip = InetAddress.getByName(cleanHost).hostAddress ?: ""
            val ping = System.currentTimeMillis() - start

            val geoJson = fetchJson("http://ip-api.com/json/$ip?fields=countryCode,country")
            val obj = JSONObject(geoJson)
            val code = obj.optString("countryCode", "")
            val countryName = obj.optString("country", "سيرفر مباشر")
            val flag = if (code.length == 2) {
                val c1 = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
                val c2 = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
                String(Character.toChars(c1)) + String(Character.toChars(c2))
            } else "🌐"
            Pair("$flag $countryName", ping)
        } catch (_: Exception) {
            Pair("🌐 اتصال مباشر", 0L)
        }
    }

    // اختبار سرعة التحميل الحقيقية من السيرفر (Speed Test)
    suspend fun runSpeedTest(hostUrl: String): String = withContext(Dispatchers.IO) {
        try {
            val base = if (hostUrl.startsWith("http")) hostUrl.trimEnd('/') else "http://${hostUrl.trimEnd('/')}"
            val start = System.currentTimeMillis()
            val conn = (URL(base).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", currentUserAgent)
            }
            val code = conn.responseCode
            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            if (code in 200..499) {
                val quality = when {
                    elapsed < 180 -> "ممتاز (يدعم 4K)"
                    elapsed < 450 -> "جيد جداً (يدعم FHD)"
                    else -> "متوسط (يفضل HD/SD)"
                }
                "الاستجابة: ${elapsed}ms • $quality"
            } else {
                "تعذر الوصول للسيرفر ($code)"
            }
        } catch (_: Exception) {
            "فشل الاتصال بالسيرفر"
        }
    }

    // استخراج بيانات Xtream تلقائياً من رابط M3U
    fun parseXtreamFromUrl(rawUrl: String): Triple<String, String, String>? {
        return try {
            val uri = Uri.parse(rawUrl.trim())
            val user = uri.getQueryParameter("username")
            val pass = uri.getQueryParameter("password")
            if (!user.isNullOrBlank() && !pass.isNullOrBlank() && uri.host != null) {
                val port = if (uri.port != -1) ":${uri.port}" else ""
                Triple("${uri.scheme ?: "http"}://${uri.host}$port", user, pass)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    // إعادة توجيه رابط البث عبر السيرفر الأساسي وتطبيق صيغة TS/M3U8
    fun resolveStreamUrl(rawUrl: String, activeHost: String): String {
        var finalUrl = rawUrl
        if (sameHostProxy && activeHost.isNotBlank()) {
            try {
                val baseUri = Uri.parse(if (activeHost.startsWith("http")) activeHost else "http://$activeHost")
                val streamUri = Uri.parse(finalUrl)
                if (baseUri.host != null && streamUri.host != null && baseUri.host != streamUri.host) {
                    finalUrl = streamUri.buildUpon()
                        .scheme(baseUri.scheme ?: "http")
                        .encodedAuthority(baseUri.encodedAuthority)
                        .build()
                        .toString()
                }
            } catch (_: Exception) {}
        }
        if (streamFormat == "m3u8" && finalUrl.endsWith(".ts", ignoreCase = true)) {
            finalUrl = finalUrl.dropLast(3) + ".m3u8"
        } else if (streamFormat == "ts" && finalUrl.endsWith(".m3u8", ignoreCase = true)) {
            finalUrl = finalUrl.dropLast(5) + ".ts"
        }
        return finalUrl
    }

    // جلب معلومات الاشتراك
    suspend fun getAccountInfo(host: String, user: String, pass: String): AccountInfo? = withContext(Dispatchers.IO) {
        try {
            val base = host.trimEnd('/')
            val infoObj = JSONObject(fetchJson("$base/player_api.php?username=$user&password=$pass")).getJSONObject("user_info")
            val expTimestamp = infoObj.optString("exp_date", "0").toLongOrNull() ?: 0L
            val expFormatted = if (expTimestamp > 0) {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(java.util.Date(expTimestamp * 1000))
            } else "غير محدود"
            AccountInfo(
                status = infoObj.optString("status", "Active"),
                expDate = expFormatted,
                activeCons = infoObj.optString("active_cons", "0"),
                maxCons = infoObj.optString("max_connections", "1")
            )
        } catch (_: Exception) {
            null
        }
    }

    // التحقق من القنوات الفاصلة الوهمية مثل ##### BEIN #####
    fun isSeparatorChannel(name: String): Boolean {
        val cleaned = name.trim()
        if (cleaned.isBlank()) return true
        if (cleaned.startsWith("###") || cleaned.startsWith("---") || cleaned.startsWith("===") || cleaned.startsWith("***")) return true
        val lettersAndDigits = cleaned.count { it.isLetterOrDigit() }
        return lettersAndDigits < 2
    }

    // اكتشاف وسم الجودة تلقائياً وإضافته لاسم الباقة إذا كان السيرفر يخفيه
    fun enrichCategoryQualities(items: List<MediaItemModel>): List<MediaItemModel> {
        val byCat = items.groupBy { it.category }
        val catLabelMap = mutableMapOf<String, String>()

        for ((catName, chList) in byCat) {
            val upperCat = catName.uppercase()
            val hasExplicitTag = listOf("4K", "UHD", "FHD", "1080", "HD", "720", "SD", "LOW", "MINI", "HEVC", "RAW").any { upperCat.contains(it) }
            if (hasExplicitTag || chList.isEmpty()) {
                catLabelMap[catName] = catName
            } else {
                var c4k = 0; var cFhd = 0; var cHd = 0; var cSd = 0
                for (ch in chList.take(25)) {
                    val u = ch.name.uppercase()
                    when {
                        u.contains("4K") || u.contains("UHD") || u.contains("2160") -> c4k++
                        u.contains("FHD") || u.contains("1080") || u.contains("RAW") || u.contains("HEVC") -> cFhd++
                        u.contains("LOW") || u.contains("MINI") || u.contains("SD") || u.contains("480") -> cSd++
                        u.contains("HD") || u.contains("720") -> cHd++
                    }
                }
                val detected = when {
                    c4k > 0 && c4k >= cFhd && c4k >= cHd -> "4K"
                    cFhd > 0 && cFhd >= cHd && cFhd >= cSd -> "FHD"
                    cSd > 0 && cSd > cHd -> "SD / LOW"
                    cHd > 0 -> "HD"
                    else -> ""
                }
                catLabelMap[catName] = if (detected.isNotEmpty()) "$catName [$detected]" else catName
            }
        }
        return items.map { it.copy(category = catLabelMap[it.category] ?: it.category) }
    }

    // جلب القنوات، الأفلام، أو المسلسلات مع فلترة الفواصل وتوضيح جودات الباقات
    suspend fun loadXtream(host: String, user: String, pass: String, type: String): List<MediaItemModel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItemModel>()
        try {
            val base = host.trimEnd('/')
            val catAction = when (type) {
                "vod" -> "get_vod_categories"
                "series" -> "get_series_categories"
                else -> "get_live_categories"
            }
            val streamAction = when (type) {
                "vod" -> "get_vod_streams"
                "series" -> "get_series"
                else -> "get_live_streams"
            }

            val catMap = mutableMapOf<String, String>()
            val catArr = JSONArray(fetchJson("$base/player_api.php?username=$user&password=$pass&action=$catAction"))
            for (i in 0 until catArr.length()) {
                val c = catArr.getJSONObject(i)
                catMap[c.optString("category_id")] = c.optString("category_name", "عام").trim()
            }

            val streamArr = JSONArray(fetchJson("$base/player_api.php?username=$user&password=$pass&action=$streamAction"))
            for (i in 0 until streamArr.length()) {
                val s = streamArr.getJSONObject(i)
                val rawTitle = s.optString("name", "بدون اسم")
                if (isSeparatorChannel(rawTitle)) continue

                val id = if (type == "series") s.optString("series_id") else s.optString("stream_id")
                val cleanTitle = cleanMediaName(rawTitle)
                val catName = catMap[s.optString("category_id")] ?: "عام"
                val icon = if (type == "series") s.optString("cover") else s.optString("stream_icon")
                val ext = s.optString("container_extension", "mp4")

                val playUrl = when (type) {
                    "vod" -> "$base/movie/$user/$pass/$id.$ext"
                    "series" -> "series://$base|$user|$pass|$id"
                    else -> "$base/live/$user/$pass/$id.$streamFormat"
                }
                list.add(MediaItemModel(id, cleanTitle, playUrl, catName, icon, type))
            }
        } catch (_: Exception) {}
        if (type == "live") enrichCategoryQualities(list) else list
    }

    // جلب المواسم والحلقات الكاملة لأي مسلسل (Series Seasons & Episodes)
    suspend fun getSeriesDetails(seriesPseudoUrl: String): SeriesDetails? = withContext(Dispatchers.IO) {
        try {
            val parts = seriesPseudoUrl.removePrefix("series://").split("|")
            if (parts.size < 4) return@withContext null
            val (base, user, pass, seriesId) = parts

            val root = JSONObject(fetchJson("$base/player_api.php?username=$user&password=$pass&action=get_series_info&series_id=$seriesId"))
            val info = root.optJSONObject("info")
            val title = cleanMediaName(info?.optString("name", "مسلسل") ?: "مسلسل")
            val plot = info?.optString("plot", "لا يوجد وصف متاح حالياً.") ?: ""
            val rating = info?.optString("rating", "8.0") ?: "8.0"
            val cover = info?.optString("cover", "") ?: ""

            val episodesObj = root.optJSONObject("episodes") ?: return@withContext null
            val seasonsMap = sortedMapOf<Int, List<EpisodeModel>>()

            val keys = episodesObj.keys()
            while (keys.hasNext()) {
                val seasonKey = keys.next()
                val seasonNum = seasonKey.toIntOrNull() ?: 1
                val epArr = episodesObj.getJSONArray(seasonKey)
                val epList = mutableListOf<EpisodeModel>()
                for (i in 0 until epArr.length()) {
                    val ep = epArr.getJSONObject(i)
                    val epId = ep.optString("id")
                    val epNum = ep.optInt("episode_num", i + 1)
                    val epTitle = cleanMediaName(ep.optString("title", "الحلقة $epNum"))
                    val ext = ep.optString("container_extension", "mp4")
                    val epInfo = ep.optJSONObject("info")
                    val dur = epInfo?.optString("duration", "") ?: ""
                    val epPlot = epInfo?.optString("plot", "") ?: ""
                    val playUrl = "$base/series/$user/$pass/$epId.$ext"
                    epList.add(EpisodeModel(epId, epNum, epTitle, playUrl, dur, epPlot))
                }
                seasonsMap[seasonNum] = epList.sortedBy { it.episodeNum }
            }
            SeriesDetails(title, plot, rating, cover, seasonsMap)
        } catch (_: Exception) {
            null
        }
    }

    // جلب دليل البرامج المباشر (Live EPG) لقناة معينة
    suspend fun getShortEpg(host: String, user: String, pass: String, streamId: String): EpgProgram? = withContext(Dispatchers.IO) {
        try {
            val base = host.trimEnd('/')
            val root = JSONObject(fetchJson("$base/player_api.php?username=$user&password=$pass&action=get_short_epg&stream_id=$streamId&limit=2"))
            val listings = root.optJSONArray("epg_listings") ?: return@withContext null
            if (listings.length() == 0) return@withContext null

            fun decodeTitle(raw: String): String {
                return try {
                    String(Base64.decode(raw, Base64.DEFAULT), Charsets.UTF_8)
                } catch (_: Exception) {
                    raw
                }
            }

            val nowObj = listings.getJSONObject(0)
            val nowTitle = decodeTitle(nowObj.optString("title", ""))
            val nextTitle = if (listings.length() > 1) decodeTitle(listings.getJSONObject(1).optString("title", "")) else ""
            if (nowTitle.isBlank()) null else EpgProgram(nowTitle, nextTitle)
        } catch (_: Exception) {
            null
        }
    }

    // Stalker Portal
    suspend fun loadStalker(portalUrl: String, mac: String): List<MediaItemModel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItemModel>()
        try {
            val base = portalUrl.trimEnd('/').removeSuffix("/c")
            val headers = mapOf("Cookie" to "mac=${URLEncoder.encode(mac, "UTF-8")}; stb_lang=en; timezone=GMT")
            val token = JSONObject(fetchJson("$base/portal.php?type=stb&action=handshake&token=&JsHttpRequest=1-xml", headers))
                .getJSONObject("js").getString("token")
            val authHeaders = headers + ("Authorization" to "Bearer $token")

            val chArr = JSONObject(fetchJson("$base/portal.php?type=itv&action=get_all_channels&JsHttpRequest=1-xml", authHeaders))
                .getJSONObject("js").getJSONArray("data")
            for (i in 0 until chArr.length()) {
                val ch = chArr.getJSONObject(i)
                val rawTitle = ch.optString("name", "قناة")
                if (isSeparatorChannel(rawTitle)) continue
                val cmd = ch.optString("cmd").removePrefix("ffmpeg ")
                if (cmd.isNotBlank()) {
                    list.add(MediaItemModel(ch.optString("id"), cleanMediaName(rawTitle), cmd, "Stalker Live", ch.optString("logo"), "live"))
                }
            }
        } catch (_: Exception) {}
        list
    }

    // تنظيف أسماء الأفلام والقنوات من الرموز المزعجة
    fun cleanMediaName(raw: String): String {
        return raw
            .replace(Regex("^[#|*\\-\\[\\(=]+\\s*"), "")
            .replace(Regex("\\s*[#|*\\-=]+$"), "")
            .replace(Regex("^(AR|EN|FR|TR|KU|VIP|4K|FHD|HD|SD)\\s*[:|\\-]\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    // اكتشاف لغة/تصنيف الفيلم أو المسلسل تلقائياً
    fun detectContentLanguage(item: MediaItemModel): String {
        val text = "${item.category} ${item.name}".lowercase()
        val hasArabicLetters = item.name.any { it in '\u0600'..'\u06FF' }
        return when {
            text.contains("anime") || text.contains("cartoon") || text.contains("kids") || text.contains("انمي") || text.contains("أنمي") || text.contains("كرتون") || text.contains("أطفال") -> "أنمي وكرتون"
            text.contains("korea") || text.contains("asian") || text.contains("كوري") || text.contains("اسيوي") || text.contains("آسيوي") -> "كوري وآسيوي"
            text.contains("arab") || text.contains("arabic") || text.contains("عرب") || text.contains("مصري") || text.contains("خليجي") || text.contains("سوري") || text.contains("عراقي") || hasArabicLetters -> "عربي"
            else -> "أجنبي مترجم"
        }
    }

    // الترتيب الطبيعي للأرقام (Channel 1 قبل Channel 2 وقبل Channel 10)
    private fun naturalCompare(a: String, b: String): Int {
        val regex = Regex("(\\d+|\\D+)")
        val partsA = regex.findAll(a.lowercase()).map { it.value }.toList()
        val partsB = regex.findAll(b.lowercase()).map { it.value }.toList()
        val len = minOf(partsA.size, partsB.size)
        for (i in 0 until len) {
            val pA = partsA[i]
            val pB = partsB[i]
            val numA = pA.toLongOrNull()
            val numB = pB.toLongOrNull()
            val cmp = if (numA != null && numB != null) {
                numA.compareTo(numB)
            } else {
                pA.compareTo(pB)
            }
            if (cmp != 0) return cmp
        }
        return partsA.size.compareTo(partsB.size)
    }

    // محرك تجميع الجودات الموحد والترتيب النظيف لأي قائمة قنوات (مباشر + رياضة)
    fun groupChannelsByQuality(channels: List<MediaItemModel>, markLiveEvents: Boolean = false): List<GroupedSportsChannel> {
        val groupedMap = linkedMapOf<String, MutableList<QualityVariant>>()

        for (ch in channels) {
            if (isSeparatorChannel(ch.name)) continue
            val upper = "${ch.name} ${ch.category}".uppercase()
            val (qTag, prio) = when {
                upper.contains("4K") || upper.contains("UHD") || upper.contains("2160") -> Pair("4K UHD", 1)
                upper.contains("FHD") || upper.contains("1080") || upper.contains("RAW") || upper.contains("HEVC") -> Pair("1080p FHD", 2)
                upper.contains("LOW") || upper.contains("MINI") || upper.contains("SD") || upper.contains("480") -> Pair("SD / LOW (للنت الضعيف)", 4)
                upper.contains("HD") || upper.contains("720") -> Pair("720p HD", 3)
                else -> Pair("بث قياسي / احتياطي", 5)
            }

            val baseName = ch.name
                .replace(Regex("\\b(4K|UHD|FHD|1080P|720P|HD|SD|LOW|MINI|HEVC|H265|RAW|VIP|BACKUP|VEGA|MULTI|SOURCE|S1|S2)\\b", RegexOption.IGNORE_CASE), "")
                .replace(Regex("[\\[\\]\\(\\)\\|\\-_:#*]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .ifBlank { ch.name }

            val key = baseName.lowercase()
            val list = groupedMap.getOrPut(key) { mutableListOf() }
            list.add(QualityVariant(qTag, prio, "${ch.name} (${ch.category})", ch))
        }

        val result = groupedMap.values.map { variants ->
            val sorted = variants.sortedBy { it.priority }
            val first = sorted.first().item
            val displayBase = first.name
                .replace(Regex("\\b(4K|UHD|FHD|1080P|720P|HD|SD|LOW|MINI|HEVC|H265|RAW|VIP|BACKUP|VEGA|MULTI|SOURCE|S1|S2)\\b", RegexOption.IGNORE_CASE), "")
                .replace(Regex("[\\[\\]\\(\\)\\|\\-_:#*]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .ifBlank { first.name }

            val combinedText = "${first.category} ${first.name}".lowercase()
            val isEvent = markLiveEvents && (
                combinedText.contains(" vs ") || combinedText.contains(" x ") ||
                combinedText.contains("event") || combinedText.contains("match") ||
                combinedText.contains("مبار") || combinedText.contains("نقل") ||
                combinedText.contains("ppv") || combinedText.contains("live event") ||
                Regex("\\b\\d{1,2}:\\d{2}\\b").containsMatchIn(first.name)
            )

            GroupedSportsChannel(
                cleanName = displayBase,
                category = first.category,
                logo = sorted.firstOrNull { it.item.logo.isNotBlank() }?.item?.logo ?: "",
                variants = sorted,
                isLiveEvent = isEvent
            )
        }

        return result.sortedWith { a, b -> naturalCompare(a.cleanName, b.cleanName) }
    }

    // فلترة القنوات الرياضية الحقيقية فقط (استبعاد الأفلام والمسلسلات والترفيه)
    fun groupSportsChannels(allChannels: List<MediaItemModel>): List<GroupedSportsChannel> {
        val excludeKeywords = listOf(
            "movie", "series", "drama", "action", "cinema", "entertainment", "kids", "cartoon", "news", "doc", "music", "box office", "star ",
            "افلام", "أفلام", "مسلسل", "دراما", "سينما", "ترفيه", "اطفال", "أطفال", "كرتون", "اخبار", "أخبار", "وثائق"
        )
        val sportsKeywords = listOf(
            "sport", "bein sport", "ssc", "alkass", "kass", "ad sport", "abu dhabi sport", "alrabiaa", "sky sport", "dazn", "espn", "arena sport", "super sport", "tnt sport", "euro sport", "eurosport",
            "رياض", "الكأس", "السعودية الرياضية", "أبو ظبي الرياضية", "الرابعة الرياضية", "مبار", "كأس", "دوري"
        )

        val sportsOnly = allChannels.filter { ch ->
            if (isSeparatorChannel(ch.name)) return@filter false
            val combined = "${ch.category} ${ch.name}".lowercase()
            val isExcluded = excludeKeywords.any { combined.contains(it) }
            val isSport = sportsKeywords.any { combined.contains(it) } ||
                (combined.contains("bein") && !isExcluded)
            !isExcluded && isSport
        }

        return groupChannelsByQuality(sportsOnly, markLiveEvents = true)
    }
}
// END_ENGINE
