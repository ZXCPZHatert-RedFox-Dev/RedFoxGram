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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.config.RedFoxSettingsState
import com.redfox.gram.engine.RedFoxTdEngine
import com.redfox.gram.network.RedFoxProxyManager
import com.redfox.gram.security.applyScreenAntiScreenshot
import com.redfox.gram.ui.components.ExpressiveAvatarStyle
import com.redfox.gram.ui.components.RedFoxLocalImage
import com.redfox.gram.ui.components.buildExpressiveAvatarShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxTweaksScreen(onBack: () -> Unit) {
    val settings: RedFoxSettingsState by RedFoxConfig.state.collectAsState()
    val currentUser by RedFoxTdEngine.currentUser.collectAsState()
    val tgThemes by RedFoxTdEngine.syncedTelegramThemes.collectAsState()
    val proxyState by RedFoxProxyManager.state.collectAsState()
    val clipboard = LocalClipboardManager.current

    var pinInput by remember { mutableStateOf(settings.pinCode) }
    var backupNote by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        RedFoxTdEngine.loadCurrentUserProfile()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Настройки",
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Карточка профиля в стиле Telegram + M3E
            item {
                val flowerShape = remember { buildExpressiveAvatarShape(ExpressiveAvatarStyle.FLOWER_12) }
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(flowerShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.fullName.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            if (currentUser.avatarPath.isNotBlank()) {
                                RedFoxLocalImage(
                                    filePath = currentUser.avatarPath,
                                    contentDescription = currentUser.fullName,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            val subInfo = listOf(currentUser.phone, currentUser.username)
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                            if (subInfo.isNotEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = subInfo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            if (currentUser.id != 0L) {
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                    modifier = Modifier.clickable {
                                        clipboard.setText(AnnotatedString(currentUser.id.toString()))
                                        backupNote = "ID ${currentUser.id} скопирован"
                                    }
                                ) {
                                    Text(
                                        text = "ID: ${currentUser.id} • ${if (proxyState.enabled) "Shield ON" else "Сеть OK"}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Секция Telegram: Приватность и функции AyuGram / Opexgram

item {
TelegramSettingsGroup(title = "Приватность и AyuGram") {
TelegramSwitchRow(
icon = Icons.Rounded.Lock,
title = "Режим Призрака (Ghost Read)",
subtitle = "Читать сообщения без отправки галочек",
checked = settings.ghostReadMessages,
onCheckedChange = { v -> RedFoxConfig.update { it.copy(ghostReadMessages = v) } }
)
HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
TelegramSwitchRow(
icon = Icons.Rounded.Edit,
title = "Скрывать набор текста",
subtitle = "Не показывать статус «печатает...»",
checked = settings.hideTypingStatus,
onCheckedChange = { v -> RedFoxConfig.update { it.copy(hideTypingStatus = v) } }
)
HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
TelegramSwitchRow(
icon = Icons.Rounded.Delete,
title = "Анти-удаление сообщений",
subtitle = "Сохранять удалённые собеседником посты",
checked = settings.antiDeleteMessages,
onCheckedChange = { v -> RedFoxConfig.update { it.copy(antiDeleteMessages = v) } }
)
HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
TelegramSwitchRow(
icon = Icons.Rounded.Warning,
title = "Системный анти-скриншот",
subtitle = "Запретить снимки и запись экрана (FLAG_SECURE)",
checked = settings.antiScreenshotEnabled,
onCheckedChange = { v ->
RedFoxConfig.update { it.copy(antiScreenshotEnabled = v) }
applyScreenAntiScreenshot(v)
}
)
}
}
// 3. Секция Telegram: Оформление, Темы и Шрифт M3E
item {
TelegramSettingsGroup(title = "Оформление и Темы Telegram") {
Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
Text("Палитра интерфейса", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
val presets = listOf(
"MONET" to "Material You",
"AMOLED" to "AMOLED Чёрная",
"FOX" to "Fox Sunset",
"TEAL" to "Cyber Teal",
"ROSE" to "Sakura Rose"
)
Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
presets.chunked(2).forEach { rowPresets ->
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
rowPresets.forEach { (code, label) ->
val selected = settings.themePreset == code
FilterChip(
selected = selected,
onClick = { RedFoxConfig.update { it.copy(themePreset = code) } },
shape = CircleShape,
label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
modifier = Modifier.weight(1f)
)
}
}
}
}
if (tgThemes.isNotEmpty()) {
HorizontalDivider()
Text("Облачные темы вашего аккаунта Telegram", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
tgThemes.chunked(2).forEach { rowThemes ->
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
rowThemes.forEach { themeItem ->
val isPicked = settings.themePreset == "TG_SYNC" && settings.syncedThemeName == themeItem.name
FilterChip(
selected = isPicked,
onClick = {
RedFoxConfig.update {
it.copy(
themePreset = "TG_SYNC",
syncedTelegramAccentHex = themeItem.primaryColorHex,
syncedThemeName = themeItem.name
)
}
},
shape = CircleShape,
label = { Text(themeItem.name, maxLines = 1) },
modifier = Modifier.weight(1f)
)
}
}
}
}
}
HorizontalDivider()
Text("Углы пузырей сообщений (${settings.bubbleCornerDp} dp)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
listOf(12 to "Классика", 22 to "M3E", 28 to "Капсулы").forEach { (dpVal, title) ->
val selected = settings.bubbleCornerDp == dpVal
FilterChip(
selected = selected,
onClick = { RedFoxConfig.update { it.copy(bubbleCornerDp = dpVal) } },
shape = CircleShape,
label = { Text(title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
modifier = Modifier.weight(1f)
)
}
}
HorizontalDivider()
Text("Размер текста в чатах: ${(settings.chatFontScale * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
Slider(
value = settings.chatFontScale,
onValueChange = { scale -> RedFoxConfig.update { it.copy(chatFontScale = scale) } },
valueRange = 0.85f..1.35f,
steps = 9
)
}
}
}
// 4. Секция Telegram: Медиа и Плеер Android 17
item {
TelegramSettingsGroup(title = "Медиа и Плеер Android 17") {
TelegramSwitchRow(
icon = Icons.Rounded.Star,
title = "Ambient Mode подсветка фото",
subtitle = "Свечение вокруг изображений в переписках",
checked = settings.ambientPhotoGlow,
onCheckedChange = { v -> RedFoxConfig.update { it.copy(ambientPhotoGlow = v) } }
)
HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
TelegramSwitchRow(
icon = Icons.Rounded.PlayArrow,
title = "Умный плеер Live LRC",
subtitle = "Волнистый прогресс A17 и тексты LRCLIB при запуске аудио",
checked = settings.liveLrcLyricsBar,
onCheckedChange = { v -> RedFoxConfig.update { it.copy(liveLrcLyricsBar = v) } }
)
}
}
// 5. Секция Telegram: Защита Fortress и Бэкап (.opex)
item {
TelegramSettingsGroup(title = "Код-пароль Fortress и Бэкап (.opex)") {
TelegramSwitchRow(
icon = Icons.Rounded.Lock,
title = "Блокировка приложения PIN-кодом",
subtitle = "Запрашивать код при входе и открытии скрытых чатов",
checked = settings.pinLockEnabled,
onCheckedChange = { v -> RedFoxConfig.update { it.copy(pinLockEnabled = v) } }
)
Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
OutlinedTextField(
value = pinInput,
onValueChange = { v ->
val digits = v.filter { it.isDigit() }.take(4)
pinInput = digits
if (digits.length == 4) {
RedFoxConfig.update { it.copy(pinCode = digits) }
}
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
HorizontalDivider()
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
FilledTonalButton(
onClick = {
val data = RedFoxConfig.exportSettingsOpex()
runCatching {
java.io.File(System.getProperty("java.io.tmpdir"), "redfox-settings.opex").writeText(data)
}
clipboard.setText(AnnotatedString(data))
backupNote = "Экспортировано в .opex и буфер обмена"
},
shape = CircleShape,
modifier = Modifier.weight(1f)
) {
Text("Экспорт .opex")
}
OutlinedButton(
onClick = {
val f = java.io.File(System.getProperty("java.io.tmpdir"), "redfox-settings.opex")
val clip = clipboard.getText()?.text ?: ""
val ok = (f.exists() && RedFoxConfig.importSettingsOpex(f.readText())) || RedFoxConfig.importSettingsOpex(clip)
backupNote = if (ok) "Конфигурация .opex восстановлена!" else "Скопируйте файл .opex в буфер"
},
shape = CircleShape,
modifier = Modifier.weight(1f)
) {
Text("Импорт .opex")
}
}
if (backupNote.isNotEmpty()) {
Text(
text = backupNote,
style = MaterialTheme.typography.labelMedium,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.primary
)
}
}
}
}
item { Spacer(Modifier.height(32.dp)) }
}
}
}
@Composable
private fun TelegramSettingsGroup(
title: String,
content: @Composable ColumnScope.() -> Unit
) {
Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
Text(
text = title,
style = MaterialTheme.typography.titleSmall,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.primary,
modifier = Modifier.padding(start = 12.dp)
)
Surface(
shape = RoundedCornerShape(28.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth()
) {
Column(content = content)
}
}
}
@Composable
private fun TelegramSwitchRow(
icon: ImageVector,
title: String,
subtitle: String,
checked: Boolean,
onCheckedChange: (Boolean) -> Unit
) {
Row(
modifier = Modifier
.fillMaxWidth()
.clickable { onCheckedChange(!checked) }
.padding(horizontal = 16.dp, vertical = 14.dp),
verticalAlignment = Alignment.CenterVertically
) {
Icon(
imageVector = icon,
contentDescription = title,
tint = MaterialTheme.colorScheme.primary,
modifier = Modifier.size(24.dp)
)
Spacer(Modifier.width(16.dp))
Column(modifier = Modifier.weight(1f)) {
Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
Spacer(Modifier.width(12.dp))
Switch(checked = checked, onCheckedChange = onCheckedChange)
}
}
