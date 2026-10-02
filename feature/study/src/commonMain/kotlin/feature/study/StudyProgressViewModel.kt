package feature.study

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.UiState
import core.common.getOrThrow
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.focus.model.LanguageSummary
import domain.focus.model.LearningFocus
import domain.focus.usecase.AcknowledgeFocusIntroUseCase
import domain.focus.usecase.DismissFocusNudgeUseCase
import domain.focus.usecase.ObserveStudyFocusUseCase
import domain.focus.usecase.SetLearningFocusUseCase
import domain.notifications.usecase.ScheduleNotificationsUseCase
import domain.settings.usecase.GetSkipTagSelectorUseCase
import domain.settings.usecase.SetSkipTagSelectorUseCase
import domain.tag.model.Tag
import domain.word.usecase.EvaluateProgressUseCase
import feature.study.model.ProgressScreenState
import feature.study.util.ComposeNotificationTextResolver
import feature.study.util.NotificationTextResolver
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import performance.IPerformanceTracer

data class StudyTagUseCases(
    val getSkipTagSelector: GetSkipTagSelectorUseCase,
    val setSkipTagSelector: SetSkipTagSelectorUseCase,
)

data class StudyFocusUseCases(
    val observeStudyFocus: ObserveStudyFocusUseCase,
    val setLearningFocus: SetLearningFocusUseCase,
    val dismissFocusNudge: DismissFocusNudgeUseCase,
    val acknowledgeFocusIntro: AcknowledgeFocusIntroUseCase,
)

data class StudyProgressState(
    val progress: UiState<ProgressScreenState> = UiState.Loading,
    val hasPremiumAccess: Boolean = false,
    val dueTags: List<Tag> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val skipTagSelector: Boolean = false,
    val stageTagsMap: Map<Int, List<Tag>> = emptyMap(),
    val focus: LearningFocus = LearningFocus.All,
    val languages: List<LanguageSummary> = emptyList(),
    val nudge: LanguageSummary? = null,
    val showIntro: Boolean = false,
) {
    val showFocusSwitcher: Boolean get() = languages.size >= 2
}

class StudyProgressViewModel(
    private val evaluateProgressUseCase: EvaluateProgressUseCase,
    private val scheduleNotificationsUseCase: ScheduleNotificationsUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val performanceTracer: IPerformanceTracer,
    getFeatureAccessUseCase: GetFeatureAccessUseCase,
    private val tagUseCases: StudyTagUseCases,
    private val focusUseCases: StudyFocusUseCases,
    private val notificationTextResolver: NotificationTextResolver = ComposeNotificationTextResolver,
) : BaseViewModel<StudyProgressState, Nothing>() {

    override fun initialState() = StudyProgressState()

    private var progressObservationJob: Job? = null

    init {
        observeFeatureAccess(getFeatureAccessUseCase)
        startObservingProgress()
        observeSkipTagSelector()
    }

    fun selectFocus(focus: LearningFocus) {
        viewModelScope.launch {
            focusUseCases.setLearningFocus(focus)
            // Picking a focus means the user found the switcher; the intro has done its job.
            focusUseCases.acknowledgeFocusIntro()
            analyticsTracker.logEvent(
                "learning_focus_changed",
                mapOf("focus" to ((focus as? LearningFocus.Single)?.language?.code ?: "all")),
            )
        }
    }

    fun dismissNudge() {
        viewModelScope.launch { focusUseCases.dismissFocusNudge() }
    }

    fun acknowledgeIntro() {
        viewModelScope.launch { focusUseCases.acknowledgeFocusIntro() }
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
            focusUseCases.observeStudyFocus()
                .collect { overview ->
                    val stats = overview.progressStats
                    val screenState = ProgressScreenState(
                        progressStats = stats,
                        progressEvaluation = evaluateProgressUseCase(stats).getOrThrow(),
                    )
                    updateState {
                        copy(
                            progress = UiState.Loaded(screenState),
                            focus = overview.focus,
                            languages = overview.languages,
                            nudge = overview.nudge,
                            showIntro = overview.showIntro,
                            tags = overview.tagStats.tags,
                            dueTags = overview.tagStats.dueTags,
                            stageTagsMap = overview.tagStats.tagsByLevel,
                        )
                    }
                    performanceTracer.putMetric(trace, "total_words", stats.totalWords.toLong())
                    performanceTracer.putMetric(trace, "due_cards", stats.dueCards.toLong())
                    performanceTracer.stopTrace(trace)

                    analyticsTracker.updateUserProgress(
                        totalWords = stats.totalWords,
                        matureWords = stats.matureWords,
                        currentStreak = 0
                    )

                    val text = notificationTextResolver.resolve(stats.dueCards)
                    scheduleNotificationsUseCase(
                        stats = stats,
                        titleProvider = { text.title },
                        messageProvider = { text.message }
                    )
                }
        }
    }
}
