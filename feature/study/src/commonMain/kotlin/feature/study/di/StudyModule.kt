package feature.study.di

import feature.study.ReviewSessionUseCases
import feature.study.ReviewViewModel
import feature.study.ReviewWordUseCases
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
            getTagsUseCase = get(),
            getDueWordsUseCase = get(),
        )
    }
    viewModel {
        ReviewViewModel(
            wordUseCases = ReviewWordUseCases(
                getDueWords = get(),
                getWordsByStage = get(),
                getDueWordsByTag = get(),
                reviewWord = get(),
                updateWord = get(),
                deleteWord = get(),
            ),
            sessionUseCases = ReviewSessionUseCases(
                startSession = get(),
                endSession = get(),
                recordEvent = get(),
                recordStreak = get(),
                getSettings = get(),
            ),
            speakWordUseCase = get(),
            analyticsTracker = get(),
            settingsRepository = get(),
            ttsRepository = get(),
        )
    }
}
