# Consuming the OpenMPT Shared Library

This document provides instructions for developers who want to use the `shared` module as a dependency in their own Kotlin Multiplatform projects.

## Maven Coordinates

```kotlin
// Add to your commonMain dependencies
implementation("com.beyond-eye:libopenmpt-kmp:1.0.0")
```

**Available artifacts:**

| Artifact | Platform |
|----------|----------|
| `com.beyond-eye:libopenmpt-kmp:1.0.0` | Common (metadata) |
| `com.beyond-eye:libopenmpt-kmp-androidRelease:1.0.0` | Android (AAR) |
| `com.beyond-eye:libopenmpt-kmp-desktop:1.0.0` | Desktop JVM |
| `com.beyond-eye:libopenmpt-kmp-iosArm64:1.0.0` | iOS arm64 (device) |
| `com.beyond-eye:libopenmpt-kmp-iosSimulatorArm64:1.0.0` | iOS arm64 simulator |
| `com.beyond-eye:libopenmpt-kmp-wasmJs:1.0.0` | Wasm/JS (browser) |

> **Note:** You typically only need to depend on `com.beyond-eye:libopenmpt-kmp:1.0.0` in your commonMain dependencies. Gradle will automatically resolve the correct platform-specific artifacts.

## Usage Scenarios

There are two primary ways to consume the shared module:

### Scenario 1: Local/Monorepo Dependency (e.g., this demo project)

If you're using the `shared` module as a **local project dependency** (like the `app` module in this repository):

| Platform | Setup Required |
|----------|---------------|
| Android | ✅ No action required |
| Desktop (JVM) | ✅ No action required |
| iOS | ✅ No action required |
| Wasm/JS | ⚠️ Manual setup required |

The iOS native library (`libopenmpt.a`) is **automatically linked** via cinterop configuration in the shared module. Consumer apps just need to add the dependency and everything works.

### Scenario 2: Published Maven Artifact Dependency

If you're using the `shared` module as a **published Maven artifact** (e.g., `com.beyondeye.openmpt:shared`):

| Platform | Setup Required |
|----------|---------------|
| Android | ✅ No action required |
| Desktop (JVM) | ✅ No action required |
| iOS | ⚠️ Manual setup required |
| Wasm/JS | ⚠️ Manual setup required |

**Why is iOS different for published artifacts?**
When the shared module is published to Maven, Kotlin/Native klibs cannot include compiled static library binaries. The cinterop bindings are included, but the actual `libopenmpt.a` is not. You must provide it yourself.

---

## Platform-Specific Requirements

### Android ✅ No Action Required

The Android native libraries (`libopenmpt.so`) are bundled in the AAR and will be automatically included in your APK.

### Desktop (JVM) ✅ No Action Required

The Desktop implementation uses JNI with bundled native libraries that are automatically extracted at runtime.

### iOS (Local Dependency) ✅ No Action Required

When using the shared module as a **local project dependency**, the libopenmpt static library is automatically linked via the cinterop configuration. Your app module just needs:

```kotlin
dependencies {
    implementation(project(":shared"))
}
```

No additional `linkerOpts` are required in your iOS framework configuration.

### iOS (Published Artifact) ⚠️ Manual Setup Required

When using the shared module as a **published Maven artifact**, you must provide the native library yourself:

1. **Obtain the libopenmpt XCFramework**
   
   You can build it from source using the `libopenmpt` module:
   ```bash
   ./gradlew :libopenmpt:buildIos
   ```
   
   This creates `libopenmpt.xcframework` in `shared/src/iosMain/libs/`.

2. **Copy the XCFramework to your project**
   
   Copy the XCFramework to your iOS app module:
   ```
   your-app/
   └── src/
       └── iosMain/
           └── libs/
               └── libopenmpt.xcframework/
                   ├── Info.plist
                   ├── ios-arm64/
                   │   └── libopenmpt.a
                   └── ios-arm64-simulator/
                       └── libopenmpt.a
   ```

3. **Add linker options to your iOS framework configuration**
   
   In your app's `build.gradle.kts`:
   
   ```kotlin
   kotlin {
       iosArm64 {
           binaries.framework {
               baseName = "YourAppName"
               isStatic = true
               // Link against libopenmpt static library for iOS device (arm64)
               linkerOpts(
                   "-L${projectDir}/src/iosMain/libs/libopenmpt.xcframework/ios-arm64",
                   "-lopenmpt"
               )
           }
       }
       iosSimulatorArm64 {
           binaries.framework {
               baseName = "YourAppName"
               isStatic = true
               // Link against libopenmpt static library for iOS simulator (arm64)
               linkerOpts(
                   "-L${projectDir}/src/iosMain/libs/libopenmpt.xcframework/ios-arm64-simulator",
                   "-lopenmpt"
               )
           }
       }
   }
   ```

