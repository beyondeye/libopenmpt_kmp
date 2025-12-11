package com.beyondeye.openmpt.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

/**
 * Desktop (JVM) implementation of the ModPlayer interface.
 * Uses JNI + libopenmpt for MOD rendering and JavaSound for audio output.
 * 
 * Architecture:
 * - Native layer (libopenmpt via JNI): Module loading, rendering to float buffers
 * - Kotlin layer (JavaSound): Audio output via SourceDataLine
 * 
 * Audio rendering happens in a background coroutine that:
 * 1. Calls native code to render float samples
 * 2. Converts float samples to 16-bit PCM
 * 3. Writes PCM data to JavaSound SourceDataLine
 */
class DesktopModPlayer : ModPlayer {
    
    private val native: DesktopModPlayerNative = DesktopModPlayerNative()
    private val handle: Long get() = native.getHandle()
    
    // State flows
    private val _playbackStateFlow = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackStateFlow: StateFlow<PlaybackState> = _playbackStateFlow.asStateFlow()
    
    private val _positionFlow = MutableStateFlow(0.0)
    override val positionFlow: StateFlow<Double> = _positionFlow.asStateFlow()
    
    // Audio playback
    private var audioLine: SourceDataLine? = null
    private var audioJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    
    // Audio configuration
    companion object {
        private const val TAG = "DesktopModPlayer"
        private const val SAMPLE_RATE = 48000
        private const val CHANNELS = 2
        private const val BITS_PER_SAMPLE = 16
        private const val BUFFER_SIZE_FRAMES = 2048  // ~42ms at 48kHz
        private const val POSITION_UPDATE_INTERVAL_MS = 100L
    }
    
    // ========== Lifecycle ==========
    
    override fun loadModule(data: ByteArray): Boolean {
        println("$TAG: Loading module from byte array (${data.size} bytes)")
        _playbackStateFlow.value = PlaybackState.Loading
        
        return try {
            val success = native.nativeLoadModule(handle, data)
            if (success) {
                val metadata = getMetadata()
                _playbackStateFlow.value = PlaybackState.Loaded(metadata)
                println("$TAG: Module loaded: ${metadata.title}")
            } else {
                _playbackStateFlow.value = PlaybackState.Error("Failed to load module")
            }
            success
        } catch (e: Exception) {
            println("$TAG: Error loading module: ${e.message}")
            e.printStackTrace()
            _playbackStateFlow.value = PlaybackState.Error("Error loading module: ${e.message}", e)
            false
        }
    }
    
    override fun loadModuleFromPath(path: String): Boolean {
        println("$TAG: Loading module from path: $path")
        _playbackStateFlow.value = PlaybackState.Loading
        
        return try {
            val success = native.nativeLoadModuleFromPath(handle, path)
            if (success) {
                val metadata = getMetadata()
                _playbackStateFlow.value = PlaybackState.Loaded(metadata)
                println("$TAG: Module loaded: ${metadata.title}")
            } else {
                _playbackStateFlow.value = PlaybackState.Error("Failed to load module from path")
            }
            success
        } catch (e: Exception) {
            println("$TAG: Error loading module from path: ${e.message}")
            e.printStackTrace()
            _playbackStateFlow.value = PlaybackState.Error("Error loading module: ${e.message}", e)
            false
        }
    }
    
    override fun release() {
        println("$TAG: Releasing player")
        stop()
        audioJob?.cancel()
        closeAudioLine()
        native.nativeUnloadModule(handle)
        native.release()
        _playbackStateFlow.value = PlaybackState.Idle
    }
    
    // ========== Playback Control ==========
    
    override fun play() {
        println("$TAG: Play requested")
        if (playbackState !is PlaybackState.Loaded && 
            playbackState !is PlaybackState.Paused && 
            playbackState !is PlaybackState.Stopped) {
            throw ModPlayerException.InvalidOperation("Cannot play: no module loaded or invalid state")
        }
        
        try {
            // Initialize audio line if needed
            if (audioLine == null) {
                initAudioLine()
            }
            
            audioLine?.start()
            _playbackStateFlow.value = PlaybackState.Playing
            startAudioLoop()
        } catch (e: Exception) {
            println("$TAG: Failed to start playback: ${e.message}")
            e.printStackTrace()
            _playbackStateFlow.value = PlaybackState.Error("Failed to start playback: ${e.message}", e)
        }
    }
    
    override fun pause() {
        println("$TAG: Pause requested")
        if (playbackState !is PlaybackState.Playing) {
            println("$TAG: Cannot pause: not currently playing")
            return
        }
        
        audioJob?.cancel()
        audioJob = null
        audioLine?.stop()
        _playbackStateFlow.value = PlaybackState.Paused
    }
    
    override fun stop() {
        println("$TAG: Stop requested")
        audioJob?.cancel()
        audioJob = null
        audioLine?.stop()
        audioLine?.flush()
        
        // Reset position to beginning
        native.nativeSeek(handle, 0.0)
        _positionFlow.value = 0.0
        _playbackStateFlow.value = PlaybackState.Stopped
    }
    
    override fun seek(positionSeconds: Double) {
        println("$TAG: Seek to $positionSeconds seconds")
        native.nativeSeek(handle, positionSeconds)
        _positionFlow.value = positionSeconds
    }
    
