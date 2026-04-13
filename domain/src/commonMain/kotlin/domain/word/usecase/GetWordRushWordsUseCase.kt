package domain.word.usecase

import core.common.Try
import core.common.getOrThrow
import domain.word.model.Word
import domain.word.repository.IWordRepository

/**
 * Fetches words for the Word Rush game.
 * Returns a mixed list of [count] words drawn proportionally from each SRS level,
 * so a single round never shows only one type of word.
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
        selectMixedWords(allWords, count)
    }

    /**
     * Groups words by SRS level, shuffles each group, then interleaves them
     * round-robin so the resulting list contains words from every level present
     * before exhausting any single level. The final list is shuffled again so
     * the player cannot infer level order from question sequence.
     */
    private fun selectMixedWords(allWords: List<Word>, count: Int): List<Word> {
        val queues = allWords
            .groupBy { it.level }
            .values
            .map { ArrayDeque(it.shuffled()) }

        return buildList {
            while (size < count) {
                val sizeBefore = size
                for (queue in queues) {
                    if (size >= count) break
                    queue.removeFirstOrNull()?.let { add(it) }
                }
                if (size == sizeBefore) break // all queues exhausted
            }
        }.shuffled()
    }

    companion object {
        const val MINIMUM_WORDS = 4
    }
}
