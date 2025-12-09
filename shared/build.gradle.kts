import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
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
        
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        
        val iosMain by creating {
            dependsOn(commonMain)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
        
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
}

dependencies {
    // Oboe for audio playback on Android
    implementation(libs.oboe)
}

// Ensure libopenmpt is built and exported before shared module's native build
afterEvaluate {
    tasks.matching { it.name.startsWith("buildCMake") }.configureEach {
        val exportTask = when {
            name.contains("Debug", ignoreCase = true) -> ":libopenmpt:exportPrebuiltLibsDebug"
            name.contains("Release", ignoreCase = true) -> ":libopenmpt:exportPrebuiltLibsRelease"
            else -> ":libopenmpt:exportPrebuiltLibsDebug"
        }
        dependsOn(exportTask)
    }
    tasks.matching { it.name.startsWith("externalNativeBuild") }.configureEach {
        val exportTask = when {
            name.contains("Debug", ignoreCase = true) -> ":libopenmpt:exportPrebuiltLibsDebug"
            name.contains("Release", ignoreCase = true) -> ":libopenmpt:exportPrebuiltLibsRelease"
            else -> ":libopenmpt:exportPrebuiltLibsDebug"
        }
        dependsOn(exportTask)
    }
}
