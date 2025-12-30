package com.beyondeye.openmpt.core

import kotlinx.cinterop.*
import platform.AudioToolbox.*
import platform.CoreAudio.*
import platform.CoreAudioTypes.*
import platform.darwin.OSStatus
import kotlin.concurrent.Volatile

/**
 * iOS Audio Engine using AudioUnit for low-latency audio playback.
 * This class manages the audio output and calls a render callback to get audio data.
 */
@OptIn(ExperimentalForeignApi::class)
class IosAudioEngine(
    private val sampleRate: Int = 48000,
    private val bufferSize: Int = 1024
) {
    private var audioUnit: AudioComponentInstance? = null
    @Volatile
    private var isInitialized = false
    @Volatile
    private var renderCallback: ((FloatArray) -> Int)? = null
    
    // Audio buffer for interleaved stereo data (left, right, left, right, ...)
    private val audioBuffer = FloatArray(bufferSize * 2)
    
    // StableRef to pass this instance to the C callback
    private var stableRef: StableRef<IosAudioEngine>? = null
    
    /**
     * Initialize the audio unit for playback
     */
    @OptIn(ExperimentalForeignApi::class)
    fun initialize(): Boolean {
        // println("IosAudioEngine: initialize() called, isInitialized=$isInitialized")
        if (isInitialized) return true
        
        memScoped {
            // Describe the audio component we want (Remote I/O for iOS)
            val desc = alloc<AudioComponentDescription>()
            desc.componentType = kAudioUnitType_Output
            desc.componentSubType = kAudioUnitSubType_RemoteIO
            desc.componentManufacturer = kAudioUnitManufacturer_Apple
            desc.componentFlags = 0u
            desc.componentFlagsMask = 0u
            
            // Find the component
            val component = AudioComponentFindNext(null, desc.ptr)
            if (component == null) {
                // println("IosAudioEngine: Failed to find audio component")
                return false
            }
            
            // Create instance
            val audioUnitPtr = alloc<AudioComponentInstanceVar>()
            var status = AudioComponentInstanceNew(component, audioUnitPtr.ptr)
            if (status != 0) { //noErr.toInt()
                // println("IosAudioEngine: Failed to create audio component instance: $status")
                return false
            }
            audioUnit = audioUnitPtr.value
            
            // Set the audio format
            val streamFormat = alloc<AudioStreamBasicDescription>()
            streamFormat.mSampleRate = sampleRate.toDouble()
            streamFormat.mFormatID = kAudioFormatLinearPCM
            streamFormat.mFormatFlags = kAudioFormatFlagIsFloat or kAudioFormatFlagIsPacked
            streamFormat.mBytesPerPacket = 8u // 2 channels * 4 bytes (float)
            streamFormat.mFramesPerPacket = 1u
            streamFormat.mBytesPerFrame = 8u
            streamFormat.mChannelsPerFrame = 2u
            streamFormat.mBitsPerChannel = 32u
            
            status = AudioUnitSetProperty(
                audioUnit,
                kAudioUnitProperty_StreamFormat,
                kAudioUnitScope_Input,
                0u, // Output bus
                streamFormat.ptr,
                sizeOf<AudioStreamBasicDescription>().toUInt()
            )
            
            if (status != 0) { //noErr.toInt()
                // println("IosAudioEngine: Failed to set stream format: $status")
                return false
            }
            
            // Create a stable reference to this instance for the callback
            stableRef = StableRef.create(this@IosAudioEngine)
            
            // Set up the render callback
            val callbackStruct = alloc<AURenderCallbackStruct>()
            callbackStruct.inputProc = staticCFunction { inRefCon, _, _, _, inNumberFrames, ioData ->
                val engine = inRefCon?.asStableRef<IosAudioEngine>()?.get()
                    ?: return@staticCFunction -1
                
                engine.fillAudioBuffer(inNumberFrames.toInt(), ioData)
            }
            callbackStruct.inputProcRefCon = stableRef!!.asCPointer()
            
            status = AudioUnitSetProperty(
                audioUnit,
                kAudioUnitProperty_SetRenderCallback,
                kAudioUnitScope_Input,
                0u, // Output bus
                callbackStruct.ptr,
                sizeOf<AURenderCallbackStruct>().toUInt()
            )
            
            if (status != 0) { //noErr.toInt()
                // println("IosAudioEngine: Failed to set render callback: $status")
                stableRef?.dispose()
                stableRef = null
                return false
            }
            
            // Initialize the audio unit
            status = AudioUnitInitialize(audioUnit)
            if (status != 0) { //noErr.toInt()
                // println("IosAudioEngine: Failed to initialize audio unit: $status")
                stableRef?.dispose()
                stableRef = null
                return false
            }
            
            isInitialized = true
            // println("IosAudioEngine: initialization complete, isInitialized=$isInitialized")
            return true
        }
    }
    
    // Counter to limit logging frequency
    private var callbackCount = 0
    
    /**
     * Fill the audio buffer with rendered audio data.
     * Called from the audio render callback.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun fillAudioBuffer(frameCount: Int, ioData: CPointer<AudioBufferList>?): OSStatus {
        callbackCount++
        // Log every 100 callbacks to avoid flooding
        if (callbackCount <= 5 || callbackCount % 100 == 0) {
            // println("IosAudioEngine: fillAudioBuffer() called, frameCount=$frameCount, callbackCount=$callbackCount")
        }
        
        val bufferList = ioData?.pointed ?: run {
            // println("IosAudioEngine: fillAudioBuffer() - ioData is null!")
            return -1
        }
        
        // Get the audio buffer from the buffer list
        val buffer = bufferList.mBuffers[0]
        val dataPtr = buffer.mData?.reinterpret<FloatVar>() ?: return -1
        
        // Create a temporary Kotlin array to pass to the render callback
        val tempBuffer = FloatArray(frameCount * 2)
        
        // Call the render callback to fill the buffer
        val framesRendered = renderCallback?.invoke(tempBuffer) ?: run {
            if (callbackCount <= 5) {
                // println("IosAudioEngine: fillAudioBuffer() - renderCallback is null!")
            }
            0
        }
        
        if (callbackCount <= 5 || callbackCount % 100 == 0) {
            // println("IosAudioEngine: fillAudioBuffer() - framesRendered=$framesRendered")
        }
        
        if (framesRendered > 0) {
            // Copy the rendered data to the audio buffer
            for (i in 0 until (framesRendered * 2).coerceAtMost(frameCount * 2)) {
                dataPtr[i] = tempBuffer[i]
            }
            // Fill remaining with silence if needed
            for (i in (framesRendered * 2) until (frameCount * 2)) {
                dataPtr[i] = 0f
            }
        } else {
            // Fill with silence
            for (i in 0 until frameCount * 2) {
                dataPtr[i] = 0f
            }
        }
        
        return 0 // noErr
    }
    
    /**
     * Set the render callback that will be called to fill audio buffers
     * @param callback Function that receives a float array to fill with interleaved stereo data.
     *                 Returns the number of frames actually rendered.
     */
    fun setRenderCallback(callback: (FloatArray) -> Int) {
        // println("IosAudioEngine: setRenderCallback() called, callback is not null: ${callback != null}")
        renderCallback = callback
        // println("IosAudioEngine: setRenderCallback() - renderCallback set, is not null: ${renderCallback != null}")
    }
    
    /**
     * Start audio playback
     */
    @OptIn(ExperimentalForeignApi::class)
    fun start(): Boolean {
        // println("IosAudioEngine: start() called, isInitialized=$isInitialized, renderCallback is not null: ${renderCallback != null}")
        callbackCount = 0 // Reset callback counter
        
        // Verify callback is set before starting
        if (renderCallback == null) {
            // println("IosAudioEngine: start() - WARNING: renderCallback is null! Audio will be silent.")
        }
        
        if (!isInitialized) {
            // println("IosAudioEngine: start() - need to initialize first")
            if (!initialize()) {
                // println("IosAudioEngine: start() - initialization failed!")
                return false
            }
        }
        
        val unit = audioUnit ?: run {
            // println("IosAudioEngine: start() - audioUnit is null!")
            return false
        }
        
        // println("IosAudioEngine: start() - calling AudioOutputUnitStart...")
        val status = AudioOutputUnitStart(unit)
        if (status != 0) { //noErr.toInt()
            // println("IosAudioEngine: Failed to start audio unit: $status")
            return false
        }
        
        // println("IosAudioEngine: start() - AudioOutputUnitStart succeeded!")
        return true
    }
    
    /**
     * Stop audio playback
     */
    @OptIn(ExperimentalForeignApi::class)
    fun stop(): Boolean {
        val unit = audioUnit ?: return false
        
        val status = AudioOutputUnitStop(unit)
        if (status != 0) { //noErr.toInt()
            // println("IosAudioEngine: Failed to stop audio unit: $status")
            return false
        }
        
        return true
    }
    
    /**
     * Clean up and release resources
     */
    @OptIn(ExperimentalForeignApi::class)
    fun release() {
        audioUnit?.let { unit ->
            AudioOutputUnitStop(unit)
            AudioUnitUninitialize(unit)
            AudioComponentInstanceDispose(unit)
        }
        audioUnit = null
        isInitialized = false
        // NOTE: Do NOT clear renderCallback here!
        // The callback represents the connection between the player and audio engine,
        // and should persist across module loads. It's set once in IosModPlayer.init.
        
        // Dispose the stable reference
        stableRef?.dispose()
        stableRef = null
    }
    
    /**
     * Internal render callback implementation
     * This would be called from the audio render callback
     */
    internal fun render(buffer: FloatArray, frameCount: Int): Int {
        return renderCallback?.invoke(buffer) ?: 0
    }
}
