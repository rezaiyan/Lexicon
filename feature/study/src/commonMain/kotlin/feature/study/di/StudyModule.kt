package feature.study.di

import domain.study.usecase.GenerateSessionIdUseCase
import domain.study.usecase.ResolveCardLanguageUseCase
import domain.word.usecase.GetNextDueDateUseCase
import domain.word.usecase.GetWordRushWordsUseCase
import domain.wordrush.usecase.GetWordRushInsightsUseCase
import domain.wordrush.usecase.GetWordRushRoundUseCase
import domain.wordrush.usecase.RecordWordRushGameUseCase
import feature.study.ReviewViewModel
import feature.study.StudyProgressViewModel
import feature.study.StudyFocusUseCases
import feature.study.StudyTagUseCases
import feature.study.listening.ListeningViewModel
import feature.study.wordrush.WordRushViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun studyModule() = module {
    single { GenerateSessionIdUseCase() }
    singleOf(::ResolveCardLanguageUseCase)
    factoryOf(::GetNextDueDateUseCase)
    factoryOf(::GetWordRushWordsUseCase)
    factoryOf(::GetWordRushRoundUseCase)
    factoryOf(::RecordWordRushGameUseCase)
    factoryOf(::GetWordRushInsightsUseCase)

    viewModel {
        StudyProgressViewModel(
            evaluateProgressUseCase = get(),
            scheduleNotificationsUseCase = get(),
            analyticsTracker = get(),
            performanceTracer = get(),
            tagUseCases = StudyTagUseCases(
                getSkipTagSelector = get(),
                setSkipTagSelector = get(),
            ),
            focusUseCases = StudyFocusUseCases(
                observeStudyFocus = get(),
                setLearningFocus = get(),
                dismissFocusNudge = get(),
                acknowledgeFocusIntro = get(),
            ),
        )
    }

    viewModelOf(::ReviewViewModel)
    viewModelOf(::ListeningViewModel)
    viewModel {
        WordRushViewModel(
            getWordRushWordsUseCase = get(),
            getWordRushRoundUseCase = get(),
            recordWordRushGameUseCase = get(),
            analyticsTracker = get(),
            getWordRushInsightsUseCase = get(),
            observeLearningFocus = get(),
        )
    }
}
