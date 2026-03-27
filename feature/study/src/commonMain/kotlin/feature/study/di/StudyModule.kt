package feature.study.di

import domain.study.usecase.GenerateSessionIdUseCase
import domain.study.usecase.ResolveCardLanguageUseCase
import domain.word.usecase.GetWordRushWordsUseCase
import feature.study.ReviewViewModel
import feature.study.StudyProgressViewModel
import feature.study.StudyTagUseCases
import feature.study.wordrush.WordRushViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun studyModule() = module {
    single { GenerateSessionIdUseCase() }
    singleOf(::ResolveCardLanguageUseCase)
    factoryOf(::GetWordRushWordsUseCase)

    viewModel {
        StudyProgressViewModel(
            getProgressStatsUseCase = get(),
            evaluateProgressUseCase = get(),
            scheduleNotificationsUseCase = get(),
            getFeatureAccessUseCase = get(),
            analyticsTracker = get(),
            performanceTracer = get(),
            tagUseCases = StudyTagUseCases(
                getDueTags = get(),
                getTagsByLevel = get(),
                getSkipTagSelector = get(),
                setSkipTagSelector = get(),
            ),
        )
    }

    viewModelOf(::ReviewViewModel)
    viewModelOf(::WordRushViewModel)
}
