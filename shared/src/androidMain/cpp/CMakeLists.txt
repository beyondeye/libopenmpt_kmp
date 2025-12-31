cmake_minimum_required(VERSION 3.22.1)

project("openmptcore")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find Oboe package (using prefab)
find_package(oboe REQUIRED CONFIG)

# Path to libopenmpt.so - using single jniLibs directory for KMP compatibility
# Note: In KMP projects, using a single jniLibs directory (not debug/release specific)
# is required for proper packaging of native libraries in the AAR.
set(OPENMPT_JNILIB_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs")

# Add libopenmpt as an imported library
add_library(openmpt SHARED IMPORTED)
set_target_properties(openmpt PROPERTIES
    IMPORTED_LOCATION ${OPENMPT_JNILIB_DIR}/${ANDROID_ABI}/libopenmpt.so
)

# Fallback: try merged_jni_libs if direct path doesn't exist yet
if(NOT EXISTS ${OPENMPT_JNILIB_DIR}/${ANDROID_ABI}/libopenmpt.so)
    set_target_properties(openmpt PROPERTIES
        IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../../../build/intermediates/merged_jni_libs/debug/out/${ANDROID_ABI}/libopenmpt.so
    )
endif()

# Main native library
add_library(modplayer SHARED
    mod_player_jni.cpp
    ModPlayerEngine.cpp
)

# Enable 16 KB page size support for Android 15+ compatibility
# This ensures LOAD segments are aligned at 16 KB boundaries
target_link_options(modplayer PRIVATE "-Wl,-z,max-page-size=16384")

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
