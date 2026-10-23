package feature.insights.coach.model

import androidx.compose.runtime.Immutable
import domain.insights.model.CoachAction
import domain.insights.model.InsightWord
import domain.insights.model.WordChip

enum class Trend { UP, DOWN, FLAT }
enum class ChangeUnit { COUNT, POINTS }

/** A big number with its change vs last week; the UI renders arrow + sign + words (never color alone). */
@Immutable
data class StatUi(
    val value: String,
    /** Null when there is nothing to compare against; the UI then shows no change line. */
    val trend: Trend?,
    val changeAmount: Int,
    val unit: ChangeUnit,
)

@Immutable
data class DayDotUi(val isoDay: Int, val reviews: Int, val isToday: Boolean, val isFuture: Boolean)

@Immutable
data class HeroUi(
    val headline: String,
    val reviews: StatUi,
    /** Null below the sample gate: the tile shows "—" with a hint instead of a misleading %. */
    val accuracy: StatUi?,
    val leveledUp: StatUi,
    val streak: Int,
    val week: List<DayDotUi>,
)

@Immutable
data class CoachCardUi(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val words: List<WordChip>,
    val moreWordsCount: Int,
    val action: CoachAction,
    /** Pre-formatted hour for ENABLE_REMINDER cards ("8 PM" / "20:00"). */
    val hourLabel: String?,
)

/** Server-side part of the screen: how this week went and what to do about it. */
@Immutable
data class InsightsUiModel(
    val hero: HeroUi,
    /** Only cards the learner can act on. */
    val coach: List<CoachCardUi>,
    val hardest: List<InsightWord>,
    val isNewUser: Boolean,
)

@Immutable
data class ForecastBarUi(
    val isoDay: Int,
    val reviews: Int,
    /** Height relative to the busiest day, 0..1. */
    val fraction: Float,
    val isToday: Boolean,
    val isBusiest: Boolean,
)

/** Reviews coming due today and the next six days, from local words. */
@Immutable
data class ForecastUi(
    val dueNow: Int,
    val tomorrow: Int,
    /** Reviews on the busiest day after today; null when none are scheduled. */
    val busiest: Int?,
    val bars: List<ForecastBarUi>,
)

/** The library in four plain-language buckets instead of seven internal levels. */
@Immutable
data class StagesUi(val new: Int, val learning: Int, val strong: Int, val mastered: Int) {
    val total: Int get() = new + learning + strong + mastered
}
