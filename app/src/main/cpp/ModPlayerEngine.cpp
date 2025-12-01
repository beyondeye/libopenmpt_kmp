#include "ModPlayerEngine.h"
#include <android/log.h>
#include <cstring>
#include <fstream>
#include <vector>

#define LOG_TAG "ModPlayerEngine"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

ModPlayerEngine::ModPlayerEngine()
    : module_(nullptr),
      stream_(nullptr),
      playing_(false),
      shouldStop_(false) {
    LOGD("ModPlayerEngine created");
}

ModPlayerEngine::~ModPlayerEngine() {
    LOGD("ModPlayerEngine destroyed");
    unloadModule();
    destroyAudioStream();
}

// ========== Module Management ==========

bool ModPlayerEngine::loadModule(const uint8_t* data, size_t size) {
    std::lock_guard<std::mutex> lock(moduleMutex_);
    
    // Unload any existing module
    if (module_) {
        openmpt_module_destroy(module_);
        module_ = nullptr;
    }
    
    // Load new module from memory
    module_ = openmpt_module_create_from_memory2(
        data, size,
        openmpt_log_func_default, nullptr,
        openmpt_error_func_default, nullptr,
        nullptr, nullptr, nullptr
    );
    
    if (!module_) {
        LOGE("Failed to load module from memory");
        return false;
    }
    
    LOGI("Module loaded successfully");
    LOGI("Title: %s", openmpt_module_get_metadata(module_, "title"));
    LOGI("Type: %s", openmpt_module_get_metadata(module_, "type_long"));
    LOGI("Duration: %.2f seconds", openmpt_module_get_duration_seconds(module_));
    
    // Create audio stream if not already created
    if (!stream_) {
        createAudioStream();
    }
    
    return true;
}

bool ModPlayerEngine::loadModuleFromFile(const char* path) {
    LOGD("Loading module from file: %s", path);
    
    // Read file into memory
    std::ifstream file(path, std::ios::binary | std::ios::ate);
    if (!file.is_open()) {
        LOGE("Failed to open file: %s", path);
        return false;
    }
    
    std::streamsize size = file.tellg();
    file.seekg(0, std::ios::beg);
    
    std::vector<uint8_t> buffer(size);
    if (!file.read(reinterpret_cast<char*>(buffer.data()), size)) {
        LOGE("Failed to read file: %s", path);
        return false;
    }
    
    return loadModule(buffer.data(), buffer.size());
}

void ModPlayerEngine::unloadModule() {
    std::lock_guard<std::mutex> lock(moduleMutex_);
    
    if (module_) {
        LOGD("Unloading module");
        stop();
        openmpt_module_destroy(module_);
        module_ = nullptr;
    }
}

// ========== Playback Control ==========

bool ModPlayerEngine::play() {
    if (!module_) {
        LOGE("Cannot play: no module loaded");
        return false;
    }
    
    if (!stream_) {
        LOGE("Cannot play: no audio stream");
        return false;
    }
    
    if (playing_.load()) {
        LOGD("Already playing");
        return true;
    }
    
    shouldStop_.store(false);
    playing_.store(true);
    
    oboe::Result result = stream_->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start stream: %s", oboe::convertToText(result));
        playing_.store(false);
        return false;
    }
    
    LOGI("Playback started");
    return true;
}

void ModPlayerEngine::pause() {
    if (!playing_.load()) {
        LOGD("Already paused");
        return;
    }
    
    playing_.store(false);
    
    if (stream_) {
        oboe::Result result = stream_->requestPause();
        if (result != oboe::Result::OK) {
            LOGE("Failed to pause stream: %s", oboe::convertToText(result));
        }
    }
    
    LOGI("Playback paused");
}

void ModPlayerEngine::stop() {
    if (!playing_.load()) {
        LOGD("Already stopped");
        return;
    }
    
    playing_.store(false);
    shouldStop_.store(true);
    
    if (stream_) {
        oboe::Result result = stream_->requestStop();
        if (result != oboe::Result::OK) {
            LOGE("Failed to stop stream: %s", oboe::convertToText(result));
        }
    }
    
    // Reset position to beginning
    if (module_) {
        std::lock_guard<std::mutex> lock(moduleMutex_);
        openmpt_module_set_position_seconds(module_, 0.0);
    }
    
    LOGI("Playback stopped");
}

void ModPlayerEngine::seek(double positionSeconds) {
    if (!module_) {
        LOGE("Cannot seek: no module loaded");
        return;
    }
    
    std::lock_guard<std::mutex> lock(moduleMutex_);
    openmpt_module_set_position_seconds(module_, positionSeconds);
    LOGD("Seeked to %.2f seconds", positionSeconds);
}

// ========== Configuration ==========

void ModPlayerEngine::setRepeatCount(int32_t count) {
    if (!module_) return;
    
    std::lock_guard<std::mutex> lock(moduleMutex_);
    openmpt_module_set_repeat_count(module_, count);
    LOGD("Repeat count set to %d", count);
}

void ModPlayerEngine::setMasterGain(int32_t gainMillibel) {
    if (!module_) return;
    
    std::lock_guard<std::mutex> lock(moduleMutex_);
    openmpt_module_set_render_param(module_, OPENMPT_MODULE_RENDER_MASTERGAIN_MILLIBEL, gainMillibel);
    LOGD("Master gain set to %d mB", gainMillibel);
}

void ModPlayerEngine::setStereoSeparation(int32_t percent) {
    if (!module_) return;
    
    std::lock_guard<std::mutex> lock(moduleMutex_);
    openmpt_module_set_render_param(module_, OPENMPT_MODULE_RENDER_STEREOSEPARATION_PERCENT, percent);
    LOGD("Stereo separation set to %d%%", percent);
}

