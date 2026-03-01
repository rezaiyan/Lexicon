package domain.profile.usecase

import domain.auth.model.AuthUser
import domain.common.Try
import domain.profile.repository.IProfileRepository

class UpdateProfileUseCase(
    private val profileRepository: IProfileRepository
) {
    suspend operator fun invoke(name: String?, displayAlias: String?): Try<AuthUser> =
        profileRepository.updateProfile(name, displayAlias)
}
