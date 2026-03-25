package di

import data.settings.local.ISettingsLocalDataSource
import data.settings.local.SettingsLocalDataSourceImpl
import data.settings.repository.SettingsRepositoryImpl
import data.streak.remote.IStreakRemoteDataSource
import data.streak.remote.StreakRemoteDataSource
import data.streak.repository.StreakRepositoryImpl
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.settings.usecase.GetReviewSettingsUseCase
import domain.settings.usecase.ObserveSpeechRateUseCase
import domain.settings.usecase.GetSkipTagSelectorUseCase
import domain.settings.usecase.SetLanguageUseCase
import domain.settings.usecase.SetNotificationsEnabledUseCase
import domain.settings.usecase.SetSkipTagSelectorUseCase
import domain.settings.usecase.SetThemeModeUseCase
import domain.settings.usecase.SetTtsVoiceUseCase
import domain.settings.usecase.SetTtsSpeechRateUseCase
import domain.streak.repository.IStreakRepository
import domain.streak.usecase.GetStreakUseCase
import domain.streak.usecase.RecordStreakActivityUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

fun settingsModule() = module {

    // Local Data Sources
    single<ISettingsLocalDataSource> { SettingsLocalDataSourceImpl(queries = get()) }

    // Remote Data Sources
    single<IStreakRemoteDataSource> { StreakRemoteDataSource(apiClient = get()) }

    // Repositories
    single {
        SettingsRepositoryImpl(localDataSource = get())
    } bind ISettingsRepository::class

    single<IStreakRepository> {
        StreakRepositoryImpl(streakRemoteDataSource = get())
    }

    // Use Cases - Settings Read
    singleOf(::GetCurrentLanguageUseCase)
    single { GetReviewSettingsUseCase() }
    singleOf(::ObserveSpeechRateUseCase)

    // Use Cases - Settings Write
    singleOf(::SetLanguageUseCase)
    singleOf(::SetThemeModeUseCase)
    singleOf(::SetNotificationsEnabledUseCase)
    singleOf(::SetTtsSpeechRateUseCase)
    singleOf(::SetTtsVoiceUseCase)
    singleOf(::GetSkipTagSelectorUseCase)
    singleOf(::SetSkipTagSelectorUseCase)

    // Use Cases - Streak
    singleOf(::GetStreakUseCase)
    singleOf(::RecordStreakActivityUseCase)
}
