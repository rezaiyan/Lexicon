package domain.listening.model

import domain.word.model.LearningStage

/** Which words a listening session plays. */
sealed interface ListeningSource {
    /** Words due for review now, most overdue first. */
    data object Due : ListeningSource

    /** Every word in the learning focus, newest first. */
    data object All : ListeningSource

    data class Level(val stage: LearningStage) : ListeningSource

    data class Tag(val tagId: Long) : ListeningSource
}

/** What to listen to: [source], capped at [limit] words ([LIMIT_ALL] for no cap), optionally shuffled. */
data class ListeningSelection(
    val source: ListeningSource = ListeningSource.Due,
    val limit: Int = DEFAULT_LIMIT,
    val shuffle: Boolean = false,
) {
    /** How many words a session plays when [available] words match the source. */
    fun sessionSize(available: Int): Int = if (limit == LIMIT_ALL) available else minOf(limit, available)

    companion object {
        const val LIMIT_ALL = 0
        const val DEFAULT_LIMIT = 20
        val LIMIT_OPTIONS = listOf(10, 20, 30, 50)
    }
}

data class ListeningLevelOption(val stage: LearningStage, val count: Int)

data class ListeningTagOption(val tagId: Long, val name: String, val count: Int)

/** Word counts per source within the learning focus; levels and tags without words are left out. */
data class ListeningOptions(
    val dueCount: Int = 0,
    val allCount: Int = 0,
    val levels: List<ListeningLevelOption> = emptyList(),
    val tags: List<ListeningTagOption> = emptyList(),
) {
    val hasWords: Boolean get() = allCount > 0

    fun count(source: ListeningSource): Int = when (source) {
        ListeningSource.Due -> dueCount
        ListeningSource.All -> allCount
        is ListeningSource.Level -> levels.firstOrNull { it.stage == source.stage }?.count ?: 0
        is ListeningSource.Tag -> tags.firstOrNull { it.tagId == source.tagId }?.count ?: 0
    }

    /**
     * The selection to offer: [selection] as saved, unless its source has no words right now
     * (nothing due today, a deleted tag) — then every word, so the start button always works.
     */
    fun resolve(selection: ListeningSelection): ListeningSelection =
        if (count(selection.source) > 0) selection else selection.copy(source = ListeningSource.All)
}
