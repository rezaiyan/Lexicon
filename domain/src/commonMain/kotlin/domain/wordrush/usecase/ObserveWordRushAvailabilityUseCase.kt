package domain.wordrush.usecase

import core.common.NoParamFlowUseCase
import domain.focus.filterBy
import domain.focus.usecase.ObserveLearningFocusUseCase
import domain.word.repository.IWordRepository
import domain.word.usecase.GetWordRushWordsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Whether the learning focus holds enough words to play Word Rush.
 * Follows the local word set as well as the focus, so words that land after
 * the first check (e.g. the post-login sync) unlock the game right away.
 */
class ObserveWordRushAvailabilityUseCase(
    private val wordRepository: IWordRepository,
    private val observeLearningFocus: ObserveLearningFocusUseCase,
) : NoParamFlowUseCase<Boolean> {

    operator fun invoke(): Flow<Boolean> =
        combine(wordRepository.getAllWords(), observeLearningFocus()) { words, focus ->
            words.filterBy(focus).size >= GetWordRushWordsUseCase.MINIMUM_WORDS
        }.distinctUntilChanged()

    override operator fun invoke(params: Unit) = invoke()
}
