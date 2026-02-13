package di

import data.settings.remote.SettingsRemoteDataSource
import data.settings.repository.SettingsRepositoryImpl
import data.streak.remote.StreakRemoteDataSource
import data.streak.repository.StreakRepositoryImpl
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.settings.usecase.GetReviewSettingsUseCase
import domain.settings.usecase.UpdateReviewSettingsUseCase
import domain.streak.repository.IStreakRepository
import domain.streak.usecase.GetStreakUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

fun settingsModule() = module {

    // Remote Data Sources
    single { SettingsRemoteDataSource(apiClient = get()) }
    single { StreakRemoteDataSource(apiClient = get()) }

    // Repositories
    single {
        SettingsRepositoryImpl(
            queries = get(),
            settingsRemoteDataSource = get()
        )
    } bind ISettingsRepository::class

    single<IStreakRepository> {
        StreakRepositoryImpl(streakRemoteDataSource = get())
    }

    // Use Cases
    singleOf(::GetCurrentLanguageUseCase)
    singleOf(::GetReviewSettingsUseCase)
    singleOf(::UpdateReviewSettingsUseCase)
    singleOf(::GetStreakUseCase)
}
