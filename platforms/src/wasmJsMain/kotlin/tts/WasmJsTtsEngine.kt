package tts

class WasmJsTtsEngine : ITtsEngine {
    override suspend fun initialize(modelPath: String, tokensPath: String, dataDir: String) {
        // TTS not supported on WasmJs
    }

    override suspend fun synthesizeAndPlay(text: String) {
        // TTS not supported on WasmJs
    }

    override suspend fun stop() {
        // No-op
    }

    override fun release() {
        // No-op
    }

    override fun isInitialized(): Boolean = false

    override fun numSpeakers(): Int = 1
}
