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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class SavedServer(
    val id: String,
    val title: String,
    val type: String,
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
    val type: String = "live"
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
    val qualityTag: String,
    val priority: Int,
    val rawName: String,
    val item: MediaItemModel
)

data class GroupedSportsChannel(
    val cleanName: String,
    val category: String,
    val logo: String,
    val variants: List<QualityVariant>,
    val regionTag: String = "global" // "arab", "global"
)

data class LiveMatchInfo(
    val id: String,
    val leagueName: String,
    val leagueSlug: String,
    val homeTeam: String,
    val homeLogo: String,
    val homeScore: String,
    val awayTeam: String,
    val awayLogo: String,
    val awayScore: String,
    val statusState: String, // "in", "pre", "post"
    val statusText: String,
    val suggestedBroadcaster: String
)

data class EpgProgram(
    val nowTitle: String,
    val nextTitle: String
)

object ServersEngine {
    var currentUserAgent: String = "IPTVSmartersPro"
    var streamFormat: String = "ts"
    var sameHostProxy: Boolean = false

    private val QUALITY_REGEX = Regex("\\b(4K|UHD|FHD|1080P|720P|HD|SD|LOW|MINI|HEVC|H265|RAW|VIP|BACKUP|VEGA|MULTI|SOURCE|S1|S2)\\b", RegexOption.IGNORE_CASE)
    private val SYMBOLS_REGEX = Regex("[\\[\\]\\(\\)\\|\\-_:#*]+")
    private val SPACES_REGEX = Regex("\\s+")
    private val PREFIX_CLEAN_REGEX = Regex("^(AR|EN|FR|TR|KU|VIP|4K|FHD|HD|SD)\\s*[:|\\-]\\s*", RegexOption.IGNORE_CASE)
    private val NATURAL_SPLIT_REGEX = Regex("(\\d+|\\D+)")

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

    // جلب مباريات اليوم الحية من ESPN لجميع البطولات العالمية والعربية
    suspend fun fetchTodayMatches(lang: String = "ar"): List<LiveMatchInfo> = withContext(Dispatchers.IO) {
        val matches = ArrayList<LiveMatchInfo>()
        try {
            val raw = fetchJson(
                "https://site.api.espn.com/apis/site/v2/sports/soccer/all/scoreboard",
                mapOf("User-Agent" to "Mozilla/5.0")
            )
            val root = JSONObject(raw)
            val events = root.optJSONArray("events") ?: return@withContext emptyList()

            val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val localTimeFmt = SimpleDateFormat("hh:mm a", Locale.ENGLISH)

            for (i in 0 until events.length()) {
                val ev = events.getJSONObject(i)
                val id = ev.optString("id")
                val season = ev.optJSONObject("season")
                val leagueSlug = season?.optString("slug", "") ?: ""
                val rawLeague = ev.optJSONArray("leagues")?.optJSONObject(0)?.optString("name", "")
                    ?: leagueSlug.uppercase()

                val leagueTitle = translateLeagueName(rawLeague, leagueSlug, lang)
                val broadcaster = detectBroadcasterHint(leagueSlug, rawLeague, lang)

                val comp = ev.optJSONArray("competitions")?.optJSONObject(0) ?: continue
                val competitors = comp.optJSONArray("competitors") ?: continue
                if (competitors.length() < 2) continue

                var homeName = ""; var homeLogo = ""; var homeScore = "0"
                var awayName = ""; var awayLogo = ""; var awayScore = "0"

                for (c in 0 until competitors.length()) {
                    val teamObj = competitors.getJSONObject(c)
                    val info = teamObj.optJSONObject("team")
                    val tName = info?.optString("shortDisplayName", info.optString("displayName", "")) ?: ""
                    val tLogo = info?.optString("logo", "") ?: ""
                    val tScore = teamObj.optString("score", "0")
                    if (teamObj.optString("homeAway") == "home") {
                        homeName = tName; homeLogo = tLogo; homeScore = tScore
                    } else {
                        awayName = tName; awayLogo = tLogo; awayScore = tScore
                    }
                }

                val statusObj = comp.optJSONObject("status")
                val typeObj = statusObj?.optJSONObject("type")
                val state = typeObj?.optString("state", "pre") ?: "pre"
                val clock = statusObj?.optString("displayClock", "") ?: ""

                val statusText = when (state) {
                    "in" -> if (lang == "ar") "🔴 مباشر $clock" else "🔴 LIVE $clock"
                    "post" -> if (lang == "ar") "انتهت" else "FT"
                    else -> {
                        val rawDate = ev.optString("date", "")
                        val formattedTime = try {
                            val d = isoParser.parse(rawDate)
                            if (d != null) localTimeFmt.format(d) else typeObj?.optString("shortDetail", "") ?: ""
                        } catch (_: Exception) {
                            typeObj?.optString("shortDetail", "") ?: ""
                        }
                        if (lang == "ar") "الساعة $formattedTime" else formattedTime
                    }
                }

                matches.add(
                    LiveMatchInfo(
                        id = id,
                        leagueName = leagueTitle,
                        leagueSlug = leagueSlug,
                        homeTeam = homeName,
                        homeLogo = homeLogo,
                        homeScore = homeScore,
                        awayTeam = awayName,
                        awayLogo = awayLogo,
                        awayScore = awayScore,
                        statusState = state,
                        statusText = statusText,
                        suggestedBroadcaster = broadcaster
                    )
                )
            }
        } catch (_: Exception) {}

        // ترتيب: المباريات الجارية حالياً أولاً، ثم القادمة، ثم المنتهية
        matches.sortedBy {
            when (it.statusState) {
                "in" -> 0
                "pre" -> 1
                else -> 2
            }
        }
    }

