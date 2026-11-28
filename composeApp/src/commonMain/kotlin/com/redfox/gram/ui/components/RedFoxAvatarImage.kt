package com.redfox.gram.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun RedFoxLocalImage(
    filePath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
)
