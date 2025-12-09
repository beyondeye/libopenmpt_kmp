package com.beyondeye.openmpt.core

/**
 * Metadata information about a MOD music module.
 * This data class contains all relevant information extracted from the module file.
 */
data class ModMetadata(
    val title: String = "",
    val artist: String = "",
    val type: String = "",              // e.g., "it", "xm", "mod", "s3m"
    val typeLong: String = "",          // e.g., "Impulse Tracker", "FastTracker 2"
    val tracker: String = "",           // Tracker used to create the module
    val message: String = "",           // Module message/comments
    val durationSeconds: Double = 0.0,  // Estimated duration in seconds
    val numChannels: Int = 0,           // Number of channels
    val numPatterns: Int = 0,           // Number of patterns
    val numOrders: Int = 0,             // Number of orders in the sequence
    val numInstruments: Int = 0,        // Number of instruments
    val numSamples: Int = 0             // Number of samples
)
