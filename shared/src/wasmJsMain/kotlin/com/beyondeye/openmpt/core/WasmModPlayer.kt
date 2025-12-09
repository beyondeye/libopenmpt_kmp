package com.beyondeye.openmpt.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wasm/JS implementation of the ModPlayer interface.
 * Currently a stub - will be implemented with libopenmpt WASM + Web Audio API.
 */
class WasmModPlayer : ModPlayer {
    
    private val _playbackStateFlow = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackStateFlow: StateFlow<PlaybackState> = _playbackStateFlow.asStateFlow()
    
    private val _positionFlow = MutableStateFlow(0.0)
    override val positionFlow: StateFlow<Double> = _positionFlow.asStateFlow()
    
    override fun loadModule(data: ByteArray): Boolean {
        TODO("WasmJS implementation not yet available")
    }
    
    override fun loadModuleFromPath(path: String): Boolean {
        TODO("WasmJS implementation not yet available")
    }
    
    override fun release() {
        TODO("WasmJS implementation not yet available")
    }
    
    override fun play() {
        TODO("WasmJS implementation not yet available")
    }
    
    override fun pause() {
        TODO("WasmJS implementation not yet available")
    }
    
    override fun stop() {
        TODO("WasmJS implementation not yet available")
    }
    
    override fun seek(positionSeconds: Double) {
        TODO("WasmJS implementation not yet available")
    }
    
    override fun setRepeatCount(count: Int) {
        TODO("WasmJS implementation not yet available")
    }
    
    override fun setMasterGain(gainMillibel: Int) {
        TODO("WasmJS implementation not yet available")
    }
    
    override fun setStereoSeparation(percent: Int) {
        TODO("WasmJS implementation not yet available")
    }
    
    override fun setPlaybackSpeed(speed: Double) {
        TODO("WasmJS implementation not yet available")
    }
    
    override fun getPlaybackSpeed(): Double {
        TODO("WasmJS implementation not yet available")
    }
    
    override fun setPitch(pitch: Double) {
        TODO("WasmJS implementation not yet available")
    }
    
    override fun getPitch(): Double {
        TODO("WasmJS implementation not yet available")
    }
    
    override val playbackState: PlaybackState
        get() = _playbackStateFlow.value
    
    override val isPlaying: Boolean
        get() = false
    
    override val positionSeconds: Double
        get() = 0.0
    
    override val durationSeconds: Double
        get() = 0.0
    
    override fun getMetadata(): ModMetadata {
        return ModMetadata()
    }
    
    override fun getCurrentOrder(): Int = -1
    
    override fun getCurrentPattern(): Int = -1
    
    override fun getCurrentRow(): Int = -1
    
    override fun getNumChannels(): Int = 0
}
