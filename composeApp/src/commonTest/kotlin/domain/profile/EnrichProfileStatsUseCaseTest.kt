package domain.profile

import domain.profile.model.DayActivity
import domain.profile.model.LanguagePair
import domain.profile.model.ProfileStats
import domain.profile.usecase.EnrichProfileStatsUseCase
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnrichProfileStatsUseCaseTest {

    private val useCase = EnrichProfileStatsUseCase()

    private val today = LocalDate(2025, 1, 15) // Wednesday

    private fun stats(
        activity: List<DayActivity> = emptyList(),
        currentStreak: Int = 5,
        longestStreak: Int = 10,
        memberSince: String = "Jan 1, 2024",
        languages: List<LanguagePair> = emptyList(),
    ) = ProfileStats(
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        memberSince = memberSince,
        weeklyActivity = activity,
        languages = languages,
    )

    @Test
    fun `scalar fields are copied unchanged`() {
        val result = useCase(stats(currentStreak = 7, longestStreak = 14, memberSince = "Mar 2024"), today)
        assertEquals(7, result.currentStreak)
        assertEquals(14, result.longestStreak)
        assertEquals("Mar 2024", result.memberSince)
    }

    @Test
    fun `today flag is set correctly`() {
        val activity = listOf(
            DayActivity(date = "2025-01-14", reviewCount = 3),
            DayActivity(date = "2025-01-15", reviewCount = 5),
        )
        val result = useCase(stats(activity), today)
        val todayItem = result.weeklyActivity.find { it.date == "2025-01-15" }!!
        val notToday = result.weeklyActivity.find { it.date == "2025-01-14" }!!
        assertTrue(todayItem.isToday)
        assertFalse(notToday.isToday)
    }

    @Test
    fun `day of month is extracted correctly`() {
        val activity = listOf(DayActivity(date = "2025-01-09", reviewCount = 2))
        val result = useCase(stats(activity), today)
        assertEquals(9, result.weeklyActivity.first().dayOfMonth)
    }

    @Test
    fun `day of week label is set for wednesday`() {
        val activity = listOf(DayActivity(date = "2025-01-15", reviewCount = 1)) // Wednesday
        val result = useCase(stats(activity), today)
        assertEquals("WED", result.weeklyActivity.first().dayOfWeekLabel)
    }

    @Test
    fun `all day of week labels are assigned correctly`() {
        val days = listOf(
            "2025-01-13" to "MON",
            "2025-01-14" to "TUE",
            "2025-01-15" to "WED",
            "2025-01-16" to "THU",
            "2025-01-17" to "FRI",
            "2025-01-18" to "SAT",
            "2025-01-19" to "SUN",
        )
        val activity = days.map { (date, _) -> DayActivity(date = date, reviewCount = 0) }
        val result = useCase(stats(activity), today)
        days.forEach { (date, expected) ->
            val item = result.weeklyActivity.find { it.date == date }!!
            assertEquals(expected, item.dayOfWeekLabel, "Wrong label for $date")
        }
    }

    @Test
    fun `activity is sorted by date ascending`() {
        val activity = listOf(
            DayActivity(date = "2025-01-15", reviewCount = 1),
            DayActivity(date = "2025-01-13", reviewCount = 2),
            DayActivity(date = "2025-01-14", reviewCount = 3),
        )
        val result = useCase(stats(activity), today)
        assertEquals(
            listOf("2025-01-13", "2025-01-14", "2025-01-15"),
            result.weeklyActivity.map { it.date }
        )
    }

    @Test
    fun `review count is preserved`() {
        val activity = listOf(DayActivity(date = "2025-01-15", reviewCount = 42))
        val result = useCase(stats(activity), today)
        assertEquals(42, result.weeklyActivity.first().reviewCount)
    }

    @Test
    fun `empty activity list produces empty enriched list`() {
        val result = useCase(stats(activity = emptyList()), today)
        assertTrue(result.weeklyActivity.isEmpty())
    }

    @Test
    fun `languages are passed through unchanged`() {
        val langs = listOf(
            LanguagePair("en", "es", 50),
            LanguagePair("en", "fr", 30),
        )
        val result = useCase(stats(languages = langs), today)
        assertEquals(langs, result.languages)
    }
}
