package di

import data.focus.local.ILearningFocusLocalDataSource
import data.focus.local.LearningFocusLocalDataSourceImpl
import data.focus.repository.LearningFocusRepositoryImpl
import domain.focus.repository.ILearningFocusRepository
import domain.focus.usecase.AcknowledgeFocusIntroUseCase
import domain.focus.usecase.DismissFocusNudgeUseCase
import domain.focus.usecase.ObserveLearningFocusUseCase
import domain.focus.usecase.ObserveStudyFocusUseCase
import domain.focus.usecase.SetLearningFocusUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** Learning focus: one active learning language at a time (local-only preference). */
fun focusModule() = module {
    single<ILearningFocusLocalDataSource> { LearningFocusLocalDataSourceImpl(queries = get()) }
    single<ILearningFocusRepository> { LearningFocusRepositoryImpl(localDataSource = get()) }

    // Explicit lambdas: these take a `nowMillis` default param that Koin's constructor DSL can't resolve.
    factory { ObserveLearningFocusUseCase(wordRepository = get(), focusRepository = get()) }
    factory { ObserveStudyFocusUseCase(wordRepository = get(), tagRepository = get(), focusRepository = get()) }
    factoryOf(::SetLearningFocusUseCase)
    factory { DismissFocusNudgeUseCase(focusRepository = get()) }
    factoryOf(::AcknowledgeFocusIntroUseCase)
}
