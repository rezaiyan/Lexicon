package feature.study.di

import feature.study.ReviewViewModel
import feature.study.StudyProgressViewModel
import org.koin.core.module.dsl.viewModel
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
        )
    }
    viewModel {
        ReviewViewModel(
            getDueWordsUseCase = get(),
            getWordsByStageUseCase = get(),
            reviewWordUseCase = get(),
            updateWordUseCase = get(),
            deleteWordUseCase = get(),
            recordStreakActivityUseCase = get(),
            speakWordUseCase = get(),
            analyticsTracker = get(),
            ttsRepository = get(),
        )
    }
}
