package feature.study

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.UiState
import core.common.getOrThrow
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.notifications.usecase.ScheduleNotificationsUseCase
import domain.tag.model.Tag
import domain.tag.usecase.GetTagsUseCase
import domain.word.usecase.EvaluateProgressUseCase
import domain.word.usecase.GetDueWordsUseCase
import domain.word.usecase.GetProgressStatsUseCase
import feature.study.model.ProgressScreenState
import feature.study.util.NotificationStringHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import performance.IPerformanceTracer

data class StudyProgressState(
    val progress: UiState<ProgressScreenState> = UiState.Loading,
    val hasPremiumAccess: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val dueTagIds: Set<Long> = emptySet(),
)

class StudyProgressViewModel(
    private val getProgressStatsUseCase: GetProgressStatsUseCase,
    private val evaluateProgressUseCase: EvaluateProgressUseCase,
    private val scheduleNotificationsUseCase: ScheduleNotificationsUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val performanceTracer: IPerformanceTracer,
    getFeatureAccessUseCase: GetFeatureAccessUseCase,
    getTagsUseCase: GetTagsUseCase,
    getDueWordsUseCase: GetDueWordsUseCase,
) : BaseViewModel<StudyProgressState, Nothing>() {

    override fun initialState() = StudyProgressState()

    private var progressObservationJob: Job? = null

    init {
        observeFeatureAccess(getFeatureAccessUseCase)
        startObservingProgress()
        startObservingTags(getTagsUseCase)
        startObservingDueTagIds(getDueWordsUseCase)
    }

    private fun startObservingTags(getTagsUseCase: GetTagsUseCase) {
        viewModelScope.launch {
            getTagsUseCase()
                .catch { /* tags unavailable, keep empty list */ }
                .collect { tags ->
                    updateState { copy(tags = tags) }
                }
        }
    }

    private fun startObservingDueTagIds(getDueWordsUseCase: GetDueWordsUseCase) {
        viewModelScope.launch {
            getDueWordsUseCase()
                .catch { /* due words unavailable */ }
                .collect { words ->
                    updateState { copy(dueTagIds = words.flatMap { it.tagIds }.toSet()) }
                }
        }
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
