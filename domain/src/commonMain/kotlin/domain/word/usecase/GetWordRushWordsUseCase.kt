package domain.word.usecase

import core.common.Try
import core.common.getOrThrow
import domain.word.model.Word
import domain.word.repository.IWordRepository

/**
 * Fetches words for the Word Rush game.
 * Returns a shuffled list of [count] words (or fewer if not enough exist).
 * Minimum 4 words required to generate distractor options.
 */
class GetWordRushWordsUseCase(
    private val wordRepository: IWordRepository,
) {

    suspend operator fun invoke(count: Int): Try<List<Word>> = Try {
        val allWords = wordRepository.getAllWordsAsync().getOrThrow()
        require(allWords.size >= MINIMUM_WORDS) {
            "Need at least $MINIMUM_WORDS words to play Word Rush"
        }
        allWords.shuffled().take(count)
    }

    companion object {
        const val MINIMUM_WORDS = 4
    }
}
