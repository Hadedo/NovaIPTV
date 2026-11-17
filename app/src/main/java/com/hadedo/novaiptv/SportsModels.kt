package com.hadedo.novaiptv

data class EpgProgram(
    val id: String,
    val channelId: String,
    val channelName: String,
    val channelLogo: String? = null,
    val streamQuality: String? = null, // 4K, FHD, HD
    val title: String,
    val description: String?,
    val startTime: Long,          // Timestamp (Millis)
    val endTime: Long,
    val category: String?,
    // بيانات مستخرجة للرياضة (تُحسب مرة واحدة وقت المزامنة وتُخزن في Room)
    val isSports: Boolean = false,
    val homeTeam: String? = null,
    val awayTeam: String? = null,
    val competition: String? = null,
    val isLive: Boolean = false
)

data class SportsMatchInfo(
    val homeTeam: String,
    val awayTeam: String,
    val competition: String?,
    val rawTitle: String
)

/**
 * بطاقة مباراة مجمّعة (تدمج القنوات والجودات المختلفة لنفس المباراة في بطاقة واحدة)
 */
data class GroupedSportsMatch(
    val matchKey: String,
    val homeTeam: String,
    val awayTeam: String,
    val competition: String?,
    val startTime: Long,
    val endTime: Long,
    val isLive: Boolean,
    val streams: List<EpgProgram> // مرتبة تلقائياً من الأعلى جودة (4K -> FHD -> HD)
) {
    val primaryStream: EpgProgram get() = streams.first()
}

data class ClassifiedSportsData(
    val liveMatches: List<GroupedSportsMatch>,
    val upcoming24hMatches: List<GroupedSportsMatch>,
    val upcomingLaterMatches: List<GroupedSportsMatch>,
    val livePrograms: List<EpgProgram>
)
