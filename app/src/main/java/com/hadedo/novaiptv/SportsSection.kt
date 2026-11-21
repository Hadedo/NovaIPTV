package com.hadedo.novaiptv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object SportsConfig {
    const val SPORTS_API_URL = ""
    const val SPORTS_API_KEY = ""
    const val SPORTS_API_KEY_HEADER = "X-Api-Key"
}

data class SportsMatch(
    val league: String,
    val home: String,
    val away: String,
    val timeOrScore: String,
    val isLive: Boolean,
    val channel: String
)

sealed class SportsState {
    object Loading : SportsState()
    object NoKey : SportsState()
    object Empty : SportsState()
    data class Error(val msg: String) : SportsState()
    data class Ready(val matches: List<SportsMatch>) : SportsState()
}

object SportsRepository {
    suspend fun fetchToday(): SportsState = withContext(Dispatchers.IO) {
        if (SportsConfig.SPORTS_API_URL.isBlank() || SportsConfig.SPORTS_API_KEY.isBlank()) {
            return@withContext SportsState.NoKey
        }
        try {
            val conn = (URL(SportsConfig.SPORTS_API_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty(SportsConfig.SPORTS_API_KEY_HEADER, SportsConfig.SPORTS_API_KEY)
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode !in 200..299) return@withContext SportsState.Error("HTTP ${conn.responseCode}")
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val list = parseMatches(body)
                if (list.isEmpty()) SportsState.Empty else SportsState.Ready(list)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            SportsState.Error(e.message ?: "connection")
        }
    }

    private fun parseMatches(body: String): List<SportsMatch> {
        val arr: JSONArray = JSONObject(body).optJSONArray("matches") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val hasScore = o.has("home_score") && !o.isNull("home_score") &&
                o.has("away_score") && !o.isNull("away_score")
            val status = o.optString("status", "")
            SportsMatch(
                league = o.optString("league", ""),
                home = o.optString("home", ""),
                away = o.optString("away", ""),
                timeOrScore = if (hasScore) "${o.optInt("home_score")} - ${o.optInt("away_score")}" else o.optString("time", "--:--"),
                isLive = status.equals("LIVE", true),
                channel = o.optString("channel", "")
            )
        }
    }
}

@Composable
private fun SportsMessageCard(icon: ImageVector, title: String, sub: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF12141F))
            .border(1.dp, Color(0xFF202436), RoundedCornerShape(16.dp))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(32.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
        Text(sub, color = Color(0xFF9CA3AF), fontSize = 11.5.sp, textAlign = TextAlign.Center, lineHeight = 17.sp)
    }
}

@Composable
private fun SportsMatchCard(
    match: SportsMatch,
    liveChannels: List<MediaItemModel>,
    onPlayChannel: (MediaItemModel) -> Unit
) {
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFDE047)
    val linked = remember(match.channel, liveChannels) {
        if (match.channel.isBlank()) null
        else liveChannels.firstOrNull { it.title.contains(match.channel, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF131520))
            .border(1.dp, if (match.isLive) Color(0xFFDC2626).copy(alpha = 0.6f) else Color(0xFF212436), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(match.league, color = goldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (match.isLive) {
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFDC2626)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("LIVE", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(match.home, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Box(
                    Modifier.padding(horizontal = 8.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1D2030)).padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(match.timeOrScore, color = if (match.isLive) Color(0xFF4ADE80) else goldLight, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                Text(match.away, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }

        if (match.channel.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1C2A))
                    .then(if (linked != null) Modifier.clickable { onPlayChannel(linked) } else Modifier)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = gold, modifier = Modifier.size(16.dp))
                Text(match.channel, color = Color(0xFFCBD5E1), fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (linked != null) Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = gold, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
fun SportsMatchesSection(
    liveChannels: List<MediaItemModel>,
    onPlayChannel: (MediaItemModel) -> Unit
) {
    var refreshKey by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<SportsState>(SportsState.Loading) }

    LaunchedEffect(refreshKey) {
        state = SportsState.Loading
        state = SportsRepository.fetchToday()
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(22.dp))
                Text("مباريات اليوم", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            IconButton(
                onClick = { refreshKey++ },
                modifier = Modifier.size(34.dp).background(Color(0xFF171723), CircleShape)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFFFDE047), modifier = Modifier.size(18.dp))
            }
        }

        when (val s = state) {
            SportsState.Loading -> Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFD4AF37), strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
            }
            SportsState.NoKey -> SportsMessageCard(
                Icons.Default.VpnKey, "مصدر المباريات غير مفعّل",
                "أضف SPORTS_API_URL و SPORTS_API_KEY في SportsConfig لعرض نتائج المباريات المباشرة."
            )
            SportsState.Empty -> SportsMessageCard(Icons.Default.EventBusy, "لا توجد مباريات اليوم", "جرّب التحديث لاحقاً.")
            is SportsState.Error -> SportsMessageCard(Icons.Default.WifiOff, "تعذّر تحميل المباريات", "تحقق من الاتصال أو صلاحية المفتاح ثم اضغط تحديث.")
            is SportsState.Ready -> s.matches.forEach { SportsMatchCard(it, liveChannels, onPlayChannel) }
        }
        Spacer(Modifier.height(8.dp))
    }
}
