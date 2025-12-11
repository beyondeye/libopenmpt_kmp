package com.beyondeye.openmpt.core

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files

/**
 * Cross-platform native library loader for desktop JVM.
 * 
 * This utility extracts native libraries from JAR resources to a temporary directory
 * and loads them using System.load(). This approach works for all distribution methods
 * (JAR, jpackage, etc.) and supports multiple platforms/architectures.
 * 
 * Resource structure expected:
 * resources/native/{os}-{arch}/
 *   - libopenmpt.so / libopenmpt.dylib / openmpt.dll
 *   - libmodplayer_desktop.so / libmodplayer_desktop.dylib / modplayer_desktop.dll
 */
object NativeLibraryLoader {
    
    private var loaded = false
    private var loadError: Throwable? = null
    
    /**
     * Represents the current operating system
     */
    enum class OS {
        LINUX, MACOS, WINDOWS, UNKNOWN
    }
    
    /**
     * Represents the current CPU architecture
     */
    enum class Arch {
        X64, ARM64, UNKNOWN
    }
    
    /**
     * Detected operating system
     */
    val currentOS: OS by lazy {
        val osName = System.getProperty("os.name").lowercase()
        when {
            osName.contains("linux") -> OS.LINUX
            osName.contains("mac") || osName.contains("darwin") -> OS.MACOS
            osName.contains("windows") -> OS.WINDOWS
            else -> OS.UNKNOWN
        }
    }
    
    /**
     * Detected CPU architecture
     */
    val currentArch: Arch by lazy {
        val archName = System.getProperty("os.arch").lowercase()
        when {
            archName.contains("amd64") || archName.contains("x86_64") -> Arch.X64
            archName.contains("aarch64") || archName.contains("arm64") -> Arch.ARM64
            else -> Arch.UNKNOWN
        }
    }
    
    /**
     * Platform identifier string (e.g., "linux-x64", "macos-arm64", "windows-x64")
     */
    val platformId: String by lazy {
        val os = when (currentOS) {
            OS.LINUX -> "linux"
            OS.MACOS -> "macos"
            OS.WINDOWS -> "windows"
            OS.UNKNOWN -> "unknown"
        }
        val arch = when (currentArch) {
            Arch.X64 -> "x64"
            Arch.ARM64 -> "arm64"
            Arch.UNKNOWN -> "unknown"
        }
        "$os-$arch"
    }
    
    /**
     * Library file extension for the current platform
     */
    private val libraryExtension: String by lazy {
        when (currentOS) {
            OS.LINUX -> "so"
            OS.MACOS -> "dylib"
            OS.WINDOWS -> "dll"
            OS.UNKNOWN -> "so"
        }
    }
    
    /**
     * Library prefix for the current platform (empty on Windows)
     */
    private val libraryPrefix: String by lazy {
        when (currentOS) {
            OS.WINDOWS -> ""
            else -> "lib"
        }
    }
    
    /**
     * Temporary directory for extracted libraries
     */
    private val tempDir: File by lazy {
        val dir = Files.createTempDirectory("openmpt-native-").toFile()
        dir.deleteOnExit()
        dir
    }
    
    /**
     * Load all required native libraries.
     * This method is idempotent - calling it multiple times has no effect after first successful load.
     * 
     * @throws ModPlayerException.NativeLibraryError if loading fails
     */
    @Synchronized
    fun loadLibraries() {
        if (loaded) return
        
        loadError?.let { throw it }
        
        try {
            println("NativeLibraryLoader: Platform detected: $platformId")
            println("NativeLibraryLoader: OS=${currentOS}, Arch=${currentArch}")
            
            // Load libraries in dependency order
            // 1. First load libopenmpt (dependency)
            loadLibrary("openmpt")
            
            // 2. Then load modplayer_desktop (our JNI wrapper)
            loadLibrary("modplayer_desktop")
            
            loaded = true
            println("NativeLibraryLoader: All libraries loaded successfully")
        } catch (e: Throwable) {
            loadError = ModPlayerException.NativeLibraryError(
                "Failed to load native libraries for platform $platformId: ${e.message}", e
            )
            throw loadError!!
        }
    }
    
    /**
     * Load a single native library by base name.
     * 
     * @param baseName Library name without prefix/extension (e.g., "openmpt", "modplayer_desktop")
     */
    private fun loadLibrary(baseName: String) {
        val fileName = "$libraryPrefix$baseName.$libraryExtension"
        val resourcePath = "/native/$platformId/$fileName"
        
        println("NativeLibraryLoader: Loading $baseName from $resourcePath")
        
        // Try to find the resource
        val resourceStream: InputStream = javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException(
                "Native library not found in resources: $resourcePath\n" +
                "Make sure the library is placed in: src/desktopMain/resources$resourcePath"
            )
        
        // Extract to temp directory
        val targetFile = File(tempDir, fileName)
        resourceStream.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        
        // Make executable on Unix-like systems
        if (currentOS != OS.WINDOWS) {
            targetFile.setExecutable(true)
        }
        
        targetFile.deleteOnExit()
        
        // Load the library
        println("NativeLibraryLoader: Extracted to ${targetFile.absolutePath}")
        System.load(targetFile.absolutePath)
        println("NativeLibraryLoader: Loaded $baseName successfully")
    }
    
    /**
     * Check if libraries are loaded
     */
    fun isLoaded(): Boolean = loaded
    
    /**
     * Get the last load error, if any
     */
    fun getLoadError(): Throwable? = loadError
}
