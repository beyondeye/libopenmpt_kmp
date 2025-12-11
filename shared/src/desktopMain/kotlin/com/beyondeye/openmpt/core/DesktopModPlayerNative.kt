package com.beyondeye.openmpt.core

/**
 * JNI wrapper for the native libopenmpt functions on Desktop JVM.
 * 
 * This class provides a Kotlin-friendly interface to the C++ native code.
 * Unlike the Android version, this doesn't handle audio output - that's done
 * in Kotlin using JavaSound.
 */
class DesktopModPlayerNative {
    
    private var nativeHandle: Long = 0
    
    init {
        try {
            NativeLibraryLoader.loadLibraries()
            nativeHandle = nativeCreate()
            println("DesktopModPlayerNative: Native handle created: $nativeHandle")
        } catch (e: Throwable) {
            println("DesktopModPlayerNative: Failed to initialize: ${e.message}")
            throw ModPlayerException.NativeLibraryError("Failed to load native libraries", e)
        }
    }
    
    // ========== Lifecycle ==========
    
    private external fun nativeCreate(): Long
    
    private external fun nativeDestroy(handle: Long)
    
    external fun nativeLoadModule(handle: Long, data: ByteArray): Boolean
    
    external fun nativeLoadModuleFromPath(handle: Long, path: String): Boolean
    
    external fun nativeUnloadModule(handle: Long)
    
    // ========== Audio Rendering ==========
    
    /**
     * Render audio frames from the module.
     * 
     * @param handle Native handle
     * @param sampleRate Sample rate in Hz (e.g., 48000)
     * @param numFrames Number of frames to render
     * @return Float array with interleaved stereo samples (length = numFrames * 2),
     *         or null if no module is loaded or rendering failed
     */
    external fun nativeReadAudio(handle: Long, sampleRate: Int, numFrames: Int): FloatArray?
    
    // ========== Position Control ==========
    
    external fun nativeSeek(handle: Long, positionSeconds: Double)
    
    external fun nativeGetPositionSeconds(handle: Long): Double
    
    external fun nativeGetDurationSeconds(handle: Long): Double
    
    // ========== Configuration ==========
    
    external fun nativeSetRepeatCount(handle: Long, count: Int)
    
    external fun nativeSetMasterGain(handle: Long, gainMillibel: Int)
    
    external fun nativeSetStereoSeparation(handle: Long, percent: Int)
    
    external fun nativeSetTempoFactor(handle: Long, factor: Double)
    
    external fun nativeGetTempoFactor(handle: Long): Double
    
    external fun nativeSetPitchFactor(handle: Long, factor: Double)
    
    external fun nativeGetPitchFactor(handle: Long): Double
    
    // ========== Metadata Queries ==========
    
    external fun nativeGetMetadata(handle: Long, key: String): String
    
    external fun nativeGetCurrentOrder(handle: Long): Int
    
    external fun nativeGetCurrentPattern(handle: Long): Int
    
    external fun nativeGetCurrentRow(handle: Long): Int
    
    external fun nativeGetNumChannels(handle: Long): Int
    
    external fun nativeGetNumPatterns(handle: Long): Int
    
    external fun nativeGetNumOrders(handle: Long): Int
    
    external fun nativeGetNumInstruments(handle: Long): Int
    
    external fun nativeGetNumSamples(handle: Long): Int
    
    // ========== Public API ==========
    
    fun getHandle(): Long = nativeHandle
    
    fun release() {
        if (nativeHandle != 0L) {
            println("DesktopModPlayerNative: Releasing native handle: $nativeHandle")
            nativeDestroy(nativeHandle)
            nativeHandle = 0
        }
    }
    
    protected fun finalize() {
        release()
    }
    
    companion object {
        private const val TAG = "DesktopModPlayerNative"
    }
}
