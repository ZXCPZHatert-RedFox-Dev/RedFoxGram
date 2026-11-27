package com.redfox.gram.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class LaunchAnimationStyle(val title: String) {
    M3E_FLOWER_FOX("Цветок M3E + Красная Лиса"),
    EXPRESSIVE_BURST("Взрыв волны M3E"),
    GLASS_MORPH("Стеклянный морфинг"),
    NONE("Без заставки")
}

@Composable
fun RedFoxSplashOverlay(
    style: LaunchAnimationStyle = LaunchAnimationStyle.M3E_FLOWER_FOX,
    onFinished: () -> Unit = {}
) {
    if (style == LaunchAnimationStyle.NONE) {
        LaunchedEffect(Unit) { onFinished() }
        return
    }

    var visible by remember { mutableStateOf(true) }
    val scaleAnim = remember { Animatable(0.2f) }
    val rotationAnim = remember { Animatable(-25f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1.05f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        rotationAnim.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(550)
        visible = false
        delay(320)
        onFinished()
    }

    val flowerColor = MaterialTheme.colorScheme.primaryContainer
    val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val foxOrange = Color(0xFFE85D3F)
    val foxCream = Color(0xFFFFF3E8)
    val tgBlue = Color(0xFF2AABEE)

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(tween(300)) + scaleOut(targetScale = 1.35f, animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .scale(scaleAnim.value)
                        .rotate(rotationAnim.value),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val baseR = size.minDimension * 0.42f

                        if (style == LaunchAnimationStyle.EXPRESSIVE_BURST) {
                            drawCircle(
                                color = ringColor,
                                radius = baseR * 1.18f,
                                style = Stroke(width = 6.dp.toPx())
                            )
                        }

                        val petals = if (style == LaunchAnimationStyle.GLASS_MORPH) 8 else 12
                        val flowerPath = Path()
                        val steps = 180
                        for (i in 0..steps) {
                            val angle = (2.0 * PI * i) / steps
                            val wave = sin(angle * petals) * (baseR * 0.11f)
                            val r = baseR + wave.toFloat()
                            val x = cx + cos(angle).toFloat() * r
                            val y = cy + sin(angle).toFloat() * r
                            if (i == 0) flowerPath.moveTo(x, y) else flowerPath.lineTo(x, y)
                        }
                        flowerPath.close()
                        drawPath(path = flowerPath, color = flowerColor)

                        val earLeft = Path().apply {
                            moveTo(cx - 42.dp.toPx(), cy - 10.dp.toPx())
                            lineTo(cx - 32.dp.toPx(), cy - 54.dp.toPx())
                            lineTo(cx - 10.dp.toPx(), cy - 26.dp.toPx())
                            close()
                        }
                        val earRight = Path().apply {
                            moveTo(cx + 42.dp.toPx(), cy - 10.dp.toPx())
                            lineTo(cx + 32.dp.toPx(), cy - 54.dp.toPx())
                            lineTo(cx + 10.dp.toPx(), cy - 26.dp.toPx())
                            close()
                        }
                        drawPath(earLeft, foxOrange)
                        drawPath(earRight, foxOrange)

                        val facePath = Path().apply {
                            moveTo(cx - 46.dp.toPx(), cy - 12.dp.toPx())
                            lineTo(cx, cy - 32.dp.toPx())
                            lineTo(cx + 46.dp.toPx(), cy - 12.dp.toPx())
                            lineTo(cx, cy + 44.dp.toPx())
                            close()
                        }
                        drawPath(facePath, foxOrange)

                        val cheekLeft = Path().apply {
                            moveTo(cx - 46.dp.toPx(), cy - 12.dp.toPx())
                            lineTo(cx - 12.dp.toPx(), cy + 6.dp.toPx())
                            lineTo(cx, cy + 44.dp.toPx())
                            close()
                        }
                        val cheekRight = Path().apply {
                            moveTo(cx + 46.dp.toPx(), cy - 12.dp.toPx())
                            lineTo(cx + 12.dp.toPx(), cy + 6.dp.toPx())
                            lineTo(cx, cy + 44.dp.toPx())
                            close()
                        }
                        drawPath(cheekLeft, foxCream)
                        drawPath(cheekRight, foxCream)

                        drawCircle(color = Color(0xFF231917), radius = 5.5.dp.toPx(), center = Offset(cx, cy + 38.dp.toPx()))
                        drawCircle(color = Color(0xFF231917), radius = 4.dp.toPx(), center = Offset(cx - 17.dp.toPx(), cy - 2.dp.toPx()))
                        drawCircle(color = Color(0xFF231917), radius = 4.dp.toPx(), center = Offset(cx + 17.dp.toPx(), cy - 2.dp.toPx()))

                        val badgeCenter = Offset(cx + 46.dp.toPx(), cy + 42.dp.toPx())
                        drawCircle(color = tgBlue, radius = 20.dp.toPx(), center = badgeCenter)
                        val planePath = Path().apply {
                            moveTo(badgeCenter.x - 10.dp.toPx(), badgeCenter.y)
                            lineTo(badgeCenter.x + 9.dp.toPx(), badgeCenter.y - 8.dp.toPx())
                            lineTo(badgeCenter.x + 4.dp.toPx(), badgeCenter.y + 9.dp.toPx())
                            lineTo(badgeCenter.x - 2.dp.toPx(), badgeCenter.y + 3.dp.toPx())
                            close()
                        }
                        drawPath(planePath, Color.White)
                        drawLine(
                            color = tgBlue,
                            start = Offset(badgeCenter.x - 2.dp.toPx(), badgeCenter.y + 3.dp.toPx()),
                            end = Offset(badgeCenter.x + 6.dp.toPx(), badgeCenter.y - 5.dp.toPx()),
                            strokeWidth = 1.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "RedFoxGram M3E",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Android 17 Expressive Edition",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
