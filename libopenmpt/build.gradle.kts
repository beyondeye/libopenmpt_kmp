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
        release {
            isMinifyEnabled = false
        }
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/cpp/Android.mk")
        }
    }
}

// Task to export built libraries to app's jniLibs
tasks.register<Copy>("exportPrebuiltLibs") {
    description = "Export built libopenmpt.so files to app/src/main/jniLibs"
    dependsOn("build")
    
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
