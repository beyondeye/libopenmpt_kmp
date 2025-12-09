package com.beyondeye.openmpt.core

/**
 * Factory function to create a platform-specific ModPlayer instance.
 * 
 * Each platform provides its own implementation:
 * - Android: AndroidModPlayer using JNI + libopenmpt + Oboe
 * - iOS: IosModPlayer (stub for now)
 * - Desktop: DesktopModPlayer (stub for now)
 * - Wasm/JS: WasmModPlayer (stub for now)
 */
expect fun createModPlayer(): ModPlayer
