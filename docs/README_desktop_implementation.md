# Desktop (JVM) Implementation Guide

This document describes how to build and use the Desktop JVM implementation of the ModPlayer interface.

## Architecture Overview

The Desktop implementation follows a similar architecture to the Android version:

```
┌─────────────────────────────────────────────────────────────────┐
│                    DesktopModPlayer.kt                          │
│  (High-level Kotlin implementation, handles JavaSound audio)    │
└────────────────────────────┬────────────────────────────────────┘
                             │ uses
┌────────────────────────────▼────────────────────────────────────┐
│                  DesktopModPlayerNative.kt                      │
│  (JNI wrapper with external native functions for libopenmpt)    │
└────────────────────────────┬────────────────────────────────────┘
                             │ JNI calls
┌────────────────────────────▼────────────────────────────────────┐
│                  desktop_mod_player_jni.cpp                     │
│  (JNI bridge - wraps libopenmpt C API, no audio handling)       │
└────────────────────────────┬────────────────────────────────────┘
                             │ calls
┌────────────────────────────▼────────────────────────────────────┐
│                      libopenmpt.so                              │
│  (Pre-compiled libopenmpt library)                              │
└─────────────────────────────────────────────────────────────────┘
```

### Key Differences from Android

| Aspect | Android | Desktop |
|--------|---------|---------|
| Audio Output | Oboe (native) | JavaSound (SourceDataLine) |
| Native Lib Build | CMake via Gradle | CMake standalone |
| Lib Loading | System.loadLibrary | Extract from resources |

## Prerequisites

1. **JDK with JNI headers** - JDK 11+ recommended
2. **CMake 3.16+** - For building the native library
3. **C++ compiler** - GCC on Linux, Clang on macOS, MSVC on Windows
4. **libopenmpt** - Pre-compiled library and headers

## Directory Structure

```
shared/src/desktopMain/
├── cpp/
│   ├── CMakeLists.txt           # Build configuration
│   ├── desktop_mod_player_jni.cpp  # JNI wrapper code
│   └── build_native.sh          # Build script
├── kotlin/com/beyondeye/openmpt/core/
│   ├── DesktopModPlayer.kt      # Main implementation
│   ├── DesktopModPlayerNative.kt # JNI interface
│   ├── NativeLibraryLoader.kt   # Cross-platform library loader
│   └── ModPlayerFactory.desktop.kt
└── resources/native/
    ├── linux-x64/
    │   ├── libopenmpt.so
    │   └── libmodplayer_desktop.so
    ├── linux-arm64/
    │   └── ...
    ├── macos-x64/
    │   ├── libopenmpt.dylib
    │   └── libmodplayer_desktop.dylib
    ├── macos-arm64/
    │   └── ...
    └── windows-x64/
        ├── openmpt.dll
        └── modplayer_desktop.dll
```

## Building the Native Library

### Quick Start (Linux)

```bash
cd shared/src/desktopMain/cpp
./build_native.sh
```

### Manual Build

```bash
cd shared/src/desktopMain/cpp
mkdir build && cd build

# Configure (auto-detect paths)
cmake ..

# Or specify paths explicitly
cmake .. \
  -DLIBOPENMPT_INCLUDE_DIR=/path/to/libopenmpt/include \
  -DLIBOPENMPT_LIBRARY=/path/to/libopenmpt.so

# Build
cmake --build . --config Release
```

### Build Output

The built library will be placed in:
```
shared/src/desktopMain/resources/native/{platform}/libmodplayer_desktop.so
```

Where `{platform}` is one of:
- `linux-x64`
- `linux-arm64`
- `macos-x64`
- `macos-arm64`
- `windows-x64`

## Required Libraries

For each platform, you need two libraries in the resources directory:

1. **libopenmpt** - The MOD player library
   - Linux: `libopenmpt.so`
   - macOS: `libopenmpt.dylib`
   - Windows: `openmpt.dll`

2. **modplayer_desktop** - The JNI wrapper (built from this project)
   - Linux: `libmodplayer_desktop.so`
   - macOS: `libmodplayer_desktop.dylib`
   - Windows: `modplayer_desktop.dll`

## Getting libopenmpt

### Option 1: System Package (Linux)

```bash
# Debian/Ubuntu
sudo apt install libopenmpt-dev

# Fedora
sudo dnf install libopenmpt-devel

# Arch
sudo pacman -S libopenmpt
```

### Option 2: Build from Source

libopenmpt source is included in this project at `libopenmpt/src/main/cpp/`.

For desktop, build with:
```bash
cd libopenmpt/src/main/cpp
make CONFIG=standard shared
```

### Option 3: Download Pre-built

Visit https://lib.openmpt.org/libopenmpt/ for pre-built binaries.

## Library Loading

The `NativeLibraryLoader` class handles cross-platform library loading:

1. **Detection** - Detects current OS (Linux/macOS/Windows) and architecture (x64/arm64)
2. **Extraction** - Extracts libraries from JAR resources to a temp directory
3. **Loading** - Loads libraries using `System.load()` in dependency order

Libraries are loaded from: `/native/{platform}/` within the resources.

## Usage Example

```kotlin
// Create player
val player = DesktopModPlayer()

// Load a module
val moduleData = File("song.xm").readBytes()
player.loadModule(moduleData)

// Play
player.play()

// Control playback
player.pause()
player.seek(30.0)  // Seek to 30 seconds
player.setRepeatCount(-1)  // Infinite loop

// Get info
println("Title: ${player.getMetadata().title}")
println("Duration: ${player.durationSeconds}s")

// Cleanup
player.release()
```

## Audio Configuration

The desktop implementation uses JavaSound with these defaults:

| Parameter | Value |
|-----------|-------|
| Sample Rate | 48000 Hz |
| Channels | 2 (Stereo) |
| Bit Depth | 16-bit |
| Buffer Size | 2048 frames (~42ms) |

## Troubleshooting

### "Native library not found"

1. Check that libraries are in the correct resources directory
2. Verify the platform directory name matches your system
3. Run with `-Djava.library.path` to debug:
   ```bash
   java -Djava.library.path=/path/to/libs -jar app.jar
   ```

### "UnsatisfiedLinkError"

1. Ensure libopenmpt is loaded before modplayer_desktop
2. Check library dependencies with `ldd` (Linux) or `otool -L` (macOS)
3. Verify the library was built with the same architecture

### "No audio output"

1. Check that JavaSound is available: `AudioSystem.isLineSupported()`
2. Verify audio format is supported on your system
3. Try different audio buffer sizes

### Build Errors

1. **"Could not find libopenmpt"**: Set `LIBOPENMPT_INCLUDE_DIR` and `LIBOPENMPT_LIBRARY`
2. **"JNI.h not found"**: Set `JAVA_HOME` or ensure JDK is in PATH
3. **"Undefined reference"**: Check library linking order

## Cross-Platform Building

To build for multiple platforms, you'll need to build on each target platform or use cross-compilation:

### Linux x64 → Linux arm64

```bash
cmake .. \
  -DCMAKE_SYSTEM_NAME=Linux \
  -DCMAKE_SYSTEM_PROCESSOR=aarch64 \
  -DCMAKE_C_COMPILER=aarch64-linux-gnu-gcc \
  -DCMAKE_CXX_COMPILER=aarch64-linux-gnu-g++
```

### macOS Universal Binary

```bash
cmake .. -DCMAKE_OSX_ARCHITECTURES="x86_64;arm64"
```

## Future Improvements

- [ ] Gradle integration for automatic native build
- [ ] Pre-built binaries for all platforms in CI
- [ ] Windows support testing
- [ ] macOS code signing
