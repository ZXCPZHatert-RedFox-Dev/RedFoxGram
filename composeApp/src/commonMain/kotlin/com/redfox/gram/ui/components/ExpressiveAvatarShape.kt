package com.redfox.gram.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class ExpressiveAvatarStyle(val label: String) {
    SQUIRCLE("Сквиркл"),
    FLOWER_12("Цветок M3E"),
    STAR_8("Звезда M3E"),
    CIRCLE("Круг")
}

fun buildExpressiveAvatarShape(style: ExpressiveAvatarStyle): Shape {
    return when (style) {
        ExpressiveAvatarStyle.CIRCLE -> CircleShape
        ExpressiveAvatarStyle.SQUIRCLE -> RoundedCornerShape(18.dp)
        ExpressiveAvatarStyle.FLOWER_12 -> GenericShape { size, _ ->
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseR = size.minDimension * 0.44f
            val steps = 120
            for (i in 0..steps) {
                val angle = (2.0 * PI * i) / steps
                val wave = sin(angle * 12.0) * (baseR * 0.11f)
                val r = baseR + wave.toFloat()
                val x = cx + cos(angle).toFloat() * r
                val y = cy + sin(angle).toFloat() * r
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        ExpressiveAvatarStyle.STAR_8 -> GenericShape { size, _ ->
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseR = size.minDimension * 0.43f
            val steps = 120
            for (i in 0..steps) {
                val angle = (2.0 * PI * i) / steps
                val wave = sin(angle * 8.0) * (baseR * 0.14f)
                val r = baseR + wave.toFloat()
                val x = cx + cos(angle).toFloat() * r
                val y = cy + sin(angle).toFloat() * r
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }
}
