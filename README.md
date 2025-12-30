# LibOpenMPT Demo Player- Multiplatform MOD Music Player

A **Compose Multiplatform** application demonstrating native MOD music playback using libopenmpt. Built with Kotlin Multiplatform (KMP), supporting Android, iOS, Desktop (JVM), and Web (WASM/JS).

## Features

- **Native MOD Playback**: Uses libopenmpt C library for authentic tracker music reproduction
- **Cross-Platform UI**: Compose Multiplatform for consistent UI across all platforms
- **Dependency Injection**: Koin for platform-specific ModPlayer injection
- **Reactive State Management**: Kotlin Flows for real-time UI updates
- **Full Playback Control**: Play, pause, stop, and seek functionality
- **Metadata Display**: Shows module information (title, artist, format, etc.)
- **Playback Settings**: Speed and pitch control with presets

## Supported Platforms

| Platform       | Status   | Audio Backend              |
|----------------|----------|----------------------------|
| Android        | ✅ Ready | Oboe + libopenmpt          |
| Desktop (JVM)  | ✅ Ready | JavaSound + libopenmpt     |
| iOS            | ✅ Ready | AudioUnit + libopenmpt     |
| Web (WASM/JS)  | ✅ Ready | Web Audio API + libopenmpt |

## Demo Player Features

The demo app showcases all capabilities of the ModPlayer API:

### File Loading
- **Sample File**: Load a bundled sample MOD file from app resources
- **File Picker**: Load any tracker module from the file system (supports 60+ formats)

### Track Information Display
- Module title and artist
- Format type and tracker name
- Number of channels, patterns, instruments, and samples
- Total duration

### Playback Controls
- **Play/Pause**: Toggle playback with a single button
- **Stop**: Stop playback and reset to beginning
- **Seek Bar**: Drag to seek to any position with real-time position display
- **Playback Info**: Live display of current order, pattern, and row position

### Playback Settings
- **Master Gain**: Volume control from -10dB to +10dB with reset preset
- **Auto-Loop**: Toggle infinite repeat mode
- **Speed Control**: Adjust tempo from 0.25x to 2.0x with preset buttons (0.5x, 1.0x, 1.5x, 2.0x)
- **Pitch Control**: Adjust pitch from 0.25x to 2.0x with preset buttons (0.5x, 1.0x, 1.5x, 2.0x)

## ModPlayer API Reference

The `ModPlayer` interface (`shared/src/commonMain/kotlin/com/beyondeye/openmpt/core/ModPlayer.kt`) provides a platform-agnostic API for MOD music playback.

### Lifecycle Methods

| Method | Description |
|--------|-------------|
| `loadModule(data: ByteArray): Boolean` | Load a module from a byte array. Returns `true` on success. |
| `loadModuleSuspend(data: ByteArray): Boolean` | Suspend version of `loadModule`. Recommended for platforms requiring async initialization (e.g., wasmJS). |
| `loadModuleFromPath(path: String): Boolean` | Load a module from a file path. Not available on all platforms. |
| `release()` | Release all resources. Must be called when the player is no longer needed. |

### Playback Control Methods

| Method | Description |
|--------|-------------|
| `play()` | Start or resume playback. |
| `pause()` | Pause playback. |
| `stop()` | Stop playback and reset position to the beginning. |
| `seek(positionSeconds: Double)` | Seek to a specific position in seconds. |

### Configuration Methods

| Method | Description |
|--------|-------------|
| `setRepeatCount(count: Int)` | Set repeat mode: `-1` = infinite, `0` = no repeat, `n` = repeat n times. |
| `setMasterGain(gainMillibel: Int)` | Set master volume in millibels (0 = normal, negative = quieter, positive = louder). |
| `setStereoSeparation(percent: Int)` | Set stereo separation (0-200%, default 100%). |
| `setPlaybackSpeed(speed: Double)` | Set playback speed/tempo factor (0.25 to 2.0, 1.0 = normal). |
| `getPlaybackSpeed(): Double` | Get current playback speed. |
| `setPitch(pitch: Double)` | Set pitch factor (0.25 to 2.0, 1.0 = normal). |
| `getPitch(): Double` | Get current pitch factor. |

