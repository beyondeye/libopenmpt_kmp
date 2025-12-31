import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.publish)
}

// ============================================================================
// Publishing Configuration
// ============================================================================
val libraryGroup: String by project
val libraryArtifactId: String by project
val libraryVersion: String by project
val libraryName: String by project
val libraryDescription: String by project
val pomUrl: String by project
val pomScmUrl: String by project
val pomScmConnection: String by project
val pomScmDevConnection: String by project
val pomLicenseName: String by project
val pomLicenseUrl: String by project
val pomDeveloperId: String by project
val pomDeveloperName: String by project

group = libraryGroup
version = libraryVersion

kotlin {
    // Apply default hierarchy template to create intermediate source sets (iosMain, appleMain, etc.)
    applyDefaultHierarchyTemplate()
    
    // Android target
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        // Enable publishing for Android target
        publishLibraryVariants("release")
    }
    
    // iOS targets with cinterop for libopenmpt
    // NOTE: The static library is linked here to properly resolve symbols.
    // When publishing this library, consumers must provide their own libopenmpt.xcframework.
    iosArm64 {
        compilations.getByName("main") {
            cinterops {
                val libopenmpt by creating {
                    defFile(project.file("src/nativeInterop/cinterop/libopenmpt.def"))
                    includeDirs(project.file("src/iosMain/headers"))
                    // Link the static library for iOS arm64 (device)
                    extraOpts(
                        "-libraryPath", "${project.file("src/iosMain/libs/libopenmpt.xcframework/ios-arm64").absolutePath}",
                        "-staticLibrary", "libopenmpt.a"
                    )
                }
            }
        }
    }
    iosSimulatorArm64 {
        compilations.getByName("main") {
            cinterops {
                val libopenmpt by creating {
                    defFile(project.file("src/nativeInterop/cinterop/libopenmpt.def"))
                    includeDirs(project.file("src/iosMain/headers"))
                    // Link the static library for iOS arm64 simulator
                    extraOpts(
                        "-libraryPath", "${project.file("src/iosMain/libs/libopenmpt.xcframework/ios-arm64-simulator").absolutePath}",
                        "-staticLibrary", "libopenmpt.a"
                    )
                }
            }
        }
    }
    
    // Desktop (JVM) target
    jvm("desktop")
    
    // Wasm/JS target
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.logger)
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
//
android {
    namespace = "com.beyondeye.openmpt.core"
    compileSdk = 36
    
    // Map KMP source set directories to Android source sets
    // This ensures jniLibs from androidMain are properly packaged into the AAR/APK
    // Note: Release libraries are in jniLibs/, Debug libraries in jniLibsDebug/
    sourceSets {
        getByName("main") {
            // Release jniLibs - used for publishing and release builds
            jniLibs.srcDirs("src/androidMain/jniLibs")
        }
        getByName("debug") {
            // Debug jniLibs - separate directory to avoid task dependency conflicts
            jniLibs.srcDirs("src/androidMain/jniLibsDebug")
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
    
    // ============================================================================
    // Native Library Packaging Configuration
    // ============================================================================
    // 
    // The jniLibs directories (jniLibs/ for release, jniLibsDebug/ for debug) are the
    // AUTHORITATIVE source for libopenmpt.so. These pre-built libraries are committed
    // to source control, allowing the shared module to be built without requiring
    // the libopenmpt module to be rebuilt.
    //
    // However, CMake also references libopenmpt.so as a SHARED IMPORTED library
    // (see src/androidMain/cpp/CMakeLists.txt) so that modplayer.so can link against it.
    // AGP automatically tries to package any SHARED IMPORTED library, causing a
    // duplicate file conflict with the jniLibs version.
    //
    // The pickFirsts rule resolves this by using the jniLibs version (which is processed
    // first in the merge order), ensuring the committed pre-built library takes precedence.
    // See: https://developer.android.com/r/tools/jniLibs-vs-imported-targets
    //
    packaging {
        jniLibs {
            pickFirsts.add("lib/*/libopenmpt.so")
        }
    }
}

dependencies {
    // Oboe for audio playback on Android
    implementation(libs.oboe)
}

/**
 * Native Library Configuration for libopenmpt
 * 
 * IMPORTANT: The shared module relies on pre-committed native libraries (libopenmpt.so).
 * These libraries should be committed to the repository and are NOT rebuilt during normal builds.
 * 
 * Directory structure:
 * - shared/src/androidMain/jniLibs/        : Release-optimized .so files (used for publishing)
 * - shared/src/androidMain/jniLibsDebug/   : Debug .so files with symbols (for local development)
 * 
 * To rebuild native libraries from source, use the "Rebuild Native Libraries" GitHub workflow
 * or run the following Gradle tasks manually:
 * - ./gradlew :libopenmpt:exportPrebuiltLibsRelease  (for release builds)
 * - ./gradlew :libopenmpt:exportPrebuiltLibsDebug    (for debug builds)
 * 
 * See docs/README_building.md for complete build instructions.
 */

// ============================================================================
// Maven Publishing Configuration (Vanniktech Maven Publish Plugin)
// ============================================================================

/**
 * Configure publishing for Maven Central via the new Central Portal.
 * 
 * This uses the Vanniktech Maven Publish plugin which handles:
 * - Automatic POM generation
 * - GPG signing
 * - Publishing to Maven Central via the new Central Portal API
 * 
 * Required environment variables (for CI) or gradle.properties (for local):
 * - mavenCentralUsername: Central Portal username (from https://central.sonatype.com/account)
 * - mavenCentralPassword: Central Portal password/token
 * - signingInMemoryKey: ASCII-armored GPG private key
 * - signingInMemoryKeyPassword: GPG key passphrase
 * 
 * For local development, configure in ~/.gradle/gradle.properties:
 *   mavenCentralUsername=your-username
 *   mavenCentralPassword=your-password
 *   signing.keyId=LAST_8_CHARS_OF_KEY_ID
 *   signing.password=your-gpg-passphrase
 *   signing.secretKeyRingFile=/path/to/secring.gpg
 */
mavenPublishing {
    // Sonatype host and signing are configured via gradle.properties:
    // SONATYPE_HOST=CENTRAL_PORTAL
    // RELEASE_SIGNING_ENABLED=true
    
    // Configure POM metadata
    pom {
        name.set(libraryName)
        description.set(libraryDescription)
        url.set(pomUrl)
        
        licenses {
            license {
                name.set(pomLicenseName)
                url.set(pomLicenseUrl)
                distribution.set("repo")
            }
        }
        
        developers {
            developer {
                id.set(pomDeveloperId)
                name.set(pomDeveloperName)
            }
        }
        
        scm {
            url.set(pomScmUrl)
            connection.set(pomScmConnection)
            developerConnection.set(pomScmDevConnection)
        }
    }
    
    // Configure coordinates
    coordinates(libraryGroup, libraryArtifactId, libraryVersion)
}
