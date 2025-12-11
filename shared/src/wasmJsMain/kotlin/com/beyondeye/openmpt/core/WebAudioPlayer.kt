package com.beyondeye.openmpt.core

import kotlin.math.pow

/**
 * External declarations for Web Audio API in Kotlin/Wasm.
 */

/**
 * AudioContext external declaration
 */
@JsName("AudioContext")
external class JsAudioContext : JsAny {
    val sampleRate: Int
    val currentTime: Double
    val state: String
    val destination: JsAudioDestinationNode
    
    fun createScriptProcessor(bufferSize: Int, numberOfInputChannels: Int, numberOfOutputChannels: Int): JsScriptProcessorNode
    fun createGain(): JsGainNode
    fun resume(): JsAny?
    fun suspend(): JsAny?
    fun close(): JsAny?
}

external class JsAudioDestinationNode : JsAny

external class JsScriptProcessorNode : JsAny {
    fun connect(destination: JsAudioDestinationNode)
    fun connect(destination: JsGainNode)
    fun disconnect()
}

external class JsGainNode : JsAny {
    val gain: JsAudioParam
    fun connect(destination: JsAudioDestinationNode)
    fun disconnect()
}

external class JsAudioParam : JsAny {
    var value: Float
}

// Note: JsAudioProcessingEvent is NOT declared as external class because Kotlin/Wasm
// generates instanceof checks that fail at runtime. Instead, we use JsAny and
// access properties via js() helper functions.

external class JsAudioBuffer : JsAny {
    val numberOfChannels: Int
    val length: Int
    val sampleRate: Float
    fun getChannelData(channel: Int): JsFloat32Array
}

// JS helper functions
private fun jsCreateAudioContext(): JsAudioContext =
    js("new AudioContext()")

private fun jsSetOnaudioprocess(node: JsScriptProcessorNode, callback: (JsAny) -> Unit): Unit =
    js("node.onaudioprocess = callback")

private fun jsGetOutputBuffer(event: JsAny): JsAudioBuffer =
    js("event.outputBuffer")

private fun jsClearOnaudioprocess(node: JsScriptProcessorNode): Unit =
    js("node.onaudioprocess = null")

private fun jsSetGainValue(param: JsAudioParam, value: Float): Unit =
    js("param.value = value")

private fun jsGetGainValue(param: JsAudioParam): Float =
    js("param.value")

private fun jsConsoleLog(message: String): Unit =
    js("console.log(message)")

private fun jsConsoleError(message: String): Unit =
    js("console.error(message)")

private fun jsConsoleWarn(message: String): Unit =
    js("console.warn(message)")

/**
 * Callback interface for audio processing.
 */
fun interface AudioRenderCallback {
    /**
     * Called when audio data needs to be rendered.
     * @param outputLeft Left channel output buffer
     * @param outputRight Right channel output buffer
     * @param frameCount Number of frames to render
     * @return Number of frames actually rendered
     */
    fun onRender(outputLeft: JsFloat32Array, outputRight: JsFloat32Array, frameCount: Int): Int
}

/**
 * Web Audio API player using ScriptProcessorNode.
 * 
 * Note: ScriptProcessorNode is deprecated but still widely supported.
 * A future version should migrate to AudioWorklet for better performance.
 */
