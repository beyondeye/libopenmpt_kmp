package com.beyondeye.openmptdemo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import androidx.compose.ui.window.ComposeViewport
import com.beyondeye.openmptdemo.di.appModule
import org.koin.core.context.startKoin

/**
 * WASM/JS entry point for the OpenMPT Demo application.
 * 
 * Initializes Koin and launches the application in a browser canvas.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize Koin before launching the app
    startKoin {
        modules(appModule)
    }

    ComposeViewport(
        configure = {
            isA11YEnabled = false
        }
    ) {
        App()
    }
}
