package di

import data.listening.IListeningSettingsLocalDataSource
import data.listening.ListeningSettingsLocalDataSourceImpl
import data.listening.ListeningRecorderImpl
import data.listening.ListeningSettingsRepositoryImpl
import data.listening.local.IListeningPendingQueue
import data.listening.local.ListeningPendingQueue
import data.listening.remote.IListeningDataSource
import data.listening.remote.ListeningRemoteDataSource
import domain.listening.repository.IListeningRecorder
import domain.listening.repository.IListeningSettingsRepository
import domain.listening.usecase.BuildListeningQueueUseCase
import domain.listening.usecase.CheckListeningVoicesUseCase
import domain.listening.usecase.ObserveListeningOptionsUseCase
import domain.listening.usecase.ObserveListeningSettingsUseCase
import domain.listening.usecase.RecordListeningSessionUseCase
import domain.listening.usecase.SaveListeningSettingsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/** Listening mode: hands-free audio review (local preferences, sessions synced for insights). */
fun listeningModule() = module {
    single { ListeningSettingsLocalDataSourceImpl(queries = get()) } bind IListeningSettingsLocalDataSource::class
    single { ListeningSettingsRepositoryImpl(localDataSource = get()) } bind IListeningSettingsRepository::class

    factoryOf(::ObserveListeningSettingsUseCase)
    factoryOf(::ObserveListeningOptionsUseCase)
    factoryOf(::SaveListeningSettingsUseCase)
    factory { BuildListeningQueueUseCase(wordRepository = get(), observeLearningFocus = get()) }
    factoryOf(::CheckListeningVoicesUseCase)

    singleOf(::ListeningRemoteDataSource) bind IListeningDataSource::class
    singleOf(::ListeningPendingQueue) bind IListeningPendingQueue::class
    singleOf(::ListeningRecorderImpl) bind IListeningRecorder::class
    factoryOf(::RecordListeningSessionUseCase)
}
