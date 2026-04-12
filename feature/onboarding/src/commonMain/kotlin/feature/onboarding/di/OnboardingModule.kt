package feature.onboarding.di

import feature.onboarding.OnboardingViewModel
import feature.onboarding.VocabularyPreviewViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun onboardingModule() = module {
    viewModel {
        OnboardingViewModel(
            submitPreferencesUseCase = get(),
            setLanguageUseCase = get(),
            setDailyGoalWordsUseCase = get(),
            analyticsTracker = get()
        )
    }
    viewModel { VocabularyPreviewViewModel() }
}
