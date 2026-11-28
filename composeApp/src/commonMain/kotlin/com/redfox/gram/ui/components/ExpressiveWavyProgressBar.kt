package com.redfox.gram.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "opex_wavy_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
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
            .height(24.dp)
    ) {
        val midY = size.height / 2f
        val waveStrokePx = 2.2.dp.toPx()
        val trackStrokePx = 2.dp.toPx()
        val thumbWidthPx = 3.dp.toPx()
        val thumbHeightPx = 14.dp.toPx()
        val pad = thumbWidthPx + 2.dp.toPx()
        val usableWidth = (size.width - pad * 2f).coerceAtLeast(1f)
        val clampedProgress = progress.coerceIn(0f, 1f)
        val activeEndX = pad + usableWidth * clampedProgress

        val trackStartX = activeEndX + thumbWidthPx + 3.dp.toPx()
        if (trackStartX < size.width - pad) {
            drawLine(
                color = trackColor,
                start = Offset(trackStartX, midY),
                end = Offset(size.width - pad, midY),
                strokeWidth = trackStrokePx,
                cap = StrokeCap.Round
            )
        }

        val maxAmp = 2.7.dp.toPx() * animatedAmplitudeFactor
        val wavelengthPx = 15.dp.toPx()
        val fadeZonePx = 10.dp.toPx()
        val waveStopX = (activeEndX - thumbWidthPx - 2.dp.toPx()).coerceAtLeast(pad)

        val path = Path()
        var x = pad
        val step = 0.8.dp.toPx()
        var first = true

        while (x <= waveStopX) {
            val distFromStart = (x - pad).coerceAtLeast(0f)
            val distFromEnd = (waveStopX - x).coerceAtLeast(0f)
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
        path.lineTo(waveStopX, midY)

        drawPath(
            path = path,
            color = activeColor,
            style = Stroke(
                width = waveStrokePx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        drawRoundRect(
            color = activeColor,
            topLeft = Offset(activeEndX - thumbWidthPx / 2f, midY - thumbHeightPx / 2f),
            size = Size(thumbWidthPx, thumbHeightPx),
            cornerRadius = CornerRadius(thumbWidthPx / 2f, thumbWidthPx / 2f)
        )
    }
}
