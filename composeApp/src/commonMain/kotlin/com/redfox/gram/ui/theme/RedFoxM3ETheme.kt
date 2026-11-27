package com.redfox.gram.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Выразительная палитра Material 3 Expressive (RedFox Edition)
private val RedFoxExpressiveDark = darkColorScheme(
    primary = Color(0xFFFFB4A8),
    onPrimary = Color(0xFF561E16),
    primaryContainer = Color(0xFF73342A),
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = Color(0xFFE7BDB6),
    onSecondary = Color(0xFF442925),
    secondaryContainer = Color(0xFF5D3F3B),
    onSecondaryContainer = Color(0xFFFFDAD4),
    tertiary = Color(0xFFE0C38C),
    onTertiary = Color(0xFF3F2E04),
    tertiaryContainer = Color(0xFF584419),
    onTertiaryContainer = Color(0xFFFDE0A6),
    surface = Color(0xFF1A1110),
    surfaceContainerLow = Color(0xFF231918),
    surfaceContainerHigh = Color(0xFF322726),
    surfaceContainerHighest = Color(0xFF3E3230)
)

// Крупные тактильные скругления M3E
private val ExpressiveShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun RedFoxM3ETheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RedFoxExpressiveDark,
        shapes = ExpressiveShapes,
        content = content
    )
}
