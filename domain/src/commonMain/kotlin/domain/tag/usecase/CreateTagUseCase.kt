package domain.tag.usecase

import core.common.Try
import core.common.UseCase
import core.error.DomainError
import domain.tag.model.Tag
import domain.tag.repository.ITagRepository

class CreateTagUseCase(
    private val tagRepository: ITagRepository
) : UseCase<String, Tag> {
    override suspend fun invoke(params: String): Try<Tag> {
        val trimmed = params.trim()
        if (trimmed.isBlank()) {
            return Try.failure(DomainError.Validation.BlankField("Tag name"))
        }
        return tagRepository.createTag(trimmed)
    }
}
