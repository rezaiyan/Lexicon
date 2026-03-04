package domain.profile.usecase

import core.common.Try
import domain.profile.repository.IProfileRepository

class DeleteAvatarUseCase(
    private val profileRepository: IProfileRepository
) {
    suspend operator fun invoke(): Try<Unit> =
        profileRepository.deleteAvatar()
}
