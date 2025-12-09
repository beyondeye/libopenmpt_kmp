#pragma once

#include <libopenmpt/libopenmpt.h>
#include <oboe/Oboe.h>
#include <memory>
#include <atomic>
#include <mutex>
#include <vector>
#include <string>

/**
 * ModPlayerEngine - Core C++ engine for MOD music playback
 * 
 * This class integrates libopenmpt (for MOD rendering) with Oboe (for audio output).
 * It manages the audio stream, renders audio in real-time, and provides thread-safe
 * playback control.
 */
class ModPlayerEngine : public oboe::AudioStreamDataCallback,
                        public oboe::AudioStreamErrorCallback {
public:
    ModPlayerEngine();
    ~ModPlayerEngine();

    // ========== Module Management ==========
    
    /**
     * Load a module from memory buffer
     * @param data Pointer to module file data
     * @param size Size of the data in bytes
     * @return true if successfully loaded
     */
    bool loadModule(const uint8_t* data, size_t size);
    
    /**
     * Load a module from a file path
     * @param path File path to the module
     * @return true if successfully loaded
     */
    bool loadModuleFromFile(const char* path);
    
    /**
     * Unload the current module and free resources
     */
    void unloadModule();

    // ========== Playback Control ==========
    
    /**
     * Start or resume playback
     * @return true if successful
     */
    bool play();
    
    /**
     * Pause playback
     */
    void pause();
    
    /**
     * Stop playback and reset position to start
     */
    void stop();
    
    /**
     * Seek to a specific position
     * @param positionSeconds Position in seconds
     */
    void seek(double positionSeconds);

    // ========== Configuration ==========
    
    /**
     * Set repeat count
     * @param count -1 for infinite, 0 for once, n for n repeats
     */
    void setRepeatCount(int32_t count);
    
    /**
     * Set master gain
     * @param gainMillibel Gain in millibels
     */
    void setMasterGain(int32_t gainMillibel);
    
    /**
     * Set stereo separation.
     * @param percent Separation percentage (0-200, default 100)
     */
    void setStereoSeparation(int32_t percent);
    
    /**
     * Set tempo factor (playback speed without changing pitch)
     * @param factor Tempo factor (0.25 to 2.0, 1.0 = normal)
     */
    void setTempoFactor(double factor);
    
    /**
     * Get current tempo factor
     * @return Current tempo factor
     */
    double getTempoFactor() const;
    
    /**
     * Set pitch factor (pitch without changing tempo)
     * @param factor Pitch factor (0.25 to 2.0, 1.0 = normal)
     */
    void setPitchFactor(double factor);
    
    /**
     * Get current pitch factor
     * @return Current pitch factor
     */
    double getPitchFactor() const;

    // ========== State Queries ==========
    
    /**
     * Check if currently playing
     */
    bool isPlaying() const;
    
    /**
     * Get current playback position in seconds
     */
    double getPositionSeconds() const;
    
    /**
     * Get total duration in seconds
     */
    double getDurationSeconds() const;

    // ========== Metadata Queries ==========
    
    /**
     * Get metadata value for a given key
     * @param key Metadata key (e.g., "title", "artist", "type")
     * @return Metadata value string (caller must free with openmpt_free_string)
     */
    const char* getMetadata(const char* key);
    
    /**
     * Get current order position
     */
    int32_t getCurrentOrder();
    
    /**
     * Get current pattern being played
     */
    int32_t getCurrentPattern();
    
    /**
     * Get current row in pattern
     */
    int32_t getCurrentRow();
    
    /**
     * Get number of channels
     */
    int32_t getNumChannels();
    
    /**
     * Get number of patterns
     */
    int32_t getNumPatterns();
    
    /**
     * Get number of orders
     */
    int32_t getNumOrders();
    
    /**
     * Get number of instruments
     */
    int32_t getNumInstruments();
    
    /**
     * Get number of samples
     */
    int32_t getNumSamples();

    // ========== Oboe Callbacks ==========
    
    /**
     * Called by Oboe when audio frames are needed
     */
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* stream,
        void* audioData,
        int32_t numFrames) override;
    
    /**
     * Called by Oboe when an error occurs after stream is closed
     */
    void onErrorAfterClose(
        oboe::AudioStream* stream,
        oboe::Result error) override;

private:
    // Audio stream management
    void createAudioStream();
    void destroyAudioStream();
    
    // Module object from libopenmpt
    openmpt_module* module_;
    
    // Oboe audio stream
    std::shared_ptr<oboe::AudioStream> stream_;
    
    // Playback state
    std::atomic<bool> playing_;
    std::atomic<bool> shouldStop_;
    
    // Thread synchronization
    std::mutex moduleMutex_;
    
    // Audio configuration
    static constexpr int32_t SAMPLE_RATE = 48000;
    static constexpr int32_t CHANNEL_COUNT = 2;  // Stereo
    static constexpr oboe::AudioFormat AUDIO_FORMAT = oboe::AudioFormat::Float;
};
