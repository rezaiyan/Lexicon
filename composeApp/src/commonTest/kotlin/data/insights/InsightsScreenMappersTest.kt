package data.insights

import data.insights.remote.CoachActionDto
import data.insights.remote.CoachCardDto
import data.insights.remote.DayCountDto
import data.insights.remote.HeroDto
import data.insights.remote.InsightsScreenDto
import data.insights.remote.LockedSectionDto
import domain.insights.model.CoachAction
import domain.insights.model.InsightSectionKey
import domain.insights.model.LockedSection
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InsightsScreenMappersTest {

    private fun card(kind: String, hour: Int? = null, type: String = "X") = CoachCardDto(
        id = "id", type = type, title = "t",
        action = CoachActionDto(kind = kind, label = "Go", wordIds = listOf(1, 2), hour = hour),
    )

    @Test
    fun `maps known action kinds`() {
        assertEquals(CoachAction.ReviewWords("Go", listOf(1, 2)), card("REVIEW_WORDS").toDomain().action)
        assertEquals(CoachAction.StartReview("Go"), card("START_REVIEW").toDomain().action)
        assertEquals(CoachAction.EnableReminder("Go", 20), card("ENABLE_REMINDER", hour = 20).toDomain().action)
        assertEquals(CoachAction.StartWordRush("Go"), card("START_WORD_RUSH").toDomain().action)
    }

    @Test
    fun `unknown kind or reminder without hour becomes None`() {
        assertEquals(CoachAction.None, card("TELEPORT").toDomain().action)
        assertEquals(CoachAction.None, card("ENABLE_REMINDER", hour = null).toDomain().action)
    }

    @Test
    fun `unknown card type is kept for generic rendering`() {
        assertEquals("BRAND_NEW_TYPE", card("NONE", type = "BRAND_NEW_TYPE").toDomain().type)
    }

    @Test
    fun `malformed dates are dropped and unknown locked sections ignored`() {
        val dto = InsightsScreenDto(
            hero = HeroDto(week = listOf(DayCountDto("2026-10-05", 3), DayCountDto("garbage", 9))),
            locked = listOf(LockedSectionDto("habits", 18), LockedSectionDto("future_section", 5)),
        )
        val screen = dto.toDomain()
        assertEquals(listOf(LocalDate(2026, 10, 5)), screen.hero.week.map { it.date })
        assertEquals(listOf(LockedSection(InsightSectionKey.HABITS, 18)), screen.locked)
    }

    @Test
    fun `null sections stay null`() {
        val screen = InsightsScreenDto(hero = HeroDto()).toDomain()
        assertTrue(screen.mastery == null && screen.habits == null && screen.words == null && screen.wordRush == null)
    }
}