    // ========== Configuration ==========
    
    override fun setRepeatCount(count: Int) {
        println("$TAG: Setting repeat count to $count")
        native.nativeSetRepeatCount(handle, count)
    }
    
    override fun setMasterGain(gainMillibel: Int) {
        println("$TAG: Setting master gain to $gainMillibel mB")
        native.nativeSetMasterGain(handle, gainMillibel)
    }
    
    override fun setStereoSeparation(percent: Int) {
        println("$TAG: Setting stereo separation to $percent%")
        native.nativeSetStereoSeparation(handle, percent)
    }
    
    override fun setPlaybackSpeed(speed: Double) {
        println("$TAG: Setting playback speed to $speed")
        native.nativeSetTempoFactor(handle, speed)
    }
    
    override fun getPlaybackSpeed(): Double {
        return native.nativeGetTempoFactor(handle)
    }
    
    override fun setPitch(pitch: Double) {
        println("$TAG: Setting pitch to $pitch")
        native.nativeSetPitchFactor(handle, pitch)
    }
    
    override fun getPitch(): Double {
        return native.nativeGetPitchFactor(handle)
    }
    
    // ========== State Queries ==========
    
    override val playbackState: PlaybackState
        get() = _playbackStateFlow.value
    
    override val isPlaying: Boolean
        get() = _playbackStateFlow.value is PlaybackState.Playing
    
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
    
    /**
     * Initialize the JavaSound audio line for playback.
     */
    private fun initAudioLine() {
        val format = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            SAMPLE_RATE.toFloat(),
            BITS_PER_SAMPLE,
            CHANNELS,
            (CHANNELS * BITS_PER_SAMPLE) / 8,  // Frame size
            SAMPLE_RATE.toFloat(),
            false  // Little endian
        )
        
        val info = DataLine.Info(SourceDataLine::class.java, format)
        
        if (!AudioSystem.isLineSupported(info)) {
            throw ModPlayerException.InitializationFailed("Audio format not supported: $format")
        }
        
        audioLine = (AudioSystem.getLine(info) as SourceDataLine).apply {
            open(format, BUFFER_SIZE_FRAMES * CHANNELS * 2)  // Buffer in bytes
        }
        
        println("$TAG: Audio line initialized: $format")
    }
    
    /**
     * Close the audio line and release resources.
     */
    private fun closeAudioLine() {
        audioLine?.let { line ->
            if (line.isOpen) {
                line.stop()
                line.close()
            }
        }
        audioLine = null
    }
    
    /**
     * Start the audio rendering loop in a background coroutine.
     */
    private fun startAudioLoop() {
        audioJob?.cancel()
        
        audioJob = scope.launch {
            val byteBuffer = ByteArray(BUFFER_SIZE_FRAMES * CHANNELS * 2)  // 16-bit stereo
            var lastPositionUpdate = System.currentTimeMillis()
            
            println("$TAG: Audio loop started")
            
            while (isActive && _playbackStateFlow.value is PlaybackState.Playing) {
                // Render audio from native
                val floatSamples = native.nativeReadAudio(handle, SAMPLE_RATE, BUFFER_SIZE_FRAMES)
                
                if (floatSamples == null) {
                    // Module ended or error
                    println("$TAG: No more audio data, stopping playback")
                    _playbackStateFlow.value = PlaybackState.Stopped
                    _positionFlow.value = 0.0
                    break
                }
                
                // Check if all samples are zero (end of module)
                val hasAudio = floatSamples.any { it != 0f }
                if (!hasAudio) {
                    // Check position - if we're at or past duration, we've finished
                    val position = native.nativeGetPositionSeconds(handle)
                    val duration = native.nativeGetDurationSeconds(handle)
                    if (position >= duration - 0.1) {
                        println("$TAG: Module playback completed")
                        _playbackStateFlow.value = PlaybackState.Stopped
                        _positionFlow.value = 0.0
                        break
                    }
                }
                
                // Convert float samples to 16-bit PCM
                convertFloatToPcm16(floatSamples, byteBuffer)
                
                // Write to audio line (blocking)
                audioLine?.write(byteBuffer, 0, byteBuffer.size)
                
                // Update position periodically
                val now = System.currentTimeMillis()
                if (now - lastPositionUpdate >= POSITION_UPDATE_INTERVAL_MS) {
                    _positionFlow.value = native.nativeGetPositionSeconds(handle)
                    lastPositionUpdate = now
                }
            }
            
            println("$TAG: Audio loop ended")
        }
    }
    
    /**
     * Convert interleaved float samples [-1.0, 1.0] to 16-bit PCM little-endian.
     */
    private fun convertFloatToPcm16(floatSamples: FloatArray, byteBuffer: ByteArray) {
        for (i in floatSamples.indices) {
            // Clamp and scale to 16-bit range
            val sample = (floatSamples[i].coerceIn(-1f, 1f) * 32767f).toInt()
            
            // Little-endian encoding
            val byteIndex = i * 2
            byteBuffer[byteIndex] = (sample and 0xFF).toByte()
            byteBuffer[byteIndex + 1] = ((sample shr 8) and 0xFF).toByte()
        }
    }
}
