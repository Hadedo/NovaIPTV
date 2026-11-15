package com.hadedo.novaiptv

import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class SheetChoice(
    val key: String,
    val title: String,
    val subtitle: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    prefs: SharedPreferences,
    servers: List<SavedServer>,
    activeServerId: String,
    activeServer: SavedServer?,
    accountInfo: AccountInfo?,
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
    defaultPlayer: String,
    onDefaultPlayerChange: (String) -> Unit,
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
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val greenText = Color(0xFF4ADE80)
    val subTextColor = Color(0xFF94A3B8)

    var currentSubPage by remember { mutableIntStateOf(0) }
    var activeSheetType by remember { mutableStateOf("") }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showUaDialog by remember { mutableStateOf(false) }
    var speedResult by remember { mutableStateOf("") }
    var cacheSizeLabel by remember { mutableStateOf(AppStorage.getCacheSizeMb(context)) }

    BackHandler(enabled = currentSubPage != 0) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        currentSubPage = 0
    }

    val langLabel = when (appLang) {
        "en" -> "English"
        "fr" -> "Français"
        "tr" -> "Türkçe"
        "ku" -> "کوردی"
        else -> "العربية"
    }
    val playerLabel = when (defaultPlayer) {
        "mx" -> "MX Player"
        "vlc" -> "VLC Player"
        "ask" -> "اختيار عند التشغيل"
        else -> "VELOX الداخلي"
    }
    val decoderLabel = when (decoderMode) {
        "sw" -> "Software"
        "auto" -> "تلقائي"
        else -> "Hardware 4K"
    }
    val bufferLabel = when (bufferProfile) {
        "zerodelay" -> "وضع المباريات (أقل تأخير)"
        "fast" -> "تبديل فوري"
        "antifreeze" -> "مضاد التقطيع"
        else -> "متوازن"
    }
    val fmtLabel = when (streamFmt) {
        "ts" -> "MPEG-TS"
        "m3u8" -> "HLS / M3U8"
        else -> "TS Plus / تلقائي"
    }
    val startupLabel = when (startupTab) {
        1 -> "البث المباشر"
        2 -> "مركز الرياضة"
        3 -> "السينما"
        else -> "الرئيسية"
    }

    AnimatedContent(
        targetState = currentSubPage,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInHorizontally { it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 2 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { it / 2 } + fadeOut())
            }
        },
        label = "velox_settings_nav",
        modifier = Modifier.fillMaxSize()
    ) { page ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (page != 0) {
                val pageTitle = when (page) {
                    1 -> "الاشتراكات والمصادر"
                    2 -> "محرك المشغل والبث"
                    3 -> "الترجمة ومؤقت النوم"
                    4 -> "المظهر والواجهة"
                    else -> "الحماية والنظام"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardColor)
                        .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentSubPage = 0
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(accentColor.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowForward, null, tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(pageTitle, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text("الإعدادات", color = subTextColor, fontSize = 12.sp)
                }
            }

            when (page) {
                0 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(accentColor.copy(alpha = 0.32f), Color(0xFF151520), Color(0xFF0A0A0F))
                                )
                            )
                            .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentSubPage = 1
                            }
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                            .border(1.dp, accentColor, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                    Column {
                                        Text(activeServer?.title ?: "لا يوجد اشتراك نشط", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                        Text(
                                            if (activeServer != null) "${activeServer.countryCode} • ${activeServer.type.uppercase()} • ⚡ ${activeServer.pingMs}ms"
                                            else "اضغط لإضافة اشتراكك الأول",
                                            color = greenText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x26FFFFFF), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text("إدارة ›", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (accountInfo != null) {
                                HorizontalDivider(color = Color(0x22FFFFFF))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("الحالة", color = subTextColor, fontSize = 10.sp)
                                        Text(accountInfo.status, color = greenText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Column {
                                        Text("تاريخ الانتهاء", color = subTextColor, fontSize = 10.sp)
                                        Text(accountInfo.expDate, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("المتصلون", color = subTextColor, fontSize = 10.sp)
                                        Text("${accountInfo.activeCons} / ${accountInfo.maxCons}", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    GlassSettingsGroup(cardColor = cardColor) {
                        SettingsPortalRow(
                            icon = Icons.Default.Dns,
                            iconTint = Color(0xFF38BDF8),
                            title = "الاشتراكات والسيرفرات",
                            subtitle = "التبديل بين الحسابات، إضافة M3U أو Xtream، وفحص السرعة",
                            badgeText = "${servers.size}",
                            onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentSubPage = 1 }
                        )
                        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                        SettingsPortalRow(
                            icon = Icons.Default.PlayCircleFilled,
                            iconTint = Color(0xFF4ADE80),
                            title = "محرك المشغل والبث",
                            subtitle = "وضع تقليل تأخير المباريات، صيغة TS Plus، والمشغل الافتراضي",
                            badgeText = bufferLabel.take(12),
                            onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentSubPage = 2 }
                        )
                        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                        SettingsPortalRow(
                            icon = Icons.Default.Subtitles,
                            iconTint = Color(0xFFFBBF24),
                            title = "الترجمة ومؤقت النوم",
                            subtitle = "حجم ولون خط الترجمة، والإيقاف التلقائي للبث",
                            badgeText = "${subFontSize.toInt()}sp",
                            onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentSubPage = 3 }
                        )
                        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                        SettingsPortalRow(
                            icon = Icons.Default.Palette,
                            iconTint = Color(0xFFC084FC),
                            title = "المظهر والواجهة واللغة",
                            subtitle = "ألوان النيون، خلفيات AMOLED، اللغة، وشاشة البداية",
                            badgeText = langLabel,
                            onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentSubPage = 4 }
                        )
                        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                        SettingsPortalRow(
                            icon = Icons.Default.Shield,
                            iconTint = Color(0xFFF87171),
                            title = "الحماية والنسخ الاحتياطي",
                            subtitle = "الرقابة الأبوية، إخفاء الباقات، User-Agent، وتنظيف الكاش",
                            badgeText = cacheSizeLabel,
                            onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentSubPage = 5 }
                        )
                    }
                }

                1 -> {
                    Button(
                        onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onAddServerClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة اشتراك جديد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    GlassSettingsGroup(cardColor = cardColor) {
                        if (servers.isEmpty()) {
                            Text("لا توجد اشتراكات محفوظة حالياً.", color = subTextColor, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                        } else {
                            servers.forEachIndexed { idx, srv ->
                                val isAct = srv.id == activeServerId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isAct) accentColor.copy(alpha = 0.14f) else Color.Transparent)
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSelectServer(srv)
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(if (isAct) greenText.copy(alpha = 0.2f) else Color(0x18FFFFFF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (isAct) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            null,
                                            tint = if (isAct) greenText else subTextColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(srv.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${srv.countryCode} • ${srv.type.uppercase()} • ⚡ ${srv.pingMs}ms", color = if (isAct) greenText else subTextColor, fontSize = 11.sp, maxLines = 1)
                                    }
                                    IconButton(onClick = { onEditServerClick(srv) }) {
                                        Icon(Icons.Default.Edit, null, tint = Color.LightGray, modifier = Modifier.size(19.dp))
                                    }
                                    IconButton(onClick = { onDeleteServerClick(srv) }) {
                                        Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFF87171), modifier = Modifier.size(20.dp))
                                    }
                                }
                                if (idx < servers.lastIndex) HorizontalDivider(color = Color(0x14FFFFFF))
                            }
                        }
                    }

                    activeServer?.let { srv ->
                        OutlinedButton(
                            onClick = {
                                speedResult = "جاري قياس استجابة السيرفر..."
                                scope.launch { speedResult = ServersEngine.runSpeedTest(srv.host) }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.NetworkCheck, null, tint = accentColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اختبار سرعة واستجابة السيرفر", color = Color.White, fontSize = 13.sp)
                        }
                        if (speedResult.isNotBlank()) {
                            Text(speedResult, color = greenText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                2 -> {
                    GlassSettingsGroup(cardColor = cardColor) {
                        SettingsPortalRow(
                            icon = Icons.Default.Speed,
                            iconTint = greenText,
                            title = "وضع تقليل تأخير المباريات والبفر",
                            subtitle = "تقليص فرق التوقيت مع الستلايت واللحاق بالبث المباشر",
                            badgeText = bufferLabel,
                            onClick = { activeSheetType = "buffer" }
                        )
                        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                        SettingsPortalRow(
                            icon = Icons.Default.Stream,
                            iconTint = Color(0xFFFBBF24),
                            title = "صيغة روابط البث المباشر",
                            subtitle = "تشغيل روابط TS Plus بدون امتداد أو MPEG-TS أو HLS",
                            badgeText = fmtLabel,
                            onClick = { activeSheetType = "fmt" }
                        )
                        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                        SettingsPortalRow(
                            icon = Icons.Default.SmartDisplay,
                            iconTint = accentColor,
                            title = "المشغل الافتراضي",
                            subtitle = "التشغيل عبر محرك VELOX الداخلي أو MX Player أو VLC",
                            badgeText = playerLabel,
                            onClick = { activeSheetType = "player" }
                        )
                        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                        SettingsPortalRow(
                            icon = Icons.Default.Memory,
                            iconTint = Color(0xFF38BDF8),
                            title = "معالج فك الترميز ونقاوة الصورة",
                            subtitle = "تسريع الرسوميات Hardware 4K لأعلى وضوح ونعومة",
                            badgeText = decoderLabel,
                            onClick = { activeSheetType = "decoder" }
                        )
                    }

                    GlassSettingsGroup(cardColor = cardColor) {
                        TactileSwitchRow("توحيد رابط البث", "تجاوز قيود توجيه الروابط في بعض السيرفرات", sameHost, onSameHostChange)
                        HorizontalDivider(color = Color(0x14FFFFFF))
                        TactileSwitchRow("إيماءات اللمس الذكية", "السحب يميناً للصوت ويساراً للسطوع داخل المشغل", gesturesEnabled, onGesturesChange)
                        HorizontalDivider(color = Color(0x14FFFFFF))
                        TactileSwitchRow("التشغيل التلقائي للحلقات", "الانتقال للحلقة التالية تلقائياً عند انتهاء الحالية", autoplayNextEp, onAutoplayChange)
                    }
                }

                3 -> {
                    GlassSettingsGroup(cardColor = cardColor) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("حجم خط الترجمة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${subFontSize.toInt()} sp", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Slider(
                                value = subFontSize,
                                onValueChange = onSubFontSizeChange,
                                valueRange = 14f..30f,
                                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                            )
                        }
                        HorizontalDivider(color = Color(0x14FFFFFF))
                        TactileSwitchRow("لون الترجمة الأصفر", "إبراز نص الترجمة باللون الأصفر لوضوح أعلى", subColorYellow, onSubColorYellowChange)
                        HorizontalDivider(color = Color(0x14FFFFFF))
                        SettingsPortalRow(
                            icon = Icons.Default.Bedtime,
                            iconTint = Color(0xFFC084FC),
                            title = "مؤقت النوم التلقائي",
                            subtitle = "إيقاف البث تلقائياً بعد مدة محددة",
                            badgeText = if (sleepMinutesLeft > 0) "$sleepMinutesLeft د" else "متوقف",
                            onClick = { activeSheetType = "sleep" }
                        )
                    }
                }

                4 -> {
                    GlassSettingsGroup(cardColor = cardColor) {
                        SettingsPortalRow(
                            icon = Icons.Default.Language,
                            iconTint = Color(0xFF38BDF8),
                            title = "لغة التطبيق",
                            subtitle = "تغيير لغة القوائم والواجهة بالكامل",
                            badgeText = langLabel,
                            onClick = { activeSheetType = "lang" }
                        )
                        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                        SettingsPortalRow(
                            icon = Icons.Default.Home,
                            iconTint = greenText,
                            title = "شاشة البداية",
                            subtitle = "القسم الذي يفتح مباشرة عند تشغيل التطبيق",
                            badgeText = startupLabel,
                            onClick = { activeSheetType = "startup" }
                        )
                        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                        SettingsPortalRow(
                            icon = Icons.Default.ColorLens,
                            iconTint = accentColor,
                            title = "لون الإضاءة النيونية",
                            subtitle = "تخصيص اللون الأساسي للأزرار والتأثيرات",
                            badgeText = accentOptions.getOrElse(accentIdx) { accentOptions[0] }.first,
                            onClick = { activeSheetType = "accent" }
                        )
                        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                        SettingsPortalRow(
                            icon = Icons.Default.DarkMode,
                            iconTint = Color(0xFFC084FC),
                            title = "سمة الخلفية الداكنة",
                            subtitle = "اختيار درجة السواد المريحة للعين",
                            badgeText = bgOptions.getOrElse(bgIdx) { bgOptions[0] }.first,
                            onClick = { activeSheetType = "bg" }
                        )
                    }
                }

                5 -> {
                    GlassSettingsGroup(cardColor = cardColor) {
                        SettingsPortalRow(
                            icon = Icons.Default.Badge,
                            iconTint = accentColor,
                            title = "هوية المشغل (User-Agent)",
                            subtitle = "تحديد اسم المشغل المُرسل إلى السيرفر",
                            badgeText = userAgent.take(14),
                            onClick = { activeSheetType = "ua" }
                        )
                        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                        SettingsPortalRow(
                            icon = Icons.Default.VisibilityOff,
                            iconTint = Color(0xFF38BDF8),
                            title = "إدارة الباقات المخفية",
                            subtitle = "إظهار أو إخفاء باقات معينة من القوائم",
                            badgeText = "تعديل",
                            onClick = onOpenCategoriesManager
                        )
                        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                        SettingsPortalRow(
                            icon = Icons.Default.Lock,
                            iconTint = Color(0xFFFBBF24),
                            title = "رمز الرقابة الأبوية",
                            subtitle = "الرمز السري للخروج من وضع الأطفال",
                            badgeText = parentalPin,
                            onClick = { showChangePinDialog = true }
                        )
                        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                        SettingsPortalRow(
                            icon = Icons.Default.Backup,
                            iconTint = greenText,
                            title = "النسخ الاحتياطي والاستعادة",
                            subtitle = "تصدير أو استيراد اشتراكاتك ومجلداتك بكود واحد",
                            badgeText = "نقل",
                            onClick = onOpenBackupDialog
                        )
                        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                        SettingsPortalRow(
                            icon = Icons.Default.CleaningServices,
                            iconTint = Color(0xFFF87171),
                            title = "تنظيف الذاكرة المؤقتة",
                            subtitle = "حذف الملفات المؤقتة لتحرير المساحة",
                            badgeText = cacheSizeLabel,
                            onClick = {
                                AppStorage.clearAllMediaCache(context)
                                cacheSizeLabel = AppStorage.getCacheSizeMb(context)
                                Toast.makeText(context, "تم تنظيف الذاكرة المؤقتة", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    if (activeSheetType.isNotEmpty()) {
        val (sheetTitle, choices, selectedKey) = when (activeSheetType) {
            "lang" -> Triple(
                "اختر لغة التطبيق",
                listOf(
                    SheetChoice("ar", "العربية", "واجهة عربية متكاملة من اليمين لليسار"),
                    SheetChoice("en", "English", "Full English interface"),
                    SheetChoice("fr", "Français", "Interface en français"),
                    SheetChoice("tr", "Türkçe", "Türkçe arayüz"),
                    SheetChoice("ku", "کوردی", "ڕووکاری کوردی")
                ),
                appLang
            )
            "player" -> Triple(
                "اختر المشغل الافتراضي",
                listOf(
                    SheetChoice("internal", "VELOX الداخلي", "أسرع تنقل بين القنوات مع دعم 4K وتعدد الشاشات"),
                    SheetChoice("mx", "MX Player", "فتح البث مباشرة في تطبيق MX Player"),
                    SheetChoice("vlc", "VLC Player", "فتح البث مباشرة في تطبيق VLC"),
                    SheetChoice("ask", "اختيار عند كل تشغيل", "إظهار قائمة المشغلات المثبتة على جهازك")
                ),
                defaultPlayer
            )
            "decoder" -> Triple(
                "معالج فك الترميز ونقاوة الصورة",
                listOf(
                    SheetChoice("hw", "Hardware 4K (الأعلى نقاوة)", "يمرر الفيديو مباشرة عبر معالج الرسوميات GPU بأعلى جودة"),
                    SheetChoice("auto", "تلقائي", "يبدل تلقائياً حسب صيغة القناة"),
                    SheetChoice("sw", "Software", "معالجة برمجية للصيغ النادرة")
                ),
                decoderMode
            )
            "buffer" -> Triple(
                "وضع البفر وتقليل تأخير المباريات",
                listOf(
                    SheetChoice("zerodelay", "وضع المباريات المباشرة (أقل تأخير)", "يقلل البفر إلى 400ms ويسرّع البث تلقائياً للحاق بالستلايت"),
                    SheetChoice("fast", "تبديل فوري", "فتح القنوات خلال أجزاء من الثانية للإنترنت السريع"),
                    SheetChoice("balanced", "متوازن", "توازن مثالي بين السرعة والاستقرار"),
                    SheetChoice("antifreeze", "مضاد التقطيع", "تخزين مسبق أكبر لمنع التوقف في الشبكات الضعيفة")
                ),
                bufferProfile
            )
            "fmt" -> Triple(
                "صيغة روابط البث المباشر",
                listOf(
                    SheetChoice("raw", "TS Plus / تلقائي (بدون امتداد)", "يشغل جميع روابط Xtream الحديثة بأسرع استجابة"),
                    SheetChoice("ts", "MPEG-TS (.ts)", "الصيغة القياسية التقليدية للبث المباشر"),
                    SheetChoice("m3u8", "HLS (.m3u8)", "بث تكيفي مناسب للشبكات المتقلبة")
                ),
                streamFmt
            )
            "startup" -> Triple(
                "شاشة البداية",
                listOf(
                    SheetChoice("0", "الرئيسية", "العروض المميزة وواصل المشاهدة"),
                    SheetChoice("1", "البث المباشر", "الدخول مباشرة إلى باقات القنوات"),
                    SheetChoice("2", "مركز الرياضة", "جدول المباريات والقنوات الرياضية"),
                    SheetChoice("3", "السينما", "مكتبة الأفلام والمسلسلات")
                ),
                startupTab.toString()
            )
            "accent" -> Triple(
                "لون الإضاءة النيونية",
                accentOptions.mapIndexed { idx, pair -> SheetChoice(idx.toString(), pair.first) },
                accentIdx.toString()
            )
            "bg" -> Triple(
                "سمة الخلفية الداكنة",
                bgOptions.mapIndexed { idx, triple -> SheetChoice(idx.toString(), triple.first) },
                bgIdx.toString()
            )
            "sleep" -> Triple(
                "مؤقت النوم التلقائي",
                listOf(
                    SheetChoice("0", "إيقاف المؤقت", "استمرار التشغيل بدون إيقاف"),
                    SheetChoice("15", "بعد 15 دقيقة", ""),
                    SheetChoice("30", "بعد 30 دقيقة", ""),
                    SheetChoice("60", "بعد ساعة واحدة", "")
                ),
                sleepMinutesLeft.toString()
            )
            else -> Triple(
                "هوية المشغل (User-Agent)",
                listOf(
                    SheetChoice("IPTVSmartersPro", "IPTV Smarters Pro", "التوافق القياسي الأوسع"),
                    SheetChoice("TiviMate/4.7.0", "TiviMate", "هوية مشغل TiviMate الاحترافي"),
                    SheetChoice("VLC/3.0.18", "VLC Media Player", "هوية مشغل VLC"),
                    SheetChoice("custom", "إدخال هوية مخصصة...", "كتابة User-Agent يدوياً")
                ),
                userAgent
            )
        }

        ModalBottomSheet(
            onDismissRequest = { activeSheetType = "" },
            containerColor = cardColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(sheetTitle, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                choices.forEach { ch ->
                    val isSel = ch.key == selectedKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSel) accentColor.copy(alpha = 0.16f) else Color(0xFF09090E))
                            .border(1.dp, if (isSel) accentColor else Color(0x1FFFFFFF), RoundedCornerShape(14.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                when (activeSheetType) {
                                    "lang" -> onAppLangChange(ch.key)
                                    "player" -> onDefaultPlayerChange(ch.key)
                                    "decoder" -> onDecoderChange(ch.key)
                                    "buffer" -> onBufferChange(ch.key)
                                    "fmt" -> onStreamFmtChange(ch.key)
                                    "startup" -> onStartupTabChange(ch.key.toIntOrNull() ?: 0)
                                    "accent" -> onAccentChange(ch.key.toIntOrNull() ?: 0)
                                    "bg" -> onBgChange(ch.key.toIntOrNull() ?: 0)
                                    "sleep" -> onSleepMinutesChange(ch.key.toIntOrNull() ?: 0)
                                    "ua" -> {
                                        if (ch.key == "custom") showUaDialog = true
                                        else onUserAgentChange(ch.key)
                                    }
                                }
                                activeSheetType = ""
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ch.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (ch.subtitle.isNotBlank()) {
                                Text(ch.subtitle, color = subTextColor, fontSize = 11.sp)
                            }
                        }
                        if (isSel) {
                            Icon(Icons.Default.CheckCircle, null, tint = greenText, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    if (showUaDialog) {
        var uaIn by remember { mutableStateOf(userAgent) }
        AlertDialog(
            onDismissRequest = { showUaDialog = false },
            containerColor = cardColor,
            title = { Text("هوية المشغل المخصصة", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { OutlinedTextField(value = uaIn, onValueChange = { uaIn = it }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    if (uaIn.isNotBlank()) onUserAgentChange(uaIn.trim())
                    showUaDialog = false
                }) { Text("حفظ") }
            }
        )
    }

    if (showChangePinDialog) {
        var newPin by remember { mutableStateOf(parentalPin) }
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            containerColor = cardColor,
            title = { Text("تغيير رمز الرقابة الأبوية", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6) newPin = it },
                    label = { Text("أدخل الرمز الجديد") },
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
private fun GlassSettingsGroup(cardColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(20.dp)),
        content = content
    )
}

@Composable
private fun SettingsPortalRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconTint.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .background(Color(0x14FFFFFF), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(badgeText, color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Default.KeyboardArrowLeft, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun TactileSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
