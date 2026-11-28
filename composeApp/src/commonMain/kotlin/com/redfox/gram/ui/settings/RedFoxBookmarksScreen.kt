package com.redfox.gram.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.redfox.gram.config.RedFoxBookmarksStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxBookmarksScreen(onBack: () -> Unit) {
    val bookmarks by RedFoxBookmarksStore.items.collectAsState()
    var selectedTag by remember { mutableStateOf("Все") }
    val clipboard = LocalClipboardManager.current
    var copiedNote by remember { mutableStateOf("") }

    val tags = listOf("Все", "#важное", "#музыка", "#идеи", "#для_нас")
    val filtered = remember(bookmarks, selectedTag) {
        if (selectedTag == "Все") bookmarks else bookmarks.filter { it.tag == selectedTag }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                title = { Text("Умные закладки", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                items(tags) { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { selectedTag = tag },
                        shape = CircleShape,
                        label = { Text(tag, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (copiedNote.isNotEmpty()) {
                Text(
                    text = copiedNote,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Нет закладок по тегу $selectedTag.\nНажмите на любое сообщение в чате → «В закладки».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { item ->
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth().clickable {
                                clipboard.setText(AnnotatedString(item.text))
                                copiedNote = "Текст закладки скопирован в буфер"
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                            Text(
                                                text = item.tag,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Text(
                                            text = item.chatTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(text = item.text, style = MaterialTheme.typography.bodyLarge)
                                }
                                IconButton(onClick = { RedFoxBookmarksStore.removeBookmark(item.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
