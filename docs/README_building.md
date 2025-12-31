# Building libopenmpt-kmp

This document explains how to build the libopenmpt-kmp library from source, including all native library dependencies.

## Overview

The `shared` module is a Kotlin Multiplatform library that provides Kotlin bindings for libopenmpt. It relies on **pre-committed native libraries** for each platform:

| Platform | Native Library | Location |
|----------|---------------|----------|
| Android | `libopenmpt.so` | `shared/src/androidMain/jniLibs/` (Release) |
| Android Debug | `libopenmpt.so` | `shared/src/androidMain/jniLibsDebug/` (Debug) |
| iOS | `libopenmpt.xcframework` | `shared/src/iosMain/libs/` |
| macOS Desktop | `libopenmpt.dylib` | `shared/src/desktopMain/resources/native/macos-arm64/` |
| Linux Desktop | `libopenmpt.so` | `shared/src/desktopMain/resources/native/linux-x64/` |
| Windows Desktop | `openmpt.dll` | `shared/src/desktopMain/resources/native/windows-x64/` |
| Wasm/JS | `libopenmpt.wasm` | `shared/src/wasmJsMain/resources/` |

## Quick Start

### Building the Shared Module

The shared module can be built without rebuilding native libraries (they are pre-committed):

```bash
# Build all targets
./gradlew :shared:build

# Build specific targets
./gradlew :shared:compileKotlinAndroid
./gradlew :shared:compileKotlinIosArm64
./gradlew :shared:compileKotlinDesktop
./gradlew :shared:compileKotlinWasmJs
```

### Running the Demo App

```bash
# Android
./gradlew :app:installDebug

# Desktop (JVM)
./gradlew :app:runDesktop

# iOS (requires Xcode)
# Open iosApp/iosApp.xcodeproj and run from Xcode

# Wasm/JS
./gradlew :app:wasmJsBrowserRun
```

## Rebuilding Native Libraries

If you need to rebuild native libraries from source (e.g., after updating libopenmpt), you have two options:

### Option 1: GitHub Actions Workflow (Recommended)

Use the "Rebuild Native Libraries" workflow in GitHub Actions:

1. Go to Actions → "Rebuild Native Libraries"
2. Click "Run workflow"
3. Select which platforms to rebuild (or "all")
4. Choose whether to create a PR with the updated binaries
5. The workflow will build on appropriate runners and create a PR

### Option 2: Manual Build

#### Android Native Libraries

Requires: Android NDK

```bash
# Build Release libraries (recommended for publishing)
./gradlew :libopenmpt:exportPrebuiltLibsRelease

# Build Debug libraries (for local development)
./gradlew :libopenmpt:exportPrebuiltLibsDebug
```

Output locations:
- Release: `shared/src/androidMain/jniLibs/`
- Debug: `shared/src/androidMain/jniLibsDebug/`

See also: [README.AndroidNDK.txt](README.AndroidNDK.txt)

#### iOS Native Libraries

Requires: macOS with Xcode and CMake

```bash
# Build XCFramework for iOS
./gradlew :libopenmpt:buildIos
```

Output: `shared/src/iosMain/libs/libopenmpt.xcframework/`

See also: [README_iOS_implementation.md](README_iOS_implementation.md)

#### macOS Desktop Native Library

Requires: macOS with CMake

```bash
cd libopenmpt/src/main/cpp/macos
mkdir -p build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

Then copy `libopenmpt.dylib` to `shared/src/desktopMain/resources/native/macos-arm64/`

See also: [README_macos_build.md](README_macos_build.md)

#### Linux Desktop Native Library

Requires: Linux with CMake and build-essential

```bash
cd libopenmpt/src/main/cpp
mkdir -p build-linux && cd build-linux
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

Then copy `libopenmpt.so` to `shared/src/desktopMain/resources/native/linux-x64/`

#### Windows Desktop Native Library

Requires: Windows with Visual Studio and CMake

```cmd
cd libopenmpt\src\main\cpp
mkdir build-windows
cd build-windows
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

Then copy `openmpt.dll` to `shared/src/desktopMain/resources/native/windows-x64/`

#### Wasm Library

Requires: Emscripten SDK

See: [README_recompiling_libopenmpt_wasm.md](README_recompiling_libopenmpt_wasm.md)

## Publishing

The library is published to Maven Central. See [README_publishing.md](README_publishing.md) for details.

### Publishing Workflow

The GitHub Actions publish workflow:
1. Uses pre-committed native libraries (no rebuild)
2. Builds the shared module for all targets
3. Publishes to Maven Central (on release or manual trigger)

```bash
# Local publishing test
./gradlew :shared:publishAllPublicationsToLocalRepository
```

## Directory Structure

```
shared/
├── src/
│   ├── androidMain/
│   │   ├── cpp/              # Android JNI wrapper source
│   │   ├── jniLibs/          # Pre-built Release .so files
│   │   │   ├── arm64-v8a/
│   │   │   └── armeabi-v7a/
│   │   └── jniLibsDebug/     # Pre-built Debug .so files
│   │       ├── arm64-v8a/
│   │       └── armeabi-v7a/
│   ├── iosMain/
│   │   ├── headers/          # libopenmpt headers for cinterop
│   │   └── libs/             # Pre-built XCFramework
│   ├── desktopMain/
│   │   ├── cpp/              # Desktop JNI wrapper source
│   │   └── resources/native/ # Pre-built desktop natives
│   │       ├── linux-x64/
│   │       ├── macos-arm64/
│   │       └── windows-x64/
│   └── wasmJsMain/
│       └── resources/        # Pre-built wasm files
│           ├── libopenmpt.js
│           └── libopenmpt.wasm
```

## Troubleshooting

### Build fails with missing native library

Ensure all pre-built native libraries are present in the repository. If they're missing, run the "Rebuild Native Libraries" GitHub Action or build them manually.

### Gradle task dependency errors

If you see errors about implicit task dependencies, ensure you're using the latest build configuration. The `shared` module no longer has automatic dependencies on libopenmpt export tasks - it relies on pre-committed binaries.

### CMake errors on Android

Ensure CMake 3.22.1 is installed. The Android Gradle Plugin will download it automatically if needed.

## Platform-Specific Documentation

- **Android**: [README.AndroidNDK.txt](README.AndroidNDK.txt)
- **iOS**: [README_iOS_implementation.md](README_iOS_implementation.md)
- **macOS**: [README_macos_build.md](README_macos_build.md)
- **Desktop JVM**: [README_desktop_implementation.md](README_desktop_implementation.md)
- **Wasm/JS**: [README_wasmjs_integration.md](README_wasmjs_integration.md), [README_recompiling_libopenmpt_wasm.md](README_recompiling_libopenmpt_wasm.md)
- **Library Consumers**: [README_library_consumers.md](README_library_consumers.md)
- **Publishing**: [README_publishing.md](README_publishing.md)