### State Properties

| Property | Type | Description |
|----------|------|-------------|
| `playbackState` | `PlaybackState` | Current state: `Idle`, `Loading`, `Loaded`, `Playing`, `Paused`, `Stopped`, or `Error`. |
| `isPlaying` | `Boolean` | Whether the module is currently playing. |
| `positionSeconds` | `Double` | Current playback position in seconds. |
| `durationSeconds` | `Double` | Total duration of the module in seconds. |

### Module Information Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getMetadata()` | `ModMetadata` | Get metadata (title, artist, format, tracker, channels, patterns, instruments, samples, duration). |
| `getCurrentOrder()` | `Int` | Get current order position (-1 if no module loaded). |
| `getCurrentPattern()` | `Int` | Get current pattern being played (-1 if no module loaded). |
| `getCurrentRow()` | `Int` | Get current row in the pattern (-1 if no module loaded). |
| `getNumChannels()` | `Int` | Get number of channels (0 if no module loaded). |

### Reactive State Observers

| Property | Type | Description |
|----------|------|-------------|
| `playbackStateFlow` | `StateFlow<PlaybackState>` | Flow of playback state changes for reactive UI updates. |
| `positionFlow` | `StateFlow<Double>` | Flow of position updates in seconds, updated periodically during playback. |

## Architecture

```
┌─────────────────────────────────────────────────────┐
│  UI Layer (Compose Multiplatform)                   │
│  ├─ App.kt (common entry point)                     │
│  ├─ ModPlayerScreen                                 │
│  └─ ModPlayerViewModel (Koin-injected)              │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│  Dependency Injection (Koin 4.1.1)                  │
│  ├─ appModule                                       │
│  ├─ factory<ModPlayer> { createModPlayer() }        │
│  └─ viewModel { ModPlayerViewModel(get()) }         │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│  Shared Module (KMP)                                │
│  ├─ ModPlayer interface (commonMain)                │
│  ├─ createModPlayer() expect/actual factory         │
│  └─ Platform implementations:                       │
│      ├─ AndroidModPlayer (JNI + Oboe)               │
│      ├─ IosModPlayer (cinterop + AudioUnit)         │
│      ├─ DesktopModPlayer (JNI + JavaSound)          │
│      └─ WasmModPlayer (JS interop + Web Audio)      │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│  Native Layer (Android)                             │
│  ├─ mod_player_jni.cpp (JNI bridge)                 │
│  ├─ ModPlayerEngine.cpp (C++ engine)                │
│  ├─ libopenmpt (MOD rendering)                      │
│  └─ Oboe (Audio output)                             │
└─────────────────────────────────────────────────────┘
```

## Project Structure

```
app/
├── src/
│   ├── commonMain/kotlin/com/beyondeye/openmptdemo/
│   │   ├── App.kt                      # Main Compose UI
│   │   ├── ModPlayerViewModel.kt       # Shared ViewModel
│   │   ├── di/
│   │   │   └── AppModule.kt            # Koin DI module
│   │   └── ui/theme/
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   ├── commonMain/composeResources/files/
│   │   └── sm64_mainmenuss.xm          # Sample MOD file
│   ├── androidMain/kotlin/com/beyondeye/openmptdemo/
│   │   ├── MainActivity.kt             # Android entry point
│   │   └── OpenMPTDemoApp.kt           # Application class (Koin init)
│   ├── desktopMain/kotlin/com/beyondeye/openmptdemo/
│   │   └── main.kt                     # Desktop entry point
│   ├── wasmJsMain/kotlin/com/beyondeye/openmptdemo/
│   │   └── main.kt                     # WASM/JS entry point
│   └── iosMain/kotlin/com/beyondeye/openmptdemo/
│       └── MainViewController.kt       # iOS entry point

shared/
├── src/
│   ├── commonMain/kotlin/com/beyondeye/openmpt/core/
│   │   ├── ModPlayer.kt                # Platform-agnostic interface
│   │   ├── ModPlayerFactory.kt         # expect fun createModPlayer()
│   │   ├── PlaybackState.kt
│   │   ├── ModMetadata.kt
│   │   └── ModPlayerException.kt
│   ├── androidMain/
│   │   ├── kotlin/.../AndroidModPlayer.kt
│   │   ├── kotlin/.../ModPlayerNative.kt
│   │   ├── kotlin/.../ModPlayerFactory.android.kt
│   │   ├── cpp/                        # JNI native code
│   │   └── jniLibs/                    # Prebuilt libopenmpt.so
│   ├── iosMain/kotlin/.../
│   ├── desktopMain/kotlin/.../
│   └── wasmJsMain/kotlin/.../

libopenmpt/                             # Native library build module
```

