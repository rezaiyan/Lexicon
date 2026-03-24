package domain.tag.usecase

import core.common.Try
import core.common.UseCase
import domain.tag.model.Tag
import domain.tag.repository.ITagRepository

class CreateTagUseCase(
    private val tagRepository: ITagRepository
) : UseCase<String, Tag> {
    override suspend fun invoke(params: String): Try<Tag> =
        tagRepository.createTag(params)
}