    private fun translateLeagueName(raw: String, slug: String, lang: String): String {
        if (lang != "ar") return raw.ifBlank { slug.uppercase() }
        val s = "${raw.lowercase()} ${slug.lowercase()}"
        return when {
            s.contains("uefa.champions") || s.contains("champions league") -> "دوري أبطال أوروبا"
            s.contains("uefa.europa") || s.contains("europa league") -> "الدوري الأوروبي"
            s.contains("eng.1") || s.contains("premier league") -> "الدوري الإنجليزي الممتاز"
            s.contains("esp.1") || s.contains("laliga") -> "الدوري الإسباني"
            s.contains("ita.1") || s.contains("serie a") -> "الدوري الإيطالي"
            s.contains("ger.1") || s.contains("bundesliga") -> "الدوري الألماني"
            s.contains("fra.1") || s.contains("ligue 1") -> "الدوري الفرنسي"
            s.contains("ksa.1") || s.contains("saudi") -> "دوري روشن السعودي"
            s.contains("afc.champions") -> "دوري أبطال آسيا"
            s.contains("fifa.world") || s.contains("world cup") -> "تصفيات كأس العالم"
            s.contains("uefa.nations") -> "دوري الأمم الأوروبية"
            else -> raw.ifBlank { slug.uppercase() }
        }
    }

    private fun detectBroadcasterHint(slug: String, raw: String, lang: String): String {
        val s = "${slug.lowercase()} ${raw.lowercase()}"
        return if (lang == "ar") {
            when {
                s.contains("ksa") || s.contains("saudi") -> "SSC Sports"
                s.contains("ita.") || s.contains("serie a") -> "AD Sports / StarzPlay"
                s.contains("esp.") || s.contains("eng.") || s.contains("uefa") || s.contains("fra.") || s.contains("ger.") || s.contains("afc") -> "beIN Sports"
                else -> "beIN / SSC / الكأس"
            }
        } else {
            when {
                s.contains("eng.") -> "Sky Sports / TNT"
                s.contains("esp.") -> "Movistar / ESPN"
                s.contains("ita.") -> "DAZN / TNT"
                s.contains("uefa") -> "TNT / Canal+ / beIN"
                else -> "Sports Live"
            }
        }
    }

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

