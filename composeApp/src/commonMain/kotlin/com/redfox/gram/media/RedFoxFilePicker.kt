package com.redfox.gram.media

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFilePickerLauncher(onFilePicked: (filePath: String, isImage: Boolean) -> Unit): () -> Unit
