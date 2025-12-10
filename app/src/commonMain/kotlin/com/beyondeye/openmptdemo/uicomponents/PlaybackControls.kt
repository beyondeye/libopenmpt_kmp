package com.beyondeye.openmptdemo.uicomponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beyondeye.openmpt.core.PlaybackState
import com.beyondeye.openmptdemo.timeutils.formatTime

@Composable
fun PlaybackControls(
    playbackState: PlaybackState,
    position: Double,
    duration: Double,
    onSeek: (Double) -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Position display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(position),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Seek bar
        Slider(
            value = position.toFloat(),
            onValueChange = { onSeek(it.toDouble()) },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            enabled = playbackState !is PlaybackState.Idle &&
                    playbackState !is PlaybackState.Loading &&
                    duration > 0
        )

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stop button
            IconButton(
                onClick = onStop,
                enabled = playbackState is PlaybackState.Playing ||
                        playbackState is PlaybackState.Paused
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Play/Pause button
            FilledIconButton(
                onClick = onPlayPause,
                enabled = playbackState !is PlaybackState.Idle &&
                        playbackState !is PlaybackState.Loading &&
                        playbackState !is PlaybackState.Error,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = when (playbackState) {
                        is PlaybackState.Playing -> Icons.Default.Pause
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = if (playbackState is PlaybackState.Playing) "Pause" else "Play",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}