package com.redfox.gram.ui.components
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
@Composable
fun ExpressiveWavyProgressBar(
progress: Float,
isPlaying: Boolean,
modifier: Modifier = Modifier,
waveAmplitudeDp: Dp = 3.5.dp,
waveLengthDp: Dp = 26.dp,
strokeWidthDp: Dp = 3.5.dp,
activeColor: Color = MaterialTheme.colorScheme.primary,
trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
onSeek: ((Float) -> Unit)? = null
) {
val clampedProgress = progress.coerceIn(0f, 1f)
val infiniteTransition = rememberInfiniteTransition(label = "a17_wavy_progress")
val phase by infiniteTransition.animateFloat(
initialValue = 0f,
targetValue = (2f * PI).toFloat(),
animationSpec = infiniteRepeatable(
animation = tween(durationMillis = 1200, easing = LinearEasing),
repeatMode = RepeatMode.Restart
),
label = "wave_phase"
)
val animatedAmplitude by animateFloatAsState(
targetValue = if (isPlaying) 1f else 0f,
animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
label = "wave_amp"
)
val interactiveModifier = if (onSeek != null) {
modifier
.pointerInput(Unit) {
detectTapGestures { offset ->
if (size.width > 0) {
onSeek((offset.x / size.width).coerceIn(0f, 1f))
}
}
}
.pointerInput(Unit) {
detectHorizontalDragGestures { change, _ ->
change.consume()
if (size.width > 0) {
onSeek((change.position.x / size.width).coerceIn(0f, 1f))
}
}
}
} else {
modifier
}
Canvas(
modifier = interactiveModifier
.fillMaxWidth()
.height(24.dp)
) {
val centerY = size.height / 2f
val strokePx = strokeWidthDp.toPx()
val ampPx = waveAmplitudeDp.toPx() * animatedAmplitude
val waveLenPx = waveLengthDp.toPx().coerceAtLeast(8f)
val thumbX = size.width * clampedProgress
if (thumbX < size.width - 6.dp.toPx()) {
drawLine(
color = trackColor,
start = Offset((thumbX + 6.dp.toPx()).coerceAtMost(size.width), centerY),
end = Offset(size.width, centerY),
strokeWidth = strokePx,
cap = StrokeCap.Round
)
}
if (thumbX > 2f) {
val wavePath = Path()
val step = 2.5f
var x = 0f
var first = true
while (x <= thumbX) {
val angle = (x / waveLenPx) * (2f * PI).toFloat() - phase
val y = centerY + sin(angle) * ampPx
if (first) {
wavePath.moveTo(x, y)
first = false
} else {
wavePath.lineTo(x, y)
}
x += step
}
drawPath(
path = wavePath,
color = activeColor,
style = Stroke(width = strokePx, cap = StrokeCap.Round)
)
}
val thumbWidth = 4.5.dp.toPx()
val thumbHeight = 16.dp.toPx()
drawRoundRect(
color = activeColor,
topLeft = Offset(
x = (thumbX - thumbWidth / 2f).coerceIn(0f, (size.width - thumbWidth).coerceAtLeast(0f)),
y = centerY - thumbHeight / 2f
),
size = Size(thumbWidth, thumbHeight),
cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f)
)
}
}
