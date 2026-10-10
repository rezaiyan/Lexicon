package data.insights.remote

import kotlinx.serialization.Serializable

@Serializable
data class InsightsScreenDto(
    val generatedAt: String = "",
    val totalReviews: Long = 0,
    val hero: HeroDto,
    val coach: List<CoachCardDto> = emptyList(),
    val sections: SectionsDto = SectionsDto(),
    val locked: List<LockedSectionDto> = emptyList(),
)

@Serializable data class MetricDto(val value: Int = 0, val previous: Int? = null)
@Serializable data class DayCountDto(val date: String, val reviews: Int = 0)

@Serializable
data class HeroDto(
    val headline: String = "",
    val reviews: MetricDto = MetricDto(),
    val accuracyPct: MetricDto? = null,
    val leveledUp: MetricDto = MetricDto(),
    val currentStreak: Int = 0,
    val week: List<DayCountDto> = emptyList(),
)

@Serializable data class WordChipDto(val id: Long, val text: String)

@Serializable
data class CoachActionDto(
    val kind: String = "NONE",
    val label: String? = null,
    val wordIds: List<Long> = emptyList(),
    val hour: Int? = null,
)

@Serializable
data class CoachCardDto(
    val id: String,
    val type: String = "",
    val priority: Int = 0,
    val title: String,
    val body: String = "",
    val words: List<WordChipDto> = emptyList(),
    val moreWordsCount: Int = 0,
    val action: CoachActionDto = CoachActionDto(),
)

@Serializable
data class SectionsDto(
    val mastery: MasterySectionDto? = null,
    val habits: HabitsSectionDto? = null,
    val words: WordsSectionDto? = null,
    val wordRush: WordRushSectionDto? = null,
)

@Serializable data class LevelCountDto(val level: Int, val words: Long = 0)
@Serializable data class MasterySectionDto(
    val caption: String = "",
    val levels: List<LevelCountDto> = emptyList(),
    val promotedThisWeek: Int = 0,
    val demotedThisWeek: Int = 0,
)
@Serializable data class BestHourDto(val hour: Int, val accuracyPct: Int)
@Serializable data class WeekdayAccuracyDto(val isoDay: Int, val accuracyPct: Int, val reviews: Int = 0)
@Serializable data class HabitsSectionDto(
    val caption: String = "",
    val heatmap: List<DayCountDto> = emptyList(),
    val bestHour: BestHourDto? = null,
    val weekdays: List<WeekdayAccuracyDto> = emptyList(),
)
@Serializable data class WordAccuracyDto(val id: Long, val text: String, val accuracyPct: Int)
@Serializable data class WordsSectionDto(
    val caption: String = "",
    val hardest: List<WordAccuracyDto> = emptyList(),
    val comebacks: List<WordChipDto> = emptyList(),
)
@Serializable data class WordRushSectionDto(
    val caption: String = "",
    val gamesPlayed: Long = 0,
    val bestScore: Int = 0,
    val recentScores: List<Int> = emptyList(),
)
@Serializable data class LockedSectionDto(val section: String, val reviewsNeeded: Int = 0)