void ModPlayerEngine::setTempoFactor(double factor) {
    if (!module_) return;
    
    std::lock_guard<std::mutex> lock(moduleMutex_);
    openmpt_module_ctl_set_floatingpoint(module_, "play.tempo_factor", factor);
    LOGD("Tempo factor set to %.2f", factor);
}

double ModPlayerEngine::getTempoFactor() const {
    if (!module_) return 1.0;
    
    return openmpt_module_ctl_get_floatingpoint(module_, "play.tempo_factor");
}

void ModPlayerEngine::setPitchFactor(double factor) {
    if (!module_) return;
    
    std::lock_guard<std::mutex> lock(moduleMutex_);
    openmpt_module_ctl_set_floatingpoint(module_, "play.pitch_factor", factor);
    LOGD("Pitch factor set to %.2f", factor);
}

double ModPlayerEngine::getPitchFactor() const {
    if (!module_) return 1.0;
    
    return openmpt_module_ctl_get_floatingpoint(module_, "play.pitch_factor");
}

// ========== State Queries ==========

bool ModPlayerEngine::isPlaying() const {
    return playing_.load();
}

double ModPlayerEngine::getPositionSeconds() const {
    if (!module_) return 0.0;
    
    // Note: This is thread-safe as it's a read operation
    return openmpt_module_get_position_seconds(module_);
}

double ModPlayerEngine::getDurationSeconds() const {
    if (!module_) return 0.0;
    
    return openmpt_module_get_duration_seconds(module_);
}

// ========== Metadata Queries ==========

const char* ModPlayerEngine::getMetadata(const char* key) {
    if (!module_) return nullptr;
    
    std::lock_guard<std::mutex> lock(moduleMutex_);
    return openmpt_module_get_metadata(module_, key);
}

int32_t ModPlayerEngine::getCurrentOrder() {
    if (!module_) return -1;
    return openmpt_module_get_current_order(module_);
}

int32_t ModPlayerEngine::getCurrentPattern() {
    if (!module_) return -1;
    return openmpt_module_get_current_pattern(module_);
}

int32_t ModPlayerEngine::getCurrentRow() {
    if (!module_) return -1;
    return openmpt_module_get_current_row(module_);
}

int32_t ModPlayerEngine::getNumChannels() {
    if (!module_) return 0;
    return openmpt_module_get_num_channels(module_);
}

int32_t ModPlayerEngine::getNumPatterns() {
    if (!module_) return 0;
    return openmpt_module_get_num_patterns(module_);
}

int32_t ModPlayerEngine::getNumOrders() {
    if (!module_) return 0;
    return openmpt_module_get_num_orders(module_);
}

int32_t ModPlayerEngine::getNumInstruments() {
    if (!module_) return 0;
    return openmpt_module_get_num_instruments(module_);
}

int32_t ModPlayerEngine::getNumSamples() {
    if (!module_) return 0;
    return openmpt_module_get_num_samples(module_);
}

// ========== Oboe Callbacks ==========

oboe::DataCallbackResult ModPlayerEngine::onAudioReady(
        oboe::AudioStream* stream,
        void* audioData,
        int32_t numFrames) {
    
    if (!module_ || !playing_.load()) {
        // Fill with silence
        memset(audioData, 0, numFrames * CHANNEL_COUNT * sizeof(float));
        return oboe::DataCallbackResult::Continue;
    }
    
    // Lock for reading from module
    std::lock_guard<std::mutex> lock(moduleMutex_);
    
    // Render audio from libopenmpt
    size_t framesRendered = openmpt_module_read_interleaved_float_stereo(
        module_,
        SAMPLE_RATE,
        numFrames,
        static_cast<float*>(audioData)
    );
    
    // If we rendered fewer frames than requested, fill the rest with silence
    if (framesRendered < static_cast<size_t>(numFrames)) {
        float* buffer = static_cast<float*>(audioData);
        size_t silenceStart = framesRendered * CHANNEL_COUNT;
        size_t silenceCount = (numFrames - framesRendered) * CHANNEL_COUNT;
        memset(buffer + silenceStart, 0, silenceCount * sizeof(float));
        
        // If we got 0 frames, the module has ended
        if (framesRendered == 0 && !shouldStop_.load()) {
            LOGD("Module playback ended");
            playing_.store(false);
            return oboe::DataCallbackResult::Stop;
        }
    }
    
    return oboe::DataCallbackResult::Continue;
}

void ModPlayerEngine::onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) {
    LOGE("Audio stream error after close: %s", oboe::convertToText(error));
    playing_.store(false);
    
    // Attempt to recreate the stream
    destroyAudioStream();
    createAudioStream();
}

// ========== Private Methods ==========

void ModPlayerEngine::createAudioStream() {
    LOGD("Creating audio stream");
    
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setFormat(AUDIO_FORMAT)
        ->setChannelCount(CHANNEL_COUNT)
        ->setSampleRate(SAMPLE_RATE)
        ->setDataCallback(this)
        ->setErrorCallback(this);
    
    oboe::Result result = builder.openStream(stream_);
    if (result != oboe::Result::OK) {
        LOGE("Failed to create stream: %s", oboe::convertToText(result));
        return;
    }
    
    LOGI("Audio stream created successfully");
    LOGI("Sample rate: %d", stream_->getSampleRate());
    LOGI("Buffer capacity: %d frames", stream_->getBufferCapacityInFrames());
    LOGI("Frames per burst: %d", stream_->getFramesPerBurst());
}

void ModPlayerEngine::destroyAudioStream() {
    if (stream_) {
        LOGD("Destroying audio stream");
        
        if (playing_.load()) {
            stream_->requestStop();
            playing_.store(false);
        }
        
        stream_->close();
        stream_.reset();
    }
}
