package com.beyondeye.openmpt.core

/**
 * Android implementation of the ModPlayer factory.
 * Creates an AndroidModPlayer instance.
 */
actual fun createModPlayer(): ModPlayer = AndroidModPlayer()
