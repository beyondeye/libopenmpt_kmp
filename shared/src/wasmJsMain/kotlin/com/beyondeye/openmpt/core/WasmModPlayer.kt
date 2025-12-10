package com.beyondeye.openmpt.core

import de.halfbit.logger.d
import de.halfbit.logger.e
import de.halfbit.logger.w
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
 * Wasm/JS implementation of the ModPlayer interface.
 * Uses libopenmpt compiled to WASM + Web Audio API for playback.
 */
class WasmModPlayer : ModPlayer {
    companion object {
        const val LOGTAG="WasmModPlayer"
    }
    
    // libopenmpt module handle
    private var moduleHandle: Int = 0
    
    // Web Audio player
    private val audioPlayer = WebAudioPlayer()
    
    // State flows
    private val _playbackStateFlow = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackStateFlow: StateFlow<PlaybackState> = _playbackStateFlow.asStateFlow()
    
    private val _positionFlow = MutableStateFlow(0.0)
    override val positionFlow: StateFlow<Double> = _positionFlow.asStateFlow()
    
    // Position update job
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var positionUpdateJob: Job? = null
    
    init {
        // Set up audio render callback
        audioPlayer.setRenderCallback { outputLeft, outputRight, frameCount ->
            renderAudio(outputLeft, outputRight, frameCount)
        }
    }
    
    // ========== Lifecycle ==========
    
    /**
     * Suspend version of loadModule that handles libopenmpt initialization automatically.
     * This is the recommended way to load modules in wasmJS as it ensures the library
     * is properly initialized before attempting to load.
     * 
     * @param data The module file data as a ByteArray
     * @return true if the module was loaded successfully, false otherwise
     */
    override suspend fun loadModuleSuspend(data: ByteArray): Boolean {
        d(LOGTAG) { "Loading module from byte array (${data.size} bytes) with initialization" }
        _playbackStateFlow.value = PlaybackState.Loading
        
        return try {
            // Initialize libopenmpt if not already initialized
            if (!LibOpenMpt.isReady()) {
                d(LOGTAG) { "libopenmpt not ready, initializing..." }
                val initialized = LibOpenMpt.initializeLibOpenMpt()
                if (!initialized) {
                    e(LOGTAG) { "Failed to initialize libopenmpt" }
                    _playbackStateFlow.value = PlaybackState.Error("Failed to initialize libopenmpt")
                    return false
                }
                d(LOGTAG) { "libopenmpt initialized successfully" }
            }
            
            // Now proceed with loading the module
            loadModuleInternal(data)
        } catch (e: Throwable) {
            e(LOGTAG) { "Error loading module: ${e.message}" }
            _playbackStateFlow.value = PlaybackState.Error("Error loading module: ${e.message}", e)
            false
        }
    }
    
    override fun loadModule(data: ByteArray): Boolean {
        d(LOGTAG) { "Loading module from byte array (${data.size} bytes)" }
        _playbackStateFlow.value = PlaybackState.Loading
        
        return try {
            // Check if libopenmpt is ready
            if (!LibOpenMpt.isReady()) {
                e(LOGTAG){"libopenmpt is not ready. Call loadModuleSuspend() instead for automatic initialization."}
                _playbackStateFlow.value = PlaybackState.Error("libopenmpt is not ready. Use loadModuleSuspend() for automatic initialization.")
                return false
            }
            
            loadModuleInternal(data)
        } catch (e: Throwable) {
            e(LOGTAG){"Error loading module: ${e.message}"}
            _playbackStateFlow.value = PlaybackState.Error("Error loading module: ${e.message}", e)
            false
        }
    }
    
    /**
     * Internal module loading logic shared by loadModule and loadModuleSuspend.
     */
    private fun loadModuleInternal(data: ByteArray): Boolean {
        // Unload any existing module
        if (moduleHandle != 0) {
            LibOpenMpt.destroyModule(moduleHandle)
            moduleHandle = 0
        }
        
        // Create new module
        moduleHandle = LibOpenMpt.createModule(data)
        
        if (moduleHandle == 0) {
            e(LOGTAG){"Failed to load module"}
            _playbackStateFlow.value = PlaybackState.Error("Failed to load module")
            return false
        }
        
        val metadata = getMetadata()
        _playbackStateFlow.value = PlaybackState.Loaded(metadata)
        d(LOGTAG){"Module loaded: ${metadata.title}"}
        return true
    }
    
