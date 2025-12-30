package com.beyondeye.openmptdemo.uicomponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beyondeye.openmptdemo.ModPlayerViewModel
import com.beyondeye.openmptdemo.timeutils.formatDecimal

@Composable
fun PlaybackSettings(
    viewModel: ModPlayerViewModel,
    enabled: Boolean
) {
    var masterGain by rememberSaveable { mutableStateOf(0.0) }
    var autoLoop by rememberSaveable { mutableStateOf(false) }
    var playbackSpeed by rememberSaveable { mutableStateOf(1.0) }
    var pitch by rememberSaveable { mutableStateOf(1.0) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Master Gain Control
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Master Gain")
                Text(
                    if (masterGain >= 0) "+${formatDecimal(masterGain, 1)} dB"
                    else "${formatDecimal(masterGain, 1)} dB"
                )
            }

            Slider(
                value = masterGain.toFloat(),
                onValueChange = {
                    masterGain = it.toDouble()
                    viewModel.setMasterGain(masterGain)
                },
                valueRange = -10f..10f,
                enabled = enabled
            )

            // Master Gain preset button (reset to 0 dB)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = {
                        masterGain = 0.0
                        viewModel.setMasterGain(0.0)
                    },
                    enabled = enabled
                ) {
                    Text("0 dB")
                }
            }
        }

        HorizontalDivider()

        // Auto-loop toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Auto-Loop")
            Switch(
                checked = autoLoop,
                onCheckedChange = {
                    autoLoop = it
                    viewModel.setAutoLoop(it)
                },
                enabled = enabled
            )
        }

        HorizontalDivider()

        // Playback Speed Control
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Speed")
                Text(formatDecimal(playbackSpeed, 2) + "x")
            }

            Slider(
                value = playbackSpeed.toFloat(),
                onValueChange = {
                    playbackSpeed = it.toDouble()
                    viewModel.setPlaybackSpeed(playbackSpeed)
                },
                valueRange = 0.25f..2.0f,
                enabled = enabled
            )

            // Speed preset buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(0.5, 1.0, 1.5, 2.0).forEach { speed ->
                    OutlinedButton(
                        onClick = {
                            playbackSpeed = speed
                            viewModel.setPlaybackSpeed(speed)
                        },
                        enabled = enabled,
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    ) {
                        Text(formatDecimal(speed, 1) + "x")
                    }
                }
            }
        }

        HorizontalDivider()

        // Pitch Control
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pitch")
                Text(formatDecimal(pitch, 2) + "x")
            }

            Slider(
                value = pitch.toFloat(),
                onValueChange = {
                    pitch = it.toDouble()
                    viewModel.setPitch(pitch)
                },
                valueRange = 0.25f..2.0f,
                enabled = enabled
            )

            // Pitch preset buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(0.5, 1.0, 1.5, 2.0).forEach { p ->
                    OutlinedButton(
                        onClick = {
                            pitch = p
                            viewModel.setPitch(p)
                        },
                        enabled = enabled,
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    ) {
                        Text(formatDecimal(p, 1) + "x")
                    }
                }
            }
        }

        // Reset button
        Button(
            onClick = {
                masterGain = 0.0
                playbackSpeed = 1.0
                pitch = 1.0
                viewModel.setMasterGain(0.0)
                viewModel.setPlaybackSpeed(1.0)
                viewModel.setPitch(1.0)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && (masterGain != 0.0 || playbackSpeed != 1.0 || pitch != 1.0)
        ) {
            Text("Reset to Defaults")
        }
    }
}
