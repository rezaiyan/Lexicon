package domain.profile.usecase

import domain.auth.model.AuthUser
import core.common.Try
import core.common.UseCase
import domain.profile.repository.IProfileRepository

class UpdateProfileUseCase(
    private val profileRepository: IProfileRepository
) : UseCase<UpdateProfileUseCase.Params, AuthUser> {

    data class Params(val name: String?, val displayAlias: String?)

    suspend operator fun invoke(name: String?, displayAlias: String?): Try<AuthUser> =
        profileRepository.updateProfile(name, displayAlias)

    override suspend operator fun invoke(params: Params) = invoke(params.name, params.displayAlias)
}
