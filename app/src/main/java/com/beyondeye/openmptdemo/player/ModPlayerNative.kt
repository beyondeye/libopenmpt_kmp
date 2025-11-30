package com.beyondeye.openmptdemo.player

import android.util.Log

/**
 * JNI wrapper for the native ModPlayerEngine.
 * This class provides a Kotlin-friendly interface to the C++ native code.
 */
class ModPlayerNative {
    
    private var nativeHandle: Long = 0
    
    init {
        try {
            System.loadLibrary("openmpt")
            System.loadLibrary("modplayer")
            nativeHandle = nativeCreate()
            Log.d(TAG, "Native libraries loaded, handle: $nativeHandle")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native libraries", e)
            throw ModPlayerException.NativeLibraryError("Failed to load native libraries", e)
        }
    }
    
    // ========== Lifecycle ==========
    
    private external fun nativeCreate(): Long
    
    private external fun nativeDestroy(handle: Long)
    
    external fun nativeLoadModule(handle: Long, data: ByteArray): Boolean
    
    external fun nativeLoadModuleFromPath(handle: Long, path: String): Boolean
    
    external fun nativeUnloadModule(handle: Long)
    
    // ========== Playback Control ==========
    
    external fun nativePlay(handle: Long): Boolean
    
    external fun nativePause(handle: Long)
    
    external fun nativeStop(handle: Long)
    
    external fun nativeSeek(handle: Long, positionSeconds: Double)
    
    // ========== Configuration ==========
    
    external fun nativeSetRepeatCount(handle: Long, count: Int)
    
    external fun nativeSetMasterGain(handle: Long, gainMillibel: Int)
    
    external fun nativeSetStereoSeparation(handle: Long, percent: Int)
    
    // ========== State Queries ==========
    
    external fun nativeIsPlaying(handle: Long): Boolean
    
    external fun nativeGetPositionSeconds(handle: Long): Double
    
    external fun nativeGetDurationSeconds(handle: Long): Double
    
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
            Log.d(TAG, "Releasing native handle: $nativeHandle")
            nativeDestroy(nativeHandle)
            nativeHandle = 0
        }
    }
    
    protected fun finalize() {
        release()
    }
    
    companion object {
        private const val TAG = "ModPlayerNative"
    }
}
