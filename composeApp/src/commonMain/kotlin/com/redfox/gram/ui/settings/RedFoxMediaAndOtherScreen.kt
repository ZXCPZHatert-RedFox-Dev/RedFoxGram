package com.redfox.gram.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.config.RedFoxSettingsState
import com.redfox.gram.ui.components.ExpressiveM3Slider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxMediaAndOtherScreen(
    section: String, // "PLAYER", "DOWNLOAD", "CAMERA", "OTHER"
    onBack: () -> Unit
) {
    val settings by RedFoxConfig.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    var statusMsg by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                title = {
                    Text(
                        text = when (section) {
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
            if (section == "PLAYER") {
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
                            com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.liveLrcLyricsBar, onCheckedChange = { v -> RedFoxConfig.update { it.copy(liveLrcLyricsBar = v) } })
                        }
                    }
                }
            }

            if (section == "DOWNLOAD") {
                item {
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            Canvas(modifier = Modifier.size(64.dp)) {
                                drawArc(color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                                drawArc(color = primaryColor, startAngle = -90f, sweepAngle = 240f, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                            }
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
                            Column(Modifier.weight(1f)) {
                                Text("Волнистое кольцо загрузки M3E", style = MaterialTheme.typography.titleMedium)
                                Text("Фигура, затем кольцо прогресса при скачивании файлов", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.wavyDownloadRing, onCheckedChange = { v -> RedFoxConfig.update { it.copy(wavyDownloadRing = v) } })
                        }
                    }
                }
            }

            if (section == "CAMERA") {
                item {
                    Text("Тип камеры", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("TELEGRAM" to "Telegram", "CAMERA2" to "Camera 2", "CAMERA1" to "Camera 1").forEach { (code, label) ->

val selected = settings.cameraEngine == code
Surface(
shape = RoundedCornerShape(18.dp),
color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.weight(1f).clickable {
RedFoxConfig.update { it.copy(cameraEngine = code) }
}
) {
Text(
text = label,
modifier = Modifier.padding(vertical = 14.dp),
textAlign = TextAlign.Center,
fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
)
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
com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.useBackCameraDefault, onCheckedChange = { v -> RedFoxConfig.update { it.copy(useBackCameraDefault = v) } })
}
}
}
}
if (section == "OTHER") {
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
com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.autoCheckUpdates, onCheckedChange = { v -> RedFoxConfig.update { it.copy(autoCheckUpdates = v) } })
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
