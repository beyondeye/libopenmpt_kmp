package com.beyondeye.openmptdemo.di

import com.beyondeye.openmpt.core.ModPlayer
import com.beyondeye.openmpt.core.createModPlayer
import com.beyondeye.openmptdemo.ModPlayerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin dependency injection module for the OpenMPT Demo application.
 * 
 * Provides:
 * - ModPlayer: Platform-specific implementation via createModPlayer() factory
 * - ModPlayerViewModel: Main ViewModel for the player UI
 */
val appModule = module {
    // Platform-specific ModPlayer instance
    factory<ModPlayer> { createModPlayer() }
    
    // ViewModel with injected ModPlayer
    factory { ModPlayerViewModel(get()) } //TODO this was viewModel: try if it works with viewModel instead of factory
}
