package com.beyondeye.openmptdemo

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beyondeye.openmptdemo.player.AndroidModPlayer
import com.beyondeye.openmptdemo.player.ModMetadata
import com.beyondeye.openmptdemo.player.ModPlayer
import com.beyondeye.openmptdemo.player.PlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * ViewModel for the MOD player UI.
 * Manages the player state and provides UI actions.
 */
class ModPlayerViewModel : ViewModel() {
    
    private val player: ModPlayer = AndroidModPlayer()
    
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
        Log.d(TAG, "ModPlayerViewModel created")
    }
    
    // ========== File Loading ==========
    
    /**
     * Load a MOD file from a Uri (e.g., file picker result)
     */
    fun loadModuleFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val data = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes()
                    } ?: throw Exception("Failed to open file")
                }
                
                val success = withContext(Dispatchers.Default) {
                    player.loadModule(data)
                }
                
                if (success) {
                    _metadata.value = player.getMetadata()
                    Log.i(TAG, "Module loaded successfully from URI")
                } else {
                    Log.e(TAG, "Failed to load module from URI")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading module from URI", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load a MOD file from assets folder
     */
    fun loadModuleFromAssets(context: Context, assetPath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val data = withContext(Dispatchers.IO) {
                    context.assets.open(assetPath).use { input ->
                        input.readBytes()
                    }
                }
                
                val success = withContext(Dispatchers.Default) {
                    player.loadModule(data)
                }
                
                if (success) {
                    _metadata.value = player.getMetadata()
                    Log.i(TAG, "Module loaded successfully from assets: $assetPath")
                } else {
                    Log.e(TAG, "Failed to load module from assets: $assetPath")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading module from assets", e)
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
                val success = withContext(Dispatchers.Default) {
                    player.loadModuleFromPath(path)
                }
                
                if (success) {
                    _metadata.value = player.getMetadata()
                    Log.i(TAG, "Module loaded successfully from path: $path")
                } else {
                    Log.e(TAG, "Failed to load module from path: $path")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading module from path", e)
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
            Log.e(TAG, "Error playing", e)
        }
    }
    
    fun pause() {
        try {
            player.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing", e)
        }
    }
    
    fun stop() {
        try {
            player.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping", e)
        }
    }
    
    fun togglePlayPause() {
        when (playbackState.value) {
            is PlaybackState.Playing -> pause()
            is PlaybackState.Loaded,
            is PlaybackState.Paused,
            is PlaybackState.Stopped -> play()
            else -> Log.w(TAG, "Cannot toggle play/pause in current state")
        }
    }
    
    fun seek(positionSeconds: Double) {
        try {
            player.seek(positionSeconds)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking", e)
        }
    }
    
    // ========== Configuration ==========
    
    fun setRepeatMode(isRepeat: Boolean) {
        player.setRepeatCount(if (isRepeat) -1 else 0)
    }
    
    fun setAutoLoop(enabled: Boolean) {
        player.setRepeatCount(if (enabled) -1 else 0)
        Log.d(TAG, "Auto-loop ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun setPlaybackSpeed(speed: Double) {
        player.setPlaybackSpeed(speed.coerceIn(0.25, 2.0))
        Log.d(TAG, "Playback speed set to $speed")
    }
    
    fun getPlaybackSpeed(): Double {
        return player.getPlaybackSpeed()
    }
    
    fun setPitch(pitch: Double) {
        player.setPitch(pitch.coerceIn(0.25, 2.0))
        Log.d(TAG, "Pitch set to $pitch")
    }
    
    fun getPitch(): Double {
        return player.getPitch()
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
        super.onCleared()
        Log.d(TAG, "ModPlayerViewModel cleared, releasing player")
        player.release()
    }
    
    companion object {
        private const val TAG = "ModPlayerViewModel"
    }
}
