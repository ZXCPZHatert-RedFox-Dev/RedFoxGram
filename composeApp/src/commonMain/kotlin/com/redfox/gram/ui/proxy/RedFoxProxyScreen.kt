package com.redfox.gram.ui.proxy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redfox.gram.network.ProxyNode
import com.redfox.gram.network.RedFoxProxyManager
import com.redfox.gram.ui.components.Android17Switch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxProxyScreen(onBack: () -> Unit) {
    val state by RedFoxProxyManager.state.collectAsState()
    var linkInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("RedFox Shield (Без VPN)", fontWeight = FontWeight.Bold)
                        Text(
                            "Встроенный MTProto Fake-TLS & SOCKS5 движок",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("<- Назад")
                    }
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
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.enabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (state.enabled && state.nodes.isNotEmpty()) "Обход активен" else "Обход отключен",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                val activeNode = state.nodes.find { it.id == state.activeNodeId }
                                Text(
                                    text = activeNode?.let { "${it.name} • ${it.pingMs} мс" } ?: "Прямое соединение",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Android17Switch(
                                checked = state.enabled && state.nodes.isNotEmpty(),
                                onCheckedChange = { RedFoxProxyManager.toggleEnabled(it) }
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    RedFoxProxyManager.refreshAllPings()
                                    statusMessage = "Пинг узлов обновлён, выбран лучший сервер"
                                },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Проверить пинг")
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { RedFoxProxyManager.toggleSmartRotation(!state.autoSmartRotation) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Умная авто-ротация (NekoX)", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Автоматически переключаться на узел с минимальным пингом",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Android17Switch(
                            checked = state.autoSmartRotation,
                            onCheckedChange = { RedFoxProxyManager.toggleSmartRotation(it) }
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {

Column(modifier = Modifier.padding(16.dp)) {
Text("Добавить прокси по ссылке", fontWeight = FontWeight.SemiBold)
Spacer(Modifier.height(8.dp))
Row(
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
OutlinedTextField(
value = linkInput,
onValueChange = { linkInput = it },
modifier = Modifier.weight(1f),
placeholder = { Text("tg://proxy?server=...&port=...&secret=...") },
singleLine = true,
shape = RoundedCornerShape(16.dp)
)
FilledTonalButton(
onClick = {
if (RedFoxProxyManager.addFromLink(linkInput)) {
statusMessage = "Прокси добавлен и активирован!"
linkInput = ""
} else {
statusMessage = "Неверный формат ссылки (нужны server и port)"
}
},
shape = RoundedCornerShape(16.dp)
) {
Text("+")
}
}
statusMessage?.let { msg ->
Spacer(Modifier.height(6.dp))
Text(msg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
}
}
}
}
item {
Text(
text = "Доступные серверы (${state.nodes.size})",
style = MaterialTheme.typography.titleSmall,
color = MaterialTheme.colorScheme.primary,
fontWeight = FontWeight.SemiBold,
modifier = Modifier.padding(start = 8.dp, top = 4.dp)
)
}
items(state.nodes, key = { it.id }) { node ->
ProxyNodeCard(
node = node,
isSelected = node.id == state.activeNodeId,
onSelect = { RedFoxProxyManager.selectNode(node.id) }
)
}
item { Spacer(Modifier.height(24.dp)) }
}
}
}
@Composable
private fun ProxyNodeCard(
node: ProxyNode,
isSelected: Boolean,
onSelect: () -> Unit
) {
Surface(
shape = RoundedCornerShape(22.dp),
color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier
.fillMaxWidth()
.clickable { onSelect() }
) {
Row(
modifier = Modifier.padding(16.dp),
verticalAlignment = Alignment.CenterVertically
) {
Column(modifier = Modifier.weight(1f)) {
Text(node.name, fontWeight = FontWeight.SemiBold)
Text(
text = "${node.protocol.label} • ${node.host}:${node.port}",
style = MaterialTheme.typography.bodySmall,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
}
Surface(
shape = RoundedCornerShape(12.dp),
color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
Text(
text = if (node.pingMs > 0) "${node.pingMs} мс" else "—",
style = MaterialTheme.typography.labelMedium,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.primary,
modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
)
}
}
}
}
