package com.hadedo.novaiptv

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
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
            .fillMaxWidth()
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
                color = Color(0xFFEF4444),
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
fun CinemaHorizontalRow(
    title: String,
    items: List<MediaItemModel>,
    onMoreClick: () -> Unit,
    onItemClick: (MediaItemModel) -> Unit
) {
    val context = LocalContext.current
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
    val savedPosMs = remember { DevkanVault.getVodPosition(context, movie.id) }

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
                val mins = (savedPosMs / 60000L).toInt()
                val secs = ((savedPosMs / 1000L) % 60L).toInt()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "وصلت إلى ${(savedProgress * 100).toInt()}% (الدقيقة ${String.format(Locale.US, "%02d:%02d", mins, secs)})",
                        color = Color(0xFFF87171),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = { savedProgress },
                        color = Color(0xFFEF4444),
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
                        text = if (savedProgress > 0.02f) "متابعة المشاهدة" else "شاهد الآن",
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
    onPlayEpisode: (MediaItemModel, String, Int, Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    var details by remember { mutableStateOf<SeriesDetailedInfo?>(null) }
    var selectedSeason by remember { mutableIntStateOf(1) }
    val lastWatchedInfo = remember(series.id) { DevkanVault.getLastWatchedEpisodeForSeries(context, series.id) }

    LaunchedEffect(series.id) {
        if (subscription != null && subscription.type == "XTREAM" && series.rawId.isNotEmpty()) {
            val fetched = DevkanPlaylistRepository.fetchSeriesDetails(subscription, series.rawId, series.title, series.logo)
            details = fetched
            if (lastWatchedInfo != null && fetched.seasons.containsKey(lastWatchedInfo.second)) {
                selectedSeason = lastWatchedInfo.second
            } else if (fetched.seasons.isNotEmpty()) {
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
            val resumeEpisode = lastWatchedInfo?.let { (epId, sNum, _) ->
                info.seasons[sNum]?.firstOrNull { it.id == epId }
            }

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
                    if (resumeEpisode != null) {
                        Text(
                            text = "الموسم ${resumeEpisode.seasonNum} • الحلقة ${resumeEpisode.episodeNum}",
                            color = Color(0xFFF87171),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Text(info.plot, color = Color(0xFFD1D5DB), fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val targetEp = resumeEpisode ?: episodes.firstOrNull()
                                if (targetEp != null) {
                                    onPlayEpisode(
                                        MediaItemModel(targetEp.id, targetEp.id, "${series.title} - م${targetEp.seasonNum} ح${targetEp.episodeNum}", targetEp.url, "", series.group, series.logo, "SERIES"),
                                        series.id,
                                        targetEp.seasonNum,
                                        targetEp.episodeNum
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (resumeEpisode != null) "متابعة الحلقة ${resumeEpisode.episodeNum}" else "شاهد الآن",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
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
                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        progress > 0.02f -> "واصل المشاهدة (${(progress * 100).toInt()}%)"
                        else -> ep.duration.ifEmpty { "جاهزة للمشاهدة" }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF13131B))
                            .border(1.dp, if (progress > 0.02f) Color(0xFFEF4444).copy(alpha = 0.55f) else Color(0xFF232332), RoundedCornerShape(14.dp))
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
                                    ),
                                    series.id,
                                    ep.seasonNum,
                                    ep.episodeNum
                                )
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.width(110.dp).height(66.dp).clip(RoundedCornerShape(10.dp))) {
                            if (series.logo.isNotEmpty()) {
                                AsyncImage(model = series.logo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                            Box(
                                modifier = Modifier.align(Alignment.Center).size(28.dp).background(Color(0xFFDC2626).copy(alpha = 0.9f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            if (progress > 0.02f) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = Color(0xFFEF4444),
                                    trackColor = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${series.title} • الحلقة ${ep.episodeNum}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(statusText, color = if (progress > 0.02f) Color(0xFFF87171) else Color.Gray, fontSize = 11.sp, fontWeight = if (progress > 0.02f) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionsManagerScreen(
    strings: AppStrings,
    activeSubId: String?,
    allItemsForCategories: List<MediaItemModel>,
    onActivateSub: (SavedSubscription) -> Unit,
    onEditSub: (SavedSubscription) -> Unit,
    onDeleteSubById: (String) -> Unit,
    onOpenAddDialog: () -> Unit,
    onCategoriesChanged: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    var subsList by remember { mutableStateOf(DevkanVault.getAllSubscriptions(context)) }
    var expandedMenuSubId by remember { mutableStateOf<String?>(null) }

    var testingSub by remember { mutableStateOf<SavedSubscription?>(null) }
    var testResult by remember { mutableStateOf<SubscriptionTestSummary?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    var diagnosticsSub by remember { mutableStateOf<SavedSubscription?>(null) }
    var visibleCatsSub by remember { mutableStateOf<SavedSubscription?>(null) }

    fun reloadList() {
        subsList = DevkanVault.getAllSubscriptions(context)
    }

    if (visibleCatsSub != null) {
        VisibleCategoriesScreen(
            sub = visibleCatsSub!!,
            allItems = allItemsForCategories,
            onBack = {
                visibleCatsSub = null
                onCategoriesChanged()
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("الاتصالات (${subsList.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            }
        }

        OutlinedButton(
            onClick = onOpenAddDialog,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, gold.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = goldLight)
            Spacer(Modifier.width(8.dp))
            Text("إضافة اتصال", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(subsList, key = { it.id }) { subItem ->
                val isCurrent = subItem.id == activeSubId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF13131D))
                        .border(1.dp, if (isCurrent) gold.copy(alpha = 0.6f) else Color(0xFF232334), RoundedCornerShape(16.dp))
                        .clickable {
                            if (!isCurrent) onActivateSub(subItem)
                        }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box {
                            IconButton(onClick = { expandedMenuSubId = subItem.id }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = expandedMenuSubId == subItem.id,
                                onDismissRequest = { expandedMenuSubId = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("تشخيص الشبكة / VPN", color = Color.White, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = goldLight) },
                                    onClick = {
                                        expandedMenuSubId = null
                                        diagnosticsSub = subItem
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("اختبار", color = Color.White, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF4ADE80)) },
                                    onClick = {
                                        expandedMenuSubId = null
                                        testingSub = subItem
                                        testResult = null
                                        isTesting = true
                                        scope.launch {
                                            val res = DevkanPlaylistRepository.testSubscriptionConnection(subItem, DevkanVault.isGeoBypassEnabled(context))
                                            testResult = res
                                            isTesting = false
                                            val updated = subItem.copy(
                                                accessStatus = res.accessStatus,
                                                serverCountryCode = res.countryCode.ifEmpty { subItem.serverCountryCode },
                                                serverIsp = res.ispName.ifEmpty { subItem.serverIsp },
                                                serverTimezone = res.timezone.ifEmpty { subItem.serverTimezone },
                                                expDateFormatted = if (res.success) res.expiry else subItem.expDateFormatted
                                            )
                                            DevkanVault.saveSubscription(context, updated, setAsActive = isCurrent)
                                            reloadList()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("الفئات المرئية", color = Color.White, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF60A5FA)) },
                                    onClick = {
                                        expandedMenuSubId = null
                                        visibleCatsSub = subItem
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("تعديل", color = Color.White, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF93C5FD)) },
                                    onClick = {
                                        expandedMenuSubId = null
                                        onEditSub(subItem)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("حذف", color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFF87171)) },
                                    onClick = {
                                        expandedMenuSubId = null
                                        onDeleteSubById(subItem.id)
                                        reloadList()
                                        Toast.makeText(context, "تم حذف الاشتراك (${subItem.label})", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("إعادة تعيين متابعة المشاهدة", color = Color(0xFFFBBF24), fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFFFBBF24)) },
                                    onClick = {
                                        expandedMenuSubId = null
                                        DevkanVault.resetWatchProgress(context)
                                        Toast.makeText(context, "تم تصفير سجل متابعة المشاهدة", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier.background(Color(0xFF16A34A).copy(alpha = 0.25f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("نشط", color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(subItem.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(
                                text = if (subItem.type == "XTREAM") "Xtream Codes" else if (subItem.type == "STALKER") "Stalker Portal" else "M3U Playlist",
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (subItem.serverCountryCode.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0xFF1F1F2E))
                                            .border(1.dp, Color(0xFF323248), RoundedCornerShape(50))
                                            .clickable { diagnosticsSub = subItem }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("🌐 ${subItem.serverCountryCode}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                val (badgeText, badgeBg, badgeFg) = when (subItem.accessStatus) {
                                    "BLOCKED_403" -> Triple("⚠️ البث محجوب", Color(0xFF7F1D1D).copy(alpha = 0.45f), Color(0xFFF87171))
                                    "VPN_REQUIRED" -> Triple("🔒 VPN مطلوب", Color(0xFF312E81).copy(alpha = 0.5f), Color(0xFFA5B4FC))
                                    "DNS_BYPASS" -> Triple("🛡️ تم تجاوز DNS", Color(0xFF134E4A).copy(alpha = 0.5f), Color(0xFF5EEAD4))
                                    else -> Triple("⚡ وصول مباشر", Color(0xFF14532D).copy(alpha = 0.5f), Color(0xFF4ADE80))
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(badgeBg)
                                        .clickable { diagnosticsSub = subItem }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(badgeText, color = badgeFg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF232038)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }

    if (testingSub != null) {
        AlertDialog(
            onDismissRequest = { testingSub = null },
            containerColor = Color(0xFF181822),
            title = {
                if (isTesting) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(color = gold, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("جاري فحص السيرفر...", color = Color.White, fontSize = 16.sp)
                    }
                } else {
                    val ok = testResult?.success == true
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (ok) Color(0xFF4ADE80) else Color(0xFFF87171)
                        )
                        Text(
                            text = if (ok) "تم الاتصال بنجاح" else "فشل الاتصال بالسيرفر",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }
            },
            text = {
                val res = testResult
                if (res != null) {
                    if (!res.success) {
                        Text(res.errorReason, color = Color(0xFFFCA5A5), fontSize = 13.sp, lineHeight = 20.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TestStatRow("الاسم", res.title)
                            TestStatRow("انتهاء الصلاحية", res.expiry)
                            TestStatRow("الشاشات المتزامنة", res.maxConnections)
                            TestStatRow("الاتصالات النشطة", res.activeConnections)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            TestStatRow("القنوات", "${res.liveCount}")
                            TestStatRow("الأفلام", "${res.movieCount}")
                            TestStatRow("المسلسلات", "${res.seriesCount}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { testingSub = null }) {
                    Text("إغلاق", color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    diagnosticsSub?.let { dSub ->
        val isBlocked = dSub.accessStatus == "BLOCKED_403" || dSub.accessStatus == "VPN_REQUIRED"
        AlertDialog(
            onDismissRequest = { diagnosticsSub = null },
            containerColor = Color(0xFF181822),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (isBlocked) Icons.Default.Warning else Icons.Default.Public,
                        contentDescription = null,
                        tint = if (isBlocked) Color(0xFFF87171) else Color(0xFFA78BFA)
                    )
                    Text(
                        text = if (isBlocked) "بث مقيد جغرافياً أو IP محظور" else "خادم المزود",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isBlocked) {
                        Text(
                            text = "لوحة المزود تستجيب، لكن بث الفيديو مرفوض (HTTP 403). هذا ليس حد الشاشات: إنه تقييد جغرافي أو أن IP محظور. عادة ما يحل VPN المشكلة.",
                            color = Color(0xFFD1D5DB),
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF222230))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("بلد الاستضافة: ${dSub.serverCountryCode.ifEmpty { "غير محدد" }}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("مزود الاستضافة: ${dSub.serverIsp.ifEmpty { "Cloud Server" }}", color = goldLight, fontSize = 12.sp)
                        Text("المنطقة الزمنية: ${dSub.serverTimezone.ifEmpty { "UTC" }}", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                        Text(
                            text = "تلميح لاختيار بلد VPN عندما يتطلب البث ذلك: جرّب هذا البلد أو بلداً مجاوراً أولاً.",
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { diagnosticsSub = null }) {
                    Text("موافق", color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun TestStatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, color = Color(0xFF9CA3AF), fontSize = 13.sp)
    }
}

@Composable
fun VisibleCategoriesScreen(
    sub: SavedSubscription,
    allItems: List<MediaItemModel>,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val goldLight = Color(0xFFFDE047)

    var selectedType by remember { mutableStateOf("LIVE") }
    var filterQuery by remember { mutableStateOf("") }

    val categoriesForType = remember(allItems, selectedType) {
        allItems.filter { it.contentType == selectedType }
            .map { it.group }
            .distinct()
            .sorted()
    }

    var hiddenSet by remember(sub.id, selectedType) {
        mutableStateOf(DevkanVault.getHiddenCategories(context, sub.id, selectedType))
    }

    val displayedCategories = remember(categoriesForType, filterQuery) {
        if (filterQuery.isBlank()) categoriesForType
        else categoriesForType.filter { it.contains(filterQuery.trim(), ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column {
                    Text("الفئات المرئية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(sub.label, color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("LIVE" to "مباشر", "MOVIE" to "أفلام", "SERIES" to "مسلسلات").forEach { (code, label) ->
                val active = selectedType == code
                TextButton(onClick = { selectedType = code }) {
                    Text(label, color = if (active) Color(0xFFA78BFA) else Color.Gray, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                }
            }
        }

        OutlinedTextField(
            value = filterQuery,
            onValueChange = { filterQuery = it },
            placeholder = { Text("تصفية الفئات (مثل AR أو ALWAN)...", fontSize = 12.sp) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val visibleCount = (categoriesForType.size - hiddenSet.size).coerceAtLeast(0)
            Text("$visibleCount / ${categoriesForType.size} مرئية", color = Color.Gray, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = {
                    hiddenSet = emptySet()
                    DevkanVault.setHiddenCategories(context, sub.id, selectedType, emptySet())
                }) {
                    Text("✓ الكل", color = goldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = {
                    val allHidden = categoriesForType.toSet()
                    hiddenSet = allHidden
                    DevkanVault.setHiddenCategories(context, sub.id, selectedType, allHidden)
                }) {
                    Text("✕ لا شيء", color = Color(0xFFF87171), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
            items(displayedCategories, key = { it }) { catName ->
                val isVisible = !hiddenSet.contains(catName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF13131C))
                        .clickable {
                            val updated = if (isVisible) hiddenSet + catName else hiddenSet - catName
                            hiddenSet = updated
                            DevkanVault.setHiddenCategories(context, sub.id, selectedType, updated)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(catName, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Checkbox(
                        checked = isVisible,
                        onCheckedChange = { checked ->
                            val updated = if (!checked) hiddenSet + catName else hiddenSet - catName
                            hiddenSet = updated
                            DevkanVault.setHiddenCategories(context, sub.id, selectedType, updated)
                        }
                    )
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
    onOpenConnectionsManager: () -> Unit,
    onRefreshSub: () -> Unit,
    onOpenAddDialog: () -> Unit,
    sampleTestUrl: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)

    var activeSheet by remember { mutableStateOf<String?>(null) }
    val allSubsCount = remember(subscription?.id) { DevkanVault.getAllSubscriptions(context).size }

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
        "fr" to "Français (الفرنسية)",
        "en" to "English (الإنجليزية)",
        "de" to "Deutsch (الألمانية)",
        "tr" to "Türkçe (التركية)",
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
                    Text("إدارة الاتصالات ($allSubsCount)", color = goldLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Button(
                    onClick = onOpenConnectionsManager,
                    colors = ButtonDefaults.buttonColors(containerColor = gold),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("فتح الاتصالات", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.expiryLabel, color = Color.Gray, fontSize = 12.sp)
                    Text(subscription.expDateFormatted, color = goldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRefreshSub, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = gold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تحديث الباقات", color = gold, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = onOpenAddDialog, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("إضافة جديد", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        SettingsRowCard(
            icon = Icons.Default.Language,
            title = "اللغة / Language (ترتيب القنوات التلقائي)",
            subtitle = languagesList.firstOrNull { it.first == lang }?.second ?: "العربية",
            onClick = { activeSheet = "LANG" }
        )

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
    var liveOnly by remember { mutableStateOf(editingSub?.liveOnly ?: false) }

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

    fun submitConnection() {
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
                password = password.trim(),
                liveOnly = liveOnly
            )
            "M3U" -> SavedSubscription(
                id = editingSub?.id ?: UUID.randomUUID().toString(),
                label = finalLabel,
                type = "M3U",
                host = m3uUrl.trim(),
                m3uUrl = m3uUrl.trim(),
                liveOnly = liveOnly
            )
            else -> SavedSubscription(
                id = editingSub?.id ?: UUID.randomUUID().toString(),
                label = finalLabel,
                type = "STALKER",
                host = host.trim(),
                macAddress = macAddress.trim(),
                liveOnly = liveOnly
            )
        }
        onSaveAndConnect(sub)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .imePadding()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF14141D))
                .border(1.dp, gold.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingSub != null) "تعديل الاشتراك" else strings.addConnection,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", color = Color.Gray, fontSize = 13.sp)
                    }
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = gold),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        onClick = { submitConnection() }
                    ) {
                        Text("حفظ واتصال", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .heightIn(max = 390.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(false to "الكل (مباشر + أفلام + مسلسلات)", true to "بث مباشر فقط (سريع)").forEach { (isLiveOnlyOpt, label) ->
                        val active = liveOnly == isLiveOnlyOpt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) Color(0xFF1E293B) else Color(0xFF181822))
                                .border(1.dp, if (active) Color(0xFF38BDF8) else Color(0xFF282838), RoundedCornerShape(10.dp))
                                .clickable { liveOnly = isLiveOnlyOpt }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (active) Color(0xFF38BDF8) else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, maxLines = 1)
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
        }
    }
}
