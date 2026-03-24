package domain.tag.usecase

import core.common.Try
import core.common.UseCase
import domain.tag.repository.ITagRepository

data class AssignWordTagsParams(val wordId: Long, val tagIds: List<Long>)

class AssignWordTagsUseCase(
    private val tagRepository: ITagRepository
) : UseCase<AssignWordTagsParams, Unit> {
    override suspend fun invoke(params: AssignWordTagsParams): Try<Unit> =
        tagRepository.assignWordTags(params.wordId, params.tagIds)
}
