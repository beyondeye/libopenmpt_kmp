plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.beyondeye.openmpt.lib"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        
        externalNativeBuild {
            ndkBuild {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a")
            }
        }
    }

    buildTypes {
        debug {
            externalNativeBuild {
                ndkBuild {
                    // Pass NDK_DEBUG=1 for debug builds
                    arguments("NDK_DEBUG=1")
                }
            }
        }
        release {
            isMinifyEnabled = false
            externalNativeBuild {
                ndkBuild {
                    // Pass NDK_DEBUG=0 for release builds
                    arguments("NDK_DEBUG=0")
                }
            }
        }
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/cpp/Android.mk")
        }
    }
}

// Task to export built libraries to shared module's jniLibs (Debug variant)
// Note: Both debug and release export to the same directory. In a KMP project,
// using a single jniLibs directory is required for proper packaging in the AAR.
tasks.register<Copy>("exportPrebuiltLibsDebug") {
    description = "Export built libopenmpt.so files (Debug) to shared/src/androidMain/jniLibs"
    dependsOn("externalNativeBuildDebug")
    
    // Find all libopenmpt.so files in the cxx/Debug directory
    from(layout.buildDirectory.dir("intermediates/cxx/Debug")) {
        include("**/obj/local/armeabi-v7a/libopenmpt.so")
        include("**/obj/local/arm64-v8a/libopenmpt.so")
        eachFile {
            // Flatten the directory structure to maintain ABI folders
            relativePath = RelativePath(true, *relativePath.segments.takeLast(2).toTypedArray())
        }
        includeEmptyDirs = false
    }
    into(rootProject.file("shared/src/androidMain/jniLibs"))
}

// Task to export built libraries to shared module's jniLibs (Release variant)
// Note: Both debug and release export to the same directory. In a KMP project,
// using a single jniLibs directory is required for proper packaging in the AAR.
tasks.register<Copy>("exportPrebuiltLibsRelease") {
    description = "Export built libopenmpt.so files (Release) to shared/src/androidMain/jniLibs"
    dependsOn("externalNativeBuildRelease")
    
    // Find all libopenmpt.so files in the cxx/Release directory
    from(layout.buildDirectory.dir("intermediates/cxx/Release")) {
        include("**/obj/local/armeabi-v7a/libopenmpt.so")
        include("**/obj/local/arm64-v8a/libopenmpt.so")
        eachFile {
            // Flatten the directory structure to maintain ABI folders
            relativePath = RelativePath(true, *relativePath.segments.takeLast(2).toTypedArray())
        }
        includeEmptyDirs = false
    }
    into(rootProject.file("shared/src/androidMain/jniLibs"))
}

// Convenience task to export both variants
tasks.register("exportPrebuiltLibs") {
    description = "Export both Debug and Release libopenmpt.so files to shared/src/androidMain/jniLibs"
    dependsOn("exportPrebuiltLibsDebug", "exportPrebuiltLibsRelease")
}

// ============================================================================
// iOS Build Tasks using CMake
// ============================================================================

val iosCmakeDir = file("src/main/cpp/ios")
val iosCppDir = file("src/main/cpp")
val iosBuildDir = layout.buildDirectory.dir("ios").get().asFile
// iOS libs are exported to the shared module where cinterop is configured
val iosOutputDir = rootProject.file("shared/src/iosMain/libs")
val iosHeadersDir = rootProject.file("shared/src/iosMain/headers/libopenmpt")

// Task to create iOS build directories
tasks.register("createIosBuildDirs") {
    description = "Create iOS build directories"
    group = "ios build"
    
    doLast {
        File(iosBuildDir, "arm64-device").mkdirs()
        File(iosBuildDir, "arm64-simulator").mkdirs()
    }
}

// Task to configure CMake for iOS arm64 (physical devices)
tasks.register<Exec>("configureIosArm64") {
    description = "Configure CMake for iOS arm64 (device)"
    group = "ios build"
    dependsOn("createIosBuildDirs")
    
    val buildDir = File(iosBuildDir, "arm64-device")
    
    doFirst {
        buildDir.mkdirs()
    }
    
    workingDir(buildDir)
    
    commandLine(
        "cmake",
        "-G", "Xcode",
        "-DCMAKE_SYSTEM_NAME=iOS",
        "-DCMAKE_OSX_ARCHITECTURES=arm64",
        "-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0",
        "-DCMAKE_OSX_SYSROOT=iphoneos",
        "-DCMAKE_BUILD_TYPE=Release",
        iosCmakeDir.absolutePath
    )
}

