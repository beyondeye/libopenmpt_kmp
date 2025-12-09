package com.beyondeye.openmpt.core

/**
 * Wasm/JS implementation of the ModPlayer factory.
 * Creates a WasmModPlayer instance.
 */
actual fun createModPlayer(): ModPlayer = WasmModPlayer()
