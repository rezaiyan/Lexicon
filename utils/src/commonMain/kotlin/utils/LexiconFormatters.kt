package utils

import kotlin.math.roundToInt

object LexiconFormatters {

    fun oneDecimal(value: Double): String {
        val rounded = kotlin.math.round(value * 10) / 10.0
        val whole = rounded.toLong()
        val decimal = kotlin.math.round((rounded - whole) * 10).toInt()
        return "$whole.$decimal"
    }

    fun oneDecimal(value: Float): String = oneDecimal(value.toDouble())

    fun fileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> "${oneDecimal(gb)} GB"
            mb >= 1.0 -> "${oneDecimal(mb)} MB"
            kb >= 1.0 -> "${oneDecimal(kb)} KB"
            else -> "$bytes B"
        }
    }

    fun fileSizeApprox(bytes: Int): String {
        return if (bytes >= 1024 * 1024) {
            val rounded = kotlin.math.round(bytes / (1024.0 * 1024.0) * 10).toInt()
            "~${rounded / 10}.${rounded % 10} MB"
        } else {
            "~${bytes / 1024} KB"
        }
    }

    fun duration(ms: Long, showSeconds: Boolean = false): String {
        val totalMinutes = ms / 60_000
        return when {
            totalMinutes >= 60 -> {
                val h = totalMinutes / 60
                val m = totalMinutes % 60
                if (m > 0) "${h}h ${m}m" else "${h}h"
            }
            totalMinutes >= 1 -> "${totalMinutes}m"
            showSeconds -> "${ms / 1_000}s"
            else -> "<1m"
        }
    }

    fun secondsOneDecimal(ms: Long): String {
        val tenths = ms / 100
        return "${tenths / 10}.${tenths % 10}s"
    }

    fun secondsOneDecimal(ms: Double): String = secondsOneDecimal(ms.toLong())

    fun percent(value: Double): String = "${value.roundToInt()}%"

    fun percentChange(value: Double?): String? {
        if (value == null) return null
        return "${if (value >= 0) "+" else ""}${value.roundToInt()}%"
    }

    fun speed(value: Float): String = oneDecimal(value)
}
