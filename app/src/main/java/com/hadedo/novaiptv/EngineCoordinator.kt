package com.hadedo.novaiptv

import java.util.Locale

object EngineCoordinator {
    fun resolveLiveStreamUrls(
        primaryUrl: String,
        fallbackUrl: String,
        formatMode: Any?
    ): Pair<String, String> {
        val cleanPrimary = primaryUrl.trim()
        val cleanFallback = fallbackUrl.trim()
        val modeName = formatMode?.toString()?.uppercase(Locale.US).orEmpty()

        fun adjustFormat(url: String): String {
            if (url.isBlank()) return ""
            return when {
                modeName.contains("HLS") || modeName.contains("M3U8") -> {
                    if (url.endsWith(".ts", ignoreCase = true)) url.dropLast(3) + ".m3u8" else url
                }
                modeName.contains("TS") && !modeName.contains("AUTO") -> {
                    if (url.endsWith(".m3u8", ignoreCase = true)) url.dropLast(5) + ".ts" else url
                }
                else -> url
            }
        }

        return Pair(adjustFormat(cleanPrimary), adjustFormat(cleanFallback))
    }
}
