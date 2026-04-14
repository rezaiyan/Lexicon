package domain.tag.usecase

import core.common.Try
import core.common.UseCase
import domain.tag.repository.ITagRepository

class SyncTagsFromRemoteUseCase(
    private val tagRepository: ITagRepository
) : UseCase<Unit, Unit> {
    override suspend operator fun invoke(params: Unit): Try<Unit> {
        return tagRepository.syncTagsFromRemote()
    }
}
