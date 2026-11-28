package com.redfox.gram.ui.chat
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.engine.RedFoxTdEngine
import com.redfox.gram.media.rememberFilePickerLauncher
import com.redfox.gram.ui.components.ExpressiveWavyProgressBar
import com.redfox.gram.ui.home.ChatPreview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxChatScreen(
chat: ChatPreview,
onBack: () -> Unit
) {
val settings by RedFoxConfig.state.collectAsState()
val liveMessages by RedFoxTdEngine.activeChatMessages.collectAsState()
val lastReadOutboxMap by RedFoxTdEngine.lastReadOutboxMap.collectAsState()
val lastReadOutboxId = lastReadOutboxMap.get(chat.id) ?: 0L
var inputText by remember { mutableStateOf("") }
var inspectedMessage by remember { mutableStateOf<RedFoxTdEngine.LiveMessageItem?>(null) }
var uploadingFileName by remember { mutableStateOf<String?>(null) }
val listState = rememberLazyListState()
val pickFile = rememberFilePickerLauncher { path, isImage ->
val name = path.substringAfterLast('/')
uploadingFileName = name
RedFoxTdEngine.sendMediaFile(chat.id, path, isImage, inputText)
inputText = ""
}
LaunchedEffect(chat.id) {
RedFoxTdEngine.openChatHistory(chat.id)
}
LaunchedEffect(liveMessages.size) {
if (liveMessages.isNotEmpty()) {
uploadingFileName = null
listState.scrollToItem(liveMessages.size - 1)
}
}
inspectedMessage?.let { msg ->
AlertDialog(
onDismissRequest = { inspectedMessage = null },
title = { Text("Инспектор Opexgram M3E", fontWeight = FontWeight.Bold) },
text = {
Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
Text("Chat ID: ${chat.id}", style = MaterialTheme.typography.bodySmall)
Text("Message ID: ${msg.id}", style = MaterialTheme.typography.bodySmall)
Text("Направление: ${if (msg.isOutgoing) "Исходящее" else "Входящее"}", style = MaterialTheme.typography.bodySmall)
if (msg.isOutgoing) {
val readStatus = when {
!msg.isSentToServer -> "Отправляется на сервер (🕒)"
msg.id <= lastReadOutboxId -> "Прочитано собеседником (✓✓)"
else -> "Доставлено на сервер (✓)"
}
Text("Статус: $readStatus", style = MaterialTheme.typography.bodySmall)
}
HorizontalDivider()
Text(msg.text, style = MaterialTheme.typography.bodyMedium)
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
Column {
TopAppBar(
title = {
Column {
Text(chat.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
Text(
text = "${if (settings.ghostReadMessages) "Призрак ВКЛ" else "В сети"} • Анти-скриншот • Инспектор",
style = MaterialTheme.typography.labelSmall,
color = MaterialTheme.colorScheme.primary
)
}
},
navigationIcon = {
TextButton(onClick = onBack) {
Text("<- Чаты", fontWeight = FontWeight.Bold)
}
},
actions = {
TextButton(onClick = { RedFoxTdEngine.openChatHistory(chat.id) }) {
Text("↻")
}
}
)
uploadingFileName?.let { fileName ->
Surface(
color = MaterialTheme.colorScheme.secondaryContainer,
modifier = Modifier.fillMaxWidth()
) {
Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
Text(
text = "Отправка: $fileName",
style = MaterialTheme.typography.labelMedium,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.onSecondaryContainer
)
Spacer(Modifier.height(4.dp))
ExpressiveWavyProgressBar(progress = 0.72f, isPlaying = true)
}
}
}
}
},
bottomBar = {
Surface(
color = MaterialTheme.colorScheme.surfaceContainerHigh,
modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
) {
Row(
modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(6.dp)
) {
FilledTonalButton(
onClick = { pickFile() },
shape = CircleShape,
contentPadding = PaddingValues(horizontal = 12.dp),
modifier = Modifier.height(50.dp)
) {
Text("📎", style = MaterialTheme.typography.titleMedium)
}
OutlinedTextField(
value = inputText,
onValueChange = { inputText = it },
modifier = Modifier.weight(1f),
placeholder = {
Text(if (settings.hideTypingStatus) "Сообщение..." else "Сообщение...")
},
shape = CircleShape,
singleLine = true
)
Button(
onClick = {
if (inputText.isNotBlank()) {
RedFoxTdEngine.sendTextMessage(chat.id, inputText)
inputText = ""
}
},
shape = CircleShape,
modifier = Modifier.height(50.dp)
) {
Text("->", fontWeight = FontWeight.Bold)
}
}
}
}
) { padding ->
if (liveMessages.isEmpty()) {
Box(
modifier = Modifier.fillMaxSize().padding(padding),
contentAlignment = Alignment.Center
) {
Column(horizontalAlignment = Alignment.CenterHorizontally) {
CircularProgressIndicator()
Spacer(Modifier.height(12.dp))
Text("Загрузка сообщений из Telegram...", style = MaterialTheme.typography.bodyMedium)
}
}
} else {
LazyColumn(
state = listState,
modifier = Modifier
.fillMaxSize()
.padding(padding)
.padding(horizontal = 14.dp),
verticalArrangement = Arrangement.spacedBy(8.dp)
) {
item { Spacer(Modifier.height(4.dp)) }
items(liveMessages, key = { it.id }) { msg ->
val isSpam = remember(msg.id) {
listOf("казино", "крипто", "заработок", "ставка").any { msg.text.contains(it, ignoreCase = true) }
}
var revealSpam by remember { mutableStateOf(false) }
Row(
modifier = Modifier.fillMaxWidth(),
horizontalArrangement = if (msg.isOutgoing) Arrangement.End else Arrangement.Start
) {
Surface(
shape = RoundedCornerShape(
topStart = 22.dp,
topEnd = 22.dp,
bottomStart = if (msg.isOutgoing) 22.dp else 6.dp,
bottomEnd = if (msg.isOutgoing) 6.dp else 22.dp
),
color = when {
msg.isDeletedSaved && settings.antiDeleteMessages -> MaterialTheme.colorScheme.errorContainer
msg.isOutgoing -> MaterialTheme.colorScheme.primaryContainer
else -> MaterialTheme.colorScheme.surfaceContainerHigh
},
modifier = Modifier
.widthIn(max = 310.dp)
.clickable {
if (isSpam && !revealSpam) revealSpam = true
else inspectedMessage = msg
}
) {
Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
if (msg.isDeletedSaved && settings.antiDeleteMessages) {
Text(
text = "Удалено • Сохранено (Opex/Ayu)",
style = MaterialTheme.typography.labelSmall,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.error
)
Spacer(Modifier.height(4.dp))
}
if (isSpam && !revealSpam) {
Text(
"Скрыто фильтром Opexgram (нажмите для просмотра)",
style = MaterialTheme.typography.bodySmall,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
} else {
Text(
text = msg.text,
style = MaterialTheme.typography.bodyLarge,
color = if (msg.isOutgoing) {
MaterialTheme.colorScheme.onPrimaryContainer
} else {
MaterialTheme.colorScheme.onSurface
}
)
}
if (msg.isOutgoing) {
val isRead = msg.isSentToServer && msg.id <= lastReadOutboxId
val checkSymbol = when {
!msg.isSentToServer -> "🕒"
isRead -> "✓✓"
else -> "✓"
}
Row(
modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
verticalAlignment = Alignment.CenterVertically
) {
Text(
text = checkSymbol,
style = MaterialTheme.typography.labelSmall,
fontWeight = FontWeight.Bold,
color = if (isRead) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
)
}
}
}
}
}
}
item { Spacer(Modifier.height(8.dp)) }
}
}
}
}
