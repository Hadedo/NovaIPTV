package com.hadedo.novaiptv

import android.content.Context
import android.net.ConnectivityManager
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

private enum class NetSpeedTier(val targetRank: Int, val labelAr: String, val labelEn: String, val badge: String) {
    ULTRA(4, "إنترنت فائق (4K / FHD)", "Ultra Net (4K/FHD)", "4K/FHD"),
    MEDIUM(2, "إنترنت متوسط (HD)", "Medium Net (HD)", "HD"),
    LOW(1, "إنترنت ضعيف (SD)", "Low Net (SD)", "SD")
}

private fun detectNetworkSpeedTier(context: Context): NetSpeedTier {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val downKbps = caps?.linkDownstreamBandwidthKbps ?: 12_000
        when {
            downKbps >= 15_000 -> NetSpeedTier.ULTRA
            downKbps >= 5_000 -> NetSpeedTier.MEDIUM
            else -> NetSpeedTier.LOW
        }
    } catch (_: Exception) {
        NetSpeedTier.MEDIUM
    }
}

private fun cleanStudioLeagueName(raw: String): String {
    val cleaned = raw
        .replace(Regex("""[|┃┆┇┊┋︱︲∣║‖•/_\\]+"""), " ")
        .replace(Regex("""(?i)^\s*(fr|uk|us|ar|de|tr|es|be|nl|it|pt)\b\s*"""), "")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()
    return cleaned.ifEmpty { "بطولة دولية" }
}

private data class StudioUiTexts(
    val headerTitle: String,
    val liveNowCount: (Int) -> String,
    val leaguesCount: (Int) -> String,
    val searchHint: String,
    val allLeagues: String,
    val noLiveTitle: String,
    val noLiveSub: String,
    val quickChannelsTitle: String,
    val autoPlayBtn: String,
    val detailsBtn: String,
    val otherChannelsBtn: String,
    val detailsSheetTitle: String,
    val statusLabel: String,
    val leagueLabel: String,
    val autoChannelLabel: String,
    val netQualityLabel: String,
    val matchSummaryLabel: String,
    val liveFlowDesc: (String, String, String) -> String
)

