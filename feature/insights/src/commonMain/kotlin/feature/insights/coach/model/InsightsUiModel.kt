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

enum class SectionKind { MASTERY, HABITS, WORDS, WORD_RUSH }

@Immutable
data class LevelBarUi(val level: Int, val words: Long, val fraction: Float)

@Immutable
data class HeatCellUi(val epochDay: Long, val reviews: Int, val intensity: Int)

@Immutable
data class WeekdayBarUi(val isoDay: Int, val accuracyPct: Int, val isBest: Boolean)

@Immutable
sealed interface DeepDiveUi {
    val kind: SectionKind
    val caption: String

    data class Mastery(
        override val caption: String,
        val levels: List<LevelBarUi>,
        val promoted: Int,
        val demoted: Int,
    ) : DeepDiveUi {
        override val kind = SectionKind.MASTERY
    }

    data class Habits(
        override val caption: String,
        val heatmap: List<HeatCellUi>,
        val bestHourLabel: String?,
        val bestHourAccuracy: Int?,
        val weekdays: List<WeekdayBarUi>,
    ) : DeepDiveUi {
        override val kind = SectionKind.HABITS
    }

    data class Words(
        override val caption: String,
        val hardest: List<InsightWord>,
        val comebacks: List<WordChip>,
    ) : DeepDiveUi {
        override val kind = SectionKind.WORDS
    }

    data class WordRush(
        override val caption: String,
        val gamesPlayed: String,
        val bestScore: String,
        val recentScores: List<Int>,
    ) : DeepDiveUi {
        override val kind = SectionKind.WORD_RUSH
    }

    /** Not enough data yet: teaser with progress instead of an empty chart. */
    data class Locked(override val kind: SectionKind, val reviewsNeeded: Int, val progress: Float) : DeepDiveUi {
        override val caption: String = ""
    }
}

@Immutable
data class InsightsUiModel(
    val hero: HeroUi,
    val coach: List<CoachCardUi>,
    val sections: List<DeepDiveUi>,
    val isNewUser: Boolean,
)
