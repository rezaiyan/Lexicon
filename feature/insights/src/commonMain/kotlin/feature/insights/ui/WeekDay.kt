package feature.insights.ui

/** One day of the rolling last-7-days window, oldest first; the last entry is today. */
internal data class WeekDay(
    val label: String,
    val count: Int,
    val isToday: Boolean,
)
