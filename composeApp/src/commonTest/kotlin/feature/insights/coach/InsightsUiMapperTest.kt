package feature.insights.coach

import domain.insights.InsightsFixtures
import domain.insights.model.CoachAction
import domain.insights.model.InsightWord
import domain.insights.model.InsightsScreen
import domain.insights.model.Metric
import domain.insights.model.WordsSection
import domain.word.model.ForecastDay
import domain.word.model.ProgressStats
import domain.word.model.ReviewForecast
import feature.insights.coach.model.ChangeUnit
import feature.insights.coach.model.StagesUi
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
    fun `cards without an action are left out`() {
        val trend = InsightsFixtures.card(type = "WEEK_TREND", action = CoachAction.None)
        val coach = map(InsightsFixtures.screen(coach = listOf(trend, InsightsFixtures.card()))).coach
        assertEquals(listOf("SLIPPING_WORDS"), coach.map { it.type })
    }

    @Test
    fun `hardest words are capped at five`() {
        val words = (1..8).map { InsightWord(it.toLong(), "w$it", 20) }
        val screen = InsightsFixtures.screen().copy(words = WordsSection("", words, emptyList()))
        assertEquals((1L..5L).toList(), map(screen).hardest.map { it.id })
    }

    @Test
    fun `forecast scales bars and finds tomorrow and the busiest later day`() {
        val counts = listOf(30, 12, 0, 40, 5, 0, 1)
        val forecast = ReviewForecast(
            dueNow = 25,
            days = counts.mapIndexed { i, n -> ForecastDay(LocalDate.fromEpochDays(wednesday.toEpochDays() + i), n) },
        )

        val ui = InsightsUiMapper.forecast(forecast)

        assertEquals(25, ui.dueNow)
        assertEquals(12, ui.tomorrow)
        assertEquals(40, ui.busiest)
        assertEquals(listOf(3, 4, 5, 6, 7, 1, 2), ui.bars.map { it.isoDay })
        assertEquals(1f, ui.bars[3].fraction)
        assertEquals(0.75f, ui.bars[0].fraction)
        assertEquals(listOf(true, false, false, false, false, false, false), ui.bars.map { it.isToday })
        assertEquals(3, ui.bars.indexOfFirst { it.isBusiest })
    }

    @Test
    fun `forecast with nothing scheduled after today has no busiest day`() {
        val forecast = ReviewForecast(
            dueNow = 0,
            days = (0..6).map { ForecastDay(LocalDate.fromEpochDays(wednesday.toEpochDays() + it), 0) },
        )

        val ui = InsightsUiMapper.forecast(forecast)

        assertNull(ui.busiest)
        assertTrue(ui.bars.none { it.isBusiest })
    }

    @Test
    fun `stages group seven levels into four plain buckets`() {
        val stats = ProgressStats(
            level0Count = 4, level1Count = 3, level2Count = 2, level3Count = 1,
            level4Count = 1, level5Count = 1, level6Count = 5, totalWords = 17,
        )
        assertEquals(StagesUi(new = 4, learning = 5, strong = 3, mastered = 5), InsightsUiMapper.stages(stats))
    }

    @Test
    fun `stages are null for an empty library`() {
        assertNull(InsightsUiMapper.stages(ProgressStats()))
    }

    @Test
    fun `new user flag when no reviews at all`() {
        assertTrue(map(InsightsFixtures.screen().copy(totalReviews = 0)).isNewUser)
    }
}
