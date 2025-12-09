package com.beyondeye.openmptdemo

import android.app.Application
import com.beyondeye.openmptdemo.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Application class for the OpenMPT Demo Android app.
 * 
 * Initializes Koin dependency injection framework on app startup.
 */
class OpenMPTDemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger()
            androidContext(this@OpenMPTDemoApp)
            modules(appModule)
        }
    }
}
