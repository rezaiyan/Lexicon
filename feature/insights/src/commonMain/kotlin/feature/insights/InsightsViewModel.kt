package feature.insights

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.UiState
import data.storage.DailyInsightCache
import domain.analytics.model.AccuracyByLevel
import domain.analytics.model.DayOfWeekAccuracy
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
import domain.analytics.model.LevelTransition
import domain.analytics.model.ResponseTimeTrend
import domain.analytics.usecase.GetLevelTransitionsUseCase
import domain.analytics.usecase.GetResponseTimeTrendUseCase
import domain.analytics.usecase.GetWeeklyReportUseCase
import domain.profile.usecase.GetProfileStatsUseCase
import domain.settings.usecase.ObserveReviewRemindersEnabledUseCase
import domain.settings.usecase.SetReviewRemindersEnabledUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
import utils.LexiconFormatters

data class InsightsState(
    val overview: UiState<StudyInsights> = UiState.Loading,
    val accuracyTrend: UiState<List<DailyStudyStats>> = UiState.Loading,
    val accuracyByDayOfWeek: List<DayOfWeekAccuracy> = emptyList(),
    val difficultWords: UiState<List<WordDifficulty>> = UiState.Loading,
    val accuracyByLevel: UiState<List<AccuracyByLevel>> = UiState.Loading,
    val heatmap: UiState<List<StudyHeatmapDay>> = UiState.Loading,
    val bestStudyTime: UiState<HourlyAccuracy?> = UiState.Loading,
    val wordRushInsights: UiState<WordRushInsights> = UiState.Loading,
    val weeklyReport: UiState<WeeklyReportUiModel> = UiState.Loading,
    val levelTransitions: UiState<List<LevelTransition>> = UiState.Loading,
    val responseTimeTrend: UiState<List<ResponseTimeTrend>> = UiState.Loading,
    val dailyInsight: String? = null,
    val currentStreak: Int? = null,
    val longestStreak: Int? = null,
    val reviewRemindersEnabled: Boolean = false,
) {
    val availability: InsightsAvailability get() = InsightsAvailability.from(this)

    val isLoaded: Boolean get() = overview !is UiState.Loading
            && difficultWords !is UiState.Loading
            && accuracyByLevel !is UiState.Loading
            && heatmap !is UiState.Loading
            && bestStudyTime !is UiState.Loading
            && wordRushInsights !is UiState.Loading
            && weeklyReport !is UiState.Loading
            && levelTransitions !is UiState.Loading
            && responseTimeTrend !is UiState.Loading

    val isError: Boolean get() = isLoaded && !availability.hasAnyContent && (
            overview is UiState.Error
            || difficultWords is UiState.Error
            || accuracyByLevel is UiState.Error
            || heatmap is UiState.Error
            || bestStudyTime is UiState.Error
            || wordRushInsights is UiState.Error
    )
}

sealed class InsightsEffect {
    data class NavigateToReviewWithWords(val wordIds: List<Long>) : InsightsEffect()
    data object NavigateToNotificationSettings : InsightsEffect()
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
    private val getLevelTransitionsUseCase: GetLevelTransitionsUseCase,
    private val getResponseTimeTrendUseCase: GetResponseTimeTrendUseCase,
    private val getProfileStatsUseCase: GetProfileStatsUseCase,
    private val dailyInsightCache: DailyInsightCache,
    private val setReviewRemindersEnabledUseCase: SetReviewRemindersEnabledUseCase,
    private val observeReviewRemindersEnabledUseCase: ObserveReviewRemindersEnabledUseCase,
) : BaseViewModel<InsightsState, InsightsEffect>() {

    override fun initialState() = InsightsState()

    init {
        observeReviewRemindersEnabledUseCase(Unit)
            .onEach { enabled -> updateState { copy(reviewRemindersEnabled = enabled) } }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        loadAllData()
    }

    fun dismissDailyInsight() {
        dailyInsightCache.clearDailyInsight()
        updateState { copy(dailyInsight = null) }
    }

    fun studyDifficultWords() {
        val loaded = currentState.difficultWords as? UiState.Loaded ?: return
        val wordIds = loaded.value.map { it.wordId }
        if (wordIds.isEmpty()) return
        emitEffect(InsightsEffect.NavigateToReviewWithWords(wordIds))
    }

    fun setReminder(enabled: Boolean) {
        viewModelScope.launch {
            val result = setReviewRemindersEnabledUseCase(enabled)
            if (!result.isSuccess) {
                emitEffect(InsightsEffect.NavigateToNotificationSettings)
            }
        }
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
        loadLevelTransitions()
        loadResponseTimeTrend()
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
                onSuccess = { copy(accuracyTrend = UiState.Loaded(it), accuracyByDayOfWeek = computeDayOfWeekAccuracy(it)) },
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

    private fun loadLevelTransitions() {
        viewModelScope.launch {
            updateState { copy(levelTransitions = UiState.Loading) }
            getLevelTransitionsUseCase(Unit).reduce(
                onSuccess = { copy(levelTransitions = UiState.Loaded(it)) },
                onFailure = { copy(levelTransitions = UiState.Error(it.toUserMessage())) },
            )
        }
    }

    private fun loadResponseTimeTrend() {
        viewModelScope.launch {
            updateState { copy(responseTimeTrend = UiState.Loading) }
            getResponseTimeTrendUseCase(Unit).reduce(
                onSuccess = { copy(responseTimeTrend = UiState.Loaded(it)) },
                onFailure = { copy(responseTimeTrend = UiState.Error(it.toUserMessage())) },
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

