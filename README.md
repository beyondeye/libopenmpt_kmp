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

| Platform | Status  | Audio Backend               |
|----------|---------|-----------------------------|
| Android | ✅ Ready | Oboe + libopenmpt           |
| Desktop (JVM) | ✅ Ready | JavaSound audio +libopenmpt |
| iOS | 🚧 Stub | To be implemented           |
| Web (WASM/JS) | ✅ Ready | Web Audio API +libopenmpt     |

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
│      ├─ IosModPlayer (stub)                         │
│      ├─ DesktopModPlayer (stub)                     │
│      └─ WasmModPlayer (stub)                        │
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

**Build WASM/JS:**
```bash
./gradlew :app:wasmJsBrowserDevelopmentRun
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

libopenmpt supports a wide range of tracker formats:
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

### Desktop (JVM) 🚧
- Stub implementation
- Needs JNI/JNA binding to libopenmpt
- Needs audio backend (JavaSound or similar)

### iOS 🚧
- Stub implementation
- Needs Kotlin/Native interop with libopenmpt
- Needs CoreAudio integration

### Web (WASM/JS) 🚧
- Stub implementation
- Needs libopenmpt compiled to WASM
- Needs Web Audio API integration

## Dependencies

- **Kotlin Multiplatform**: 2.2.21
- **Compose Multiplatform**: 1.9.3
- **Koin**: 4.1.1
- **Logger**: 0.9 (de.halfbit:logger)
- **libopenmpt**: Native MOD rendering
- **Oboe**: 1.8.0 (Android low-latency audio)
- **Kotlinx Coroutines**: 1.10.2

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