private fun getStudioTexts(lang: String): StudioUiTexts = when (lang) {
    "en" -> StudioUiTexts(
        headerTitle = "Live Matches Studio",
        liveNowCount = { "$it Live Now" },
        leaguesCount = { "$it Leagues" },
        searchHint = "Search live team or league...",
        allLeagues = "All",
        noLiveTitle = "No Live Matches Right Now",
        noLiveSub = "Tap refresh or watch quick sports channels below.",
        quickChannelsTitle = "Quick Sports Channels",
        autoPlayBtn = "Smart Watch",
        detailsBtn = "Match Details",
        otherChannelsBtn = "Other Channels",
        detailsSheetTitle = "Live Match Details",
        statusLabel = "Match Status",
        leagueLabel = "Competition",
        autoChannelLabel = "Auto-Selected Channel",
        netQualityLabel = "Quality for Your Net",
        matchSummaryLabel = "Live Overview",
        liveFlowDesc = { h, a, s -> "Live match between $h and $a is currently underway ($s). Channel auto-matched to your language and internet speed." }
    )
    "fr" -> StudioUiTexts(
        headerTitle = "Studio Matchs en Direct",
        liveNowCount = { "$it En Direct" },
        leaguesCount = { "$it Ligues" },
        searchHint = "Rechercher une équipe ou ligue...",
        allLeagues = "Tout",
        noLiveTitle = "Aucun match en direct",
        noLiveSub = "Actualisez ou choisissez une chaîne sportive.",
        quickChannelsTitle = "Chaînes Sportives Rapides",
        autoPlayBtn = "Regarder",
        detailsBtn = "Détails",
        otherChannelsBtn = "Autres chaînes",
        detailsSheetTitle = "Détails du Match en Direct",
        statusLabel = "Statut",
        leagueLabel = "Compétition",
        autoChannelLabel = "Chaîne Auto-Sélectionnée",
        netQualityLabel = "Qualité selon Débit",
        matchSummaryLabel = "Résumé en Direct",
        liveFlowDesc = { h, a, s -> "Le match en direct entre $h et $a est en cours ($s). Chaîne choisie selon votre langue et connexion." }
    )
    "tr" -> StudioUiTexts(
        headerTitle = "Canlı Maç Stüdyosu",
        liveNowCount = { "$it Canlı Maç" },
        leaguesCount = { "$it Lig" },
        searchHint = "Takım veya lig ara...",
        allLeagues = "Tümü",
        noLiveTitle = "Şu An Canlı Maç Yok",
        noLiveSub = "Yenileyin veya hızlı spor kanallarını açın.",
        quickChannelsTitle = "Hızlı Spor Kanalları",
        autoPlayBtn = "Akıllı İzle",
        detailsBtn = "Maç Detayı",
        otherChannelsBtn = "Diğer Kanallar",
        detailsSheetTitle = "Canlı Maç Detayları",
        statusLabel = "Durum",
        leagueLabel = "Turnuva",
        autoChannelLabel = "Otomatik Seçilen Kanal",
        netQualityLabel = "Hıza Göre Kalite",
        matchSummaryLabel = "Maç Özeti",
        liveFlowDesc = { h, a, s -> "$h ve $a arasındaki canlı karşılaşma devam ediyor ($s). Kanal dilinize ve internet hızınıza göre seçildi." }
    )
    "de" -> StudioUiTexts(
        headerTitle = "Live-Spiele Studio",
        liveNowCount = { "$it Live Jetzt" },
        leaguesCount = { "$it Ligen" },
        searchHint = "Team oder Liga suchen...",
        allLeagues = "Alle",
        noLiveTitle = "Derzeit keine Live-Spiele",
        noLiveSub = "Aktualisieren oder Sportkanäle öffnen.",
        quickChannelsTitle = "Schnelle Sportkanäle",
        autoPlayBtn = "Smart Start",
        detailsBtn = "Spieldetails",
        otherChannelsBtn = "Andere Kanäle",
        detailsSheetTitle = "Live-Spieldetails",
        statusLabel = "Spielstatus",
        leagueLabel = "Wettbewerb",
        autoChannelLabel = "Automatischer Sender",
        netQualityLabel = "Netz-Qualität",
        matchSummaryLabel = "Spielverlauf",
        liveFlowDesc = { h, a, s -> "Das Live-Spiel zwischen $h und $a läuft aktuell ($s). Sender passend zu Sprache und Internet gewählt." }
    )
    else -> StudioUiTexts(
        headerTitle = "استوديو المباريات المباشرة",
        liveNowCount = { "$it جارية الآن" },
        leaguesCount = { "$it بطولة" },
        searchHint = "ابحث عن فريق أو بطولة مباشرة...",
        allLeagues = "الكل",
        noLiveTitle = "لا توجد مباريات مباشرة في هذه اللحظة",
        noLiveSub = "اضغط على تحديث أو تصفح القنوات الرياضية السريعة بالأعلى.",
        quickChannelsTitle = "⚡ القنوات الرياضية السريعة",
        autoPlayBtn = "مشاهدة ذكية",
        detailsBtn = "تفاصيل المباراة",
        otherChannelsBtn = "قنوات أخرى",
        detailsSheetTitle = "تفاصيل مجريات المباراة",
        statusLabel = "حالة المباراة",
        leagueLabel = "البطولة",
        autoChannelLabel = "القناة المختارة تلقائياً",
        netQualityLabel = "الجودة المناسبة لسرعة نتك",
        matchSummaryLabel = "ملخص المجريات المباشرة",
        liveFlowDesc = { h, a, s -> "تجري الآن مواجهة مباشرة بين $h و $a ($s). تم اختيار القناة الناقلة تلقائياً لتطابق لغتك وسرعة الإنترنت لديك بدون تقطيع." }
    )
}

private fun pickSmartChannelForMatch(
    match: RealSportsMatch,
    sportsChannels: List<MediaItemModel>,
    lang: String,
    speedTier: NetSpeedTier
): MediaItemModel? {
    if (sportsChannels.isEmpty()) return null

    val langKeywords = when (lang) {
        "ar", "ku" -> listOf("ar", "arab", "bein", "ssc", "alkass", "كأس", "كاس", "الرابعة", "rabiaa", "alwan", "الوان", "ألوان", "kuwait", "كويت", "ad sport", "أبو ظبي")
        "fr" -> listOf("fr", "france", "canal", "rmc", "bein sports fr", "eurosport fr")
        "tr" -> listOf("tr", "turkey", "bein sports tr", "s sport", "exxen", "trt spor")
        "de" -> listOf("de", "germany", "dazn", "sky sport de", "rtl")
        else -> listOf("uk", "us", "en", "sky sports", "tnt", "espn", "bt sport", "bein sports en")
    }

    val broadcasterHint = match.suggestedBroadcaster.substringBefore(" ").lowercase()

    return sportsChannels.maxByOrNull { ch ->
        var score = 0
        val combined = "${ch.group} ${ch.title}".lowercase()

        if (match.directChannelId.isNotEmpty() && ch.id == match.directChannelId) {
            score += 40
        }

        if (langKeywords.any { combined.contains(it) }) {
            score += 50
        }

        if (broadcasterHint.length >= 3 && combined.contains(broadcasterHint)) {
            score += 25
        }

        val qRank = LanguagePrioritySorter.qualityRank(ch.title)
        score += when (speedTier) {
            NetSpeedTier.ULTRA -> when (qRank) {
                4 -> 35
                3 -> 30
                2 -> 15
                else -> 5
            }
            NetSpeedTier.MEDIUM -> when (qRank) {
                2 -> 35
                3 -> 25
                1 -> 15
                else -> 5
            }
            NetSpeedTier.LOW -> when (qRank) {
                1 -> 40
                2 -> 20
                else -> 0
            }
        }

        score
    }
}

