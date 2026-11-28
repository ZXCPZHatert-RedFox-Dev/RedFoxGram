package com.redfox.gram.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ExpressiveWavyProgressBar(
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3e_wavy_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val animatedAmplitudeFactor by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "wave_amp"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val midY = size.height / 2f
        val strokePx = 3.5.dp.toPx()
        val thumbRadius = 6.dp.toPx()
        val pad = thumbRadius + 2.dp.toPx()
        val usableWidth = (size.width - pad * 2f).coerceAtLeast(1f)
        val clampedProgress = progress.coerceIn(0f, 1f)
        val activeEndX = pad + usableWidth * clampedProgress

        if (activeEndX + thumbRadius < size.width - pad) {
            drawLine(
                color = trackColor,
                start = Offset(activeEndX + thumbRadius + 3.dp.toPx(), midY),
                end = Offset(size.width - pad, midY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }

        val maxAmp = 4.2.dp.toPx() * animatedAmplitudeFactor
        val wavelengthPx = 28.dp.toPx()
        val fadeZonePx = 14.dp.toPx()

        val path = Path()
        var x = pad
        val step = 1.dp.toPx()
        var first = true

        while (x <= activeEndX) {
            val distFromStart = (x - pad).coerceAtLeast(0f)
            val distFromEnd = (activeEndX - x).coerceAtLeast(0f)
            val edgeEnvelope = minOf(
                (distFromStart / fadeZonePx).coerceIn(0f, 1f),
                (distFromEnd / fadeZonePx).coerceIn(0f, 1f)
            )
            val smoothEnvelope = edgeEnvelope * edgeEnvelope * (3f - 2f * edgeEnvelope)
            val angle = ((x - pad) / wavelengthPx) * (2.0 * PI).toFloat() - phase
            val y = midY + sin(angle) * maxAmp * smoothEnvelope

            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
            x += step
        }
        path.lineTo(activeEndX, midY)

        drawPath(
            path = path,
            color = activeColor,
            style = Stroke(
                width = strokePx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        drawCircle(
            color = activeColor,
            radius = thumbRadius,
            center = Offset(activeEndX, midY)
        )
    }
}
