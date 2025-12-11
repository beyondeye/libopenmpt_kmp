/**
 * JNI wrapper for libopenmpt on Desktop JVM.
 * 
 * This file provides JNI bindings that directly call the libopenmpt C API.
 * Unlike the Android implementation, this doesn't include audio output -
 * audio playback is handled in Kotlin using JavaSound.
 */

#include <jni.h>
#include <cstring>
#include <cstdio>
#include <fstream>
#include <vector>
#include <mutex>

// libopenmpt header
#include <libopenmpt/libopenmpt.h>

// Simple logging macros for desktop
#define LOG_TAG "DesktopModPlayerJNI"
#define LOGD(...) printf("[DEBUG] " LOG_TAG ": " __VA_ARGS__); printf("\n")
#define LOGE(...) fprintf(stderr, "[ERROR] " LOG_TAG ": " __VA_ARGS__); fprintf(stderr, "\n")
#define LOGI(...) printf("[INFO] " LOG_TAG ": " __VA_ARGS__); printf("\n")

/**
 * Native handle structure that holds the openmpt module and associated state.
 */
struct ModuleHandle {
    openmpt_module* module;
    std::mutex mutex;
    
    ModuleHandle() : module(nullptr) {}
    ~ModuleHandle() {
        if (module) {
            openmpt_module_destroy(module);
            module = nullptr;
        }
    }
};

// Helper function to convert ModuleHandle pointer to jlong handle
static inline jlong handle_to_jlong(ModuleHandle* handle) {
    return reinterpret_cast<jlong>(handle);
}

// Helper function to convert jlong handle to ModuleHandle pointer
static inline ModuleHandle* jlong_to_handle(jlong handle) {
    return reinterpret_cast<ModuleHandle*>(handle);
}

extern "C" {

// ========== Lifecycle ==========

JNIEXPORT jlong JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeCreate(
        JNIEnv* env, jobject thiz) {
    LOGD("Creating native ModuleHandle");
    ModuleHandle* handle = new ModuleHandle();
    return handle_to_jlong(handle);
}

JNIEXPORT void JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeDestroy(
        JNIEnv* env, jobject thiz, jlong handle) {
    LOGD("Destroying native ModuleHandle");
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (moduleHandle) {
        delete moduleHandle;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeLoadModule(
        JNIEnv* env, jobject thiz, jlong handle, jbyteArray data) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle) {
        LOGE("Invalid handle");
        return JNI_FALSE;
    }
    
    std::lock_guard<std::mutex> lock(moduleHandle->mutex);
    
    // Unload any existing module
    if (moduleHandle->module) {
        openmpt_module_destroy(moduleHandle->module);
        moduleHandle->module = nullptr;
    }
    
    jsize size = env->GetArrayLength(data);
    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    
    // Load module from memory
    moduleHandle->module = openmpt_module_create_from_memory2(
        bytes, size,
        openmpt_log_func_default, nullptr,
        openmpt_error_func_default, nullptr,
        nullptr, nullptr, nullptr
    );
    
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    
    if (!moduleHandle->module) {
        LOGE("Failed to load module from memory");
        return JNI_FALSE;
    }
    
    LOGI("Module loaded successfully");
    const char* title = openmpt_module_get_metadata(moduleHandle->module, "title");
    const char* type = openmpt_module_get_metadata(moduleHandle->module, "type_long");
    LOGI("Title: %s", title ? title : "(unknown)");
    LOGI("Type: %s", type ? type : "(unknown)");
    LOGI("Duration: %.2f seconds", openmpt_module_get_duration_seconds(moduleHandle->module));
    
    if (title) openmpt_free_string(title);
    if (type) openmpt_free_string(type);
    
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeLoadModuleFromPath(
        JNIEnv* env, jobject thiz, jlong handle, jstring path) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle) {
        LOGE("Invalid handle");
        return JNI_FALSE;
    }
    
    const char* pathStr = env->GetStringUTFChars(path, nullptr);
    LOGD("Loading module from file: %s", pathStr);
    
    // Read file into memory
    std::ifstream file(pathStr, std::ios::binary | std::ios::ate);
    env->ReleaseStringUTFChars(path, pathStr);
    
    if (!file.is_open()) {
        LOGE("Failed to open file");
        return JNI_FALSE;
    }
    
    std::streamsize size = file.tellg();
    file.seekg(0, std::ios::beg);
    
    std::vector<uint8_t> buffer(size);
    if (!file.read(reinterpret_cast<char*>(buffer.data()), size)) {
        LOGE("Failed to read file");
        return JNI_FALSE;
    }
    
    std::lock_guard<std::mutex> lock(moduleHandle->mutex);
    
    // Unload any existing module
    if (moduleHandle->module) {
        openmpt_module_destroy(moduleHandle->module);
        moduleHandle->module = nullptr;
    }
    
    // Load module from memory
    moduleHandle->module = openmpt_module_create_from_memory2(
        buffer.data(), buffer.size(),
        openmpt_log_func_default, nullptr,
        openmpt_error_func_default, nullptr,
        nullptr, nullptr, nullptr
    );
    
    if (!moduleHandle->module) {
        LOGE("Failed to load module from file");
        return JNI_FALSE;
    }
    
    LOGI("Module loaded from file successfully");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeUnloadModule(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle) return;
    
    std::lock_guard<std::mutex> lock(moduleHandle->mutex);
    
    if (moduleHandle->module) {
        LOGD("Unloading module");
        openmpt_module_destroy(moduleHandle->module);
        moduleHandle->module = nullptr;
    }
}