class WebAudioPlayer(
    private val bufferSize: Int = 4096
) {
    private var audioContext: JsAudioContext? = null
    private var scriptProcessor: JsScriptProcessorNode? = null
    private var gainNode: JsGainNode? = null
    private var renderCallback: AudioRenderCallback? = null
    
    private var _isPlaying = false
    val isPlaying: Boolean get() = _isPlaying
    
    val sampleRate: Int
        get() = audioContext?.sampleRate ?: 48000
    
    /**
     * Initialize the audio context.
     * Must be called in response to a user gesture (click, etc.) on some browsers.
     */
    fun initialize(): Boolean {
        if (audioContext != null) return true
        
        return try {
            // Create AudioContext
            audioContext = jsCreateAudioContext()
            
            // Create gain node for volume control
            gainNode = audioContext!!.createGain()
            gainNode!!.connect(audioContext!!.destination)
            
            // Create ScriptProcessorNode for audio rendering
            scriptProcessor = audioContext!!.createScriptProcessor(bufferSize, 0, 2)
            jsSetOnaudioprocess(scriptProcessor!!) { event -> processAudio(event) }
            
            jsConsoleLog("WebAudioPlayer initialized, sample rate: ${audioContext!!.sampleRate}")
            true
        } catch (e: Throwable) {
            jsConsoleError("Failed to initialize WebAudioPlayer: ${e.message}")
            false
        }
    }
    
    /**
     * Set the audio render callback.
     */
    fun setRenderCallback(callback: AudioRenderCallback?) {
        renderCallback = callback
    }
    
    /**
     * Start audio playback.
     */
    fun play(): Boolean {
        if (audioContext == null) {
            if (!initialize()) return false
        }
        
        if (_isPlaying) return true
        
        return try {
            // Resume context if suspended (required for autoplay policy)
            if (audioContext!!.state == "suspended") {
                audioContext!!.resume()
            }
            
            // Connect processor to output
            scriptProcessor?.connect(gainNode!!)
            
            _isPlaying = true
            jsConsoleLog("WebAudioPlayer started")
            true
        } catch (e: Throwable) {
            jsConsoleError("Failed to start WebAudioPlayer: ${e.message}")
            false
        }
    }
    
    /**
     * Pause audio playback.
     */
    fun pause() {
        if (!_isPlaying) return
        
        try {
            scriptProcessor?.disconnect()
            _isPlaying = false
            jsConsoleLog("WebAudioPlayer paused")
        } catch (e: Throwable) {
            jsConsoleError("Failed to pause WebAudioPlayer: ${e.message}")
        }
    }
    
    /**
     * Stop audio playback.
     */
    fun stop() {
        pause()
    }
    
    /**
     * Set the volume.
     * @param volume Volume level (0.0 to 1.0)
     */
    fun setVolume(volume: Float) {
        gainNode?.let { node ->
            jsSetGainValue(node.gain, volume.coerceIn(0f, 1f))
        }
    }
    
    /**
     * Set master gain in millibels.
     * @param gainMillibel Gain in millibels (0 = unity gain, negative = quieter, positive = louder)
     */
    fun setMasterGain(gainMillibel: Int) {
        // Convert millibels to linear gain: gain = 10^(mB/2000)
        val linearGain = 10.0.pow(gainMillibel / 2000.0).toFloat()
        setVolume(linearGain.coerceIn(0f, 4f))  // Limit to +12dB max
    }
    
    /**
     * Release all resources.
     */
    fun release() {
        stop()
        
        try {
            scriptProcessor?.let { jsClearOnaudioprocess(it) }
            scriptProcessor?.disconnect()
            gainNode?.disconnect()
            audioContext?.close()
        } catch (e: Throwable) {
            jsConsoleError("Error releasing WebAudioPlayer: ${e.message}")
        }
        
        scriptProcessor = null
        gainNode = null
        audioContext = null
        renderCallback = null
        
        jsConsoleLog("WebAudioPlayer released")
    }
    
    /**
     * Process audio callback from ScriptProcessorNode.
     */
    private fun processAudio(event: JsAny) {
        val outputBuffer = jsGetOutputBuffer(event)
        val leftChannel = outputBuffer.getChannelData(0)
        val rightChannel = outputBuffer.getChannelData(1)
        val frameCount = outputBuffer.length
        
        val callback = renderCallback
        if (callback != null && _isPlaying) {
            val framesRendered = callback.onRender(leftChannel, rightChannel, frameCount)
            
            // Fill remaining with silence if needed
            if (framesRendered < frameCount) {
                for (i in framesRendered until frameCount) {
                    jsFloat32ArraySet(leftChannel, i, 0f)
                    jsFloat32ArraySet(rightChannel, i, 0f)
                }
            }
        } else {
            // Fill with silence
            for (i in 0 until frameCount) {
                jsFloat32ArraySet(leftChannel, i, 0f)
                jsFloat32ArraySet(rightChannel, i, 0f)
            }
        }
    }
}

/**
 * Console logging helpers accessible from other files
 */
object Console {
    fun log(message: String) = jsConsoleLog(message)
    fun error(message: String) = jsConsoleError(message)
    fun warn(message: String) = jsConsoleWarn(message)
}
