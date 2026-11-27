package com.redfox.gram.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.ClientPreset
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
                        Text("RedFox M3E Hub", fontWeight = FontWeight.Bold)
                        Text(
                            "AyuGram • Cherrygram • Hortay • MonoGram • extera • Mategram",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("<- Назад")
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
                SectionHeader("Пресеты клиентов (Быстрое переключение)")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClientPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = state.activePreset == preset,
                            onClick = { RedFoxConfig.applyPreset(preset) },
                            label = { Text(preset.title) }
                        )
                    }
                }
            }

            // Блок 0: Opexgram (Безопасность, Инспектор и Фильтры)
            item {
                SectionHeader("Opexgram (Защита, Инспектор и Фильтры)")
                SettingsCard {
                    TweakSwitchRow(
                        title = "Ложный пароль-маскировка (Stealth Pass)",
                        subtitle = "Скрывать личные чаты и архив при вводе резервного кода",
                        checked = state.opexFakePasscodeMask,
                        onCheckedChange = { v -> RedFoxConfig.update { it.copy(opexFakePasscodeMask = v) } }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    TweakSwitchRow(
                        title = "Инспектор сообщений (ID, DC, JSON)",
                        subtitle = "Показывать дата-центр (DC1-DC5), точное время и сырые данные",
                        checked = state.opexMessageInspector,
                        onCheckedChange = { v -> RedFoxConfig.update { it.copy(opexMessageInspector = v) } }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    TweakSwitchRow(
                        title = "Фильтр спама по ключевым словам",
                        subtitle = "Скрывать посты и сообщения по локальному списку стоп-слов",
                        checked = state.opexKeywordSpamFilter,
                        onCheckedChange = { v -> RedFoxConfig.update { it.copy(opexKeywordSpamFilter = v) } }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    TweakSwitchRow(
                        title = "Анти-скриншот и иконки устройств",
                        subtitle = "Блокировать отчеты о скриншотах и показывать ОС собеседника",
                        checked = state.opexAntiScreenshotAlert,
                        onCheckedChange = { v -> RedFoxConfig.update { it.copy(opexAntiScreenshotAlert = v, opexShowClientDeviceIcon = v) } }
                    )
                }
            }

            item {
                SectionHeader("AyuGram & Opex (Стелс и история)")
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
                        title = "Анти-удаление и история правок",
                        subtitle = "Сохранять удаленные сообщения и старые версии текста",
                        checked = state.antiDeleteMessages,
                        onCheckedChange = { v -> RedFoxConfig.update { it.copy(antiDeleteMessages = v, saveEditHistory = v) } }
                    )
                }
            }

            item {
                SectionHeader("Cherrygram & exteraGram (Чаты и медиа)")
                SettingsCard {
                    TweakSwitchRow(
                        title = "Пересылка без автора",
                        subtitle = "Быстрая пересылка сообщений без ссылки на отправителя",
                        checked = state.quickForwardNoQuote,
                        onCheckedChange = { v -> RedFoxConfig.update { it.copy(quickForwardNoQuote = v) } }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    TweakSwitchRow(
                        title = "Перемотка кружочков",
                        subtitle = "Полоса прогресса для видеосообщений (extera/Materialgram)",
                        checked = state.seekableVideoNotes,
                        onCheckedChange = { v -> RedFoxConfig.update { it.copy(seekableVideoNotes = v) } }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    TweakSwitchRow(
                        title = "Отключить рекламу и скрыть номер",
                        subtitle = "Убирать спонсорские посты и прятать телефон в меню",
                        checked = state.disableSponsoredAds,
                        onCheckedChange = { v -> RedFoxConfig.update { it.copy(disableSponsoredAds = v, hidePhoneNumber = v) } }
                    )
                }
            }

            item {

SectionHeader("Hortay, MonoGram & Materialgram (M3E)")
SettingsCard {
TweakSwitchRow(
title = "Лента каналов (Hortay Feed Mode)",
subtitle = "Объединять посты из всех подписок в единую M3E-ленту",
checked = state.channelFeedMode,
onCheckedChange = { v -> RedFoxConfig.update { it.copy(channelFeedMode = v) } }
)
HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
TweakSwitchRow(
title = "Пружинная физика (MonoGram M3E)",
subtitle = "Тактильные spring-анимации карточек и списков",
checked = state.springPhysicsScroll,
onCheckedChange = { v -> RedFoxConfig.update { it.copy(springPhysicsScroll = v) } }
)
HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
TweakSwitchRow(
title = "Меню админа и шрифт Google Sans",
subtitle = "Быстрые действия в группах и фирменная типографика",
checked = state.adminQuickMenu,
onCheckedChange = { v -> RedFoxConfig.update { it.copy(adminQuickMenu = v, googleSansFont = v) } }
)
}
}
item {
SectionHeader("Mategram, NekoX & Мультимедиа RedFox")
SettingsCard {
TweakSwitchRow(
title = "Автоперевод чатов (Mategram / NekoX)",
subtitle = "Мгновенный перевод иностранных сообщений на лету",
checked = state.autoTranslateForeign,
onCheckedChange = { v -> RedFoxConfig.update { it.copy(autoTranslateForeign = v) } }
)
HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
TweakSwitchRow(
title = "Живой бар текста (LRCLIB) и волна",
subtitle = "Синхронный текст песен и волнистый прогресс-бар",
checked = state.liveLrcLyricsBar,
onCheckedChange = { v -> RedFoxConfig.update { it.copy(liveLrcLyricsBar = v, wavySeekbar = v) } }
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
