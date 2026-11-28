package com.redfox.gram.ui.player
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.redfox.gram.media.RedFoxAudioController
import com.redfox.gram.ui.components.ExpressiveWavyProgressBar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxFullPlayerScreen(
trackTitle: String,
artist: String,
isPlaying: Boolean,
onTogglePlay: () -> Unit,
onBack: () -> Unit
) {
val activeTrack by RedFoxAudioController.track.collectAsState()
val displayTitle = activeTrack.title.ifBlank { trackTitle }
val displayArtist = activeTrack.artist.ifBlank { artist }
val currentProgress = activeTrack.progress.coerceIn(0f, 1f)
val lyrics = if (activeTrack.fullLyricsLines.isNotEmpty()) {
activeTrack.fullLyricsLines
} else {
listOf(
"Запустите любой трек из чата или бота Яндекс Музыка",
"Тексты песен подгрузятся автоматически из LRCLIB"
)
}
Scaffold(
topBar = {
CenterAlignedTopAppBar(
navigationIcon = {
TextButton(onClick = onBack) {
Text("↓ Свернуть", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}
},
title = {
Text(
"RedFox Player • LRCLIB",
style = MaterialTheme.typography.titleMedium,
fontWeight = FontWeight.ExtraBold
)
},
actions = {
TextButton(onClick = {
RedFoxAudioController.closePlayer()
onBack()
}) {
Text("✕", color = MaterialTheme.colorScheme.primary)
}
}
)
}
) { padding ->
Column(
modifier = Modifier
.fillMaxSize()
.padding(padding)
.padding(horizontal = 24.dp),
horizontalAlignment = Alignment.CenterHorizontally
) {
Spacer(Modifier.height(12.dp))
Box(
modifier = Modifier
.size(164.dp)
.clip(RoundedCornerShape(42.dp))
.background(MaterialTheme.colorScheme.primaryContainer),
contentAlignment = Alignment.Center
) {
Text(
text = "♫",
style = MaterialTheme.typography.displayMedium,
color = MaterialTheme.colorScheme.primary
)
}
Spacer(Modifier.height(20.dp))
Text(
text = displayTitle,
style = MaterialTheme.typography.headlineSmall,
fontWeight = FontWeight.ExtraBold,
textAlign = TextAlign.Center
)
Spacer(Modifier.height(4.dp))
Text(
text = displayArtist,
style = MaterialTheme.typography.bodyMedium,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
Spacer(Modifier.height(22.dp))
Box(
modifier = Modifier
.fillMaxWidth()
.pointerInput(Unit) {
detectTapGestures { offset ->
val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
RedFoxAudioController.seekTo(frac)
}
}
) {
ExpressiveWavyProgressBar(
progress = currentProgress,
isPlaying = activeTrack.isPlaying,
activeColor = MaterialTheme.colorScheme.primary,
trackColor = MaterialTheme.colorScheme.surfaceVariant
)
}
Spacer(Modifier.height(16.dp))
Button(
onClick = onTogglePlay,
shape = CircleShape,
contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
) {
Text(
text = if (activeTrack.isPlaying) "Пауза" else "Играть",
style = MaterialTheme.typography.titleMedium,
fontWeight = FontWeight.Bold
)
}
Spacer(Modifier.height(20.dp))
Surface(
shape = RoundedCornerShape(28.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().weight(1f)
) {
val activeIndex = if (lyrics.isNotEmpty()) (currentProgress * lyrics.size).toInt().coerceIn(0, lyrics.lastIndex) else 0
LazyColumn(
modifier = Modifier.padding(18.dp),
verticalArrangement = Arrangement.spacedBy(10.dp)
) {
itemsIndexed(lyrics) { idx, line ->
val isCurrent = idx == activeIndex
Text(
text = line,
style = if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
)
}
}
}
Spacer(Modifier.height(16.dp))
}
}
}
