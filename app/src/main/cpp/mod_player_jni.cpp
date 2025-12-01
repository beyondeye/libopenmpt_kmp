#include <jni.h>
#include <android/log.h>
#include "ModPlayerEngine.h"
#include <string>

#define LOG_TAG "ModPlayerJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Helper function to convert ModPlayerEngine pointer to jlong handle
static inline jlong engine_to_handle(ModPlayerEngine* engine) {
    return reinterpret_cast<jlong>(engine);
}

// Helper function to convert jlong handle to ModPlayerEngine pointer
static inline ModPlayerEngine* handle_to_engine(jlong handle) {
    return reinterpret_cast<ModPlayerEngine*>(handle);
}

extern "C" {

// ========== Lifecycle ==========

JNIEXPORT jlong JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeCreate(
        JNIEnv* env, jobject thiz) {
    LOGD("Creating native ModPlayerEngine");
    ModPlayerEngine* engine = new ModPlayerEngine();
    return engine_to_handle(engine);
}

JNIEXPORT void JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeDestroy(
        JNIEnv* env, jobject thiz, jlong handle) {
    LOGD("Destroying native ModPlayerEngine");
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (engine) {
        delete engine;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeLoadModule(
        JNIEnv* env, jobject thiz, jlong handle, jbyteArray data) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) {
        LOGE("Invalid engine handle");
        return JNI_FALSE;
    }
    
    jsize size = env->GetArrayLength(data);
    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    
    bool result = engine->loadModule(reinterpret_cast<const uint8_t*>(bytes), size);
    
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeLoadModuleFromPath(
        JNIEnv* env, jobject thiz, jlong handle, jstring path) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) {
        LOGE("Invalid engine handle");
        return JNI_FALSE;
    }
    
    const char* pathStr = env->GetStringUTFChars(path, nullptr);
    bool result = engine->loadModuleFromFile(pathStr);
    env->ReleaseStringUTFChars(path, pathStr);
    
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeUnloadModule(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (engine) {
        engine->unloadModule();
    }
}

// ========== Playback Control ==========

JNIEXPORT jboolean JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativePlay(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) {
        LOGE("Invalid engine handle");
        return JNI_FALSE;
    }
    
    return engine->play() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativePause(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (engine) {
        engine->pause();
    }
}

JNIEXPORT void JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeStop(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (engine) {
        engine->stop();
    }
}

JNIEXPORT void JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeSeek(
        JNIEnv* env, jobject thiz, jlong handle, jdouble positionSeconds) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (engine) {
        engine->seek(positionSeconds);
    }
}

// ========== Configuration ==========

JNIEXPORT void JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeSetRepeatCount(
        JNIEnv* env, jobject thiz, jlong handle, jint count) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (engine) {
        engine->setRepeatCount(count);
    }
}

JNIEXPORT void JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeSetMasterGain(
        JNIEnv* env, jobject thiz, jlong handle, jint gainMillibel) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (engine) {
        engine->setMasterGain(gainMillibel);
    }
}

JNIEXPORT void JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeSetStereoSeparation(
        JNIEnv* env, jobject thiz, jlong handle, jint percent) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (engine) {
        engine->setStereoSeparation(percent);
    }
}

JNIEXPORT void JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeSetTempoFactor(
        JNIEnv* env, jobject thiz, jlong handle, jdouble factor) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (engine) {
        engine->setTempoFactor(factor);
    }
}

JNIEXPORT jdouble JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeGetTempoFactor(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) return 1.0;
    
    return engine->getTempoFactor();
}

JNIEXPORT void JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeSetPitchFactor(
        JNIEnv* env, jobject thiz, jlong handle, jdouble factor) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (engine) {
        engine->setPitchFactor(factor);
    }
}

JNIEXPORT jdouble JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeGetPitchFactor(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) return 1.0;
    
    return engine->getPitchFactor();
}

// ========== State Queries ==========

JNIEXPORT jboolean JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeIsPlaying(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) return JNI_FALSE;
    
    return engine->isPlaying() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jdouble JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeGetPositionSeconds(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) return 0.0;
    
    return engine->getPositionSeconds();
}

JNIEXPORT jdouble JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeGetDurationSeconds(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) return 0.0;
    
    return engine->getDurationSeconds();
}

// ========== Metadata Queries ==========

JNIEXPORT jstring JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeGetMetadata(
        JNIEnv* env, jobject thiz, jlong handle, jstring key) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) return env->NewStringUTF("");
    
    const char* keyStr = env->GetStringUTFChars(key, nullptr);
    const char* value = engine->getMetadata(keyStr);
    env->ReleaseStringUTFChars(key, keyStr);
    
    jstring result = env->NewStringUTF(value ? value : "");
    
    // Free the string returned by libopenmpt
    if (value) {
        openmpt_free_string(value);
    }
    
    return result;
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeGetCurrentOrder(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) return -1;
    
    return engine->getCurrentOrder();
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeGetCurrentPattern(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) return -1;
    
    return engine->getCurrentPattern();
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeGetCurrentRow(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) return -1;
    
    return engine->getCurrentRow();
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeGetNumChannels(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) return 0;
    
    return engine->getNumChannels();
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeGetNumPatterns(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) return 0;
    
    return engine->getNumPatterns();
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeGetNumOrders(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) return 0;
    
    return engine->getNumOrders();
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeGetNumInstruments(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) return 0;
    
    return engine->getNumInstruments();
}

JNIEXPORT jint JNICALL
Java_com_beyondeye_openmptdemo_player_ModPlayerNative_nativeGetNumSamples(
        JNIEnv* env, jobject thiz, jlong handle) {
    ModPlayerEngine* engine = handle_to_engine(handle);
    if (!engine) return 0;
    
    return engine->getNumSamples();
}

} // extern "C"
