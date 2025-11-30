# OpenMPT Demo - Android MOD Music Player

An Android application demonstrating native MOD music playback using libopenmpt and Oboe, with a clean architecture designed for future Kotlin Multiplatform expansion.

## Features

- **Native MOD Playback**: Uses libopenmpt C library for authentic tracker music reproduction
- **Low-Latency Audio**: Oboe library for professional-quality audio output
- **Platform-Agnostic Design**: Clean abstraction layer ready for KMP migration
- **Modern Android UI**: Jetpack Compose Material3 interface
- **Reactive State Management**: Kotlin Flows for real-time UI updates
- **Full Playback Control**: Play, pause, stop, and seek functionality
- **Metadata Display**: Shows module information (title, artist, format, etc.)

## Architecture

```
┌─────────────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose)                         │
│  ├─ MainActivity                                     │
│  └─ ModPlayerViewModel                              │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│  Abstraction Layer (KMP-Ready)                      │
│  ├─ ModPlayer interface                             │
│  ├─ PlaybackState sealed class                      │
│  ├─ ModMetadata data class                          │
│  └─ AndroidModPlayer implementation                 │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│  Native Layer (C++/JNI)                             │
│  ├─ ModPlayerNative (JNI wrapper)                   │
│  ├─ mod_player_jni.cpp (JNI bridge)                 │
│  └─ ModPlayerEngine (C++ engine)                    │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│  External Libraries                                 │
│  ├─ libopenmpt (MOD rendering)                      │
│  └─ Oboe (Audio output)                             │
└─────────────────────────────────────────────────────┘
```

## Project Structure

```
app/src/main/
├── assets/
│   └── sm64_mainmenuss.xm              # Sample MOD file
├── cpp/
│   ├── CMakeLists.txt                  # Native build configuration
│   ├── ModPlayerEngine.h/cpp           # C++ audio engine
│   └── mod_player_jni.cpp              # JNI bridge
└── java/com/beyondeye/openmptdemo/
    ├── MainActivity.kt                 # Compose UI
    ├── ModPlayerViewModel.kt           # State management
    └── player/
        ├── ModPlayer.kt                # Platform-agnostic interface
        ├── PlaybackState.kt            # State sealed class
        ├── ModMetadata.kt              # Metadata data class
        ├── ModPlayerException.kt       # Exception hierarchy
        ├── ModPlayerNative.kt          # JNI wrapper
        └── AndroidModPlayer.kt         # Android implementation
```

## Building the Project

### Prerequisites

- Android Studio Hedgehog or newer
- Android NDK (configured in SDK Manager)
- CMake 3.22.1 or newer
- JDK 11 or newer (use `/opt/android-studio/jbr` as specified in project rules)

### Build Steps

1. **Build libopenmpt module first**:
   ```bash
   ./gradlew :libopenmpt:build
   ```

2. **Build the app**:
   ```bash
   ./gradlew :app:assembleDebug
   ```

3. **Run on device/emulator**:
   ```bash
   ./gradlew :app:installDebug
   ```

## Implementation Details

### Key Design Decisions

1. **Platform-Agnostic Interface**
   - `ModPlayer` interface uses only Kotlin stdlib types
   - No Android-specific dependencies in the interface
   - Ready for KMP migration to iOS and JVM platforms

2. **Reactive State Management**
   - `StateFlow` for playback state and position updates
   - Automatic UI updates via Compose's `collectAsState()`
   - Clean separation between business logic and UI

3. **Native Audio Pipeline**
   - Oboe for low-latency, high-performance audio
   - Float samples (48kHz stereo) for best quality
   - Thread-safe rendering in Oboe callback
   - Automatic stream management and error recovery

4. **Memory Safety**
   - Proper lifecycle management with `release()` methods
   - RAII patterns in C++ (smart pointers, destructors)
   - JNI local references properly managed

### Supported Module Formats

libopenmpt supports a wide range of tracker formats:
- MOD (ProTracker, NoiseTracker, etc.)
- XM (FastTracker 2)
- IT (Impulse Tracker)
- S3M (Scream Tracker 3)
- And many more...

## Future Enhancements

### Ready for Kotlin Multiplatform

The abstraction layer is designed for easy KMP migration:

1. **Move to KMP module**:
   - Move interfaces to `commonMain`
   - Keep `AndroidModPlayer` in `androidMain`

2. **Add iOS support**:
   - Create `IosModPlayer` in `iosMain`
   - Use Kotlin/Native interop with libopenmpt
   - Use AVAudioEngine for iOS audio output

3. **Add JVM support**:
   - Create `JvmModPlayer` in `jvmMain`
   - Use JNI or JNA for libopenmpt access
   - Use JavaSound or similar for desktop audio

### Potential Features

- File picker for loading custom MOD files
- Playlist support
- Pattern visualization
- Equalizer controls
- Export to WAV/MP3
- Android MediaSession integration
- Background playback service
- Notification controls

## Testing

The sample MOD file (Super Mario 64 Main Menu theme) is automatically loaded from assets. To test:

1. Launch the app
2. Tap "Load Sample MOD File"
3. View the metadata display
4. Use play/pause/stop controls
5. Drag the seek bar to navigate

## Dependencies

- **libopenmpt**: MOD music rendering engine
- **Oboe 1.8.0**: Low-latency audio for Android
- **Jetpack Compose**: Modern UI toolkit
- **Kotlin Coroutines**: Async programming
- **AndroidX Lifecycle**: ViewModel and lifecycle management

## License

This project uses:
- libopenmpt (BSD license)
- Oboe (Apache 2.0)
- Sample MOD file from [The Mod Archive](https://modarchive.org/)

## Credits

- **libopenmpt**: OpenMPT development team
- **Oboe**: Google Android Audio team
- **Sample Module**: Super Mario 64 Main Menu by [Module Author]

## Contributing

When extending this project to Kotlin Multiplatform:

1. Keep the `ModPlayer` interface unchanged
2. Add platform-specific implementations in appropriate source sets
3. Maintain the reactive Flow-based state management
4. Follow the existing error handling patterns

## Troubleshooting

### Build Issues

- **CMake not found**: Install CMake from Android SDK Manager
- **NDK errors**: Ensure NDK is properly installed and configured
- **Oboe linking errors**: Check that prefab is enabled in build.gradle.kts

### Runtime Issues

- **Native library loading fails**: Check that both `libopenmpt.so` and `libmodplayer.so` are in the APK
- **No audio output**: Verify device audio settings and permissions
- **Crashes on play**: Check logcat for native crash details

## Documentation

For more information about the underlying libraries:
- [libopenmpt Documentation](https://lib.openmpt.org/libopenmpt/)
- [Oboe Documentation](https://github.com/google/oboe)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
