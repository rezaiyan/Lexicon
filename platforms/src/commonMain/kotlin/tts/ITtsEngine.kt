package tts

interface ITtsEngine {
    suspend fun initialize(modelPath: String, tokensPath: String, dataDir: String)
    suspend fun synthesizeAndPlay(text: String, speed: Float = 1.0f, speakerId: Int = 0)
    suspend fun stop()
    fun release()
    fun isInitialized(): Boolean
    fun numSpeakers(): Int
}
