package feature.insights.coach

import domain.insights.model.CoachAction
import domain.insights.model.CoachCard
import domain.insights.model.HabitsSection
import domain.insights.model.InsightSectionKey
import domain.insights.model.InsightsScreen
import domain.insights.model.LockedSection
import domain.insights.model.MasterySection
import domain.insights.model.Metric
import domain.insights.model.WeeklyHero
import domain.insights.model.WordRushSection
import domain.insights.model.WordsSection
import feature.insights.coach.model.ChangeUnit
import feature.insights.coach.model.CoachCardUi
import feature.insights.coach.model.DayDotUi
import feature.insights.coach.model.DeepDiveUi
import feature.insights.coach.model.HeatCellUi
import feature.insights.coach.model.HeroUi
import feature.insights.coach.model.InsightsUiModel
import feature.insights.coach.model.LevelBarUi
import feature.insights.coach.model.SectionKind
import feature.insights.coach.model.StatUi
import feature.insights.coach.model.Trend
import feature.insights.coach.model.WeekdayBarUi
import kotlinx.datetime.LocalDate
import kotlin.math.abs

/** Domain → UI. Every spec §6 display rule that is not wording lives here. */
object InsightsUiMapper {

    /** Must match the server's MIN_REVIEWS_PER_BUCKET (habits unlock threshold). */
    private const val HABITS_UNLOCK_REVIEWS = 30
    private const val HEAT_LEVELS = 4

    fun map(screen: InsightsScreen, dismissed: Set<String>, today: LocalDate, use24Hour: Boolean) = InsightsUiModel(
        hero = screen.hero.toUi(today),
        coach = screen.coach.filterNot { it.id in dismissed }.map { it.toUi(use24Hour) },
        sections = sections(screen, use24Hour),
        isNewUser = screen.totalReviews == 0L,
    )

    private fun WeeklyHero.toUi(today: LocalDate) = HeroUi(
        headline = headline,
        reviews = reviews.toStat(InsightsFormatter.compactCount(reviews.value), ChangeUnit.COUNT),
        accuracy = accuracyPct?.let { it.toStat("${it.value}%", ChangeUnit.POINTS) },
        leveledUp = leveledUp.toStat(InsightsFormatter.compactCount(leveledUp.value), ChangeUnit.COUNT),
        streak = currentStreak,
        week = week.map { day ->
            DayDotUi(
                isoDay = day.date.dayOfWeek.ordinal + 1,
                reviews = day.reviews,
                isToday = day.date == today,
                isFuture = day.date > today,
            )
        },
    )

    private fun Metric.toStat(display: String, unit: ChangeUnit): StatUi {
        val delta = change
        return StatUi(
            value = display,
            trend = when {
                delta == null -> null
                delta > 0 -> Trend.UP
                delta < 0 -> Trend.DOWN
                else -> Trend.FLAT
            },
            changeAmount = abs(delta ?: 0),
            unit = unit,
        )
    }

    private fun CoachCard.toUi(use24Hour: Boolean) = CoachCardUi(
        id = id,
        type = type,
        title = title,
        body = body,
        words = words,
        moreWordsCount = moreWordsCount,
        action = action,
        hourLabel = (action as? CoachAction.EnableReminder)?.let { InsightsFormatter.hour(it.hour, use24Hour) },
    )

    private fun sections(screen: InsightsScreen, use24Hour: Boolean): List<DeepDiveUi> {
        val locked = screen.locked.associateBy { it.section }
        return listOfNotNull(
            screen.mastery?.toUi() ?: locked[InsightSectionKey.MASTERY]?.toUi(SectionKind.MASTERY),
            screen.habits?.toUi(use24Hour) ?: locked[InsightSectionKey.HABITS]?.toUi(SectionKind.HABITS),
            screen.words?.toUi() ?: locked[InsightSectionKey.WORDS]?.toUi(SectionKind.WORDS),
            screen.wordRush?.toUi() ?: locked[InsightSectionKey.WORD_RUSH]?.toUi(SectionKind.WORD_RUSH),
        )
    }

    private fun LockedSection.toUi(kind: SectionKind) = DeepDiveUi.Locked(
        kind = kind,
        reviewsNeeded = reviewsNeeded,
        progress = ((HABITS_UNLOCK_REVIEWS - reviewsNeeded).toFloat() / HABITS_UNLOCK_REVIEWS).coerceIn(0f, 1f),
    )

    private fun MasterySection.toUi(): DeepDiveUi.Mastery {
        val max = levels.maxOfOrNull { it.words }?.takeIf { it > 0 } ?: 1L
        return DeepDiveUi.Mastery(
            caption = caption,
            levels = levels.map { LevelBarUi(it.level, it.words, it.words.toFloat() / max) },
            promoted = promotedThisWeek,
            demoted = demotedThisWeek,
        )
    }

    private fun HabitsSection.toUi(use24Hour: Boolean): DeepDiveUi.Habits {
        val maxReviews = heatmap.maxOfOrNull { it.reviews }?.takeIf { it > 0 } ?: 1
        val bestDay = weekdays.maxByOrNull { it.accuracyPct }?.isoDay
        return DeepDiveUi.Habits(
            caption = caption,
            heatmap = heatmap.map { day ->
                val intensity = ((day.reviews.toFloat() / maxReviews) * HEAT_LEVELS).toInt().coerceIn(1, HEAT_LEVELS)
                HeatCellUi(day.date.toEpochDays().toLong(), day.reviews, intensity)
            },
            bestHourLabel = bestHour?.let { InsightsFormatter.hour(it.hour, use24Hour) },
            bestHourAccuracy = bestHour?.accuracyPct,
            weekdays = weekdays.map { WeekdayBarUi(it.isoDay, it.accuracyPct, it.isoDay == bestDay) },
        )
    }

    private fun WordsSection.toUi() = DeepDiveUi.Words(caption, hardest, comebacks)

    private fun WordRushSection.toUi() = DeepDiveUi.WordRush(
        caption = caption,
        gamesPlayed = InsightsFormatter.compactCount(gamesPlayed),
        bestScore = InsightsFormatter.compactCount(bestScore),
        recentScores = recentScores,
    )
}