    override fun loadModuleFromPath(path: String): Boolean {
        // In browser environment, we can't load from file path directly
        throw UnsupportedOperationException("loadModuleFromPath is not supported in wasmJS. Use loadModule with ByteArray instead.")
    }
    
    override fun release() {
        d(LOGTAG){"Releasing player"}
        stop()
        positionUpdateJob?.cancel()
        
        audioPlayer.release()
        
        if (moduleHandle != 0) {
            LibOpenMpt.destroyModule(moduleHandle)
            moduleHandle = 0
        }
        
        _playbackStateFlow.value = PlaybackState.Idle
    }
    
    // ========== Playback Control ==========
    
    override fun play() {
        d(LOGTAG){"Play requested"}
        
        if (playbackState !is PlaybackState.Loaded && 
            playbackState !is PlaybackState.Paused && 
            playbackState !is PlaybackState.Stopped) {
            throw ModPlayerException.InvalidOperation("Cannot play: no module loaded or invalid state")
        }
        
        if (moduleHandle == 0) {
            throw ModPlayerException.InvalidOperation("Cannot play: no module loaded")
        }
        
        val success = audioPlayer.play()
        if (success) {
            _playbackStateFlow.value = PlaybackState.Playing
            startPositionUpdates()
        } else {
            _playbackStateFlow.value = PlaybackState.Error("Failed to start playback")
        }
    }
    
    override fun pause() {
        d(LOGTAG){"Pause requested"}
        
        if (playbackState !is PlaybackState.Playing) {
            w(LOGTAG){"Cannot pause: not currently playing"}
            return
        }
        
        audioPlayer.pause()
        _playbackStateFlow.value = PlaybackState.Paused
        stopPositionUpdates()
    }
    
    override fun stop() {
        d(LOGTAG){"Stop requested"}
        
        audioPlayer.stop()
        
        // Reset position to beginning
        if (moduleHandle != 0) {
            LibOpenMpt.setPositionSeconds(moduleHandle, 0.0)
        }
        
        _playbackStateFlow.value = PlaybackState.Stopped
        _positionFlow.value = 0.0
        stopPositionUpdates()
    }
    
    override fun seek(positionSeconds: Double) {
        d(LOGTAG){"Seek to $positionSeconds seconds"}
        
        if (moduleHandle != 0) {
            LibOpenMpt.setPositionSeconds(moduleHandle, positionSeconds)
            _positionFlow.value = positionSeconds
        }
    }
    
    // ========== Configuration ==========
    
    override fun setRepeatCount(count: Int) {
        d(LOGTAG){"Setting repeat count to $count"}
        if (moduleHandle != 0) {
            LibOpenMpt.setRepeatCount(moduleHandle, count)
        }
    }
    
    override fun setMasterGain(gainMillibel: Int) {
        d(LOGTAG){"\"Setting master gain to \$gainMillibel mB\""}
        // Use Web Audio API gain node for volume control
        audioPlayer.setMasterGain(gainMillibel)
        
        // Also set in libopenmpt
        if (moduleHandle != 0) {
            LibOpenMpt.setRenderParam(moduleHandle, RenderParam.MASTERGAIN_MILLIBEL, gainMillibel)
        }
    }
    
    override fun setStereoSeparation(percent: Int) {
        d(LOGTAG){"Setting stereo separation to $percent%"}
        if (moduleHandle != 0) {
            LibOpenMpt.setRenderParam(moduleHandle, RenderParam.STEREOSEPARATION_PERCENT, percent)
        }
    }
    
    override fun setPlaybackSpeed(speed: Double) {
        d(LOGTAG){"Setting playback speed to $speed"}
        if (moduleHandle != 0) {
            LibOpenMpt.ctlSetFloat(moduleHandle, "play.tempo_factor", speed)
        }
    }
    
    override fun getPlaybackSpeed(): Double {
        return if (moduleHandle != 0) {
            LibOpenMpt.ctlGetFloat(moduleHandle, "play.tempo_factor")
        } else {
            1.0
        }
    }
    
