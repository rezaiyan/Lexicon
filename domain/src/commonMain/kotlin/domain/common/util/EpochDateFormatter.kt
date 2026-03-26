package domain.common.util

import kotlinx.datetime.Instant
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
}
