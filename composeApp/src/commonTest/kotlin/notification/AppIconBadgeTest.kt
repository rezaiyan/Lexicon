package notification

import kotlin.test.Test
import kotlin.test.assertEquals

class AppIconBadgeTest {

    @Test
    fun `cap when count is within limit returns count`() {
        assertEquals(0, AppIconBadge.cap(0))
        assertEquals(42, AppIconBadge.cap(42))
        assertEquals(99, AppIconBadge.cap(99))
    }

    @Test
    fun `cap when count exceeds limit returns 99`() {
        assertEquals(99, AppIconBadge.cap(100))
        assertEquals(99, AppIconBadge.cap(5_000))
    }

    @Test
    fun `cap when count is negative returns 0`() {
        assertEquals(0, AppIconBadge.cap(-3))
    }
}
