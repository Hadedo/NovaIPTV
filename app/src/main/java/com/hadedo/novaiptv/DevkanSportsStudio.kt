package com.hadedo.novaiptv

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

private fun cleanStudioLeagueName(raw: String): String {
    val cleaned = raw
        .replace(Regex("""[|┃┆┇┊┋︱︲∣║‖•/_\\]+"""), " ")
        .replace(Regex("""(?i)^\s*(fr|uk|us|ar|de|tr|es|be|nl|it|pt)\b\s*"""), "")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()
    return cleaned.ifEmpty { "بطولة دولية" }
}

@Composable
fun DevkanSportsStudioScreen(
    matches: List<RealSportsMatch>,
    quickSportsChannels: List<MediaItemModel>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onMatchClick: (RealSportsMatch) -> Unit,
    onQuickChannelClick: (MediaItemModel) -> Unit
) {
    val context = LocalContext.current
    val bgDark = Color(0xFF07070A)
    val cardBg = Color(0xFF12131C)
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)
    val liveRed = Color(0xFFEF4444)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: الكل، 1: مباشر الآن، 2: تنبيهاتي
    var selectedLeague by remember { mutableStateOf("الكل") }
    var reminderTick by remember { mutableIntStateOf(0) }

    val liveCount = remember(matches) { matches.count { it.isLive } }
    val upcomingCount = (matches.size - liveCount).coerceAtLeast(0)

    val leagues = remember(matches) {
        listOf("الكل") + matches.map { cleanStudioLeagueName(it.league) }.distinct()
    }

    val filteredMatches = remember(matches, searchQuery, selectedTab, selectedLeague, reminderTick) {
        val q = searchQuery.trim()
        matches.filter { m ->
            val cleanLg = cleanStudioLeagueName(m.league)
            val matchKey = "${m.homeTeam} vs ${m.awayTeam}"
            val tabOk = when (selectedTab) {
                1 -> m.isLive
                2 -> MatchReminderManager.remindedMatches.contains(matchKey)
                else -> true
            }
            val leagueOk = selectedLeague == "الكل" || cleanLg == selectedLeague
            val searchOk = q.isEmpty() ||
                m.homeTeam.contains(q, ignoreCase = true) ||
                m.awayTeam.contains(q, ignoreCase = true) ||
                cleanLg.contains(q, ignoreCase = true) ||
                m.suggestedBroadcaster.contains(q, ignoreCase = true)

            tabOk && leagueOk && searchOk
        }
    }

    val groupedMatches = remember(filteredMatches) {
        filteredMatches.groupBy { cleanStudioLeagueName(it.league) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. الهيدر السينمائي للاستوديو الرياضي
        item(key = "studio_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1A160B), Color(0xFF121420), Color(0xFF1B1116))
                        )
                    )
                    .border(1.dp, gold.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = goldLight, modifier = Modifier.size(22.dp))
                            Text(
                                text = "استوديو المباريات المباشر",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StudioLivePulseBadge(text = "$liveCount جارية الآن")
                            Text(
                                text = "• $upcomingCount قادمة • ${(leagues.size - 1).coerceAtLeast(0)} بطولة",
                                color = Color(0xFFB4B9C6),
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(gold.copy(alpha = 0.16f))
                            .border(1.dp, gold.copy(alpha = 0.45f), CircleShape)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(color = goldLight, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = goldLight, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // 2. شريط القنوات الرياضية السريعة
        if (quickSportsChannels.isNotEmpty()) {
            item(key = "quick_channels") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚡ القنوات الرياضية السريعة", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${quickSportsChannels.size} قناة", color = Color(0xFF8B90A0), fontSize = 11.sp)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickSportsChannels.take(20), key = { it.id }) { ch ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                                    .border(1.dp, Color(0xFF252638), RoundedCornerShape(12.dp))
                                    .clickable { onQuickChannelClick(ch) }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                if (ch.logo.isNotEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(ch.logo).size(72, 72).build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                    )
                                } else {
                                    Icon(Icons.Default.LiveTv, contentDescription = null, tint = gold, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = ch.title,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 135.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. شريط البحث
        item(key = "studio_search") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث عن فريق، بطولة، أو قناة ناقلة...", color = Color(0xFF7B8090), fontSize = 12.5.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = gold, modifier = Modifier.size(19.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(17.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = gold,
                    unfocusedBorderColor = Color(0xFF232436),
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. تبويبات الفلترة الذكية (الكل / مباشر الآن / تنبيهاتي)
        item(key = "studio_tabs") {
            val remindedCount = remember(reminderTick, matches.size) {
                matches.count { MatchReminderManager.remindedMatches.contains("${it.homeTeam} vs ${it.awayTeam}") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudioFilterTab(
                    title = "الكل (${matches.size})",
                    selected = selectedTab == 0,
                    accent = gold,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 }
                )
                StudioFilterTab(
                    title = "مباشر ($liveCount)",
                    selected = selectedTab == 1,
                    accent = liveRed,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 }
                )
                StudioFilterTab(
                    title = "تنبيهاتي ($remindedCount)",
                    selected = selectedTab == 2,
                    accent = goldLight,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 2 }
                )
            }
        }

        // 5. شريط البطولات المنظّفة
        item(key = "studio_leagues") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(leagues, key = { it }) { lg ->
                    val isSelected = selectedLeague == lg
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) gold else cardBg)
                            .border(1.dp, if (isSelected) gold else Color(0xFF252638), RoundedCornerShape(10.dp))
                            .clickable { selectedLeague = lg }
                            .padding(horizontal = 13.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = lg,
                            color = if (isSelected) Color.Black else Color(0xFFD0D4DF),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 6. عرض المباريات مجمّعة ببطاقات بث احترافية
        if (isRefreshing && groupedMatches.isEmpty()) {
            item(key = "loading") {
                Box(modifier = Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = gold)
                }
            }
        } else if (groupedMatches.isEmpty()) {
            item(key = "empty") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 42.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color(0xFF4B5563), modifier = Modifier.size(40.dp))
                    Text(
                        text = if (selectedTab == 2) "لم تقم بتفعيل تنبيه لأي مباراة بعد" else "لا توجد مباريات مطابقة للفلتر الحالي",
                        color = Color(0xFF8B90A0),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            groupedMatches.forEach { (leagueTitle, leagueMatches) ->
                item(key = "hdr_$leagueTitle") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier.weight(1f)
                        ) {
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
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text("${leagueMatches.size} مباراة", color = Color(0xFF8B90A0), fontSize = 11.sp)
                    }
                }

                itemsIndexed(leagueMatches, key = { idx, m -> "card_${leagueTitle}_${idx}_${m.id}" }) { _, match ->
                    val matchKey = "${match.homeTeam} vs ${match.awayTeam}"
                    val isReminded = remember(reminderTick, matchKey) {
                        MatchReminderManager.remindedMatches.contains(matchKey)
                    }
                    StudioBroadcastMatchCard(
                        match = match,
                        isReminded = isReminded,
                        onToggleReminder = {
                            val added = MatchReminderManager.toggleReminder(matchKey)
                            reminderTick++
                            Toast.makeText(
                                context,
                                if (added) "تم تفعيل تنبيه المباراة 🔔" else "تم إلغاء التنبيه",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onWatchClick = { onMatchClick(match) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioBroadcastMatchCard(
    match: RealSportsMatch,
    isReminded: Boolean,
    onToggleReminder: () -> Unit,
    onWatchClick: () -> Unit
) {
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)
    val liveRed = Color(0xFFEF4444)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF13141E))
            .border(
                width = 1.dp,
                color = if (match.isLive) liveRed.copy(alpha = 0.5f) else Color(0xFF232538),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onWatchClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // المنتصف: 3 أعمدة واضحة بدون قص أسماء الفرق
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
                TeamLogoOrMonogram(name = match.homeTeam, logoUrl = match.homeLogo, accent = gold)
                Text(
                    text = match.homeTeam,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            // التوقيت والنتيجة
            Column(
                modifier = Modifier.width(96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (match.isLive) {
                    StudioLivePulseBadge(text = match.status.ifEmpty { "LIVE" })
                } else {
                    Text(
                        text = match.matchTime,
                        color = goldLight,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (match.isLive) Color(0xFF1C2920) else Color(0xFF1E2030))
                        .border(
                            1.dp,
                            if (match.isLive) Color(0xFF22C55E).copy(alpha = 0.4f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = match.scoreText.ifEmpty { "VS" },
                        color = if (match.isLive) Color(0xFF4ADE80) else goldLight,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }

            // الفريق الثاني
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TeamLogoOrMonogram(name = match.awayTeam, logoUrl = match.awayLogo, accent = Color(0xFF60A5FA))
                Text(
                    text = match.awayTeam,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        HorizontalDivider(color = Color(0xFF202232), thickness = 1.dp)

        // الشريط السفلي: القناة الناقلة + زر التنبيه + زر المشاهدة
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
                    text = match.suggestedBroadcaster.ifEmpty { "قنوات رياضية متعددة" },
                    color = Color(0xFFB4B9C6),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onToggleReminder,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isReminded) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                        contentDescription = "تنبيه",
                        tint = if (isReminded) goldLight else Color(0xFF8B90A0),
                        modifier = Modifier.size(19.dp)
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
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (match.isLive) Color.White else Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (match.isLive) "شاهد البث" else "اختر القناة",
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
private fun TeamLogoOrMonogram(name: String, logoUrl: String, accent: Color) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(logoUrl).size(100, 100).build(),
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(30.dp)
            )
        } else {
            val initials = remember(name) {
                name.trim().split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                    .ifEmpty { "FC" }
            }
            Text(text = initials, color = accent, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
    }
}

@Composable
private fun StudioLivePulseBadge(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "studioLivePulse")
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
        Text(text = text, color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun StudioFilterTab(
    title: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = 0.2f) else Color(0xFF12131C))
            .border(1.dp, if (selected) accent else Color(0xFF232538), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) accent else Color(0xFFB4B9C6),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.5.sp,
            maxLines = 1
        )
    }
}
