# iOS App Module for libopenmpt_kmp

This document describes the iOS app wrapper that runs the Compose Multiplatform UI on iOS devices.

## Overview

The `iosApp` folder contains an Xcode project that wraps the Kotlin Multiplatform `app` module, providing a native iOS application that displays the Compose Multiplatform UI.

## Project Structure

```
iosApp/
├── iosApp.xcodeproj/
│   └── project.pbxproj     # Xcode project configuration
└── iosApp/
    ├── iOSApp.swift        # SwiftUI App entry point
    ├── ContentView.swift   # UIViewControllerRepresentable wrapper
    ├── Info.plist          # iOS app configuration
    └── Assets.xcassets/    # App icons and colors
        ├── Contents.json
        ├── AppIcon.appiconset/
        │   └── Contents.json
        └── AccentColor.colorset/
            └── Contents.json
```

## Key Components

### iOSApp.swift

The main entry point for the iOS application. It:
- Initializes Koin dependency injection via `MainViewControllerKt.doInitKoin()`
- Initializes the logger for iOS platform
- Presents the `ContentView` as the root view

### ContentView.swift

A SwiftUI view that wraps the Compose Multiplatform UI using `UIViewControllerRepresentable`. It:
- Creates a bridge between SwiftUI and UIKit
- Calls `MainViewControllerKt.MainViewController()` to get the Compose UI
- Handles keyboard insets via `.ignoresSafeArea(.keyboard)`

## Building and Running

### Prerequisites

1. **macOS** with Xcode installed
2. **JDK 17+** (for Gradle)
3. **libopenmpt XCFramework** built and placed in `shared/src/iosMain/libs/`

### Build Steps

1. **Build the Kotlin framework for iOS:**

   For iOS Simulator (M1/M2 Macs):
   ```bash
   ./gradlew :app:linkDebugFrameworkIosSimulatorArm64
   ```

   For iOS Device:
   ```bash
   ./gradlew :app:linkDebugFrameworkIosArm64
   ```

2. **Open the Xcode project:**
   ```bash
   open iosApp/iosApp.xcodeproj
   ```

3. **Select a target device/simulator** in Xcode

4. **Build and run** (⌘+R)

### Alternative: Automatic Build via Xcode

The Xcode project includes a build phase script that automatically runs:
```bash
./gradlew :app:embedAndSignAppleFrameworkForXcode
```

This script builds and embeds the Kotlin framework when you build from Xcode.

## Configuration

### Bundle Identifier

The app uses `com.beyondeye.openmptdemo` as the bundle identifier. To change it:
1. Open the project in Xcode
2. Select the target → General → Bundle Identifier
3. Update the identifier

### Development Team

To run on a physical device:
1. Open the project in Xcode
2. Select the target → Signing & Capabilities
3. Select your development team
4. Enable "Automatically manage signing"

### iOS Deployment Target

The minimum iOS version is set to **iOS 15.0**. This can be changed in:
- Xcode: Target → General → Minimum Deployments
- Or in `project.pbxproj`: `IPHONEOS_DEPLOYMENT_TARGET = 15.0`

## Background Audio

The app is configured to support background audio playback via the `UIBackgroundModes` key in `Info.plist`:
```xml
<key>UIBackgroundModes</key>
<array>
    <string>audio</string>
</array>
```

## Framework Search Paths

The Xcode project is configured to find the Kotlin framework at:
```
$(SRCROOT)/../app/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)
```

This path is set in:
- FRAMEWORK_SEARCH_PATHS
- OTHER_LDFLAGS (`-framework app`)

## Troubleshooting

### "Framework not found" Error

If you see "Framework not found app", run:
```bash
./gradlew :app:linkDebugFrameworkIosSimulatorArm64
```
or
```bash
./gradlew :app:linkDebugFrameworkIosArm64
```

### "Module 'app' not found" in Swift

Ensure the Kotlin framework is built and the framework search paths are correct.

### Logger Not Working

Check that logger is initialized in `iOSApp.swift`:
```swift
LoggerKt.initializeLogger { builder in
    builder.registerIosLogSink(logPrinter: LogPrinterCompanion.shared.Default)
}
```

### Simulator Architecture Mismatch

On M1/M2 Macs, use the `IosSimulatorArm64` target:
```bash
./gradlew :app:linkDebugFrameworkIosSimulatorArm64
```

On Intel Macs, use the `IosX64` target (if configured).

## Dependencies

The iOS app depends on:
- **app module** - Kotlin Multiplatform framework with Compose UI
- **shared module** - Contains `IosModPlayer` and `IosAudioEngine`
- **libopenmpt.xcframework** - Native library for mod playback

## Future Improvements

- Add app icon assets
- Implement launch screen
- Add AVAudioSession configuration for better audio handling
- Support for additional iOS-specific features (widgets, shortcuts)