// Task to build libopenmpt for iOS arm64 (physical devices)
tasks.register<Exec>("buildIosArm64") {
    description = "Build libopenmpt.a for iOS arm64 (device)"
    group = "ios build"
    dependsOn("configureIosArm64")
    
    val buildDir = File(iosBuildDir, "arm64-device")
    
    workingDir(buildDir)
    
    commandLine("cmake", "--build", ".", "--config", "Release")
}

// Task to configure CMake for iOS arm64 simulator (M1/M2 Macs)
tasks.register<Exec>("configureIosSimArm64") {
    description = "Configure CMake for iOS arm64 simulator"
    group = "ios build"
    dependsOn("createIosBuildDirs")
    
    val buildDir = File(iosBuildDir, "arm64-simulator")
    
    doFirst {
        buildDir.mkdirs()
    }
    
    workingDir(buildDir)
    
    commandLine(
        "cmake",
        "-G", "Xcode",
        "-DCMAKE_SYSTEM_NAME=iOS",
        "-DCMAKE_OSX_ARCHITECTURES=arm64",
        "-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0",
        "-DCMAKE_OSX_SYSROOT=iphonesimulator",
        "-DCMAKE_BUILD_TYPE=Release",
        iosCmakeDir.absolutePath
    )
}

// Task to build libopenmpt for iOS arm64 simulator (M1/M2 Macs)
tasks.register<Exec>("buildIosSimArm64") {
    description = "Build libopenmpt.a for iOS arm64 simulator"
    group = "ios build"
    dependsOn("configureIosSimArm64")
    
    val buildDir = File(iosBuildDir, "arm64-simulator")
    
    workingDir(buildDir)
    
    commandLine("cmake", "--build", ".", "--config", "Release")
}

// Task to create XCFramework from both architectures
tasks.register<Exec>("createIosXCFramework") {
    description = "Create XCFramework from iOS arm64 device and simulator libraries"
    group = "ios build"
    dependsOn("buildIosArm64", "buildIosSimArm64")
    
    val xcframeworkDir = File(iosBuildDir, "libopenmpt.xcframework")
    val deviceLib = File(iosBuildDir, "arm64-device/lib/Release/libopenmpt.a")
    val simulatorLib = File(iosBuildDir, "arm64-simulator/lib/Release/libopenmpt.a")
    
    doFirst {
        // Remove existing xcframework
        xcframeworkDir.deleteRecursively()
    }
    
    commandLine(
        "xcodebuild", "-create-xcframework",
        "-library", deviceLib.absolutePath,
        "-headers", File(iosCppDir, "libopenmpt").absolutePath,
        "-library", simulatorLib.absolutePath,
        "-headers", File(iosCppDir, "libopenmpt").absolutePath,
        "-output", xcframeworkDir.absolutePath
    )
}

// Task to copy headers to shared module
tasks.register<Copy>("copyIosHeaders") {
    description = "Copy libopenmpt headers to shared/src/iosMain/headers"
    group = "ios build"
    
    from(File(iosCppDir, "libopenmpt")) {
        include("libopenmpt.h")
        include("libopenmpt_config.h")
        include("libopenmpt_version.h")
    }
    into(iosHeadersDir)
    
    doFirst {
        iosHeadersDir.mkdirs()
    }
}

// Task to export XCFramework to shared module
tasks.register<Copy>("exportIosLibs") {
    description = "Export iOS XCFramework to shared/src/iosMain/libs"
    group = "ios build"
    dependsOn("createIosXCFramework", "copyIosHeaders")
    
    from(File(iosBuildDir, "libopenmpt.xcframework"))
    into(File(iosOutputDir, "libopenmpt.xcframework"))
    
    doFirst {
        iosOutputDir.mkdirs()
    }
}

// Convenience task to build all iOS artifacts
tasks.register("buildIos") {
    description = "Build libopenmpt for all iOS architectures and export to shared module"
    group = "ios build"
    dependsOn("exportIosLibs")
}
