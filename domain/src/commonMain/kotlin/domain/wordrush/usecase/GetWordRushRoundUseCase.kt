package domain.wordrush.usecase

import core.common.Try
import core.common.UseCase
import core.common.getOrThrow
import domain.focus.filterBy
import domain.focus.usecase.ObserveLearningFocusUseCase
import domain.word.repository.IWordRepository
import domain.word.usecase.GetWordRushWordsUseCase
import domain.word.usecase.selectMixedWords
import domain.wordrush.WordRushRoundBuilder
import domain.wordrush.model.WordRushQuestion
import kotlinx.coroutines.flow.first

/**
 * Builds a Word Rush round of up to [count] questions from the user's learning focus.
 * Distractors are drawn from the whole focused library, not just the round's words.
 */
class GetWordRushRoundUseCase(
    private val wordRepository: IWordRepository,
    private val observeLearningFocus: ObserveLearningFocusUseCase,
) : UseCase<Int, List<WordRushQuestion>> {

    override suspend fun invoke(params: Int): Try<List<WordRushQuestion>> = Try {
        val focus = observeLearningFocus().first()
        val pool = wordRepository.getAllWordsAsync().getOrThrow().filterBy(focus)
        require(pool.size >= GetWordRushWordsUseCase.MINIMUM_WORDS) {
            "Need at least ${GetWordRushWordsUseCase.MINIMUM_WORDS} words to play Word Rush"
        }
        val questions = WordRushRoundBuilder.build(selectMixedWords(pool, params), pool)
        require(questions.isNotEmpty()) { "Not enough distinct words to build Word Rush questions" }
        questions
    }
}