@Composable
private fun StudioLivePulseBadge(text: String) {
    val pulse by rememberInfiniteTransition(label = "live_pulse").animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFEF4444).copy(alpha = 0.18f))
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(Color(0xFFEF4444))
        )
        Text(
            text = text,
            color = Color(0xFFF87171),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.5.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun LiveMatchBroadcastCard(
    match: RealSportsMatch,
    autoChannel: MediaItemModel?,
    speedTier: NetSpeedTier,
    texts: StudioUiTexts,
    onSmartWatch: () -> Unit,
    onDetailsClick: () -> Unit,
    onOtherChannelsClick: () -> Unit
) {
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF151724), Color(0xFF10111A))
                )
            )
            .border(1.dp, Color(0xFF25283C), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cleanStudioLeagueName(match.league),
                color = Color(0xFF9CA3AF),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            StudioLivePulseBadge(text = match.status.ifEmpty { match.matchTime.ifEmpty { "LIVE" } })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (match.homeLogo.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(match.homeLogo).size(96, 96).build(),
                        contentDescription = match.homeTeam,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    text = match.homeTeam,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF212436))
                    .border(1.dp, gold.copy(alpha = 0.4f), RoundedCornerShape(9.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = match.scoreText.ifEmpty { "VS" },
                    color = goldLight,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = match.awayTeam,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                if (match.awayLogo.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(match.awayLogo).size(96, 96).build(),
                        contentDescription = match.awayTeam,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1D2C))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📺 ${autoChannel?.title ?: match.suggestedBroadcaster}",
                color = goldLight,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = speedTier.badge,
                color = Color(0xFF4ADE80),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onSmartWatch,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.weight(1.3f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text(texts.autoPlayBtn, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            OutlinedButton(
                onClick = onDetailsClick,
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(texts.detailsBtn, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            OutlinedButton(
                onClick = onOtherChannelsClick,
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(texts.otherChannelsBtn, color = goldLight, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevkanSportsStudioScreen(
    matches: List<RealSportsMatch>,
    quickSportsChannels: List<MediaItemModel>,
    lang: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSmartPlayChannel: (RealSportsMatch, MediaItemModel) -> Unit,
    onOpenChannelPicker: (RealSportsMatch) -> Unit,
    onQuickChannelClick: (MediaItemModel) -> Unit
) {
    val context = LocalContext.current
    val texts = remember(lang) { getStudioTexts(lang) }
    val speedTier = remember(matches.size) { detectNetworkSpeedTier(context) }

    val bgDark = Color(0xFF07070A)
    val cardBg = Color(0xFF12131C)
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    var searchQuery by remember { mutableStateOf("") }
    var selectedLeague by remember { mutableStateOf(texts.allLeagues) }
    var detailsMatch by remember { mutableStateOf<RealSportsMatch?>(null) }

    val liveOnlyMatches = remember(matches) {
        matches.filter { it.isLive }
    }

    val leagues = remember(liveOnlyMatches, texts.allLeagues) {
        listOf(texts.allLeagues) + liveOnlyMatches.map { cleanStudioLeagueName(it.league) }.distinct()
    }

    val filteredLiveMatches = remember(liveOnlyMatches, searchQuery, selectedLeague, texts.allLeagues) {
        val q = searchQuery.trim()
        liveOnlyMatches.filter { m ->
            val cleanLg = cleanStudioLeagueName(m.league)
            val leagueOk = selectedLeague == texts.allLeagues || cleanLg == selectedLeague
            val searchOk = q.isEmpty() ||
                m.homeTeam.contains(q, ignoreCase = true) ||
                m.awayTeam.contains(q, ignoreCase = true) ||
                cleanLg.contains(q, ignoreCase = true)
            leagueOk && searchOk
        }
    }

    val groupedMatches = remember(filteredLiveMatches) {
        filteredLiveMatches.groupBy { cleanStudioLeagueName(it.league) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "studio_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1A160B), Color(0xFF121420), Color(0xFF1E1015))
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
                                text = texts.headerTitle,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StudioLivePulseBadge(text = texts.liveNowCount(liveOnlyMatches.size))
                            Text(
                                text = "• ${texts.leaguesCount((leagues.size - 1).coerceAtLeast(0))}",
                                color = Color(0xFFB4B9C6),
                                fontSize = 11.5.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(14.dp))
                            Text(
                                text = if (lang == "ar" || lang == "ku") speedTier.labelAr else speedTier.labelEn,
                                color = Color(0xFF4ADE80),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
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
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = goldLight, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        if (quickSportsChannels.isNotEmpty()) {
            item(key = "quick_channels") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(texts.quickChannelsTitle, color = goldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickSportsChannels.take(18), key = { it.id }) { ch ->
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
                                        modifier = Modifier.size(22.dp).clip(RoundedCornerShape(5.dp))
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

        item(key = "studio_search") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(texts.searchHint, color = Color(0xFF7B8090), fontSize = 12.5.sp) },
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

        if (leagues.size > 1) {
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
        }

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
                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color(0xFF4B5563), modifier = Modifier.size(42.dp))
                    Text(texts.noLiveTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Text(texts.noLiveSub, color = Color(0xFF8B90A0), fontSize = 12.sp, textAlign = TextAlign.Center)
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
                            Box(modifier = Modifier.width(4.dp).height(16.dp).clip(CircleShape).background(gold))
                            Text(
                                text = leagueTitle,
                                color = goldLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        StudioLivePulseBadge(text = "${leagueMatches.size} LIVE")
                    }
                }

                itemsIndexed(leagueMatches, key = { idx, m -> "live_${leagueTitle}_${idx}_${m.id}" }) { _, match ->
                    val autoChannel = remember(match.id, quickSportsChannels.size, lang, speedTier) {
                        pickSmartChannelForMatch(match, quickSportsChannels, lang, speedTier)
                    }

                    LiveMatchBroadcastCard(
                        match = match,
                        autoChannel = autoChannel,
                        speedTier = speedTier,
                        texts = texts,
                        onSmartWatch = {
                            if (autoChannel != null) onSmartPlayChannel(match, autoChannel)
                            else onOpenChannelPicker(match)
                        },
                        onDetailsClick = { detailsMatch = match },
                        onOtherChannelsClick = { onOpenChannelPicker(match) }
                    )
                }
            }
        }
    }

    detailsMatch?.let { m ->
        val autoChannel = remember(m.id, quickSportsChannels.size, lang, speedTier) {
            pickSmartChannelForMatch(m, quickSportsChannels, lang, speedTier)
        }
        ModalBottomSheet(
            onDismissRequest = { detailsMatch = null },
            containerColor = Color(0xFF13141F)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(texts.detailsSheetTitle, color = goldLight, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    StudioLivePulseBadge(text = m.status.ifEmpty { "LIVE" })
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1B1D2C))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(m.homeTeam, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF26293B))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = m.scoreText.ifEmpty { "VS" },
                            color = goldLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                    Text(m.awayTeam, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${texts.leagueLabel}: ${cleanStudioLeagueName(m.league)}", color = Color(0xFFD0D4DF), fontSize = 12.5.sp)
                    Text("${texts.statusLabel}: ${m.status.ifEmpty { m.matchTime }}", color = Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${texts.autoChannelLabel}: ${autoChannel?.title ?: m.suggestedBroadcaster}",
                        color = goldLight,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${texts.netQualityLabel}: ${if (lang == "ar" || lang == "ku") speedTier.labelAr else speedTier.labelEn}",
                        color = Color(0xFF93C5FD),
                        fontSize = 12.sp
                    )
                    Text(texts.matchSummaryLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(
                        text = texts.liveFlowDesc(m.homeTeam, m.awayTeam, m.status.ifEmpty { m.matchTime }),
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val targetMatch = m
                            detailsMatch = null
                            if (autoChannel != null) onSmartPlayChannel(targetMatch, autoChannel)
                            else onOpenChannelPicker(targetMatch)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(texts.autoPlayBtn, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            val targetMatch = m
                            detailsMatch = null
                            onOpenChannelPicker(targetMatch)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(texts.otherChannelsBtn, color = goldLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}
