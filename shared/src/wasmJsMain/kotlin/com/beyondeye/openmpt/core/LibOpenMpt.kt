@file:Suppress("UNUSED_PARAMETER")

package com.beyondeye.openmpt.core

/**
 * External declarations for libopenmpt WASM module compiled with Emscripten.
 * These declarations map to the C API exposed by libopenmpt.js.
 * 
 * Note: In Kotlin/Wasm, we use JsAny and JsArray for interop.
 */

/**
 * JsNumber wrapper for number operations
 */
external class JsNumber : JsAny {
    fun toInt(): Int
    fun toDouble(): Double
}

/**
 * Float32Array external declaration for Kotlin/Wasm
 */
@JsName("Float32Array")
external class JsFloat32Array(length: Int) : JsAny {
    val length: Int
}

/**
 * Uint8Array external declaration for Kotlin/Wasm
 */
@JsName("Uint8Array")
external class JsUint8Array : JsAny {
    val length: Int
}

/**
 * Get element from Float32Array
 */
internal fun jsFloat32ArrayGet(array: JsFloat32Array, index: Int): Float =
    js("array[index]")

/**
 * Set element in Float32Array
 */
internal fun jsFloat32ArraySet(array: JsFloat32Array, index: Int, value: Float): Unit =
    js("array[index] = value")

/**
 * Get element from Uint8Array
 */
private fun jsUint8ArrayGet(array: JsUint8Array, index: Int): Int =
    js("array[index]")

/**
 * Set element in Uint8Array
 */
private fun jsUint8ArraySet(array: JsUint8Array, index: Int, value: Int): Unit =
    js("array[index] = value")

// Direct JS function calls for libopenmpt

private fun jsLibOpenMptMalloc(size: Int): Int =
    js("libopenmpt._malloc(size)")

private fun jsLibOpenMptFree(ptr: Int): Unit =
    js("libopenmpt._free(ptr)")

private fun jsLibOpenMptHeapU8(): JsUint8Array =
    js("libopenmpt.HEAPU8")

private fun jsLibOpenMptHeapF32(): JsFloat32Array =
    js("libopenmpt.HEAPF32")

private fun jsLibOpenMptUTF8ToString(ptr: Int): String =
    js("libopenmpt.UTF8ToString(ptr)")

private fun jsLibOpenMptStringToUTF8(str: String, outPtr: Int, maxBytesToWrite: Int): Int =
    js("libopenmpt.stringToUTF8(str, outPtr, maxBytesToWrite)")

private fun jsLibOpenMptLengthBytesUTF8(str: String): Int =
    js("libopenmpt.lengthBytesUTF8(str)")

private fun jsLibOpenMptModuleCreateFromMemory2(
    data: Int, size: Int,
    logfunc: Int, loguser: Int,
    errfunc: Int, erruser: Int,
    error: Int, errorMessage: Int, ctls: Int
): Int = js("libopenmpt._openmpt_module_create_from_memory2(data, size, logfunc, loguser, errfunc, erruser, error, errorMessage, ctls)")

private fun jsLibOpenMptModuleDestroy(mod: Int): Unit =
    js("libopenmpt._openmpt_module_destroy(mod)")

private fun jsLibOpenMptModuleReadInterleavedFloatStereo(mod: Int, samplerate: Int, count: Int, interleaved: Int): Int =
    js("libopenmpt._openmpt_module_read_interleaved_float_stereo(mod, samplerate, count, interleaved)")

private fun jsLibOpenMptModuleGetDurationSeconds(mod: Int): Double =
    js("libopenmpt._openmpt_module_get_duration_seconds(mod)")

private fun jsLibOpenMptModuleGetPositionSeconds(mod: Int): Double =
    js("libopenmpt._openmpt_module_get_position_seconds(mod)")

private fun jsLibOpenMptModuleSetPositionSeconds(mod: Int, seconds: Double): Double =
    js("libopenmpt._openmpt_module_set_position_seconds(mod, seconds)")

private fun jsLibOpenMptModuleGetRepeatCount(mod: Int): Int =
    js("libopenmpt._openmpt_module_get_repeat_count(mod)")

private fun jsLibOpenMptModuleSetRepeatCount(mod: Int, repeatCount: Int): Int =
    js("libopenmpt._openmpt_module_set_repeat_count(mod, repeatCount)")

private fun jsLibOpenMptModuleSetRenderParam(mod: Int, param: Int, value: Int): Int =
    js("libopenmpt._openmpt_module_set_render_param(mod, param, value)")

private fun jsLibOpenMptModuleCtlGetFloatingpoint(mod: Int, ctl: Int): Double =
    js("libopenmpt._openmpt_module_ctl_get_floatingpoint(mod, ctl)")

