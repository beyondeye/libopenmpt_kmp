package com.beyondeye.openmptdemo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.beyondeye.openmptdemo.di.appModule
import de.halfbit.logger.initializeLogger
import de.halfbit.logger.sink.println.registerPrintlnSink
import org.koin.core.context.startKoin

/**
 * Desktop (JVM) entry point for the OpenMPT Demo application.
 * 
 * Initializes Koin and launches the application window.
 */
fun main() = application {
    initializeLogger {
        registerPrintlnSink()
    }
    // Initialize Koin before launching the app
    startKoin {
        modules(appModule)
    }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "LibOpenMPT Demo Player"
    ) {
        App()
    }
}
