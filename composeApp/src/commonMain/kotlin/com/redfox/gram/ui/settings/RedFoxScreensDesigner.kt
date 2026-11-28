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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.ui.components.ExpressiveAvatarStyle
import com.redfox.gram.ui.components.buildExpressiveAvatarShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxScreensDesigner(
    initialSection: String, // "NAV_BAR", "HEADER", "SETTINGS_SCREEN"
    onBack: () -> Unit
) {
    val settings by RedFoxConfig.state.collectAsState()
    var headerInput by remember { mutableStateOf(settings.customHeaderTitle) }
    val starShape = remember { buildExpressiveAvatarShape(ExpressiveAvatarStyle.FLOWER_12) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                title = {
                    Text(
                        text = when (initialSection) {
                            "NAV_BAR" -> "Панель вкладок"
                            "HEADER" -> "Шапка списка чатов"
                            else -> "Экран настроек"
                        },
                        fontWeight = FontWeight.Bold
                    )
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
            if (initialSection == "NAV_BAR") {
                // Живое превью нижней панели (скриншот 2)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = if (settings.navBarStyle == "BAR") RoundedCornerShape(16.dp) else CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = if (settings.navBarStyle == "FLOATING") Modifier else Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text(
                                        "Чаты",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.padding(8.dp))
                                if (settings.navShowProfileTab) {
                                    Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                        if (settings.navActionInRow) {
                            Spacer(Modifier.width(12.dp))
                            Surface(
                                shape = starShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("+", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Стиль панели", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("GLASS" to "Стекло", "BAR" to "Панель", "FLOATING" to "Плавающая").forEach { (code, label) ->
                            val selected = settings.navBarStyle == code
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.weight(1f).clickable {
                                    RedFoxConfig.update { it.copy(navBarStyle = code) }
                                }
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 14.dp),
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                item {
                    Text("Вкладки и кнопка действия", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().clickable {
                            RedFoxConfig.update { it.copy(navShowProfileTab = !it.navShowProfileTab) }
                        }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Вкладка «Профиль» в панели", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.navShowProfileTab, onCheckedChange = { v -> RedFoxConfig.update { it.copy(navShowProfileTab = v) } })
                        }
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().clickable {
                            RedFoxConfig.update { it.copy(navActionInRow = !it.navActionInRow) }
                        }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Кнопка действия (+) в одном ряду", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.navActionInRow, onCheckedChange = { v -> RedFoxConfig.update { it.copy(navActionInRow = v) } })
                        }
                    }
                }
            }


if (initialSection == "HEADER") {
// Превью шапки списка чатов (скриншот 3)
item {
Surface(
shape = RoundedCornerShape(24.dp),
color = MaterialTheme.colorScheme.surfaceContainerHigh,
modifier = Modifier.fillMaxWidth()
) {
Row(
modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
verticalAlignment = Alignment.CenterVertically
) {
if (settings.showSearchInHeader) {
Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
Spacer(Modifier.width(12.dp))
}
Text(
text = (if (settings.holidaySantaHatEnabled) "🎅 " else "") +
settings.customHeaderTitle.ifBlank { "RedFoxGram" } +
(if (settings.holidaySnowEnabled) " ❄" else ""),
style = MaterialTheme.typography.titleLarge,
fontWeight = FontWeight.ExtraBold,
textAlign = if (settings.centerHeaderTitle) TextAlign.Center else TextAlign.Start,
modifier = Modifier.weight(1f)
)
}
}
}
item {
Text("Заголовок", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}
item {
OutlinedTextField(
value = headerInput,
onValueChange = { txt ->
headerInput = txt
RedFoxConfig.update { it.copy(customHeaderTitle = txt) }
},
label = { Text("Текст шапки (пусто = RedFoxGram)") },
singleLine = true,
shape = RoundedCornerShape(20.dp),
modifier = Modifier.fillMaxWidth()
)
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable {
RedFoxConfig.update { it.copy(centerHeaderTitle = !it.centerHeaderTitle) }
}
) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Заголовок по центру", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.centerHeaderTitle, onCheckedChange = { v -> RedFoxConfig.update { it.copy(centerHeaderTitle = v) } })
}
}
}
item {
Text("Праздники и Кнопки", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable {
RedFoxConfig.update { it.copy(holidaySnowEnabled = !it.holidaySnowEnabled) }
}
) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Column(Modifier.weight(1f)) { Text("Снег", style = MaterialTheme.typography.titleMedium); Text("Всегда", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.holidaySnowEnabled, onCheckedChange = { v -> RedFoxConfig.update { it.copy(holidaySnowEnabled = v) } })
}
}
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable {
RedFoxConfig.update { it.copy(holidaySantaHatEnabled = !it.holidaySantaHatEnabled) }
}
) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Column(Modifier.weight(1f)) { Text("Колпак", style = MaterialTheme.typography.titleMedium); Text("Всегда", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.holidaySantaHatEnabled, onCheckedChange = { v -> RedFoxConfig.update { it.copy(holidaySantaHatEnabled = v) } })
}
}
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().clickable {
RedFoxConfig.update { it.copy(showSearchInHeader = !it.showSearchInHeader) }
}
) {
Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
Text("Поиск в шапке", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
com.redfox.gram.ui.components.ExpressiveSwitch(checked = settings.showSearchInHeader, onCheckedChange = { v -> RedFoxConfig.update { it.copy(showSearchInHeader = v) } })
}
}
}
}
if (initialSection == "SETTINGS_SCREEN") {
// Цвет иконок: Стандарт / Один акцент / Гармонизация (скриншоты 4 и 5)
item {
Text("Цвет иконок в настройках", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}
item {
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
listOf(
"STANDARD" to "Стандарт",
"ACCENT" to "Один акцент",
"HARMONIZED" to "Гармонизация"
).forEach { (mode, title) ->
val selected = settings.iconColorMode == mode
Surface(
shape = RoundedCornerShape(18.dp),
color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.weight(1f).clickable {
RedFoxConfig.update { it.copy(iconColorMode = mode) }
}
) {
Text(
text = title,
modifier = Modifier.padding(vertical = 14.dp),
textAlign = TextAlign.Center,
style = MaterialTheme.typography.labelLarge,
fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
)
}
}
}
}
}
item { Spacer(Modifier.height(24.dp)) }
}
}
}
