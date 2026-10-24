package feature.study.di

import domain.study.usecase.GenerateSessionIdUseCase
import domain.study.usecase.ResolveCardLanguageUseCase
import domain.word.usecase.GetNextDueDateUseCase
import domain.word.usecase.GetWordRushWordsUseCase
import domain.wordrush.usecase.GetWordRushInsightsUseCase
import domain.wordrush.usecase.GetWordRushRoundUseCase
import domain.wordrush.usecase.ObserveWordRushAvailabilityUseCase
import domain.wordrush.usecase.RecordWordRushGameUseCase
import feature.study.ReviewViewModel
import feature.study.StudyProgressViewModel
import feature.study.StudyFocusUseCases
import feature.study.StudyTagUseCases
import feature.study.listening.ListeningMiniPlayerViewModel
import feature.study.listening.ListeningViewModel
import feature.study.listening.playback.ListeningPlayerVisibility
import feature.study.listening.playback.ListeningMediaBridge
import feature.study.listening.playback.ListeningPlaybackController
import feature.study.listening.playback.loadListeningMediaText
import feature.study.wordrush.WordRushViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import media.IListeningMediaSession
import media.createListeningMediaSession
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.Module
import org.koin.dsl.module

fun studyModule() = module {
    single { GenerateSessionIdUseCase() }
    singleOf(::ResolveCardLanguageUseCase)
    factoryOf(::GetNextDueDateUseCase)
    factoryOf(::GetWordRushWordsUseCase)
    factoryOf(::GetWordRushRoundUseCase)
    factoryOf(::ObserveWordRushAvailabilityUseCase)
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

    listeningPlayback()
    viewModelOf(::ListeningViewModel)
    viewModel {
        WordRushViewModel(
            getWordRushRoundUseCase = get(),
            recordWordRushGameUseCase = get(),
            analyticsTracker = get(),
            getWordRushInsightsUseCase = get(),
            observeWordRushAvailability = get(),
        )
    }
}

/** Listening playback: app-scoped, so it keeps playing in the background and outlives the Study screen. */
private fun Module.listeningPlayback() {
    single<IListeningMediaSession> { createListeningMediaSession() }
    singleOf(::ListeningPlayerVisibility)
    viewModelOf(::ListeningMiniPlayerViewModel)

    // Its media bridge (notification / lock screen) starts with it, so app start pays nothing.
    single {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        ListeningPlaybackController(
            scope = scope,
            speakWord = get(),
            stopSpeaking = get(),
            observeSettings = get(),
            observeSpeechRate = get(),
            analyticsTracker = get(),
            recordSession = get(),
            generateSessionId = get(),
        ).also { controller ->
            ListeningMediaBridge(
                controller = controller,
                session = get(),
                observeSettings = get(),
                loadText = ::loadListeningMediaText,
                scope = scope,
            ).start()
        }
    }
}
