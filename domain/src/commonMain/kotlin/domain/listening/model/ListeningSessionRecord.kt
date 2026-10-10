package domain.listening.model

import domain.word.model.Word

/** A word whose answer was spoken during a session, with its language pair codes. */
data class HeardWord(
    val wordId: Int,
    val sourceLanguage: String,
    val targetLanguage: String,
    val heardAt: Long,
)

/** One finished listening session, as synced to the backend for future study insights. */
data class ListeningSessionRecord(
    val clientSessionId: String,
    val selection: ListeningSelection,
    val order: ListeningOrder,
    val repeatCount: Int,
    val pauseMs: Long,
    val speechRate: Float,
    val plannedWords: Int,
    val heardWords: List<HeardWord>,
    val wordsSkipped: Int,
    val pauseCount: Int,
    /** Time spent playing, excluding the user's pauses. */
    val listeningMs: Long,
    /** Wall-clock time from start to end. */
    val durationMs: Long,
    val completedNormally: Boolean,
    val startedAt: Long,
    val endedAt: Long,
)

/**
 * Accumulates what happens in one listening session. Pure: every event takes its timestamp, so
 * the ViewModel owns the clock. Playback starts running at [startedAt].
 */
data class ListeningSessionTracker(
    val clientSessionId: String,
    val selection: ListeningSelection,
    val settings: ListeningSettings,
    val speechRate: Float,
    val plannedWords: Int,
    val startedAt: Long,
    val heardWords: List<HeardWord> = emptyList(),
    val wordsSkipped: Int = 0,
    val pauseCount: Int = 0,
    val listenedMs: Long = 0L,
    val playingSince: Long? = startedAt,
) {
    /** Counts each word once, at the first time its answer was spoken (repeats don't add more). */
    fun heard(word: Word, at: Long): ListeningSessionTracker =
        if (heardWords.any { it.wordId == word.id }) {
            this
        } else {
            copy(heardWords = heardWords + HeardWord(word.id, word.sourceLanguage.code, word.targetLanguage.code, at))
        }

    fun skipped(): ListeningSessionTracker = copy(wordsSkipped = wordsSkipped + 1)

    fun paused(at: Long): ListeningSessionTracker {
        val since = playingSince ?: return this
        return copy(
            pauseCount = pauseCount + 1,
            listenedMs = listenedMs + (at - since).coerceAtLeast(0),
            playingSince = null,
        )
    }

    fun resumed(at: Long): ListeningSessionTracker = if (playingSince != null) this else copy(playingSince = at)

    fun end(at: Long, completedNormally: Boolean): ListeningSessionRecord = ListeningSessionRecord(
        clientSessionId = clientSessionId,
        selection = selection,
        order = settings.order,
        repeatCount = settings.repeatCount,
        pauseMs = settings.pauseMs,
        speechRate = speechRate,
        plannedWords = plannedWords,
        heardWords = heardWords,
        wordsSkipped = wordsSkipped,
        pauseCount = pauseCount,
        listeningMs = listenedMs + (playingSince?.let { (at - it).coerceAtLeast(0) } ?: 0L),
        durationMs = (at - startedAt).coerceAtLeast(0),
        completedNormally = completedNormally,
        startedAt = startedAt,
        endedAt = at,
    )
}
