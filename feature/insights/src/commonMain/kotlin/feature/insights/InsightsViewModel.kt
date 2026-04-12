package feature.insights

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.UiState
import data.storage.DailyInsightCache
import domain.analytics.model.AccuracyByLevel
import domain.analytics.model.WeeklyReport
import domain.analytics.model.DailyStudyStats
import domain.analytics.model.HourlyAccuracy
import domain.analytics.model.StudyHeatmapDay
import domain.analytics.model.StudyInsights
import domain.analytics.model.WordDifficulty
import domain.analytics.usecase.GetAccuracyByLevelUseCase
import domain.analytics.usecase.GetAccuracyTrendUseCase
import domain.analytics.usecase.GetBestStudyTimeUseCase
import domain.analytics.usecase.GetDifficultWordsUseCase
import domain.analytics.usecase.GetStudyHeatmapUseCase
import domain.analytics.usecase.GetStudyInsightsUseCase
import domain.analytics.usecase.GetWeeklyReportUseCase
import domain.profile.usecase.GetProfileStatsUseCase
import domain.wordrush.model.WordRushInsights
import domain.wordrush.usecase.GetWordRushInsightsUseCase
import core.error.toUserMessage
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

data class InsightsState(
    val overview: UiState<StudyInsights> = UiState.Loading,
    val accuracyTrend: UiState<List<DailyStudyStats>> = UiState.Loading,
    val difficultWords: UiState<List<WordDifficulty>> = UiState.Loading,
    val accuracyByLevel: UiState<List<AccuracyByLevel>> = UiState.Loading,
    val heatmap: UiState<List<StudyHeatmapDay>> = UiState.Loading,
    val bestStudyTime: UiState<HourlyAccuracy?> = UiState.Loading,
    val wordRushInsights: UiState<WordRushInsights> = UiState.Loading,
    val weeklyReport: UiState<WeeklyReportUiModel> = UiState.Loading,
    val dailyInsight: String? = null,
    val currentStreak: Int? = null,
    val longestStreak: Int? = null,
) {
    val availability: InsightsAvailability get() = InsightsAvailability.from(this)

    val isLoaded: Boolean get() = overview !is UiState.Loading
            && difficultWords !is UiState.Loading
            && accuracyByLevel !is UiState.Loading
            && heatmap !is UiState.Loading
            && bestStudyTime !is UiState.Loading
            && wordRushInsights !is UiState.Loading
            && weeklyReport !is UiState.Loading

    val isError: Boolean get() = isLoaded && !availability.hasAnyContent && (
            overview is UiState.Error
            || difficultWords is UiState.Error
            || accuracyByLevel is UiState.Error
            || heatmap is UiState.Error
            || bestStudyTime is UiState.Error
            || wordRushInsights is UiState.Error
    )
}

