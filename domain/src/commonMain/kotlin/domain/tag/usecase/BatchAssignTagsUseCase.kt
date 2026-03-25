package domain.tag.usecase

import core.common.Try
import core.common.UseCase
import core.common.getOrThrow
import domain.tag.repository.ITagRepository

data class BatchAssignTagsParams(val wordIds: List<Int>, val tagIds: List<Long>)

class BatchAssignTagsUseCase(
    private val tagRepository: ITagRepository
) : UseCase<BatchAssignTagsParams, Int> {
    override suspend fun invoke(params: BatchAssignTagsParams): Try<Int> = Try {
        tagRepository.batchAssignWordTags(
            wordIds = params.wordIds.map { it.toLong() },
            tagIds = params.tagIds
        ).getOrThrow()
        params.wordIds.size
    }
}
