package feature.insights.coach

import domain.insights.InsightsFixtures
import domain.insights.model.BestHour
import domain.insights.model.CoachAction
import domain.insights.model.DayCount
import domain.insights.model.HabitsSection
import domain.insights.model.InsightsScreen
import domain.insights.model.Metric
import domain.insights.model.WeekdayAccuracy
import feature.insights.coach.model.ChangeUnit
import feature.insights.coach.model.DeepDiveUi
import feature.insights.coach.model.SectionKind
import feature.insights.coach.model.StatUi
import feature.insights.coach.model.Trend
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InsightsUiMapperTest {

    private val wednesday = LocalDate(2026, 10, 7)
    private fun map(
        screen: InsightsScreen = InsightsFixtures.screen(),
        dismissed: Set<String> = emptySet(),
        use24: Boolean = false,
    ) = InsightsUiMapper.map(screen, dismissed, today = wednesday, use24Hour = use24)

    @Test
    fun `hero stats carry trend and change`() {
        val hero = map().hero
        assertEquals(StatUi("212", Trend.UP, 18, ChangeUnit.COUNT), hero.reviews)
        assertEquals(StatUi("84%", Trend.UP, 5, ChangeUnit.POINTS), hero.accuracy)
        assertEquals(StatUi("31", Trend.UP, 9, ChangeUnit.COUNT), hero.leveledUp)
    }

    @Test
    fun `hero accuracy null below sample gate and flat when unchanged`() {
        val hero = map(InsightsFixtures.screen(hero = InsightsFixtures.hero(accuracy = null, reviews = Metric(5, 5)))).hero
        assertNull(hero.accuracy)
        assertEquals(Trend.FLAT, hero.reviews.trend)
    }

    @Test
    fun `no comparison when last week had too little data`() {
        val hero = map(InsightsFixtures.screen(hero = InsightsFixtures.hero(accuracy = Metric(84, null)))).hero
        assertEquals(StatUi("84%", null, 0, ChangeUnit.POINTS), hero.accuracy)
    }

    @Test
    fun `large counts are compact`() {
        val hero = map(InsightsFixtures.screen(hero = InsightsFixtures.hero(reviews = Metric(1249, 2000)))).hero
        assertEquals(StatUi("1.2k", Trend.DOWN, 751, ChangeUnit.COUNT), hero.reviews)
    }

    @Test
    fun `week dots mark today and future`() {
        val week = map().hero.week
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), week.map { it.isoDay })
        assertTrue(week[2].isToday)
        assertTrue(week[3].isFuture && !week[1].isFuture)
    }

    @Test
    fun `dismissed coach cards are filtered`() {
        val card = InsightsFixtures.card()
        assertTrue(map(dismissed = setOf(card.id)).coach.isEmpty())
    }

    @Test
    fun `reminder card gets a localized hour label`() {
        val card = InsightsFixtures.card(type = "BEST_TIME", action = CoachAction.EnableReminder("Remind me", 20))
        assertEquals("8 PM", map(InsightsFixtures.screen(coach = listOf(card))).coach.single().hourLabel)
        assertEquals("20:00", map(InsightsFixtures.screen(coach = listOf(card)), use24 = true).coach.single().hourLabel)
    }

    @Test
    fun `sections keep fixed order and locked ones become teasers`() {
        val sections = map().sections
        assertEquals(listOf(SectionKind.MASTERY, SectionKind.HABITS, SectionKind.WORDS), sections.map { it.kind })
        val locked = sections[1] as DeepDiveUi.Locked
        assertEquals(18, locked.reviewsNeeded)
        assertEquals(0.4f, locked.progress, 0.01f) // (30 - 18) / 30
    }

    @Test
    fun `mastery bars are scaled to the largest stage`() {
        val mastery = map().sections.first() as DeepDiveUi.Mastery
        assertEquals(1f, mastery.levels.last().fraction)
        assertEquals(0f, mastery.levels.first().fraction)
    }

    @Test
    fun `habits mark the best weekday and bucket heat`() {
        val habits = HabitsSection(
            caption = "You're sharpest on Tuesdays",
            heatmap = listOf(DayCount(LocalDate(2026, 10, 1), 2), DayCount(LocalDate(2026, 10, 2), 40)),
            bestHour = BestHour(20, 91),
            weekdays = listOf(WeekdayAccuracy(1, 70, 40), WeekdayAccuracy(2, 88, 40)),
        )
        val ui = map(InsightsFixtures.screen(habits = habits, locked = emptyList())).sections
            .filterIsInstance<DeepDiveUi.Habits>().single()
        assertEquals("8 PM", ui.bestHourLabel)
        assertEquals(listOf(false, true), ui.weekdays.map { it.isBest })
        assertEquals(listOf(1, 4), ui.heatmap.map { it.intensity })
    }

    @Test
    fun `new user flag when no reviews at all`() {
        assertTrue(map(InsightsFixtures.screen().copy(totalReviews = 0)).isNewUser)
    }
}
