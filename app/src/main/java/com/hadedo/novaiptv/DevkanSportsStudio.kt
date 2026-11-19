package com.hadedo.novaiptv

import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// نموذج مبسط للمباراة (اربطه بنموذجك الحالي بسهولة)
data class ProMatchItem(
    val id: String,
    val leagueRaw: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: String? = null,
    val awayScore: String? = null,
    val timeText: String,
    val isLive: Boolean,
    val channelName: String,
    val qualityBadge: String = "FHD",
    val streamUrl: String
)

// دالة تنظيف أسماء البطولات الخام من رموز السيرفر
fun cleanLeagueName(raw: String): String {
    return raw
        .replace(Regex("^[A-Z]{2,3}\\s*\\|+\\s*"), "") // يحذف FR | و BE ||
        .replace("|", "")
        .replace("•", " - ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifEmpty { "بطولة رياضية" }
}

@Composable
fun DevkanSportsStudioScreen(
    matches: List<ProMatchItem>,
    quickSportsChannels: List<MediaItemModel> = emptyList(),
    onRefresh: () -> Unit,
    onWatchMatch: (ProMatchItem) -> Unit,
    onWatchQuickChannel: (MediaItemModel) -> Unit
) {
    val bgDark = Color(0xFF07070A)
    val cardBg = Color(0xFF12121A)
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)
    val liveRed = Color(0xFFEF4444)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: الكل, 1: مباشر, 2: المفضلة
    var selectedLeague by remember { mutableStateOf("الكل") }
    val favoriteIds = remember { mutableStateListOf<String>() }

    // حساب الإحصائيات والبطولات المنظفة
    val liveCount = remember(matches) { matches.count { it.isLive } }
    val leagues = remember(matches) {
        listOf("الكل") + matches.map { cleanLeagueName(it.leagueRaw) }.distinct()
    }

    // فلترة ذكية وسريعة
    val filteredMatches = remember(matches, searchQuery, selectedTab, selectedLeague, favoriteIds.size) {
        matches.filter { match ->
            val cleanLeague = cleanLeagueName(match.leagueRaw)
            val matchesTab = when (selectedTab) {
                1 -> match.isLive
                2 -> favoriteIds.contains(match.id)
                else -> true
            }
            val matchesLeague = selectedLeague == "الكل" || cleanLeague == selectedLeague
            val matchesSearch = searchQuery.isBlank() ||
                match.homeTeam.contains(searchQuery, ignoreCase = true) ||
                match.awayTeam.contains(searchQuery, ignoreCase = true) ||
                cleanLeague.contains(searchQuery, ignoreCase = true)

            matchesTab && matchesLeague && matchesSearch
        }
    }

    val groupedMatches = remember(filteredMatches) {
        filteredMatches.groupBy { cleanLeagueName(it.leagueRaw) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. الهيدر السينمائي للاستوديو
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1A160B), Color(0xFF11121A), Color(0xFF1C1317))
                        )
                    )
                    .border(1.dp, gold.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = goldLight, modifier = Modifier.size(22.dp))
                            Text(
                                text = "استوديو المباريات المباشر",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LivePulseBadge(text = "$liveCount مباشر الآن")
                            Text(
                                text = "• ${matches.size - liveCount} قادمة • ${leagues.size - 1} بطولة",
                                color = Color(0xFFB4B9C6),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(gold.copy(alpha = 0.15f))
                            .border(1.dp, gold.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = goldLight)
                    }
                }
            }
        }

        // 2. شريط القنوات الرياضية السريعة (إذا توفرت)
        if (quickSportsChannels.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "وصول سريع للقنوات الرياضية",
                        color = gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickSportsChannels.take(15), key = { it.id }) { ch ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                                    .border(1.dp, Color(0xFF262636), RoundedCornerShape(12.dp))
                                    .clickable { onWatchQuickChannel(ch) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.LiveTv, contentDescription = null, tint = gold, modifier = Modifier.size(15.dp))
                                Text(
                                    text = ch.title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. شريط البحث الأنيق
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث عن فريق، بطولة، أو قناة...", color = Color(0xFF7B8090), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gold) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = gold,
                    unfocusedBorderColor = Color(0xFF232332),
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. تبويبات الفلترة السريعة (الكل / مباشر الآن / المفضلة)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SportsFilterTab(
                    title = "الكل (${matches.size})",
                    selected = selectedTab == 0,
                    accent = gold,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 }
                )
                SportsFilterTab(
                    title = "مباشر ($liveCount)",
                    selected = selectedTab == 1,
                    accent = liveRed,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 }
                )
                SportsFilterTab(
                    title = "المفضلة (${favoriteIds.size})",
                    selected = selectedTab == 2,
                    accent = goldLight,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 2 }
                )
            }
        }

        // 5. شريط البطولات المنظّفة
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(leagues) { league ->
                    val isSelected = selectedLeague == league
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) gold else cardBg)
                            .border(1.dp, if (isSelected) gold else Color(0xFF262636), RoundedCornerShape(10.dp))
                            .clickable { selectedLeague = league }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = league,
                            color = if (isSelected) Color.Black else Color(0xFFD0D4DF),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 6. عرض المباريات مجمّعة حسب البطولة
        if (groupedMatches.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مباريات مطابقة للفلتر الحالي",
                        color = Color(0xFF8B90A0),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            groupedMatches.forEach { (leagueTitle, leagueMatches) ->
                item(key = "header_$leagueTitle") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(16.dp)
                                    .clip(CircleShape)
                                    .background(gold)
                            )
                            Text(
                                text = leagueTitle,
                                color = goldLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "${leagueMatches.size} مباراة",
                            color = Color(0xFF8B90A0),
                            fontSize = 11.sp
                        )
                    }
                }

                items(leagueMatches, key = { it.id }) { match ->
                    val isFav = favoriteIds.contains(match.id)
                    BroadcastMatchCard(
                        match = match,
                        isFavorite = isFav,
                        onToggleFavorite = {
                            if (isFav) favoriteIds.remove(match.id) else favoriteIds.add(match.id)
                        },
                        onWatchClick = { onWatchMatch(match) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BroadcastMatchCard(
    match: ProMatchItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onWatchClick: () -> Unit
) {
    val gold = Color(0xFFD4AF37)
    val liveRed = Color(0xFFEF4444)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF13131C))
            .border(
                width = 1.dp,
                color = if (match.isLive) liveRed.copy(alpha = 0.45f) else Color(0xFF232333),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onWatchClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // المنتصف: الفريقان + النتيجة أو الوقت
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // الفريق الأول
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TeamMonogramBadge(name = match.homeTeam, accent = gold)
                Text(
                    text = match.homeTeam,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            // النتيجة أو التوقيت
            Column(
                modifier = Modifier.width(92.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (match.isLive) {
                    LivePulseBadge(text = "LIVE")
                } else {
                    Text(
                        text = match.timeText,
                        color = gold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1E2C))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val centerText = if (match.homeScore != null && match.awayScore != null) {
                        "${match.homeScore}  -  ${match.awayScore}"
                    } else {
                        "VS"
                    }
                    Text(
                        text = centerText,
                        color = if (match.isLive) Color.White else gold,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }

            // الفريق الثاني
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TeamMonogramBadge(name = match.awayTeam, accent = Color(0xFF60A5FA))
                Text(
                    text = match.awayTeam,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        HorizontalDivider(color = Color(0xFF20202E), thickness = 1.dp)

        // الشريط السفلي: القناة + الجودة + زر المشاهدة والمفضلة
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(14.dp))
                Text(
                    text = match.channelName,
                    color = Color(0xFFB4B9C6),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(gold.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(match.qualityBadge, color = gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "مفضلة",
                        tint = if (isFavorite) gold else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Button(
                    onClick = onWatchClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (match.isLive) Color(0xFF16A34A) else gold
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (match.isLive) Color.White else Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (match.isLive) "شاهد البث" else "افتح القناة",
                        color = if (match.isLive) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamMonogramBadge(name: String, accent: Color) {
    val initials = remember(name) {
        name.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "FC" }
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = initials, color = accent, fontWeight = FontWeight.Black, fontSize = 13.sp)
    }
}

@Composable
private fun LivePulseBadge(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEF4444).copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color(0xFFEF4444))
        )
        Text(text = text, color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

@Composable
private fun SportsFilterTab(
    title: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = 0.2f) else Color(0xFF12121A))
            .border(1.dp, if (selected) accent else Color(0xFF232332), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) accent else Color(0xFFB4B9C6),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}
