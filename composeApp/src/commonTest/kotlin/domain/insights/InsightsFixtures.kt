package domain.insights

import domain.insights.model.CachedInsights
import domain.insights.model.CoachAction
import domain.insights.model.CoachCard
import domain.insights.model.DayCount
import domain.insights.model.HabitsSection
import domain.insights.model.InsightSectionKey
import domain.insights.model.InsightWord
import domain.insights.model.InsightsScreen
import domain.insights.model.LevelCount
import domain.insights.model.LockedSection
import domain.insights.model.MasterySection
import domain.insights.model.Metric
import domain.insights.model.WeeklyHero
import domain.insights.model.WordChip
import domain.insights.model.WordsSection
import kotlinx.datetime.LocalDate

object InsightsFixtures {
    val MONDAY: LocalDate = LocalDate(2026, 10, 5)

    fun hero(
        reviews: Metric = Metric(212, 194),
        accuracy: Metric? = Metric(84, 79),
        leveledUp: Metric = Metric(31, 22),
        streak: Int = 6,
    ) = WeeklyHero(
        headline = "Your best week in 12 weeks",
        reviews = reviews,
        accuracyPct = accuracy,
        leveledUp = leveledUp,
        currentStreak = streak,
        week = (0..6).map { DayCount(LocalDate.fromEpochDays(MONDAY.toEpochDays() + it), if (it < 5) 30 else 0) },
    )

    fun card(
        type: String = "SLIPPING_WORDS",
        action: CoachAction = CoachAction.ReviewWords("Review 3 words", listOf(1, 2, 3)),
    ) = CoachCard(
        id = "$type:2026-10-10",
        type = type,
        title = "3 words need a refresh",
        body = "They dropped a stage this week.",
        words = listOf(WordChip(1, "ubiquitous"), WordChip(2, "ephemeral"), WordChip(3, "laconic")),
        moreWordsCount = 0,
        action = action,
    )

    fun screen(
        coach: List<CoachCard> = listOf(card()),
        hero: WeeklyHero = hero(),
        habits: HabitsSection? = null,
        locked: List<LockedSection> = listOf(LockedSection(InsightSectionKey.HABITS, 18)),
    ) = InsightsScreen(
        totalReviews = 1420,
        hero = hero,
        coach = coach,
        mastery = MasterySection("31 words moved up a stage this week", (0..6).map { LevelCount(it, 10L * it) }, 31, 12),
        habits = habits,
        words = WordsSection("Focus here for quick wins", listOf(InsightWord(1, "ubiquitous", 33)), emptyList()),
        wordRush = null,
        locked = locked,
    )

    fun cached(screen: InsightsScreen = screen(), fetchedAtMs: Long = 1_000L) = CachedInsights(screen, fetchedAtMs)
}
