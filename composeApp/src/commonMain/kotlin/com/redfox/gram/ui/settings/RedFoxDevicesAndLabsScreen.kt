package com.redfox.gram.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.engine.RedFoxTdEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxDevicesAndLabsScreen(
    mode: String, // "DEVICES" or "LABS"
    onBack: () -> Unit
) {
    val settings by RedFoxConfig.state.collectAsState()
    val currentUser by RedFoxTdEngine.currentUser.collectAsState()
    var statusText by remember { mutableStateOf("") }

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
                        text = if (mode == "DEVICES") "Устройства и Сессии" else "Экспериментальные (Labs)",
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
            if (mode == "DEVICES") {
                item {
                    Text(
                        "Текущий сеанс",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("RedFoxGram Android • M3E", fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Аккаунт: ${currentUser.fullName} • В сети",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "TDLib 1.8 • RedFox Shield Ready",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item {
                    FilledTonalButton(
                        onClick = {
                            RedFoxTdEngine.loadCurrentUserProfile()
                            statusText = "Список активных сессий TDLib синхронизирован"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Обновить статус сессий")
                    }
                }
            }

            if (mode == "LABS") {
                item {
                    Text(
                        "Медиа и Качество",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().clickable {
                            RedFoxConfig.update { it.copy(sendPhotosUncompressed = !it.sendPhotosUncompressed) }
                        }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Отправка фото без сжатия", style = MaterialTheme.typography.titleMedium)
                                Text("Передавать снимки в оригинальном разрешении", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.sendPhotosUncompressed, onCheckedChange = { v -> RedFoxConfig.update { it.copy(sendPhotosUncompressed = v) } })
                        }
                    }
                }

                item {
                    Text(
                        "Умные функции и Защита от случайных нажатий",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp, top = 6.dp)
                    )
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().clickable {
                            RedFoxConfig.update { it.copy(confirmBeforeCallOrVoice = !it.confirmBeforeCallOrVoice) }
                        }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Подтверждать звонки и голосовые", style = MaterialTheme.typography.titleMedium)
                                Text("Защита от случайного нажатия на микрофон или звонок", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.confirmBeforeCallOrVoice, onCheckedChange = { v -> RedFoxConfig.update { it.copy(confirmBeforeCallOrVoice = v) } })
                        }
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().clickable {
                            RedFoxConfig.update { it.copy(autoTranslateForeign = !it.autoTranslateForeign) }
                        }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Автоперевод иностранных сообщений", style = MaterialTheme.typography.titleMedium)
                                Text("Показывать кнопку мгновенного перевода в чатах", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.autoTranslateForeign, onCheckedChange = { v -> RedFoxConfig.update { it.copy(autoTranslateForeign = v) } })
                        }
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().clickable {
                            RedFoxConfig.update { it.copy(powerSavingMode = !it.powerSavingMode) }
                        }
                    ) {

Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Column(Modifier.weight(1f)) {
Text("Энергосбережение", style = MaterialTheme.typography.titleMedium)
Text("Уменьшить частоту фоновых анимаций для экономии заряда", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.powerSavingMode, onCheckedChange = { v -> RedFoxConfig.update { it.copy(powerSavingMode = v) } })
}
}
}
}
if (statusText.isNotEmpty()) {
item {
Text(statusText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
}
}
item { Spacer(Modifier.height(28.dp)) }
}
}
}
