package domain.listening.usecase

import core.common.NoParamFlowUseCase
import core.common.Try
import core.common.UseCase
import core.common.getOrThrow
import domain.focus.filterBy
import domain.focus.model.LearningFocus
import domain.focus.usecase.ObserveLearningFocusUseCase
import domain.listening.model.LanguageVoice
import domain.listening.model.ListeningLevelOption
import domain.listening.model.ListeningOptions
import domain.listening.model.ListeningSelection
import domain.listening.model.ListeningSettings
import domain.listening.model.ListeningSource
import domain.listening.model.ListeningTagOption
import domain.listening.model.ListeningVoiceCheck
import domain.listening.model.VoiceStatus
import domain.listening.repository.IListeningSettingsRepository
import domain.tag.repository.ITagRepository
import domain.tts.repository.ITtsRepository
import domain.word.model.LearningStage
import domain.word.model.Word
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import utils.Language
import kotlin.random.Random

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
            selection = params.selection.copy(
                limit = params.selection.limit.coerceAtLeast(ListeningSelection.LIMIT_ALL),
            ),
        )
    )
}

/** Word counts for every listening source the user can pick, within the learning focus. */
class ObserveListeningOptionsUseCase(
    private val wordRepository: IWordRepository,
    private val tagRepository: ITagRepository,
    private val observeLearningFocus: ObserveLearningFocusUseCase,
) : NoParamFlowUseCase<ListeningOptions> {
    override fun invoke(params: Unit): Flow<ListeningOptions> = combine(
        wordRepository.getAllWords(),
        wordRepository.getDueCards(),
        tagRepository.getTags(),
        observeLearningFocus(),
    ) { all, due, tags, focus ->
        val words = all.filterBy(focus)
        val byStage = words.groupingBy { LearningStage.fromLevel(it.level) }.eachCount()
        val byTag = words.flatMap(Word::tagIds).groupingBy { it }.eachCount()
        ListeningOptions(
            dueCount = due.filterBy(focus).size,
            allCount = words.size,
            levels = LearningStage.entries.mapNotNull { stage ->
                byStage[stage]?.let { ListeningLevelOption(stage, it) }
            },
            tags = tags.sortedBy { it.name.lowercase() }.mapNotNull { tag ->
                byTag[tag.id]?.let { ListeningTagOption(tag.id, tag.name, it) }
            },
        )
    }.distinctUntilChanged()
}

/**
 * Picks the words for a session from [ListeningSelection]: the source's words in the learning
 * focus (due ones most overdue first, the rest newest first), optionally shuffled, then capped.
 */
class BuildListeningQueueUseCase(
    private val wordRepository: IWordRepository,
    private val observeLearningFocus: ObserveLearningFocusUseCase,
    private val random: Random = Random.Default,
) : UseCase<ListeningSelection, List<Word>> {

    override suspend fun invoke(params: ListeningSelection): Try<List<Word>> = Try {
        val focus = observeLearningFocus().first()
        val words = when (val source = params.source) {
            ListeningSource.Due -> wordRepository.getDueCards().first().filterBy(focus)
            ListeningSource.All -> newestFirst(focus)
            is ListeningSource.Level -> newestFirst(focus).filter { LearningStage.fromLevel(it.level) == source.stage }
            is ListeningSource.Tag -> newestFirst(focus).filter { source.tagId in it.tagIds }
        }
        val ordered = if (params.shuffle) words.shuffled(random) else words
        ordered.take(params.sessionSize(ordered.size))
    }

    private suspend fun newestFirst(focus: LearningFocus): List<Word> =
        wordRepository.getAllWords().first().filterBy(focus).sortedByDescending(Word::dateAdded)
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
