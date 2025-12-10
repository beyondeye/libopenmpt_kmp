package com.beyondeye.openmptdemo.uicomponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beyondeye.openmpt.core.ModMetadata
import com.beyondeye.openmpt.core.PlaybackState
import com.beyondeye.openmptdemo.timeutils.formatTime

@Composable
fun MetadataDisplay(
    metadata: ModMetadata?,
    playbackState: PlaybackState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (playbackState) {
                is PlaybackState.Idle -> {
                    Text(
                        text = "No module loaded",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is PlaybackState.Loading -> {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is PlaybackState.Error -> {
                    Text(
                        text = "Error: ${playbackState.message}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                else -> {
                    metadata?.let { meta ->
                        Text(
                            text = meta.title.ifEmpty { "Unknown Title" },
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (meta.artist.isNotEmpty()) {
                            Text(
                                text = "by ${meta.artist}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        HorizontalDivider()
                        Text("Format: ${meta.typeLong.ifEmpty { meta.type }}")
                        if (meta.tracker.isNotEmpty()) {
                            Text("Tracker: ${meta.tracker}")
                        }
                        Text("Channels: ${meta.numChannels}")
                        Text("Patterns: ${meta.numPatterns}")
                        Text("Instruments: ${meta.numInstruments}")
                        Text("Samples: ${meta.numSamples}")
                        Text("Duration: ${formatTime(meta.durationSeconds)}")
                    }
                }
            }
        }
    }
}