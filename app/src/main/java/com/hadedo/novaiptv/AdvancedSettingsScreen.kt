package com.hadedo.novaiptv

import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AdvancedSettingsScreen(
    prefs: SharedPreferences,
    servers: List<SavedServer>,
    activeServerId: String,
    activeServer: SavedServer?,
    appLang: String,
    onAppLangChange: (String) -> Unit,
    accentIdx: Int,
    accentOptions: List<Pair<String, Color>>,
    onAccentChange: (Int) -> Unit,
    bgIdx: Int,
    bgOptions: List<Triple<String, Color, Color>>,
    onBgChange: (Int) -> Unit,
    startupTab: Int,
    onStartupTabChange: (Int) -> Unit,
    decoderMode: String,
    onDecoderChange: (String) -> Unit,
    bufferProfile: String,
    onBufferChange: (String) -> Unit,
    streamFmt: String,
    onStreamFmtChange: (String) -> Unit,
    sameHost: Boolean,
    onSameHostChange: (Boolean) -> Unit,
    userAgent: String,
    onUserAgentChange: (String) -> Unit,
    gesturesEnabled: Boolean,
    onGesturesChange: (Boolean) -> Unit,
    autoplayNextEp: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    subFontSize: Float,
    onSubFontSizeChange: (Float) -> Unit,
    subColorYellow: Boolean,
    onSubColorYellowChange: (Boolean) -> Unit,
    sleepMinutesLeft: Int,
    onSleepMinutesChange: (Int) -> Unit,
    parentalPin: String,
    onParentalPinChange: (String) -> Unit,
    accentColor: Color,
    cardColor: Color,
    onSelectServer: (SavedServer) -> Unit,
    onAddServerClick: () -> Unit,
    onEditServerClick: (SavedServer) -> Unit,
    onDeleteServerClick: (SavedServer) -> Unit,
    onOpenCategoriesManager: () -> Unit,
    onOpenBackupDialog: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val greenText = Color(0xFF4ADE80)

    var speedResult by remember { mutableStateOf("") }
    var customUaInput by remember { mutableStateOf(userAgent) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var cacheSizeLabel by remember { mutableStateOf(AppStorage.getCacheSizeMb(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. قسم الاشتراكات والمصادر
        SettingsSectionCard(
            title = "الاشتراكات والمصادر (${servers.size})",
            icon = Icons.Default.Dns,
            accentColor = accentColor,
            cardColor = cardColor,
            trailingAction = {
                Text(
                    "+ إضافة اشتراك",
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onAddServerClick() }
                )
            }
        ) {
            if (servers.isEmpty()) {
                Text("لا توجد اشتراكات مضافة حالياً.", color = Color.Gray, fontSize = 12.sp)
            } else {
                servers.forEach { srv ->
                    val isAct = srv.id == activeServerId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isAct) accentColor.copy(alpha = 0.14f) else Color(0xFF0A0A0F), RoundedCornerShape(12.dp))
                            .clickable { onSelectServer(srv) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(srv.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${srv.countryCode} • ${srv.type.uppercase()} • ⚡ ${srv.pingMs}ms", color = greenText, fontSize = 11.sp)
                        }
                        IconButton(onClick = { onEditServerClick(srv) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDeleteServerClick(srv) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenCategoriesManager, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.VisibilityOff, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إخفاء الباقات", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = {
                        activeServer?.let { srv ->
                            speedResult = "جاري فحص سرعة السيرفر..."
                            scope.launch { speedResult = ServersEngine.runSpeedTest(srv.host) }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Speed, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فحص السيرفر", fontSize = 11.sp)
                }
            }
            if (speedResult.isNotBlank()) {
                Text(speedResult, color = greenText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 2. قسم محرك المشغل والفيديو
        SettingsSectionCard(
            title = "محرك المشغل والبث المباشر",
            icon = Icons.Default.PlayCircleFilled,
            accentColor = accentColor,
            cardColor = cardColor
        ) {
            Text("معالج فك الترميز (Video Decoder):", color = Color.LightGray, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("hw" to "Hardware (4K GPU)", "auto" to "تلقائي", "sw" to "Software").forEach { (k, lbl) ->
                    FilterChip(
                        selected = decoderMode == k,
                        onClick = { onDecoderChange(k) },
                        label = { Text(lbl, fontSize = 10.sp) }
                    )
                }
            }

            Text("ذاكرة التخزين المؤقت (Buffer Profile):", color = Color.LightGray, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("fast" to "صاروخ (0.4ث)", "balanced" to "متوازن", "antifreeze" to "مضاد التقطيع").forEach { (k, lbl) ->
                    FilterChip(
                        selected = bufferProfile == k,
                        onClick = { onBufferChange(k) },
                        label = { Text(lbl, fontSize = 10.sp) }
                    )
                }
            }

            Text("صيغة البث الافتراضية (Stream Format):", color = Color.LightGray, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = streamFmt == "ts", onClick = { onStreamFmtChange("ts") }, label = { Text("MPEG-TS (الأسرع)") })
                FilterChip(selected = streamFmt == "m3u8", onClick = { onStreamFmtChange("m3u8") }, label = { Text("HLS / M3U8") })
            }

            SettingsSwitchRow("توحيد رابط البث (Same Host Proxy)", sameHost, onSameHostChange)
            SettingsSwitchRow("إيماءات اللمس للصوت والسطوع", gesturesEnabled, onGesturesChange)
            SettingsSwitchRow("تشغيل الحلقة التالية تلقائياً", autoplayNextEp, onAutoplayChange)
        }

        // 3. قسم الترجمة ومؤقت النوم
        SettingsSectionCard(
            title = "الترجمة ومؤقت النوم",
            icon = Icons.Default.Subtitles,
            accentColor = accentColor,
            cardColor = cardColor
        ) {
            Text("حجم خط الترجمة: ${subFontSize.toInt()}sp", color = Color.LightGray, fontSize = 12.sp)
            Slider(
                value = subFontSize,
                onValueChange = onSubFontSizeChange,
                valueRange = 14f..28f,
                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
            )
            SettingsSwitchRow("لون الترجمة أصفر سينمائي", subColorYellow, onSubColorYellowChange)

            Text("مؤقت النوم التلقائي: ${if (sleepMinutesLeft > 0) "بعد $sleepMinutesLeft دقيقة" else "متوقف"}", color = Color.LightGray, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "إيقاف", 15 to "15 د", 30 to "30 د", 60 to "60 د").forEach { (m, lbl) ->
                    FilterChip(selected = sleepMinutesLeft == m, onClick = { onSleepMinutesChange(m) }, label = { Text(lbl, fontSize = 10.sp) })
                }
            }
        }

        // 4. قسم المظهر والواجهة
        SettingsSectionCard(
            title = "المظهر والواجهة واللغة",
            icon = Icons.Default.Palette,
            accentColor = accentColor,
            cardColor = cardColor
        ) {
            Text("لغة التطبيق (App Language):", color = Color.LightGray, fontSize = 12.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val langs = listOf("ar" to "العربية", "en" to "English", "fr" to "Français", "tr" to "Türkçe", "ku" to "کوردی")
                items(langs) { (code, label) ->
                    FilterChip(selected = appLang == code, onClick = { onAppLangChange(code) }, label = { Text(label, fontSize = 11.sp) })
                }
            }

            Text("شاشة البداية عند فتح التطبيق:", color = Color.LightGray, fontSize = 12.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val tabs = listOf(0 to "الرئيسية", 1 to "مباشر", 2 to "الرياضة", 3 to "السينما")
                items(tabs) { (idx, lbl) ->
                    FilterChip(selected = startupTab == idx, onClick = { onStartupTabChange(idx) }, label = { Text(lbl, fontSize = 11.sp) })
                }
            }

            Text("لون التمييز النيوني:", color = Color.LightGray, fontSize = 12.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(accentOptions.indices.toList()) { idx ->
                    FilterChip(selected = accentIdx == idx, onClick = { onAccentChange(idx) }, label = { Text(accentOptions[idx].first, fontSize = 10.sp) })
                }
            }

            Text("خلفية التطبيق:", color = Color.LightGray, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                bgOptions.indices.forEach { idx ->
                    FilterChip(selected = bgIdx == idx, onClick = { onBgChange(idx) }, label = { Text(bgOptions[idx].first, fontSize = 10.sp) })
                }
            }
        }

        // 5. قسم الهوية والحماية والنسخ الاحتياطي
        SettingsSectionCard(
            title = "الهوية والحماية والنسخ الاحتياطي",
            icon = Icons.Default.Security,
            accentColor = accentColor,
            cardColor = cardColor
        ) {
            Text("هوية المشغل (User-Agent):", color = Color.LightGray, fontSize = 12.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val agents = listOf("IPTVSmartersPro", "TiviMate/4.7.0", "VLC/3.0.18", "VU IPTV Player", "Mozilla/5.0")
                items(agents) { ua ->
                    FilterChip(
                        selected = userAgent == ua,
                        onClick = { customUaInput = ua; onUserAgentChange(ua) },
                        label = { Text(ua, fontSize = 10.sp) }
                    )
                }
            }
            OutlinedTextField(
                value = customUaInput,
                onValueChange = { customUaInput = it; onUserAgentChange(it) },
                label = { Text("User-Agent مخصص", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showChangePinDialog = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("رمز الأطفال ($parentalPin)", fontSize = 11.sp)
                }
                OutlinedButton(onClick = onOpenBackupDialog, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Backup, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ احتياطي", fontSize = 11.sp)
                }
            }

            Button(
                onClick = {
                    AppStorage.clearAllMediaCache(context)
                    cacheSizeLabel = AppStorage.getCacheSizeMb(context)
                    Toast.makeText(context, "تم تنظيف الذاكرة المؤقتة بنجاح", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272736)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CleaningServices, null, tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تنظيف الذاكرة المؤقتة ($cacheSizeLabel)", color = Color.White, fontSize = 12.sp)
            }
        }
    }

    if (showChangePinDialog) {
        var newPin by remember { mutableStateOf(parentalPin) }
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            containerColor = cardColor,
            title = { Text("تغيير رمز الرقابة الأبوية (PIN)", color = Color.White, fontSize = 15.sp) },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6) newPin = it },
                    label = { Text("أدخل الرمز الجديد (4 أرقام)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newPin.length >= 4) {
                        onParentalPinChange(newPin)
                        prefs.edit().putString("parental_pin", newPin).apply()
                        Toast.makeText(context, "تم تحديث الرمز السري", Toast.LENGTH_SHORT).show()
                    }
                    showChangePinDialog = false
                }) { Text("حفظ") }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    cardColor: Color,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                trailingAction?.invoke()
            }
            HorizontalDivider(color = Color(0x18FFFFFF))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 12.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
