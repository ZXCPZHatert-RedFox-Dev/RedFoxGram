package com.redfox.gram.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

data class LrcLine(val timeMs: Long, val text: String)

object LrcParser {
    private val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)")

    fun parse(rawLrc: String): List<LrcLine> {
        return rawLrc.lines().mapNotNull { line ->
            val match = regex.find(line.trim()) ?: return@mapNotNull null
            val (_, minStr, secStr, msStr, rawText) = match.groupValues
            val min = minStr.toLongOrNull() ?: 0L
            val sec = secStr.toLongOrNull() ?: 0L
            val ms = if (msStr.length == 2) (msStr.toLongOrNull() ?: 0L) * 10 else (msStr.toLongOrNull() ?: 0L)
            val text = rawText.trim()
            if (text.isEmpty()) null else LrcLine(min * 60_000 + sec * 1000 + ms, text)
        }.sortedBy { it.timeMs }
    }

    fun getCurrentLine(lines: List<LrcLine>, positionMs: Long): String {
        return lines.lastOrNull { it.timeMs <= positionMs }?.text ?: "♪ • • •"
    }
}

@Composable
fun LiveLrcMiniBar(
    trackTitle: String,
    artist: String,
    currentLine: String,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onOpenFullPlayer: () -> Unit
) {
    Surface(
        onClick = onOpenFullPlayer,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentLine,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$artist — $trackTitle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onTogglePlay,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(if (isPlaying) "Пауза" else "Играть")
            }
        }
    }
}

@Composable
fun WavySeekbar(
    progress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val activePhase = if (isPlaying) phase else 0f
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
    ) {
        val midY = size.height / 2f
        val activeWidth = size.width * progress.coerceIn(0f, 1f)
        val amplitude = if (isPlaying) 6.dp.toPx() else 0f
        val wavelength = 28.dp.toPx()

        drawLine(
            color = trackColor,
            start = Offset(activeWidth, midY),
            end = Offset(size.width, midY),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        if (activeWidth > 0f) {
            val path = Path()
            path.moveTo(0f, midY)
            var x = 0f
            while (x <= activeWidth) {
                val y = midY + amplitude * sin((x / wavelength) * 2 * PI + activePhase).toFloat()
                path.lineTo(x, y)
                x += 3f
            }
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        drawCircle(
            color = primaryColor,
            radius = 7.dp.toPx(),
            center = Offset(activeWidth, midY)
        )
    }
}
