package feature.insights

sealed interface WeeklyReportUiModel {
    data object Empty : WeeklyReportUiModel

    data class Content(
        val weekRangeLabel: String,
        val cardsReviewed: String,
        val changeLabel: String?,
        val isChangePositive: Boolean,
        val accuracyValue: String,
        val masteredValue: String,
        val studyTimeValue: String,
        val sessionsValue: String,
        val bestDayLabel: String?,
    ) : WeeklyReportUiModel
}
