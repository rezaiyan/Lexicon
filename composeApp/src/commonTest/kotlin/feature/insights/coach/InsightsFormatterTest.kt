package feature.insights.coach

import kotlin.test.Test
import kotlin.test.assertEquals

class InsightsFormatterTest {

    @Test
    fun `compact counts`() {
        assertEquals("999", InsightsFormatter.compactCount(999))
        assertEquals("1k", InsightsFormatter.compactCount(1000))
        assertEquals("1.2k", InsightsFormatter.compactCount(1249))
        assertEquals("12.5k", InsightsFormatter.compactCount(12_500))
    }

    @Test
    fun `hours follow the clock preference`() {
        assertEquals("20:00", InsightsFormatter.hour(20, use24Hour = true))
        assertEquals("08:00", InsightsFormatter.hour(8, use24Hour = true))
        assertEquals("8 PM", InsightsFormatter.hour(20, use24Hour = false))
        assertEquals("12 AM", InsightsFormatter.hour(0, use24Hour = false))
        assertEquals("12 PM", InsightsFormatter.hour(12, use24Hour = false))
    }

    @Test
    fun `updated ago buckets`() {
        assertEquals(UpdatedAgo.JustNow, InsightsFormatter.updatedAgo(fetchedAtMs = 0, nowMs = 59_000))
        assertEquals(UpdatedAgo.Minutes(5), InsightsFormatter.updatedAgo(0, 5 * 60_000))
        assertEquals(UpdatedAgo.Hours(2), InsightsFormatter.updatedAgo(0, 2 * 3_600_000 + 10))
        assertEquals(UpdatedAgo.Days(3), InsightsFormatter.updatedAgo(0, 3 * 86_400_000L))
    }
}
