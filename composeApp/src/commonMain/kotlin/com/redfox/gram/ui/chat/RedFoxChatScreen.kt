package com.redfox.gram.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val editHistory: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxChatScreen(
    chat: ChatPreview,
    onBack: () -> Unit
) {
    val settings by RedFoxConfig.state.collectAsState()
    var inputText by remember { mutableStateOf("") }

    val messages = remember(chat.id) {
        mutableStateListOf(
            ChatMessageItem(
                id = 101,
                senderName = chat.title,
                text = "Welcome to RedFoxGram M3E! All open-source features combined in one clean UI.",
                translatedText = "Добро пожаловать в RedFoxGram M3E! Все лучшие open-source фичи в одном чистом интерфейсе.",
                time = "15:40",
                isOutgoing = false
            ),
            ChatMessageItem(
                id = 102,
                senderName = "Вы",
                text = "Проверил сборку на Android, Linux и Windows — анимации Material 3 Expressive летают!",
                time = "15:42",
                isOutgoing = true,
                editHistory = "Проверил сборку на Android — всё работает"
            ),
            ChatMessageItem(
                id = 103,
                senderName = chat.title,
                text = "Это сообщение собеседник удалил для всех, но модуль Ayu/Opex сохранил его в локальной базе.",
                time = "15:44",
                isOutgoing = false,
                isDeletedByPeer = true
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← Чаты") }
                },
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(chat.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (settings.showUserIdsAndDc) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "DC2 • ID:${chat.id}0948",
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (settings.ghostReadMessages) "Режим призрака активен • Не читает" else "В сети",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(if (settings.hideTypingStatus) "Сообщение (без статуса набора)..." else "Сообщение M3E...")
                        },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                messages.add(
                                    ChatMessageItem(
                                        id = System.currentTimeMillis(),
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
                        Text("Отпр.")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            items(messages, key = { it.id }) { msg ->
                if (msg.isDeletedByPeer && !settings.antiDeleteMessages) return@items
                ExpressiveMessageBubble(
                    message = msg,
                    autoTranslate = settings.autoTranslateForeign
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ExpressiveMessageBubble(
    message: ChatMessageItem,
    autoTranslate: Boolean
) {
    var showTranslation by remember(autoTranslate) { mutableStateOf(autoTranslate) }

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

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = containerColor,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (message.isDeletedByPeer) {
                    Text(
                        text = "УДАЛЕНО СОБЕСЕДНИКОМ • СОХРАНЕНО",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(4.dp))
                }

                val displayText = if (showTranslation && message.translatedText != null) {
                    message.translatedText
                } else {
                    message.text
                }

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )

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
