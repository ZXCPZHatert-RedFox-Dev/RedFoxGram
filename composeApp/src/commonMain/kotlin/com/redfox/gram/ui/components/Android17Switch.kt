package com.redfox.gram.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun Android17Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val dpSpringSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "trackColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "borderColor"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
        label = "thumbColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "iconColor"
    )

    // В Android 17 M3E включенный бегунок крупнее (24.dp), выключенный компактнее (18.dp)
    val thumbSize by animateDpAsState(
        targetValue = if (checked) 24.dp else 18.dp,
        animationSpec = dpSpringSpec,
        label = "thumbSize"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 6.dp,
        animationSpec = dpSpringSpec,
        label = "thumbOffset"
    )
    val checkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = springSpec,
        label = "checkProgress"
    )

    Box(
        modifier = modifier
            .width(52.dp)
            .height(32.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(width = 2.dp, color = borderColor, shape = CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(14.dp)) {
                val w = size.width
                val h = size.height
                val stroke = 2.2.dp.toPx()

                if (checkProgress >= 0.5f) {
                    // Рисуем галочку (✓) как в Android 17 Settings
                    val alpha = ((checkProgress - 0.5f) * 2f).coerceIn(0f, 1f)
                    val p1 = Offset(w * 0.18f, h * 0.52f)
                    val p2 = Offset(w * 0.42f, h * 0.76f)
                    val p3 = Offset(w * 0.84f, h * 0.26f)

                    drawLine(
                        color = iconColor.copy(alpha = alpha),
                        start = p1,
                        end = p2,
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = iconColor.copy(alpha = alpha),
                        start = p2,
                        end = p3,
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                } else {
                    // Рисуем крестик (✕) как в выключенном Switch Android 17
                    val alpha = ((0.5f - checkProgress) * 2f).coerceIn(0f, 1f)
                    val pad = w * 0.24f
                    drawLine(
                        color = iconColor.copy(alpha = alpha),
                        start = Offset(pad, pad),
                        end = Offset(w - pad, h - pad),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = iconColor.copy(alpha = alpha),
                        start = Offset(w - pad, pad),
                        end = Offset(pad, h - pad),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