private fun jsLibOpenMptModuleCtlSetFloatingpoint(mod: Int, ctl: Int, value: Double): Int =
    js("libopenmpt._openmpt_module_ctl_set_floatingpoint(mod, ctl, value)")

private fun jsLibOpenMptModuleGetMetadata(mod: Int, key: Int): Int =
    js("libopenmpt._openmpt_module_get_metadata(mod, key)")

private fun jsLibOpenMptFreeString(str: Int): Unit =
    js("libopenmpt._openmpt_free_string(str)")

private fun jsLibOpenMptModuleGetNumChannels(mod: Int): Int =
    js("libopenmpt._openmpt_module_get_num_channels(mod)")

private fun jsLibOpenMptModuleGetNumOrders(mod: Int): Int =
    js("libopenmpt._openmpt_module_get_num_orders(mod)")

private fun jsLibOpenMptModuleGetNumPatterns(mod: Int): Int =
    js("libopenmpt._openmpt_module_get_num_patterns(mod)")

private fun jsLibOpenMptModuleGetNumInstruments(mod: Int): Int =
    js("libopenmpt._openmpt_module_get_num_instruments(mod)")

private fun jsLibOpenMptModuleGetNumSamples(mod: Int): Int =
    js("libopenmpt._openmpt_module_get_num_samples(mod)")

private fun jsLibOpenMptModuleGetCurrentOrder(mod: Int): Int =
    js("libopenmpt._openmpt_module_get_current_order(mod)")

private fun jsLibOpenMptModuleGetCurrentPattern(mod: Int): Int =
    js("libopenmpt._openmpt_module_get_current_pattern(mod)")

private fun jsLibOpenMptModuleGetCurrentRow(mod: Int): Int =
    js("libopenmpt._openmpt_module_get_current_row(mod)")

private fun jsCheckLibOpenMptReady(): Boolean =
    js("typeof libopenmpt !== 'undefined' && libopenmpt.HEAPU8 !== undefined")

/**
 * Render parameter constants
 */
object RenderParam {
    const val MASTERGAIN_MILLIBEL = 1
    const val STEREOSEPARATION_PERCENT = 2
    const val INTERPOLATIONFILTER_LENGTH = 3
    const val VOLUMERAMPING_STRENGTH = 4
}

/**
 * Helper object for working with libopenmpt from Kotlin.
 */
object LibOpenMpt {
    
    /**
     * Check if libopenmpt module is ready to use.
     */
    fun isReady(): Boolean {
        return try {
            jsCheckLibOpenMptReady()
        } catch (e: Throwable) {
            false
        }
    }
    
    /**
     * Allocate memory in the WASM heap.
     */
    fun malloc(size: Int): Int = jsLibOpenMptMalloc(size)
    
    /**
     * Free memory in the WASM heap.
     */
    fun free(ptr: Int) = jsLibOpenMptFree(ptr)
    
    /**
     * Copy a ByteArray to the WASM heap.
     * @return Pointer to the allocated memory (must be freed with free())
     */
    fun copyToHeap(data: ByteArray): Int {
        val ptr = malloc(data.size)
        if (ptr == 0) return 0
        
        val heap = jsLibOpenMptHeapU8()
        for (i in data.indices) {
            jsUint8ArraySet(heap, ptr + i, data[i].toInt() and 0xFF)
        }
        return ptr
    }
    
    /**
     * Copy a string to the WASM heap as UTF-8.
     * @return Pointer to the allocated memory (must be freed with free())
     */
    fun stringToHeap(str: String): Int {
        val len = jsLibOpenMptLengthBytesUTF8(str) + 1
        val ptr = malloc(len)
        if (ptr == 0) return 0
        jsLibOpenMptStringToUTF8(str, ptr, len)
        return ptr
    }
    
    /**
     * Read a string from the WASM heap.
     */
    fun stringFromHeap(ptr: Int): String {
        return if (ptr == 0) "" else jsLibOpenMptUTF8ToString(ptr)
    }
    
    /**
     * Create a module from a ByteArray.
     * @return Module handle, or 0 on failure
     */
    fun createModule(data: ByteArray): Int {
        val dataPtr = copyToHeap(data)
        if (dataPtr == 0) return 0
        
        try {
            val mod = jsLibOpenMptModuleCreateFromMemory2(
                data = dataPtr,
                size = data.size,
                logfunc = 0,
                loguser = 0,
                errfunc = 0,
                erruser = 0,
                error = 0,
                errorMessage = 0,
                ctls = 0
            )
            return mod
        } finally {
            free(dataPtr)
        }
    }
    
    /**
     * Destroy a module.
     */
    fun destroyModule(mod: Int) {
        if (mod != 0) {
            jsLibOpenMptModuleDestroy(mod)
        }
    }
    
