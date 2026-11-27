package com.redfox.gram.ui.player
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedFoxFullPlayerScreen(
trackTitle: String,
artist: String,
isPlaying: Boolean,
onTogglePlay: () -> Unit,
onBack: () -> Unit
) {
var progress by remember { mutableStateOf(0.35f) }
val demoLyrics = remember {
listOf(
"Waiting in a car",
"Waiting for a ride in the dark",
"The night city grows",
"Look and see her eyes, they glow",
"Waiting in a car",
"Waiting for a ride in the dark",
"Drinking in the lounge",
"Following the neon signs"
)
}
val activeIndex = (progress * demoLyrics.size).toInt().coerceIn(0, demoLyrics.lastIndex)
Scaffold(
topBar = {
CenterAlignedTopAppBar(
navigationIcon = {
TextButton(onClick = onBack) { Text("↓ Свернуть") }
},
title = {
Text("RedFox Player • LRCLIB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}
)
}
) { padding ->
Column(
modifier = Modifier
.fillMaxSize()
.padding(padding)
.padding(horizontal = 20.dp),
horizontalAlignment = Alignment.CenterHorizontally
) {
// Обложка в стиле Rhythm / Vivi-Music M3E
Box(
modifier = Modifier
.size(160.dp)
.clip(RoundedCornerShape(34.dp))
.background(MaterialTheme.colorScheme.primaryContainer),
contentAlignment = Alignment.Center
) {
Text(
text = "♫",
style = MaterialTheme.typography.displayLarge,
color = MaterialTheme.colorScheme.onPrimaryContainer
)
}
Spacer(Modifier.height(16.dp))
Text(trackTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
Text(artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
Spacer(Modifier.height(18.dp))
// Волнистая полоса перемотки M3E
WavySeekbar(
progress = progress,
isPlaying = isPlaying,
onSeek = { progress = it }
)
Spacer(Modifier.height(12.dp))
Button(
onClick = onTogglePlay,
shape = CircleShape,
contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
) {
Text(if (isPlaying) "Пауза" else "Воспроизвести", fontWeight = FontWeight.Bold)
}
Spacer(Modifier.height(18.dp))
// Карточка караоке-текста LRCLIB
Surface(
shape = RoundedCornerShape(28.dp),
color = MaterialTheme.colorScheme.surfaceContainerLow,
modifier = Modifier.fillMaxWidth().weight(1f)
) {
LazyColumn(
modifier = Modifier.padding(18.dp),
verticalArrangement = Arrangement.spacedBy(12.dp)
) {
itemsIndexed(demoLyrics) { index, line ->
val isCurrent = index == activeIndex
Text(
text = line,
style = if (isCurrent) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
color = if (isCurrent) MaterialTheme.colorScheme.primary
else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
)
}
}
}
Spacer(Modifier.height(16.dp))
}
}
}
