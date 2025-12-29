import SwiftUI
import UIKit
import app

/// SwiftUI view that wraps the Compose Multiplatform UI.
/// Uses UIViewControllerRepresentable to embed the Kotlin Compose UI in SwiftUI.
struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard) // Compose handles keyboard insets
    }
}

/// UIViewControllerRepresentable wrapper for the Compose Multiplatform UI.
/// This bridges between SwiftUI and the UIKit-based Compose UI.
struct ComposeView: UIViewControllerRepresentable {
    
    func makeUIViewController(context: Context) -> UIViewController {
        // Call the Kotlin function to create the Compose UI view controller
        return MainViewControllerKt.MainViewController()
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // No updates needed - Compose handles its own state
    }
}

#Preview {
    ContentView()
}
