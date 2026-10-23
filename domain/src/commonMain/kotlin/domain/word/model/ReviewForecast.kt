package domain.word.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Reviews coming due over the next days.
 *
 * @property dueNow words already due, overdue included
 * @property days today and the following days in order; today counts everything due by the end of it,
 *   so it includes [dueNow]
 */
data class ReviewForecast(val dueNow: Int, val days: List<ForecastDay>) {

    companion object {
        const val DAYS = 7

        fun of(dueAtMillis: List<Long>, now: Instant, zone: TimeZone): ReviewForecast {
            val nowMs = now.toEpochMilliseconds()
            val today = now.toLocalDateTime(zone).date
            val counts = IntArray(DAYS)
            var dueNow = 0
            dueAtMillis.forEach { dueAt ->
                if (dueAt <= nowMs) {
                    dueNow++
                    counts[0]++
                } else {
                    val offset = today.daysUntil(Instant.fromEpochMilliseconds(dueAt).toLocalDateTime(zone).date)
                    if (offset < DAYS) counts[offset]++
                }
            }
            return ReviewForecast(
                dueNow = dueNow,
                days = counts.mapIndexed { offset, reviews ->
                    ForecastDay(today.plus(DatePeriod(days = offset)), reviews)
                },
            )
        }

        private fun LocalDate.daysUntil(other: LocalDate): Int = (other.toEpochDays() - toEpochDays()).toInt()
    }
}

data class ForecastDay(val date: LocalDate, val reviews: Int)
