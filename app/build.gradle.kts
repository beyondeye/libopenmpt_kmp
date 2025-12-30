import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    // Apply default hierarchy template
    applyDefaultHierarchyTemplate()
    
    // Android target
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    // iOS targets with framework configuration for Xcode integration
    // The libopenmpt static library is linked via cinterop in the shared module
    iosArm64 {
        binaries.framework {
            baseName = "app"
            isStatic = true
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "app"
            isStatic = true
        }
    }
    
    // Desktop (JVM) target
    jvm("desktop")
    
    // Wasm/JS target
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "openmptdemo.js"
            }
        }
        // We want an executable wasm "app"
        binaries.executable()
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Shared module with ModPlayer
                implementation(project(":shared"))
                
                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)
                
                // Koin DI
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                
                // Coroutines
                implementation(libs.kotlinx.coroutines.core)
                
                // Logging
                implementation(libs.logger)
                
                // File picker
                implementation(libs.filekit.dialogs.compose)
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        
        val androidMain by getting {
            dependencies {
                // Android-specific dependencies
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(libs.kotlinx.coroutines.android)
                
                // Koin Android
                implementation(libs.koin.android)
            }
        }
        
        // iosMain is automatically created by the Default Hierarchy Template
        val iosMain by getting
        
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        
        val wasmJsMain by getting
    }
}

android {
    namespace = "com.beyondeye.openmptdemo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.beyondeye.openmptdemo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Restrict to ABIs supported by our native libraries (libopenmpt, libmodplayer)
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    buildFeatures {
        compose = true
    }
    
    // Configure source sets for KMP
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            res.srcDirs("src/androidMain/res")
        }
    }
}

compose {
    resources {
        publicResClass = true
        packageOfResClass = "com.beyondeye.openmptdemo.resources"
        generateResClass = always
    }
}

// Desktop application configuration
compose.desktop {
    application {
        mainClass = "com.beyondeye.openmptdemo.MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "OpenMPTDemo"
            packageVersion = "1.0.0"
        }
    }
}
