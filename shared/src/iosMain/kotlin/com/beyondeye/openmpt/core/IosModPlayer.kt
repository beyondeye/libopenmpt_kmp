package com.beyondeye.openmpt.core

import kotlinx.cinterop.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import libopenmpt.*

/**
 * iOS implementation of the ModPlayer interface using libopenmpt via cinterop.
 * Uses AudioUnit for audio playback on iOS.
 */
@OptIn(ExperimentalForeignApi::class)
class IosModPlayer : ModPlayer {
    
    companion object {
        private const val SAMPLE_RATE = 48000
        private const val BUFFER_SIZE = 1024
    }
    
    private val _playbackStateFlow = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackStateFlow: StateFlow<PlaybackState> = _playbackStateFlow.asStateFlow()
    
    private val _positionFlow = MutableStateFlow(0.0)
    override val positionFlow: StateFlow<Double> = _positionFlow.asStateFlow()
    
    // libopenmpt module handle
    private var module: CPointer<openmpt_module>? = null
    
    // Audio engine for iOS
    private val audioEngine = IosAudioEngine(SAMPLE_RATE, BUFFER_SIZE)
    
    // Audio buffer for rendering
    private val renderBuffer = FloatArray(BUFFER_SIZE * 2) // Interleaved stereo
    
    // Playback state
    private var _duration: Double = 0.0
    private var _isModuleLoaded = false
    
    // Tempo/pitch factors
    private var _tempoFactor = 1.0
    private var _pitchFactor = 1.0
    
    init {
        // Set up the audio render callback
        audioEngine.setRenderCallback { buffer ->
            renderAudio(buffer)
        }
    }
    
    override fun loadModule(data: ByteArray): Boolean {
        release()
        
        memScoped {
            val dataPtr = data.refTo(0).getPointer(this)
            val errorCode = alloc<IntVar>()
            
            module = openmpt_module_create_from_memory2(
                filedata = dataPtr,
                filesize = data.size.toULong(),
                logfunc = null,
                loguser = null,
                errfunc = null,
                erruser = null,
                error = errorCode.ptr,
                error_message = null,
                ctls = null
            )
            
            if (module == null) {
                println("IosModPlayer: Failed to load module, error: ${errorCode.value}")
                return false
            }
        }
        
        _duration = openmpt_module_get_duration_seconds(module)
        _isModuleLoaded = true
        _playbackStateFlow.value = PlaybackState.Stopped
        
        // Apply stored settings
        applyTempoFactor()
        applyPitchFactor()
        
        return true
    }
    
    override fun loadModuleFromPath(path: String): Boolean {
        // For iOS, we typically load from memory (ByteArray from resources)
        // File path loading would require NSFileManager to read the file
        // For now, this is not implemented - use loadModule(ByteArray) instead
        println("IosModPlayer: loadModuleFromPath not implemented. Use loadModule(ByteArray) instead.")
        return false
    }
    
    override fun release() {
        stop()
        audioEngine.release()
        
        module?.let { mod ->
            openmpt_module_destroy(mod)
        }
        module = null
        _isModuleLoaded = false
        _duration = 0.0
        _playbackStateFlow.value = PlaybackState.Idle
        _positionFlow.value = 0.0
    }
    
    override fun play() {
        if (!_isModuleLoaded || module == null) return
        
        if (audioEngine.start()) {
            _playbackStateFlow.value = PlaybackState.Playing
        }
    }
    
    override fun pause() {
        if (!_isModuleLoaded) return
        
        if (audioEngine.stop()) {
            _playbackStateFlow.value = PlaybackState.Paused
        }
    }
    
    override fun stop() {
        if (!_isModuleLoaded) return
        
        audioEngine.stop()
        module?.let { mod ->
            openmpt_module_set_position_seconds(mod, 0.0)
        }
        _positionFlow.value = 0.0
        _playbackStateFlow.value = PlaybackState.Stopped
    }
    
    override fun seek(positionSeconds: Double) {
        module?.let { mod ->
            openmpt_module_set_position_seconds(mod, positionSeconds)
            _positionFlow.value = openmpt_module_get_position_seconds(mod)
        }
    }
    