    override fun setPitch(pitch: Double) {
        d(LOGTAG){"Setting pitch to $pitch"}
        if (moduleHandle != 0) {
            LibOpenMpt.ctlSetFloat(moduleHandle, "play.pitch_factor", pitch)
        }
    }
    
    override fun getPitch(): Double {
        return if (moduleHandle != 0) {
            LibOpenMpt.ctlGetFloat(moduleHandle, "play.pitch_factor")
        } else {
            1.0
        }
    }
    
    // ========== State Queries ==========
    
    override val playbackState: PlaybackState
        get() = _playbackStateFlow.value
    
    override val isPlaying: Boolean
        get() = audioPlayer.isPlaying && playbackState is PlaybackState.Playing
    
    override val positionSeconds: Double
        get() = if (moduleHandle != 0) {
            LibOpenMpt.getPositionSeconds(moduleHandle)
        } else {
            0.0
        }
    
    override val durationSeconds: Double
        get() = if (moduleHandle != 0) {
            LibOpenMpt.getDurationSeconds(moduleHandle)
        } else {
            0.0
        }
    
    // ========== Module Information ==========
    
    override fun getMetadata(): ModMetadata {
        if (moduleHandle == 0) return ModMetadata()
        
        return ModMetadata(
            title = LibOpenMpt.getMetadata(moduleHandle, "title"),
            artist = LibOpenMpt.getMetadata(moduleHandle, "artist"),
            type = LibOpenMpt.getMetadata(moduleHandle, "type"),
            typeLong = LibOpenMpt.getMetadata(moduleHandle, "type_long"),
            tracker = LibOpenMpt.getMetadata(moduleHandle, "tracker"),
            message = LibOpenMpt.getMetadata(moduleHandle, "message"),
            durationSeconds = durationSeconds,
            numChannels = LibOpenMpt.getNumChannels(moduleHandle),
            numPatterns = LibOpenMpt.getNumPatterns(moduleHandle),
            numOrders = LibOpenMpt.getNumOrders(moduleHandle),
            numInstruments = LibOpenMpt.getNumInstruments(moduleHandle),
            numSamples = LibOpenMpt.getNumSamples(moduleHandle)
        )
    }
    
    override fun getCurrentOrder(): Int {
        return if (moduleHandle != 0) {
            LibOpenMpt.getCurrentOrder(moduleHandle)
        } else {
            -1
        }
    }
    
    override fun getCurrentPattern(): Int {
        return if (moduleHandle != 0) {
            LibOpenMpt.getCurrentPattern(moduleHandle)
        } else {
            -1
        }
    }
    
    override fun getCurrentRow(): Int {
        return if (moduleHandle != 0) {
            LibOpenMpt.getCurrentRow(moduleHandle)
        } else {
            -1
        }
    }
    
    override fun getNumChannels(): Int {
        return if (moduleHandle != 0) {
            LibOpenMpt.getNumChannels(moduleHandle)
        } else {
            0
        }
    }
    
    // ========== Private Methods ==========
    
    /**
     * Render audio from libopenmpt to the output buffers.
     */
    private fun renderAudio(outputLeft: JsFloat32Array, outputRight: JsFloat32Array, frameCount: Int): Int {
        if (moduleHandle == 0 || !audioPlayer.isPlaying) {
            return 0
        }
        
        // Read audio directly to the output buffers (de-interleaving happens in LibOpenMpt)
        val framesRead = LibOpenMpt.readInterleavedStereo(
            moduleHandle, 
            audioPlayer.sampleRate, 
            frameCount, 
            outputLeft,
            outputRight
        )
        
        // Check if playback has ended
        if (framesRead == 0) {
            // Module finished playing
            scope.launch {
                _playbackStateFlow.value = PlaybackState.Stopped
                _positionFlow.value = 0.0
                audioPlayer.stop()
            }
        }
        
        return framesRead
    }
    
    private fun startPositionUpdates() {
        stopPositionUpdates()
        
        positionUpdateJob = scope.launch {
            while (isActive && isPlaying) {
                val position = positionSeconds
                _positionFlow.value = position
                
                // Check if playback has ended
                if (!audioPlayer.isPlaying && playbackState is PlaybackState.Playing) {
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
}
