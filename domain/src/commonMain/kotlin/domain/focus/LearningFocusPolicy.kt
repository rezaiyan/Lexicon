package domain.focus

import domain.focus.model.FocusedTagStats
import domain.focus.model.LanguageSummary
import domain.focus.model.LearningFocus
import domain.tag.model.Tag
import domain.word.model.ProgressStats
import domain.word.model.Word
import utils.Language

fun Word.isDue(nowMillis: Long): Boolean = nextReviewDate <= nowMillis

/**
 * The language the user is learning with this word. Onboarding and import store it in
 * [Word.targetLanguage]; [Word.sourceLanguage] holds the user's native language.
 */
val Word.learningLanguage: Language get() = targetLanguage

fun List<Word>.filterBy(focus: LearningFocus): List<Word> = when (focus) {
    LearningFocus.All -> this
    is LearningFocus.Single -> filter { it.learningLanguage == focus.language }
}

/** Pure rules for the learning focus feature. No I/O, no clock — time is passed in. */
object LearningFocusPolicy {

    const val NUDGE_THRESHOLD = 10
    private const val MILLIS_PER_DAY = 86_400_000L

    fun epochDay(nowMillis: Long): Long = nowMillis / MILLIS_PER_DAY

    /** A `Single` focus always implies 2+ languages; ≤1 language behaves exactly like `All`. */
    fun resolve(words: List<Word>, preference: LearningFocus?, nowMillis: Long): LearningFocus {
        val languages = words.mapTo(mutableSetOf()) { it.learningLanguage }
        return when {
            languages.size <= 1 -> LearningFocus.All
            preference == LearningFocus.All -> LearningFocus.All
            preference is LearningFocus.Single && preference.language in languages -> preference
            else -> LearningFocus.Single(smartDefault(words, nowMillis))
        }
    }

    fun summaries(words: List<Word>, focus: LearningFocus, nowMillis: Long): List<LanguageSummary> {
        val active = (focus as? LearningFocus.Single)?.language
        return words.groupBy { it.learningLanguage }
            .map { (language, list) ->
                LanguageSummary(language, wordCount = list.size, dueCount = list.count { it.isDue(nowMillis) })
            }
            .sortedWith(
                compareByDescending<LanguageSummary> { it.language == active }
                    .thenByDescending { it.dueCount }
                    .thenByDescending { it.wordCount }
                    .thenBy { it.language.ordinal }
            )
    }

    fun nudge(
        summaries: List<LanguageSummary>,
        focus: LearningFocus,
        dismissedOnDay: Long?,
        nowMillis: Long,
    ): LanguageSummary? {
        if (focus !is LearningFocus.Single) return null
        if (dismissedOnDay == epochDay(nowMillis)) return null
        return summaries
            .filter { it.language != focus.language && it.dueCount >= NUDGE_THRESHOLD }
            .maxByOrNull { it.dueCount }
    }

    fun progressStats(words: List<Word>, nowMillis: Long): ProgressStats {
        val byLevel = words.groupingBy { it.level }.eachCount()
        return ProgressStats(
            level0Count = byLevel[0] ?: 0,
            level1Count = byLevel[1] ?: 0,
            level2Count = byLevel[2] ?: 0,
            level3Count = byLevel[3] ?: 0,
            level4Count = byLevel[4] ?: 0,
            level5Count = byLevel[5] ?: 0,
            level6Count = byLevel[6] ?: 0,
            totalWords = words.size,
            dueCards = words.count { it.isDue(nowMillis) },
        )
    }

    /**
     * Mirrors the SQL tag queries (getAllTagsWithWordCount / getDueTagWordCounts /
     * getTagWordCountsByLevel) over an already-focused word list. Keeps [tags] input order.
     */
    fun tagStats(
        tags: List<Tag>,
        focusedWords: List<Word>,
        focus: LearningFocus,
        nowMillis: Long,
    ): FocusedTagStats {
        val wordsByTag: Map<Long, List<Word>> = focusedWords
            .flatMap { word -> word.tagIds.map { tagId -> tagId to word } }
            .groupBy({ it.first }, { it.second })

        fun countedTags(predicate: (Word) -> Boolean): List<Tag> = tags.mapNotNull { tag ->
            val count = wordsByTag[tag.id].orEmpty().count(predicate)
            if (count > 0) tag.copy(wordCount = count.toLong()) else null
        }

        val allTags = tags
            .map { tag -> tag.copy(wordCount = wordsByTag[tag.id].orEmpty().size.toLong()) }
            .filter { focus == LearningFocus.All || it.wordCount > 0 }

        val byLevel = focusedWords.map { it.level }.distinct().sorted()
            .associateWith { level -> countedTags { it.level == level } }
            .filterValues { it.isNotEmpty() }

        return FocusedTagStats(
            tags = allTags,
            dueTags = countedTags { it.isDue(nowMillis) },
            tagsByLevel = byLevel,
        )
    }

    /** Most recently studied language; ties broken by most due words, then most words. */
    private fun smartDefault(words: List<Word>, nowMillis: Long): Language =
        words.groupBy { it.learningLanguage }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<Language, List<Word>>> { (_, list) -> list.maxOf { it.lastReviewDate } }
                    .thenByDescending { (_, list) -> list.count { it.isDue(nowMillis) } }
                    .thenByDescending { (_, list) -> list.size }
                    .thenBy { (language, _) -> language.ordinal }
            )
            .first()
            .key
}
