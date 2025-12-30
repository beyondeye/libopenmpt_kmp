package com.beyondeye.openmptdemo

import androidx.lifecycle.ViewModel
import com.beyondeye.openmpt.core.ModMetadata
import com.beyondeye.openmpt.core.ModPlayer
import com.beyondeye.openmpt.core.PlaybackState
import de.halfbit.logger.d
import de.halfbit.logger.e
import de.halfbit.logger.i
import de.halfbit.logger.w
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the MOD player UI.
 * Manages the player state and provides UI actions.
 * 
 * This is a multiplatform ViewModel that works on Android, iOS, Desktop, and Web.
 */
class ModPlayerViewModel(
    private val player: ModPlayer
) : ViewModel() {
    
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Observe player state
    val playbackState: StateFlow<PlaybackState> = player.playbackStateFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PlaybackState.Idle
    )
    
    val position: StateFlow<Double> = player.positionFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0.0
    )
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Current metadata
    private val _metadata = MutableStateFlow<ModMetadata?>(null)
    val metadata: StateFlow<ModMetadata?> = _metadata.asStateFlow()
    
    init {
        d(TAG) { "ModPlayerViewModel created" }
    }
    
    // ========== File Loading ==========
    
    /**
     * Load a MOD file from a byte array.
     * Uses the suspend version of loadModule which handles async initialization
     * properly on platforms that require it (like wasmJS).
     */
    fun loadModuleAsync(data: ByteArray) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Use loadModuleSuspend for proper async initialization support
                // (especially important for wasmJS where libopenmpt needs to be loaded dynamically)
                val success = player.loadModuleSuspend(data)
                
                if (success) {
                    _metadata.value = player.getMetadata()
                    i(TAG) { "Module loaded successfully" }
                } else {
                    e(TAG) { "Failed to load module" }
                }
            } catch (e: Exception) {
                e(TAG) { "Error loading module: ${e.message}" }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load a MOD file from a file path
     */
    fun loadModuleFromPath(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val success = player.loadModuleFromPath(path)
                
                if (success) {
                    _metadata.value = player.getMetadata()
                    i(TAG) { "Module loaded successfully from path: $path" }
                } else {
                    e(TAG) { "Failed to load module from path: $path" }
                }
            } catch (e: Exception) {
                e(TAG) { "Error loading module from path: ${e.message}" }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // ========== Playback Control ==========
    
    fun play() {
        try {
            player.play()
        } catch (e: Exception) {
            e(TAG) { "Error playing: ${e.message}" }
        }
    }
    
    fun pause() {
        try {
            player.pause()
        } catch (e: Exception) {
            e(TAG) { "Error pausing: ${e.message}" }
        }
    }
    
    fun stop() {
        try {
            player.stop()
        } catch (e: Exception) {
            e(TAG) { "Error stopping: ${e.message}" }
        }
    }
    
    fun togglePlayPause() {
        when (playbackState.value) {
            is PlaybackState.Playing -> pause()
            is PlaybackState.Loaded,
            is PlaybackState.Paused,
            is PlaybackState.Stopped -> play()
            else -> w(TAG) { "Cannot toggle play/pause in current state" }
        }
    }
    
    fun seek(positionSeconds: Double) {
        try {
            player.seek(positionSeconds)
        } catch (e: Exception) {
            e(TAG) { "Error seeking: ${e.message}" }
        }
    }
    
    // ========== Configuration ==========
    
    fun setRepeatMode(isRepeat: Boolean) {
        player.setRepeatCount(if (isRepeat) -1 else 0)
    }
    
    fun setAutoLoop(enabled: Boolean) {
        player.setRepeatCount(if (enabled) -1 else 0)
        d(TAG) { "Auto-loop ${if (enabled) "enabled" else "disabled"}" }
    }
    
    fun setPlaybackSpeed(speed: Double) {
        player.setPlaybackSpeed(speed.coerceIn(0.25, 2.0))
        d(TAG) { "Playback speed set to $speed" }
    }
    
    fun getPlaybackSpeed(): Double {
        return player.getPlaybackSpeed()
    }
    
    fun setPitch(pitch: Double) {
        player.setPitch(pitch.coerceIn(0.25, 2.0))
        d(TAG) { "Pitch set to $pitch" }
    }
    
    fun getPitch(): Double {
        return player.getPitch()
    }
    
    fun setMasterGain(gainDb: Double) {
        // Convert dB to millibels (1 dB = 100 mB)
        val gainMillibel = (gainDb.coerceIn(-10.0, 10.0) * 100).toInt()
        player.setMasterGain(gainMillibel)
        d(TAG) { "Master gain set to $gainDb dB ($gainMillibel mB)" }
    }
    
    // ========== Queries ==========
    
    fun getDuration(): Double = player.durationSeconds
    
    fun getCurrentPosition(): Double = player.positionSeconds
    
    fun getPlaybackInfo(): String {
        if (playbackState.value is PlaybackState.Idle || playbackState.value is PlaybackState.Loading) {
            return "No module loaded"
        }
        
        val order = player.getCurrentOrder()
        val pattern = player.getCurrentPattern()
        val row = player.getCurrentRow()
        
        return "Order: $order | Pattern: $pattern | Row: $row"
    }
    
    // ========== Lifecycle ==========
    
    override fun onCleared() {
        d(TAG) { "ModPlayerViewModel cleared, releasing player" }
        super.onCleared()
        viewModelScope.cancel()
        player.release()
    }
    
    companion object {
        private const val TAG = "ModPlayerViewModel"
    }
}
