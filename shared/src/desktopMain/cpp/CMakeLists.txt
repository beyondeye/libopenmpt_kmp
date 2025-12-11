# CMakeLists.txt for Desktop JVM native library (modplayer_desktop)
#
# This builds a JNI library that wraps libopenmpt for use with JavaSound.
# The library only handles module loading, rendering, and metadata -
# audio output is handled in Kotlin using JavaSound.
#
# Build instructions:
#   mkdir build && cd build
#   cmake ..
#   make
#
# Cross-platform build:
#   - Linux: produces libmodplayer_desktop.so
#   - macOS: produces libmodplayer_desktop.dylib
#   - Windows: produces modplayer_desktop.dll

cmake_minimum_required(VERSION 3.16)
project(modplayer_desktop LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find JNI
find_package(JNI REQUIRED)
if(JNI_FOUND)
    message(STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message(STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
endif()

# libopenmpt paths - can be overridden via cmake options
# Default: look for libopenmpt in common locations
set(LIBOPENMPT_INCLUDE_DIR "" CACHE PATH "Path to libopenmpt include directory")
set(LIBOPENMPT_LIBRARY "" CACHE FILEPATH "Path to libopenmpt library")

# If not specified, try to find libopenmpt
if(NOT LIBOPENMPT_INCLUDE_DIR)
    # Check common locations
    find_path(LIBOPENMPT_INCLUDE_DIR_FOUND
        NAMES libopenmpt/libopenmpt.h
        PATHS
            /usr/include
            /usr/local/include
            ${CMAKE_CURRENT_SOURCE_DIR}/../../../libopenmpt/src/main/cpp
            ${CMAKE_CURRENT_SOURCE_DIR}/../../../../libopenmpt/src/main/cpp
        NO_DEFAULT_PATH
    )
    if(LIBOPENMPT_INCLUDE_DIR_FOUND)
        set(LIBOPENMPT_INCLUDE_DIR ${LIBOPENMPT_INCLUDE_DIR_FOUND})
    else()
        # Try system paths
        find_path(LIBOPENMPT_INCLUDE_DIR_FOUND NAMES libopenmpt/libopenmpt.h)
        if(LIBOPENMPT_INCLUDE_DIR_FOUND)
            set(LIBOPENMPT_INCLUDE_DIR ${LIBOPENMPT_INCLUDE_DIR_FOUND})
        endif()
    endif()
endif()

if(NOT LIBOPENMPT_LIBRARY)
    # Check in resources directory first (bundled library)
    find_library(LIBOPENMPT_LIBRARY_FOUND
        NAMES openmpt openmpt.0 libopenmpt.so.0 libopenmpt.so.0.8.3
        PATHS
            ${CMAKE_CURRENT_SOURCE_DIR}/../resources/native/linux-x64
            ${CMAKE_CURRENT_SOURCE_DIR}/../resources/native/macos-x64
            ${CMAKE_CURRENT_SOURCE_DIR}/../resources/native/macos-arm64
            /usr/lib
            /usr/local/lib
            /usr/lib/x86_64-linux-gnu
        NO_DEFAULT_PATH
    )
    if(LIBOPENMPT_LIBRARY_FOUND)
        set(LIBOPENMPT_LIBRARY ${LIBOPENMPT_LIBRARY_FOUND})
    else()
        # Try system paths
        find_library(LIBOPENMPT_LIBRARY_FOUND NAMES openmpt)
        if(LIBOPENMPT_LIBRARY_FOUND)
            set(LIBOPENMPT_LIBRARY ${LIBOPENMPT_LIBRARY_FOUND})
        endif()
    endif()
endif()

message(STATUS "LIBOPENMPT_INCLUDE_DIR=${LIBOPENMPT_INCLUDE_DIR}")
message(STATUS "LIBOPENMPT_LIBRARY=${LIBOPENMPT_LIBRARY}")

if(NOT LIBOPENMPT_INCLUDE_DIR)
    message(FATAL_ERROR "Could not find libopenmpt include directory. Please set LIBOPENMPT_INCLUDE_DIR")
endif()

if(NOT LIBOPENMPT_LIBRARY)
    message(WARNING "Could not find libopenmpt library. Library will need to be available at runtime.")
endif()

# Create the shared library
add_library(modplayer_desktop SHARED
    desktop_mod_player_jni.cpp
)

# Include directories
target_include_directories(modplayer_desktop PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${LIBOPENMPT_INCLUDE_DIR}
)

# Link against libopenmpt
if(LIBOPENMPT_LIBRARY)
    target_link_libraries(modplayer_desktop PRIVATE
        ${LIBOPENMPT_LIBRARY}
    )
else()
    # Just link by name, expect it at runtime
    target_link_libraries(modplayer_desktop PRIVATE
        openmpt
    )
endif()

# Platform-specific settings
if(APPLE)
    # macOS: Set rpath to look in same directory as library
    set_target_properties(modplayer_desktop PROPERTIES
        INSTALL_RPATH "@loader_path"
        BUILD_WITH_INSTALL_RPATH TRUE
    )
elseif(UNIX)
    # Linux: Set rpath to look in same directory as library
    set_target_properties(modplayer_desktop PROPERTIES
        INSTALL_RPATH "$ORIGIN"
        BUILD_WITH_INSTALL_RPATH TRUE
    )
endif()

# Output directory
set_target_properties(modplayer_desktop PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../resources/native/${CMAKE_SYSTEM_NAME}-${CMAKE_SYSTEM_PROCESSOR}
)

# Custom target to normalize output directory name
# (convert Linux-x86_64 to linux-x64, Darwin-arm64 to macos-arm64, etc.)
if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
    if(CMAKE_SYSTEM_PROCESSOR STREQUAL "x86_64")
        set(PLATFORM_DIR "linux-x64")
    elseif(CMAKE_SYSTEM_PROCESSOR STREQUAL "aarch64")
        set(PLATFORM_DIR "linux-arm64")
    else()
        set(PLATFORM_DIR "linux-${CMAKE_SYSTEM_PROCESSOR}")
    endif()
elseif(CMAKE_SYSTEM_NAME STREQUAL "Darwin")
    if(CMAKE_SYSTEM_PROCESSOR STREQUAL "x86_64")
        set(PLATFORM_DIR "macos-x64")
    elseif(CMAKE_SYSTEM_PROCESSOR STREQUAL "arm64")
        set(PLATFORM_DIR "macos-arm64")
    else()
        set(PLATFORM_DIR "macos-${CMAKE_SYSTEM_PROCESSOR}")
    endif()
elseif(CMAKE_SYSTEM_NAME STREQUAL "Windows")
    if(CMAKE_SYSTEM_PROCESSOR STREQUAL "AMD64")
        set(PLATFORM_DIR "windows-x64")
    elseif(CMAKE_SYSTEM_PROCESSOR STREQUAL "ARM64")
        set(PLATFORM_DIR "windows-arm64")
    else()
        set(PLATFORM_DIR "windows-${CMAKE_SYSTEM_PROCESSOR}")
    endif()
else()
    set(PLATFORM_DIR "${CMAKE_SYSTEM_NAME}-${CMAKE_SYSTEM_PROCESSOR}")
endif()

message(STATUS "Platform directory: ${PLATFORM_DIR}")

set_target_properties(modplayer_desktop PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../resources/native/${PLATFORM_DIR}
)

# Print summary
message(STATUS "")
message(STATUS "=== modplayer_desktop build configuration ===")
message(STATUS "  Platform: ${CMAKE_SYSTEM_NAME} ${CMAKE_SYSTEM_PROCESSOR}")
message(STATUS "  Output dir: ${CMAKE_CURRENT_SOURCE_DIR}/../resources/native/${PLATFORM_DIR}")
message(STATUS "  C++ Standard: C++${CMAKE_CXX_STANDARD}")
message(STATUS "==============================================")
