package com.devkan.vip.domain.sports

import android.util.Base64
import com.devkan.vip.domain.model.ClassifiedSportsData
import com.devkan.vip.domain.model.EpgProgram
import com.devkan.vip.domain.model.GroupedSportsMatch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object EpgSportsPipeline {

    private const val TWENTY_FOUR_HOURS_MS = 24L * 60L * 60L * 1000L

    /**
     * يُستدعى مرة واحدة عند مزامنة الـ EPG في الخلفية قبل التخزين في Room
     */
    fun enrichProgramForStorage(raw: EpgProgram): EpgProgram {
        val decodedTitle = smartDecodeBase64(raw.title)
        val decodedDesc = raw.description?.let { smartDecodeBase64(it) }

        val sportsFlag = SportsMatchExtractor.isSportsProgram(raw.category, decodedTitle, decodedDesc)
        if (!sportsFlag) {
            return raw.copy(title = decodedTitle, description = decodedDesc, isSports = false)
        }

        val matchInfo = SportsMatchExtractor.extractMatchInfo(
            title = decodedTitle,
            description = decodedDesc,
            isCategorySports = true
        )

        return raw.copy(
            title = decodedTitle,
            description = decodedDesc,
            isSports = true,
            homeTeam = matchInfo?.homeTeam,
            awayTeam = matchInfo?.awayTeam,
            competition = matchInfo?.competition ?: SportsMatchExtractor.extractCompetition(decodedTitle, decodedDesc)
        )
    }

    /**
     * مرور واحد (Single-Pass) لتصنيف البرامج الرياضية وتجميع القنوات المكررة لنفس المباراة
     */
    fun classifyAndGroupSports(
        programs: List<EpgProgram>,
        now: Long = System.currentTimeMillis()
    ): ClassifiedSportsData {
        val liveSports = mutableListOf<EpgProgram>()
        val upcoming24h = mutableListOf<EpgProgram>()
        val upcomingLater = mutableListOf<EpgProgram>()
        val allLive = mutableListOf<EpgProgram>()

        for (program in programs) {
            if (program.endTime <= now) continue // تجاهل المنتهية فوراً

            val isCurrentlyLive = now in program.startTime until program.endTime
            if (isCurrentlyLive) {
                allLive.add(program)
            }

            if (!program.isSports) continue

            if (isCurrentlyLive) {
                liveSports.add(program.copy(isLive = true))
            } else if (program.startTime > now) {
                if (program.startTime - now <= TWENTY_FOUR_HOURS_MS) {
                    upcoming24h.add(program)
                } else {
                    upcomingLater.add(program)
                }
            }
        }

        return ClassifiedSportsData(
            liveMatches = groupDuplicateMatches(liveSports, isLive = true),
            upcoming24hMatches = groupDuplicateMatches(upcoming24h.sortedBy { it.startTime }, isLive = false),
            upcomingLaterMatches = groupDuplicateMatches(upcomingLater.sortedBy { it.startTime }, isLive = false).take(30),
            livePrograms = allLive
        )
    }

    private fun groupDuplicateMatches(programs: List<EpgProgram>, isLive: Boolean): List<GroupedSportsMatch> {
        val validMatches = programs.filter { !it.homeTeam.isNullOrBlank() && !it.awayTeam.isNullOrBlank() }

        return validMatches
            .groupBy { "${it.homeTeam}_${it.awayTeam}_${it.startTime / (30 * 60 * 1000)}" } // تجميع نفس المباراة
            .map { (key, group) ->
                val sortedStreams = group.sortedByDescending { qualityScore(it.streamQuality, it.channelName) }
                val first = sortedStreams.first()
                GroupedSportsMatch(
                    matchKey = key,
                    homeTeam = first.homeTeam!!,
                    awayTeam = first.awayTeam!!,
                    competition = first.competition,
                    startTime = first.startTime,
                    endTime = first.endTime,
                    isLive = isLive,
                    streams = sortedStreams
                )
            }
    }

    private fun qualityScore(quality: String?, channelName: String): Int {
        val text = "${quality.orEmpty()} $channelName".uppercase()
        return when {
            text.contains("4K") || text.contains("UHD") -> 4
            text.contains("FHD") || text.contains("1080") -> 3
            text.contains("HD") || text.contains("720") -> 2
            else -> 1
        }
    }

    fun parseXmltvTimestamp(raw: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
            sdf.parse(raw.trim())?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    fun resolveXtreamTimestamp(unixSeconds: Long?, rawDateStr: String?): Long {
        val fromUnix = (unixSeconds ?: 0L) * 1000L
        // التحقق من أن السيرفر لم يرسل تاريخاً قديماً خاطئاً في الـ Timestamp
        if (fromUnix > 1_767_225_600_000L) return fromUnix // >= 2026-01-01
        if (!rawDateStr.isNullOrBlank()) {
            return try {
                val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(rawDateStr.trim())?.time ?: fromUnix
            } catch (_: Exception) {
                fromUnix
            }
        }
        return fromUnix
    }

    private fun smartDecodeBase64(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.any { it in '\u0600'..'\u06FF' } || trimmed.contains(" ")) {
            return trimmed
        }
        return try {
            val decoded = String(Base64.decode(trimmed, Base64.DEFAULT), Charsets.UTF_8)
            if (decoded.isNotBlank() && !decoded.contains('\uFFFD')) decoded else trimmed
        } catch (_: Exception) {
            trimmed
        }
    }
}
