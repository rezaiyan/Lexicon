package di

import data.listening.IListeningSettingsLocalDataSource
import data.listening.ListeningSettingsLocalDataSourceImpl
import data.listening.ListeningSettingsRepositoryImpl
import domain.listening.repository.IListeningSettingsRepository
import domain.listening.usecase.BuildListeningQueueUseCase
import domain.listening.usecase.CheckListeningVoicesUseCase
import domain.listening.usecase.ObserveListeningOptionsUseCase
import domain.listening.usecase.ObserveListeningSettingsUseCase
import domain.listening.usecase.SaveListeningSettingsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

/** Listening mode: hands-free audio review (local-only preferences). */
fun listeningModule() = module {
    single { ListeningSettingsLocalDataSourceImpl(queries = get()) } bind IListeningSettingsLocalDataSource::class
    single { ListeningSettingsRepositoryImpl(localDataSource = get()) } bind IListeningSettingsRepository::class

    factoryOf(::ObserveListeningSettingsUseCase)
    factoryOf(::ObserveListeningOptionsUseCase)
    factoryOf(::SaveListeningSettingsUseCase)
    factory { BuildListeningQueueUseCase(wordRepository = get(), observeLearningFocus = get()) }
    factoryOf(::CheckListeningVoicesUseCase)
}