// ========== Audio Rendering ==========

/**
 * Render audio from the module into a float array (interleaved stereo).
 * 
 * @param handle Module handle
 * @param sampleRate Sample rate in Hz (e.g., 48000)
 * @param numFrames Number of frames to render
 * @return Float array with interleaved stereo samples, or null if no module loaded
 */
JNIEXPORT jfloatArray JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeReadAudio(
        JNIEnv* env, jobject thiz, jlong handle, jint sampleRate, jint numFrames) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) {
        return nullptr;
    }
    
    std::lock_guard<std::mutex> lock(moduleHandle->mutex);
    
    // Create output array (stereo = 2 channels)
    const int numSamples = numFrames * 2;
    jfloatArray result = env->NewFloatArray(numSamples);
    if (!result) {
        LOGE("Failed to allocate float array");
        return nullptr;
    }
    
    // Get direct access to the array
    jfloat* buffer = env->GetFloatArrayElements(result, nullptr);
    
    // Render audio from libopenmpt
    size_t framesRendered = openmpt_module_read_interleaved_float_stereo(
        moduleHandle->module,
        sampleRate,
        numFrames,
        buffer
    );
    
    // If we rendered fewer frames than requested, fill the rest with silence
    if (framesRendered < static_cast<size_t>(numFrames)) {
        size_t silenceStart = framesRendered * 2;
        size_t silenceCount = (numFrames - framesRendered) * 2;
        memset(buffer + silenceStart, 0, silenceCount * sizeof(float));
    }
    
    env->ReleaseFloatArrayElements(result, buffer, 0);
    
    return result;
}

// ========== Position Control ==========

JNIEXPORT void JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeSeek(
        JNIEnv* env, jobject thiz, jlong handle, jdouble positionSeconds) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return;
    
    std::lock_guard<std::mutex> lock(moduleHandle->mutex);
    openmpt_module_set_position_seconds(moduleHandle->module, positionSeconds);
    LOGD("Seeked to %.2f seconds", positionSeconds);
}

JNIEXPORT jdouble JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeGetPositionSeconds(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return 0.0;
    
    // Read-only, no lock needed
    return openmpt_module_get_position_seconds(moduleHandle->module);
}

JNIEXPORT jdouble JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeGetDurationSeconds(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return 0.0;
    
    return openmpt_module_get_duration_seconds(moduleHandle->module);
}

// ========== Configuration ==========

