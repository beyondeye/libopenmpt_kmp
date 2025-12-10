package com.beyondeye.openmpt.core

/**
 * WasmJS implementation of the ModPlayer factory function.
 */
actual fun createModPlayer(): ModPlayer = WasmModPlayer()
