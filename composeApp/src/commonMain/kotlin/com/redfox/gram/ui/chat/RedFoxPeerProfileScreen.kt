package com.redfox.gram.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.engine.RedFoxTdEngine
import com.redfox.gram.ui.components.ExpressiveAvatarStyle
import com.redfox.gram.ui.components.RedFoxLocalImage
import com.redfox.gram.ui.components.buildExpressiveAvatarShape
import com.redfox.gram.ui.home.ChatPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxPeerProfileScreen(
    chat: ChatPreview,
    onBack: () -> Unit
) {
    val settings by RedFoxConfig.state.collectAsState()
    val avatars by RedFoxTdEngine.chatAvatars.collectAsState()
    val avatarPath = avatars.get(chat.id)
    val flowerShape = remember { buildExpressiveAvatarShape(ExpressiveAvatarStyle.FLOWER_12) }
    val clipboard = LocalClipboardManager.current

    val isHidden = settings.hiddenChatIds.contains(chat.id)
    val isDuo = settings.duoPartnerChatId == chat.id
    var isMuted by remember { mutableStateOf(false) }
    var statusToast by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = { Text("Профиль", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(flowerShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.title.take(1).uppercase(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
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

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = chat.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (chat.id < 0) "Канал / Группа Telegram" else "В сети • RedFox M3E",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDuo) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            RedFoxConfig.update { cur ->
                                if (isDuo) {
                                    cur.copy(duoPartnerChatId = 0L, duoPartnerName = "", duoStartTimestampMs = 0L)
                                } else {
                                    cur.copy(
                                        duoPartnerChatId = chat.id,
                                        duoPartnerName = chat.title,
                                        duoStartTimestampMs = System.currentTimeMillis()
                                    )
                                }
                            }
                            statusToast = if (!isDuo) "Чат закреплён в RedFox Duo ❤️" else "Парный режим отключён"
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("❤️", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Парный режим (RedFox Duo)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isDuo) "Активен • Закреплён на главном экране" else "Закрепить диалог с любимым человеком сверху",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDuo,
                            onCheckedChange = { enable ->
                                RedFoxConfig.update { cur ->
                                    if (enable) cur.copy(duoPartnerChatId = chat.id, duoPartnerName = chat.title, duoStartTimestampMs = System.currentTimeMillis())
                                    else cur.copy(duoPartnerChatId = 0L, duoPartnerName = "", duoStartTimestampMs = 0L)
                                }
                            }
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboard.setText(AnnotatedString(chat.id.toString()))
                            statusToast = "ID ${chat.id} скопирован в буфер"
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chat.id.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ID (нажмите, чтобы скопировать)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isMuted = !isMuted }
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {

Text("Уведомления", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
Text(
text = if (isMuted) "Выключены" else "Включены",
style = MaterialTheme.typography.bodySmall,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
}
Switch(checked = !isMuted, onCheckedChange = { isMuted = !it })
}
}
}
item {
Surface(
shape = RoundedCornerShape(22.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier
.fillMaxWidth()
.clickable {
RedFoxConfig.update { cur ->
val next = if (isHidden) cur.hiddenChatIds - chat.id else cur.hiddenChatIds + chat.id
cur.copy(hiddenChatIds = next)
}
statusToast = if (!isHidden) "Чат перемещён в скрытый раздел Fortress" else "Чат возвращён в общий список"
}
) {
Row(
modifier = Modifier.padding(18.dp),
verticalAlignment = Alignment.CenterVertically
) {
Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
Spacer(Modifier.width(14.dp))
Column(modifier = Modifier.weight(1f)) {
Text("Скрыть чат под замком", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
Text(
text = if (isHidden) "Чат сейчас скрыт" else "Убрать из общего списка чатов",
style = MaterialTheme.typography.bodySmall,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
}
Switch(
checked = isHidden,
onCheckedChange = {
RedFoxConfig.update { cur ->
val next = if (isHidden) cur.hiddenChatIds - chat.id else cur.hiddenChatIds + chat.id
cur.copy(hiddenChatIds = next)
}
}
)
}
}
}
if (statusToast.isNotEmpty()) {
item {
Text(
text = statusToast,
style = MaterialTheme.typography.labelLarge,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.primary
)
}
}
item { Spacer(Modifier.height(24.dp)) }
}
}
}
