package notification

/**
 * App icon badge count rules shared across platforms.
 *
 * Native badges (e.g. iOS `UNNotificationContent.badge`) only accept an integer,
 * so large counts are capped at [MAX_COUNT] instead of showing huge numbers.
 */
object AppIconBadge {
    const val MAX_COUNT = 99

    fun cap(count: Int): Int = count.coerceIn(0, MAX_COUNT)
}
