package domain.listening.model

import domain.word.model.Word

enum class VoiceStatus { READY, MISSING, UNSUPPORTED }

data class LanguageVoice(val languageCode: String, val status: VoiceStatus)

/** Voice-model readiness for every language a listening queue will speak. */
data class ListeningVoiceCheck(val voices: List<LanguageVoice>) {
    val missing: List<LanguageVoice> get() = voices.filter { it.status == VoiceStatus.MISSING }
    val unsupported: List<LanguageVoice> get() = voices.filter { it.status == VoiceStatus.UNSUPPORTED }

    /** False when no language in the queue can be spoken at all. */
    val canSpeakAnything: Boolean get() = voices.any { it.status != VoiceStatus.UNSUPPORTED }
}

/** A listening queue; [isRecentFallback] is true when nothing was due and recent words were used instead. */
data class ListeningQueue(val words: List<Word>, val isRecentFallback: Boolean)
