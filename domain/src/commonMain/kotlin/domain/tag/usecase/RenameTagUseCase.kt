package domain.tag.usecase

import core.common.Try
import core.common.UseCase
import domain.tag.model.Tag
import domain.tag.repository.ITagRepository

data class RenameTagParams(val id: Long, val name: String)

class RenameTagUseCase(
    private val tagRepository: ITagRepository
) : UseCase<RenameTagParams, Tag> {
    override suspend fun invoke(params: RenameTagParams): Try<Tag> =
        tagRepository.renameTag(params.id, params.name)
}
