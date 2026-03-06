package feature.study.di

import feature.study.StudyViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun studyModule() = module {
    viewModel {
        StudyViewModel(
            getProgressStatsUseCase = get(),
            evaluateProgressUseCase = get(),
            getFeatureAccessUseCase = get(),
            scheduleNotificationsUseCase = get(),
            getDueWordsUseCase = get(),
            getWordsByStageUseCase = get(),
            reviewWordUseCase = get(),
            updateWordUseCase = get(),
            deleteWordUseCase = get(),
            recordStreakActivityUseCase = get(),
            speakWordUseCase = get(),
            ttsRepository = get(),
            analyticsTracker = get(),
        )
    }
}
