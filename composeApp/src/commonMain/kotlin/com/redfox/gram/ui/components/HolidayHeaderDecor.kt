package com.redfox.gram.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun HolidayHeaderOverlay(
    snowAlways: Boolean,
    santaHatAlways: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "header_snow")
    val snowPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow_fall"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (snowAlways) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val flakes = listOf(0.08f, 0.22f, 0.37f, 0.52f, 0.68f, 0.81f, 0.93f)
                flakes.forEachIndexed { idx, xRatio ->
                    val y = ((snowPhase + idx * 0.14f) % 1f) * size.height
                    drawCircle(
                        color = Color.White.copy(alpha = 0.65f),
                        radius = (1.8f + (idx % 3)).dp.toPx(),
                        center = Offset(size.width * xRatio, y)
                    )
                }
            }
        }

        Box {
            content()
            if (santaHatAlways) {
                Canvas(
                    modifier = Modifier
                        .size(22.dp)
                        .offset(x = (-8).dp, y = (-8).dp)
                ) {
                    val hatPath = Path().apply {
                        moveTo(size.width * 0.15f, size.height * 0.78f)
                        lineTo(size.width * 0.82f, size.height * 0.78f)
                        lineTo(size.width * 0.42f, size.height * 0.18f)
                        close()
                    }
                    drawPath(hatPath, color = Color(0xFFE53935))
                    drawCircle(
                        color = Color.White,
                        radius = 3.2.dp.toPx(),
                        center = Offset(size.width * 0.42f, size.height * 0.16f)
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(size.width * 0.12f, size.height * 0.80f),
                        end = Offset(size.width * 0.85f, size.height * 0.80f),
                        strokeWidth = 4.dp.toPx()
                    )
                }
            }
        }
    }
}
