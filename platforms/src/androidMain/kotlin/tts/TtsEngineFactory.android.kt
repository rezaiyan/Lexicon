package tts

actual fun createTtsEngine(): ITtsEngine {
    return AndroidTtsEngine()
}
