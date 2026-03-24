package domain.tag.usecase

import core.common.Try
import core.common.UseCase
import domain.tag.repository.ITagRepository

class DeleteTagUseCase(
    private val tagRepository: ITagRepository
) : UseCase<Long, Unit> {
    override suspend fun invoke(params: Long): Try<Unit> =
        tagRepository.deleteTag(params)
}
