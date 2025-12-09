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
