package domain.common.util

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class EpochDateFormatterTest {

    // 2025-01-15 12:00:00 UTC
    private val jan15 = 1736942400000L

    // 2025-12-31 00:00:00 UTC
    private val dec31 = 1767139200000L

    @Test
    fun `formats january date correctly`() {
        val result = EpochDateFormatter.toMediumDate(jan15, TimeZone.UTC)
        assertEquals("Jan 15, 2025", result)
    }

    @Test
    fun `formats december date correctly`() {
        val result = EpochDateFormatter.toMediumDate(dec31, TimeZone.UTC)
        assertEquals("Dec 31, 2025", result)
    }

    @Test
    fun `formats each month name correctly`() {
        // 2024 was a leap year; use timestamps for 1st of each month at noon UTC
        val months = listOf(
            1704110400000L to "Jan 1, 2024",
            1706788800000L to "Feb 1, 2024",
            1709467200000L to "Mar 3, 2024",
            1712059200000L to "Apr 2, 2024",
        )
        months.forEach { (millis, expected) ->
            assertEquals(expected, EpochDateFormatter.toMediumDate(millis, TimeZone.UTC), "Failed for expected=$expected")
        }
    }
}
