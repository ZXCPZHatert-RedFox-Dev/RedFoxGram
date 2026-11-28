package com.redfox.gram.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.redfox.gram.media.RedFoxAudioController

@Composable
fun LiveLrcMiniPlayer(
    trackTitle: String,
    artist: String,
    currentLine: String,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onOpenFullPlayer: () -> Unit
) {
    val activeTrack by RedFoxAudioController.track.collectAsState()
    if (!activeTrack.isActive) return

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onOpenFullPlayer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (activeTrack.albumArt != null) {
                    Image(
                        bitmap = activeTrack.albumArt!!,
                        contentDescription = activeTrack.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("♫", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "♪ ${activeTrack.currentLyricLine}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${activeTrack.artist} — ${activeTrack.title}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { RedFoxAudioController.togglePlayPause() }) {
                if (activeTrack.isPlaying) {
                    Text("❚❚", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Играть", tint = MaterialTheme.colorScheme.primary)
                }
            }

            IconButton(onClick = { RedFoxAudioController.closePlayer() }) {
                Icon(Icons.Rounded.Close, contentDescription = "Закрыть", tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
            }
        }
    }
}
