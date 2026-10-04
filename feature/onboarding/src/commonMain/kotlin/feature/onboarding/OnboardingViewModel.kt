package feature.onboarding

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.onFailure
import core.common.onSuccess
import core.error.toUserMessage
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.usecase.SubmitPreferencesUseCase
import domain.settings.usecase.SetDailyGoalWordsUseCase
import domain.settings.usecase.SetLanguageUseCase
import feature.onboarding.model.DailyGoalOption
import feature.onboarding.model.OnboardingEffect
import feature.onboarding.model.OnboardingStep
import feature.onboarding.model.OnboardingSubmission
import feature.onboarding.model.OnboardingUiState
import feature.onboarding.model.ProficiencyLevel
import kotlinx.coroutines.launch
import utils.Language

class OnboardingViewModel(
    private val submitPreferencesUseCase: SubmitPreferencesUseCase,
    private val setLanguageUseCase: SetLanguageUseCase,
    private val setDailyGoalWordsUseCase: SetDailyGoalWordsUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    deviceLanguageProvider: DeviceLanguageProvider,
) : BaseViewModel<OnboardingUiState, OnboardingEffect>() {

    override fun initialState() = OnboardingUiState()

    init {
        val code = deviceLanguageProvider.languageCode()?.lowercase()
        val device = currentState.languages.firstOrNull { it.code == code }
        updateState { copy(deviceLanguage = device, nativeLanguage = device) }
        analyticsTracker.logEvent("onboarding_started")
    }

    fun selectTargetLanguage(language: Language) {
        updateState {
            // The native language can't be the one being learned; fall back to the phone's language.
            val native = nativeLanguage.takeIf { it != language } ?: deviceLanguage?.takeIf { it != language }
            copy(targetLanguage = language, nativeLanguage = native)
        }
        analyticsTracker.logEvent(
            "onboarding_language_selected",
            mapOf("type" to "target", "language" to language.displayName),
        )
    }

    fun selectNativeLanguage(language: Language) {
        updateState { copy(nativeLanguage = language) }
        analyticsTracker.logEvent(
            "onboarding_language_selected",
            mapOf("type" to "native", "language" to language.displayName),
        )
    }

    fun selectLevel(level: ProficiencyLevel) {
        updateState { copy(level = level) }
        analyticsTracker.logEvent("onboarding_level_selected", mapOf("level" to level.apiValue))
    }

    fun selectDailyGoal(goal: DailyGoalOption) {
        updateState { copy(dailyGoal = goal) }
    }

    /** Primary action of the current screen: advance, or submit after the last question. */
    fun next() {
        val state = currentState
        if (!state.canContinue) return
        if (state.step == OnboardingStep.DailyGoal) {
            submit()
            return
        }
        val nextStep = OnboardingStep.entries[state.step.ordinal + 1]
        updateState { copy(step = nextStep) }
        analyticsTracker.logEvent("onboarding_step_viewed", mapOf("step" to nextStep.questionNumber.toString()))
    }

    /** Back arrow and system back. A failed submission is dismissed first; nothing happens while submitting. */
    fun back() {
        val state = currentState
        when {
            state.submission is OnboardingSubmission.Failed ->
                updateState { copy(submission = OnboardingSubmission.Idle) }
            state.submission == OnboardingSubmission.InProgress -> Unit
            state.step != OnboardingStep.Welcome ->
                updateState { copy(step = OnboardingStep.entries[step.ordinal - 1]) }
        }
    }

    fun retry() = submit()

    fun skip() {
        analyticsTracker.logEvent("onboarding_skipped")
        emitEffect(OnboardingEffect.NavigateToMain)
    }

    private fun submit() {
        val state = currentState
        if (state.submission == OnboardingSubmission.InProgress) return
        val target = state.targetLanguage ?: return
        val native = state.nativeLanguage ?: return
        val level = state.level ?: return

        updateState { copy(submission = OnboardingSubmission.InProgress) }
        viewModelScope.launch {
            val preferences = OnboardingPreferences(
                targetLanguage = target.displayName,
                nativeLanguage = native.displayName,
                level = level.apiValue,
            )
            submitPreferencesUseCase(preferences)
                .onSuccess { response ->
                    setLanguageUseCase(target)
                    setDailyGoalWordsUseCase(state.dailyGoal.words)
                    analyticsTracker.logEvent("onboarding_completed")
                    // Submission stays InProgress so the progress screen holds until navigation swaps it out.
                    emitEffect(OnboardingEffect.NavigateToPreview(response))
                }
                .onFailure { error ->
                    updateState { copy(submission = OnboardingSubmission.Failed(error.toUserMessage())) }
                }
        }
    }
}
