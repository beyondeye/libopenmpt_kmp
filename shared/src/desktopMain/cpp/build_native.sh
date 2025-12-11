#!/bin/bash

# Build script for modplayer_desktop native library
#
# This script builds the JNI wrapper library that provides Kotlin access
# to libopenmpt functions.
#
# Prerequisites:
#   - CMake 3.16+
#   - JDK with JNI headers (JAVA_HOME must be set or JDK in PATH)
#   - libopenmpt development headers
#
# Usage:
#   ./build_native.sh                    # Build for current platform
#   ./build_native.sh clean              # Clean build directory
#   ./build_native.sh --help             # Show help

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/build"
OUTPUT_DIR="$SCRIPT_DIR/../resources/native"

# Detect platform
detect_platform() {
    local os=$(uname -s)
    local arch=$(uname -m)
    
    case "$os" in
        Linux)
            case "$arch" in
                x86_64) echo "linux-x64" ;;
                aarch64) echo "linux-arm64" ;;
                *) echo "linux-$arch" ;;
            esac
            ;;
        Darwin)
            case "$arch" in
                x86_64) echo "macos-x64" ;;
                arm64) echo "macos-arm64" ;;
                *) echo "macos-$arch" ;;
            esac
            ;;
        MINGW*|MSYS*|CYGWIN*)
            case "$arch" in
                x86_64) echo "windows-x64" ;;
                aarch64) echo "windows-arm64" ;;
                *) echo "windows-$arch" ;;
            esac
            ;;
        *)
            echo "unknown-$arch"
            ;;
    esac
}

show_help() {
    echo "Build script for modplayer_desktop native library"
    echo ""
    echo "Usage: $0 [command] [options]"
    echo ""
    echo "Commands:"
    echo "  (default)   Build the native library"
    echo "  clean       Remove build directory"
    echo "  --help      Show this help message"
    echo ""
    echo "Options (passed to CMake):"
    echo "  -DLIBOPENMPT_INCLUDE_DIR=<path>  Path to libopenmpt headers"
    echo "  -DLIBOPENMPT_LIBRARY=<path>      Path to libopenmpt library"
    echo ""
    echo "Example:"
    echo "  $0 -DLIBOPENMPT_INCLUDE_DIR=/usr/include"
    echo ""
}

clean() {
    echo "Cleaning build directory..."
    rm -rf "$BUILD_DIR"
    echo "Done."
}

build() {
    local platform=$(detect_platform)
    local platform_output_dir="$OUTPUT_DIR/$platform"
    
    echo "=== Building modplayer_desktop ==="
    echo "Platform: $platform"
    echo "Build dir: $BUILD_DIR"
    echo "Output dir: $platform_output_dir"
    echo ""
    
    # Create build directory
    mkdir -p "$BUILD_DIR"
    cd "$BUILD_DIR"
    
    # Find libopenmpt include directory
    local libopenmpt_include=""
    local libopenmpt_lib=""
    
    # Check project's libopenmpt submodule first
    if [ -d "$SCRIPT_DIR/../../../../libopenmpt/src/main/cpp" ]; then
        libopenmpt_include="$SCRIPT_DIR/../../../../libopenmpt/src/main/cpp"
        echo "Found libopenmpt headers in project: $libopenmpt_include"
    fi
    
    # Check for library in resources
    if [ -f "$platform_output_dir/libopenmpt.so" ]; then
        libopenmpt_lib="$platform_output_dir/libopenmpt.so"
        echo "Found libopenmpt library in resources: $libopenmpt_lib"
    elif [ -f "$platform_output_dir/libopenmpt.dylib" ]; then
        libopenmpt_lib="$platform_output_dir/libopenmpt.dylib"
        echo "Found libopenmpt library in resources: $libopenmpt_lib"
    fi
    
    # Run CMake
    cmake_args=()
    if [ -n "$libopenmpt_include" ]; then
        cmake_args+=("-DLIBOPENMPT_INCLUDE_DIR=$libopenmpt_include")
    fi
    if [ -n "$libopenmpt_lib" ]; then
        cmake_args+=("-DLIBOPENMPT_LIBRARY=$libopenmpt_lib")
    fi
    
    # Add any additional arguments passed to the script
    cmake_args+=("$@")
    
    echo "Running CMake..."
    cmake "${cmake_args[@]}" "$SCRIPT_DIR"
    
    echo ""
    echo "Building..."
    cmake --build . --config Release
    
    echo ""
    echo "=== Build complete ==="
    echo "Library built at: $platform_output_dir"
    ls -la "$platform_output_dir"/*.so 2>/dev/null || ls -la "$platform_output_dir"/*.dylib 2>/dev/null || ls -la "$platform_output_dir"/*.dll 2>/dev/null || true
}

# Main
case "${1:-}" in
    --help|-h)
        show_help
        ;;
    clean)
        clean
        ;;
    *)
        build "$@"
        ;;
esac
