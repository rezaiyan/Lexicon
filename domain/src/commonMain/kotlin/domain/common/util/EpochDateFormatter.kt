package domain.common.util

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object EpochDateFormatter {

    private val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    fun toMediumDate(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val localDateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
        val monthName = monthNames[localDateTime.month.ordinal]
        return "$monthName ${localDateTime.dayOfMonth}, ${localDateTime.year}"
    }

    fun toShortDate(
        epochMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        fallback: String = "—",
    ): String {
        if (epochMillis <= 0L) return fallback
        val localDateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
        val monthName = monthNames[localDateTime.month.ordinal]
        return "${localDateTime.dayOfMonth} $monthName ${localDateTime.year}"
    }
}
