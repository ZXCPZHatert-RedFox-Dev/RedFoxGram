package com.redfox.gram.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.config.RedFoxSettingsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxTweaksScreen(onBack: () -> Unit) {
    val state: RedFoxSettingsState by RedFoxConfig.state.collectAsState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("RedFox Tweaks", fontWeight = FontWeight.Bold)
                        Text(
                            "Приватность, обход лимитов и плеер",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Назад")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader("Режим призрака (Opex-стандарт)")
                SettingsCard {
                    TweakSwitchRow(
                        title = "Не читать сообщения",
                        subtitle = "Скрывать галочки прочтения до вашего ответа",
                        checked = state.ghostReadMessages,
                        onCheckedChange = { v -> RedFoxConfig.update { it.copy(ghostReadMessages = v) } }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    TweakSwitchRow(
                        title = "Анонимные истории",
                        subtitle = "Смотреть сторис без попадания в список зрителей",
                        checked = state.ghostReadStories,
                        onCheckedChange = { v -> RedFoxConfig.update { it.copy(ghostReadStories = v) } }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    TweakSwitchRow(
                        title = "Скрывать статус «печатает...»",
                        subtitle = "Не показывать собеседнику набор текста и запись ГС",
                        checked = state.hideTypingStatus,
                        onCheckedChange = { v -> RedFoxConfig.update { it.copy(hideTypingStatus = v) } }
                    )
                }
            }

            item {
                SectionHeader("Защита и локальные улучшения")
                SettingsCard {
                    TweakSwitchRow(
                        title = "Анти-удаление сообщений",
                        subtitle = "Сохранять удаленные собеседником сообщения с пометкой",
                        checked = state.antiDeleteMessages,
                        onCheckedChange = { v -> RedFoxConfig.update { it.copy(antiDeleteMessages = v) } }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    TweakSwitchRow(
                        title = "Отключить рекламу",
                        subtitle = "Фильтровать спонсорские посты в каналах",
                        checked = state.disableSponsoredAds,
                        onCheckedChange = { v -> RedFoxConfig.update { it.copy(disableSponsoredAds = v) } }
                    )
                }
            }

            item {
                SectionHeader("Мультимедиа и плагины RedFox")
                SettingsCard {
                    TweakSwitchRow(
                        title = "Живой бар текста (LRCLIB)",
                        subtitle = "Автоматически подгружать синхронный текст песен",
                        checked = state.liveLrcLyricsBar,
                        onCheckedChange = { v -> RedFoxConfig.update { it.copy(liveLrcLyricsBar = v) } }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    TweakSwitchRow(
                        title = "Система плагинов (.rfplugin)",
                        subtitle = "Разрешить установку скриптов и тем из чатов",
                        checked = state.pluginsEnabled,
                        onCheckedChange = { v -> RedFoxConfig.update { it.copy(pluginsEnabled = v) } }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun TweakSwitchRow(
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
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
