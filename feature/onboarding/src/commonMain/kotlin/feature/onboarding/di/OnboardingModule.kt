package feature.onboarding.di

import androidx.compose.ui.text.intl.Locale
import domain.onboarding.model.SuggestedVocabulary
import feature.onboarding.DeviceLanguageProvider
import feature.onboarding.OnboardingViewModel
import feature.onboarding.VocabularyPreviewViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun onboardingModule() = module {
    single { DeviceLanguageProvider { Locale.current.language } }
    viewModel {
        OnboardingViewModel(
            submitPreferencesUseCase = get(),
            setDailyGoalWordsUseCase = get(),
            analyticsTracker = get(),
            deviceLanguageProvider = get(),
        )
    }
    viewModel { (words: List<SuggestedVocabulary>) -> VocabularyPreviewViewModel(words) }
}
