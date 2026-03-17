package feature.study.formatter

import domain.analytics.model.WeeklyReport
import feature.study.model.BestDayUiModel
import feature.study.model.WeeklyReportUiModel
import kotlin.math.roundToInt

object WeeklyReportFormatter {

    fun format(report: WeeklyReport): WeeklyReportUiModel.Content = WeeklyReportUiModel.Content(
        weekRangeLabel = formatWeekRange(report.weekStartDate, report.weekEndDate),
        cardsReviewed = report.cardsReviewed,
        changeLabel = formatChangePercent(report.changePercent),
        isChangePositive = (report.changePercent ?: 0.0) >= 0,
        accuracyValue = "${report.accuracyPercent.roundToInt()}%",
        masteredValue = "${report.wordsMastered}",
        studyTimeValue = formatStudyTime(report.totalStudyTimeMs),
        sessionsValue = "${report.sessionsCount}",
        bestDay = report.bestDay?.let { day ->
            BestDayUiModel(
                dayName = day.dayName,
                subtitle = "${day.cardsReviewed} cards \u2014 ${day.accuracyPercent.roundToInt()}% accuracy",
            )
        },
        showInsightsCta = report.cardsReviewed > 0,
    )

    private fun formatChangePercent(changePercent: Double?): String? {
        if (changePercent == null) return null
        val sign = if (changePercent >= 0) "+" else ""
        return "$sign${changePercent.roundToInt()}%"
    }

    private fun formatWeekRange(startDate: String, endDate: String): String {
        if (startDate.isEmpty() || endDate.isEmpty()) return ""
        val startParts = startDate.split("-")
        val endParts = endDate.split("-")
        if (startParts.size != 3 || endParts.size != 3) return "$startDate \u2013 $endDate"

        val months = listOf(
            "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )
        val startMonth = startParts[1].toIntOrNull()?.let { months.getOrNull(it) } ?: startParts[1]
        val endMonth = endParts[1].toIntOrNull()?.let { months.getOrNull(it) } ?: endParts[1]
        val startDay = startParts[2].toIntOrNull()?.toString() ?: startParts[2]
        val endDay = endParts[2].toIntOrNull()?.toString() ?: endParts[2]

        return if (startMonth == endMonth) {
            "$startMonth $startDay\u2013$endDay"
        } else {
            "$startMonth $startDay \u2013 $endMonth $endDay"
        }
    }

    private fun formatStudyTime(totalMs: Long): String {
        val totalMinutes = totalMs / 60_000
        return when {
            totalMinutes < 1 -> "${totalMs / 1_000}s"
            totalMinutes < 60 -> "${totalMinutes}m"
            else -> {
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
            }
        }
    }
}
