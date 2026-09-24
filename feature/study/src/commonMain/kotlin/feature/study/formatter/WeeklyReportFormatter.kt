package feature.study.formatter

import domain.analytics.model.WeeklyReport
import feature.study.model.BestDayUiModel
import feature.study.model.WeeklyReportUiModel
import kotlinx.datetime.LocalDate
import utils.LexiconFormatters

object WeeklyReportFormatter {

    fun format(report: WeeklyReport): WeeklyReportUiModel.Content = WeeklyReportUiModel.Content(
        weekRangeLabel = formatWeekRange(report.weekStartDate, report.weekEndDate),
        cardsReviewed = report.cardsReviewed,
        changeLabel = LexiconFormatters.percentChange(report.changePercent),
        isChangePositive = (report.changePercent ?: 0.0) >= 0,
        accuracyValue = LexiconFormatters.percent(report.accuracyPercent),
        masteredValue = "${report.wordsMastered}",
        studyTimeValue = LexiconFormatters.duration(report.totalStudyTimeMs, showSeconds = true),
        sessionsValue = "${report.sessionsCount}",
        bestDay = report.bestDay?.let { day ->
            BestDayUiModel(
                dayName = day.dayName,
                subtitle = "${day.cardsReviewed} cards \u2014 ${LexiconFormatters.percent(day.accuracyPercent)} accuracy",
            )
        },
        showInsightsCta = report.cardsReviewed > 0,
    )

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
}
