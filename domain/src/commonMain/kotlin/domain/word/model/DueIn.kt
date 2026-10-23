package domain.word.model

/** How long until a word comes up for review, rounded for a compact list label. */
sealed interface DueIn {
    data object Now : DueIn

    /** Under a day away, rounded up so a word never reads as due before it is. */
    data class Hours(val hours: Int) : DueIn

    /** A day or more away, rounded to the nearest day. */
    data class Days(val days: Int) : DueIn

    companion object {
        private const val HOUR_MS = 3_600_000L
        private const val DAY_MS = 24 * HOUR_MS

        fun of(nextReviewMs: Long, nowMs: Long): DueIn {
            val remaining = nextReviewMs - nowMs
            if (remaining <= 0) return Now
            val hours = (remaining + HOUR_MS - 1) / HOUR_MS
            return if (hours < 24) Hours(hours.toInt()) else Days(((remaining + DAY_MS / 2) / DAY_MS).toInt())
        }
    }
}
