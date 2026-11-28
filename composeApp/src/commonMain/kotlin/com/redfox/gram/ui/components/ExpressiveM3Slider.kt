package com.redfox.gram.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun ExpressiveM3Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    isExpressiveStyle: Boolean = true,
    trackHeightDp: Float = 20f,
    thumbHeightDp: Float = 44f,
    thumbWidthDp: Float = 8f,
    gapDp: Float = 8f,
    endDotDp: Float = 6f,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height((thumbHeightDp.coerceAtLeast(trackHeightDp) + 8f).dp)
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    val f = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + f * (valueRange.endInclusive - valueRange.start))
                }
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val f = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + f * (valueRange.endInclusive - valueRange.start))
                }
            }
    ) {
        val midY = size.height / 2f
        val tHeight = if (isExpressiveStyle) trackHeightDp.dp.toPx() else 6.dp.toPx()
        val hHeight = if (isExpressiveStyle) thumbHeightDp.dp.toPx() else 22.dp.toPx()
        val hWidth = if (isExpressiveStyle) thumbWidthDp.dp.toPx() else 22.dp.toPx()
        val gap = if (isExpressiveStyle) gapDp.dp.toPx() else 4.dp.toPx()
        val pad = hWidth / 2f + 2.dp.toPx()
        val usable = (size.width - pad * 2f).coerceAtLeast(1f)
        val thumbX = pad + usable * fraction
        val corner = CornerRadius(tHeight / 2f, tHeight / 2f)

        // Левая активная дорожка M3E
        val leftEnd = (thumbX - hWidth / 2f - gap).coerceAtLeast(pad)
        if (leftEnd > pad) {
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(pad, midY - tHeight / 2f),
                size = Size(leftEnd - pad, tHeight),
                cornerRadius = corner
            )
        }

        // Правая неактивная дорожка M3E
        val rightStart = (thumbX + hWidth / 2f + gap).coerceAtMost(size.width - pad)
        if (size.width - pad > rightStart) {
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(rightStart, midY - tHeight / 2f),
                size = Size((size.width - pad) - rightStart, tHeight),
                cornerRadius = corner
            )
            // Фирменная точка в конце дорожки (как на скриншоте из Opexgram)
            if (isExpressiveStyle && endDotDp > 0f) {
                drawCircle(
                    color = activeColor.copy(alpha = 0.7f),
                    radius = (endDotDp / 2f).dp.toPx(),
                    center = Offset(size.width - pad - tHeight / 2f, midY)
                )
            }
        }

        // Вертикальный ползунок-ручка M3E (или круглый в классическом стиле)
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(thumbX - hWidth / 2f, midY - hHeight / 2f),
            size = Size(hWidth, hHeight),
            cornerRadius = CornerRadius(hWidth / 2f, hWidth / 2f)
        )
    }
}
