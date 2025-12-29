# iOS Implementation for libopenmpt_kmp

This document describes the iOS platform implementation for the libopenmpt Kotlin Multiplatform project.

## Overview

The iOS implementation consists of:
1. **Native library build** - CMake-based build system for compiling libopenmpt as a static library for iOS
2. **Kotlin/Native cinterop** - Bindings to call libopenmpt C API from Kotlin
3. **IosModPlayer** - Kotlin implementation of the ModPlayer interface
4. **IosAudioEngine** - AudioUnit-based audio playback engine

## Building the iOS Native Library

### Prerequisites
- Xcode with iOS SDK
- CMake 3.21+
- macOS

### Gradle Tasks

The following Gradle tasks are available in the `libopenmpt` module:

```bash
# Build libopenmpt for iOS arm64 (physical devices)
./gradlew :libopenmpt:buildIosArm64

# Build libopenmpt for iOS arm64 simulator (M1/M2 Macs)
./gradlew :libopenmpt:buildIosSimArm64

# Create XCFramework from both architectures
./gradlew :libopenmpt:createIosXCFramework

# Copy headers to shared module
./gradlew :libopenmpt:copyIosHeaders

# Export XCFramework to shared module
./gradlew :libopenmpt:exportIosLibs

# Build all iOS artifacts (recommended)
./gradlew :libopenmpt:buildIos
```

### Output Files

After running `./gradlew :libopenmpt:buildIos`, the following files are created:
- `shared/src/iosMain/libs/libopenmpt.xcframework/` - XCFramework with device and simulator libraries
- `shared/src/iosMain/headers/libopenmpt/` - Header files for cinterop

## Cinterop Configuration

The cinterop configuration is defined in:
- `shared/src/nativeInterop/cinterop/libopenmpt.def`

This generates Kotlin bindings for the libopenmpt C API in the `libopenmpt` package.

## Implementation Details

### IosAudioEngine

Located at: `shared/src/iosMain/kotlin/com/beyondeye/openmpt/core/IosAudioEngine.kt`

Features:
- Uses AudioUnit (RemoteIO) for low-latency audio output
- 48kHz sample rate, stereo output
- Float32 audio format
- Callback-based rendering

### IosModPlayer

Located at: `shared/src/iosMain/kotlin/com/beyondeye/openmpt/core/IosModPlayer.kt`

Implements the `ModPlayer` interface with:
- Module loading from ByteArray
- Playback controls (play, pause, stop, seek)
- Repeat count setting
- Master gain and stereo separation
- Tempo and pitch control
- Position tracking via StateFlow
- Metadata retrieval

## Usage Example

```kotlin
// Create player
val player = IosModPlayer()

// Load module from ByteArray
val moduleData: ByteArray = // ... load from resources or network
player.loadModule(moduleData)

// Get metadata
val metadata = player.getMetadata()
println("Title: ${metadata.title}")
println("Duration: ${player.durationSeconds} seconds")

// Set repeat (loop forever)
player.setRepeatCount(-1)

// Play
player.play()

// Observe position
player.positionFlow.collect { position ->
    println("Position: $position seconds")
}

// Cleanup
player.release()
```

## Dependencies

The iOS implementation has no external dependencies beyond:
- iOS SDK (AudioToolbox, CoreAudio)
- Kotlin/Native standard library
- kotlinx.coroutines

## Limitations

1. `loadModuleFromPath()` is not implemented - use `loadModule(ByteArray)` instead
2. The audio render callback mechanism may need refinement for production use
3. Background audio session configuration is not included (add AVAudioSession setup for background playback)

## Future Improvements

- Add AVAudioSession configuration for background playback
- Implement file path loading via NSFileManager
- Add audio interruption handling
- Consider using AVAudioEngine for more features
