package domain.profile.usecase

import core.common.Try
import core.common.UseCase
import domain.profile.repository.IProfileRepository

class UploadAvatarUseCase(
    private val profileRepository: IProfileRepository
) : UseCase<UploadAvatarUseCase.Params, String> {

    data class Params(val imageBytes: ByteArray, val mimeType: String)

    suspend operator fun invoke(imageBytes: ByteArray, mimeType: String): Try<String> =
        profileRepository.uploadAvatar(imageBytes, mimeType)

    override suspend operator fun invoke(params: Params) = invoke(params.imageBytes, params.mimeType)
}
