package domain.focus.model

import domain.tag.model.Tag
import domain.word.model.ProgressStats
import utils.Language

/** Which learning language (Word.learningLanguage) the user is currently studying. */
sealed interface LearningFocus {
    data class Single(val language: Language) : LearningFocus
    data object All : LearningFocus
}

data class LanguageSummary(
    val language: Language,
    val wordCount: Int,
    val dueCount: Int,
)

data class FocusedTagStats(
    val tags: List<Tag> = emptyList(),
    val dueTags: List<Tag> = emptyList(),
    val tagsByLevel: Map<Int, List<Tag>> = emptyMap(),
)

/** Everything the Study screen needs, computed from one pass over the word list. */
data class StudyFocusOverview(
    val focus: LearningFocus,
    val languages: List<LanguageSummary>,
    val nudge: LanguageSummary?,
    val showIntro: Boolean,
    val progressStats: ProgressStats,
    val tagStats: FocusedTagStats,
)
