package com.redfox.gram.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.config.RedFoxSettingsState
import com.redfox.gram.ui.components.ExpressiveAvatarStyle
import com.redfox.gram.ui.components.ExpressiveM3Slider
import com.redfox.gram.ui.components.ExpressiveSwitch
import com.redfox.gram.ui.components.buildExpressiveAvatarShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxMediaAndOtherScreen(
    section: String, // "MEDIA_HUB", "PLAYER", "DOWNLOAD", "CAMERA", "OTHER"
    onBack: () -> Unit
) {
    val settings by RedFoxConfig.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    var activeSub by remember(section) { mutableStateOf(section) }
    var statusMsg by remember { mutableStateOf("") }

    var autoPlayVoices by remember { mutableStateOf(true) }
    var autoPlayCircles by remember { mutableStateOf(true) }
    var autoPlayMusic by remember { mutableStateOf(true) }
    var videoGlow by remember { mutableStateOf(true) }
    var compactCamera by remember { mutableStateOf(true) }

    var dlThickness by remember { mutableStateOf(8f) }
    var dlGap by remember { mutableStateOf(10f) }
    var dlCenterStop by remember { mutableStateOf(true) }
    var dlWaveHeight by remember { mutableStateOf(6f) }
    var dlWaveLength by remember { mutableStateOf(8f) }

    val handleBack: () -> Unit = {
        if (section == "MEDIA_HUB" && activeSub != "MEDIA_HUB") {
            activeSub = "MEDIA_HUB"
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                title = {
                    Text(
                        text = when (activeSub) {
                            "MEDIA_HUB" -> "Медиа"
                            "PLAYER" -> "Плеер"
                            "DOWNLOAD" -> "Загрузка"
                            "CAMERA" -> "Камера"
                            else -> "Другое"
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
            if (activeSub == "MEDIA_HUB") {
                item {
                    Text("Воспроизведение", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp))
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().clickable { activeSub = "PLAYER" }
                    ) {
                        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Плеер", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("Material, Тонкая волна", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Играть подряд", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf(
                                    Triple("Голосовые", autoPlayVoices) { autoPlayVoices = !autoPlayVoices },
                                    Triple("Кружки", autoPlayCircles) { autoPlayCircles = !autoPlayCircles },
                                    Triple("Музыка", autoPlayMusic) { autoPlayMusic = !autoPlayMusic }
                                ).forEach { (lbl, isOn, toggle) ->
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isOn) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.weight(1f).clickable { toggle() }
                                    ) {
                                        Text(
                                            text = lbl,
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            textAlign = TextAlign.Center,
                                            fontWeight = if (isOn) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isOn) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 6.dp, top = 6.dp)) {
                        Text("Свечение медиа", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("NEW", modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().clickable {
                            RedFoxConfig.update { it.copy(ambientPhotoGlow = !it.ambientPhotoGlow) }
                        }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Фото", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            ExpressiveSwitch(checked = settings.ambientPhotoGlow, onCheckedChange = { v -> RedFoxConfig.update { it.copy(ambientPhotoGlow = v) } })
                        }
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().clickable { videoGlow = !videoGlow }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Видео", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            ExpressiveSwitch(checked = videoGlow, onCheckedChange = { videoGlow = it })
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().clickable { activeSub = "DOWNLOAD" }
                    ) {
                        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)

Spacer(Modifier.width(12.dp))
Text("Загрузка", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
Text("Material", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
}
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable { activeSub = "CAMERA" }
) {
Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
Spacer(Modifier.width(12.dp))
Text("Камера", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
Text("Camera 2", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
}
}
}
if (activeSub == "PLAYER") {
item {
Text("Полоса перемотки", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}
item {
Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
listOf("DEFAULT" to "По умолчанию", "WAVY" to "Волнистый", "THIN" to "Тонкий", "FINE_WAVE" to "Тонкая волна").chunked(2).forEach { row ->
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
row.forEach { (code, label) ->
val selected = settings.seekbarStyle == code
Surface(
shape = RoundedCornerShape(18.dp),
color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.weight(1f).clickable {
RedFoxConfig.update { it.copy(seekbarStyle = code) }
}
) {
Text(
text = label,
modifier = Modifier.padding(vertical = 14.dp),
textAlign = TextAlign.Center,
fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
)
}
}
}
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Column(modifier = Modifier.padding(16.dp)) {
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
Text("Скорость волны", fontWeight = FontWeight.SemiBold)
Text("${settings.waveSpeedDpPerSec.toInt()} dp/с", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
Spacer(Modifier.height(8.dp))
ExpressiveM3Slider(
value = settings.waveSpeedDpPerSec,
onValueChange = { v -> RedFoxConfig.update { it.copy(waveSpeedDpPerSec = v) } },
valueRange = 8f..40f,
isExpressiveStyle = settings.expressiveSliders
)
}
}
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable {
RedFoxConfig.update { it.copy(liveLrcLyricsBar = !it.liveLrcLyricsBar) }
}
) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Показывать текст песни в мини-плеере", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
ExpressiveSwitch(checked = settings.liveLrcLyricsBar, onCheckedChange = { v -> RedFoxConfig.update { it.copy(liveLrcLyricsBar = v) } })
}
}
}
}
if (activeSub == "DOWNLOAD") {
item {
Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
val primaryColor = MaterialTheme.colorScheme.primary
val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
val flowerShape = remember { buildExpressiveAvatarShape(ExpressiveAvatarStyle.FLOWER_12) }
Box(
modifier = Modifier
.size(56.dp)
.clip(flowerShape)
.background(primaryColor.copy(alpha = 0.25f)),
contentAlignment = Alignment.Center
) {
Canvas(modifier = Modifier.size(48.dp)) {
drawArc(color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = dlThickness.dp.toPx(), cap = StrokeCap.Round))
drawArc(color = primaryColor, startAngle = -90f, sweepAngle = 235f, useCenter = false, style = Stroke(width = dlThickness.dp.toPx(), cap = StrokeCap.Round))
if (dlCenterStop) {
val sq = 10.dp.toPx()
drawRoundRect(
color = primaryColor,
topLeft = Offset((size.width - sq) / 2f, (size.height - sq) / 2f),
size = Size(sq, sq),
cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
)
}
}
}
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Column(modifier = Modifier.padding(16.dp)) {
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
Text("Толщина", fontWeight = FontWeight.SemiBold)
Text("${dlThickness.toInt()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
Spacer(Modifier.height(8.dp))
ExpressiveM3Slider(value = dlThickness, onValueChange = { dlThickness = it }, valueRange = 4f..14f, isExpressiveStyle = settings.expressiveSliders)
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Column(modifier = Modifier.padding(16.dp)) {
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
Text("Зазор", fontWeight = FontWeight.SemiBold)
Text("${dlGap.toInt()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
Spacer(Modifier.height(8.dp))
ExpressiveM3Slider(value = dlGap, onValueChange = { dlGap = it }, valueRange = 4f..18f, isExpressiveStyle = settings.expressiveSliders)
}
}
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable { dlCenterStop = !dlCenterStop }
) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Column(Modifier.weight(1f)) {
Text("Точка в центре", style = MaterialTheme.typography.titleMedium)
Text("Квадрат остановки внутри кольца", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
ExpressiveSwitch(checked = dlCenterStop, onCheckedChange = { dlCenterStop = it })
}
}
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable {
RedFoxConfig.update { it.copy(wavyDownloadRing = !it.wavyDownloadRing) }
}
) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Волнистое кольцо", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
ExpressiveSwitch(checked = settings.wavyDownloadRing, onCheckedChange = { v -> RedFoxConfig.update { it.copy(wavyDownloadRing = v) } })
}
}
}
}
if (activeSub == "CAMERA") {
item {
Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Row(
modifier = Modifier.padding(18.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(18.dp)
) {
Box(
modifier = Modifier
.size(width = 76.dp, height = 104.dp)
.clip(RoundedCornerShape(18.dp))
.border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
.padding(10.dp)
) {
Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
Box(Modifier.size(22.dp).clip(CircleShape).border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape))
Box(Modifier.size(22.dp).clip(CircleShape).border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape))
}
}
Text(
text = if (settings.cameraEngine == "CAMERA2") "2" else "1",
style = MaterialTheme.typography.displayMedium,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
listOf("TELEGRAM" to "Telegram", "CAMERA2" to "Camera 2", "CAMERA1" to "Camera 1").forEach { (code, label) ->
val selected = settings.cameraEngine == code
Surface(
shape = RoundedCornerShape(14.dp),
color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
modifier = Modifier.fillMaxWidth().clickable {
RedFoxConfig.update { it.copy(cameraEngine = code) }
}
) {
Text(
text = label,
modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
)
}
}
}
}
}
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable {
RedFoxConfig.update { it.copy(useBackCameraDefault = !it.useBackCameraDefault) }
}
) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Column(Modifier.weight(1f)) {
Text("Использовать заднюю камеру", style = MaterialTheme.typography.titleMedium)
Text("Для видеосообщений вместо фронтальной", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
ExpressiveSwitch(checked = settings.useBackCameraDefault, onCheckedChange = { v -> RedFoxConfig.update { it.copy(useBackCameraDefault = v) } })
}
}
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable { compactCamera = !compactCamera }
) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Column(Modifier.weight(1f)) {
Text("Компактная камера", style = MaterialTheme.typography.titleMedium)
Text("Круглая кнопка вместо превью в галерее", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
ExpressiveSwitch(checked = compactCamera, onCheckedChange = { compactCamera = it })
}
}
}
}
if (activeSub == "OTHER") {
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable {
RedFoxConfig.update { it.copy(autoCheckUpdates = !it.autoCheckUpdates) }
}
) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Проверять обновления RedFoxGram", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
ExpressiveSwitch(checked = settings.autoCheckUpdates, onCheckedChange = { v -> RedFoxConfig.update { it.copy(autoCheckUpdates = v) } })
}
}
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable {
val data = RedFoxConfig.exportSettingsOpex()
clipboard.setText(AnnotatedString(data))
statusMsg = "Файл настроек .opex скопирован в буфер обмена"
}
) {
Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Экспорт настроек (.opex)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}
}
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable {
val clip = clipboard.getText()?.text ?: ""
val ok = RedFoxConfig.importSettingsOpex(clip)
statusMsg = if (ok) "Настройки .opex успешно импортированы!" else "Скопируйте содержимое .opex в буфер"
}
) {
Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Импорт настроек (.opex)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}
}
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
modifier = Modifier.fillMaxWidth().clickable {
RedFoxConfig.update { RedFoxSettingsState() }
statusMsg = "Все настройки RedFoxGram сброшены до стандартных"
}
) {
Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Сбросить настройки RedFoxGram", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
}
}
}
}
if (statusMsg.isNotEmpty()) {
item {
Text(statusMsg, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
}
}
item { Spacer(Modifier.height(28.dp)) }
}
}
}
