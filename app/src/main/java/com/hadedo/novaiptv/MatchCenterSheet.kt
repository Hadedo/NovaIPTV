package com.hadedo.novaiptv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class MatchStatRow(
    val labelAr: String,
    val homeVal: String,
    val awayVal: String,
    val homeRatio: Float,
    val awayRatio: Float
)

data class MatchTimelineEvent(
    val minute: String,
    val playerName: String,
    val detail: String,
    val type: String, // goal, yellow, red, sub
    val isHome: Boolean
)

data class PitchPlayer(
    val number: String,
    val name: String
)

data class MatchDetailedSummary(
    val referee: String = "",
    val venue: String = "",
    val homeFormation: String = "4-3-3",
    val awayFormation: String = "4-2-3-1",
    val homeLineup: List<PitchPlayer> = emptyList(),
    val awayLineup: List<PitchPlayer> = emptyList(),
    val stats: List<MatchStatRow> = emptyList(),
    val events: List<MatchTimelineEvent> = emptyList()
)

object MatchDetailsEngine {
    suspend fun fetchMatchSummary(eventId: String, leagueSlug: String): MatchDetailedSummary = withContext(Dispatchers.IO) {
        val slug = leagueSlug.ifBlank { "all" }
        val urlsToTry = listOf(
            "https://site.api.espn.com/apis/site/v2/sports/soccer/$slug/summary?event=$eventId",
            "https://site.api.espn.com/apis/site/v2/sports/soccer/all/summary?event=$eventId"
        )

        for (u in urlsToTry) {
            try {
                val conn = (URL(u).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 9000
                    readTimeout = 10000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(raw)

                // 1. الحكم والملعب
                val gameInfo = root.optJSONObject("gameInfo")
                val venue = gameInfo?.optJSONObject("venue")?.optString("fullName", "") ?: ""
                val officials = gameInfo?.optJSONArray("officials")
                val referee = if (officials != null && officials.length() > 0) {
                    officials.getJSONObject(0).optString("fullName", "")
                } else ""

                // 2. التشكيلة والخطط
                var homeForm = "4-3-3"
                var awayForm = "4-2-3-1"
                val homePlayers = mutableListOf<PitchPlayer>()
                val awayPlayers = mutableListOf<PitchPlayer>()

                val rosters = root.optJSONArray("rosters")
                if (rosters != null && rosters.length() >= 2) {
                    for (r in 0 until rosters.length()) {
                        val rObj = rosters.getJSONObject(r)
                        val isHome = rObj.optString("homeAway") == "home"
                        val form = rObj.optString("formation", if (isHome) "4-3-3" else "4-2-3-1")
                        if (isHome) homeForm = form else awayForm = form

                        val rosterArr = rObj.optJSONArray("roster") ?: continue
                        for (p in 0 until rosterArr.length()) {
                            val pObj = rosterArr.getJSONObject(p)
                            if (pObj.optBoolean("starter", false) || p < 11) {
                                val ath = pObj.optJSONObject("athlete")
                                val pName = ath?.optString("shortName", ath.optString("displayName", "لاعب")) ?: "لاعب"
                                val jersey = pObj.optString("jersey", "${p + 1}")
                                val targetList = if (isHome) homePlayers else awayPlayers
                                if (targetList.size < 11) {
                                    targetList.add(PitchPlayer(jersey, pName))
                                }
                            }
                        }
                    }
                }

                // 3. الإحصائيات
                val statsList = mutableListOf<MatchStatRow>()
                val boxscore = root.optJSONObject("boxscore")
                val teamsStats = boxscore?.optJSONArray("teams")
                if (teamsStats != null && teamsStats.length() >= 2) {
                    val hStatsArr = teamsStats.getJSONObject(0).optJSONArray("statistics")
                    val aStatsArr = teamsStats.getJSONObject(1).optJSONArray("statistics")
                    if (hStatsArr != null && aStatsArr != null) {
                        val statMap = mapOf(
                            "possessionPct" to "الاستحواذ",
                            "totalShots" to "التسديدات",
                            "shotsOnTarget" to "التسديد على المرمى",
                            "wonCorners" to "الركنيات",
                            "foulsCommitted" to "الأخطاء",
                            "offsides" to "التسلل",
                            "yellowCards" to "البطاقات الصفراء"
                        )
                        for (s in 0 until hStatsArr.length()) {
                            val hItem = hStatsArr.getJSONObject(s)
                            val key = hItem.optString("name", "")
                            val arLabel = statMap[key] ?: continue
                            val hValStr = hItem.optString("displayValue", "0")
                            var aValStr = "0"
                            for (k in 0 until aStatsArr.length()) {
                                val aItem = aStatsArr.getJSONObject(k)
                                if (aItem.optString("name") == key) {
                                    aValStr = aItem.optString("displayValue", "0")
                                    break
                                }
                            }
                            val hNum = hValStr.replace("%", "").toFloatOrNull() ?: 0f
                            val aNum = aValStr.replace("%", "").toFloatOrNull() ?: 0f
                            val sum = (hNum + aNum).coerceAtLeast(1f)
                            statsList.add(
                                MatchStatRow(
                                    labelAr = arLabel,
                                    homeVal = if (key == "possessionPct") "$hValStr%" else hValStr,
                                    awayVal = if (key == "possessionPct") "$aValStr%" else aValStr,
                                    homeRatio = (hNum / sum).coerceIn(0.08f, 0.92f),
                                    awayRatio = (aNum / sum).coerceIn(0.08f, 0.92f)
                                )
                            )
                        }
                    }
                }

                // 4. الأحداث (الأهداف، البطاقات، التبديلات)
                val eventsList = mutableListOf<MatchTimelineEvent>()
                val homeTeamId = root.optJSONObject("header")?.optJSONArray("competitions")
                    ?.optJSONObject(0)?.optJSONArray("competitors")?.optJSONObject(0)?.optString("id", "") ?: ""

                val keyEvents = root.optJSONArray("keyEvents")
                if (keyEvents != null) {
                    for (e in 0 until keyEvents.length()) {
                        val evObj = keyEvents.getJSONObject(e)
                        val typeText = evObj.optJSONObject("type")?.optString("text", "")?.lowercase() ?: ""
                        val min = evObj.optJSONObject("clock")?.optString("displayValue", "") ?: ""
                        val teamId = evObj.optJSONObject("team")?.optString("id", "") ?: ""
                        val participants = evObj.optJSONArray("participants")
                        val p1 = participants?.optJSONObject(0)?.optJSONObject("athlete")?.optString("shortName", "") ?: ""
                        val p2 = if (participants != null && participants.length() > 1) {
                            participants.getJSONObject(1).optJSONObject("athlete")?.optString("shortName", "") ?: ""
                        } else ""

                        val evType = when {
                            typeText.contains("goal") -> "goal"
                            typeText.contains("yellow") -> "yellow"
                            typeText.contains("red") -> "red"
                            typeText.contains("sub") -> "sub"
                            else -> continue
                        }
                        val detailAr = when (evType) {
                            "goal" -> if (p2.isNotBlank()) "هدف • تمريرة: $p2" else "هدف محقق ⚽"
                            "yellow" -> "بطاقة صفراء"
                            "red" -> "بطاقة حمراء"
                            else -> if (p2.isNotBlank()) "خروج: $p2" else "تبديل"
                        }
                        eventsList.add(
                            MatchTimelineEvent(
                                minute = min,
                                playerName = p1.ifBlank { "لاعب" },
                                detail = detailAr,
                                type = evType,
                                isHome = teamId == homeTeamId
                            )
                        )
                    }
                }

                return@withContext MatchDetailedSummary(
                    referee = referee,
                    venue = venue,
                    homeFormation = homeForm,
                    awayFormation = awayForm,
                    homeLineup = homePlayers,
                    awayLineup = awayPlayers,
                    stats = statsList,
                    events = eventsList
                )
            } catch (_: Exception) {}
        }
        MatchDetailedSummary()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaMatchCenterBottomSheet(
    match: LiveMatchInfo,
    matchingChannels: List<GroupedSportsChannel>,
    liveEpgMap: Map<String, String>,
    accentColor: Color,
    cardColor: Color,
    bgColor: Color,
    onDismiss: () -> Unit,
    onPlayChannelItem: (MediaItemModel) -> Unit,
    onOpenChannelVariants: (GroupedSportsChannel) -> Unit
) {
    val greenText = Color(0xFF4ADE80)
    val greenBadge = Color(0xFF1E3A2F)

    var summary by remember { mutableStateOf<MatchDetailedSummary?>(null) }
    var isLoadingSummary by remember { mutableStateOf(true) }
    var activeSubTab by remember { mutableIntStateOf(0) } // 0: التشكيلة, 1: الأحداث, 2: الإحصائيات
    var showChannelsList by remember { mutableStateOf(false) }

    LaunchedEffect(match.id) {
        isLoadingSummary = true
        summary = MatchDetailsEngine.fetchMatchSummary(match.id, match.leagueSlug)
        isLoadingSummary = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardColor,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 14.dp)
        ) {
            // 1. بطاقة النتيجة العلوية الزجاجية
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(accentColor.copy(alpha = 0.18f), Color(0xFF0B0B10))
                        )
                    )
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(match.leagueName, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            AsyncImage(model = match.homeLogo, contentDescription = match.homeTeam, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(match.homeTeam, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 10.dp)) {
                            Text(
                                if (match.statusState == "pre") "VS" else "${match.homeScore}  -  ${match.awayScore}",
                                color = if (match.statusState == "in") greenText else Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(if (match.statusState == "in") Color(0x33EF4444) else greenBadge, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    match.statusText,
                                    color = if (match.statusState == "in") Color(0xFFF87171) else greenText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            AsyncImage(model = match.awayLogo, contentDescription = match.awayTeam, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(match.awayTeam, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    val refText = summary?.referee ?: ""
                    if (refText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("الحكم: $refText", color = Color.Gray, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // زر المشاهدة المباشرة المتوهج
                    Button(
                        onClick = { showChannelsList = !showChannelsList },
                        colors = ButtonDefaults.buttonColors(containerColor = greenBadge),
                        border = BorderStroke(1.dp, greenText.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.LiveTv, null, tint = greenText, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (showChannelsList) "إخفاء القنوات الناقلة" else "المشاهدة المباشرة (${match.suggestedBroadcaster})",
                            color = greenText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // قائمة القنوات الناقلة الفورية عند الضغط على زر المشاهدة
            if (showChannelsList) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    border = BorderStroke(1.dp, greenText.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                ) {
                    LazyColumn(contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(matchingChannels) { grp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardColor)
                                    .clickable {
                                        onDismiss()
                                        if (grp.variants.size == 1) onPlayChannelItem(grp.variants[0].item)
                                        else onOpenChannelVariants(grp)
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(grp.cleanName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    val epg = liveEpgMap[grp.cleanName]
                                    if (!epg.isNullOrBlank()) {
                                        Text(epg, color = greenText, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                                Box(modifier = Modifier.background(greenBadge, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("${grp.variants.size} جودات ▶", color = greenText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. تبويبات تفاصيل المباراة (التشكيلة - الأحداث - الإحصائيات)
            val det = summary
            val eventsCount = det?.events?.size ?: 0
            Row(
                modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(14.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    0 to "التشكيلة (✓)",
                    1 to "الأحداث ($eventsCount)",
                    2 to "الإحصائيات"
                ).forEach { (idx, title) ->
                    val isSel = activeSubTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) accentColor.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { activeSubTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            color = if (isSel) accentColor else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoadingSummary) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (det != null) {
                when (activeSubTab) {
                    // تبويب 0: التشكيلة على أرضية الملعب (Interactive Pitch View)
                    0 -> {
                        if (det.homeLineup.isEmpty() && det.awayLineup.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("التشكيلة الرسمية ستتوفر قبل انطلاق المباراة بقليل", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            FootballPitchCanvasView(
                                homeTeam = match.homeTeam,
                                awayTeam = match.awayTeam,
                                homeFormation = det.homeFormation,
                                awayFormation = det.awayFormation,
                                homePlayers = det.homeLineup,
                                awayPlayers = det.awayLineup,
                                accentColor = accentColor
                            )
                        }
                    }

                    // تبويب 1: الخط الزمني للأحداث (الأهداف والبطاقات والتبديلات)
                    1 -> {
                        if (det.events.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("لا توجد أحداث مسجلة حتى الآن", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 20.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(det.events) { ev ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(bgColor, RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        if (ev.isHome) {
                                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                                                Text(ev.playerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(ev.detail, color = Color.Gray, fontSize = 10.sp)
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 10.dp)) {
                                            val badgeColor = when (ev.type) {
                                                "goal" -> greenText
                                                "yellow" -> Color(0xFFFACC15)
                                                "red" -> Color(0xFFEF4444)
                                                else -> Color(0xFF38BDF8)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .background(badgeColor.copy(alpha = 0.2f), CircleShape)
                                                    .border(1.dp, badgeColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    when (ev.type) {
                                                        "goal" -> "⚽"
                                                        "yellow" -> "🟨"
                                                        "red" -> "🟥"
                                                        else -> "⇄"
                                                    },
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Text(ev.minute, color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        if (!ev.isHome) {
                                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                                Text(ev.playerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(ev.detail, color = Color.Gray, fontSize = 10.sp)
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // تبويب 2: الإحصائيات المتوهجة (Neon Dual Progress Bars)
                    2 -> {
                        if (det.stats.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("إحصائيات المباراة ستظهر فور انطلاق الشوط الأول", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 20.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("🔴 ${match.homeTeam}", color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("${match.awayTeam} 🔵", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                items(det.stats) { st ->
                                    Column(modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(12.dp)).padding(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(st.homeVal, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(st.labelAr, color = Color.LightGray, fontSize = 11.sp)
                                            Text(st.awayVal, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(st.homeRatio)
                                                    .fillMaxHeight()
                                                    .background(Color(0xFFF43F5E), CircleShape)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .weight(st.awayRatio)
                                                    .fillMaxHeight()
                                                    .background(Color(0xFF3B82F6), CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FootballPitchCanvasView(
    homeTeam: String,
    awayTeam: String,
    homeFormation: String,
    awayFormation: String,
    homePlayers: List<PitchPlayer>,
    awayPlayers: List<PitchPlayer>,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF14532D), Color(0xFF166534), Color(0xFF14532D))
                )
            )
            .border(2.dp, Color(0x55FFFFFF), RoundedCornerShape(18.dp))
    ) {
        // رسم خطوط الملعب
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = Color.White.copy(alpha = 0.35f)
            val stroke = Stroke(width = 2.dp.toPx())
            drawRect(color = lineColor, topLeft = Offset(12f, 12f), size = Size(size.width - 24f, size.height - 24f), style = stroke)
            drawLine(color = lineColor, start = Offset(12f, size.height / 2f), end = Offset(size.width - 12f, size.height / 2f), strokeWidth = 2.dp.toPx())
            drawCircle(color = lineColor, radius = 42.dp.toPx(), center = Offset(size.width / 2f, size.height / 2f), style = stroke)
            drawRect(color = lineColor, topLeft = Offset(size.width * 0.25f, 12f), size = Size(size.width * 0.5f, size.height * 0.14f), style = stroke)
            drawRect(color = lineColor, topLeft = Offset(size.width * 0.25f, size.height * 0.86f - 12f), size = Size(size.width * 0.5f, size.height * 0.14f), style = stroke)
        }

        Column(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp, horizontal = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
            // نصف الملعب العلوي (الفريق المضيف)
            Column(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.SpaceEvenly) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(homeTeam, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(homeFormation, color = Color(0xFFFACC15), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                val homeRows = splitPlayersByFormation(homePlayers, homeFormation, isTopHalf = true)
                homeRows.forEach { rowPlayers ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        rowPlayers.forEach { p ->
                            PlayerJerseyBadge(p, Color(0xFFDC2626))
                        }
                    }
                }
            }

            // نصف الملعب السفلي (الفريق الضيف)
            Column(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.SpaceEvenly) {
                val awayRows = splitPlayersByFormation(awayPlayers, awayFormation, isTopHalf = false)
                awayRows.forEach { rowPlayers ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        rowPlayers.forEach { p ->
                            PlayerJerseyBadge(p, Color(0xFF2563EB))
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(awayTeam, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(awayFormation, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun PlayerJerseyBadge(player: PitchPlayer, shirtColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(shirtColor, CircleShape)
                .border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(player.number, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .background(Color(0xBB000000), RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                player.name.substringAfterLast(" "),
                color = Color.White,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun splitPlayersByFormation(players: List<PitchPlayer>, formation: String, isTopHalf: Boolean): List<List<PitchPlayer>> {
    if (players.isEmpty()) return emptyList()
    val lines = formation.split("-").mapNotNull { it.trim().toIntOrNull() }.ifEmpty { listOf(4, 3, 3) }
    val rows = mutableListOf<List<PitchPlayer>>()
    rows.add(listOf(players[0])) // حارس المرمى

    var idx = 1
    for (count in lines) {
        if (idx >= players.size) break
        val end = minOf(idx + count, players.size)
        rows.add(players.subList(idx, end))
        idx = end
    }
    if (idx < players.size) {
        rows.add(players.subList(idx, players.size))
    }
    return if (isTopHalf) rows else rows.reversed()
}
