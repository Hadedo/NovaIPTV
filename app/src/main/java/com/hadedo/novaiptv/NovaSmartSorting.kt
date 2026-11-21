package com.hadedo.novaiptv

import java.util.Locale

enum class ContentBucket {
    APP_LANG_SPORTS,
    APP_LANG_STANDARD,
    OTHER_SPORTS,
    OTHER_STANDARD
}

object NovaSmartSorting {

    private val arabicTokens = listOf(
        "ar", "arab", "arabic", "عربي", "عربيه", "العربية", "العربيه",
        "خليج", "الخليج", "عراق", "العراق", "مصر", "المصر", "شام", "الشام",
        "مغرب", "المغرب", "سعود", "السعود", "امارات", "الإمارات", "قطر", "كويت", "اردن", "الأردن", "لبنان"
    )

    private val sportsTokens = listOf(
        "sport", "sports", "رياضة", "رياضه", "رياضي", "bein", "beinsport", "ssport",
        "الكأس", "الكاس", "abu dhabi sport", "dubai sport", "arena sport", "sky sport",
        "bt sport", "tnt sport", "dazn", "espn", "ufc", "كرة", "ملاعب", "بطولات"
    )

    private val translatedTokens = listOf(
        "مترجم", "مترجمه", "مترجمة", "مدبلج", "مدبلجه", "مدبلجة",
        "sub", "subbed", "arabic sub", "ar sub", "dub", "dubbed", "ar dub"
    )

    private val offTopicVodTokens = listOf(
        "ufc", "wwe", "wrestling", "مصارعة", "نزال", "fight night"
    )

    fun matchesAppLanguage(title: String, lang: String): Boolean {
        val norm = title.lowercase(Locale.ROOT).trim()
        if (norm.isEmpty()) return false
        return when (lang.lowercase(Locale.ROOT)) {
            "ar" -> {
                val hasArabicScript = norm.any { ch -> ch in '\u0600'..'\u06FF' }
                val hasArPrefixOrTag = Regex("""(^|[^a-z0-9])(ar|arab|arabic)([^a-z0-9]|$)""").containsMatchIn(norm)
                val hasArKeyword = arabicTokens.any { token -> norm.contains(token) }
                hasArabicScript || hasArPrefixOrTag || hasArKeyword
            }
            "en" -> Regex("""(^|[^a-z0-9])(en|eng|english|uk|us|usa)([^a-z0-9]|$)""").containsMatchIn(norm)
            "tr" -> Regex("""(^|[^a-z0-9])(tr|turk|turkish|تركية|تركي)([^a-z0-9]|$)""").containsMatchIn(norm)
            "de" -> Regex("""(^|[^a-z0-9])(de|ger|german|deutsch)([^a-z0-9]|$)""").containsMatchIn(norm)
            "fr" -> Regex("""(^|[^a-z0-9])(fr|fra|french|فرنسي|فرنسية)([^a-z0-9]|$)""").containsMatchIn(norm)
            else -> false
        }
    }

    fun isSportsTitle(title: String): Boolean {
        val norm = title.lowercase(Locale.ROOT).trim()
        if (norm.isEmpty()) return false
        return sportsTokens.any { token -> norm.contains(token) }
    }

    fun classifyBucket(title: String, lang: String): ContentBucket {
        val isLang = matchesAppLanguage(title, lang)
        val isSport = isSportsTitle(title)
        return when {
            isLang && isSport -> ContentBucket.APP_LANG_SPORTS
            isLang && !isSport -> ContentBucket.APP_LANG_STANDARD
            !isLang && isSport -> ContentBucket.OTHER_SPORTS
            else -> ContentBucket.OTHER_STANDARD
        }
    }

    fun <T> sortBySmartPriority(
        items: List<T>,
        lang: String,
        titleSelector: (T) -> String
    ): List<T> {
        return items.sortedWith(
            compareBy<T> { classifyBucket(titleSelector(it), lang).ordinal }
                .thenBy { titleSelector(it).lowercase(Locale.ROOT).trim() }
        )
    }

    fun hasExplicitTranslationTag(title: String, categoryHint: String = ""): Boolean {
        val combined = "$title $categoryHint".lowercase(Locale.ROOT)
        return translatedTokens.any { token -> combined.contains(token) }
    }

    fun looksLikeOffTopicVod(title: String): Boolean {
        val norm = title.lowercase(Locale.ROOT)
        return offTopicVodTokens.any { token -> norm.contains(token) }
    }

    fun <T> sortVodOrSeriesSmartly(
        items: List<T>,
        lang: String,
        titleSelector: (T) -> String,
        categorySelector: (T) -> String = { "" }
    ): List<T> {
        return items.sortedWith(
            compareBy<T> { item ->
                val title = titleSelector(item)
                val cat = categorySelector(item)
                when {
                    looksLikeOffTopicVod(title) -> 4
                    matchesAppLanguage(title, lang) || matchesAppLanguage(cat, lang) -> 0
                    hasExplicitTranslationTag(title, cat) -> 1
                    title.isNotBlank() -> 2
                    else -> 3
                }
            }.thenBy { titleSelector(it).lowercase(Locale.ROOT).trim() }
        )
    }

    fun computeProgressFraction(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L || positionMs <= 0L) return 0f
        return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }
}