4. **Ensure iOS deployment target compatibility**
   
   The libopenmpt XCFramework is built with iOS deployment target 13.0. Ensure your app's deployment target is 13.0 or higher.

### Wasm/JS ⚠️ Manual Setup Required

For Wasm/JS (browser), you must:

1. **Obtain the libopenmpt WASM build**
   
   Download or build the libopenmpt Emscripten build. You need:
   - `libopenmpt.js` - JavaScript glue code
   - `libopenmpt.wasm` - WebAssembly binary

   You can get prebuilt versions from:
   - [libopenmpt official releases](https://lib.openmpt.org/libopenmpt/)
   - Build from source using Emscripten (see `docs/README_recompiling_libopenmpt_wasm.md`)

2. **Include in your web app**
   
   Copy the files to your web resources directory:
   ```
   your-app/
   └── src/
       └── wasmJsMain/
           └── resources/
               ├── libopenmpt.js
               └── libopenmpt.wasm
   ```

3. **Load the script in your HTML**
   
   The `shared` module's `LibOpenMpt.initializeLibOpenMpt()` function will dynamically load the script if it's available at `./libopenmpt.js`.
   
   Alternatively, you can preload it in your HTML:
   ```html
   <script src="libopenmpt.js"></script>
   ```

4. **Call initialization before use**
   
   ```kotlin
   // In your Kotlin/Wasm code
   import com.beyondeye.openmpt.core.LibOpenMpt
   
   suspend fun setup() {
       val initialized = LibOpenMpt.initializeLibOpenMpt()
       if (!initialized) {
           throw RuntimeException("Failed to initialize libopenmpt")
       }
       // Now you can use ModPlayer
   }
   ```

## Build Configuration Example

Here's a complete example `build.gradle.kts` for a consumer project:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
}

kotlin {
    androidTarget()
    
    iosArm64 {
        binaries.framework {
            baseName = "MyApp"
            isStatic = true
            linkerOpts(
                "-L${projectDir}/src/iosMain/libs/libopenmpt.xcframework/ios-arm64",
                "-lopenmpt"
            )
        }
    }
    
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "MyApp"
            isStatic = true
            linkerOpts(
                "-L${projectDir}/src/iosMain/libs/libopenmpt.xcframework/ios-arm64-simulator",
                "-lopenmpt"
            )
        }
    }
    
    wasmJs {
        browser()
    }
    
    sourceSets {
        commonMain {
            dependencies {
                // Add the shared module dependency
                implementation("com.beyond-eye:libopenmpt-kmp:1.0.0")
            }
        }
    }
}
```

## Troubleshooting

### iOS: Undefined symbols for libopenmpt functions

**Error:**
```
Undefined symbol: _openmpt_module_create_from_memory2
Undefined symbol: _openmpt_module_destroy
...
```

**Solution:** Ensure you have:
1. Copied `libopenmpt.xcframework` to your project
2. Added the correct `linkerOpts` to your iOS targets
3. The path in `linkerOpts` correctly points to the XCFramework location

### iOS: Library built for different iOS version

**Error:**
```
Object file was built for newer 'iOS-simulator' version (X.X) than being linked (Y.Y)
```

**Solution:** Ensure your iOS deployment target matches or exceeds the libopenmpt build target (13.0).

### Wasm/JS: libopenmpt is undefined

**Error:**
```
ReferenceError: libopenmpt is not defined
```

**Solution:**
1. Ensure `libopenmpt.js` is accessible in your web app
2. Call `LibOpenMpt.initializeLibOpenMpt()` before using any libopenmpt functions
3. Wait for the WASM module to fully initialize

### Wasm/JS: Failed to load libopenmpt.js

**Solution:**
1. Check that the file path is correct
2. Ensure your web server is configured to serve `.js` and `.wasm` files
3. Check browser console for CORS errors

## Version Compatibility

| shared module | libopenmpt version | iOS deployment target |
|---------------|--------------------|-----------------------|
| 1.0.0         | 0.8.3              | iOS 13.0+             |

## License

The libopenmpt native library is licensed under BSD. See [libopenmpt license](https://lib.openmpt.org/libopenmpt/) for details.
