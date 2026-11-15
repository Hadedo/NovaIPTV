package com.hadedo.novaiptv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaMatchCenterBottomSheet(
    match: LiveMatchInfo,
    matchingChannels: List<GroupedSportsChannel>,
    isEpgVerified: Boolean,
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
    val isLiveNow = match.statusState == "in"

    var intelligence by remember { mutableStateOf<MatchFullIntelligence?>(null) }
    var isLoadingEvents by remember { mutableStateOf(true) }

    LaunchedEffect(match.id) {
        isLoadingEvents = true
        intelligence = ServersEngine.fetchMatchDetailedEvents(match)
        isLoadingEvents = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // بطاقة النتيجة والملعب
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(accentColor.copy(alpha = 0.25f), bgColor)))
                    .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(match.leagueName, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .background(if (isLiveNow) greenBadge else Color(0xFF1E1E2A), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(match.statusText, color = if (isLiveNow) greenText else Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            AsyncImage(model = match.awayLogo, contentDescription = match.awayTeam, modifier = Modifier.size(46.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(match.awayTeam, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 10.dp)) {
                            Text(
                                if (match.statusState == "pre") "VS" else "${match.awayScore} - ${match.homeScore}",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp
                            )
                            Text("📺 ${match.suggestedBroadcaster}", color = greenText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            AsyncImage(model = match.homeLogo, contentDescription = match.homeTeam, modifier = Modifier.size(46.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(match.homeTeam, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2)
                        }
                    }

                    intelligence?.let { intel ->
                        if (intel.venue.isNotBlank()) {
                            Text(
                                "🏟️ ${intel.venue}" + if (intel.attendance.isNotBlank()) " • ${intel.attendance}" else "",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // قسم القنوات الناقلة الحقيقية المطابقة بالذكاء الاصطناعي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📡 القناة الناقلة في اشتراكك", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .background(if (isEpgVerified) greenBadge else accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        if (isEpgVerified) "✓ مؤكد من دليل السيرفر" else "مطابقة ذكية للقناة",
                        color = if (isEpgVerified) greenText else accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (matchingChannels.isEmpty()) {
                Text("لم يتم العثور على قنوات رياضية في الاشتراك الحالي.", color = Color.Gray, fontSize = 12.sp)
            } else {
                matchingChannels.forEach { grp ->
                    val primaryItem = grp.variants.first().item
                    val liveEpgTitle = liveEpgMap[grp.cleanName] ?: ""
                    Card(
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = BorderStroke(1.dp, if (isEpgVerified) greenText.copy(alpha = 0.55f) else accentColor.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            onDismiss()
                            if (grp.variants.size == 1) onPlayChannelItem(primaryItem)
                            else onOpenChannelVariants(grp)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(model = grp.logo, contentDescription = grp.cleanName, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(grp.cleanName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (liveEpgTitle.isNotBlank()) {
                                    Text("🔴 يعرض الآن: $liveEpgTitle", color = greenText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                } else {
                                    Text("متوفرة بـ ${grp.variants.size} جودات بث", color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                            Button(
                                onClick = {
                                    onDismiss()
                                    if (grp.variants.size == 1) onPlayChannelItem(primaryItem)
                                    else onOpenChannelVariants(grp)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("شاهد", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // قسم تفاصيل المباراة الدقيقة: مسجلو الأهداف والبطاقات والإحصائيات
            if (isLoadingEvents) {
                LinearProgressIndicator(color = accentColor, modifier = Modifier.fillMaxWidth())
            } else {
                intelligence?.let { intel ->
                    if (intel.homeShots != "-" || intel.homePossession != "50%") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(bgColor)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("الاستحواذ", color = Color.Gray, fontSize = 10.sp)
                                Text("${intel.awayPossession}  |  ${intel.homePossession}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("التسديدات على المرمى", color = Color.Gray, fontSize = 10.sp)
                                Text("${intel.awayShots}  |  ${intel.homeShots}", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    if (intel.timelineEvents.isNotEmpty()) {
                        Text("⏱️ أحداث المباراة (الأهداف والبطاقات)", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(bgColor)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            intel.timelineEvents.forEach { ev ->
                                val iconEmoji = when (ev.eventType) {
                                    "goal" -> "⚽"
                                    "penalty" -> "⚽ (جزاء)"
                                    "owngoal" -> "⚽ (عكسي)"
                                    "yellow" -> "🟨"
                                    "red" -> "🟥"
                                    else -> "🔄"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF1A1A26), CircleShape)
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(ev.minute, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(iconEmoji, fontSize = 13.sp)
                                        Text(ev.playerName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(ev.teamName, color = Color.Gray, fontSize = 10.sp, maxLines = 1)
                                }
                            }
                        }
                    } else if (match.statusState != "pre") {
                        Text("لا توجد أحداث مسجلة (أهداف أو بطاقات) في هذه المباراة حتى الآن.", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
