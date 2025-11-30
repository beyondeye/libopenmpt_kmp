package com.beyondeye.openmptdemo.player

/**
 * Base exception class for MOD player errors.
 * This hierarchy provides type-safe error handling.
 */
sealed class ModPlayerException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    /**
     * Failed to load module file
     */
    class LoadFailed(message: String, cause: Throwable? = null) : ModPlayerException(message, cause)
    
    /**
     * Failed to initialize the player engine
     */
    class InitializationFailed(message: String, cause: Throwable? = null) : ModPlayerException(message, cause)
    
    /**
     * Invalid operation for current state (e.g., playing when no module is loaded)
     */
    class InvalidOperation(message: String) : ModPlayerException(message)
    
    /**
     * Native library not available
     */
    class NativeLibraryError(message: String, cause: Throwable? = null) : ModPlayerException(message, cause)
}
