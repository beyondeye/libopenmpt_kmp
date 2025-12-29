import SwiftUI
import app

/// Main entry point for the iOS application.
/// This SwiftUI App struct initializes Koin and presents the Compose Multiplatform UI.
@main
struct iOSApp: App {
    
    init() {
        // Initialize Koin for dependency injection (this also initializes the logger)
        MainViewControllerKt.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
