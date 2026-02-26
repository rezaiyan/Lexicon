package tts

interface ITtsEngine {
    suspend fun initialize(modelPath: String, tokensPath: String, dataDir: String)
    suspend fun synthesizeAndPlay(text: String)
    suspend fun stop()
    fun release()
    fun isInitialized(): Boolean
}
