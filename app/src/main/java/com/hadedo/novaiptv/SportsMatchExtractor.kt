package com.devkan.vip.domain.sports

import com.devkan.vip.domain.model.SportsMatchInfo

object SportsMatchExtractor {

    private val teamAliases = mapOf(
        // الدوري الإنجليزي
        "مانشستر سيتي" to listOf("مان سيتي", "السيتي", "manchester city", "man city", "city"),
        "مانشستر يونايتد" to listOf("مان يونايتد", "اليونايتد", "manchester united", "man united", "man utd"),
        "ليفربول" to listOf("liverpool", "الريدز"),
        "تشيلسي" to listOf("chelsea"),
        "أرسنال" to listOf("arsenal", "الجنرز"),
        "توتنهام" to listOf("توتنهام هوتسبير", "tottenham", "spurs"),
        "نيوكاسل" to listOf("نيوكاسل يونايتد", "newcastle"),
        "أستون فيلا" to listOf("aston villa", "فيلا"),
        // الدوري السعودي
        "الهلال" to listOf("الهلال السعودي", "al hilal", "hilal"),
        "النصر" to listOf("النصر السعودي", "al nassr", "nassr"),
        "الاتحاد" to listOf("الاتحاد السعودي", "al ittihad", "ittihad"),
        "الأهلي" to listOf("الأهلي السعودي", "al ahli", "ahli"),
        "الشباب" to listOf("الشباب السعودي", "al shabab"),
        // إسبانيا
        "ريال مدريد" to listOf("الريال", "real madrid", "madrid"),
        "برشلونة" to listOf("البارسا", "barcelona", "barca", "barça"),
        "أتلتيكو مدريد" to listOf("أتلتيكو", "atletico madrid", "atletico"),
        // أوروبا
        "بايرن ميونخ" to listOf("بايرن", "bayern munich", "bayern"),
        "يوفنتوس" to listOf("اليوفي", "juventus", "juve"),
        "باريس سان جيرمان" to listOf("باريس", "psg", "paris saint germain"),
        "إنتر ميلان" to listOf("الإنتر", "inter milan", "inter"),
        "ميلان" to listOf("إيه سي ميلان", "ac milan", "milan"),
        // العراق والمنتخبات العربية
        "المنتخب العراقي" to listOf("العراق", "اسود الرافدين", "iraq"),
        "الشرطة" to listOf("نادي الشرطة", "الشرطه العراقي", "al shorta"),
        "القوة الجوية" to listOf("الجوية", "القوه الجويه", "al quwa al jawiya"),
        "الزوراء" to listOf("نادي الزوراء", "al zawraa"),
        "أربيل" to listOf("نادي اربيل", "erbil")
    )

    private val sportsKeywords = listOf(
        "مباراة", "مباراه", "دوري", "كأس", "كاس", "نهائي", "نصف نهائي", "تصفيات",
        "league", "cup", "final", "match", "derby", "ufc", "wwe", "f1", "grand prix", "nba", "tennis"
    )

    private val competitionRules = listOf(
        "دوري أبطال أوروبا" to listOf("دوري ابطال اوروبا", "champions league", "ucl"),
        "الدوري الإنجليزي" to listOf("الدوري الانجليزي", "premier league", "epl"),
        "الدوري الإسباني" to listOf("الدوري الاسباني", "la liga", "laliga"),
        "الدوري الإيطالي" to listOf("الدوري الايطالي", "serie a"),
        "الدوري الألماني" to listOf("الدوري الالماني", "bundesliga"),
        "الدوري الفرنسي" to listOf("الدوري الفرنسي", "ligue 1"),
        "دوري روشن السعودي" to listOf("الدوري السعودي", "دوري روشن", "saudi pro league"),
        "دوري نجوم العراق" to listOf("دوري نجوم العراق", "الدوري العراقي", "iraq stars league"),
        "دوري أبطال آسيا" to listOf("دوري ابطال اسيا", "afc champions"),
        "الدوري الأوروبي" to listOf("الدوري الاوروبي", "europa league"),
        "كأس العالم" to listOf("كاس العالم", "world cup")
    )

    // ====================== إعدادات MinHash + LSH ======================
    private const val NUM_HASHES = 64
    private const val NUM_BANDS = 16
    private const val ROWS_PER_BAND = NUM_HASHES / NUM_BANDS // 4
    private const val NGRAM_SIZE = 2
    private val hashSeeds: IntArray = IntArray(NUM_HASHES) { it * 0x9E3779B9.toInt() }

