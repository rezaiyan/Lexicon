package time

import java.text.DateFormat
import java.text.SimpleDateFormat

actual fun is24HourClock(): Boolean =
    (DateFormat.getTimeInstance(DateFormat.SHORT) as? SimpleDateFormat)?.toPattern()?.contains('H') ?: true
