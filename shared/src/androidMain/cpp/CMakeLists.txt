cmake_minimum_required(VERSION 3.22.1)

project("openmptcore")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find Oboe package (using prefab)
find_package(oboe REQUIRED CONFIG)

# Determine the library path based on build type
# Debug builds use jniLibsDebug, Release builds use jniLibsRelease
if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(OPENMPT_JNILIB_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibsDebug")
else()
    set(OPENMPT_JNILIB_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibsRelease")
endif()

# Add libopenmpt as an imported library
add_library(openmpt SHARED IMPORTED)
set_target_properties(openmpt PROPERTIES
    IMPORTED_LOCATION ${OPENMPT_JNILIB_DIR}/${ANDROID_ABI}/libopenmpt.so
)

# Fallback: try merged_jni_libs if direct path doesn't exist yet
if(NOT EXISTS ${OPENMPT_JNILIB_DIR}/${ANDROID_ABI}/libopenmpt.so)
    if(CMAKE_BUILD_TYPE STREQUAL "Debug")
        set_target_properties(openmpt PROPERTIES
            IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../../../build/intermediates/merged_jni_libs/debug/out/${ANDROID_ABI}/libopenmpt.so
        )
    else()
        set_target_properties(openmpt PROPERTIES
            IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../../../build/intermediates/merged_jni_libs/release/out/${ANDROID_ABI}/libopenmpt.so
        )
    endif()
endif()

# Main native library
add_library(modplayer SHARED
    mod_player_jni.cpp
    ModPlayerEngine.cpp
)

# Add include directories to the target
# Path explanation: from shared/src/androidMain/cpp/ we go up 4 levels to reach project root
#   .. -> shared/src/androidMain/
#   ../.. -> shared/src/
#   ../../.. -> shared/
#   ../../../.. -> project root (/home/ddt/Work/OpenMPTDemo)
target_include_directories(modplayer PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../libopenmpt/src/main/cpp
)

# Link libraries
target_link_libraries(modplayer
    android
    log
    openmpt
    oboe::oboe
)

# Export ANativeActivity_onCreate for native code debugging
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -u ANativeActivity_onCreate")
