package domain.insights.model

import kotlinx.datetime.LocalDate

data class InsightsScreen(
    val totalReviews: Long,
    val hero: WeeklyHero,
    val coach: List<CoachCard>,
    val mastery: MasterySection?,
    val habits: HabitsSection?,
    val words: WordsSection?,
    val wordRush: WordRushSection?,
    val locked: List<LockedSection>,
)

/** [previous] is null when last week had too little data to compare (server sample gate). */
data class Metric(val value: Int, val previous: Int?) {
    val change: Int? get() = previous?.let { value - it }
}

data class DayCount(val date: LocalDate, val reviews: Int)

data class WeeklyHero(
    val headline: String,
    val reviews: Metric,
    val accuracyPct: Metric?,
    val leveledUp: Metric,
    val currentStreak: Int,
    /** Monday … Sunday of the current week. */
    val week: List<DayCount>,
)

data class WordChip(val id: Long, val text: String)

sealed interface CoachAction {
    data class ReviewWords(val label: String, val wordIds: List<Long>) : CoachAction
    data class StartReview(val label: String) : CoachAction
    data class EnableReminder(val label: String, val hour: Int) : CoachAction
    data class StartWordRush(val label: String) : CoachAction
    data object None : CoachAction
}

data class CoachCard(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val words: List<WordChip>,
    val moreWordsCount: Int,
    val action: CoachAction,
)

data class LevelCount(val level: Int, val words: Long)

data class MasterySection(
    val caption: String,
    val levels: List<LevelCount>,
    val promotedThisWeek: Int,
    val demotedThisWeek: Int,
)

data class BestHour(val hour: Int, val accuracyPct: Int)

data class WeekdayAccuracy(val isoDay: Int, val accuracyPct: Int, val reviews: Int)

data class HabitsSection(
    val caption: String,
    val heatmap: List<DayCount>,
    val bestHour: BestHour?,
    val weekdays: List<WeekdayAccuracy>,
)

data class InsightWord(val id: Long, val text: String, val accuracyPct: Int)

data class WordsSection(val caption: String, val hardest: List<InsightWord>, val comebacks: List<WordChip>)

data class WordRushSection(val caption: String, val gamesPlayed: Long, val bestScore: Int, val recentScores: List<Int>)

enum class InsightSectionKey { MASTERY, HABITS, WORDS, WORD_RUSH }

data class LockedSection(val section: InsightSectionKey, val reviewsNeeded: Int)

data class CachedInsights(val screen: InsightsScreen, val fetchedAtMs: Long)
