package domain.word.usecase

import core.common.Try
import core.common.UseCase
import core.common.getOrDefault
import domain.settings.usecase.GetDailyGoalWordsUseCase
import domain.word.model.ReviewSource
import domain.word.model.Word
import kotlinx.coroutines.flow.first

/**
 * Single entry point for loading a review queue.
 *
 * Dispatches on [ReviewSource] so callers don't need to pick among five
 * different use cases or special-case tag filtering in the ViewModel.
 *
 * For [ReviewSource.DueCards] the queue is capped at the user's daily goal
 * so a single study session stays focused and achievable.
 */
class LoadReviewQueueUseCase(
    private val getDueWords: GetDueWordsUseCase,
    private val getWordsByStage: GetWordsByStageUseCase,
    private val getDueWordsByTag: GetDueWordsByTagUseCase,
    private val getDailyGoalWords: GetDailyGoalWordsUseCase,
) : UseCase<ReviewSource, List<Word>> {

    override suspend fun invoke(params: ReviewSource): Try<List<Word>> = Try<List<Word>> {
        val limit = getDailyGoalWords().getOrDefault(Int.MAX_VALUE)
        when (params) {
            is ReviewSource.DueCards -> {
                getDueWords().first().take(limit)
            }

            is ReviewSource.ByStage ->
                getWordsByStage(params.stage).first()

            is ReviewSource.ByTag ->
                getDueWordsByTag(params.tagId).first().take(limit)

            is ReviewSource.ByStageAndTag ->
                getWordsByStage(params.stage).first()
                    .filter { params.tagId in it.tagIds }
        }
    }
}
