# Building libopenmpt for macOS

This document describes how to build libopenmpt and the JNI wrapper for macOS desktop (JVM) platform.

## Prerequisites

Before building, ensure you have the following installed:

1. **Xcode** - Install from the Mac App Store
2. **Xcode Command Line Tools** - Install via:
   ```bash
   xcode-select --install
   ```
3. **CMake 3.21+** - Install via Homebrew:
   ```bash
   brew install cmake
   ```
4. **JDK 11+** - Required for JNI headers. Recommended: JDK 17 or 21
   - Download from [Adoptium](https://adoptium.net/) or install via Homebrew:
   ```bash
   brew install openjdk@21
   ```

## Build Options

### Option 1: Build WITHOUT External Dependencies (Recommended for simplicity)

This builds libopenmpt without MP3, OGG, or Vorbis support. All native tracker formats (MOD, XM, IT, S3M, etc.) work perfectly.

#### Step 1: Build libopenmpt.dylib

```bash
cd libopenmpt/src/main/cpp/macos
mkdir -p build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release -j8
```

The built library will be at: `libopenmpt/src/main/cpp/macos/build/lib/libopenmpt.dylib`

#### Step 2: Copy to Resources

```bash
mkdir -p shared/src/desktopMain/resources/native/macos-arm64
cp libopenmpt/src/main/cpp/macos/build/lib/libopenmpt.dylib shared/src/desktopMain/resources/native/macos-arm64/
```

For Intel Macs, use `macos-x64` instead of `macos-arm64`.

#### Step 3: Build the JNI Wrapper (libmodplayer_desktop.dylib)

```bash
cd shared/src/desktopMain/cpp
mkdir -p build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release \
  -DLIBOPENMPT_INCLUDE_DIR=$(pwd)/../../../../libopenmpt/src/main/cpp \
  -DLIBOPENMPT_LIBRARY=$(pwd)/../../resources/native/macos-arm64/libopenmpt.dylib
cmake --build . --config Release
```

The JNI wrapper will be automatically placed in `shared/src/desktopMain/resources/native/macos-arm64/`.

### Option 2: Build WITH External Dependencies

For MP3, OGG, and Vorbis support, you need to download and build external dependencies.

#### Step 1: Download External Dependencies

```bash
cd libopenmpt/src/main/cpp
./build/download_externals.sh
```

This downloads:
- mpg123 (MP3 support)
- libogg (OGG container support)
- libvorbis (Vorbis audio support)

#### Step 2: Build with Xcode Workspace

```bash
cd libopenmpt/src/main/cpp/build/xcode-macosx
xcodebuild -workspace libopenmpt.xcworkspace -scheme libopenmpt -configuration Release -arch arm64
```

For Intel Macs, use `-arch x86_64`.

For Universal Binary (both architectures):
```bash
xcodebuild -workspace libopenmpt.xcworkspace -scheme libopenmpt -configuration Release -arch arm64 -arch x86_64
```

The built library will be in the Xcode DerivedData directory.

## Architecture Support

| Architecture | Directory Name | Description |
|-------------|----------------|-------------|
| Apple Silicon (M1/M2/M3) | `macos-arm64` | ARM64 architecture |
| Intel | `macos-x64` | x86_64 architecture |

## Build Script

For convenience, you can use the provided build script:

```bash
cd shared/src/desktopMain/cpp
./build_native.sh
```

This script will:
1. Detect your platform (macos-arm64 or macos-x64)
2. Build the JNI wrapper
3. Place the output in the correct resources directory

## Troubleshooting

### "JNI.h not found"

Ensure `JAVA_HOME` is set correctly:
```bash
export JAVA_HOME=$(/usr/libexec/java_home)
```

Or specify it when running cmake:
```bash
cmake .. -DJAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
```

### "libopenmpt.h not found"

Ensure the `LIBOPENMPT_INCLUDE_DIR` path is correct and points to the libopenmpt source directory.

### Library loading fails at runtime

1. Check that both `.dylib` files are in the resources directory
2. Verify architectures match: `file libopenmpt.dylib` should show `arm64` for Apple Silicon or `x86_64` for Intel
3. Check library dependencies: `otool -L libmodplayer_desktop.dylib`

### Build fails with missing symbols

If building with external dependencies, ensure all dependencies (mpg123, ogg, vorbis) are built before libopenmpt.

## Directory Structure

After building, your resources directory should look like:

```
shared/src/desktopMain/resources/native/
├── linux-x64/
│   ├── libmodplayer_desktop.so
│   └── libopenmpt.so
├── macos-arm64/
│   ├── libmodplayer_desktop.dylib
│   └── libopenmpt.dylib
├── macos-x64/               # (if built for Intel)
│   ├── libmodplayer_desktop.dylib
│   └── libopenmpt.dylib
└── windows-x64/
    ├── modplayer_desktop.dll
    └── openmpt.dll
```

## Running the Desktop App

After building the native libraries:

```bash
./gradlew :app:desktopRun
