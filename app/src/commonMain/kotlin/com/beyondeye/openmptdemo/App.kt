package com.beyondeye.openmptdemo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beyondeye.openmpt.core.PlaybackState
import com.beyondeye.openmptdemo.resources.Res
import com.beyondeye.openmptdemo.ui.theme.OpenMPTDemoTheme
import com.beyondeye.openmptdemo.uicomponents.CollapsibleSection
import com.beyondeye.openmptdemo.uicomponents.MetadataDisplay
import com.beyondeye.openmptdemo.uicomponents.ModuleFileLoader
import com.beyondeye.openmptdemo.uicomponents.PlaybackControls
import com.beyondeye.openmptdemo.uicomponents.PlaybackSettings
import de.halfbit.logger.e
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main application composable for the libOpenMPT Demo player.
 * This is the entry point for the Compose Multiplatform UI.
 */
@Composable
fun App() {
    OpenMPTDemoTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            ModPlayerScreen(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ModPlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: ModPlayerViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
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
            text = "LibOpenMPT Demo Player",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        // Load Module Files Section
        CollapsibleSection(
            title = "Load Module Files",
            initiallyExpanded = true
        ) {
            ModuleFileLoader(
                onLoadSampleFile = {
                    scope.launch {
                        try {
                            val bytes = Res.readBytes("files/sm64_mainmenuss.xm")
                            viewModel.loadModuleAsync(bytes)
                        } catch (e: Exception) {
                            e("modplayer") { "error loading sample mod file:" }
                        }
                    }
                },
                onLoadFileBytes = { bytes ->
                    viewModel.loadModuleAsync(bytes)
                },
                isLoading = isLoading
            )
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
            metadata?.title,
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
            PlaybackSettings(
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