    private fun normalize(text: String): String {
        return text
            .trim()
            .lowercase()
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace(Regex("[أإآ]"), "ا")
            .replace("ؤ", "و")
            .replace("ئ", "ي")
            .replace(Regex("""[\u064B-\u065F]"""), "")
            .replace(Regex("""[^\w\s\u0600-\u06FF]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun extractNgrams(normalized: String): Set<String> {
        if (normalized.isEmpty()) return emptySet()
        if (normalized.length < NGRAM_SIZE) return setOf(normalized)
        return (0..normalized.length - NGRAM_SIZE)
            .map { normalized.substring(it, it + NGRAM_SIZE) }
            .toSet()
    }

    private fun computeMinHashSignature(normalized: String): IntArray {
        val ngrams = extractNgrams(normalized)
        val signature = IntArray(NUM_HASHES) { Int.MAX_VALUE }
        if (ngrams.isEmpty()) return signature

        for (ngram in ngrams) {
            val baseHash = ngram.hashCode()
            for (i in 0 until NUM_HASHES) {
                val combined = baseHash xor hashSeeds[i]
                val hashValue = combined * 0x85EBCA6B.toInt() ushr 16
                if (hashValue < signature[i]) {
                    signature[i] = hashValue
                }
            }
        }
        return signature
    }

    private fun minHashSimilarity(sig1: IntArray, sig2: IntArray): Double {
        var matches = 0
        for (i in sig1.indices) {
            if (sig1[i] == sig2[i]) matches++
        }
        return matches.toDouble() / NUM_HASHES
    }

    private data class TeamEntry(
        val officialName: String,
        val normalizedAlias: String,
        val signature: IntArray
    )

    private val exactLookupMap: Map<String, String> by lazy {
        val map = mutableMapOf<String, String>()
        teamAliases.forEach { (official, aliases) ->
            map[normalize(official)] = official
            aliases.forEach { alias -> map[normalize(alias)] = official }
        }
        map
    }

    private val sortedAliasesForContains: List<Pair<String, String>> by lazy {
        exactLookupMap.entries
            .filter { it.key.length >= 4 }
            .sortedByDescending { it.key.length }
            .map { it.key to it.value }
    }

    private val lshBuckets: Map<Long, List<TeamEntry>> by lazy {
        val buckets = mutableMapOf<Long, MutableList<TeamEntry>>()
        exactLookupMap.forEach { (normAlias, official) ->
            val signature = computeMinHashSignature(normAlias)
            val entry = TeamEntry(official, normAlias, signature)
            for (band in 0 until NUM_BANDS) {
                val key = computeBandKey(band, signature)
                buckets.getOrPut(key) { mutableListOf() }.add(entry)
            }
        }
        buckets
    }

    private fun computeBandKey(band: Int, signature: IntArray): Long {
        val start = band * ROWS_PER_BAND
        var bandHash = 0
        for (i in start until start + ROWS_PER_BAND) {
            bandHash = 31 * bandHash + signature[i]
        }
        return (band.toLong() shl 32) or (bandHash.toLong() and 0xFFFFFFFFL)
    }

    private data class MatchResult(val teamName: String, val score: Double, val aliasLength: Int)

    fun resolveTeamName(rawName: String): String? {
        val normalizedInput = normalize(rawName)
        if (normalizedInput.length < 2) return null

        // 1. تطابق تام O(1)
        exactLookupMap[normalizedInput]?.let { return it }

        // 2. احتواء ذكي
        sortedAliasesForContains.firstOrNull { (normAlias, _) ->
            normalizedInput.contains(normAlias)
        }?.let { return it.second }

        // 3. LSH + MinHash
        val inputSignature = computeMinHashSignature(normalizedInput)
        val candidates = mutableSetOf<TeamEntry>()
        for (band in 0 until NUM_BANDS) {
            val key = computeBandKey(band, inputSignature)
            lshBuckets[key]?.let { candidates.addAll(it) }
        }

        return candidates.mapNotNull { entry ->
            val similarity = minHashSimilarity(inputSignature, entry.signature)
            if (similarity >= 0.72) MatchResult(entry.officialName, similarity, entry.normalizedAlias.length) else null
        }
            .sortedWith(compareByDescending<MatchResult> { it.score }.thenByDescending { it.aliasLength })
            .firstOrNull()
            ?.teamName
    }

    fun isSportsProgram(category: String?, title: String, description: String?): Boolean {
        val cat = category?.lowercase().orEmpty()
        if (cat.contains("sport") || cat.contains("رياض") || cat.contains("bein") || cat.contains("ssc")) {
            return true
        }
        val normText = normalize("$title ${description.orEmpty()}")
        return sportsKeywords.any { normText.contains(normalize(it)) }
    }

    fun extractMatchInfo(title: String, description: String? = null, isCategorySports: Boolean = false): SportsMatchInfo? {
        val cleanTitle = cleanText(title)
        val patterns = mutableListOf(
            Regex("""(.+?)\s+(ضد|فى|في|vs\.?|VS|v\.?)\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""(.+?)\s+(at|@)\s+(.+)""", RegexOption.IGNORE_CASE)
        )
        // تقييد فاصل الشرطة (-) بالقنوات الرياضية لمنع التداخل مع نشرات الأخبار والأفلام
        if (isCategorySports || isSportsProgram(null, title, description)) {
            patterns.add(Regex("""(.+?)\s+[-–—]\s+(.+)"""))
        }

        for (pattern in patterns) {
            val matchResult = pattern.find(cleanTitle) ?: continue
            val groups = matchResult.groupValues
            val rawHome = groups[1].trim()
            val rawAway = groups.last().trim()

            val homeTeam = resolveTeamName(rawHome) ?: cleanTeamName(rawHome).takeIf { isValidTeamName(it) }
            val awayTeam = resolveTeamName(rawAway) ?: cleanTeamName(rawAway).takeIf { isValidTeamName(it) }

            if (homeTeam != null && awayTeam != null && homeTeam != awayTeam) {
                return SportsMatchInfo(
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    competition = extractCompetition(cleanTitle, description),
                    rawTitle = title
                )
            }
        }
        return null
    }

    fun extractCompetition(title: String, description: String?): String? {
        val fullText = normalize("$title ${description.orEmpty()}")
        return competitionRules.firstOrNull { (_, keywords) ->
            keywords.any { fullText.contains(it) }
        }?.first
    }

    private fun cleanText(text: String): String {
        return text
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""^(مباشر|live|حصري|exclusive|مباراة)\s*[:\-]?\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*[\(\[](hd|fhd|4k|hevc|1080p|720p|مباشر|live).*?[\)\]]""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun cleanTeamName(name: String): String {
        return name
            .replace(Regex("""[\(\[].*?[\)\]]"""), "")
            .replace(Regex("""\s+(HD|FHD|4K|HEVC|1080P|720P)""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun isValidTeamName(name: String): Boolean {
        return name.length in 3..45 && !name.matches(Regex("""^[\d\s\-:]+$"""))
    }
}
