package com.hadedo.novaiptv

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                NovaIptvProApp()
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun NovaIptvProApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("nova_prefs", Context.MODE_PRIVATE) }

    // تبويبات التنقل: 0=السيرفرات، 1=مباشر، 2=أفلام، 3=مسلسلات
    var selectedTab by remember { mutableIntStateOf(0) }
    var serverMode by remember { mutableIntStateOf(0) } // 0=Xtream, 1=Stalker, 2=M3U

    // بيانات Xtream المحفوظة
    var xtHost by remember { mutableStateOf(prefs.getString("xt_host", "") ?: "") }
    var xtUser by remember { mutableStateOf(prefs.getString("xt_user", "") ?: "") }
    var xtPass by remember { mutableStateOf(prefs.getString("xt_pass", "") ?: "") }

    // بيانات Stalker المحفوظة
    var stPortal by remember { mutableStateOf(prefs.getString("st_portal", "") ?: "") }
    var stMac by remember { mutableStateOf(prefs.getString("st_mac", "00:1A:79:") ?: "00:1A:79:") }

    // بيانات M3U المحفوظة
    var m3uUrl by remember { mutableStateOf(prefs.getString("m3u_url", "https://iptv-org.github.io/iptv/languages/ara.m3u") ?: "") }

    var mediaList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMedia by remember { mutableStateOf<MediaItemModel?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("اختر نوع الاشتراك وأدخل بياناتك للاتصال") }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    fun playStream(item: MediaItemModel) {
        currentMedia = item
        exoPlayer.setMediaItem(MediaItem.fromUri(item.url))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    val filteredList = remember(mediaList, searchQuery) {
        if (searchQuery.isBlank()) mediaList
        else mediaList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1E293B)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.SettingsInputAntenna, null) },
                    label = { Text("السيرفرات") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        if (serverMode == 0 && xtHost.isNotEmpty()) {
                            scope.launch {
                                isLoading = true
                                mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "live")
                                isLoading = false
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.LiveTv, null) },
                    label = { Text("مباشر") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        if (serverMode == 0 && xtHost.isNotEmpty()) {
                            scope.launch {
                                isLoading = true
                                mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "vod")
                                isLoading = false
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Movie, null) },
                    label = { Text("أفلام") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                        if (serverMode == 0 && xtHost.isNotEmpty()) {
                            scope.launch {
                                isLoading = true
                                mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "series")
                                isLoading = false
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.VideoLibrary, null) },
                    label = { Text("مسلسلات") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F172A))
        ) {
            // مشغل الفيديو العلوي
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (currentMedia == null) {
                    Text(
                        text = currentMedia?.name ?: "NovaIPTV Pro - جاهز للبث",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            }

            if (selectedTab == 0) {
                // شاشة إدارة السيرفرات (Xtream / Stalker / M3U)
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = serverMode == 0,
                            onClick = { serverMode = 0 },
                            label = { Text("Xtream Codes") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = serverMode == 1,
                            onClick = { serverMode = 1 },
                            label = { Text("Stalker MAC") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = serverMode == 2,
                            onClick = { serverMode = 2 },
                            label = { Text("رابط M3U") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when (serverMode) {
                        0 -> {
                            OutlinedTextField(
                                value = xtHost,
                                onValueChange = { xtHost = it },
                                label = { Text("رابط السيرفر (http://domain:port)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = xtUser,
                                onValueChange = { xtUser = it },
                                label = { Text("اسم المستخدم (Username)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = xtPass,
                                onValueChange = { xtPass = it },
                                label = { Text("كلمة المرور (Password)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    prefs.edit()
                                        .putString("xt_host", xtHost)
                                        .putString("xt_user", xtUser)
                                        .putString("xt_pass", xtPass)
                                        .apply()
                                    scope.launch {
                                        isLoading = true
                                        mediaList = ServersEngine.loadXtream(xtHost, xtUser, xtPass, "live")
                                        isLoading = false
                                        if (mediaList.isNotEmpty()) selectedTab = 1
                                        else statusMessage = "فشل الاتصال بسيرفر Xtream، تأكد من البيانات"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ والاتصال بـ Xtream")
                            }
                        }

                        1 -> {
                            OutlinedTextField(
                                value = stPortal,
                                onValueChange = { stPortal = it },
                                label = { Text("رابط البورتال (http://portal.com/c)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = stMac,
                                onValueChange = { stMac = it },
                                label = { Text("عنوان الماك (00:1A:79:XX:XX:XX)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    prefs.edit()
                                        .putString("st_portal", stPortal)
                                        .putString("st_mac", stMac)
                                        .apply()
                                    scope.launch {
                                        isLoading = true
                                        mediaList = ServersEngine.loadStalker(stPortal, stMac)
                                        isLoading = false
                                        if (mediaList.isNotEmpty()) selectedTab = 1
                                        else statusMessage = "فشل الاتصال ببورتال Stalker"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ والاتصال بـ Stalker")
                            }
                        }

                        2 -> {
                            OutlinedTextField(
                                value = m3uUrl,
                                onValueChange = { m3uUrl = it },
                                label = { Text("رابط قائمة M3U / M3U8") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    prefs.edit().putString("m3u_url", m3uUrl).apply()
                                    scope.launch {
                                        isLoading = true
                                        val raw = M3uParser.loadFromUrl(m3uUrl)
                                        mediaList = raw.mapIndexed { idx, ch ->
                                            MediaItemModel("$idx", ch.name, ch.url, ch.group)
                                        }
                                        isLoading = false
                                        if (mediaList.isNotEmpty()) selectedTab = 1
                                        else statusMessage = "تعذر جلب قائمة M3U"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ وجلب القائمة")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        Text(text = statusMessage, color = Color(0xFF38BDF8), fontSize = 13.sp)
                    }
                }
            } else {
                // شاشة عرض القنوات / الأفلام / المسلسلات
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في (${filteredList.size}) عنصر...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredList) { item ->
                            val isPlaying = currentMedia?.url == item.url
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPlaying) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { playStream(item) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isPlaying) Color(0xFF38BDF8) else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = item.category,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp
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