## Building the Project

### Prerequisites

- Android Studio Hedgehog or newer
- Android NDK (for Android builds)
- CMake 3.22.1 or newer
- JDK 11 or newer (use `/opt/android-studio/jbr` as specified in project rules)

**Additional requirements for macOS Desktop builds:**
- Xcode and Xcode Command Line Tools
- CMake 3.21+ (install via `brew install cmake`)
- See [docs/README_macos_build.md](docs/README_macos_build.md) for detailed instructions

### Build Commands

**Build libopenmpt first (Android):**
```bash
./gradlew :libopenmpt:exportPrebuiltLibsDebug
```

**Build Android app:**
```bash
./gradlew :app:assembleDebug
```

**Run Desktop app:**
```bash
./gradlew :app:run
```

**Build Desktop native libraries for macOS (if not already built):**
```bash
# Build libopenmpt.dylib
cd libopenmpt/src/main/cpp/macos
mkdir -p build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release -j8

# Copy to resources
cp lib/libopenmpt.dylib ../../../../../../shared/src/desktopMain/resources/native/macos-arm64/

# Build JNI wrapper (from project root)
cd shared/src/desktopMain/cpp
mkdir -p build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release \
  -DLIBOPENMPT_INCLUDE_DIR=$(pwd)/../../../../libopenmpt/src/main/cpp \
  -DLIBOPENMPT_LIBRARY=$(pwd)/../../resources/native/macos-arm64/libopenmpt.dylib
cmake --build . --config Release
```

**Build WASM/JS:**
```bash
./gradlew :app:wasmJsBrowserDevelopmentRun
```

**Build iOS libraries:**
```bash
./gradlew :libopenmpt:buildIos
```

**Install Android app:**
```bash
./gradlew :app:installDebug
```

## Key Technologies

### Dependency Injection with Koin

