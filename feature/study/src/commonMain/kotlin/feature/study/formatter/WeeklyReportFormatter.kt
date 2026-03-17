package feature.study.formatter

import domain.analytics.model.WeeklyReport
import feature.study.model.BestDayUiModel
import feature.study.model.WeeklyReportUiModel
import kotlinx.datetime.LocalDate
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
        val start = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: return "$startDate \u2013 $endDate"
        val end = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: return "$startDate \u2013 $endDate"

        val startMonth = start.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        val endMonth = end.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

        return if (startMonth == endMonth) {
            "$startMonth ${start.dayOfMonth}\u2013${end.dayOfMonth}"
        } else {
            "$startMonth ${start.dayOfMonth} \u2013 $endMonth ${end.dayOfMonth}"
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
