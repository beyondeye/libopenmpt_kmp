package com.beyondeye.openmptdemo.player

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Android implementation of the ModPlayer interface.
 * Uses JNI to communicate with the native ModPlayerEngine.
 */
class AndroidModPlayer : ModPlayer {
    
    private val native: ModPlayerNative = ModPlayerNative()
    private val handle: Long get() = native.getHandle()
    
    // State flows
    private val _playbackStateFlow = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackStateFlow: StateFlow<PlaybackState> = _playbackStateFlow.asStateFlow()
    
    private val _positionFlow = MutableStateFlow(0.0)
    override val positionFlow: StateFlow<Double> = _positionFlow.asStateFlow()
    
    // Position update job
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var positionUpdateJob: Job? = null
    
    // ========== Lifecycle ==========
    
    override fun loadModule(data: ByteArray): Boolean {
        Log.d(TAG, "Loading module from byte array (${data.size} bytes)")
        _playbackStateFlow.value = PlaybackState.Loading
        
        return try {
            val success = native.nativeLoadModule(handle, data)
            if (success) {
                val metadata = getMetadata()
                _playbackStateFlow.value = PlaybackState.Loaded(metadata)
                Log.i(TAG, "Module loaded: ${metadata.title}")
            } else {
                _playbackStateFlow.value = PlaybackState.Error("Failed to load module")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error loading module", e)
            _playbackStateFlow.value = PlaybackState.Error("Error loading module: ${e.message}", e)
            false
        }
    }
    
    override fun loadModuleFromPath(path: String): Boolean {
        Log.d(TAG, "Loading module from path: $path")
        _playbackStateFlow.value = PlaybackState.Loading
        
        return try {
            val success = native.nativeLoadModuleFromPath(handle, path)
            if (success) {
                val metadata = getMetadata()
                _playbackStateFlow.value = PlaybackState.Loaded(metadata)
                Log.i(TAG, "Module loaded: ${metadata.title}")
            } else {
                _playbackStateFlow.value = PlaybackState.Error("Failed to load module from path")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error loading module from path", e)
            _playbackStateFlow.value = PlaybackState.Error("Error loading module: ${e.message}", e)
            false
        }
    }
    
    override fun release() {
        Log.d(TAG, "Releasing player")
        stop()
        positionUpdateJob?.cancel()
        native.nativeUnloadModule(handle)
        native.release()
        _playbackStateFlow.value = PlaybackState.Idle
    }
    
    // ========== Playback Control ==========
    
    override fun play() {
        Log.d(TAG, "Play requested")
        if (playbackState !is PlaybackState.Loaded && 
            playbackState !is PlaybackState.Paused && 
            playbackState !is PlaybackState.Stopped) {
            throw ModPlayerException.InvalidOperation("Cannot play: no module loaded or invalid state")
        }
        
        val success = native.nativePlay(handle)
        if (success) {
            _playbackStateFlow.value = PlaybackState.Playing
            startPositionUpdates()
        } else {
            _playbackStateFlow.value = PlaybackState.Error("Failed to start playback")
        }
    }
    
    override fun pause() {
        Log.d(TAG, "Pause requested")
        if (playbackState !is PlaybackState.Playing) {
            Log.w(TAG, "Cannot pause: not currently playing")
            return
        }
        
        native.nativePause(handle)
        _playbackStateFlow.value = PlaybackState.Paused
        stopPositionUpdates()
    }
    
    override fun stop() {
        Log.d(TAG, "Stop requested")
        native.nativeStop(handle)
        _playbackStateFlow.value = PlaybackState.Stopped
        _positionFlow.value = 0.0
        stopPositionUpdates()
    }
    
    override fun seek(positionSeconds: Double) {
        Log.d(TAG, "Seek to $positionSeconds seconds")
        native.nativeSeek(handle, positionSeconds)
        _positionFlow.value = positionSeconds
    }
    
    // ========== Configuration ==========
    
    override fun setRepeatCount(count: Int) {
        Log.d(TAG, "Setting repeat count to $count")
        native.nativeSetRepeatCount(handle, count)
    }
    
    override fun setMasterGain(gainMillibel: Int) {
        Log.d(TAG, "Setting master gain to $gainMillibel mB")
        native.nativeSetMasterGain(handle, gainMillibel)
    }
    
    override fun setStereoSeparation(percent: Int) {
        Log.d(TAG, "Setting stereo separation to $percent%")
        native.nativeSetStereoSeparation(handle, percent)
    }
    
    override fun setPlaybackSpeed(speed: Double) {
        Log.d(TAG, "Setting playback speed to $speed")
        native.nativeSetTempoFactor(handle, speed)
    }
    
    override fun getPlaybackSpeed(): Double {
        return native.nativeGetTempoFactor(handle)
    }
    
    override fun setPitch(pitch: Double) {
        Log.d(TAG, "Setting pitch to $pitch")
        native.nativeSetPitchFactor(handle, pitch)
    }
    
    override fun getPitch(): Double {
        return native.nativeGetPitchFactor(handle)
    }
    
    // ========== State Queries ==========
    
    override val playbackState: PlaybackState
        get() = _playbackStateFlow.value
    
    override val isPlaying: Boolean
        get() = native.nativeIsPlaying(handle)
    
    override val positionSeconds: Double
        get() = native.nativeGetPositionSeconds(handle)
    
    override val durationSeconds: Double
        get() = native.nativeGetDurationSeconds(handle)
    
    // ========== Module Information ==========
    
    override fun getMetadata(): ModMetadata {
        return ModMetadata(
            title = native.nativeGetMetadata(handle, "title"),
            artist = native.nativeGetMetadata(handle, "artist"),
            type = native.nativeGetMetadata(handle, "type"),
            typeLong = native.nativeGetMetadata(handle, "type_long"),
            tracker = native.nativeGetMetadata(handle, "tracker"),
            message = native.nativeGetMetadata(handle, "message"),
            durationSeconds = durationSeconds,
            numChannels = native.nativeGetNumChannels(handle),
            numPatterns = native.nativeGetNumPatterns(handle),
            numOrders = native.nativeGetNumOrders(handle),
            numInstruments = native.nativeGetNumInstruments(handle),
            numSamples = native.nativeGetNumSamples(handle)
        )
    }
    
    override fun getCurrentOrder(): Int {
        return native.nativeGetCurrentOrder(handle)
    }
    
    override fun getCurrentPattern(): Int {
        return native.nativeGetCurrentPattern(handle)
    }
    
    override fun getCurrentRow(): Int {
        return native.nativeGetCurrentRow(handle)
    }
    
    override fun getNumChannels(): Int {
        return native.nativeGetNumChannels(handle)
    }
    
    // ========== Private Methods ==========
    
    private fun startPositionUpdates() {
        stopPositionUpdates()
        
        positionUpdateJob = scope.launch {
            while (isActive && isPlaying) {
                val position = native.nativeGetPositionSeconds(handle)
                _positionFlow.value = position
                
                // Check if playback has ended
                if (!native.nativeIsPlaying(handle) && playbackState is PlaybackState.Playing) {
                    _playbackStateFlow.value = PlaybackState.Stopped
                    _positionFlow.value = 0.0
                    break
                }
                
                delay(100) // Update position 10 times per second
            }
        }
    }
    
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
    
    companion object {
        private const val TAG = "AndroidModPlayer"
    }
}