JNIEXPORT void JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeSetRepeatCount(
        JNIEnv* env, jobject thiz, jlong handle, jint count) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return;
    
    std::lock_guard<std::mutex> lock(moduleHandle->mutex);
    openmpt_module_set_repeat_count(moduleHandle->module, count);
    LOGD("Repeat count set to %d", count);
}

JNIEXPORT void JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeSetMasterGain(
        JNIEnv* env, jobject thiz, jlong handle, jint gainMillibel) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return;
    
    std::lock_guard<std::mutex> lock(moduleHandle->mutex);
    openmpt_module_set_render_param(moduleHandle->module, 
        OPENMPT_MODULE_RENDER_MASTERGAIN_MILLIBEL, gainMillibel);
    LOGD("Master gain set to %d mB", gainMillibel);
}

JNIEXPORT void JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeSetStereoSeparation(
        JNIEnv* env, jobject thiz, jlong handle, jint percent) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return;
    
    std::lock_guard<std::mutex> lock(moduleHandle->mutex);
    openmpt_module_set_render_param(moduleHandle->module, 
        OPENMPT_MODULE_RENDER_STEREOSEPARATION_PERCENT, percent);
    LOGD("Stereo separation set to %d%%", percent);
}

JNIEXPORT void JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeSetTempoFactor(
        JNIEnv* env, jobject thiz, jlong handle, jdouble factor) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return;
    
    std::lock_guard<std::mutex> lock(moduleHandle->mutex);
    openmpt_module_ctl_set_floatingpoint(moduleHandle->module, "play.tempo_factor", factor);
    LOGD("Tempo factor set to %.2f", factor);
}

JNIEXPORT jdouble JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeGetTempoFactor(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return 1.0;
    
    return openmpt_module_ctl_get_floatingpoint(moduleHandle->module, "play.tempo_factor");
}

JNIEXPORT void JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeSetPitchFactor(
        JNIEnv* env, jobject thiz, jlong handle, jdouble factor) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return;
    
    std::lock_guard<std::mutex> lock(moduleHandle->mutex);
    openmpt_module_ctl_set_floatingpoint(moduleHandle->module, "play.pitch_factor", factor);
    LOGD("Pitch factor set to %.2f", factor);
}

JNIEXPORT jdouble JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeGetPitchFactor(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return 1.0;
    
    return openmpt_module_ctl_get_floatingpoint(moduleHandle->module, "play.pitch_factor");
}

// ========== Metadata Queries ==========

JNIEXPORT jstring JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeGetMetadata(
        JNIEnv* env, jobject thiz, jlong handle, jstring key) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) {
        return env->NewStringUTF("");
    }
    
    const char* keyStr = env->GetStringUTFChars(key, nullptr);
    const char* value = openmpt_module_get_metadata(moduleHandle->module, keyStr);
    env->ReleaseStringUTFChars(key, keyStr);
    
    jstring result = env->NewStringUTF(value ? value : "");
    
    if (value) {
        openmpt_free_string(value);
    }
    
    return result;
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeGetCurrentOrder(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return -1;
    
    return openmpt_module_get_current_order(moduleHandle->module);
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeGetCurrentPattern(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return -1;
    
    return openmpt_module_get_current_pattern(moduleHandle->module);
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeGetCurrentRow(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return -1;
    
    return openmpt_module_get_current_row(moduleHandle->module);
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeGetNumChannels(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return 0;
    
    return openmpt_module_get_num_channels(moduleHandle->module);
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeGetNumPatterns(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return 0;
    
    return openmpt_module_get_num_patterns(moduleHandle->module);
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeGetNumOrders(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return 0;
    
    return openmpt_module_get_num_orders(moduleHandle->module);
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeGetNumInstruments(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return 0;
    
    return openmpt_module_get_num_instruments(moduleHandle->module);
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmpt_core_DesktopModPlayerNative_nativeGetNumSamples(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModuleHandle* moduleHandle = jlong_to_handle(handle);
    if (!moduleHandle || !moduleHandle->module) return 0;
    
    return openmpt_module_get_num_samples(moduleHandle->module);
}

} // extern "C"
