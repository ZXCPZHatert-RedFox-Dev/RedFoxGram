package com.redfox.gram

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redfox.gram.player.LrclibLyricsEngine
import com.redfox.gram.player.LyricLine
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class ChatPreview(
    val id: Long,
    val title: String,
    val lastMessage: String,
    val time: String,
    val unread: Int = 0,
    val shapeType: Int = 0,
    val folder: String = "Личные"
)

data class ChatMessage(
    val id: Long,
    val sender: String,
    val text: String,
    val time: String,
    val isMine: Boolean,
    val isAudio: Boolean = false,
    val audioArtist: String = "",
    val audioTitle: String = ""
)

data class RedFoxPlugin(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val enabled: Boolean
)

fun expressivePolygonShape(sides: Int, dent: Float = 0.14f): Shape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val rOuter = minOf(cx, cy)
    val rInner = rOuter * (1f - dent)
    val steps = sides * 2
    for (i in 0..steps) {
        val angle = (i * PI / sides).toFloat()
        val r = if (i % 2 == 0) rOuter else rInner
        val x = cx + r * cos(angle)
        val y = cy + r * sin(angle)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxGramApp(
    customColorScheme: ColorScheme? = null,
    isDesktop: Boolean = false
) {
    val redFoxDark = customColorScheme ?: darkColorScheme(
        primary = Color(0xFFFF8A50),
        onPrimary = Color(0xFF2B1100),
        primaryContainer = Color(0xFF4A2511),
        onPrimaryContainer = Color(0xFFFFDBCB),
        secondaryContainer = Color(0xFF2B313C),
        onSecondaryContainer = Color(0xFFD8E2FF),
        background = Color(0xFF101318),
        surface = Color(0xFF101318),
        surfaceContainer = Color(0xFF191D24),
        surfaceContainerHigh = Color(0xFF222730)
    )

    var isAuthorized by remember { mutableStateOf(true) }
    var authPhone by remember { mutableStateOf("+7 ") }
    var authCode by remember { mutableStateOf("") }
    var authStep by remember { mutableStateOf(0) }

    var selectedTab by remember { mutableStateOf(0) }
    var selectedFolder by remember { mutableStateOf(0) }
    var activeChat by remember { mutableStateOf<ChatPreview?>(null) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var showLyricsBar by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }

    // OpexGram Expressive Customizations
    var cardCorner by remember { mutableStateOf(20f) }
    var wavySpeed by remember { mutableStateOf(20f) }
    var avatarShapeMode by remember { mutableStateOf(1) }
    var deleteEffect by remember { mutableStateOf("Портал") }
    var hidePhoneNumber by remember { mutableStateOf(true) }
    var showSecondsInTime by remember { mutableStateOf(true) }
    var lockArchive by remember { mutableStateOf(true) }

    var currentArtist by remember { mutableStateOf("Привет") }
    var currentTitle by remember { mutableStateOf("Плохой герой") }
    var lyricsLines by remember {
        mutableStateOf(
            listOf(
                LyricLine(0, "На сиденье такси"),
                LyricLine(3000, "По трубам залезу"),
                LyricLine(6000, "И прыгну с балкона"),
                LyricLine(9000, "Я буду прекрасен и невыносим"),
                LyricLine(12000, "Ао-ао-о • Я плохой герой")
            )
        )
    }
    var currentLineIdx by remember { mutableStateOf(0) }

    LaunchedEffect(currentArtist, currentTitle) {
        val fetched = LrclibLyricsEngine.fetchLyricsForAnyTrack(currentArtist, currentTitle)
        if (fetched.size > 1) {
            lyricsLines = fetched
            currentLineIdx = 0
        }
    }

    LaunchedEffect(isPlaying, lyricsLines) {
        while (isPlaying && lyricsLines.isNotEmpty()) {
            delay(2600)
            currentLineIdx = (currentLineIdx + 1) % lyricsLines.size
        }
    }

    val chats = remember {
        mutableStateListOf(
            ChatPreview(1, "Архив чатов", "248 скрытых диалогов • Замок активен", "16:38", 248, 1, "Личные"),
            ChatPreview(2, "RedFox Dev Community", "Вышла сборка RedFoxGram Android + Windows + Linux!", "16:35", 3, 0, "Каналы"),
            ChatPreview(3, "Канал ядра RMX3624", "Яркость 4095/4095 и KernelSU протестированы на A17", "15:42", 0, 2, "Каналы"),
            ChatPreview(4, "Избранное", "Заметки, плагины .rfplugin и резервные копии", "14:15", 0, 0, "Личные"),
            ChatPreview(5, "Музыкальный кэш", "Привет — Плохой герой (Нажмите для запуска)", "Вчера", 0, 1, "Личные")
        )
    }

    val messagesMap = remember {
        mutableStateMapOf(
            2L to mutableStateListOf(
                ChatMessage(1, "RedFox Dev", "Добро пожаловать в RedFoxGram! Выберите любой трек или откройте настройки Expressive.", "16:30:12", false),
                ChatMessage(2, "Вы", "Проверяю капсульное поле ввода и живой верхний мини-бар текста песни!", "16:34:45", true),
                ChatMessage(3, "RedFox Dev", "Привет — Плохой герой (Нажмите, чтобы включить трек и подтянуть текст через LRCLIB)", "16:35:10", false, true, "Привет", "Плохой герой")
            ),
            5L to mutableStateListOf(
                ChatMessage(10, "Музыкальный кэш", "Привет — Плохой герой", "14:00:00", false, true, "Привет", "Плохой герой"),
                ChatMessage(11, "Музыкальный кэш", "Miyagi & Эндшпиль — Minor", "14:05:22", false, true, "Miyagi", "Minor")
            )
        )
    }

    val plugins = remember {
        mutableStateListOf(
            RedFoxPlugin("rf.lyrics.lrclib", "Auto-Lyrics LRCLIB (Все треки)", "1.3.0", "RedFox Dev", "Автоматически находит и прокручивает текст ЛЮБОЙ песни в верхнем баре и плеере", true),
            RedFoxPlugin("rf.kernel.telemetry", "RedFox Kernel Bridge", "1.0.4", "RedFox Dev", "Команда .sysinfo отправляет температуру CPU, яркость 4095 и BBR в чат", true),
            RedFoxPlugin("rf.anti.delete", "Message Ghost Guard", "1.1.0", "Community", "Сохраняет удалённые собеседником сообщения с отметкой в локальной базе", true)
        )
    }

    MaterialTheme(colorScheme = redFoxDark) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!isAuthorized) {
                // Telegram MTProto / TDLib Login Screen
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(64.dp).clip(expressivePolygonShape(12, 0.16f)).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                            }
                            Spacer(Modifier.height(16.dp))

Text("Вход в RedFoxGram", fontWeight = FontWeight.Bold, fontSize = 22.sp)
Text("MTProto • Material 3 Expressive", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
Spacer(Modifier.height(20.dp))
if (authStep == 0) {
OutlinedTextField(
value = authPhone,
onValueChange = { authPhone = it },
label = { Text("Номер телефона") },
shape = RoundedCornerShape(16.dp),
modifier = Modifier.fillMaxWidth()
)
Spacer(Modifier.height(16.dp))
Button(
onClick = { authStep = 1 },
shape = RoundedCornerShape(50),
modifier = Modifier.fillMaxWidth().height(50.dp)
) {
Text("Получить код Telegram", fontWeight = FontWeight.Bold)
}
} else {
OutlinedTextField(
value = authCode,
onValueChange = { authCode = it },
label = { Text("Код подтверждения из Telegram") },
shape = RoundedCornerShape(16.dp),
modifier = Modifier.fillMaxWidth()
)
Spacer(Modifier.height(16.dp))
Button(
onClick = { isAuthorized = true },
shape = RoundedCornerShape(50),
modifier = Modifier.fillMaxWidth().height(50.dp)
) {
Text("Войти в аккаунт", fontWeight = FontWeight.Bold)
}
}
}
}
}
} else {
Box(modifier = Modifier.fillMaxSize()) {
if (activeChat != null) {
// Full Chat Conversation Screen
val chat = activeChat!!
val chatMsgs = messagesMap.getOrPut(chat.id) {
mutableStateListOf(
ChatMessage(100, chat.title, chat.lastMessage, "16:38:00", false)
)
}
var draftText by remember { mutableStateOf("") }
Column(modifier = Modifier.fillMaxSize()) {
TopAppBar(
title = {
Column {
Text(chat.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
Text("был(а) только что • $deleteEffect", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
}
},
navigationIcon = {
IconButton(onClick = { activeChat = null }) {
Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
}
},
actions = {
IconButton(onClick = { showFullPlayer = true }) {
Icon(Icons.Rounded.MusicNote, contentDescription = "Плеер")
}
}
)
LazyColumn(
modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
verticalArrangement = Arrangement.spacedBy(8.dp),
contentPadding = PaddingValues(vertical = 12.dp)
) {
items(chatMsgs, key = { it.id }) { msg ->
Row(
modifier = Modifier.fillMaxWidth(),
horizontalArrangement = if (msg.isMine) Arrangement.End else Arrangement.Start
) {
Surface(
shape = RoundedCornerShape(
topStart = 20.dp,
topEnd = 20.dp,
bottomStart = if (msg.isMine) 20.dp else 6.dp,
bottomEnd = if (msg.isMine) 6.dp else 20.dp
),
color = if (msg.isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
modifier = Modifier.widthIn(max = 320.dp).clickable {
if (msg.isAudio) {
currentArtist = msg.audioArtist
currentTitle = msg.audioTitle
isPlaying = true
showLyricsBar = true
showFullPlayer = true
}
}
) {
Column(modifier = Modifier.padding(12.dp)) {
if (msg.isAudio) {
Row(verticalAlignment = Alignment.CenterVertically) {
Icon(Icons.Rounded.PlayCircleFilled, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
Spacer(Modifier.width(8.dp))
Text(msg.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
}
} else {
Text(msg.text, fontSize = 15.sp)
}
Spacer(Modifier.height(4.dp))
Row(
modifier = Modifier.align(Alignment.End),
verticalAlignment = Alignment.CenterVertically
) {
Text(
text = if (showSecondsInTime) msg.time else msg.time.take(5),
fontSize = 11.sp,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
Spacer(Modifier.width(6.dp))
Icon(
Icons.Rounded.DeleteOutline,
contentDescription = "Удалить ($deleteEffect)",
modifier = Modifier.size(14.dp).clickable { chatMsgs.remove(msg) },
tint = MaterialTheme.colorScheme.onSurfaceVariant
)
}
}
}
}
}
}
// Expressive Capsule Input Bar + Separate Send Button
Row(
modifier = Modifier.fillMaxWidth().padding(12.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
Surface(
modifier = Modifier.weight(1f),
shape = RoundedCornerShape(50),
color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
TextField(
value = draftText,
onValueChange = { draftText = it },
placeholder = { Text("Сообщение (или .sysinfo)...") },
colors = TextFieldDefaults.colors(
focusedContainerColor = Color.Transparent,
unfocusedContainerColor = Color.Transparent,
focusedIndicatorColor = Color.Transparent,
unfocusedIndicatorColor = Color.Transparent
),
modifier = Modifier.fillMaxWidth()
)
}
Surface(
modifier = Modifier.size(50.dp).clip(CircleShape).clickable {
if (draftText.isNotBlank()) {
val outText = if (draftText.trim() == ".sysinfo") {
"🦊 RedFox Kernel Bridge: RMX3624 | Яркость 4095/4095 | BBR Active"
} else draftText.trim()
chatMsgs.add(ChatMessage(System.currentTimeMillis(), "Вы", outText, "16:40:00", true))
draftText = ""
}
},
color = MaterialTheme.colorScheme.primary
) {
Box(contentAlignment = Alignment.Center) {
Icon(
if (draftText.isBlank()) Icons.Rounded.Mic else Icons.AutoMirrored.Rounded.Send,
contentDescription = "Отправить",
tint = MaterialTheme.colorScheme.onPrimary
)
}
}
}
}
} else {
Column(modifier = Modifier.fillMaxSize()) {
CenterAlignedTopAppBar(
title = {
Text("RedFoxGram", fontWeight = FontWeight.Bold, fontSize = 20.sp)
},
navigationIcon = {
IconButton(onClick = { isAuthorized = false }) {
Icon(Icons.Rounded.AccountCircle, contentDescription = "Аккаунт")
}
},
actions = {
IconButton(onClick = { showFullPlayer = true }) {
Icon(Icons.Rounded.MusicNote, contentDescription = "Плеер")
}
},
colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.92f)
)
)
if (selectedTab == 0) {
val folders = listOf("Все", "Личные", "Каналы", "Боты", "Плагины")
LazyRow(
modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
items(folders.indices.toList()) { idx ->
val active = selectedFolder == idx
FilterChip(
selected = active,
onClick = { selectedFolder = idx },
label = { Text(folders[idx], fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) },
shape = RoundedCornerShape(50)
)
}
}
AnimatedVisibility(visible = showLyricsBar && lyricsLines.isNotEmpty()) {
Surface(
modifier = Modifier
.fillMaxWidth()
.padding(horizontal = 12.dp, vertical = 6.dp)
.clickable { showFullPlayer = true },
shape = RoundedCornerShape(16.dp),
color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
Row(
modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
verticalAlignment = Alignment.CenterVertically
) {
Box(
modifier = Modifier
.size(28.dp)
.clip(RoundedCornerShape(8.dp))
.background(MaterialTheme.colorScheme.primaryContainer),
contentAlignment = Alignment.Center
) {
Icon(
Icons.Rounded.GraphicEq,
contentDescription = null,
tint = MaterialTheme.colorScheme.primary,
modifier = Modifier.size(18.dp)
)
}
Spacer(Modifier.width(10.dp))
val safeIdx = currentLineIdx.coerceIn(0, lyricsLines.lastIndex)
AnimatedContent(
targetState = lyricsLines[safeIdx].text,
transitionSpec = {
(slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
},
modifier = Modifier.weight(1f)
) { line ->
Text(
text = line,
maxLines = 1,
overflow = TextOverflow.Ellipsis,
fontWeight = FontWeight.SemiBold,
fontSize = 14.sp,
color = MaterialTheme.colorScheme.onSurface
)
}
IconButton(onClick = { showLyricsBar = false }, modifier = Modifier.size(24.dp)) {
Icon(Icons.Rounded.Close, contentDescription = "Закрыть", modifier = Modifier.size(16.dp))
}
}
}
}
val filteredChats = if (selectedFolder == 0) chats else chats.filter { it.folder == folders[selectedFolder] }
LazyColumn(
modifier = Modifier.fillMaxSize(),
contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
verticalArrangement = Arrangement.spacedBy(8.dp)
) {
items(filteredChats) { chat ->
Surface(
modifier = Modifier.fillMaxWidth().clickable { activeChat = chat },
shape = RoundedCornerShape(cardCorner.dp),
color = MaterialTheme.colorScheme.surfaceContainer
) {
Row(
modifier = Modifier.padding(12.dp),
verticalAlignment = Alignment.CenterVertically
) {
val avatarShape = when (avatarShapeMode) {
1 -> expressivePolygonShape(9, 0.12f)
2 -> expressivePolygonShape(12, 0.16f)
else -> RoundedCornerShape(18.dp)
}
Box(
modifier = Modifier
.size(50.dp)
.clip(avatarShape)
.background(MaterialTheme.colorScheme.primaryContainer),
contentAlignment = Alignment.Center
) {
Text(
text = chat.title.take(1),
fontWeight = FontWeight.Bold,
fontSize = 20.sp,
color = MaterialTheme.colorScheme.onPrimaryContainer
)
}
Spacer(Modifier.width(12.dp))
Column(modifier = Modifier.weight(1f)) {
Row(verticalAlignment = Alignment.CenterVertically) {
Text(
text = chat.title,
fontWeight = FontWeight.Bold,
fontSize = 16.sp,
modifier = Modifier.weight(1f),
maxLines = 1,
overflow = TextOverflow.Ellipsis
)
Text(
text = chat.time,
fontSize = 12.sp,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
}
Spacer(Modifier.height(4.dp))
Row(verticalAlignment = Alignment.CenterVertically) {
Text(
text = chat.lastMessage,
fontSize = 14.sp,
color = MaterialTheme.colorScheme.onSurfaceVariant,
maxLines = 1,
overflow = TextOverflow.Ellipsis,
modifier = Modifier.weight(1f)
)
if (chat.unread > 0) {
Surface(
shape = CircleShape,
color = MaterialTheme.colorScheme.secondaryContainer
) {
Text(
text = chat.unread.toString(),
modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
fontSize = 12.sp,
fontWeight = FontWeight.Bold
)
}
}
}
}
}
}
}
}
} else if (selectedTab == 1) {
LazyColumn(
modifier = Modifier.fillMaxSize(),
contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
verticalArrangement = Arrangement.spacedBy(10.dp)
) {
item {
Text("Система плагинов (.rfplugin)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
Text(
"Безопасный движок расширений для Android, Windows и Linux",
fontSize = 13.sp,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
}
items(plugins.indices.toList()) { idx ->
val plugin = plugins[idx]
Surface(
shape = RoundedCornerShape(20.dp),
color = MaterialTheme.colorScheme.surfaceContainer
) {
Row(
modifier = Modifier.padding(16.dp),
verticalAlignment = Alignment.CenterVertically
) {
Column(modifier = Modifier.weight(1f)) {
Text("${plugin.name} v${plugin.version}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
Text("Автор: ${plugin.author}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
Spacer(Modifier.height(4.dp))
Text(plugin.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
Switch(
checked = plugin.enabled,
onCheckedChange = { plugins[idx] = plugin.copy(enabled = it) }
)
}
}
}
}
} else {
LazyColumn(
modifier = Modifier.fillMaxSize(),
contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
verticalArrangement = Arrangement.spacedBy(12.dp)
) {
item {
Text("Настройки OpexGram & Material 3 Expressive", fontWeight = FontWeight.Bold, fontSize = 18.sp)
}
item {
Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
Text("Скругление карточек чатов: ${cardCorner.toInt()} dp", fontWeight = FontWeight.SemiBold)
Slider(value = cardCorner, onValueChange = { cardCorner = it }, valueRange = 8f..28f)
Text("Форма аватарок (MaterialShapes)", fontWeight = FontWeight.SemiBold)
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
listOf("Сквиркл", "Cookie 9", "Sunny 12").forEachIndexed { idx, label ->
FilterChip(selected = avatarShapeMode == idx, onClick = { avatarShapeMode = idx }, label = { Text(label) })
}
}
Text("Эффект удаления сообщений", fontWeight = FontWeight.SemiBold)
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
listOf("Пыль", "Осколки", "Динамит", "Портал").forEach { eff ->
FilterChip(selected = deleteEffect == eff, onClick = { deleteEffect = eff }, label = { Text(eff) })
}
}
Text("Скорость волны плеера: ${wavySpeed.toInt()} dp/с", fontWeight = FontWeight.SemiBold)
Slider(value = wavySpeed, onValueChange = { wavySpeed = it }, valueRange = 5f..40f)
Row(verticalAlignment = Alignment.CenterVertically) {
Text("Живой текст песни вверху (Все треки)", modifier = Modifier.weight(1f))
Switch(checked = showLyricsBar, onCheckedChange = { showLyricsBar = it })
}
Row(verticalAlignment = Alignment.CenterVertically) {
Text("Скрыть номер телефона", modifier = Modifier.weight(1f))
Switch(checked = hidePhoneNumber, onCheckedChange = { hidePhoneNumber = it })
}
Row(verticalAlignment = Alignment.CenterVertically) {
Text("Показывать секунды во времени", modifier = Modifier.weight(1f))
Switch(checked = showSecondsInTime, onCheckedChange = { showSecondsInTime = it })
}
Row(verticalAlignment = Alignment.CenterVertically) {
Text("Пароль-замок на Архив чатов", modifier = Modifier.weight(1f))
Switch(checked = lockArchive, onCheckedChange = { lockArchive = it })
}
}
}
}
}
}
}
// Floating Expressive Bottom Dock + Sunny FAB
Row(
modifier = Modifier
.align(Alignment.BottomCenter)
.padding(horizontal = 16.dp, vertical = 14.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
Surface(
shape = RoundedCornerShape(50),
color = MaterialTheme.colorScheme.surfaceContainerHigh,
shadowElevation = 6.dp
) {
Row(modifier = Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
val navItems = listOf(
Triple(0, "Чаты", Icons.Rounded.ChatBubble),
Triple(1, "Плагины", Icons.Rounded.Extension),
Triple(2, "Дизайн", Icons.Rounded.Palette)
)
navItems.forEach { (idx, label, icon) ->
val selected = selectedTab == idx
Surface(
modifier = Modifier.clickable { selectedTab = idx },
shape = RoundedCornerShape(50),
color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
) {
Row(
modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
verticalAlignment = Alignment.CenterVertically
) {
Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
if (selected) {
Spacer(Modifier.width(6.dp))
Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
}
}
}
}
}
}
Surface(
modifier = Modifier
.size(56.dp)
.clip(expressivePolygonShape(14, 0.22f))
.clickable { activeChat = chats.firstOrNull() },
color = MaterialTheme.colorScheme.primaryContainer
) {
Box(contentAlignment = Alignment.Center) {
Icon(Icons.Rounded.Add, contentDescription = "Новое сообщение", tint = MaterialTheme.colorScheme.onPrimaryContainer)
}
}
}
}
// Fullscreen Apple Music Style Lyrics Player
AnimatedVisibility(
visible = showFullPlayer,
enter = slideInVertically { it } + fadeIn(),
exit = slideOutVertically { it } + fadeOut()
) {
Box(
modifier = Modifier
.fillMaxSize()
.background(
Brush.verticalGradient(
listOf(Color(0xFF3E2417), Color(0xFF1A1513), Color(0xFF101318))
)
)
) {
Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
Row(verticalAlignment = Alignment.CenterVertically) {
IconButton(onClick = { showFullPlayer = false }) {
Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Свернуть", tint = Color.White)
}
Column(modifier = Modifier.weight(1f)) {
Text("Текст песни (Auto-LRCLIB)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
Text("$currentArtist — $currentTitle", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
}
}
LazyColumn(
modifier = Modifier.weight(1f).padding(vertical = 16.dp),
verticalArrangement = Arrangement.spacedBy(16.dp)
) {
items(lyricsLines.indices.toList()) { idx ->
val isCurrent = idx == currentLineIdx
Text(
text = lyricsLines[idx].text,
fontSize = if (isCurrent) 26.sp else 20.sp,
fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.35f),
modifier = Modifier.clickable { currentLineIdx = idx }
)
}
}
val infiniteTransition = rememberInfiniteTransition()
val phase by infiniteTransition.animateFloat(
initialValue = 0f,
targetValue = (2f * PI).toFloat(),
animationSpec = infiniteRepeatable(
animation = tween((20000 / wavySpeed.coerceAtLeast(1f)).toInt(), easing = LinearEasing)
)
)
val primaryColor = MaterialTheme.colorScheme.primary
Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
val progressX = size.width * 0.42f
val midY = size.height / 2f
val path = Path()
var x = 0f
while (x <= progressX) {
val y = midY + sin((x / 18f) + phase) * 5f
if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
x += 2f
}
drawPath(path, color = primaryColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
drawLine(Color.White.copy(alpha = 0.25f), Offset(progressX, midY), Offset(size.width, midY), strokeWidth = 4f)
drawLine(Color.White, Offset(progressX, midY - 10f), Offset(progressX, midY + 10f), strokeWidth = 4f)
}
Spacer(Modifier.height(12.dp))
Row(
modifier = Modifier.fillMaxWidth(),
horizontalArrangement = Arrangement.SpaceEvenly,
verticalAlignment = Alignment.CenterVertically
) {
IconButton(onClick = { if (lyricsLines.isNotEmpty()) currentLineIdx = (currentLineIdx - 1 + lyricsLines.size) % lyricsLines.size }) {
Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
}
Surface(
modifier = Modifier
.size(68.dp)
.clip(expressivePolygonShape(12, 0.14f))
.clickable { isPlaying = !isPlaying },
color = MaterialTheme.colorScheme.secondaryContainer
) {
Box(contentAlignment = Alignment.Center) {
Icon(
if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
contentDescription = null,
modifier = Modifier.size(34.dp)
)
}
}
IconButton(onClick = { if (lyricsLines.isNotEmpty()) currentLineIdx = (currentLineIdx + 1) % lyricsLines.size }) {
Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
}
}
}
}
}
}
}
}
}
}
