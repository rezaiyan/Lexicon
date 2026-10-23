package feature.insights.coach

import domain.insights.model.CoachAction
import domain.insights.model.CoachCard
import domain.insights.model.InsightsScreen
import domain.insights.model.Metric
import domain.insights.model.WeeklyHero
import domain.word.model.ProgressStats
import domain.word.model.ReviewForecast
import feature.insights.coach.model.ChangeUnit
import feature.insights.coach.model.CoachCardUi
import feature.insights.coach.model.DayDotUi
import feature.insights.coach.model.ForecastBarUi
import feature.insights.coach.model.ForecastUi
import feature.insights.coach.model.HeroUi
import feature.insights.coach.model.InsightsUiModel
import feature.insights.coach.model.StagesUi
import feature.insights.coach.model.StatUi
import feature.insights.coach.model.Trend
import kotlinx.datetime.LocalDate
import kotlin.math.abs

/** Domain → UI. Every spec §6 display rule that is not wording lives here. */
object InsightsUiMapper {

    /** Hardest words listed on screen; the review button covers the same set. */
    const val HARDEST_SHOWN = 5

    fun map(screen: InsightsScreen, dismissed: Set<String>, today: LocalDate, use24Hour: Boolean) = InsightsUiModel(
        hero = screen.hero.toUi(today),
        // A card without an action only restates numbers the hero already shows.
        coach = screen.coach
            .filterNot { it.id in dismissed || it.action == CoachAction.None }
            .map { it.toUi(use24Hour) },
        hardest = screen.words?.hardest.orEmpty().take(HARDEST_SHOWN),
        isNewUser = screen.totalReviews == 0L,
    )

    fun forecast(forecast: ReviewForecast): ForecastUi {
        val max = forecast.days.maxOfOrNull { it.reviews }?.takeIf { it > 0 } ?: 1
        val busiest = forecast.days.drop(1).maxByOrNull { it.reviews }?.takeIf { it.reviews > 0 }
        return ForecastUi(
            dueNow = forecast.dueNow,
            tomorrow = forecast.days.getOrNull(1)?.reviews ?: 0,
            busiest = busiest?.reviews,
            bars = forecast.days.mapIndexed { index, day ->
                ForecastBarUi(
                    isoDay = day.date.dayOfWeek.ordinal + 1,
                    reviews = day.reviews,
                    fraction = day.reviews.toFloat() / max,
                    isToday = index == 0,
                    isBusiest = day == busiest,
                )
            },
        )
    }

    /** Level 0 new, 1–2 learning, 3–5 strong, 6 mastered. Null for an empty library. */
    fun stages(stats: ProgressStats): StagesUi? = StagesUi(
        new = stats.level0Count,
        learning = stats.level1Count + stats.level2Count,
        strong = stats.level3Count + stats.level4Count + stats.level5Count,
        mastered = stats.level6Count,
    ).takeIf { it.total > 0 }

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
}
