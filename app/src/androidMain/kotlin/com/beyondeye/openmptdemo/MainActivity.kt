package com.beyondeye.openmptdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * Android entry point for the OpenMPT Demo application.
 * 
 * Uses Compose Multiplatform with Koin for dependency injection.
 * The App() composable provides the shared UI across all platforms.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
