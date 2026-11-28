package com.redfox.gram.ui.player
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redfox.gram.media.LrcTimedLine
import com.redfox.gram.media.RedFoxAudioController
import com.redfox.gram.ui.components.ExpressiveAvatarStyle
import com.redfox.gram.ui.components.ExpressiveWavyProgressBar
import com.redfox.gram.ui.components.buildExpressiveAvatarShape
private fun formatMillis(ms: Long): String {
val totalSec = (ms / 1000L).coerceAtLeast(0L)
val min = totalSec / 60L
val sec = totalSec % 60L
return "$min:${sec.toString().padStart(2, '0')}"
}
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
val listState = rememberLazyListState()
val flowerButtonShape = remember { buildExpressiveAvatarShape(ExpressiveAvatarStyle.FLOWER_12) }
val displayTitle = activeTrack.title.ifBlank { trackTitle }
val displayArtist = activeTrack.artist.ifBlank { artist }
val currentProgress = activeTrack.progress.coerceIn(0f, 1f)
val lyrics = if (activeTrack.timedLyrics.isNotEmpty()) {
activeTrack.timedLyrics
} else {
listOf(
LrcTimedLine(0L, "Загрузка синхронного караоке..."),
LrcTimedLine(2000L, "$displayArtist — $displayTitle")
)
}
LaunchedEffect(activeTrack.activeLineIndex) {
if (lyrics.isNotEmpty()) {
val targetIdx = (activeTrack.activeLineIndex - 2).coerceIn(0, lyrics.lastIndex)
listState.animateScrollToItem(targetIdx)
}
}
Box(
modifier = Modifier
.fillMaxSize()
.background(MaterialTheme.colorScheme.background)
) {
// Размытая на весь экран обложка песни (как в Opexgram / Apple Music)
if (activeTrack.albumArt != null) {
Image(
bitmap = activeTrack.albumArt!!,
contentDescription = null,
contentScale = ContentScale.Crop,
modifier = Modifier
.fillMaxSize()
.blur(64.dp)
)
Box(
modifier = Modifier
.fillMaxSize()
.background(
Brush.verticalGradient(
colors = listOf(
Color.Black.copy(alpha = 0.58f),
Color.Black.copy(alpha = 0.72f),
Color.Black.copy(alpha = 0.88f)
)
)
)
)
} else {
// Мягкий акцентный градиент на весь экран, если у файла нет встроенной картинки
Box(
modifier = Modifier
.fillMaxSize()
.background(
Brush.verticalGradient(
colors = listOf(
MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
MaterialTheme.colorScheme.background
)
)
)
)
}
Scaffold(
containerColor = Color.Transparent,
topBar = {
TopAppBar(
colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
navigationIcon = {
IconButton(onClick = onBack) {
Icon(
imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
contentDescription = "Назад",
tint = Color.White
)
}
},
title = {
Column {
Text(
text = "Текст песни",
style = MaterialTheme.typography.titleMedium,
fontWeight = FontWeight.Bold,
color = Color.White
)
Text(
text = "$displayArtist — $displayTitle",
style = MaterialTheme.typography.labelSmall,
color = Color.White.copy(alpha = 0.75f),
maxLines = 1,
overflow = TextOverflow.Ellipsis
)
}
},
actions = {
IconButton(onClick = {
RedFoxAudioController.closePlayer()
onBack()
}) {
Icon(
imageVector = Icons.Rounded.Close,
contentDescription = "Закрыть плеер",
tint = Color.White.copy(alpha = 0.8f)
)
}
}
)
},
bottomBar = {
Surface(
color = Color.Black.copy(alpha = 0.42f),
shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
modifier = Modifier.fillMaxWidth().navigationBarsPadding()
) {
Column(
modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
) {
Row(
verticalAlignment = Alignment.CenterVertically,
modifier = Modifier.fillMaxWidth()
) {
Column(modifier = Modifier.weight(1f)) {
Text(
text = displayTitle,
style = MaterialTheme.typography.titleMedium,
fontWeight = FontWeight.ExtraBold,
color = Color.White,
maxLines = 1,
overflow = TextOverflow.Ellipsis
)
Text(
text = displayArtist,
style = MaterialTheme.typography.bodySmall,
color = Color.White.copy(alpha = 0.75f),
maxLines = 1
)
}
Spacer(Modifier.width(12.dp))
Box(
modifier = Modifier
.size(48.dp)
.clip(RoundedCornerShape(12.dp))
.background(MaterialTheme.colorScheme.primaryContainer),
contentAlignment = Alignment.Center
) {
if (activeTrack.albumArt != null) {
Image(
bitmap = activeTrack.albumArt!!,
contentDescription = displayTitle,
contentScale = ContentScale.Crop,
modifier = Modifier.fillMaxSize()
)
} else {
Text("♫", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
}
}
}
Spacer(Modifier.height(10.dp))
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
trackColor = Color.White.copy(alpha = 0.25f)
)
}
Row(
modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
horizontalArrangement = Arrangement.SpaceBetween
) {
Text(
text = formatMillis(activeTrack.currentMs),
style = MaterialTheme.typography.labelSmall,
color = Color.White.copy(alpha = 0.75f)
)
Text(
text = formatMillis(activeTrack.durationMs),
style = MaterialTheme.typography.labelSmall,
color = Color.White.copy(alpha = 0.75f)
)
}
Spacer(Modifier.height(8.dp))
Row(
modifier = Modifier.fillMaxWidth(),
horizontalArrangement = Arrangement.SpaceEvenly,
verticalAlignment = Alignment.CenterVertically
) {
IconButton(onClick = { RedFoxAudioController.seekTo((currentProgress - 0.1f).coerceAtLeast(0f)) }) {
Text("⏮", style = MaterialTheme.typography.headlineSmall, color = Color.White)
}
Surface(
onClick = onTogglePlay,
shape = flowerButtonShape,
color = MaterialTheme.colorScheme.primary,
modifier = Modifier.size(68.dp)
) {
Box(contentAlignment = Alignment.Center) {
if (activeTrack.isPlaying) {
Text(
text = "❚❚",
style = MaterialTheme.typography.titleLarge,
fontWeight = FontWeight.ExtraBold,
color = MaterialTheme.colorScheme.onPrimary
)
} else {
Icon(
imageVector = Icons.Rounded.PlayArrow,
contentDescription = "Играть",
tint = MaterialTheme.colorScheme.onPrimary,
modifier = Modifier.size(34.dp)
)
}
}
}
IconButton(onClick = { RedFoxAudioController.seekTo((currentProgress + 0.1f).coerceAtMost(1f)) }) {
Text("⏭", style = MaterialTheme.typography.headlineSmall, color = Color.White)
}
}
}
}
}
) { padding ->
LazyColumn(
state = listState,
modifier = Modifier
.fillMaxSize()
.padding(padding)
.padding(horizontal = 24.dp),
verticalArrangement = Arrangement.spacedBy(18.dp),
contentPadding = PaddingValues(top = 24.dp, bottom = 36.dp)
) {
itemsIndexed(lyrics) { idx, lineItem ->
val isCurrent = idx == activeTrack.activeLineIndex
val isPast = idx < activeTrack.activeLineIndex
if (isCurrent) {
val splitPos = (lineItem.text.length * activeTrack.lineFillProgress)
.toInt()
.coerceIn(0, lineItem.text.length)
val highlightedPart = lineItem.text.substring(0, splitPos)
val remainingPart = lineItem.text.substring(splitPos)
Text(
text = buildAnnotatedString {
withStyle(
SpanStyle(
color = Color.White,
fontWeight = FontWeight.ExtraBold
)
) {
append(highlightedPart)
}
withStyle(
SpanStyle(
color = Color.White.copy(alpha = 0.45f),
fontWeight = FontWeight.Bold
)
) {
append(remainingPart)
}
},
style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 32.sp)
)
} else {
Text(
text = lineItem.text,
style = MaterialTheme.typography.titleLarge.copy(lineHeight = 28.sp),
fontWeight = FontWeight.SemiBold,
color = Color.White.copy(alpha = if (isPast) 0.55f else 0.38f)
)
}
}
}
}
}
}
