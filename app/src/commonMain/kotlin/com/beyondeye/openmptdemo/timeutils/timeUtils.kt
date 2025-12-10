package com.beyondeye.openmptdemo.timeutils

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Format time in seconds to "m:ss" format
 */
fun formatTime(seconds: Double): String {
    val totalSeconds = seconds.roundToInt()
    val minutes = totalSeconds / 60
    val secs = totalSeconds % 60
    return "$minutes:${secs.toString().padStart(2, '0')}"
}

/**
 * Format a decimal number with specified decimal places (multiplatform alternative to String.format)
 */
fun formatDecimal(value: Double, decimals: Int): String {
    val multiplier = 10.0.pow(decimals)
    val rounded = (value * multiplier).roundToInt() / multiplier
    val str = rounded.toString()
    val dotIndex = str.indexOf('.')
    return if (dotIndex == -1) {
        str + "." + "0".repeat(decimals)
    } else {
        val currentDecimals = str.length - dotIndex - 1
        if (currentDecimals < decimals) {
            str + "0".repeat(decimals - currentDecimals)
        } else {
            str.substring(0, dotIndex + decimals + 1)
        }
    }
}
