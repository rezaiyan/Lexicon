package tts

interface ITtsEngine {
    /** [noiseScale] is fixed per load — changing it means initializing again. */
    suspend fun initialize(
        modelPath: String,
        tokensPath: String,
        dataDir: String,
        noiseScale: Float = DEFAULT_NOISE_SCALE,
    )
    suspend fun synthesizeAndPlay(text: String, speed: Float = 1.0f, speakerId: Int = 0)
    suspend fun stop()
    fun release()
    fun isInitialized(): Boolean
    fun numSpeakers(): Int

    companion object {
        const val DEFAULT_NOISE_SCALE = 0.667f
    }
}
