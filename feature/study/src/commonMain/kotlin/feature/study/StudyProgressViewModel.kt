@file:OptIn(ExperimentalTime::class)

package feature.study

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.UiState
import core.common.getOrThrow
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.notifications.usecase.ScheduleNotificationsUseCase
import domain.word.usecase.EvaluateProgressUseCase
import domain.word.usecase.GetProgressStatsUseCase
import feature.study.model.ProgressScreenState
import feature.study.util.NotificationStringHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import performance.IPerformanceTracer
import kotlin.time.ExperimentalTime

data class StudyProgressState(
    val progress: UiState<ProgressScreenState> = UiState.Loading,
    val hasPremiumAccess: Boolean = false,
)

class StudyProgressViewModel(
    private val getProgressStatsUseCase: GetProgressStatsUseCase,
    private val evaluateProgressUseCase: EvaluateProgressUseCase,
    private val scheduleNotificationsUseCase: ScheduleNotificationsUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val performanceTracer: IPerformanceTracer,
    getFeatureAccessUseCase: GetFeatureAccessUseCase,
) : BaseViewModel<StudyProgressState, Nothing>() {

    override fun initialState() = StudyProgressState()

    private var progressObservationJob: Job? = null

    init {
        observeFeatureAccess(getFeatureAccessUseCase)
        startObservingProgress()
    }

    private fun observeFeatureAccess(getFeatureAccessUseCase: GetFeatureAccessUseCase) {
        viewModelScope.launch {
            getFeatureAccessUseCase()
                .map { it.userAccess.hasPremiumAccess }
                .catch { emit(false) }
                .collect { hasPremium ->
                    updateState { copy(hasPremiumAccess = hasPremium) }
                }
        }
    }

    fun refreshStats() {
        progressObservationJob?.cancel()
        startObservingProgress()
    }

    private fun startObservingProgress() {
        progressObservationJob = viewModelScope.launch {
            val trace = performanceTracer.startTrace("study_session_load")
            getProgressStatsUseCase.invoke()
                .collect { stats ->
                    val screenState = ProgressScreenState(
                        progressStats = stats,
                        progressEvaluation = evaluateProgressUseCase(stats).getOrThrow(),
                        messageState = null
                    )
                    updateState { copy(progress = UiState.Loaded(screenState)) }
                    performanceTracer.putMetric(trace, "total_words", stats.totalWords.toLong())
                    performanceTracer.putMetric(trace, "due_cards", stats.dueCards.toLong())
                    performanceTracer.stopTrace(trace)

                    analyticsTracker.updateUserProgress(
                        totalWords = stats.totalWords,
                        matureWords = stats.matureWords,
                        currentStreak = 0
                    )

                    val notifStrings =
                        NotificationStringHelper.getNotificationResources(stats.dueCards)
                    val title = getString(
                        notifStrings.titleRes,
                        *notifStrings.titleParams.toTypedArray()
                    )
                    val message = getString(
                        notifStrings.messageRes,
                        *notifStrings.messageParams.toTypedArray()
                    )
                    scheduleNotificationsUseCase(
                        stats = stats,
                        titleProvider = { title },
                        messageProvider = { message }
                    )
                }
        }
    }
}
