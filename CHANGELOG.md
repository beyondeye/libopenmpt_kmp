# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Maven Central publishing support via Sonatype OSSRH
- GitHub Actions workflow for automated publishing
- Signing configuration for Maven Central compliance

### Changed
- Updated documentation with Maven coordinates and installation instructions

## [1.0.0] - 2025-01-XX

### Added
- Initial release of libopenmpt-kmp
- Kotlin Multiplatform library for MOD music playback using libopenmpt
- Platform support:
  - **Android**: Native implementation with JNI + Oboe audio backend
  - **iOS**: Kotlin/Native cinterop + AudioUnit audio backend
  - **Desktop (JVM)**: JNI + JavaSound audio backend
  - **Web (WASM/JS)**: libopenmpt WASM + Web Audio API
- `ModPlayer` interface with comprehensive playback controls:
  - Load modules from byte arrays or file paths
  - Play, pause, stop, and seek functionality
  - Playback speed and pitch control
  - Master gain and stereo separation settings
  - Repeat mode configuration
- Reactive state management with Kotlin Flows:
  - `playbackStateFlow` for state changes
  - `positionFlow` for position updates
- Module metadata extraction:
  - Title, artist, format, tracker information
  - Channel, pattern, instrument, and sample counts
  - Duration calculation
- Prebuilt native libraries for all supported platforms:
  - Android: `libopenmpt.so` (armeabi-v7a, arm64-v8a)
  - iOS: `libopenmpt.xcframework` (arm64 device, arm64 simulator)
  - Desktop: `libopenmpt.dylib` (macOS arm64), `libopenmpt.so` (Linux x64)
  - Web: `libopenmpt.js` + `libopenmpt.wasm`

### Dependencies
- Kotlin Multiplatform: 2.2.21
- libopenmpt: 0.8.3
- Kotlinx Coroutines: 1.10.2
- Oboe (Android): 1.8.0

## Version History

| Version | Date | libopenmpt | Kotlin | Notes |
|---------|------|------------|--------|-------|
| 1.0.0 | 2025-01 | 0.8.3 | 2.2.21 | Initial release |

---

## Migration Guides

### From Local Dependency to Maven Artifact

If you were previously using the `shared` module as a local project dependency:

1. Remove the local project dependency
2. Add the Maven Central dependency:
   ```kotlin
   implementation("com.beyond-eye:libopenmpt-kmp:1.0.0")
   ```
3. For iOS: Manually provide `libopenmpt.xcframework` (see [docs/README_library_consumers.md](docs/README_library_consumers.md))
4. For Wasm/JS: Copy `libopenmpt.js` and `libopenmpt.wasm` to your resources

---

[Unreleased]: https://github.com/beyondeye/libopenmpt_kmp/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/beyondeye/libopenmpt_kmp/releases/tag/v1.0.0
