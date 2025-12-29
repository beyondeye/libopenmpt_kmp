import SwiftUI
import app

/// Main entry point for the iOS application.
/// This SwiftUI App struct initializes Koin and presents the Compose Multiplatform UI.
@main
struct iOSApp: App {
    
    init() {
        // Initialize Koin for dependency injection
        MainViewControllerKt.doInitKoin()
        
        // Initialize logger for iOS
        // Using the logger library's iOS initialization
        LoggerKt.initializeLogger { builder in
            builder.registerIosLogSink(logPrinter: LogPrinterCompanion.shared.Default)
        }
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
