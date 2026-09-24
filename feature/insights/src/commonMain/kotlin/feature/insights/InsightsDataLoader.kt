package feature.insights

import core.base.BaseViewModel
import core.common.UiState
import core.common.fold
import core.error.toUserMessage
import domain.analytics.model.DailyStudyStats
import domain.analytics.model.DayOfWeekAccuracy
import domain.analytics.model.WeeklyReport
import domain.analytics.usecase.GetAccuracyTrendUseCase
import domain.analytics.usecase.GetDifficultWordsUseCase
import domain.analytics.usecase.GetStudyHeatmapUseCase
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import utils.LexiconFormatters

/** Fetches every analytics section shown on the Insights screen and folds each result into state. */
class InsightsDataLoader(
    private val useCases: InsightsUseCases,
    private val stateAccess: BaseViewModel.StateAccess<InsightsState>,
    private val scope: CoroutineScope,
) {

    fun loadAll() {
        loadOverview()
        loadAccuracyTrend()
        loadDifficultWords()
        loadAccuracyByLevel()
        loadHeatmap()
        loadBestStudyTime()
        loadWordRushInsights()
        loadWeeklyReport()
        loadLevelTransitions()
        loadResponseTimeTrend()
        loadStreak()
    }

    private fun loadOverview() {
        scope.launch {
            stateAccess.update { copy(overview = UiState.Loading) }
            useCases.getStudyInsights(Unit).fold(
                onSuccess = { stateAccess.update { copy(overview = UiState.Loaded(it)) } },
                onFailure = { stateAccess.update { copy(overview = UiState.Error(it.toUserMessage())) } },
            )
        }
    }

    private fun loadAccuracyTrend() {
        scope.launch {
            stateAccess.update { copy(accuracyTrend = UiState.Loading) }
            val tz = TimeZone.currentSystemDefault()
            val today = Clock.System.now().toLocalDateTime(tz).date
            val startDate = today.minus(30, DateTimeUnit.DAY)
            useCases.getAccuracyTrend(
                GetAccuracyTrendUseCase.Params(startDate.toString(), today.toString())
            ).fold(
                onSuccess = { trend ->
                    stateAccess.update {
                        copy(
                            accuracyTrend = UiState.Loaded(trend),
                            accuracyByDayOfWeek = computeDayOfWeekAccuracy(trend),
                        )
                    }
                },
                onFailure = { stateAccess.update { copy(accuracyTrend = UiState.Error(it.toUserMessage())) } },
            )
        }
    }

    private fun loadDifficultWords() {
        scope.launch {
            stateAccess.update { copy(difficultWords = UiState.Loading) }
            useCases.getDifficultWords(
                GetDifficultWordsUseCase.Params(minReviews = 3, limit = 20)
            ).fold(
                onSuccess = { stateAccess.update { copy(difficultWords = UiState.Loaded(it)) } },
                onFailure = { stateAccess.update { copy(difficultWords = UiState.Error(it.toUserMessage())) } },
            )
        }
    }

    private fun loadAccuracyByLevel() {
        scope.launch {
            stateAccess.update { copy(accuracyByLevel = UiState.Loading) }
            useCases.getAccuracyByLevel(Unit).fold(
                onSuccess = { stateAccess.update { copy(accuracyByLevel = UiState.Loaded(it)) } },
                onFailure = { stateAccess.update { copy(accuracyByLevel = UiState.Error(it.toUserMessage())) } },
            )
        }
    }

    private fun loadHeatmap() {
        scope.launch {
            stateAccess.update { copy(heatmap = UiState.Loading) }
            val tz = TimeZone.currentSystemDefault()
            val today = Clock.System.now().toLocalDateTime(tz).date
            val startDate = today.minus(90, DateTimeUnit.DAY)
            useCases.getStudyHeatmap(
                GetStudyHeatmapUseCase.Params(startDate.toString(), today.toString())
            ).fold(
                onSuccess = { stateAccess.update { copy(heatmap = UiState.Loaded(it)) } },
                onFailure = { stateAccess.update { copy(heatmap = UiState.Error(it.toUserMessage())) } },
            )
        }
    }

    private fun loadBestStudyTime() {
        scope.launch {
            stateAccess.update { copy(bestStudyTime = UiState.Loading) }
            useCases.getBestStudyTime(Unit).fold(
                onSuccess = { stateAccess.update { copy(bestStudyTime = UiState.Loaded(it)) } },
                onFailure = { stateAccess.update { copy(bestStudyTime = UiState.Error(it.toUserMessage())) } },
            )
        }
    }

    private fun loadWordRushInsights() {
        scope.launch {
            stateAccess.update { copy(wordRushInsights = UiState.Loading) }
            useCases.getWordRushInsights(Unit).fold(
                onSuccess = { stateAccess.update { copy(wordRushInsights = UiState.Loaded(it)) } },
                onFailure = { stateAccess.update { copy(wordRushInsights = UiState.Error(it.toUserMessage())) } },
            )
        }
    }

    private fun loadWeeklyReport() {
        scope.launch {
            stateAccess.update { copy(weeklyReport = UiState.Loading) }
            useCases.getWeeklyReport(Unit).fold(
                onSuccess = { report ->
                    stateAccess.update { copy(weeklyReport = UiState.Loaded(report.toUiModel())) }
                },
                onFailure = {
                    stateAccess.update { copy(weeklyReport = UiState.Loaded(WeeklyReportUiModel.Empty)) }
                },
            )
        }
    }

    private fun loadLevelTransitions() {
        scope.launch {
            stateAccess.update { copy(levelTransitions = UiState.Loading) }
            useCases.getLevelTransitions(Unit).fold(
                onSuccess = { stateAccess.update { copy(levelTransitions = UiState.Loaded(it)) } },
                onFailure = { stateAccess.update { copy(levelTransitions = UiState.Error(it.toUserMessage())) } },
            )
        }
    }

    private fun loadResponseTimeTrend() {
        scope.launch {
            stateAccess.update { copy(responseTimeTrend = UiState.Loading) }
            useCases.getResponseTimeTrend(Unit).fold(
                onSuccess = { stateAccess.update { copy(responseTimeTrend = UiState.Loaded(it)) } },
                onFailure = { stateAccess.update { copy(responseTimeTrend = UiState.Error(it.toUserMessage())) } },
            )
        }
    }

    private fun loadStreak() {
        scope.launch {
            useCases.getProfileStats(Unit).fold(
                onSuccess = { stats ->
                    stateAccess.update {
                        copy(currentStreak = stats.currentStreak, longestStreak = stats.longestStreak)
                    }
                },
                onFailure = { /* keep previous streak values */ },
            )
        }
    }
}

// ─── Day-of-week aggregation ─────────────────────────────────────────────────

private fun computeDayOfWeekAccuracy(stats: List<DailyStudyStats>): List<DayOfWeekAccuracy> {
    return (1..7).map { dow ->
        val dayStats = stats.filter { stat ->
            runCatching { LocalDate.parse(stat.date).dayOfWeek.ordinal + 1 }
                .getOrElse { -1 } == dow
        }
        val totalReviews = dayStats.sumOf { (it.correctCount + it.incorrectCount).toLong() }
        val correctCount = dayStats.sumOf { it.correctCount.toLong() }
        val accuracyPercent = if (totalReviews == 0L) 0.0
        else (correctCount.toDouble() / totalReviews) * 100.0
        DayOfWeekAccuracy(
            dayOfWeek = dow,
            totalReviews = totalReviews,
            correctCount = correctCount,
            accuracyPercent = accuracyPercent,
        )
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
        studyTimeValue = LexiconFormatters.duration(totalStudyTimeMs),
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
