package feature.insights

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.UiState
import data.storage.DailyInsightCache
import domain.analytics.model.AccuracyByLevel
import domain.analytics.model.DayOfWeekAccuracy
import domain.analytics.model.DailyStudyStats
import domain.analytics.model.HourlyAccuracy
import domain.analytics.model.StudyHeatmapDay
import domain.analytics.model.StudyInsights
import domain.analytics.model.WordDifficulty
import domain.analytics.model.LevelTransition
import domain.analytics.model.ResponseTimeTrend
import domain.settings.usecase.ObserveReviewRemindersEnabledUseCase
import domain.settings.usecase.SetReviewRemindersEnabledUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import domain.wordrush.model.WordRushInsights
import kotlinx.coroutines.launch

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
    private val useCases: InsightsUseCases,
    private val dailyInsightCache: DailyInsightCache,
    private val setReviewRemindersEnabledUseCase: SetReviewRemindersEnabledUseCase,
    private val observeReviewRemindersEnabledUseCase: ObserveReviewRemindersEnabledUseCase,
) : BaseViewModel<InsightsState, InsightsEffect>() {

    private val dataLoader = InsightsDataLoader(useCases, stateAccess, viewModelScope)

    override fun initialState() = InsightsState()

    init {
        observeReviewRemindersEnabledUseCase(Unit)
            .onEach { enabled -> updateState { copy(reviewRemindersEnabled = enabled) } }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        dataLoader.loadAll()
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

}
