package feature.support

import domain.support.model.SupportDeliveryStatus
import domain.support.model.SupportMessage
import domain.support.model.SupportSender
import feature.support.model.SupportDay
import feature.support.model.SupportTimelineItem
import feature.support.model.buildSupportTimeline
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SupportTimelineTest {

    private val zone = TimeZone.UTC
    private val today = LocalDate(2026, 10, 11)

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime(year, month, day, hour, minute).toInstant(zone).toEpochMilliseconds()

    private fun message(id: String, sender: SupportSender, createdAt: Long) =
        SupportMessage(id, sender, "body $id", createdAt, SupportDeliveryStatus.SENT)

    private fun build(vararg messages: SupportMessage) =
        buildSupportTimeline(messages.toList(), today = today, timeZone = zone)

    @Test
    fun `buildSupportTimeline with no messages returns empty list`() {
        assertEquals(emptyList(), build())
    }

    @Test
    fun `buildSupportTimeline inserts a day header before each new day`() {
        val items = build(
            message("a", SupportSender.USER, at(2026, 3, 2, 9, 0)),
            message("b", SupportSender.ADMIN, at(2026, 10, 10, 9, 0)),
            message("c", SupportSender.USER, at(2026, 10, 11, 9, 0)),
        )

        val headers = items.filterIsInstance<SupportTimelineItem.DayHeader>().map { it.day }
        assertEquals(
            listOf(SupportDay.Date("2 Mar"), SupportDay.Yesterday, SupportDay.Today),
            headers,
        )
        assertEquals(6, items.size)
        assertIs<SupportTimelineItem.DayHeader>(items[0])
    }

    @Test
    fun `buildSupportTimeline shows the year for days outside the current year`() {
        val items = build(message("a", SupportSender.USER, at(2025, 12, 31, 23, 0)))

        assertEquals(SupportDay.Date("31 Dec 2025"), (items[0] as SupportTimelineItem.DayHeader).day)
    }

    @Test
    fun `buildSupportTimeline groups close messages from the same sender`() {
        val items = build(
            message("a", SupportSender.USER, at(2026, 10, 11, 9, 0)),
            message("b", SupportSender.USER, at(2026, 10, 11, 9, 2)),
            message("c", SupportSender.USER, at(2026, 10, 11, 9, 4)),
            message("d", SupportSender.ADMIN, at(2026, 10, 11, 9, 5)),
        ).filterIsInstance<SupportTimelineItem.Bubble>()

        assertEquals(listOf(true, false, false, true), items.map { it.isFirstInGroup })
        assertEquals(listOf(false, false, true, true), items.map { it.isLastInGroup })
    }

    @Test
    fun `buildSupportTimeline splits a group after a long pause`() {
        val items = build(
            message("a", SupportSender.ADMIN, at(2026, 10, 11, 9, 0)),
            message("b", SupportSender.ADMIN, at(2026, 10, 11, 9, 30)),
        ).filterIsInstance<SupportTimelineItem.Bubble>()

        assertEquals(listOf(true, true), items.map { it.isFirstInGroup })
        assertEquals(listOf(true, true), items.map { it.isLastInGroup })
    }

    @Test
    fun `buildSupportTimeline formats the time as zero padded 24h clock`() {
        val items = build(message("a", SupportSender.USER, at(2026, 10, 11, 7, 5)))

        assertEquals("07:05", (items[1] as SupportTimelineItem.Bubble).time)
    }
}
