package com.beyondeye.openmptdemo.player

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic interface for MOD music playback.
 * 
 * This interface defines the core contract for playing tracker music (MOD, XM, IT, S3M, etc.).
 * It is designed to be KMP-ready, using only Kotlin stdlib and kotlinx.coroutines types.
 * 
 * Platform-specific implementations:
 * - Android: Uses JNI + libopenmpt + Oboe
 * - iOS: Will use Kotlin/Native + libopenmpt + CoreAudio
 * - JVM: Will use JNI + libopenmpt + JavaSound
 */
interface ModPlayer {
    
    // ========== Lifecycle ==========
    
    /**
     * Load a module from a byte array.
     * @param data The complete module file data
     * @return true if loaded successfully, false otherwise
     * @throws ModPlayerException.LoadFailed if loading fails
     */
    fun loadModule(data: ByteArray): Boolean
    
    /**
     * Load a module from a file path.
     * @param path Absolute path to the module file
     * @return true if loaded successfully, false otherwise
     * @throws ModPlayerException.LoadFailed if loading fails
     */
    fun loadModuleFromPath(path: String): Boolean
    
    /**
     * Release all resources associated with the player.
     * Must be called when the player is no longer needed.
     */
    fun release()
    
    // ========== Playback Control ==========
    
    /**
     * Start or resume playback.
     * @throws ModPlayerException.InvalidOperation if no module is loaded
     */
    fun play()
    
    /**
     * Pause playback.
     * @throws ModPlayerException.InvalidOperation if not playing
     */
    fun pause()
    
    /**
     * Stop playback and reset position to the beginning.
     */
    fun stop()
    
    /**
     * Seek to a specific position in the module.
     * @param positionSeconds Position in seconds (0.0 to durationSeconds)
     */
    fun seek(positionSeconds: Double)
    
    // ========== Configuration ==========
    
    /**
     * Set the number of times the module should repeat.
     * @param count -1 for infinite repeat, 0 for no repeat (play once), n for n repeats
     */
    fun setRepeatCount(count: Int)
    
    /**
     * Set the master gain (volume).
     * @param gainMillibel Gain in millibels (0 = normal, negative = quieter, positive = louder)
     */
    fun setMasterGain(gainMillibel: Int)
    
    /**
     * Set stereo separation.
     * @param percent Stereo separation percentage (0-200, default 100)
     */
    fun setStereoSeparation(percent: Int)
    
    // ========== State Queries ==========
    
    /**
     * Current playback state.
     */
    val playbackState: PlaybackState
    
    /**
     * Whether the module is currently playing.
     */
    val isPlaying: Boolean
    
    /**
     * Current playback position in seconds.
     */
    val positionSeconds: Double
    
    /**
     * Total duration of the module in seconds.
     */
    val durationSeconds: Double
    
    // ========== Module Information ==========
    
    /**
     * Get metadata about the currently loaded module.
     * @return ModMetadata object, or default values if no module is loaded
     */
    fun getMetadata(): ModMetadata
    
    /**
     * Get the current order position.
     * @return Current order index, or -1 if no module is loaded
     */
    fun getCurrentOrder(): Int
    
    /**
     * Get the current pattern being played.
     * @return Current pattern index, or -1 if no module is loaded
     */
    fun getCurrentPattern(): Int
    
    /**
     * Get the current row in the pattern.
     * @return Current row index, or -1 if no module is loaded
     */
    fun getCurrentRow(): Int
    
    /**
     * Get the number of channels in the module.
     * @return Number of channels, or 0 if no module is loaded
     */
    fun getNumChannels(): Int
    
    // ========== Reactive State Observers ==========
    
    /**
     * Flow of playback state changes.
     * Subscribe to this to reactively update UI when state changes.
     */
    val playbackStateFlow: StateFlow<PlaybackState>
    
    /**
     * Flow of position updates (in seconds).
     * Updates periodically during playback.
     */
    val positionFlow: StateFlow<Double>
}
