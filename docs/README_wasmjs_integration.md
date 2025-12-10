## Future Migration to AudioWorklet

The current implementation uses ScriptProcessorNode (deprecated but widely supported). To migrate to AudioWorklet:

1. Create `libopenmpt-worklet.js` AudioWorkletProcessor
2. Create `AudioWorkletPlayer.kt` wrapper
3. Load worklet module and establish message passing
4. Replace ScriptProcessorNode with AudioWorkletNode