    suspend fun getAccountInfo(host: String, user: String, pass: String): AccountInfo? = withContext(Dispatchers.IO) {
        try {
            val base = host.trimEnd('/')
            val infoObj = JSONObject(fetchJson("$base/player_api.php?username=$user&password=$pass")).getJSONObject("user_info")
            val expTimestamp = infoObj.optString("exp_date", "0").toLongOrNull() ?: 0L
            val expFormatted = if (expTimestamp > 0) {
                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date(expTimestamp * 1000))
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

    fun isSeparatorChannel(name: String): Boolean {
        val cleaned = name.trim()
        if (cleaned.length < 2) return true
        val c0 = cleaned[0]
        if (c0 == '#' || c0 == '-' || c0 == '=' || c0 == '*') {
            if (cleaned.startsWith("###") || cleaned.startsWith("---") || cleaned.startsWith("===") || cleaned.startsWith("***")) return true
        }
        var count = 0
        for (ch in cleaned) {
            if (ch.isLetterOrDigit()) {
                count++
                if (count >= 2) return false
            }
        }
        return true
    }

    fun enrichCategoryQualities(items: List<MediaItemModel>): List<MediaItemModel> {
        val byCat = items.groupBy { it.category }
        val catLabelMap = HashMap<String, String>(byCat.size)

        for ((catName, chList) in byCat) {
            val upperCat = catName.uppercase()
            val hasExplicitTag = upperCat.contains("4K") || upperCat.contains("UHD") || upperCat.contains("FHD") ||
                upperCat.contains("1080") || upperCat.contains("HD") || upperCat.contains("720") ||
                upperCat.contains("SD") || upperCat.contains("LOW") || upperCat.contains("MINI") || upperCat.contains("HEVC")
            if (hasExplicitTag || chList.isEmpty()) {
                catLabelMap[catName] = catName
            } else {
                var c4k = 0; var cFhd = 0; var cHd = 0; var cSd = 0
                val sampleSize = minOf(20, chList.size)
                for (i in 0 until sampleSize) {
                    val u = chList[i].name.uppercase()
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

    suspend fun loadXtream(host: String, user: String, pass: String, type: String): List<MediaItemModel> = withContext(Dispatchers.IO) {
        val list = ArrayList<MediaItemModel>()
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

            val catMap = HashMap<String, String>()
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

    fun cleanMediaName(raw: String): String {
        return raw
            .trim('#', '|', '*', '-', '[', ']', '(', ')', '=', ' ')
            .replace(PREFIX_CLEAN_REGEX, "")
            .trim()
    }

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

    private fun naturalCompare(a: String, b: String): Int {
        val partsA = NATURAL_SPLIT_REGEX.findAll(a.lowercase()).map { it.value }.toList()
        val partsB = NATURAL_SPLIT_REGEX.findAll(b.lowercase()).map { it.value }.toList()
        val len = minOf(partsA.size, partsB.size)
        for (i in 0 until len) {
            val pA = partsA[i]
            val pB = partsB[i]
            val numA = pA.toLongOrNull()
            val numB = pB.toLongOrNull()
            val cmp = if (numA != null && numB != null) numA.compareTo(numB) else pA.compareTo(pB)
            if (cmp != 0) return cmp
        }
        return partsA.size.compareTo(partsB.size)
    }

    private fun stripQualityWords(name: String): String {
        return name
            .replace(QUALITY_REGEX, "")
            .replace(SYMBOLS_REGEX, " ")
            .replace(SPACES_REGEX, " ")
            .trim()
            .ifBlank { name }
    }

    fun groupChannelsByQuality(channels: List<MediaItemModel>): List<GroupedSportsChannel> {
        val groupedMap = LinkedHashMap<String, MutableList<QualityVariant>>()

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

            val baseName = stripQualityWords(ch.name)
            val key = baseName.lowercase()
            val list = groupedMap.getOrPut(key) { ArrayList(4) }
            list.add(QualityVariant(qTag, prio, "${ch.name} (${ch.category})", ch))
        }

        val result = groupedMap.values.map { variants ->
            val sorted = variants.sortedBy { it.priority }
            val first = sorted[0].item
            val displayBase = stripQualityWords(first.name)
            val combined = "${first.category} ${first.name}".lowercase()
            val isArab = combined.contains("ar") || combined.contains("arab") || combined.contains("bein") ||
                combined.contains("ssc") || combined.contains("kass") || combined.contains("alrabiaa") ||
                combined.contains("ad sport") || first.name.any { it in '\u0600'..'\u06FF' }

            GroupedSportsChannel(
                cleanName = displayBase,
                category = first.category,
                logo = sorted.firstOrNull { it.item.logo.isNotBlank() }?.item?.logo ?: "",
                variants = sorted,
                regionTag = if (isArab) "arab" else "global"
            )
        }

        return result.sortedWith { a, b -> naturalCompare(a.cleanName, b.cleanName) }
    }

    // تصنيف القنوات الرياضية العربية والعالمية الكبرى بشكل نظيف دون قنوات عشوائية
    fun groupSportsChannels(allChannels: List<MediaItemModel>): List<GroupedSportsChannel> {
        val excludeKeywords = listOf(
            "movie", "series", "drama", "action", "cinema", "entertainment", "kids", "cartoon", "news", "doc", "music", "box office", "star ",
            "افلام", "أفلام", "مسلسل", "دراما", "سينما", "ترفيه", "اطفال", "أطفال", "كرتون", "اخبار", "أخبار", "وثائق"
        )
        val majorSportsNetworks = listOf(
            "bein sport", "bein", "ssc", "alkass", "kass", "ad sport", "abu dhabi sport", "alrabiaa", "dubai sport", "on time", "ontime",
            "sky sport", "dazn", "tnt sport", "bt sport", "espn", "canal+ sport", "movistar", "eurosport", "arena sport", "super sport",
            "بين سبورت", "بي ان", "الكأس", "الكاس", "السعودية الرياضية", "أبو ظبي الرياضية", "الرابعة", "دبي الرياضية", "اون تايم"
        )

        val sportsOnly = allChannels.filter { ch ->
            if (isSeparatorChannel(ch.name)) return@filter false
            val combined = "${ch.category} ${ch.name}".lowercase()
            if (excludeKeywords.any { combined.contains(it) }) return@filter false
            majorSportsNetworks.any { combined.contains(it) }
        }

        val grouped = groupChannelsByQuality(sportsOnly)
        return grouped.sortedWith { a, b ->
            val rankA = when {
                a.cleanName.contains("bein", true) -> 1
                a.cleanName.contains("ssc", true) -> 2
                a.cleanName.contains("kass", true) || a.cleanName.contains("الكأس") -> 3
                a.regionTag == "arab" -> 4
                else -> 5
            }
            val rankB = when {
                b.cleanName.contains("bein", true) -> 1
                b.cleanName.contains("ssc", true) -> 2
                b.cleanName.contains("kass", true) || b.cleanName.contains("الكأس") -> 3
                b.regionTag == "arab" -> 4
                else -> 5
            }
            if (rankA != rankB) rankA.compareTo(rankB) else naturalCompare(a.cleanName, b.cleanName)
        }
    }
}
// END_ENGINE
