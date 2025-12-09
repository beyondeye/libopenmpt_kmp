package com.beyondeye.openmpt.core

/**
 * Desktop (JVM) implementation of the ModPlayer factory.
 * Creates a DesktopModPlayer instance.
 */
actual fun createModPlayer(): ModPlayer = DesktopModPlayer()
