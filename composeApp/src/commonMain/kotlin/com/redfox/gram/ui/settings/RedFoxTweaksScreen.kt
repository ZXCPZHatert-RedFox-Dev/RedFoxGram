package com.redfox.gram.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.config.RedFoxSettingsState
import com.redfox.gram.engine.RedFoxTdEngine
import com.redfox.gram.media.RedFoxAudioController
import com.redfox.gram.network.RedFoxProxyManager
import com.redfox.gram.security.applyScreenAntiScreenshot
import com.redfox.gram.ui.components.ExpressiveAvatarStyle
import com.redfox.gram.ui.components.RedFoxLocalImage
import com.redfox.gram.ui.components.buildExpressiveAvatarShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxTweaksScreen(
    onBack: () -> Unit,
    onOpenProxy: () -> Unit = {}
) {
    val settings: RedFoxSettingsState by RedFoxConfig.state.collectAsState()
    val currentUser by RedFoxTdEngine.currentUser.collectAsState()
    val tgThemes by RedFoxTdEngine.syncedTelegramThemes.collectAsState()
    val tgAccents by RedFoxTdEngine.syncedCloudAccents.collectAsState()
    val proxyState by RedFoxProxyManager.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current

    var subScreen by remember { mutableStateOf("ROOT") }
    var hidePhone by remember { mutableStateOf(false) }
    var showUserId by remember { mutableStateOf(true) }
    var hapticLevel by remember { mutableStateOf(3) }
    var lockArchive by remember { mutableStateOf(true) }
    var lockHidden by remember { mutableStateOf(true) }
    var lockSaved by remember { mutableStateOf(true) }
    var pinInput by remember { mutableStateOf(settings.pinCode) }
    var backupNote by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        RedFoxTdEngine.loadCurrentUserProfile()
        RedFoxTdEngine.syncCloudThemesAndBackground()
    }

    if (subScreen != "ROOT") {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { subScreen = "ROOT" }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Назад",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    title = {
                        Text(
                            text = when (subScreen) {
                                "OPEX" -> "Основные"
                                "THEMES" -> "Настройки чатов и Темы"
                                "PRIVACY" -> "Конфиденциальность и Замок"
                                "DATA" -> "Данные, Кэш и Shield"
                                else -> "Настройки"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (subScreen == "OPEX") {
                    item {
                        Text("Профиль", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                    }
                    item {
                        OpexPillSwitch(
                            title = "Скрыть номер телефона",
                            checked = hidePhone,
                            onCheckedChange = { hidePhone = it }
                        )
                    }
                    item {
                        OpexPillSwitch(
                            title = "Показывать ID",
                            checked = showUserId,
                            onCheckedChange = { showUserId = it }
                        )
                    }
                    item {
                        OpexPillSwitch(
                            title = "Режим Призрака (Ghost Read)",
                            subtitle = "Читать сообщения без отправки галочек",
                            checked = settings.ghostReadMessages,
                            onCheckedChange = { v -> RedFoxConfig.update { it.copy(ghostReadMessages = v) } }
                        )
                    }
                    item {
                        OpexPillSwitch(
                            title = "Анти-удаление сообщений",
                            subtitle = "Сохранять удалённые собеседником сообщения",
                            checked = settings.antiDeleteMessages,
                            onCheckedChange = { v -> RedFoxConfig.update { it.copy(antiDeleteMessages = v) } }
                        )
                    }

                    item {
                        Text("Виброотклик", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, top = 8.dp))
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Выкл", "Мягко", "Обычно", "Сильно").forEachIndexed { idx, label ->
                                val selected = hapticLevel == idx
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            hapticLevel = idx
                                            if (idx > 0) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(vertical = 14.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    backupNote = "Локальная история удалённых сообщений очищена"
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Удалить сохранённые сообщения",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                if (subScreen == "THEMES") {
                    item {
                        Text("Палитра интерфейса и Темы Telegram", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
                    }

item {
Surface(
shape = RoundedCornerShape(24.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth()
) {
Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
val presets = listOf(
"MONET" to "Material You",
"AMOLED" to "AMOLED Чёрная",
"FOX" to "Fox Sunset",
"TEAL" to "Cyber Teal",
"ROSE" to "Sakura Rose"
)
presets.chunked(2).forEach { rowPresets ->
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
rowPresets.forEach { (code, label) ->
val selected = settings.themePreset == code
FilterChip(
selected = selected,
onClick = { RedFoxConfig.update { it.copy(themePreset = code) } },
shape = CircleShape,
label = { Text(label) },
modifier = Modifier.weight(1f)
)
}
}
}
if (tgThemes.isNotEmpty() || tgAccents.isNotEmpty()) {
HorizontalDivider()
Text("Облачные темы и акценты Telegram", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
val combinedTg = tgThemes.map { it.name to it.primaryColorHex } + tgAccents.take(6)
combinedTg.chunked(2).forEach { rowItems ->
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
rowItems.forEach { (tName, tHex) ->
val isPicked = settings.themePreset == "TG_SYNC" && settings.syncedThemeName == tName
FilterChip(
selected = isPicked,
onClick = {
RedFoxConfig.update {
it.copy(
themePreset = "TG_SYNC",
syncedTelegramAccentHex = tHex,
syncedThemeName = tName
)
}
},
shape = CircleShape,
label = { Text(tName, maxLines = 1) },
modifier = Modifier.weight(1f)
)
}
}
}
}
HorizontalDivider()
Text("Углы пузырей: ${settings.bubbleCornerDp} dp", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
listOf(12 to "Классика", 22 to "M3E", 28 to "Капсулы").forEach { (dpVal, title) ->
FilterChip(
selected = settings.bubbleCornerDp == dpVal,
onClick = { RedFoxConfig.update { it.copy(bubbleCornerDp = dpVal) } },
shape = CircleShape,
label = { Text(title) },
modifier = Modifier.weight(1f)
)
}
}
Text("Размер текста в чатах: ${(settings.chatFontScale * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
Slider(
value = settings.chatFontScale,
onValueChange = { scale -> RedFoxConfig.update { it.copy(chatFontScale = scale) } },
valueRange = 0.85f..1.35f,
steps = 9
)
}
}
}
}
if (subScreen == "PRIVACY") {
item {
Text("Замок Fortress (как в Opexgram)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
}
item {
OpexPillSwitch(
title = "Блокировка входа PIN-кодом",
checked = settings.pinLockEnabled,
onCheckedChange = { v -> RedFoxConfig.update { it.copy(pinLockEnabled = v) } }
)
}
item {
OpexPillSwitch(
title = "Архив под замком",
checked = lockArchive,
onCheckedChange = { lockArchive = it }
)
}
item {
OpexPillSwitch(
title = "Скрытые чаты",
checked = lockHidden,
onCheckedChange = { lockHidden = it }
)
}
item {
OpexPillSwitch(
title = "Избранное",
checked = lockSaved,
onCheckedChange = { lockSaved = it }
)
}
item {
OpexPillSwitch(
title = "Системный анти-скриншот (FLAG_SECURE)",
subtitle = "Запретить снимки и запись экрана",
checked = settings.antiScreenshotEnabled,
onCheckedChange = { v ->
RedFoxConfig.update { it.copy(antiScreenshotEnabled = v) }
applyScreenAntiScreenshot(v)
}
)
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth()
) {
Row(
modifier = Modifier.padding(14.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
OutlinedTextField(
value = pinInput,
onValueChange = { v ->
val digits = v.filter { it.isDigit() }.take(4)
pinInput = digits
if (digits.length == 4) RedFoxConfig.update { it.copy(pinCode = digits) }
},
label = { Text("4-значный PIN") },
singleLine = true,
shape = CircleShape,
modifier = Modifier.weight(1f)
)
Button(
onClick = {
if (pinInput.length == 4) {
RedFoxConfig.update { it.copy(pinCode = pinInput, pinLockEnabled = true) }
backupNote = "PIN-код установлен: $pinInput"
}
},
shape = CircleShape
) {
Text("Задать")
}
}
}
}
}
if (subScreen == "DATA") {
item {
OpexPillSwitch(
title = "Умный плеер Live LRC",
subtitle = "Волнистый прогресс A17 и караоке LRCLIB при запуске аудио",
checked = settings.liveLrcLyricsBar,
onCheckedChange = { v -> RedFoxConfig.update { it.copy(liveLrcLyricsBar = v) } }
)
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth()
) {
Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
FilledTonalButton(
onClick = {
RedFoxAudioController.closePlayer()
var freed = 0L
runCatching {
val tmp = java.io.File(System.getProperty("java.io.tmpdir"))
tmp.walkTopDown().filter { it.isFile }.forEach {
freed += it.length()
it.delete()
}
}
val mb = String.format("%.1f", freed / (1024.0 * 1024.0))
backupNote = "Внутренний кэш очищен ($mb МБ)"
},
shape = CircleShape,
modifier = Modifier.fillMaxWidth()
) {
Text("Очистить внутренний кэш медиа")
}
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
FilledTonalButton(
onClick = {
val data = RedFoxConfig.exportSettingsOpex()
clipboard.setText(AnnotatedString(data))
backupNote = "Настройки .opex скопированы"
},
shape = CircleShape,
modifier = Modifier.weight(1f)
) {
Text("Экспорт .opex")
}
OutlinedButton(
onClick = {
val clip = clipboard.getText()?.text ?: ""
val ok = RedFoxConfig.importSettingsOpex(clip)
backupNote = if (ok) "Настройки .opex импортированы!" else "Скопируйте .opex в буфер"
},
shape = CircleShape,
modifier = Modifier.weight(1f)
) {
Text("Импорт .opex")
}
}
}
}
}
}
if (backupNote.isNotEmpty()) {
item {
Text(
text = backupNote,
style = MaterialTheme.typography.labelLarge,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.primary,
modifier = Modifier.padding(horizontal = 8.dp)
)
}
}
item { Spacer(Modifier.height(32.dp)) }
}
}
return
}
// Главный экран Настроек 1 в 1 как на твоих скриншотах из Opexgram
val flowerShape = remember { buildExpressiveAvatarShape(ExpressiveAvatarStyle.FLOWER_12) }
Scaffold(
topBar = {
TopAppBar(
navigationIcon = {
IconButton(onClick = onBack) {
Icon(
imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
contentDescription = "Назад",
tint = MaterialTheme.colorScheme.onSurface
)
}
},
title = {},
actions = {
IconButton(onClick = { subScreen = "OPEX" }) {
Icon(Icons.Rounded.Search, contentDescription = "Поиск", tint = MaterialTheme.colorScheme.onSurface)
}
}
)
}
) { padding ->
LazyColumn(
modifier = Modifier
.fillMaxSize()
.padding(padding)
.padding(horizontal = 16.dp),
horizontalAlignment = Alignment.CenterHorizontally,
verticalArrangement = Arrangement.spacedBy(10.dp)
) {
// Центрированный 12-лепестковый цветок аватарки + имя + телефон • @username
item {
Column(
horizontalAlignment = Alignment.CenterHorizontally,
modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
) {
Box(
modifier = Modifier
.size(96.dp)
.clip(flowerShape)
.background(MaterialTheme.colorScheme.primaryContainer),
contentAlignment = Alignment.Center
) {
Text(
text = currentUser.fullName.take(1).uppercase(),
style = MaterialTheme.typography.headlineLarge,
fontWeight = FontWeight.ExtraBold,
color = MaterialTheme.colorScheme.onPrimaryContainer
)
if (currentUser.avatarPath.isNotBlank()) {
RedFoxLocalImage(
filePath = currentUser.avatarPath,
contentDescription = currentUser.fullName,
modifier = Modifier.fillMaxSize()
)
}
}
Spacer(Modifier.height(14.dp))
Text(
text = currentUser.fullName,
style = MaterialTheme.typography.headlineSmall,
fontWeight = FontWeight.ExtraBold,
textAlign = TextAlign.Center
)
val visiblePhone = if (hidePhone) "Телефон скрыт" else currentUser.phone
val metaParts = buildList {
if (visiblePhone.isNotBlank()) add(visiblePhone)
if (currentUser.username.isNotBlank()) add(currentUser.username)
if (showUserId && currentUser.id != 0L) add("ID ${currentUser.id}")
}.joinToString(" • ")
if (metaParts.isNotEmpty()) {
Spacer(Modifier.height(4.dp))
Text(
text = metaParts,
style = MaterialTheme.typography.bodyMedium,
color = MaterialTheme.colorScheme.onSurfaceVariant,
textAlign = TextAlign.Center
)
}
}
}
// Раздельные плашки-капсулы с цветными круглыми иконками (как в Opexgram)
item {
OpexCategoryCard(
icon = Icons.Rounded.Star,
iconBg = Color(0xFFE91E63),
title = "Настройки RedFox / Opex",
subtitle = "Функции и внешний вид клиента",
onClick = { subScreen = "OPEX" }
)
}
item {
OpexCategoryCard(
icon = Icons.Rounded.Person,
iconBg = Color(0xFF9C27B0),
title = "Аккаунт",
subtitle = "Номер, имя пользователя, ID профиля",
onClick = {
if (currentUser.id != 0L) {
clipboard.setText(AnnotatedString(currentUser.id.toString()))
backupNote = "ID ${currentUser.id} скопирован в буфер"
}
}
)
}
item {
OpexCategoryCard(
icon = Icons.Rounded.Edit,
iconBg = Color(0xFFFF7043),
title = "Настройки чатов",
subtitle = "Обои, облачные темы Telegram, углы пузырей",
onClick = { subScreen = "THEMES" }
)
}
item {
OpexCategoryCard(
icon = Icons.Rounded.Lock,
iconBg = Color(0xFFFFB300),
title = "Конфиденциальность",
subtitle = "Замок Fortress, скрытые чаты, анти-скриншот",
onClick = { subScreen = "PRIVACY" }
)
}
item {
OpexCategoryCard(
icon = Icons.Rounded.Refresh,
iconBg = Color(0xFFAB47BC),
title = "Данные и память",
subtitle = "Очистка внутреннего кэша, экспорт .opex",
onClick = { subScreen = "DATA" }
)
}
item {
OpexCategoryCard(
icon = Icons.Rounded.CheckCircle,
iconBg = Color(0xFF26A69A),
title = "RedFox Shield (Обход без VPN)",
subtitle = if (proxyState.enabled) "Активен • ${proxyState.statusNote}" else "Серверы Xray и tg-ws-proxy",
onClick = onOpenProxy
)
}
if (backupNote.isNotEmpty()) {
item {
Text(
text = backupNote,
style = MaterialTheme.typography.labelMedium,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.primary
)
}
}
item {
Spacer(Modifier.height(8.dp))
Text(
text = "RedFoxGram для Android v1.0-M3E\nTDLib • Material 3 Expressive",
style = MaterialTheme.typography.bodySmall,
color = MaterialTheme.colorScheme.onSurfaceVariant,
textAlign = TextAlign.Center,
modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)
)
}
}
}
}
@Composable
private fun OpexCategoryCard(
icon: ImageVector,
iconBg: Color,
title: String,
subtitle: String,
onClick: () -> Unit
) {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier
.fillMaxWidth()
.clickable(onClick = onClick)
) {
Row(
modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
verticalAlignment = Alignment.CenterVertically
) {
Box(
modifier = Modifier
.size(40.dp)
.clip(CircleShape)
.background(iconBg.copy(alpha = 0.22f)),
contentAlignment = Alignment.Center
) {
Icon(
imageVector = icon,
contentDescription = title,
tint = iconBg,
modifier = Modifier.size(22.dp)
)
}
Spacer(Modifier.width(14.dp))
Column(modifier = Modifier.weight(1f)) {
Text(
text = title,
style = MaterialTheme.typography.titleMedium,
fontWeight = FontWeight.SemiBold
)
Text(
text = subtitle,
style = MaterialTheme.typography.bodySmall,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
}
}
}
}
@Composable
private fun OpexPillSwitch(
title: String,
subtitle: String? = null,
checked: Boolean,
onCheckedChange: (Boolean) -> Unit
) {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier
.fillMaxWidth()
.clickable { onCheckedChange(!checked) }
) {
Row(
modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
verticalAlignment = Alignment.CenterVertically
) {
Column(modifier = Modifier.weight(1f)) {
Text(
text = title,
style = MaterialTheme.typography.bodyLarge,
fontWeight = FontWeight.SemiBold
)
if (!subtitle.isNullOrBlank()) {
Text(
text = subtitle,
style = MaterialTheme.typography.bodySmall,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
}
}
Spacer(Modifier.width(12.dp))
Switch(checked = checked, onCheckedChange = onCheckedChange)
}
}
}
