package domain.listening.model

/** Which side of the card is read first. */
enum class ListeningOrder { WORD_FIRST, TRANSLATION_FIRST }

/** User preferences for listening mode. Speech rate is shared with the review screen's TTS setting. */
data class ListeningSettings(
    val pauseMs: Long = DEFAULT_PAUSE_MS,
    val order: ListeningOrder = ListeningOrder.WORD_FIRST,
    val repeatCount: Int = MIN_REPEAT,
) {
    /** Silence between one word's answer and the next word's prompt. */
    val gapMs: Long get() = maxOf(pauseMs / 2, MIN_GAP_MS)

    companion object {
        const val DEFAULT_PAUSE_MS = 3_000L
        const val MIN_GAP_MS = 800L
        const val MIN_REPEAT = 1
        const val MAX_REPEAT = 2
        val PAUSE_OPTIONS_MS = listOf(2_000L, 3_000L, 5_000L)
    }
}