    /**
     * Read interleaved stereo samples into separate left/right Float32Arrays.
     * @param mod Module handle
     * @param sampleRate Sample rate
     * @param frameCount Number of frames to read
     * @param outputLeft Left channel output (JsFloat32Array)
     * @param outputRight Right channel output (JsFloat32Array)
     * @return Number of frames actually read
     */
    fun readInterleavedStereo(mod: Int, sampleRate: Int, frameCount: Int, outputLeft: JsFloat32Array, outputRight: JsFloat32Array): Int {
        // Allocate buffer in WASM heap (2 channels * frameCount floats * 4 bytes per float)
        val bufferSize = frameCount * 2 * 4
        val bufferPtr = malloc(bufferSize)
        if (bufferPtr == 0) return 0
        
        try {
            val framesRead = jsLibOpenMptModuleReadInterleavedFloatStereo(
                mod, sampleRate, frameCount, bufferPtr
            )
            
            // Copy from WASM heap to output buffers and de-interleave
            val heapF32 = jsLibOpenMptHeapF32()
            val heapOffset = bufferPtr / 4  // HEAPF32 is indexed by float (4 bytes)
            for (i in 0 until framesRead) {
                jsFloat32ArraySet(outputLeft, i, jsFloat32ArrayGet(heapF32, heapOffset + i * 2))
                jsFloat32ArraySet(outputRight, i, jsFloat32ArrayGet(heapF32, heapOffset + i * 2 + 1))
            }
            
            return framesRead
        } finally {
            free(bufferPtr)
        }
    }
    
    /**
     * Get module duration in seconds.
     */
    fun getDurationSeconds(mod: Int): Double = jsLibOpenMptModuleGetDurationSeconds(mod)
    
    /**
     * Get current position in seconds.
     */
    fun getPositionSeconds(mod: Int): Double = jsLibOpenMptModuleGetPositionSeconds(mod)
    
    /**
     * Set position in seconds.
     */
    fun setPositionSeconds(mod: Int, seconds: Double): Double = 
        jsLibOpenMptModuleSetPositionSeconds(mod, seconds)
    
    /**
     * Get repeat count.
     */
    fun getRepeatCount(mod: Int): Int = jsLibOpenMptModuleGetRepeatCount(mod)
    
    /**
     * Set repeat count.
     */
    fun setRepeatCount(mod: Int, count: Int) {
        jsLibOpenMptModuleSetRepeatCount(mod, count)
    }
    
    /**
     * Set a render parameter.
     */
    fun setRenderParam(mod: Int, param: Int, value: Int) {
        jsLibOpenMptModuleSetRenderParam(mod, param, value)
    }
    
    /**
     * Get a floating-point control value.
     */
    fun ctlGetFloat(mod: Int, ctl: String): Double {
        val ctlPtr = stringToHeap(ctl)
        if (ctlPtr == 0) return 0.0
        try {
            return jsLibOpenMptModuleCtlGetFloatingpoint(mod, ctlPtr)
        } finally {
            free(ctlPtr)
        }
    }
    
    /**
     * Set a floating-point control value.
     */
    fun ctlSetFloat(mod: Int, ctl: String, value: Double): Boolean {
        val ctlPtr = stringToHeap(ctl)
        if (ctlPtr == 0) return false
        try {
            return jsLibOpenMptModuleCtlSetFloatingpoint(mod, ctlPtr, value) == 1
        } finally {
            free(ctlPtr)
        }
    }
    
    /**
     * Get metadata value by key.
     */
    fun getMetadata(mod: Int, key: String): String {
        val keyPtr = stringToHeap(key)
        if (keyPtr == 0) return ""
        
        try {
            val resultPtr = jsLibOpenMptModuleGetMetadata(mod, keyPtr)
            if (resultPtr == 0) return ""
            
            try {
                return stringFromHeap(resultPtr)
            } finally {
                jsLibOpenMptFreeString(resultPtr)
            }
        } finally {
            free(keyPtr)
        }
    }
    
    // Module info
    fun getNumChannels(mod: Int): Int = jsLibOpenMptModuleGetNumChannels(mod)
    fun getNumOrders(mod: Int): Int = jsLibOpenMptModuleGetNumOrders(mod)
    fun getNumPatterns(mod: Int): Int = jsLibOpenMptModuleGetNumPatterns(mod)
    fun getNumInstruments(mod: Int): Int = jsLibOpenMptModuleGetNumInstruments(mod)
    fun getNumSamples(mod: Int): Int = jsLibOpenMptModuleGetNumSamples(mod)
    
    // Current state
    fun getCurrentOrder(mod: Int): Int = jsLibOpenMptModuleGetCurrentOrder(mod)
    fun getCurrentPattern(mod: Int): Int = jsLibOpenMptModuleGetCurrentPattern(mod)
    fun getCurrentRow(mod: Int): Int = jsLibOpenMptModuleGetCurrentRow(mod)
}
