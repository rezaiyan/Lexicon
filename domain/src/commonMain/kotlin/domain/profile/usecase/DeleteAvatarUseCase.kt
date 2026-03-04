package domain.profile.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.profile.repository.IProfileRepository

class DeleteAvatarUseCase(
    private val profileRepository: IProfileRepository
) : NoParamUseCase<Unit> {
    suspend operator fun invoke(): Try<Unit> =
        profileRepository.deleteAvatar()

    override suspend operator fun invoke(params: Unit) = invoke()
}
