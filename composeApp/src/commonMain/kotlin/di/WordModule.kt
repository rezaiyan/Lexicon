package di

import data.ai.remote.AiRemoteDataSource
import data.ai.remote.IAiRemoteDataSource
import data.ai.repository.AiRepositoryImpl
import data.word.local.IWordLocalDataSource
import data.word.local.WordLocalDataSource
import data.word.remote.IWordRemoteDataSource
import data.word.remote.WordRemoteDataSource
import data.word.repository.WordRepositoryImpl
import data.word.sync.IWordConflictResolver
import data.word.sync.IWordRemoteSyncHandler
import data.word.sync.WordConflictResolver
import data.word.sync.WordRemoteSyncHandler
import domain.ai.repository.IAiRepository
import domain.ai.usecase.ImportFromImageUseCase
import domain.ai.usecase.IsAiAvailableUseCase
import domain.word.repository.IWordRepository
import domain.word.service.IImportValidationService
import domain.word.service.ImportValidationService
import domain.word.usecase.DeleteWordUseCase
import domain.word.usecase.EvaluateProgressUseCase
import domain.word.usecase.BatchUpdateLanguagesUseCase
import domain.word.usecase.DeleteWordsUseCase
import domain.word.usecase.ExportWordsUseCase
import domain.word.usecase.GetAllWordsUseCase
import domain.word.usecase.GetSourceLanguageUseCase
import domain.word.usecase.GetDueWordsUseCase
import domain.word.usecase.GetProgressStatsUseCase
import domain.word.usecase.GetWordsByStageUseCase
import domain.word.usecase.ImportViaFileUseCase
import domain.word.usecase.ImportWordsUseCase
import domain.word.usecase.ReviewWordUseCase
import domain.word.usecase.SyncRemoteToLocalUseCase
import domain.word.usecase.UpdateWordUseCase
import domain.widget.usecase.GetDailyWidgetDataUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

fun wordModule() = module {

    // Word Data Components
    single<IWordLocalDataSource> { WordLocalDataSource(queries = get(), settingsRepository = get()) }
    single<IWordRemoteSyncHandler> {
        WordRemoteSyncHandler(wordRemoteDataSource = get(), performanceTracer = get())
    }
    single<IWordConflictResolver> { WordConflictResolver() }

    // Remote Data Sources
    single<IWordRemoteDataSource> { WordRemoteDataSource(apiClient = get()) }
    single<IAiRemoteDataSource> { AiRemoteDataSource(apiClient = get()) }

    // Repositories
    single<IWordRepository> {
        WordRepositoryImpl(
            localDataSource = get(),
            remoteSyncHandler = get(),
            conflictResolver = get()
        )
    }

    single {
        AiRepositoryImpl(aiRemoteDataSource = get())
    } bind IAiRepository::class

    // Domain Services
    single<IImportValidationService> { ImportValidationService() }

    // Use Cases - Vocabulary
    singleOf(::ReviewWordUseCase)
    singleOf(::ImportWordsUseCase)
    singleOf(::ImportFromImageUseCase)
    single {
        ImportViaFileUseCase(importWordsUseCase = get())
    }
    singleOf(::GetProgressStatsUseCase)
    singleOf(::EvaluateProgressUseCase)
    singleOf(::GetWordsByStageUseCase)
    singleOf(::GetDueWordsUseCase)
    singleOf(::IsAiAvailableUseCase)
    singleOf(::SyncRemoteToLocalUseCase)

    // Use Cases - Word Management
    singleOf(::GetAllWordsUseCase)
    singleOf(::DeleteWordUseCase)
    singleOf(::BatchUpdateLanguagesUseCase)
    singleOf(::DeleteWordsUseCase)
    singleOf(::UpdateWordUseCase)
    singleOf(::ExportWordsUseCase)
    singleOf(::GetSourceLanguageUseCase)

    // Use Cases - Widget
    singleOf(::GetDailyWidgetDataUseCase)
}
