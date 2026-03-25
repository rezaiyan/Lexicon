package feature.study.di

import feature.study.ReviewViewModel
import feature.study.StudyProgressViewModel
import feature.study.StudyTagUseCases
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun studyModule() = module {
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
}
