import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    id("maven-publish")
    id("signing")
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

// ============================================================================
// Maven Publishing Configuration
// ============================================================================

/**
 * Configure publishing for Maven Central via Sonatype OSSRH.
 * 
 * Required environment variables or gradle.properties:
 * - OSSRH_USERNAME / ossrhUsername: Sonatype OSSRH username
 * - OSSRH_PASSWORD / ossrhPassword: Sonatype OSSRH password
 * - GPG_PRIVATE_KEY / signing.key: Base64-encoded GPG private key (for CI)
 * - GPG_PASSPHRASE / signing.password: GPG key passphrase
 * 
 * For local development, configure in ~/.gradle/gradle.properties:
 *   ossrhUsername=your-username
 *   ossrhPassword=your-password
 *   signing.keyId=LAST_8_CHARS_OF_KEY_ID
 *   signing.password=your-gpg-passphrase
 *   signing.secretKeyRingFile=/path/to/secring.gpg
 */
publishing {
    publications {
        withType<MavenPublication> {
            // Artifact ID customization - use libraryArtifactId for the main artifact
            artifactId = when (name) {
                "kotlinMultiplatform" -> libraryArtifactId
                else -> "$libraryArtifactId-$name"
            }
            
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
        }
    }
    
    repositories {
        maven {
            name = "sonatype"
            
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            
            credentials {
                username = project.findProperty("ossrhUsername") as String? 
                    ?: System.getenv("OSSRH_USERNAME") 
                    ?: ""
                password = project.findProperty("ossrhPassword") as String? 
                    ?: System.getenv("OSSRH_PASSWORD") 
                    ?: ""
            }
        }
        
        // Local maven repository for testing
        maven {
            name = "local"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

// Configure signing for all publications
signing {
    // Use in-memory signing key for CI environments
    val signingKey = project.findProperty("signing.key") as String? 
        ?: System.getenv("GPG_PRIVATE_KEY")
    val signingPassword = project.findProperty("signing.password") as String? 
        ?: System.getenv("GPG_PASSPHRASE")
    
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    
    // Sign all publications
    sign(publishing.publications)
}

// Make signing required only when publishing to sonatype (not for local testing)
tasks.withType<Sign>().configureEach {
    onlyIf { 
        gradle.taskGraph.hasTask(":shared:publishAllPublicationsToSonatypeRepository") ||
        gradle.taskGraph.hasTask(":shared:publishToSonatype")
    }
}
