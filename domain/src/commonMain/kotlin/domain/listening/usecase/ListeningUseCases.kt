package domain.listening.usecase

import core.common.NoParamFlowUseCase
import core.common.Try
import core.common.UseCase
import core.common.getOrThrow
import domain.focus.filterBy
import domain.focus.usecase.ObserveLearningFocusUseCase
import domain.listening.model.LanguageVoice
import domain.listening.model.ListeningQueue
import domain.listening.model.ListeningSettings
import domain.listening.model.ListeningVoiceCheck
import domain.listening.model.VoiceStatus
import domain.listening.repository.IListeningSettingsRepository
import domain.tts.repository.ITtsRepository
import domain.word.model.ReviewSource
import domain.word.model.Word
import domain.word.repository.IWordRepository
import domain.word.usecase.LoadReviewQueueUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import utils.Language

class ObserveListeningSettingsUseCase(
    private val repository: IListeningSettingsRepository,
) : NoParamFlowUseCase<ListeningSettings> {
    override fun invoke(params: Unit): Flow<ListeningSettings> = repository.observe()
}

/** Persists settings, clamping values to the ranges the UI offers. */
class SaveListeningSettingsUseCase(
    private val repository: IListeningSettingsRepository,
) : UseCase<ListeningSettings, Unit> {
    override suspend fun invoke(params: ListeningSettings): Try<Unit> = repository.save(
        params.copy(
            pauseMs = params.pauseMs.coerceIn(
                ListeningSettings.PAUSE_OPTIONS_MS.first(),
                ListeningSettings.PAUSE_OPTIONS_MS.last(),
            ),
            repeatCount = params.repeatCount.coerceIn(ListeningSettings.MIN_REPEAT, ListeningSettings.MAX_REPEAT),
        )
    )
}

/**
 * Loads words to listen to from [ReviewSource]. When nothing is due, falls back to the most
 * recently added words in the learning focus — listening is passive, so it is useful even
 * on days with no reviews.
 */
class BuildListeningQueueUseCase(
    private val loadReviewQueue: LoadReviewQueueUseCase,
    private val wordRepository: IWordRepository,
    private val observeLearningFocus: ObserveLearningFocusUseCase,
) : UseCase<ReviewSource, ListeningQueue> {

    override suspend fun invoke(params: ReviewSource): Try<ListeningQueue> = Try {
        val queued = loadReviewQueue(params).getOrThrow()
        if (queued.isNotEmpty()) return@Try ListeningQueue(queued, isRecentFallback = false)

        val focus = observeLearningFocus().first()
        val recent = wordRepository.getAllWords().first()
            .filterBy(focus)
            .sortedByDescending(Word::dateAdded)
            .take(RECENT_FALLBACK_LIMIT)
        ListeningQueue(recent, isRecentFallback = recent.isNotEmpty())
    }

    companion object {
        const val RECENT_FALLBACK_LIMIT = 20
    }
}

/** Reports, per language in [words], whether a voice model is ready, downloadable, or unavailable. */
class CheckListeningVoicesUseCase(
    private val ttsRepository: ITtsRepository,
) : UseCase<List<Word>, ListeningVoiceCheck> {

    override suspend fun invoke(params: List<Word>): Try<ListeningVoiceCheck> = Try {
        val codes = params
            .flatMap { listOf(it.targetLanguage.code, it.sourceLanguage.code) }
            .map(Language::toCode)
            .distinct()
        ListeningVoiceCheck(
            codes.map { code ->
                val status = when {
                    !ttsRepository.isLanguageSupported(code) -> VoiceStatus.UNSUPPORTED
                    ttsRepository.isModelDownloaded(code).getOrThrow() -> VoiceStatus.READY
                    else -> VoiceStatus.MISSING
                }
                LanguageVoice(code, status)
            }
        )
    }
}
