package com.redfox.gram.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class ParticleSeed(
    val xRatio: Float,
    val yOffset: Float,
    val radius: Float,
    val speed: Float,
    val sway: Float
)

@Composable
fun ChatAmbientParticles(
    effectType: String, // "NONE", "SNOW", "SAKURA", "STARS"
    modifier: Modifier = Modifier
) {
    if (effectType == "NONE" || effectType.isBlank()) return

    val seeds = remember(effectType) {
        val rnd = Random(effectType.hashCode())
        List(28) {
            ParticleSeed(
                xRatio = rnd.nextFloat(),
                yOffset = rnd.nextFloat(),
                radius = 2.5f + rnd.nextFloat() * 4.5f,
                speed = 0.6f + rnd.nextFloat() * 0.8f,
                sway = 12f + rnd.nextFloat() * 24f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "chat_particles")
    val tick by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_tick"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val particleColor = when (effectType) {
        "SNOW" -> Color.White.copy(alpha = 0.35f)
        "SAKURA" -> Color(0xFFFF80AB).copy(alpha = 0.38f)
        else -> primaryColor.copy(alpha = 0.35f)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        seeds.forEach { p ->
            val progressY = (p.yOffset + tick * p.speed) % 1f
            val y = progressY * h
            val angle = (progressY * 2.0 * PI * 2.0).toFloat()
            val x = (p.xRatio * w + sin(angle) * p.sway).coerceIn(0f, w)
            drawCircle(
                color = particleColor,
                radius = p.radius,
                center = Offset(x, y)
            )
        }
    }
}
