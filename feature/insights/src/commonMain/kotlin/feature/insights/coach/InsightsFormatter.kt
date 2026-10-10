package feature.insights.coach

sealed interface UpdatedAgo {
    data object JustNow : UpdatedAgo
    data class Minutes(val value: Int) : UpdatedAgo
    data class Hours(val value: Int) : UpdatedAgo
    data class Days(val value: Int) : UpdatedAgo
}

/** Pure, locale-light formatting for insights numbers (spec §6). Words come from string resources. */
object InsightsFormatter {

    private const val THOUSAND = 1000
    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 60 * MINUTE_MS
    private const val DAY_MS = 24 * HOUR_MS
    private const val HOURS_PER_HALF_DAY = 12

    fun compactCount(value: Long): String {
        if (value < THOUSAND) return value.toString()
        val tenths = value * 10 / THOUSAND
        val whole = tenths / 10
        val decimal = tenths % 10
        return if (decimal == 0L) "${whole}k" else "$whole.${decimal}k"
    }

    fun compactCount(value: Int): String = compactCount(value.toLong())

    fun hour(hour: Int, use24Hour: Boolean): String {
        if (use24Hour) return "${hour.toString().padStart(2, '0')}:00"
        val display = (hour % HOURS_PER_HALF_DAY).let { if (it == 0) HOURS_PER_HALF_DAY else it }
        return "$display ${if (hour < HOURS_PER_HALF_DAY) "AM" else "PM"}"
    }

    fun updatedAgo(fetchedAtMs: Long, nowMs: Long): UpdatedAgo {
        val elapsed = (nowMs - fetchedAtMs).coerceAtLeast(0)
        return when {
            elapsed < MINUTE_MS -> UpdatedAgo.JustNow
            elapsed < HOUR_MS -> UpdatedAgo.Minutes((elapsed / MINUTE_MS).toInt())
            elapsed < DAY_MS -> UpdatedAgo.Hours((elapsed / HOUR_MS).toInt())
            else -> UpdatedAgo.Days((elapsed / DAY_MS).toInt())
        }
    }
}
