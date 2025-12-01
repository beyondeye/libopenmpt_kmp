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

// Task to export built libraries to app's jniLibs (Debug variant)
tasks.register<Copy>("exportPrebuiltLibsDebug") {
    description = "Export built libopenmpt.so files (Debug) to app/src/main/jniLibs"
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
    into(rootProject.file("app/src/main/jniLibs"))
}

// Task to export built libraries to app's jniLibs (Release variant)
tasks.register<Copy>("exportPrebuiltLibsRelease") {
    description = "Export built libopenmpt.so files (Release) to app/src/main/jniLibs"
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
    into(rootProject.file("app/src/main/jniLibs"))
}

// Convenience task to export both variants
tasks.register("exportPrebuiltLibs") {
    description = "Export both Debug and Release libopenmpt.so files to app/src/main/jniLibs"
    dependsOn("exportPrebuiltLibsDebug", "exportPrebuiltLibsRelease")
}
