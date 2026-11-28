package com.redfox.gram.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.ui.components.ExpressiveM3Slider
import com.redfox.gram.ui.components.ExpressiveSwitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxChatsTweaksScreen(onBack: () -> Unit) {
    val settings by RedFoxConfig.state.collectAsState()
    var subPage by remember { mutableStateOf("MAIN") } // MAIN, TEXT_ANIM, MSG_MENU, SWIPE_ACTIONS

    var sampleTyping by remember { mutableStateOf("Печатаю красиво") }
    var animBlur by remember { mutableStateOf(true) }
    var animSlideTop by remember { mutableStateOf(true) }
    var animGrow by remember { mutableStateOf(true) }
    var animRotate by remember { mutableStateOf(true) }
    var animScatter by remember { mutableStateOf(true) }
    var smoothCursor by remember { mutableStateOf(true) }
    var cursorShape by remember { mutableStateOf("Блок") }
    var softBlink by remember { mutableStateOf(true) }
    var cursorTrail by remember { mutableStateOf(true) }

    var folderCompression by remember { mutableStateOf(12f) }
    var folderSpring by remember { mutableStateOf("Expressive") }
    var swipeHideKeyboard by remember { mutableStateOf(true) }
    var rememberLastFolder by remember { mutableStateOf(true) }
    var pageSwipeAnim by remember { mutableStateOf(true) }
    var queueMessages by remember { mutableStateOf(true) }
    var springAnims by remember { mutableStateOf(true) }
    var bgContrast by remember { mutableStateOf(100f) }

    var enabledMenuItems by remember {
        mutableStateOf(setOf("Ответить", "Переслать", "Копировать", "Закрепить", "Перевести", "В закладки", "Реакции", "Инспектор", "Удалить"))
    }
    var enabledSwipeActions by remember {
        mutableStateOf(setOf("Ответить свайпом", "Быстрая реакция", "Переслать", "В закладки", "Копировать", "Закрепить", "Перевести", "Удалить"))
    }

    if (subPage == "TEXT_ANIM") {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { subPage = "MAIN" }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    title = { Text("Анимация текста", fontWeight = FontWeight.Bold) }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("😊", style = MaterialTheme.typography.titleLarge)
                                    Spacer(Modifier.width(8.dp))
                                    TextField(
                                        value = sampleTyping,
                                        onValueChange = { sampleTyping = it },
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clickable { RedFoxConfig.update { it.copy(animatedTypingCursor = !it.animatedTypingCursor) } }) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Анимировать набор", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            ExpressiveSwitch(checked = settings.animatedTypingCursor, onCheckedChange = { v -> RedFoxConfig.update { it.copy(animatedTypingCursor = v) } })
                        }
                    }
                }
                item {
                    Text("Появление", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp))
                }
                listOf(
                    Triple("Из размытия", animBlur) { animBlur = !animBlur },
                    Triple("Въезд сверху", animSlideTop) { animSlideTop = !animSlideTop },
                    Triple("Рост из точки", animGrow) { animGrow = !animGrow },
                    Triple("Доворот", animRotate) { animRotate = !animRotate }
                ).forEach { (label, stateVal, toggle) ->
                    item {
                        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clickable { toggle() }) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                ExpressiveSwitch(checked = stateVal, onCheckedChange = { toggle() })
                            }
                        }
                    }
                }
                item {
                    Text("Удаление и Курсор", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp))
                }
                item {
                    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Форма курсора", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Блок", "Линия", "Подчёркивание").forEach { shapeName ->
                                    val selected = cursorShape == shapeName
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            cursorShape = shapeName
                                            RedFoxConfig.update { it.copy(cursorShapeStyle = shapeName) }
                                        },
                                        shape = CircleShape,
                                        label = { Text(shapeName) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                listOf(
                    Triple("Рассыпание символов", animScatter) { animScatter = !animScatter },
                    Triple("Плавный курсор", smoothCursor) { smoothCursor = !smoothCursor },
                    Triple("Мягкое мигание", softBlink) { softBlink = !softBlink },
                    Triple("Шлейф курсора", cursorTrail) { cursorTrail = !cursorTrail }
                ).forEach { (label, stateVal, toggle) ->
                    item {

Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clickable { toggle() }) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
ExpressiveSwitch(checked = stateVal, onCheckedChange = { toggle() })
}
}
}
}
item { Spacer(Modifier.height(24.dp)) }
}
}
return
}
if (subPage == "MSG_MENU" || subPage == "SWIPE_ACTIONS") {
val isMenu = subPage == "MSG_MENU"
val allOptions = if (isMenu) {
listOf("Ответить", "Переслать", "Копировать", "Закрепить", "Перевести", "В закладки", "Реакции", "Инспектор", "Удалить", "Сохранить в галерею", "Цитировать", "Изменить")
} else {
listOf("Ответить свайпом", "Быстрая реакция", "Переслать", "В закладки", "Копировать", "Закрепить", "Перевести", "Удалить")
}
val activeSet = if (isMenu) enabledMenuItems else enabledSwipeActions
Scaffold(
topBar = {
TopAppBar(
navigationIcon = {
IconButton(onClick = { subPage = "MAIN" }) {
Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
}
},
title = { Text(if (isMenu) "Меню сообщения" else "Действия свайпом", fontWeight = FontWeight.Bold) }
)
}
) { padding ->
LazyColumn(
modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
verticalArrangement = Arrangement.spacedBy(10.dp)
) {
items(allOptions.size) { idx ->
val itemTitle = allOptions.get(idx)
val checked = activeSet.contains(itemTitle)
Surface(
shape = RoundedCornerShape(20.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable {
val next = if (checked) activeSet - itemTitle else activeSet + itemTitle
if (isMenu) enabledMenuItems = next else enabledSwipeActions = next
}
) {
Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
Text(itemTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
ExpressiveSwitch(
checked = checked,
onCheckedChange = { on ->
val next = if (on) activeSet + itemTitle else activeSet - itemTitle
if (isMenu) enabledMenuItems = next else enabledSwipeActions = next
}
)
}
}
}
item { Spacer(Modifier.height(24.dp)) }
}
}
return
}
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
// Наглядные карточки-макеты папок (1 в 1 как на 2-м скриншоте)
item {
Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
listOf("TELEGRAM" to "Telegram", "TABS" to "Вкладки", "BUTTONS" to "Кнопки").forEach { (code, label) ->
val selected = settings.folderTabStyle == code
Column(
horizontalAlignment = Alignment.CenterHorizontally,
modifier = Modifier.weight(1f).clickable {
RedFoxConfig.update { it.copy(folderTabStyle = code) }
}
) {
Surface(
shape = RoundedCornerShape(20.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier
.fillMaxWidth()
.height(84.dp)
.then(
if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
else Modifier
)
) {
Box(modifier = Modifier.padding(10.dp)) {
Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
Box(Modifier.width(22.dp).height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
Box(Modifier.width(16.dp).height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest))
Box(Modifier.width(16.dp).height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest))
}
repeat(2) {
Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest))
Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest))
}
}
}
if (selected) {
Surface(
shape = CircleShape,
color = MaterialTheme.colorScheme.primary,
modifier = Modifier.size(20.dp).align(Alignment.TopEnd)
) {
Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(3.dp))
}
}
}
}
Spacer(Modifier.height(6.dp))
Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
}
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clickable { RedFoxConfig.update { it.copy(folderIconOnly = !it.folderIconOnly) } }) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Вид вкладок: только иконка", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
ExpressiveSwitch(checked = settings.folderIconOnly, onCheckedChange = { v -> RedFoxConfig.update { it.copy(folderIconOnly = v) } })
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clickable { RedFoxConfig.update { it.copy(hideAllChatsFolder = !it.hideAllChatsFolder) } }) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Скрыть папку «Все чаты»", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
ExpressiveSwitch(checked = settings.hideAllChatsFolder, onCheckedChange = { v -> RedFoxConfig.update { it.copy(hideAllChatsFolder = v) } })
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clickable { rememberLastFolder = !rememberLastFolder }) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Запоминать последнюю папку", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
ExpressiveSwitch(checked = rememberLastFolder, onCheckedChange = { rememberLastFolder = it })
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clickable { pageSwipeAnim = !pageSwipeAnim }) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Column(Modifier.weight(1f)) {
Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
Text("Анимация листания", style = MaterialTheme.typography.titleMedium)
Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("NEW", modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onPrimary) }
}
Text("Страницы сжимаются при листании", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
ExpressiveSwitch(checked = pageSwipeAnim, onCheckedChange = { pageSwipeAnim = it })
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Column(modifier = Modifier.padding(16.dp)) {
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
Text("Сжатие", fontWeight = FontWeight.SemiBold)
Text("${folderCompression.toInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
Spacer(Modifier.height(8.dp))
ExpressiveM3Slider(value = folderCompression, onValueChange = { folderCompression = it }, valueRange = 0f..25f, isExpressiveStyle = settings.expressiveSliders)
}
}
}
item {
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
listOf("Выключено", "Стандарт", "Expressive").forEach { sp ->
val selected = folderSpring == sp
Surface(
shape = RoundedCornerShape(18.dp),
color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.weight(1f).clickable { folderSpring = sp }
) {
Text(sp, modifier = Modifier.padding(vertical = 14.dp), textAlign = TextAlign.Center, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
}
}
}
}
// Меню сообщения, Анимация текста и Эффект удаления (3-й скриншот)
item {
Spacer(Modifier.height(4.dp))
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clickable { subPage = "MSG_MENU" }) {
Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Меню сообщения", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
Text("${enabledMenuItems.size} из 12", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clickable { subPage = "TEXT_ANIM" }) {
Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
Text("Анимация текста", style = MaterialTheme.typography.titleMedium)
Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("NEW", modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onPrimary) }
}
Text(if (settings.animatedTypingCursor) "Включено" else "Выкл", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
}
}
item {
Text("Эффект удаления сообщений", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
}
item {
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
listOf("DUST" to "Пыль", "SHARDS" to "Осколки", "DYNAMITE" to "Динамит", "PORTAL" to "Портал").forEach { (code, label) ->
val selected = settings.deleteEffectStyle == code
Surface(
shape = RoundedCornerShape(18.dp),
color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.weight(1f).clickable { RedFoxConfig.update { it.copy(deleteEffectStyle = code) } }
) {
Text(label, modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
}
}
}
}
// Размер стикеров и GIF
item {
Text("Размер стикеров и GIF", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Column(modifier = Modifier.padding(16.dp)) {
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
Text("Стикеры", fontWeight = FontWeight.SemiBold)
Text("${settings.stickerSizePercent.toInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
Spacer(Modifier.height(8.dp))
ExpressiveM3Slider(value = settings.stickerSizePercent, onValueChange = { v -> RedFoxConfig.update { it.copy(stickerSizePercent = v) } }, valueRange = 25f..100f, isExpressiveStyle = settings.expressiveSliders)
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Column(modifier = Modifier.padding(16.dp)) {
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
Text("GIF", fontWeight = FontWeight.SemiBold)
Text("${settings.gifSizePercent.toInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
Spacer(Modifier.height(8.dp))
ExpressiveM3Slider(value = settings.gifSizePercent, onValueChange = { v -> RedFoxConfig.update { it.copy(gifSizePercent = v) } }, valueRange = 25f..100f, isExpressiveStyle = settings.expressiveSliders)
}
}
}
// Шапка и поле ввода (8-й скриншот)
item {
Text("Шапка и поле ввода", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
}
item {
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
listOf("TELEGRAM" to "Telegram", "EXPRESSIVE" to "Expressive", "BUTTONS" to "Кнопки").forEach { (code, label) ->
val selected = settings.inputFieldStyle == code
Surface(
shape = RoundedCornerShape(18.dp),
color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.weight(1f).clickable { RedFoxConfig.update { it.copy(inputFieldStyle = code) } }
) {
Text(label, modifier = Modifier.padding(vertical = 14.dp), textAlign = TextAlign.Center, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
}
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clickable { queueMessages = !queueMessages }) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Column(Modifier.weight(1f)) {
Text("Очередь сообщений", style = MaterialTheme.typography.titleMedium)
Text("Ждут таймер медленного режима", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
ExpressiveSwitch(checked = queueMessages, onCheckedChange = { queueMessages = it })
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clickable { springAnims = !springAnims }) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Пружинные анимации", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
ExpressiveSwitch(checked = springAnims, onCheckedChange = { springAnims = it })
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Column(modifier = Modifier.padding(16.dp)) {
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
Text("Контраст фона", fontWeight = FontWeight.SemiBold)
Text("${bgContrast.toInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
Spacer(Modifier.height(8.dp))
ExpressiveM3Slider(value = bgContrast, onValueChange = { bgContrast = it }, valueRange = 20f..100f, isExpressiveStyle = settings.expressiveSliders)
}
}
}
// Жесты и Сведения в сообщениях (9-й и 10-й скриншоты)
item {
Text("Жесты и Сведения в сообщениях", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clickable { swipeHideKeyboard = !swipeHideKeyboard }) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Свайп прячет клавиатуру", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
ExpressiveSwitch(checked = swipeHideKeyboard, onCheckedChange = { swipeHideKeyboard = it })
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clickable { subPage = "SWIPE_ACTIONS" }) {
Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Действия свайпом", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
Text("${enabledSwipeActions.size} из 8", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
}
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
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clickable { RedFoxConfig.update { it.copy(showScrollToTopBtn = !it.showScrollToTopBtn) } }) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Кнопка листания вверх", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
ExpressiveSwitch(checked = settings.showScrollToTopBtn, onCheckedChange = { v -> RedFoxConfig.update { it.copy(showScrollToTopBtn = v) } })
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clickable { RedFoxConfig.update { it.copy(showSecondsInTime = !it.showSecondsInTime) } }) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Секунды во времени", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
ExpressiveSwitch(checked = settings.showSecondsInTime, onCheckedChange = { v -> RedFoxConfig.update { it.copy(showSecondsInTime = v) } })
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("День недели в датах", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
ExpressiveSwitch(checked = true, onCheckedChange = {})
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Время оригинала у пересылок", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
ExpressiveSwitch(checked = true, onCheckedChange = {})
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Счётчик пересылок у постов", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
ExpressiveSwitch(checked = true, onCheckedChange = {})
}
}
}
item {
Text("Живые эффекты на фоне чата", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
}
item {
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
listOf("NONE" to "Выкл", "SAKURA" to "Сакура 🌸", "SNOW" to "Снег ❄", "STARS" to "Звёзды ✨").forEach { (code, label) ->
val selected = settings.chatParticleEffect == code
Surface(
shape = RoundedCornerShape(18.dp),
color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.weight(1f).clickable { RedFoxConfig.update { it.copy(chatParticleEffect = code) } }
) {
Text(label, modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
}
}
}
}
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().clickable {
                        RedFoxConfig.update { it.copy(compactChatList = !it.compactChatList) }
                    }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Компактный список чатов", style = MaterialTheme.typography.titleMedium)
                            Text("Уменьшенные отступы и аватарки как в exteraGram", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ExpressiveSwitch(
                            checked = settings.compactChatList,
                            onCheckedChange = { v -> RedFoxConfig.update { it.copy(compactChatList = v) } }
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(28.dp)) }
}
}
}
