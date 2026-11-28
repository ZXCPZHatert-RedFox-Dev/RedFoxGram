package com.redfox.gram.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.redfox.gram.bridge.RedFoxBridge
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.config.RedFoxSettingsState
import com.redfox.gram.engine.RedFoxTdEngine
import com.redfox.gram.network.RedFoxProxyManager
import com.redfox.gram.ui.chat.RedFoxChatScreen
import com.redfox.gram.ui.components.ExpressiveAvatarStyle
import com.redfox.gram.ui.components.RedFoxLocalImage
import com.redfox.gram.ui.components.buildExpressiveAvatarShape
import com.redfox.gram.ui.player.LiveLrcMiniBar
import com.redfox.gram.ui.player.RedFoxFullPlayerScreen
import com.redfox.gram.ui.proxy.RedFoxProxyScreen
import com.redfox.gram.ui.security.RedFoxPinLockScreen
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RedFoxMainScreen() {
    val settings: RedFoxSettingsState by RedFoxConfig.state.collectAsState()
    val realChats by RedFoxBridge.liveChats.collectAsState()
    val channelPosts by RedFoxTdEngine.channelFeedPosts.collectAsState()
    val syncedFolders by RedFoxTdEngine.telegramFolders.collectAsState()
    val activeFolderChats by RedFoxTdEngine.folderChats.collectAsState()
    val chatAvatars by RedFoxTdEngine.chatAvatars.collectAsState()
    val searchResults by RedFoxTdEngine.searchResults.collectAsState()
    val proxyState by RedFoxProxyManager.state.collectAsState()
    val networkBanner by RedFoxTdEngine.networkBanner.collectAsState()
    val isNetworkReady by RedFoxTdEngine.isNetworkReady.collectAsState()


var isUnlocked by remember { mutableStateOf(false) }
var showHiddenVault by remember { mutableStateOf(false) }
var unlockHiddenVaultMode by remember { mutableStateOf(false) }
var isSearchMode by remember { mutableStateOf(false) }
var searchQuery by remember { mutableStateOf("") }
var chatActionMenuTarget by remember { mutableStateOf<ChatPreview?>(null) }
var showTweaks by remember { mutableStateOf(false) }
var showFullPlayer by remember { mutableStateOf(false) }
var showProxyHub by remember { mutableStateOf(false) }
var openedChat by remember { mutableStateOf<ChatPreview?>(null) }
var isPlaying by remember { mutableStateOf(true) }
var avatarStyle by remember { mutableStateOf(ExpressiveAvatarStyle.FLOWER_12) }
var selectedTabId by remember { mutableStateOf(-1) }
LaunchedEffect(Unit) {
RedFoxTdEngine.refreshChatsNow()
}
if ((settings.pinLockEnabled && !isUnlocked) || unlockHiddenVaultMode) {
RedFoxPinLockScreen(
expectedPin = settings.pinCode,
title = if (unlockHiddenVaultMode) "Скрытые чаты (Fortress)" else "RedFox Fortress",
onUnlocked = {
if (unlockHiddenVaultMode) {
unlockHiddenVaultMode = false
showHiddenVault = true
} else {
isUnlocked = true
}
}
)
return
}
if (showProxyHub) {
RedFoxProxyScreen(onBack = { showProxyHub = false })
return
}
if (showTweaks) {
RedFoxTweaksScreen(onBack = { showTweaks = false })
return
}
if (showFullPlayer) {
    val activeTrack by com.redfox.gram.media.RedFoxAudioController.track.collectAsState()
RedFoxFullPlayerScreen(
trackTitle = activeTrack.title,
artist = activeTrack.artist,
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
// Диалог скрытия/возврата чата по долгому нажатию на карточку
chatActionMenuTarget?.let { target ->
val isHidden = settings.hiddenChatIds.contains(target.id)
AlertDialog(
onDismissRequest = { chatActionMenuTarget = null },
title = { Text(target.title, fontWeight = FontWeight.Bold) },
text = {
Text(
if (isHidden) "Вернуть этот чат обратно в общий список?"
else "Спрятать этот чат в секретный раздел? Открыть его можно долгим нажатием на заголовок RedFoxGram."
)
},
confirmButton = {
Button(onClick = {
RedFoxConfig.update { cur ->
val next = if (isHidden) cur.hiddenChatIds - target.id else cur.hiddenChatIds + target.id
cur.copy(hiddenChatIds = next)
}
chatActionMenuTarget = null
}) {
Text(if (isHidden) "Вернуть в список" else "Скрыть чат")
}
},
dismissButton = {
TextButton(onClick = { chatActionMenuTarget = null }) {
Text("Отмена")
}
}
)
}
val fallbackDemoChats = remember {
listOf(
ChatPreview(
id = 1,
title = "Загрузка реальных чатов TDLib...",
lastMessage = "Нажмите кнопку ↻ или подождите пару секунд",
time = "Сейчас",
unreadCount = 1
)
)
}
val baseChats = if (realChats.isNotEmpty()) realChats else fallbackDemoChats
val displayedChats = when {
isSearchMode -> searchResults
showHiddenVault -> baseChats.filter { settings.hiddenChatIds.contains(it.id) }
selectedTabId == -1 -> baseChats.filterNot { settings.hiddenChatIds.contains(it.id) }
selectedTabId > 0 -> activeFolderChats.filterNot { settings.hiddenChatIds.contains(it.id) }
else -> emptyList()
}
Scaffold(
topBar = {
Column {
CenterAlignedTopAppBar(
navigationIcon = {
TextButton(onClick = { showProxyHub = true }) {
Text(
text = if (proxyState.enabled) "Shield ON" else "Shield",
fontWeight = FontWeight.Bold,
color = if (proxyState.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
)
}
},
title = {
Row(
verticalAlignment = Alignment.CenterVertically,
modifier = Modifier.combinedClickable(
onClick = {
if (showHiddenVault) showHiddenVault = false
},
onLongClick = {
if (showHiddenVault) {
showHiddenVault = false
} else if (settings.pinLockEnabled) {
unlockHiddenVaultMode = true
} else {
showHiddenVault = true
}
}
)
) {
if (!isNetworkReady) {
CircularProgressIndicator(
modifier = Modifier.size(16.dp),
strokeWidth = 2.dp,
color = MaterialTheme.colorScheme.primary
)
Spacer(Modifier.width(8.dp))
Text(
text = networkBanner,
style = MaterialTheme.typography.titleMedium,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.primary
)
} else {
Text(
text = if (showHiddenVault) "Скрытые чаты" else settings.customHeaderTitle.ifBlank { "RedFoxGram" },
style = MaterialTheme.typography.titleLarge,
fontWeight = FontWeight.ExtraBold
)
Spacer(Modifier.width(4.dp))
Text(
text = "✓",
style = MaterialTheme.typography.titleMedium,
fontWeight = FontWeight.ExtraBold,
color = MaterialTheme.colorScheme.primary
)
Spacer(Modifier.width(6.dp))
Surface(
shape = CircleShape,
color = MaterialTheme.colorScheme.secondaryContainer,
modifier = Modifier.clickable {
val allStyles = ExpressiveAvatarStyle.entries
avatarStyle = allStyles[(avatarStyle.ordinal + 1) % allStyles.size]
}
) {
Text(
text = avatarStyle.label.ifBlank { avatarStyle.name },
                                        maxLines = 1,
                                        softWrap = false,
modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
style = MaterialTheme.typography.labelSmall,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.onSecondaryContainer
)
}
}
}
},
actions = {
TextButton(onClick = {
isSearchMode = !isSearchMode
if (!isSearchMode) {
searchQuery = ""
RedFoxTdEngine.searchTelegramChats("")
}
}) {
Icon(imageVector = if (isSearchMode) Icons.Rounded.Close else Icons.Rounded.Search, contentDescription = "Поиск", tint = MaterialTheme.colorScheme.primary)
}
TextButton(onClick = { RedFoxTdEngine.refreshChatsNow() }) {
Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Обновить", tint = MaterialTheme.colorScheme.primary)
}
}
)
// Строка глобального поиска по чатам и @username
if (isSearchMode) {
OutlinedTextField(
value = searchQuery,
onValueChange = { q ->
searchQuery = q
RedFoxTdEngine.searchTelegramChats(q)
},
placeholder = { Text("Поиск чатов, каналов или @username...") },
singleLine = true,
shape = CircleShape,
modifier = Modifier
.fillMaxWidth()
.padding(horizontal = 14.dp, vertical = 6.dp)
)
} else if (!showHiddenVault) {
Row(
modifier = Modifier
.fillMaxWidth()
.horizontalScroll(rememberScrollState())
.padding(horizontal = 14.dp, vertical = 6.dp),
horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
FilterChip(
selected = selectedTabId == -1,
onClick = { selectedTabId = -1 },
shape = CircleShape,
label = { Text("Все", fontWeight = if (selectedTabId == -1) FontWeight.Bold else FontWeight.Normal) }
)
FilterChip(
selected = selectedTabId == -2,
onClick = { selectedTabId = -2 },
shape = CircleShape,
label = {
Text(
text = if (channelPosts.isNotEmpty()) "Лента каналов (${channelPosts.size})" else "Лента каналов",
fontWeight = if (selectedTabId == -2) FontWeight.Bold else FontWeight.Normal
)
}
)
syncedFolders.forEach { folder ->
val selected = selectedTabId == folder.id
FilterChip(
selected = selected,
onClick = {
selectedTabId = folder.id
RedFoxTdEngine.selectTelegramFolder(folder.id)
},
shape = CircleShape,
label = {
Text(
text = folder.title,
fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
)
}
)
}
}
}
            val activeTrack by com.redfox.gram.media.RedFoxAudioController.track.collectAsState()
if (settings.liveLrcLyricsBar) {
LiveLrcMiniBar(
trackTitle = activeTrack.title,
artist = activeTrack.artist,
currentLine = "♪ Waiting in a car, waiting for a ride in the dark...",
isPlaying = isPlaying,
onTogglePlay = { isPlaying = !isPlaying },
onOpenFullPlayer = { showFullPlayer = true }
)
}
}
},
bottomBar = { val starShape = remember { buildExpressiveAvatarShape(ExpressiveAvatarStyle.FLOWER_12) }; Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 6.dp) { Row(modifier = Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.clickable { showTweaks = false }) { Text("Чаты", modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }; IconButton(onClick = { showTweaks = true }, modifier = Modifier.size(44.dp)) { Text("⚙", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) } } }; Spacer(Modifier.width(12.dp)); Surface(onClick = { isSearchMode = true }, shape = starShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 6.dp, modifier = Modifier.size(58.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) } } } }
    ) { padding ->
LazyColumn(
modifier = Modifier
.fillMaxSize()
.padding(padding)
.padding(horizontal = 14.dp),
verticalArrangement = Arrangement.spacedBy(10.dp)
) {
if (!isSearchMode && !showHiddenVault && selectedTabId == -2) {
if (channelPosts.isEmpty()) {
item {
Surface(
shape = RoundedCornerShape(24.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
) {
Text(
text = "Лента каналов пуста — подпишитесь на каналы или нажмите ↻",
modifier = Modifier.padding(20.dp),
style = MaterialTheme.typography.bodyMedium
)
}
}
} else {
items(channelPosts, key = { "${it.senderName}_${it.id}" }) { post ->
Surface(
shape = RoundedCornerShape(26.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth()
) {
Column(modifier = Modifier.padding(16.dp)) {
Text(
text = post.senderName,
style = MaterialTheme.typography.titleSmall,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.primary
)
Spacer(Modifier.height(6.dp))
Text(
text = post.text,
style = MaterialTheme.typography.bodyMedium
)
}
}
}
}
} else if (displayedChats.isEmpty()) {
item {
Surface(
shape = RoundedCornerShape(24.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
) {
Text(
text = when {
isSearchMode && searchQuery.isBlank() -> "Введите имя контакта, канала или @username"
isSearchMode -> "Ничего не найдено по запросу «$searchQuery»"
showHiddenVault -> "Скрытых чатов пока нет. Удерживайте любой чат в общем списке, чтобы спрятать его сюда."
else -> "Загрузка чатов..."
},
modifier = Modifier.padding(20.dp),
style = MaterialTheme.typography.bodyMedium
)
}
}
} else {
items(displayedChats, key = { it.id }) { chat ->
ExpressiveChatCard(
chat = chat,
avatarPath = chatAvatars.get(chat.id),
avatarStyle = avatarStyle,
antiDeleteEnabled = settings.antiDeleteMessages,
onClick = { openedChat = chat },
onLongClick = { chatActionMenuTarget = chat }
)
}
}
item { Spacer(Modifier.height(88.dp)) }
}
}
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpressiveChatCard(
chat: ChatPreview,
avatarPath: String? = null,
avatarStyle: ExpressiveAvatarStyle,
antiDeleteEnabled: Boolean,
onClick: () -> Unit,
onLongClick: () -> Unit = {}
) {
val avatarShape = remember(avatarStyle) { buildExpressiveAvatarShape(avatarStyle) }
LaunchedEffect(chat.id) { RedFoxTdEngine.ensureChatAvatar(chat.id) }
Surface(
shape = RoundedCornerShape(26.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier
.fillMaxWidth()
.combinedClickable(
onClick = onClick,
onLongClick = onLongClick
)
) {
Row(
modifier = Modifier.padding(14.dp),
verticalAlignment = Alignment.CenterVertically
) {
Box(
modifier = Modifier
.size(54.dp)
.clip(avatarShape)
.background(MaterialTheme.colorScheme.primaryContainer),
contentAlignment = Alignment.Center
) {
Text(
text = chat.title.take(1).uppercase(),
style = MaterialTheme.typography.titleMedium,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.onPrimaryContainer
)
if (!avatarPath.isNullOrBlank()) {
RedFoxLocalImage(
filePath = avatarPath,
contentDescription = chat.title,
modifier = Modifier.fillMaxSize()
)
}
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
color = if (chat.isDeletedSaved && antiDeleteEnabled) {
MaterialTheme.colorScheme.error
} else {
MaterialTheme.colorScheme.onSurfaceVariant
},
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
