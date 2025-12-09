package com.beyondeye.openmptdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beyondeye.openmpt.core.PlaybackState
import com.beyondeye.openmptdemo.ui.theme.OpenMPTDemoTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenMPTDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ModPlayerScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(),
        label = "chevron_rotation"
    )
    
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Clickable header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationAngle)
                )
            }
            
            // Animated content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring()) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring()) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun ModPlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: ModPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val playbackState by viewModel.playbackState.collectAsState()
    val position by viewModel.position.collectAsState()
    val metadata by viewModel.metadata.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "OpenMPT Demo Player",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        // Load Module Files Section
        CollapsibleSection(
            title = "Load Module Files",
            initiallyExpanded = true
        ) {
            Button(
                onClick = {
                    viewModel.loadModuleFromAssets(context, "sm64_mainmenuss.xm")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Load Sample MOD File")
            }
        }
        
        // Track Information Section
        CollapsibleSection(
            title = "Track Information",
            initiallyExpanded = false
        ) {
            MetadataDisplay(metadata, playbackState)
        }
        
        // Playback info
        if (playbackState !is PlaybackState.Idle && playbackState !is PlaybackState.Loading) {
            Text(
                text = viewModel.getPlaybackInfo(),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
        
        // Position and seek bar
        PlaybackControls(
            playbackState = playbackState,
            position = position,
            duration = viewModel.getDuration(),
            onSeek = { viewModel.seek(it) },
            onPlayPause = { viewModel.togglePlayPause() },
            onStop = { viewModel.stop() }
        )
        
        // Speed and Pitch Controls Section
        CollapsibleSection(
            title = "Playback Settings",
            initiallyExpanded = false
        ) {
            SpeedPitchControls(
                viewModel = viewModel,
                enabled = playbackState !is PlaybackState.Idle && playbackState !is PlaybackState.Loading
            )
        }
        
        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun MetadataDisplay(
    metadata: com.beyondeye.openmpt.core.ModMetadata?,
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
                        Divider()
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

@Composable
fun SpeedPitchControls(
    viewModel: ModPlayerViewModel,
    enabled: Boolean
) {
    var autoLoop by rememberSaveable { mutableStateOf(false) }
    var playbackSpeed by rememberSaveable { mutableStateOf(1.0) }
    var pitch by rememberSaveable { mutableStateOf(1.0) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
            
            Divider()
            
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
                    Text(String.format("%.2fx", playbackSpeed))
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
                            Text(String.format("%.1fx", speed))
                        }
                    }
                }
            }
            
            Divider()
            
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
                    Text(String.format("%.2fx", pitch))
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
                            Text(String.format("%.1fx", p))
                        }
                    }
                }
            }
            
        // Reset button
        Button(
            onClick = {
                playbackSpeed = 1.0
                pitch = 1.0
                viewModel.setPlaybackSpeed(1.0)
                viewModel.setPitch(1.0)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && (playbackSpeed != 1.0 || pitch != 1.0)
        ) {
            Text("Reset to Defaults")
        }
    }
}

fun formatTime(seconds: Double): String {
    val totalSeconds = seconds.roundToInt()
    val minutes = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format("%d:%02d", minutes, secs)
}
