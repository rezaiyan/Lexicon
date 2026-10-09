package di

import data.word.add.AddWordsLanguageRepositoryImpl
import data.word.add.ImagePreparer
import domain.word.add.service.IImagePreparer
import domain.word.add.usecase.ExtractWordsFromImageUseCase
import domain.word.add.usecase.ParseWordFileUseCase
import domain.word.add.usecase.SuggestWordsUseCase
import domain.word.add.repository.IAddWordsLanguageRepository
import domain.word.add.usecase.AddStarterWordsUseCase
import domain.word.add.usecase.AddWordsUseCase
import domain.word.add.usecase.ResolveAddWordsLanguagesUseCase
import domain.word.add.usecase.UploadPendingWordsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Clock

/** Add-words pipeline: one write path for manual, file, photo, AI and onboarding words. */
fun addWordsModule() = module {
    single { AddWordsLanguageRepositoryImpl(queries = get()) } bind IAddWordsLanguageRepository::class
    single<IImagePreparer> { ImagePreparer() }

    factory { AddWordsUseCase(wordRepository = get(), clock = Clock.System) }
    factoryOf(::ResolveAddWordsLanguagesUseCase)
    factoryOf(::ParseWordFileUseCase)
    factoryOf(::ExtractWordsFromImageUseCase)
    factoryOf(::SuggestWordsUseCase)
    factoryOf(::AddStarterWordsUseCase)
    factoryOf(::UploadPendingWordsUseCase)
}