The project uses [Koin 4.1.1](https://insert-koin.io/) for multiplatform dependency injection:

```kotlin
// AppModule.kt
val appModule = module {
    factory<ModPlayer> { createModPlayer() }
    viewModel { ModPlayerViewModel(get()) }
}

// Usage in Composables
@Composable
fun ModPlayerScreen(
    viewModel: ModPlayerViewModel = koinViewModel()
) { ... }
```

### Multiplatform Logging

Uses [de.halfbit:logger](https://github.com/nickel79/logger) (0.9) for cross-platform logging:

```kotlin
import de.halfbit.logger.d
import de.halfbit.logger.e

d("TAG") { "Debug message" }
e("TAG") { "Error: ${exception.message}" }
```

### Compose Multiplatform Resources

Sample MOD files are loaded using Compose Multiplatform resources:

```kotlin
val bytes = Res.readBytes("files/sm64_mainmenuss.xm")
viewModel.loadModule(bytes)
```

## Supported Module Formats

libopenmpt 0.8.3 supports a wide range of tracker formats:
- MOD (ProTracker, NoiseTracker, etc.)
- XM (FastTracker 2)
- IT (Impulse Tracker)
- S3M (Scream Tracker 3)
- And many more...
- see [libopenmpt FAQ](https://lib.openmpt.org/libopenmpt/faq/) for full list

## Implementation Status

### Android ✅
- Full native implementation with JNI
- Oboe for low-latency audio
- Complete playback control

### Desktop (JVM) ✅
- Full native implementation with JNI bridge
- libopenmpt for MOD rendering via JNI
- JavaSound `SourceDataLine` for 16-bit PCM audio output
- Complete playback control with state management

### iOS ✅
- Full native implementation with Kotlin/Native cinterop
- libopenmpt compiled as XCFramework (arm64 device + arm64 simulator)
- AudioUnit (RemoteIO) for low-latency audio output
- Complete playback control with state management
- Note: `loadModuleFromPath()` not implemented (use `loadModule(ByteArray)` instead)
- Note: Background audio session not configured (add AVAudioSession setup if needed)

### Web (WASM/JS) ✅
- Full implementation with libopenmpt compiled to WASM
- Web Audio API with ScriptProcessorNode for audio output
- Complete playback control and metadata support
- Note: ScriptProcessorNode is deprecated; future migration to AudioWorklet recommended

## Dependencies

- **Kotlin Multiplatform**: 2.2.21
- **Compose Multiplatform**: 1.9.3
- **Koin**: 4.1.1
- **Logger**: 0.9 (de.halfbit:logger)
- **libopenmpt**: 0.8.3  (Native MOD rendering)
- **Oboe**: 1.8.0 (Android low-latency audio)
- **Kotlinx Coroutines**: 1.10.2

## Using as a Library

If you want to use the `shared` module as a dependency in your own Kotlin Multiplatform project, please note the following **important requirements**:

### Native Library Requirements

The `shared` module provides Kotlin bindings for libopenmpt but **does NOT bundle** the native library for all platforms:

| Platform | Native Library Status |
|----------|----------------------|
| Android | ✅ Bundled in AAR - no action needed |
| Desktop (JVM) | ✅ Bundled - no action needed |
| iOS | ⚠️ **Manual setup required** - you must provide `libopenmpt.xcframework` |
| Wasm/JS | ⚠️ **Manual setup required** - you must provide `libopenmpt.js` and `libopenmpt.wasm` |

### Quick Setup for iOS Consumers

1. Build or obtain `libopenmpt.xcframework`
2. Copy it to `your-app/src/iosMain/libs/`
3. Add `linkerOpts` in your `build.gradle.kts`:
   ```kotlin
   iosArm64 {
       binaries.framework {
           linkerOpts("-L${projectDir}/src/iosMain/libs/libopenmpt.xcframework/ios-arm64", "-lopenmpt")
       }
   }
   ```

### Quick Setup for Wasm/JS Consumers

1. Obtain `libopenmpt.js` and `libopenmpt.wasm` files
2. Place them in your web app's resources
3. Call `LibOpenMpt.initializeLibOpenMpt()` before using ModPlayer

📖 **For detailed instructions, see [docs/README_library_consumers.md](docs/README_library_consumers.md)**

## License

This project uses:
- libopenmpt (BSD license)
- Oboe (Apache 2.0)
- Sample MOD file from [The Mod Archive](https://modarchive.org/)

## Contributing

When extending platform support:

1. Implement the `ModPlayer` interface in the platform-specific source set
2. Implement the `actual fun createModPlayer()` factory function
3. Use the appropriate audio backend for the platform
4. Test with the sample MOD file

## Troubleshooting

### Build Issues

- **CMake not found**: Install CMake from Android SDK Manager
- **NDK errors**: Ensure NDK is properly installed and configured
- **Koin not found**: Check that Koin dependencies are in the version catalog

### Runtime Issues

- **Native library loading fails**: Ensure libopenmpt.so is built and exported
- **No audio output**: Check device audio settings
- **Crashes on play**: Check logcat for native crash details

## Documentation

- [libopenmpt Documentation](https://lib.openmpt.org/libopenmpt/)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Koin Documentation](https://insert-koin.io/docs/quickstart/kmp/)
- [Oboe Documentation](https://github.com/google/oboe)
- [macOS Build Guide](docs/README_macos_build.md)
- [Desktop Implementation Guide](docs/README_desktop_implementation.md)