    override fun setRepeatCount(count: Int) {
        module?.let { mod ->
            openmpt_module_set_repeat_count(mod, count)
        }
    }
    
    override fun setMasterGain(gainMillibel: Int) {
        module?.let { mod ->
            openmpt_module_set_render_param(
                mod,
                OPENMPT_MODULE_RENDER_MASTERGAIN_MILLIBEL,
                gainMillibel
            )
        }
    }
    
    override fun setStereoSeparation(percent: Int) {
        module?.let { mod ->
            openmpt_module_set_render_param(
                mod,
                OPENMPT_MODULE_RENDER_STEREOSEPARATION_PERCENT,
                percent
            )
        }
    }
    
    override fun setPlaybackSpeed(speed: Double) {
        _tempoFactor = speed
        applyTempoFactor()
    }
    
    override fun getPlaybackSpeed(): Double = _tempoFactor
    
    override fun setPitch(pitch: Double) {
        _pitchFactor = pitch
        applyPitchFactor()
    }
    
    override fun getPitch(): Double = _pitchFactor
    
    private fun applyTempoFactor() {
        module?.let { mod ->
            openmpt_module_ctl_set_floatingpoint(mod, "play.tempo_factor", _tempoFactor)
        }
    }
    
    private fun applyPitchFactor() {
        module?.let { mod ->
            openmpt_module_ctl_set_floatingpoint(mod, "play.pitch_factor", _pitchFactor)
        }
    }
    
    override val playbackState: PlaybackState
        get() = _playbackStateFlow.value
    
    override val isPlaying: Boolean
        get() = _playbackStateFlow.value == PlaybackState.Playing
    
    override val positionSeconds: Double
        get() = module?.let { openmpt_module_get_position_seconds(it) } ?: 0.0
    
    override val durationSeconds: Double
        get() = _duration
    
    override fun getMetadata(): ModMetadata {
        val mod = module ?: return ModMetadata()
        
        return ModMetadata(
            title = openmpt_module_get_metadata(mod, "title")?.toKString() ?: "",
            artist = openmpt_module_get_metadata(mod, "artist")?.toKString() ?: "",
            tracker = openmpt_module_get_metadata(mod, "tracker")?.toKString() ?: "",
            type = openmpt_module_get_metadata(mod, "type")?.toKString() ?: "",
            typeLong = openmpt_module_get_metadata(mod, "type_long")?.toKString() ?: "",
            message = openmpt_module_get_metadata(mod, "message")?.toKString() ?: ""
        )
    }
    
    override fun getCurrentOrder(): Int {
        return module?.let { openmpt_module_get_current_order(it) } ?: -1
    }
    
    override fun getCurrentPattern(): Int {
        return module?.let { openmpt_module_get_current_pattern(it) } ?: -1
    }
    
    override fun getCurrentRow(): Int {
        return module?.let { openmpt_module_get_current_row(it) } ?: -1
    }
    
    override fun getNumChannels(): Int {
        return module?.let { openmpt_module_get_num_channels(it) } ?: 0
    }
    
    /**
     * Render audio to the provided buffer.
     * Called from the audio engine's render callback.
     * @return Number of frames rendered
     */
    private fun renderAudio(buffer: FloatArray): Int {
        val mod = module ?: return 0
        
        if (_playbackStateFlow.value != PlaybackState.Playing) {
            // Fill with silence
            buffer.fill(0f)
            return 0
        }
        
        val frameCount = buffer.size / 2 // Stereo, so divide by 2
        
        memScoped {
            val nativeBuffer = allocArray<FloatVar>(buffer.size)
            
            val framesRendered = openmpt_module_read_interleaved_float_stereo(
                mod,
                SAMPLE_RATE,
                frameCount.toULong(),
                nativeBuffer
            )
            
            // Copy to Kotlin array
            for (i in 0 until (framesRendered.toInt() * 2)) {
                buffer[i] = nativeBuffer[i]
            }
            
            // Update position
            _positionFlow.value = openmpt_module_get_position_seconds(mod)
            
            // Check for end of playback
            if (framesRendered.toInt() == 0) {
                _playbackStateFlow.value = PlaybackState.Stopped
            }
            
            return framesRendered.toInt()
        }
    }
}
