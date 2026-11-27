package com.redfox.gram.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.ui.home.ChatPreview

data class ChatMessageItem(
    val id: Long,
    val senderName: String,
    val text: String,
    val translatedText: String? = null,
    val time: String,
    val isOutgoing: Boolean,
    val isDeletedByPeer: Boolean = false,
    val editHistory: String? = null,
    val clientDevice: String = "Android • RedFox M3E",
    val dcLocation: String = "DC2 (Amsterdam)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxChatScreen(
    chat: ChatPreview,
    onBack: () -> Unit
) {
    val settings by RedFoxConfig.state.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var inspectedMessage by remember { mutableStateOf<ChatMessageItem?>(null) }
    var actionBanner by remember { mutableStateOf<String?>(null) }

    val messages = remember(chat.id) {
        mutableStateListOf(
            ChatMessageItem(
                id = 101,
                senderName = chat.title,
                text = "Welcome to RedFoxGram M3E! All open-source features combined in one clean UI.",
                translatedText = "Добро пожаловать в RedFoxGram M3E! Все лучшие open-source фичи в одном чистом интерфейсе.",
                time = "15:40",
                isOutgoing = false,
                clientDevice = "Linux Desktop • Compose",
                dcLocation = "DC2 (Amsterdam)"
            ),
            ChatMessageItem(
                id = 102,
                senderName = "Вы",
                text = "Проверил сборку на Android, Linux и Windows — анимации Material 3 Expressive летают!",
                time = "15:42",
                isOutgoing = true,
                editHistory = "Проверил сборку на Android — всё работает",
                clientDevice = "Android 15 • RedFox M3E",
                dcLocation = "DC2 (Amsterdam)"
            ),
            ChatMessageItem(
                id = 103,
                senderName = chat.title,
                text = "Это сообщение собеседник удалил для всех, но модуль Ayu/Opex сохранил его в локальной базе.",
                time = "15:44",
                isOutgoing = false,
                isDeletedByPeer = true,
                clientDevice = "iOS • AyuGram",
                dcLocation = "DC4 (Amsterdam)"
            ),
            ChatMessageItem(
                id = 104,
                senderName = "Спам-бот",
                text = "Супер-акция! Заходи в онлайн-казино и забирай бонус прямо сейчас!",
                time = "15:45",
                isOutgoing = false,
                clientDevice = "Web K • BotAPI",
                dcLocation = "DC5 (Singapore)"
            )
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    // Диалог инспектора сообщений Opexgram + действия Cherrygram/AyuGram
    inspectedMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { inspectedMessage = null },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text("Opexgram Inspector", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ID сообщения: #${msg.id} • Чат: ${chat.id}", style = MaterialTheme.typography.bodySmall)
                    if (settings.opexShowClientDeviceIcon) {
                        Text("Клиент: ${msg.clientDevice}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Дата-центр: ${msg.dcLocation}", style = MaterialTheme.typography.bodySmall)
                    Text("Время отправки: ${msg.time}:14 UTC+3", style = MaterialTheme.typography.bodySmall)

                    if (msg.editHistory != null && settings.saveEditHistory) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "История правки (AyuGram):\n«${msg.editHistory}»",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            text = "{\"id\":${msg.id},\"dc\":\"${msg.dcLocation.take(3)}\",\"deleted\":${msg.isDeletedByPeer}}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    if (settings.quickForwardNoQuote) {
                        FilledTonalButton(
                            onClick = {
                                actionBanner = "Cherrygram: переслано без автора (#${msg.id})"
                                inspectedMessage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Переслать без автора (Cherrygram)")
                        }
                    }
                    if (settings.opexQuickSaveToFavorites) {
                        OutlinedButton(
                            onClick = {
                                actionBanner = "Opexgram: сохранено в Избранное"
                                inspectedMessage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("В Избранное в 1 тап (Opexgram)")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedMessage = null }) {
                    Text("Закрыть")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(chat.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val statusParts = buildList {
                            if (settings.ghostReadMessages) add("Призрак ВКЛ")
                            if (settings.opexAntiScreenshotAlert) add("Анти-скриншот")
                            if (settings.opexMessageInspector) add("Инспектор")
                        }
                        Text(
                            text = if (statusParts.isEmpty()) "в сети" else statusParts.joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("<- Чаты")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(if (settings.hideTypingStatus) "Сообщение (без статуса набора)..." else "Сообщение...")
                        },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                messages.add(
                                    ChatMessageItem(
                                        id = (messages.maxOfOrNull { it.id } ?: 100) + 1,
                                        senderName = "Вы",
                                        text = inputText.trim(),

time = "Сейчас",
isOutgoing = true
)
)
inputText = ""
}
},
shape = CircleShape,
contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
) {
Text("->")
}
}
}
}
) { padding ->
Column(
modifier = Modifier
.fillMaxSize()
.padding(padding)
) {
actionBanner?.let { banner ->
Surface(
color = MaterialTheme.colorScheme.tertiaryContainer,
modifier = Modifier.fillMaxWidth()
) {
Row(
modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
verticalAlignment = Alignment.CenterVertically
) {
Text(
text = banner,
style = MaterialTheme.typography.labelMedium,
modifier = Modifier.weight(1f)
)
TextButton(onClick = { actionBanner = null }) {
Text("OK")
}
}
}
}
LazyColumn(
state = listState,
modifier = Modifier
.fillMaxSize()
.padding(horizontal = 12.dp),
verticalArrangement = Arrangement.spacedBy(10.dp),
contentPadding = PaddingValues(vertical = 12.dp)
) {
items(messages, key = { it.id }) { msg ->
ExpressiveMessageBubble(
message = msg,
autoTranslate = settings.autoTranslateForeign,
spamFilterEnabled = settings.opexKeywordSpamFilter,
inspectorEnabled = settings.opexMessageInspector,
showDevice = settings.opexShowClientDeviceIcon,
onInspect = { inspectedMessage = msg }
)
}
}
}
}
}
@Composable
private fun ExpressiveMessageBubble(
message: ChatMessageItem,
autoTranslate: Boolean,
spamFilterEnabled: Boolean,
inspectorEnabled: Boolean,
showDevice: Boolean,
onInspect: () -> Unit
) {
var showTranslation by remember(autoTranslate) { mutableStateOf(autoTranslate && message.translatedText != null) }
val spamKeywords = listOf("казино", "ставки", "крипто-сигнал")
val isSpam = spamFilterEnabled && !message.isOutgoing && spamKeywords.any { message.text.contains(it, ignoreCase = true) }
var revealSpam by remember { mutableStateOf(false) }
val bubbleShape = if (message.isOutgoing) {
RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 6.dp)
} else {
RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 24.dp)
}
val containerColor = when {
message.isDeletedByPeer -> MaterialTheme.colorScheme.errorContainer
message.isOutgoing -> MaterialTheme.colorScheme.primaryContainer
else -> MaterialTheme.colorScheme.surfaceContainerHigh
}
val contentColor = when {
message.isDeletedByPeer -> MaterialTheme.colorScheme.onErrorContainer
message.isOutgoing -> MaterialTheme.colorScheme.onPrimaryContainer
else -> MaterialTheme.colorScheme.onSurface
}
Box(
modifier = Modifier.fillMaxWidth(),
contentAlignment = if (message.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
) {
Surface(
shape = bubbleShape,
color = containerColor,
modifier = Modifier
.widthIn(max = 320.dp)
.clickable(enabled = inspectorEnabled) { onInspect() }
) {
if (isSpam && !revealSpam) {
Row(
modifier = Modifier
.clickable { revealSpam = true }
.padding(horizontal = 14.dp, vertical = 10.dp),
verticalAlignment = Alignment.CenterVertically
) {
Text(
text = "Скрыто фильтром Opexgram (нажмите для просмотра)",
style = MaterialTheme.typography.labelMedium,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
}
} else {
Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
if (message.isDeletedByPeer) {
Text(
text = "Удалено собеседником • Сохранено (Ayu/Opex)",
style = MaterialTheme.typography.labelSmall,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.error
)
Spacer(Modifier.height(4.dp))
}
if (showDevice && !message.isOutgoing) {
Text(
text = message.clientDevice,
style = MaterialTheme.typography.labelSmall,
color = contentColor.copy(alpha = 0.65f)
)
Spacer(Modifier.height(2.dp))
}
val bodyText = if (showTranslation && message.translatedText != null) {
message.translatedText
} else {
message.text
}
AnimatedContent(
targetState = bodyText,
transitionSpec = {
(fadeIn(spring(stiffness = Spring.StiffnessLow)) + slideInVertically { it / 4 })
.togetherWith(fadeOut() + slideOutVertically { -it / 4 })
}
) { animatedBody ->
Text(
text = animatedBody,
style = MaterialTheme.typography.bodyLarge,
color = contentColor
)
}
if (message.translatedText != null) {
Spacer(Modifier.height(6.dp))
SuggestionChip(
onClick = { showTranslation = !showTranslation },
label = {
Text(
if (showTranslation) "Показать оригинал (EN)" else "Перевести на русский",
style = MaterialTheme.typography.labelSmall
)
}
)
}
Spacer(Modifier.height(4.dp))
Row(
modifier = Modifier.align(Alignment.End),
verticalAlignment = Alignment.CenterVertically
) {
if (message.editHistory != null) {
Text(
text = "ред. • ",
style = MaterialTheme.typography.labelSmall,
color = contentColor.copy(alpha = 0.7f)
)
}
Text(
text = message.time,
style = MaterialTheme.typography.labelSmall,
color = contentColor.copy(alpha = 0.75f)
)
}
}
}
}
}
}
