package feature.onboarding

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import domain.onboarding.model.OnboardingPreferences
import core.common.onFailure
import core.common.onSuccess
import domain.onboarding.usecase.SubmitPreferencesUseCase
import domain.settings.usecase.SetDailyGoalWordsUseCase
import domain.settings.usecase.SetLanguageUseCase
import utils.Language
import core.error.toUserMessage
import kotlinx.coroutines.launch
import core.base.BaseViewModel
import feature.onboarding.model.OnboardingEffect
import feature.onboarding.model.OnboardingUiState

class OnboardingViewModel(
    private val submitPreferencesUseCase: SubmitPreferencesUseCase,
    private val setLanguageUseCase: SetLanguageUseCase,
    private val setDailyGoalWordsUseCase: SetDailyGoalWordsUseCase,
    private val analyticsTracker: IAnalyticsTracker,
) : BaseViewModel<OnboardingUiState, OnboardingEffect>() {

    override fun initialState() = OnboardingUiState()

    init {
        analyticsTracker.logEvent("onboarding_started")
    }

    fun selectTargetLanguage(language: String) {
        updateState { copy(selectedTargetLanguage = language) }
        analyticsTracker.logEvent("onboarding_language_selected", mapOf("type" to "target", "language" to language))
    }

    fun selectNativeLanguage(language: String) {
        updateState { copy(selectedNativeLanguage = language) }
        analyticsTracker.logEvent("onboarding_language_selected", mapOf("type" to "native", "language" to language))
    }

    fun selectLevel(level: String) {
        updateState { copy(selectedLevel = level) }
        analyticsTracker.logEvent("onboarding_level_selected", mapOf("level" to level))
    }

    fun selectDailyGoal(goal: Int) {
        updateState { copy(selectedDailyGoal = goal) }
    }

    fun nextStep() {
        updateState {
            if (currentStep < totalSteps) {
                copy(currentStep = currentStep + 1, error = null)
            } else this
        }
        analyticsTracker.logEvent("onboarding_step_viewed", mapOf("step" to currentState.currentStep.toString()))
    }

    fun previousStep() {
        updateState {
            if (currentStep > 1) {
                copy(currentStep = currentStep - 1, error = null)
            } else this
        }
    }

    fun submit() {
        val state = currentState
        val targetLang = state.selectedTargetLanguage ?: return
        val nativeLang = state.selectedNativeLanguage ?: return
        val level = state.selectedLevel ?: return

        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            val preferences = OnboardingPreferences(
                targetLanguage = targetLang,
                nativeLanguage = nativeLang,
                level = level,
                interests = state.interests
            )
            submitPreferencesUseCase(preferences)
                .onSuccess { response ->
                    setLanguageUseCase(Language.fromCodeOrName(targetLang))
                    setDailyGoalWordsUseCase(state.selectedDailyGoal)
                    updateState { copy(isLoading = false) }
                    analyticsTracker.logEvent("onboarding_completed")
                    emitEffect(OnboardingEffect.NavigateToPreview(response))
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, error = error.toUserMessage()) }
                }
        }
    }

    fun skip() {
        analyticsTracker.logEvent("onboarding_skipped")
        emitEffect(OnboardingEffect.NavigateToMain)
    }
}
