package com.beyondeye.openmptdemo

import androidx.compose.ui.window.ComposeUIViewController
import com.beyondeye.openmptdemo.di.appModule
import org.koin.core.context.startKoin

/**
 * iOS entry point for the OpenMPT Demo application.
 * 
 * Returns a UIViewController that hosts the Compose Multiplatform UI.
 * This function is called from Swift code (e.g., in the iOS app's SceneDelegate or SwiftUI App).
 */
fun MainViewController() = ComposeUIViewController { App() }

/**
 * Initialize Koin for iOS.
 * 
 * This should be called from Swift code before accessing any Koin-injected dependencies.
 * Typically called in the iOS app's application delegate or main entry point.
 */
fun initKoin() {
    startKoin {
        modules(appModule)
    }
}

//TODO remember to add here logger init when iOS platform will be implemented
// see https://github.com/sergejsha/logger?tab=readme-ov-file#ios-usage
/*
init() {
    LogKt.initializeLogger { builder in
        builder.registerIosLogSink(logPrinter: LogPrinterCompanion.shared.Default)
    }
}

private let TAG = "SampleApp"
LogKt.d(tag: TAG) {
    "debug message"
}

 */