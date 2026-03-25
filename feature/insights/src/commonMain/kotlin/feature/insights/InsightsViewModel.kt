package feature.insights

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.UiState
import data.storage.DailyInsightCache
import domain.analytics.model.AccuracyByLevel
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
import core.error.toUserMessage
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
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
    val dailyInsight: String? = null,
) {
    val availability: InsightsAvailability get() = InsightsAvailability.from(this)

    val isLoaded: Boolean get() = overview !is UiState.Loading
            && difficultWords !is UiState.Loading
            && accuracyByLevel !is UiState.Loading
            && heatmap !is UiState.Loading
            && bestStudyTime !is UiState.Loading

    val isError: Boolean get() = isLoaded && !availability.hasAnyContent && (
            overview is UiState.Error
            || difficultWords is UiState.Error
            || accuracyByLevel is UiState.Error
            || heatmap is UiState.Error
            || bestStudyTime is UiState.Error
    )
}

class InsightsViewModel(
    private val getStudyInsightsUseCase: GetStudyInsightsUseCase,
    private val getDifficultWordsUseCase: GetDifficultWordsUseCase,
    private val getAccuracyTrendUseCase: GetAccuracyTrendUseCase,
    private val getAccuracyByLevelUseCase: GetAccuracyByLevelUseCase,
    private val getStudyHeatmapUseCase: GetStudyHeatmapUseCase,
    private val getBestStudyTimeUseCase: GetBestStudyTimeUseCase,
    private val dailyInsightCache: DailyInsightCache,
) : BaseViewModel<InsightsState, Nothing>() {

    override fun initialState() = InsightsState()

    init {
        loadAllData()
    }

    fun refresh() {
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
}
