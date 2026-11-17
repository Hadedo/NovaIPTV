package com.hadedo.novaiptv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DeepObsidian = Color(0xFF07090F)
private val CardSurface = Color(0xFF111625)
private val RoyalBlue = Color(0xFF2563EB)
private val LiveRed = Color(0xFFEF4444)
private val UpcomingGreen = Color(0xFF10B981)
private val GlassBorder = Color.White.copy(alpha = 0.08f)

@Composable
fun HybridHomeScreen(
    liveMatches: List<GroupedSportsMatch>,
    upcomingMatches: List<GroupedSportsMatch>,
    liveChannels: List<EpgProgram>,
    onPlaySportsMatch: (GroupedSportsMatch, isMultiView: Boolean) -> Unit,
    onPlayChannel: (EpgProgram) -> Unit,
    onNavigateTab: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMultiView: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedPill by remember { mutableStateOf("الكل") }
    val pills = listOf("الكل", "Live", "رياضة", "أفلام", "مسلسلات", "مفضلة", "EPG")

    Scaffold(
        containerColor = DeepObsidian,
        topBar = {
            FloatingGlassHeader(
                onOpenSearch = onOpenSearch,
                onOpenMultiView = onOpenMultiView,
                onOpenSettings = onOpenSettings
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. شريط التنقل السريع (أسلوب TiviMate)
            item(key = "quick_pills") {
                QuickPillBar(
                    pills = pills,
                    selectedPill = selectedPill,
                    onSelect = { pill ->
                        selectedPill = pill
                        onNavigateTab(pill)
                    }
                )
            }

            // 2. واجهة العرض البطولية الديناميكية (Netflix + Sports)
            item(key = "hero_banner") {
                val featuredMatch = liveMatches.firstOrNull() ?: upcomingMatches.firstOrNull()
                if (featuredMatch != null) {
                    SportsHeroBanner(
                        match = featuredMatch,
                        onWatchNow = { onPlaySportsMatch(featuredMatch, false) },
                        onMultiView = { onPlaySportsMatch(featuredMatch, true) }
                    )
                }
            }

            // 3. صف الآن يُبث مباشرة (Live Now)
            if (liveMatches.isNotEmpty() || liveChannels.isNotEmpty()) {
                item(key = "live_now_row") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle(title = "🔴 الآن يُبث مباشرة (Live Now)")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(liveMatches, key = { it.matchKey }) { match ->
                                SmartSportsMatchCard(
                                    match = match,
                                    onClick = { onPlaySportsMatch(match, false) }
                                )
                            }
                        }
                    }
                }
            }

            // 4. صف رياضة اليوم (مرتب زمنياً بتوقيتك المحلي)
            if (upcomingMatches.isNotEmpty()) {
                item(key = "today_sports_row") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle(title = "🏆 رياضة اليوم")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(upcomingMatches, key = { it.matchKey }) { match ->
                                SmartSportsMatchCard(
                                    match = match,
                                    onClick = { onPlaySportsMatch(match, false) }
                                )
                            }
                        }
                    }
                }
            }

            // 5. صف القنوات الرياضية المميزة
            item(key = "sports_networks") {
                val networks = listOf("beIN SPORTS", "SSC", "AD Sports", "Alkass", "Sky Sports", "DAZN")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle(title = "📺 قنوات رياضية مميزة")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(networks, key = { it }) { network ->
                            Box(
                                modifier = Modifier
                                    .width(126.dp)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CardSurface)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                                    .clickable { onNavigateTab(network) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = network,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingGlassHeader(
    onOpenSearch: () -> Unit,
    onOpenMultiView: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepObsidian.copy(alpha = 0.92f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(UpcomingGreen)
            )
            Text(
                text = "DEVKAN 34 VIP",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                letterSpacing = 0.8.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HeaderIconButton(icon = Icons.Rounded.Search, contentDesc = "بحث", onClick = onOpenSearch)
            HeaderIconButton(icon = Icons.Rounded.GridView, contentDesc = "Multi-View", onClick = onOpenMultiView)
            HeaderIconButton(icon = Icons.Rounded.Person, contentDesc = "الإعدادات", onClick = onOpenSettings)
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(CardSurface)
            .border(1.dp, GlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDesc, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun QuickPillBar(
    pills: List<String>,
    selectedPill: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pills, key = { it }) { pill ->
            val isSelected = pill == selectedPill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) RoyalBlue else CardSurface)
                    .border(1.dp, if (isSelected) RoyalBlue else GlassBorder, RoundedCornerShape(50))
                    .clickable { onSelect(pill) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = pill,
                    color = Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SportsHeroBanner(
    match: GroupedSportsMatch,
    onWatchNow: () -> Unit,
    onMultiView: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF13203C), CardSurface, DeepObsidian)
                )
            )
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.competition ?: "بث رياضي مباشر",
                    color = Color(0xFF93C5FD),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                StatusBadge(isLive = match.isLive, startTime = match.startTime)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.homeTeam,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "VS",
                    color = RoyalBlue,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Text(
                    text = match.awayTeam,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onWatchNow,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("مشاهدة البث (${match.streams.size})", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onMultiView,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Text("Multi-View", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SmartSportsMatchCard(
    match: GroupedSportsMatch,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(275.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = match.competition ?: "مباراة",
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            StatusBadge(isLive = match.isLive, startTime = match.startTime)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = match.homeTeam,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = match.awayTeam,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📺 ${match.primaryStream.channelName}",
                color = Color(0xFF93C5FD),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (match.streams.size > 1) {
                Text(
                    text = "+${match.streams.size - 1} جودات",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(isLive: Boolean, startTime: Long) {
    val timeText = remember(startTime) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(startTime))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isLive) LiveRed.copy(alpha = 0.18f) else UpcomingGreen.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (isLive) "🔴 مباشر" else "🟢 $timeText",
            color = if (isLive) LiveRed else UpcomingGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
