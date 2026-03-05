package di

import data.onboarding.remote.IOnboardingRemoteDataSource
import data.onboarding.remote.OnboardingRemoteDataSource
import data.onboarding.repository.OnboardingRepositoryImpl
import domain.onboarding.repository.IOnboardingRepository
import domain.onboarding.usecase.ImportSuggestedVocabularyUseCase
import domain.onboarding.usecase.SubmitPreferencesUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun onboardingModule() = module {

    // Remote Data Source
    single<IOnboardingRemoteDataSource> { OnboardingRemoteDataSource(apiClient = get()) }

    // Repository
    single<IOnboardingRepository> {
        OnboardingRepositoryImpl(
            remoteDataSource = get(),
            secureStorage = get()
        )
    }

    // Use Cases
    singleOf(::SubmitPreferencesUseCase)
    singleOf(::ImportSuggestedVocabularyUseCase)
}
