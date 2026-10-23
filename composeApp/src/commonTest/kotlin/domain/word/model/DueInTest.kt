package domain.word.model

import kotlin.test.Test
import kotlin.test.assertEquals

class DueInTest {

    private val now = 1_800_000_000_000L
    private val hour = 3_600_000L
    private val day = 24 * hour

    @Test
    fun `of when review time has passed is Now`() {
        assertEquals(DueIn.Now, DueIn.of(nextReviewMs = now - day, nowMs = now))
        assertEquals(DueIn.Now, DueIn.of(nextReviewMs = now, nowMs = now))
    }

    @Test
    fun `of within a day rounds up to whole hours`() {
        assertEquals(DueIn.Hours(1), DueIn.of(nextReviewMs = now + 10 * 60_000L, nowMs = now))
        assertEquals(DueIn.Hours(5), DueIn.of(nextReviewMs = now + 4 * hour + 1, nowMs = now))
        assertEquals(DueIn.Hours(23), DueIn.of(nextReviewMs = now + 23 * hour, nowMs = now))
    }

    @Test
    fun `of from a day on rounds to the nearest whole day`() {
        assertEquals(DueIn.Days(1), DueIn.of(nextReviewMs = now + 24 * hour, nowMs = now))
        assertEquals(DueIn.Days(1), DueIn.of(nextReviewMs = now + 35 * hour, nowMs = now))
        assertEquals(DueIn.Days(2), DueIn.of(nextReviewMs = now + 36 * hour, nowMs = now))
        assertEquals(DueIn.Days(30), DueIn.of(nextReviewMs = now + 30 * day, nowMs = now))
    }
}
