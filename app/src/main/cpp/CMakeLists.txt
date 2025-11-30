cmake_minimum_required(VERSION 3.22.1)

project("openmptdemo")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find Oboe package (using prefab)
find_package(oboe REQUIRED CONFIG)

# Add libopenmpt as an imported library
add_library(openmpt SHARED IMPORTED)
set_target_properties(openmpt PROPERTIES
    IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopenmpt.so
)

# Include directories
include_directories(
    ${CMAKE_SOURCE_DIR}/../../../libopenmpt/src/main/cpp
    ${CMAKE_SOURCE_DIR}/../../../libopenmpt/src/main/cpp/libopenmpt
)

# Main native library
add_library(modplayer SHARED
    mod_player_jni.cpp
    ModPlayerEngine.cpp
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
