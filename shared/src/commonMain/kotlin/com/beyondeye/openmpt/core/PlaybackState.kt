package com.beyondeye.openmpt.core

/**
 * Represents the various states of the MOD player.
 * This sealed class hierarchy provides type-safe state management.
 */
sealed class PlaybackState {
    /**
     * Player is idle, no module loaded
     */
    object Idle : PlaybackState()

    /**
     * Module is currently loading
     */
    object Loading : PlaybackState()

    /**
     * Module has been loaded successfully and is ready to play
     */
    data class Loaded(val metadata: ModMetadata) : PlaybackState()

    /**
     * Module is currently playing
     */
    object Playing : PlaybackState()

    /**
     * Playback is paused
     */
    object Paused : PlaybackState()

    /**
     * Playback has been stopped
     */
    object Stopped : PlaybackState()

    /**
     * An error occurred
     */
    data class Error(val message: String, val cause: Throwable? = null) : PlaybackState()
}
