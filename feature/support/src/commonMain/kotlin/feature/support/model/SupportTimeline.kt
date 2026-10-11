package feature.support.model

import androidx.compose.runtime.Immutable
import domain.support.model.SupportMessage
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Label of a day separator; [Today] and [Yesterday] are localized by the UI. */
@Immutable
sealed interface SupportDay {
    data object Today : SupportDay
    data object Yesterday : SupportDay
    data class Date(val text: String) : SupportDay
}

/** One row of the chat list, in chronological order. */
@Immutable
sealed interface SupportTimelineItem {
    val key: String

    data class DayHeader(val date: LocalDate, val day: SupportDay) : SupportTimelineItem {
        override val key: String get() = "day-$date"
    }

    /**
     * @param time Local wall-clock time, e.g. "09:05".
     * @param isFirstInGroup Starts a run of messages from the same sender sent close together.
     * @param isLastInGroup Ends that run; the bubble gets its tail and the admin avatar.
     */
    data class Bubble(
        val message: SupportMessage,
        val time: String,
        val isFirstInGroup: Boolean,
        val isLastInGroup: Boolean,
    ) : SupportTimelineItem {
        override val key: String get() = message.id
    }
}

/** Messages further apart than this start a new bubble group. */
private const val GROUP_GAP_MS = 5 * 60 * 1000L

private val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/**
 * Turns the conversation (oldest first) into list rows: a [SupportTimelineItem.DayHeader] before
 * each new day and bubbles flagged with their position inside a same-sender group.
 */
fun buildSupportTimeline(
    messages: List<SupportMessage>,
    today: LocalDate,
    timeZone: TimeZone,
): List<SupportTimelineItem> {
    val dated = messages.map { it to Instant.fromEpochMilliseconds(it.createdAtMillis).toLocalDateTime(timeZone) }
    val yesterday = today.minus(1, DateTimeUnit.DAY)

    fun joins(index: Int, other: Int): Boolean {
        val (a, aAt) = dated.getOrNull(index) ?: return false
        val (b, bAt) = dated.getOrNull(other) ?: return false
        return a.sender == b.sender &&
            aAt.date == bAt.date &&
            kotlin.math.abs(a.createdAtMillis - b.createdAtMillis) <= GROUP_GAP_MS
    }

    return buildList {
        dated.forEachIndexed { index, (message, at) ->
            if (index == 0 || dated[index - 1].second.date != at.date) {
                val day = when (at.date) {
                    today -> SupportDay.Today
                    yesterday -> SupportDay.Yesterday
                    else -> {
                        val month = monthNames[at.date.month.ordinal]
                        val year = if (at.date.year == today.year) "" else " ${at.date.year}"
                        SupportDay.Date("${at.date.day} $month$year")
                    }
                }
                add(SupportTimelineItem.DayHeader(at.date, day))
            }
            add(
                SupportTimelineItem.Bubble(
                    message = message,
                    time = "${at.hour.toString().padStart(2, '0')}:${at.minute.toString().padStart(2, '0')}",
                    isFirstInGroup = !joins(index, index - 1),
                    isLastInGroup = !joins(index, index + 1),
                )
            )
        }
    }
}
