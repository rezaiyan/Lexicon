package domain.tts.model

sealed class TtsState {
    data object Idle : TtsState()
    data class Downloading(val languageCode: String, val progress: Float) : TtsState()
    data object Loading : TtsState()
    data object Speaking : TtsState()
    data class Error(val message: String) : TtsState()
}
