package com.redfox.gram.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.engine.RedFoxTdEngine
import com.redfox.gram.media.rememberFilePickerLauncher
import com.redfox.gram.ui.components.ExpressiveWavyProgressBar
import com.redfox.gram.ui.components.RedFoxLocalImage
import com.redfox.gram.ui.home.ChatPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


private val AccentPaperclipIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "AccentPaperclip",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2.1f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(21.44f, 11.05f)
            lineTo(12.25f, 20.24f)
            arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3.76f, 11.75f)
            lineTo(12.95f, 2.56f)
            arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18.61f, 8.22f)
            lineTo(9.41f, 17.41f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 6.58f, 14.58f)
            lineTo(15.07f, 6.1f)
        }
    }.build()
}

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
    val uiScope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }
    var selectedMessage by remember { mutableStateOf<RedFoxTdEngine.LiveMessageItem?>(null) }
    var showInspector by remember { mutableStateOf(false) }
    var replyTarget by remember { mutableStateOf<RedFoxTdEngine.LiveMessageItem?>(null) }
    var editingTarget by remember { mutableStateOf<RedFoxTdEngine.LiveMessageItem?>(null) }
    var fullscreenPhotoPath by remember { mutableStateOf<String?>(null) }
    var uploadingFileName by remember { mutableStateOf<String?>(null) }
    var vanishingIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
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

    // Полноэкранный просмотр фотографии
    fullscreenPhotoPath?.let { photoPath ->
        AlertDialog(
            onDismissRequest = { fullscreenPhotoPath = null },
            title = { Text("Просмотр фото • Ambient M3E", fontWeight = FontWeight.Bold) },
            text = {
                RedFoxLocalImage(
                    filePath = photoPath,
                    contentDescription = "Полноэкранное фото",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 420.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            },
            confirmButton = {
                TextButton(onClick = { fullscreenPhotoPath = null }) {
                    Text("Закрыть")
                }
            }
        )
    }

    // Контекстное меню действия над сообщением (Ответить / Редактировать / Удалить / Инспектор)
    selectedMessage?.let { msg ->
        if (!showInspector) {
            AlertDialog(
                onDismissRequest = { selectedMessage = null },
                title = { Text("Действия с сообщением", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = msg.text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider()
                        FilledTonalButton(
                            onClick = {
                                replyTarget = msg
                                editingTarget = null
                                selectedMessage = null

},
modifier = Modifier.fillMaxWidth()
) {
Text("Ответить (Reply)")
}
if (msg.isOutgoing) {
FilledTonalButton(
onClick = {
editingTarget = msg
replyTarget = null
inputText = msg.text
selectedMessage = null
},
modifier = Modifier.fillMaxWidth()
) {
Text("Редактировать текст")
}
}
FilledTonalButton(
onClick = { showInspector = true },
modifier = Modifier.fillMaxWidth()
) {
Text("Инспектор Opexgram M3E")
}
Button(
onClick = {
val targetId = msg.id
selectedMessage = null
vanishingIds = vanishingIds + targetId
uiScope.launch {
delay(360L)
RedFoxTdEngine.deleteChatMessage(chat.id, targetId, revoke = true)
}
},
colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
modifier = Modifier.fillMaxWidth()
) {
Text("Удалить у всех (с распадом)", color = MaterialTheme.colorScheme.onError)
}
}
},
confirmButton = {
TextButton(onClick = { selectedMessage = null }) {
Text("Отмена")
}
}
)
} else {
AlertDialog(
onDismissRequest = {
showInspector = false
selectedMessage = null
},
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
TextButton(onClick = {
showInspector = false
selectedMessage = null
}) {
Text("Закрыть")
}
}
)
}
}
Scaffold(
topBar = {
Column {
TopAppBar(
title = {
Column {
Text(chat.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
Text(
text = "${if (settings.ghostReadMessages) "Призрак ВКЛ" else "В сети"} • Фото Ambient • Редактор",
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
Column {
// Плашка ответа (Reply) или редактирования (Edit) над полем ввода
val activeBanner = editingTarget ?: replyTarget
if (activeBanner != null) {
Row(
modifier = Modifier
.fillMaxWidth()
.background(MaterialTheme.colorScheme.secondaryContainer)
.padding(horizontal = 16.dp, vertical = 6.dp),
verticalAlignment = Alignment.CenterVertically
) {
Column(modifier = Modifier.weight(1f)) {
Text(
text = if (editingTarget != null) "Редактирование сообщения" else "Ответ на сообщение",
style = MaterialTheme.typography.labelSmall,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.primary
)
Text(
text = activeBanner.text,
style = MaterialTheme.typography.bodySmall,
maxLines = 1,
overflow = TextOverflow.Ellipsis
)
}
TextButton(onClick = {
replyTarget = null
editingTarget = null
inputText = ""
}) {
Text("✕")
}
}
}
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
Icon(AccentPaperclipIcon, contentDescription = "Прикрепить", tint = MaterialTheme.colorScheme.primary)
}
OutlinedTextField(
value = inputText,
onValueChange = { inputText = it },
modifier = Modifier.weight(1f),
placeholder = { Text("Сообщение...") },
shape = CircleShape,
singleLine = true
)
Button(
onClick = {
val textToSend = inputText.trim()
if (textToSend.isNotEmpty()) {
when {
editingTarget != null -> {
RedFoxTdEngine.editTextMessage(chat.id, editingTarget!!.id, textToSend)
editingTarget = null
}
replyTarget != null -> {
RedFoxTdEngine.sendReplyMessage(chat.id, replyTarget!!.id, textToSend)
replyTarget = null
}
else -> {
RedFoxTdEngine.sendTextMessage(chat.id, textToSend)
}
}
inputText = ""
}
},
shape = CircleShape,
modifier = Modifier.height(50.dp)
) {
Text(if (editingTarget != null) "✓" else "->", fontWeight = FontWeight.Bold)
}
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
AnimatedVisibility(
visible = !vanishingIds.contains(msg.id),
exit = fadeOut(tween(340)) + scaleOut(tween(340), targetScale = 0.65f)
) {
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
else selectedMessage = msg
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
// Отображение реальной фотографии из Telegram с подсветкой Ambient Mode
if (msg.mediaLocalPath.isNotBlank()) {
RedFoxLocalImage(
filePath = msg.mediaLocalPath,
contentDescription = "Фото",
modifier = Modifier
.fillMaxWidth()
.heightIn(min = 140.dp, max = 240.dp)
.padding(bottom = 6.dp)
.shadow(10.dp, RoundedCornerShape(16.dp))
.clip(RoundedCornerShape(16.dp))
.clickable { fullscreenPhotoPath = msg.mediaLocalPath }
)
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
Row(
modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(4.dp)
) {
if (msg.isEdited) {
Text(
text = "изм.",
style = MaterialTheme.typography.labelSmall,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
}
if (msg.isOutgoing) {
val isRead = msg.isSentToServer && msg.id <= lastReadOutboxId
val checkSymbol = when {
!msg.isSentToServer -> "🕒"
isRead -> "✓✓"
else -> "✓"
}
Text(
text = checkSymbol,
style = MaterialTheme.typography.labelSmall,
fontWeight = FontWeight.Bold,
color = if (isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
)
}
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