class InsightsViewModel(
    private val getStudyInsightsUseCase: GetStudyInsightsUseCase,
    private val getDifficultWordsUseCase: GetDifficultWordsUseCase,
    private val getAccuracyTrendUseCase: GetAccuracyTrendUseCase,
    private val getAccuracyByLevelUseCase: GetAccuracyByLevelUseCase,
    private val getStudyHeatmapUseCase: GetStudyHeatmapUseCase,
    private val getBestStudyTimeUseCase: GetBestStudyTimeUseCase,
    private val getWordRushInsightsUseCase: GetWordRushInsightsUseCase,
    private val getWeeklyReportUseCase: GetWeeklyReportUseCase,
    private val getProfileStatsUseCase: GetProfileStatsUseCase,
    private val dailyInsightCache: DailyInsightCache,
) : BaseViewModel<InsightsState, Nothing>() {

    override fun initialState() = InsightsState()


    fun refresh() {
        loadAllData()
    }

    /** Called on composition entry — skips if data is already loaded to avoid redundant requests on reopen. */
    fun refreshIfNeeded() {
        if (currentState.isLoaded) return
        loadAllData()
    }

    fun dismissDailyInsight() {
        dailyInsightCache.clearDailyInsight()
        updateState { copy(dailyInsight = null) }
    }

    private fun loadAllData() {
        loadOverview()
        loadAccuracyTrend()
        loadDifficultWords()
        loadAccuracyByLevel()
        loadHeatmap()
        loadBestStudyTime()
        loadWordRushInsights()
        loadWeeklyReport()
        loadStreak()
        updateState { copy(dailyInsight = dailyInsightCache.getDailyInsight()) }
    }

    private fun loadOverview() {
        viewModelScope.launch {
            updateState { copy(overview = UiState.Loading) }
            getStudyInsightsUseCase(Unit).reduce(
                onSuccess = { copy(overview = UiState.Loaded(it)) },
                onFailure = { copy(overview = UiState.Error(it.toUserMessage())) },
            )
        }
    }

    private fun loadAccuracyTrend() {
        viewModelScope.launch {
            updateState { copy(accuracyTrend = UiState.Loading) }
            val tz = TimeZone.currentSystemDefault()
            val today = Clock.System.now().toLocalDateTime(tz).date
            val startDate = today.minus(30, DateTimeUnit.DAY)
            getAccuracyTrendUseCase(
                GetAccuracyTrendUseCase.Params(startDate.toString(), today.toString())
            ).reduce(
                onSuccess = { copy(accuracyTrend = UiState.Loaded(it)) },
                onFailure = { copy(accuracyTrend = UiState.Error(it.toUserMessage())) },
            )
        }
    }

    private fun loadDifficultWords() {
        viewModelScope.launch {
            updateState { copy(difficultWords = UiState.Loading) }
            getDifficultWordsUseCase(
                GetDifficultWordsUseCase.Params(minReviews = 3, limit = 20)
            ).reduce(
                onSuccess = { copy(difficultWords = UiState.Loaded(it)) },
                onFailure = { copy(difficultWords = UiState.Error(it.toUserMessage())) },
            )
        }
    }

    private fun loadAccuracyByLevel() {
        viewModelScope.launch {
            updateState { copy(accuracyByLevel = UiState.Loading) }
            getAccuracyByLevelUseCase(Unit).reduce(
                onSuccess = { copy(accuracyByLevel = UiState.Loaded(it)) },
                onFailure = { copy(accuracyByLevel = UiState.Error(it.toUserMessage())) },
            )
        }
    }

    private fun loadHeatmap() {
        viewModelScope.launch {
            updateState { copy(heatmap = UiState.Loading) }
            val tz = TimeZone.currentSystemDefault()
            val today = Clock.System.now().toLocalDateTime(tz).date
            val startDate = today.minus(90, DateTimeUnit.DAY)
            getStudyHeatmapUseCase(
                GetStudyHeatmapUseCase.Params(startDate.toString(), today.toString())
            ).reduce(
                onSuccess = { copy(heatmap = UiState.Loaded(it)) },
                onFailure = { copy(heatmap = UiState.Error(it.toUserMessage())) },
            )
        }
    }

    private fun loadBestStudyTime() {
        viewModelScope.launch {
            updateState { copy(bestStudyTime = UiState.Loading) }
            getBestStudyTimeUseCase(Unit).reduce(
                onSuccess = { copy(bestStudyTime = UiState.Loaded(it)) },
                onFailure = { copy(bestStudyTime = UiState.Error(it.toUserMessage())) },
            )
        }
    }

    private fun loadWordRushInsights() {
        viewModelScope.launch {
            updateState { copy(wordRushInsights = UiState.Loading) }
            getWordRushInsightsUseCase(Unit).reduce(
                onSuccess = { copy(wordRushInsights = UiState.Loaded(it)) },
                onFailure = { copy(wordRushInsights = UiState.Error(it.toUserMessage())) },
            )
        }
    }

    private fun loadWeeklyReport() {
        viewModelScope.launch {
            updateState { copy(weeklyReport = UiState.Loading) }
            getWeeklyReportUseCase(Unit).reduce(
                onSuccess = { report -> copy(weeklyReport = UiState.Loaded(report.toUiModel())) },
                onFailure = { copy(weeklyReport = UiState.Loaded(WeeklyReportUiModel.Empty)) },
            )
        }
    }

    private fun loadStreak() {
        viewModelScope.launch {
            getProfileStatsUseCase(Unit).reduce(
                onSuccess = { copy(currentStreak = it.currentStreak, longestStreak = it.longestStreak) },
                onFailure = { this },
            )
        }
    }
}

// ─── Mapper ──────────────────────────────────────────────────────────────────

private fun WeeklyReport.toUiModel(): WeeklyReportUiModel {
    if (cardsReviewed == 0 && sessionsCount == 0) return WeeklyReportUiModel.Empty
    val changeLabel = changePercent?.let { pct ->
        val rounded = abs(pct).roundToInt()
        if (pct >= 0) "+$rounded%" else "-$rounded%"
    }
    return WeeklyReportUiModel.Content(
        weekRangeLabel = buildWeekRangeLabel(weekStartDate, weekEndDate),
        cardsReviewed = cardsReviewed.toString(),
        changeLabel = changeLabel,
        isChangePositive = (changePercent ?: 0.0) >= 0.0,
        accuracyValue = "${accuracyPercent.roundToInt()}%",
        masteredValue = wordsMastered.toString(),
        studyTimeValue = formatStudyTime(totalStudyTimeMs),
        sessionsValue = sessionsCount.toString(),
        bestDayLabel = bestDay?.let { "${it.dayName} (${it.cardsReviewed})" },
    )
}

private fun buildWeekRangeLabel(startDate: String, endDate: String): String {
    val start = runCatching { LocalDate.parse(startDate) }.getOrNull()
    val end = runCatching { LocalDate.parse(endDate) }.getOrNull()
    if (start == null || end == null) return "$startDate – $endDate"
    return "${start.toShortLabel()} – ${end.toShortLabel()}"
}

private fun LocalDate.toShortLabel(): String {
    val month = when (monthNumber) {
        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
        5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
        9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"
        else -> "Dec"
    }
    return "$month $dayOfMonth"
}

private fun formatStudyTime(ms: Long): String {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}
