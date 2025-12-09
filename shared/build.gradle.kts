import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    // Apply default hierarchy template to create intermediate source sets (iosMain, appleMain, etc.)
    applyDefaultHierarchyTemplate()
    
    // Android target
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    // iOS targets
    iosArm64()
    iosSimulatorArm64()
    
    // Desktop (JVM) target
    jvm("desktop")
    
    // Wasm/JS target
    wasmJs {
        browser()
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
            }
        }
        
        // iosMain is automatically created by the Default Hierarchy Template
        val iosMain by getting
        
        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        
        val wasmJsMain by getting
    }
}

android {
    namespace = "com.beyondeye.openmpt.core"
    compileSdk = 36
    
    // Map KMP source set directories to Android source sets
    // This ensures jniLibs from androidMain are properly packaged into the AAR/APK
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/androidMain/jniLibs")
        }
    }
    
    defaultConfig {
        minSdk = 24
        
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += listOf(
                    "-DANDROID_STL=c++_shared"
                )
            }
        }
        
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    buildFeatures {
        prefab = true
    }
    
    externalNativeBuild {
        cmake {
            path = file("src/androidMain/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    
    // jniLibs are automatically picked up from src/androidMain/jniLibs by KMP
    // Note: Using a single jniLibs directory (not debug/release specific) is required
    // for proper packaging of native libraries in the AAR when using KMP.
}

dependencies {
    // Oboe for audio playback on Android
    implementation(libs.oboe)
}

/**
 * Task Dependencies for Native Build with libopenmpt
 * 
 * This configuration ensures that libopenmpt.so is built and exported to the appropriate
 * jniLibs directory (jniLibsDebug or jniLibsRelease) BEFORE any CMake-related tasks run.
 * 
 * Why is this needed?
 * - The shared module's native code (modplayer) depends on libopenmpt.so
 * - CMake needs the .so file to exist during BOTH configuration and build phases
 * - Without these dependencies, CMake would fail with "missing file" errors
 * 
 * Task patterns covered:
 * - configureCMake*      : CMake configuration phase - validates library paths and generates build files
 * - buildCMake*          : CMake build phase - compiles native code and links against libopenmpt
 * - externalNativeBuild* : Android Gradle Plugin's native build orchestration task
 * - merge*JniLibFolders  : Merges JNI libraries from different source sets into final APK/AAR
 * - generateJsonModel*   : Generates CMake JSON model for IDE integration (Android Studio)
 * 
 * Build variant matching:
 * - Debug tasks   → :libopenmpt:exportPrebuiltLibsDebug   (debug-optimized .so with symbols)
 * - Release tasks → :libopenmpt:exportPrebuiltLibsRelease (release-optimized .so, stripped)
 */
afterEvaluate {
    // Task name patterns that require libopenmpt.so to exist before execution
    val taskPatterns = listOf(
        { name: String -> name.startsWith("configureCMake") },      // CMake configuration phase
        { name: String -> name.startsWith("buildCMake") },          // CMake build phase
        { name: String -> name.startsWith("externalNativeBuild") }, // AGP native build orchestration
        { name: String -> name.startsWith("merge") && name.contains("JniLibFolders") }, // JNI library merging
        { name: String -> name.startsWith("generateJsonModel") }    // CMake JSON model for IDE
    )
    
    taskPatterns.forEach { pattern ->
        tasks.matching { pattern(it.name) }.configureEach {
            // Match build variant to appropriate libopenmpt export task
            val exportTask = when {
                name.contains("Debug", ignoreCase = true) -> ":libopenmpt:exportPrebuiltLibsDebug"
                name.contains("Release", ignoreCase = true) -> ":libopenmpt:exportPrebuiltLibsRelease"
                else -> ":libopenmpt:exportPrebuiltLibsDebug" // Default to debug for safety
            }
            dependsOn(exportTask)
        }
    }
}
