package feature.study

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.UiState
import core.common.getOrThrow
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.notifications.usecase.ScheduleNotificationsUseCase
import domain.settings.usecase.GetSkipTagSelectorUseCase
import domain.settings.usecase.SetSkipTagSelectorUseCase
import domain.tag.model.Tag
import domain.tag.usecase.GetDueTagsUseCase
import domain.tag.usecase.GetTagsByLevelUseCase
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

data class StudyTagUseCases(
    val getDueTags: GetDueTagsUseCase,
    val getTagsByLevel: GetTagsByLevelUseCase,
    val getSkipTagSelector: GetSkipTagSelectorUseCase,
    val setSkipTagSelector: SetSkipTagSelectorUseCase,
)

data class StudyProgressState(
    val progress: UiState<ProgressScreenState> = UiState.Loading,
    val hasPremiumAccess: Boolean = false,
    val dueTags: List<Tag> = emptyList(),
    val skipTagSelector: Boolean = false,
    val stageTagsMap: Map<Int, List<Tag>> = emptyMap(),
)

class StudyProgressViewModel(
    private val getProgressStatsUseCase: GetProgressStatsUseCase,
    private val evaluateProgressUseCase: EvaluateProgressUseCase,
    private val scheduleNotificationsUseCase: ScheduleNotificationsUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val performanceTracer: IPerformanceTracer,
    getFeatureAccessUseCase: GetFeatureAccessUseCase,
    private val tagUseCases: StudyTagUseCases,
) : BaseViewModel<StudyProgressState, Nothing>() {

    override fun initialState() = StudyProgressState()

    private var progressObservationJob: Job? = null

    init {
        observeFeatureAccess(getFeatureAccessUseCase)
        startObservingProgress()
        startObservingDueTags()
        observeSkipTagSelector()
        startObservingTagsByLevel()
    }

    private fun startObservingDueTags() {
        viewModelScope.launch {
            tagUseCases.getDueTags()
                .catch { }
                .collect { tags -> updateState { copy(dueTags = tags) } }
        }
    }

    private fun startObservingTagsByLevel() {
        viewModelScope.launch {
            tagUseCases.getTagsByLevel()
                .catch { }
                .collect { map -> updateState { copy(stageTagsMap = map) } }
        }
    }

    private fun observeSkipTagSelector() {
        viewModelScope.launch {
            tagUseCases.getSkipTagSelector(Unit)
                .catch { /* ignore */ }
                .collect { skip -> updateState { copy(skipTagSelector = skip) } }
        }
    }

    fun setSkipTagSelector(skip: Boolean) {
        viewModelScope.launch { tagUseCases.setSkipTagSelector(skip) }
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
