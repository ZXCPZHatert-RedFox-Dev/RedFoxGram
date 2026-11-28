package com.redfox.gram.ui.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxChatsTweaksScreen(onBack: () -> Unit) {
    val settings by RedFoxConfig.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                title = { Text("Чаты и Папки", fontWeight = FontWeight.Bold) }
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
            item {
                Text("Папки", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("TELEGRAM" to "Telegram", "TABS" to "Вкладки", "BUTTONS" to "Кнопки").forEach { (code, label) ->
                        val selected = settings.folderTabStyle == code
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.weight(1f).clickable {
                                RedFoxConfig.update { it.copy(folderTabStyle = code) }
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
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().clickable {
                        RedFoxConfig.update { it.copy(hideAllChatsFolder = !it.hideAllChatsFolder) }
                    }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Скрыть папку «Все чаты»", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Switch(checked = settings.hideAllChatsFolder, onCheckedChange = { v -> RedFoxConfig.update { it.copy(hideAllChatsFolder = v) } })
                    }
                }
            }

            item {
                Text("Эффект удаления сообщений", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("DUST" to "Пыль", "SHARDS" to "Осколки", "DYNAMITE" to "Динамит", "PORTAL" to "Портал").forEach { (code, label) ->
                        val selected = settings.deleteEffectStyle == code
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.weight(1f).clickable {
                                RedFoxConfig.update { it.copy(deleteEffectStyle = code) }
                            }
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(vertical = 12.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                Text("Поле ввода и Анимация текста", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("TELEGRAM" to "Telegram", "EXPRESSIVE" to "Expressive", "BUTTONS" to "Кнопки").forEach { (code, label) ->
                        val selected = settings.inputFieldStyle == code
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.weight(1f).clickable {
                                RedFoxConfig.update { it.copy(inputFieldStyle = code) }
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
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().clickable {
                        RedFoxConfig.update { it.copy(animatedTypingCursor = !it.animatedTypingCursor) }
                    }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Анимация текста и плавный курсор", style = MaterialTheme.typography.titleMedium)
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("NEW", modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            Text("Буквы плавно проявляются, курсор плывёт", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = settings.animatedTypingCursor, onCheckedChange = { v -> RedFoxConfig.update { it.copy(animatedTypingCursor = v) } })
                    }
                }
            }

            item {
                Text("Жесты и Сведения в сообщениях", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
            }
            item {

Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
Text("Двойное нажатие на сообщение", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
listOf("Нет", "❤️", "🔥", "👍", "😂").forEach { reaction ->
val selected = settings.doubleTapReaction == reaction
FilterChip(
selected = selected,
onClick = { RedFoxConfig.update { it.copy(doubleTapReaction = reaction) } },
shape = CircleShape,
label = { Text(reaction) },
modifier = Modifier.weight(1f)
)
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
RedFoxConfig.update { it.copy(showSecondsInTime = !it.showSecondsInTime) }
}
) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Секунды во времени", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
Switch(checked = settings.showSecondsInTime, onCheckedChange = { v -> RedFoxConfig.update { it.copy(showSecondsInTime = v) } })
}
}
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable {
RedFoxConfig.update { it.copy(showScrollToTopBtn = !it.showScrollToTopBtn) }
}
) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Column(Modifier.weight(1f)) {
Text("Кнопка листания вверх", style = MaterialTheme.typography.titleMedium)
Text("Быстрый переход к началу переписки", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
Switch(checked = settings.showScrollToTopBtn, onCheckedChange = { v -> RedFoxConfig.update { it.copy(showScrollToTopBtn = v) } })
}
}
}
item { Spacer(Modifier.height(28.dp)) }
}
}
}
