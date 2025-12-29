package com.beyondeye.openmpt.core

import kotlinx.cinterop.*
import platform.AudioToolbox.*
import platform.CoreAudio.*
import platform.darwin.OSStatus

/**
 * iOS Audio Engine using AudioUnit for low-latency audio playback.
 * This class manages the audio output and calls a render callback to get audio data.
 */
class IosAudioEngine(
    private val sampleRate: Int = 48000,
    private val bufferSize: Int = 1024
) {
    private var audioUnit: AudioComponentInstance? = null
    private var isInitialized = false
    private var renderCallback: ((FloatArray) -> Int)? = null
    
    // Audio buffer for interleaved stereo data (left, right, left, right, ...)
    private val audioBuffer = FloatArray(bufferSize * 2)
    
    /**
     * Initialize the audio unit for playback
     */
    @OptIn(ExperimentalForeignApi::class)
    fun initialize(): Boolean {
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
                println("IosAudioEngine: Failed to find audio component")
                return false
            }
            
            // Create instance
            val audioUnitPtr = alloc<AudioComponentInstanceVar>()
            var status = AudioComponentInstanceNew(component, audioUnitPtr.ptr)
            if (status != noErr.toInt()) {
                println("IosAudioEngine: Failed to create audio component instance: $status")
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
            
            if (status != noErr.toInt()) {
                println("IosAudioEngine: Failed to set stream format: $status")
                return false
            }
            
            // Initialize the audio unit
            status = AudioUnitInitialize(audioUnit)
            if (status != noErr.toInt()) {
                println("IosAudioEngine: Failed to initialize audio unit: $status")
                return false
            }
            
            isInitialized = true
            return true
        }
    }
    
    /**
     * Set the render callback that will be called to fill audio buffers
     * @param callback Function that receives a float array to fill with interleaved stereo data.
     *                 Returns the number of frames actually rendered.
     */
    fun setRenderCallback(callback: (FloatArray) -> Int) {
        renderCallback = callback
    }
    
    /**
     * Start audio playback
     */
    @OptIn(ExperimentalForeignApi::class)
    fun start(): Boolean {
        if (!isInitialized) {
            if (!initialize()) return false
        }
        
        val unit = audioUnit ?: return false
        
        val status = AudioOutputUnitStart(unit)
        if (status != noErr.toInt()) {
            println("IosAudioEngine: Failed to start audio unit: $status")
            return false
        }
        
        return true
    }
    
    /**
     * Stop audio playback
     */
    @OptIn(ExperimentalForeignApi::class)
    fun stop(): Boolean {
        val unit = audioUnit ?: return false
        
        val status = AudioOutputUnitStop(unit)
        if (status != noErr.toInt()) {
            println("IosAudioEngine: Failed to stop audio unit: $status")
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
        renderCallback = null
    }
    
    /**
     * Internal render callback implementation
     * This would be called from the audio render callback
     */
    internal fun render(buffer: FloatArray, frameCount: Int): Int {
        return renderCallback?.invoke(buffer) ?: 0
    }
}
