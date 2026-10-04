package feature.insights

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.Try
import core.common.UiState
import core.common.getOrDefault
import core.common.map
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

/** The study-analytics reads the insights screen loads on every refresh. */
class InsightsUseCases(
    val studyInsights: GetStudyInsightsUseCase,
    val difficultWords: GetDifficultWordsUseCase,
    val accuracyTrend: GetAccuracyTrendUseCase,
    val accuracyByLevel: GetAccuracyByLevelUseCase,
    val studyHeatmap: GetStudyHeatmapUseCase,
    val bestStudyTime: GetBestStudyTimeUseCase,
    val weeklyReport: GetWeeklyReportUseCase,
    val levelTransitions: GetLevelTransitionsUseCase,
    val responseTimeTrend: GetResponseTimeTrendUseCase,
)

class InsightsViewModel(
    private val useCases: InsightsUseCases,
    private val getWordRushInsightsUseCase: GetWordRushInsightsUseCase,
    private val getProfileStatsUseCase: GetProfileStatsUseCase,
    private val dailyInsightCache: DailyInsightCache,
    private val setReviewRemindersEnabledUseCase: SetReviewRemindersEnabledUseCase,
    observeReviewRemindersEnabledUseCase: ObserveReviewRemindersEnabledUseCase,
) : BaseViewModel<InsightsState, InsightsEffect>() {

    override fun initialState() = InsightsState()

    init {
        observeReviewRemindersEnabledUseCase(Unit)
            .onEach { enabled -> updateState { copy(reviewRemindersEnabled = enabled) } }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        loadSection({ useCases.studyInsights(Unit) }) { copy(overview = it) }
        loadSection({ useCases.accuracyTrend(GetAccuracyTrendUseCase.Params(daysAgo(30), today())) }) { trend ->
            val byDayOfWeek = (trend as? UiState.Loaded)?.value?.let(::computeDayOfWeekAccuracy)
            copy(accuracyTrend = trend, accuracyByDayOfWeek = byDayOfWeek ?: accuracyByDayOfWeek)
        }
        loadSection({ useCases.difficultWords(GetDifficultWordsUseCase.Params(minReviews = 3, limit = 20)) }) {
            copy(difficultWords = it)
        }
        loadSection({ useCases.accuracyByLevel(Unit) }) { copy(accuracyByLevel = it) }
        loadSection({ useCases.studyHeatmap(GetStudyHeatmapUseCase.Params(daysAgo(90), today())) }) {
            copy(heatmap = it)
        }
        loadSection({ useCases.bestStudyTime(Unit) }) { copy(bestStudyTime = it) }
        loadSection({ getWordRushInsightsUseCase(Unit) }) { copy(wordRushInsights = it) }
        // A missing weekly report renders as an empty week rather than an error.
        loadSection({
            Try.success(useCases.weeklyReport(Unit).map { it.toUiModel() }.getOrDefault(WeeklyReportUiModel.Empty))
        }) { copy(weeklyReport = it) }
        loadSection({ useCases.levelTransitions(Unit) }) { copy(levelTransitions = it) }
        loadSection({ useCases.responseTimeTrend(Unit) }) { copy(responseTimeTrend = it) }
        loadStreak()
        updateState { copy(dailyInsight = dailyInsightCache.getDailyInsight()) }
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

    /** Loads one independent section: marks it [UiState.Loading], then stores the result or its error. */
    private fun <T> loadSection(
        request: suspend () -> Try<T>,
        apply: InsightsState.(UiState<T>) -> InsightsState,
    ) {
        viewModelScope.launch {
            updateState { apply(UiState.Loading) }
            request().reduce(
                onSuccess = { apply(UiState.Loaded(it)) },
                onFailure = { apply(UiState.Error(it.toUserMessage())) },
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

private fun localToday(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun today(): String = localToday().toString()

private fun daysAgo(days: Int): String = localToday().minus(days, DateTimeUnit.DAY).toString()

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

