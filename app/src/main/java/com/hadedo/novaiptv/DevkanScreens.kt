package com.hadedo.novaiptv

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun MediaPosterCard(
    item: MediaItemModel,
    progress: Float,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF13131B))
            .border(1.dp, Color(0xFF232332), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box {
            if (item.logo.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.logo)
                        .size(320, 440)
                        .crossfade(false)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(165.dp).clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(165.dp).background(Color(0xFF1D1D29), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MovieFilter, contentDescription = null, tint = gold, modifier = Modifier.size(36.dp))
                }
            }
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(item.originTag, color = goldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (item.rating.isNotEmpty() && item.rating != "0") {
                    Box(
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("IMDb ${item.rating}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (progress > 0.02f) {
            LinearProgressIndicator(
                progress = { progress },
                color = gold,
                trackColor = Color(0xFF2B2B3C),
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
            )
        }

        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = if (item.year.isNotEmpty()) "${item.year} • ${item.genreTag}" else item.genreTag,
            color = Color(0xFF8E929F),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SmartEpgMatchCard(
    match: GroupedSportsMatch,
    onPlayPrimary: () -> Unit,
    onSelectQuality: () -> Unit
) {
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)
    val timeFormatted = remember(match.startTime) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(match.startTime))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF171626), Color(0xFF101522))))
            .border(1.dp, if (match.isLive) Color(0xFFEF4444).copy(alpha = 0.55f) else gold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable { onPlayPrimary() }
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = match.competition ?: "بث رياضي",
                color = goldLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .background(
                        if (match.isLive) Color(0xFFEF4444).copy(alpha = 0.22f) else Color(0xFF16A34A).copy(alpha = 0.22f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (match.isLive) "🔴 مباشر الآن" else "🟢 $timeFormatted",
                    color = if (match.isLive) Color(0xFFF87171) else Color(0xFF4ADE80),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = "${match.homeTeam}   🆚   ${match.awayTeam}",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📺 ${match.primaryStream.channelName}",
                color = Color(0xFF93C5FD),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (match.streams.size > 1) {
                Text(
                    text = "+${match.streams.size - 1} جودات ▾",
                    color = goldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(gold.copy(alpha = 0.16f))
                        .clickable { onSelectQuality() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun CinemaHorizontalRow(
    title: String,
    items: List<MediaItemModel>,
    onMoreClick: () -> Unit,
    onItemClick: (MediaItemModel) -> Unit
) {
    val context = LocalContext.current
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                text = "المزيد",
                color = goldLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onMoreClick() }.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                Column(
                    modifier = Modifier
                        .width(135.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF13131B))
                        .clickable { onItemClick(item) }
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box {
                        if (item.logo.isNotEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.logo)
                                    .size(270, 330)
                                    .build(),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(165.dp).clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(165.dp).background(Color(0xFF1D1D29), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Movie, contentDescription = null, tint = gold)
                            }
                        }
                        if (item.rating.isNotEmpty() && item.rating != "0") {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(5.dp)
                                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(5.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text("IMDb ${item.rating}", color = goldLight, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = if (item.year.isNotEmpty()) "${item.year} • ${item.genreTag}" else item.genreTag,
                        color = Color(0xFF9CA3AF),
                        fontSize = 10.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun MovieDetailsScreen(
    movie: MediaItemModel,
    subscription: SavedSubscription?,
    isInWatchlist: Boolean,
    onToggleWatchlist: () -> Unit,
    onPlayMovie: (MediaItemModel) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)
    var details by remember { mutableStateOf<VodDetailedInfo?>(null) }
    val savedProgress = remember { DevkanVault.getVodProgressFraction(context, movie.id) }

    LaunchedEffect(movie.id) {
        if (subscription != null && subscription.type == "XTREAM" && movie.rawId.isNotEmpty()) {
            details = DevkanPlaylistRepository.fetchVodDetails(subscription, movie.rawId, movie.title, movie.logo)
        } else {
            details = VodDetailedInfo(movie.title, "جاهز للمشاهدة الفورية بجودة عالية.", "غير متوفر", "غير متوفر", movie.year.ifEmpty { "2025" }, "HD", movie.rating.ifEmpty { "8.0" }, movie.logo)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070A))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        val info = details
        if (info == null) {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = gold)
            }
        } else {
            if (info.backdropUrl.isNotEmpty()) {
                AsyncImage(
                    model = info.backdropUrl,
                    contentDescription = info.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(18.dp))
                )
            }

            Text(info.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(gold.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("IMDb ⭐ ${info.rating}", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text("${info.releaseDate} • ${info.duration}", color = Color(0xFFB0B5C0), fontSize = 12.sp)
            }

            if (savedProgress > 0.02f) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("وصلت إلى ${(savedProgress * 100).toInt()}% من الفيلم", color = goldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { savedProgress },
                        color = gold,
                        trackColor = Color(0xFF252536),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onPlayMovie(movie) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (savedProgress > 0.02f) "إكمال المشاهدة" else "شاهد الآن",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }

                OutlinedButton(
                    onClick = onToggleWatchlist,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(if (isInWatchlist) Icons.Default.Check else Icons.Default.Add, contentDescription = null, tint = goldLight)
                    Spacer(Modifier.width(4.dp))
                    Text(if (isInWatchlist) "في القائمة" else "لاحقاً", color = Color.White, fontSize = 12.sp)
                }
            }

            Text(info.plot, color = Color(0xFFD1D5DB), fontSize = 13.sp, lineHeight = 20.sp)
            Text("الممثلون: ${info.cast}", color = Color(0xFF9CA3AF), fontSize = 12.sp)
            Text("المخرج: ${info.director}", color = Color(0xFF9CA3AF), fontSize = 12.sp)
        }
    }
}

@Composable
fun SeriesDetailsScreen(
    series: MediaItemModel,
    subscription: SavedSubscription?,
    isInWatchlist: Boolean,
    onToggleWatchlist: () -> Unit,
    onPlayEpisode: (MediaItemModel) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    var details by remember { mutableStateOf<SeriesDetailedInfo?>(null) }
    var selectedSeason by remember { mutableIntStateOf(1) }

    LaunchedEffect(series.id) {
        if (subscription != null && subscription.type == "XTREAM" && series.rawId.isNotEmpty()) {
            val fetched = DevkanPlaylistRepository.fetchSeriesDetails(subscription, series.rawId, series.title, series.logo)
            details = fetched
            if (fetched.seasons.isNotEmpty()) {
                selectedSeason = fetched.seasons.keys.first()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(series.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        val info = details
        if (info == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = gold)
            }
        } else {
            val episodes = info.seasons[selectedSeason] ?: emptyList()

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (info.backdropUrl.isNotEmpty()) {
                    AsyncImage(
                        model = info.backdropUrl,
                        contentDescription = info.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(110.dp).height(155.dp).clip(RoundedCornerShape(14.dp))
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("IMDb ⭐ ${info.rating}  •  ${info.releaseDate}", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(info.plot, color = Color(0xFFD1D5DB), fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val firstEp = episodes.firstOrNull()
                                if (firstEp != null) {
                                    onPlayEpisode(
                                        MediaItemModel(firstEp.id, firstEp.id, "${series.title} - م${firstEp.seasonNum} ح${firstEp.episodeNum}", firstEp.url, "", series.group, series.logo, "SERIES")
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("شاهد الآن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = onToggleWatchlist,
                            modifier = Modifier.size(38.dp).background(Color(0xFF1A1A26), CircleShape)
                        ) {
                            Icon(if (isInWatchlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isInWatchlist) Color(0xFFEF4444) else Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (info.seasons.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(info.seasons.keys.toList()) { seasonNum ->
                        val active = selectedSeason == seasonNum
                        FilterChip(
                            selected = active,
                            onClick = { selectedSeason = seasonNum },
                            label = {
                                Text(
                                    text = "الموسم $seasonNum",
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(episodes, key = { it.id }) { ep ->
                    val progress = remember(ep.id) { DevkanVault.getVodProgressFraction(context, ep.id) }
                    val statusText = when {
                        progress >= 0.90f -> "مكتملة ✓"
                        progress >= 0.45f -> "نصف الحلقة (${(progress * 100).toInt()}%)"
                        progress >= 0.15f -> "ربع الحلقة (${(progress * 100).toInt()}%)"
                        progress > 0.02f -> "قيد المشاهدة (${(progress * 100).toInt()}%)"
                        else -> ep.duration.ifEmpty { "جاهزة للمشاهدة" }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF13131B))
                            .border(1.dp, if (progress > 0.02f) gold.copy(alpha = 0.45f) else Color(0xFF232332), RoundedCornerShape(14.dp))
                            .clickable {
                                onPlayEpisode(
                                    MediaItemModel(
                                        id = ep.id,
                                        rawId = ep.id,
                                        title = "${series.title} - م${ep.seasonNum} ح${ep.episodeNum}",
                                        url = ep.url,
                                        group = series.group,
                                        logo = series.logo,
                                        contentType = "SERIES"
                                    )
                                )
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.width(105.dp).height(64.dp).clip(RoundedCornerShape(10.dp))) {
                            if (series.logo.isNotEmpty()) {
                                AsyncImage(model = series.logo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                            Box(
                                modifier = Modifier.align(Alignment.Center).size(28.dp).background(Color(0xFFDC2626).copy(alpha = 0.9f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${series.title} • الحلقة ${ep.episodeNum}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(statusText, color = if (progress > 0.02f) goldLight else Color.Gray, fontSize = 11.sp)
                            if (progress > 0.02f) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = gold,
                                    trackColor = Color(0xFF2B2B3C),
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevkanSettingsScreen(
    strings: AppStrings,
    lang: String,
    onLanguageChange: (String) -> Unit,
    subscription: SavedSubscription?,
    onSwitchSub: (SavedSubscription) -> Unit,
    onEditSub: (SavedSubscription) -> Unit,
    onRefreshSub: () -> Unit,
    onDeleteSub: () -> Unit,
    onOpenAddDialog: () -> Unit,
    sampleTestUrl: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    var activeSheet by remember { mutableStateOf<String?>(null) }
    val allSubs = remember(subscription?.id, subscription?.label) { DevkanVault.getAllSubscriptions(context) }

    var selectedEngine by remember { mutableStateOf(DevkanVault.getPreferredEngine(context)) }
    var selectedSyncInterval by remember { mutableStateOf(DevkanVault.getAutoSyncInterval(context)) }
    var selectedUserAgent by remember { mutableStateOf(DevkanVault.getUserAgent(context)) }
    var selectedDns by remember { mutableStateOf(DevkanVault.getDnsProvider(context)) }
    var lowLatency by remember { mutableStateOf(DevkanVault.isLowLatencyEnabled(context)) }
    var geoBypass by remember { mutableStateOf(DevkanVault.isGeoBypassEnabled(context)) }
    var autoResume by remember { mutableStateOf(DevkanVault.isAutoResumeEnabled(context)) }

    var pinInput by remember { mutableStateOf(DevkanVault.getAppLockPin(context)) }
    var speedResult by remember { mutableStateOf<String?>(null) }
    var testingSpeed by remember { mutableStateOf(false) }

    val languagesList = listOf(
        "ar" to "العربية (Arabic)",
        "en" to "English (الإنجليزية)",
        "de" to "Deutsch (الألمانية)",
        "tr" to "Türkçe (التركية)",
        "fr" to "Français (الفرنسية)",
        "ku" to "کوردی (الكردية)"
    )

    val userAgentsList = listOf(
        "IPTVSmartersPro/3.1.5 (Linux; Android 14)",
        "TiviMate/4.7.0 (Linux; Android 14)",
        "VLC/3.0.18 LibVLC/3.0.18",
        "okhttp/5.0.0-alpha.2",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1B170E), Color(0xFF111118))))
                .border(1.dp, gold.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = goldLight)
                    Text(strings.subscriptionCard, color = goldLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (allSubs.size > 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(gold.copy(alpha = 0.2f))
                                .clickable { activeSheet = "SUBS" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("تبديل (${allSubs.size}) ▾", color = goldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF222230))
                            .clickable { onOpenAddDialog() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("+ جديد", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (subscription != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${subscription.label} • ${subscription.type}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Box(
                        modifier = Modifier.background(Color(0xFF16A34A).copy(alpha = 0.22f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(subscription.status, color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.expiryLabel, color = Color.Gray, fontSize = 12.sp)
                    Text(subscription.expDateFormatted, color = goldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.connectionsLabel, color = Color.Gray, fontSize = 12.sp)
                    Text("${subscription.activeCons} / ${subscription.maxCons}", color = Color.White, fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onRefreshSub, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = gold, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("تحديث", color = gold, fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(onClick = { onEditSub(subscription) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("تعديل", color = Color(0xFF60A5FA), fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(onClick = onDeleteSub, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("حذف", color = Color(0xFFF87171), fontSize = 11.sp, maxLines = 1)
                    }
                }
            } else {
                Text("لا يوجد اشتراك محفوظ حالياً", color = Color.Gray, fontSize = 12.sp)
                Button(onClick = onOpenAddDialog, colors = ButtonDefaults.buttonColors(containerColor = gold), modifier = Modifier.fillMaxWidth()) {
                    Text(strings.addConnection, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        SettingsRowCard(
            icon = Icons.Default.Dns,
            title = "مزود DNS (تجاوز حظر المزود)",
            subtitle = selectedDns.label,
            onClick = { activeSheet = "DNS" }
        )

        SettingsRowCard(
            icon = Icons.Default.PermDeviceInformation,
            title = "User-Agent (هوية المشغل)",
            subtitle = selectedUserAgent.substringBefore(" "),
            onClick = { activeSheet = "UA" }
        )

        SettingsRowCard(
            icon = Icons.Default.SettingsInputComponent,
            title = strings.engineTitle,
            subtitle = when (lang) {
                "en" -> selectedEngine.labelEn
                "de" -> selectedEngine.labelDe
                "tr" -> selectedEngine.labelTr
                else -> selectedEngine.labelAr
            },
            onClick = { activeSheet = "ENGINE" }
        )

        SettingsRowCard(
            icon = Icons.Default.Autorenew,
            title = strings.autoSyncTitle,
            subtitle = if (lang == "ar" || lang == "ku") selectedSyncInterval.labelAr else selectedSyncInterval.labelEn,
            onClick = { activeSheet = "SYNC" }
        )

        SettingsRowCard(
            icon = Icons.Default.Language,
            title = "اللغة / Language",
            subtitle = languagesList.firstOrNull { it.first == lang }?.second ?: "العربية",
            onClick = { activeSheet = "LANG" }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF13131B))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(strings.lowLatencyTitle, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = lowLatency, onCheckedChange = {
                    lowLatency = it
                    DevkanVault.setLowLatencyEnabled(context, it)
                })
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(strings.geoBypassTitle, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = geoBypass, onCheckedChange = {
                    geoBypass = it
                    DevkanVault.setGeoBypassEnabled(context, it)
                })
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(strings.autoResumeTitle, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = autoResume, onCheckedChange = {
                    autoResume = it
                    DevkanVault.setAutoResumeEnabled(context, it)
                })
            }
        }

        Button(
            onClick = {
                testingSpeed = true
                scope.launch {
                    val (ping, mbps) = EngineCoordinator.runServerSpeedAndPingTest(sampleTestUrl)
                    speedResult = "Ping: ${ping} ms   •   Speed: ${mbps} Mbps"
                    testingSpeed = false
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = gold),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Text(if (testingSpeed) "جاري الفحص..." else strings.speedTest, color = Color.Black, fontWeight = FontWeight.Bold)
        }
        speedResult?.let {
            Text(it, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF13131B))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(strings.appLockTitle, color = gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6) pinInput = it },
                    placeholder = { Text("4-6 digits (فارغ = معطل)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        DevkanVault.setAppLockPin(context, pinInput)
                        Toast.makeText(context, if (pinInput.isEmpty()) "تم إيقاف القفل" else "تم حفظ رمز القفل", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = gold)
                ) {
                    Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (activeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            containerColor = Color(0xFF14141E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (activeSheet) {
                    "SUBS" -> {
                        Text("اختر الاشتراك النشط", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        allSubs.forEach { subItem ->
                            val isCurrent = subItem.id == subscription?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isCurrent) gold.copy(alpha = 0.2f) else Color(0xFF1C1C28))
                                    .clickable {
                                        activeSheet = null
                                        if (!isCurrent) onSwitchSub(subItem)
                                    }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(subItem.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${subItem.type} • ${subItem.expDateFormatted}", color = Color.Gray, fontSize = 11.sp)
                                }
                                if (isCurrent) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = goldLight)
                            }
                        }
                    }
                    "DNS" -> {
                        Text("اختر مزود DNS", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        DnsProvider.entries.forEach { dns ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selectedDns == dns) gold.copy(alpha = 0.2f) else Color(0xFF1C1C28))
                                    .clickable {
                                        selectedDns = dns
                                        DevkanVault.setDnsProvider(context, dns)
                                        activeSheet = null
                                    }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(dns.label, color = Color.White, fontSize = 14.sp)
                                if (selectedDns == dns) Icon(Icons.Default.Check, contentDescription = null, tint = goldLight)
                            }
                        }
                    }
                    "UA" -> {
                        Text("اختر User-Agent", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        userAgentsList.forEach { ua ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selectedUserAgent == ua) gold.copy(alpha = 0.2f) else Color(0xFF1C1C28))
                                    .clickable {
                                        selectedUserAgent = ua
                                        DevkanVault.setUserAgent(context, ua)
                                        activeSheet = null
                                    }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(ua, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                if (selectedUserAgent == ua) Icon(Icons.Default.Check, contentDescription = null, tint = goldLight)
                            }
                        }
                    }
                    "ENGINE" -> {
                        Text(strings.engineTitle, color = goldLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        EngineType.entries.forEach { eng ->
                            val label = when (lang) {
                                "en" -> eng.labelEn
                                "de" -> eng.labelDe
                                "tr" -> eng.labelTr
                                else -> eng.labelAr
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selectedEngine == eng) gold.copy(alpha = 0.2f) else Color(0xFF1C1C28))
                                    .clickable {
                                        selectedEngine = eng
                                        DevkanVault.savePreferredEngine(context, eng)
                                        activeSheet = null
                                    }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, color = Color.White, fontSize = 13.sp)
                                if (selectedEngine == eng) Icon(Icons.Default.Check, contentDescription = null, tint = goldLight)
                            }
                        }
                    }
                    "SYNC" -> {
                        Text(strings.autoSyncTitle, color = goldLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        AutoSyncInterval.entries.forEach { interval ->
                            val label = if (lang == "ar" || lang == "ku") interval.labelAr else interval.labelEn
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selectedSyncInterval == interval) gold.copy(alpha = 0.2f) else Color(0xFF1C1C28))
                                    .clickable {
                                        selectedSyncInterval = interval
                                        DevkanVault.setAutoSyncInterval(context, interval)
                                        activeSheet = null
                                    }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, color = Color.White, fontSize = 14.sp)
                                if (selectedSyncInterval == interval) Icon(Icons.Default.Check, contentDescription = null, tint = goldLight)
                            }
                        }
                    }
                    "LANG" -> {
                        Text("اللغة / Language", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        languagesList.forEach { (code, title) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (lang == code) gold.copy(alpha = 0.2f) else Color(0xFF1C1C28))
                                    .clickable {
                                        onLanguageChange(code)
                                        activeSheet = null
                                    }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(title, color = Color.White, fontSize = 14.sp)
                                if (lang == code) Icon(Icons.Default.Check, contentDescription = null, tint = goldLight)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF13131B))
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = gold)
            Column {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = goldLight, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = gold)
    }
}

@Composable
fun SmartAddConnectionDialog(
    strings: AppStrings,
    editingSub: SavedSubscription? = null,
    onDismiss: () -> Unit,
    onSaveAndConnect: (SavedSubscription) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val gold = Color(0xFFD4AF37)

    var selectedMode by remember { mutableStateOf(editingSub?.type ?: "XTREAM") }
    var subLabel by remember { mutableStateOf(editingSub?.label ?: "") }
    var smartPasteInput by remember { mutableStateOf("") }
    var autoDetectedBanner by remember { mutableStateOf(false) }

    var host by remember { mutableStateOf(editingSub?.host ?: "") }
    var username by remember { mutableStateOf(editingSub?.username ?: "") }
    var password by remember { mutableStateOf(editingSub?.password ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    var m3uUrl by remember { mutableStateOf(editingSub?.m3uUrl ?: "") }
    var macAddress by remember { mutableStateOf(editingSub?.macAddress ?: "00:1A:79:") }

    fun parseXtreamOnly(raw: String) {
        smartPasteInput = raw
        if (selectedMode != "XTREAM") return
        try {
            val uri = Uri.parse(raw.trim())
            val u = uri.getQueryParameter("username")
            val p = uri.getQueryParameter("password")
            val scheme = uri.scheme
            val auth = uri.authority
            if (!u.isNullOrEmpty() && !p.isNullOrEmpty() && !scheme.isNullOrEmpty() && !auth.isNullOrEmpty()) {
                host = "$scheme://$auth"
                username = u
                password = p
                if (subLabel.isEmpty()) subLabel = u
                autoDetectedBanner = true
            }
        } catch (_: Exception) { }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141D),
        title = {
            Text(
                text = if (editingSub != null) "تعديل الاشتراك" else strings.addConnection,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("XTREAM" to "Xtream Codes", "M3U" to "قائمة M3U", "STALKER" to "Stalker MAC").forEach { (mode, label) ->
                        val active = selectedMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) gold else Color(0xFF1F1F2C))
                                .clickable {
                                    selectedMode = mode
                                    autoDetectedBanner = false
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (active) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                OutlinedTextField(
                    value = subLabel,
                    onValueChange = { subLabel = it },
                    label = { Text("اسم الاشتراك (اختياري)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                when (selectedMode) {
                    "XTREAM" -> {
                        OutlinedTextField(
                            value = smartPasteInput,
                            onValueChange = { parseXtreamOnly(it) },
                            singleLine = true,
                            label = { Text("لصق رابط Xtream للتعبئة التلقائية", fontSize = 11.sp) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val clip = clipboardManager.getText()?.text ?: ""
                                    if (clip.isNotEmpty()) parseXtreamOnly(clip)
                                }) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = gold)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (autoDetectedBanner) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(16.dp))
                                Text("تم اكتشاف بيانات Xtream تلقائياً", color = Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("رابط الخادم (http://server:port)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("اسم المستخدم") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة المرور") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "M3U" -> {
                        OutlinedTextField(value = m3uUrl, onValueChange = { m3uUrl = it }, label = { Text("رابط M3U / M3U8 (إدخال يدوي)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    "STALKER" -> {
                        OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("رابط البورتال (http://portal.com/c)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = macAddress, onValueChange = { macAddress = it }, label = { Text("عنوان MAC (إدخال يدوي)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = gold),
                onClick = {
                    val finalLabel = subLabel.trim().ifEmpty {
                        when (selectedMode) {
                            "XTREAM" -> username.trim().ifEmpty { "Xtream VIP" }
                            "STALKER" -> "Stalker MAC"
                            else -> "M3U Playlist"
                        }
                    }
                    val sub = when (selectedMode) {
                        "XTREAM" -> SavedSubscription(
                            id = editingSub?.id ?: UUID.randomUUID().toString(),
                            label = finalLabel,
                            type = "XTREAM",
                            host = host.trim(),
                            username = username.trim(),
                            password = password.trim()
                        )
                        "M3U" -> SavedSubscription(
                            id = editingSub?.id ?: UUID.randomUUID().toString(),
                            label = finalLabel,
                            type = "M3U",
                            host = m3uUrl.trim(),
                            m3uUrl = m3uUrl.trim()
                        )
                        else -> SavedSubscription(
                            id = editingSub?.id ?: UUID.randomUUID().toString(),
                            label = finalLabel,
                            type = "STALKER",
                            host = host.trim(),
                            macAddress = macAddress.trim()
                        )
                    }
                    onSaveAndConnect(sub)
                }
            ) {
                Text("حفظ واتصال", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = Color.Gray) }
        }
    )
}
