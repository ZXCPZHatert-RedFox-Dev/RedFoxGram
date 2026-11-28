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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.engine.RedFoxTdEngine
import com.redfox.gram.ui.components.ExpressiveAvatarStyle
import com.redfox.gram.ui.components.ExpressiveM3Slider
import com.redfox.gram.ui.components.buildExpressiveAvatarShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxAppearanceScreen(onBack: () -> Unit) {
    val settings by RedFoxConfig.state.collectAsState()
    val tgThemes by RedFoxTdEngine.syncedTelegramThemes.collectAsState()
    val tgAccents by RedFoxTdEngine.syncedCloudAccents.collectAsState()

    var subPage by remember { mutableStateOf("MAIN") }
    if (subPage in listOf("NAV_BAR", "HEADER", "SETTINGS_SCREEN")) {
        RedFoxScreensDesigner(initialSection = subPage, onBack = { subPage = "MAIN" })
        return
    } // MAIN, SLIDERS, FONTS

    if (subPage == "SLIDERS") {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { subPage = "MAIN" }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    title = { Text("Слайдеры", fontWeight = FontWeight.Bold) }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Стиль", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(false to "Классический", true to "Expressive").forEach { (isExp, label) ->
                            val selected = settings.expressiveSliders == isExp
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f).clickable {
                                    RedFoxConfig.update { it.copy(expressiveSliders = isExp) }
                                }
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .then(
                                            if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(22.dp))
                                            else Modifier
                                        )
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                                        ExpressiveM3Slider(
                                            value = 0.55f,
                                            onValueChange = {},
                                            isExpressiveStyle = isExp,
                                            trackHeightDp = 16f,
                                            thumbHeightDp = 34f
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))

Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
}
}
}
}
item {
Text("Размеры", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Column(modifier = Modifier.padding(16.dp)) {
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
Text("Высота дорожки", fontWeight = FontWeight.SemiBold)
Text("${settings.sliderTrackHeightDp.toInt()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
Spacer(Modifier.height(8.dp))
ExpressiveM3Slider(
value = settings.sliderTrackHeightDp,
onValueChange = { v -> RedFoxConfig.update { it.copy(sliderTrackHeightDp = v) } },
valueRange = 8f..28f,
isExpressiveStyle = settings.expressiveSliders
)
}
}
}
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Column(modifier = Modifier.padding(16.dp)) {
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
Text("Высота ручки", fontWeight = FontWeight.SemiBold)
Text("${settings.sliderThumbHeightDp.toInt()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
Spacer(Modifier.height(8.dp))
ExpressiveM3Slider(
value = settings.sliderThumbHeightDp,
onValueChange = { v -> RedFoxConfig.update { it.copy(sliderThumbHeightDp = v) } },
valueRange = 24f..54f,
isExpressiveStyle = settings.expressiveSliders
)
}
}
}
}
}
return
}
if (subPage == "FONTS") {
Scaffold(
topBar = {
TopAppBar(
navigationIcon = {
IconButton(onClick = { subPage = "MAIN" }) {
Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
}
},
title = { Text("Шрифт", fontWeight = FontWeight.Bold) }
)
}
) { padding ->
LazyColumn(
modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
verticalArrangement = Arrangement.spacedBy(10.dp)
) {
// Живое превью диалога (как на скриншоте 4 из Opexgram)
item {
Surface(
shape = RoundedCornerShape(26.dp),
color = MaterialTheme.colorScheme.surfaceContainerHigh,
modifier = Modifier.fillMaxWidth()
) {
Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
Surface(
shape = RoundedCornerShape(
topStart = settings.bubbleCornerDp.dp,
topEnd = settings.bubbleCornerDp.dp,
bottomEnd = settings.bubbleCornerDp.dp,
bottomStart = if (settings.messageTails) 6.dp else settings.bubbleCornerDp.dp
),
color = MaterialTheme.colorScheme.surfaceContainerLow
) {
Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
Text("Боб Харрис", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
Text("Знаешь, который час?  20:40", style = MaterialTheme.typography.bodyLarge)
}
}
Surface(
shape = RoundedCornerShape(
topStart = settings.bubbleCornerDp.dp,
topEnd = settings.bubbleCornerDp.dp,
bottomStart = settings.bubbleCornerDp.dp,
bottomEnd = if (settings.messageTails) 6.dp else settings.bubbleCornerDp.dp
),
color = MaterialTheme.colorScheme.primaryContainer,
modifier = Modifier.align(Alignment.End)
) {
Text(
"В Токио утро 😎  20:40",
modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
style = MaterialTheme.typography.bodyLarge,
color = MaterialTheme.colorScheme.onPrimaryContainer
)
}
}
}
}
item {
Text("Шрифт приложения", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}
val fonts = listOf("Системный", "Roboto", "Inter", "Onest", "Manrope", "Rubik", "Nunito")
items(fonts.size) { index ->
val fName = fonts.get(index)
val selected = settings.fontStyleName == fName
Surface(
shape = RoundedCornerShape(20.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable {
RedFoxConfig.update { it.copy(fontStyleName = fName) }
}
) {
Row(
modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
verticalAlignment = Alignment.CenterVertically
) {
Text(fName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
RadioButton(selected = selected, onClick = { RedFoxConfig.update { it.copy(fontStyleName = fName) } })
}
}
}
}
}
return
}
// Главный экран «Внешний вид» 1 в 1 как на скриншотах 1, 3, 5 из Opexgram
Scaffold(
topBar = {
TopAppBar(
navigationIcon = {
IconButton(onClick = onBack) {
Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
}
},
title = { Text("Внешний вид", fontWeight = FontWeight.Bold) }
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
Text("Material Design 3", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp))
}
// Живая превью-карточка макета M3E сверху (как на 1-м скриншоте Opexgram)
item {
Surface(
shape = RoundedCornerShape(26.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth()
) {
Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
repeat(3) { idx ->
Surface(
shape = CircleShape,
color = if (idx == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
modifier = Modifier.weight(1f).height(24.dp)
) {}
}
}
Surface(
shape = RoundedCornerShape(settings.bubbleCornerDp.dp),
color = MaterialTheme.colorScheme.surfaceContainerHigh,
modifier = Modifier.fillMaxWidth().height(54.dp)
) {
Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
Box(Modifier.size(32.dp).clip(buildExpressiveAvatarShape(ExpressiveAvatarStyle.FLOWER_12)).background(MaterialTheme.colorScheme.primary))
Spacer(Modifier.width(10.dp))
Column(Modifier.weight(1f)) {
Box(Modifier.width(110.dp).height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
Spacer(Modifier.height(6.dp))
Box(Modifier.width(160.dp).height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)))
}
}
}
ExpressiveM3Slider(
value = 0.65f,
onValueChange = {},
isExpressiveStyle = settings.expressiveSliders,
trackHeightDp = 14f,
thumbHeightDp = 28f
)
}
}
}
// Фон шапки: Тон / Размытие (как на 1-м скриншоте)
item {
Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
Text("Фон шапки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
Badge(containerColor = MaterialTheme.colorScheme.primary) {
Text("NEW", modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onPrimary)
}
}
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
listOf(false to "Тон", true to "✓ Размытие").forEach { (isBlur, title) ->
val selected = settings.headerBlurStyle == isBlur
Surface(
shape = CircleShape,
color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
modifier = Modifier.weight(1f).clickable {
RedFoxConfig.update { it.copy(headerBlurStyle = isBlur) }
}
) {
Text(
text = title,
modifier = Modifier.padding(vertical = 12.dp),
textAlign = TextAlign.Center,
fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
)
}
}
}
}
}
}
// Пункт перехода в конструктор Слайдеров
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable { subPage = "SLIDERS" }
) {
Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Слайдеры", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
Text(
text = if (settings.expressiveSliders) "Expressive, Свой" else "Классический",
color = MaterialTheme.colorScheme.primary,
fontWeight = FontWeight.Bold
)
}
}
}
// Сжатие у краёв (эффект Барабан — скриншот 3)
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable {
RedFoxConfig.update { it.copy(barrelScrollEffect = !it.barrelScrollEffect) }
}
) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Column(Modifier.weight(1f)) {
Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
Text("Сжатие у краёв (Барабан)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
Badge(containerColor = MaterialTheme.colorScheme.primary) {
Text("NEW", modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onPrimary)
}
}
Text("Сообщения и чаты плавно уходят за край", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.barrelScrollEffect, onCheckedChange = { v -> RedFoxConfig.update { it.copy(barrelScrollEffect = v) } })
}
}
}
// Шрифт (переход в экран выбора шрифтов — скриншот 4)
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable { subPage = "FONTS" }
) {
Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Шрифт", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
Text(settings.fontStyleName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
}
}
// Единые скругления: Прямые — Мягкие — Сквиркл — Круглые (скриншот 5)
item {
Text("Форма", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
}
item {
Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
Text("Единые скругления", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
Text("${settings.bubbleCornerDp} dp", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}
ExpressiveM3Slider(
value = settings.bubbleCornerDp.toFloat(),
onValueChange = { v -> RedFoxConfig.update { it.copy(bubbleCornerDp = v.toInt()) } },
valueRange = 8f..30f,
isExpressiveStyle = settings.expressiveSliders,
trackHeightDp = settings.sliderTrackHeightDp,
thumbHeightDp = settings.sliderThumbHeightDp
)
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
Text("Прямые", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
Text("Мягкие", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
Text("Сквиркл", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
Text("Круглые", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
}
}
}
// Хвосты у сообщений и Стеклянные сообщения (скриншот 5)
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable { RedFoxConfig.update { it.copy(messageTails = !it.messageTails) } }
) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Хвосты у сообщений", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.messageTails, onCheckedChange = { v -> RedFoxConfig.update { it.copy(messageTails = v) } })
}
}
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable { RedFoxConfig.update { it.copy(glassMessages = !it.glassMessages) } }
) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Column(Modifier.weight(1f)) {
Text("Стеклянные сообщения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
Text("Полупрозрачные пузыри M3E в переписках", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.glassMessages, onCheckedChange = { v -> RedFoxConfig.update { it.copy(glassMessages = v) } })
}
}
}
// Палитра и облачные темы Telegram
item {
Text("Цветовая палитра и Облачные темы Telegram", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
}
item {
Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
val presets = listOf(
"MONET" to "Material You",
"AMOLED" to "AMOLED Чёрная",
"FOX" to "Fox Sunset",
"TEAL" to "Cyber Teal",
"ROSE" to "Sakura Rose"
)
presets.chunked(2).forEach { rowPresets ->
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
rowPresets.forEach { (code, label) ->
val selected = settings.themePreset == code
FilterChip(
selected = selected,
onClick = { RedFoxConfig.update { it.copy(themePreset = code) } },
shape = CircleShape,
label = { Text(label) },
modifier = Modifier.weight(1f)
)
}
}
}
if (tgThemes.isNotEmpty() || tgAccents.isNotEmpty()) {
HorizontalDivider()
val combinedTg = tgThemes.map { it.name to it.primaryColorHex } + tgAccents.take(6)
combinedTg.chunked(2).forEach { rowItems ->
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
rowItems.forEach { (tName, tHex) ->
val isPicked = settings.themePreset == "TG_SYNC" && settings.syncedThemeName == tName
FilterChip(
selected = isPicked,
onClick = {
RedFoxConfig.update {
it.copy(
themePreset = "TG_SYNC",
syncedTelegramAccentHex = tHex,
syncedThemeName = tName
)
}
},
shape = CircleShape,
label = { Text(tName, maxLines = 1) },
modifier = Modifier.weight(1f)
)
}
}
}
}
}
}
}
item { Spacer(Modifier.height(28.dp)) }
}
}
}
