package com.beyondeye.openmpt.core

/**
 * iOS implementation of the ModPlayer factory.
 * Creates an IosModPlayer instance.
 */
actual fun createModPlayer(): ModPlayer = IosModPlayer()
