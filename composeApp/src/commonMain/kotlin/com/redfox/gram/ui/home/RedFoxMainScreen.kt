package com.redfox.gram.ui.home
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.config.RedFoxSettingsState
import com.redfox.gram.ui.chat.RedFoxChatScreen
import com.redfox.gram.ui.player.LiveLrcMiniBar
import com.redfox.gram.ui.player.RedFoxFullPlayerScreen
import com.redfox.gram.ui.settings.RedFoxTweaksScreen
data class ChatPreview(
val id: Long,
val title: String,
val lastMessage: String,
val time: String,
val unreadCount: Int = 0,
val isDeletedSaved: Boolean = false,
val translatedTag: String? = null
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxMainScreen() {
val settings: RedFoxSettingsState by RedFoxConfig.state.collectAsState()
var showTweaks by remember { mutableStateOf(false) }
var showFullPlayer by remember { mutableStateOf(false) }
var openedChat by remember { mutableStateOf<ChatPreview?>(null) }
var isPlaying by remember { mutableStateOf(true) }
if (showTweaks) {
RedFoxTweaksScreen(onBack = { showTweaks = false })
return
}
if (showFullPlayer) {
RedFoxFullPlayerScreen(
trackTitle = "Midnight City (M3E Remix)",
artist = "RedFox Audio",
isPlaying = isPlaying,
onTogglePlay = { isPlaying = !isPlaying },
onBack = { showFullPlayer = false }
)
return
}
openedChat?.let { activeChat ->
RedFoxChatScreen(
chat = activeChat,
onBack = { openedChat = null }
)
return
}
val demoChats = remember {
listOf(
ChatPreview(
id = 1,
title = "RedFox Dev Channel",
lastMessage = "Нажмите на чат, чтобы открыть переписку M3E с автопереводом и анти-удалением!",
time = "16:38",
unreadCount = 2
),
ChatPreview(
id = 2,
title = "Opexgram & AyuGram Stealth",
lastMessage = "Сообщение было удалено собеседником, но сохранено в локальной базе.",
time = "14:20",
isDeletedSaved = true
),
ChatPreview(
id = 3,
title = "MonoGram & Mategram M3E",
lastMessage = "Material 3 Expressive brings tactile spring physics to desktop and mobile.",
time = "12:10",
translatedTag = "EN → RU"
)
)
}
Scaffold(
topBar = {
Column {
CenterAlignedTopAppBar(
title = {
Row(verticalAlignment = Alignment.CenterVertically) {
Text(
"RedFoxGram",
style = MaterialTheme.typography.titleLarge,
fontWeight = FontWeight.ExtraBold
)
Spacer(Modifier.width(8.dp))
Surface(
shape = CircleShape,
color = MaterialTheme.colorScheme.secondaryContainer
) {
Text(
settings.activePreset.badge,
modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
style = MaterialTheme.typography.labelSmall,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.onSecondaryContainer
)
}
}
}
)
if (settings.liveLrcLyricsBar) {
LiveLrcMiniBar(
trackTitle = "Midnight City (M3E Remix)",
artist = "RedFox Audio",
currentLine = "♪ Waiting in a car, waiting for a ride in the dark...",
isPlaying = isPlaying,
onTogglePlay = { isPlaying = !isPlaying },
onOpenFullPlayer = { showFullPlayer = true }
)
}
}
},
bottomBar = {
Box(
modifier = Modifier
.fillMaxWidth()
.padding(bottom = 16.dp, start = 24.dp, end = 24.dp),
contentAlignment = Alignment.Center
) {
Surface(
shape = CircleShape,
color = MaterialTheme.colorScheme.surfaceContainerHighest,
shadowElevation = 6.dp
) {
Row(
modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
horizontalArrangement = Arrangement.spacedBy(8.dp),
verticalAlignment = Alignment.CenterVertically
) {
FilledTonalButton(
onClick = {
RedFoxConfig.update { current: RedFoxSettingsState ->
current.copy(ghostReadMessages = !current.ghostReadMessages)
}
},
shape = CircleShape
) {
Text(if (settings.ghostReadMessages) "Призрак: ВКЛ" else "Призрак: ВЫКЛ")
}
Button(
onClick = { showTweaks = true },
shape = CircleShape
) {
Text("All-in-One M3E")
}
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
items(demoChats) { chat ->
ExpressiveChatCard(
chat = chat,
antiDeleteEnabled = settings.antiDeleteMessages,
onClick = { openedChat = chat }
)
}
item { Spacer(Modifier.height(80.dp)) }
}
}
}
@Composable
private fun ExpressiveChatCard(
chat: ChatPreview,
antiDeleteEnabled: Boolean,
onClick: () -> Unit
) {
Surface(
shape = RoundedCornerShape(26.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier
.fillMaxWidth()
.clickable(onClick = onClick)
) {
Row(
modifier = Modifier.padding(14.dp),
verticalAlignment = Alignment.CenterVertically
) {
Box(
modifier = Modifier
.size(52.dp)
.clip(RoundedCornerShape(18.dp))
.background(MaterialTheme.colorScheme.primaryContainer),
contentAlignment = Alignment.Center
) {
Text(
text = chat.title.take(1),
style = MaterialTheme.typography.titleMedium,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.onPrimaryContainer
)
}
Spacer(Modifier.width(12.dp))
Column(modifier = Modifier.weight(1f)) {
Row(verticalAlignment = Alignment.CenterVertically) {
Text(
text = chat.title,
style = MaterialTheme.typography.titleMedium,
fontWeight = FontWeight.SemiBold,
maxLines = 1,
overflow = TextOverflow.Ellipsis,
modifier = Modifier.weight(1f)
)
Text(
text = chat.time,
style = MaterialTheme.typography.labelSmall,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
}
Spacer(Modifier.height(4.dp))
Text(
text = chat.lastMessage,
style = MaterialTheme.typography.bodyMedium,
color = if (chat.isDeletedSaved && antiDeleteEnabled)
MaterialTheme.colorScheme.error
else
MaterialTheme.colorScheme.onSurfaceVariant,
maxLines = 2,
overflow = TextOverflow.Ellipsis
)
if ((chat.isDeletedSaved && antiDeleteEnabled) || chat.translatedTag != null) {
Spacer(Modifier.height(6.dp))
Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
if (chat.isDeletedSaved && antiDeleteEnabled) {
Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
Text(
"Удалено • Сохранено",
modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
color = MaterialTheme.colorScheme.onErrorContainer
)
}
}
if (chat.translatedTag != null) {
Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
Text(
"Перевод ${chat.translatedTag}",
modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
color = MaterialTheme.colorScheme.onSecondaryContainer
)
}
}
}
}
}
AnimatedVisibility(
visible = chat.unreadCount > 0,
enter = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)),
exit = scaleOut()
) {
Spacer(Modifier.width(8.dp))
Surface(
shape = CircleShape,
color = MaterialTheme.colorScheme.primary
) {
Text(
text = chat.unreadCount.toString(),
modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
style = MaterialTheme.typography.labelMedium,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.onPrimary
)
}
}
}
}
}
