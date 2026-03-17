package feature.study.model

sealed interface WeeklyReportUiModel {

    data object Empty : WeeklyReportUiModel

    data class Content(
        val weekRangeLabel: String,
        val cardsReviewed: Int,
        val changeLabel: String?,
        val isChangePositive: Boolean,
        val accuracyValue: String,
        val masteredValue: String,
        val studyTimeValue: String,
        val sessionsValue: String,
        val bestDay: BestDayUiModel?,
        val showInsightsCta: Boolean,
    ) : WeeklyReportUiModel
}

data class BestDayUiModel(
    val dayName: String,
    val subtitle: String,
)
