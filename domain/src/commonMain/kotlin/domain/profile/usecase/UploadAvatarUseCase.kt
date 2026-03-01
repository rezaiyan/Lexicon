package domain.profile.usecase

import domain.common.Try
import domain.profile.repository.IProfileRepository

class UploadAvatarUseCase(
    private val profileRepository: IProfileRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray, mimeType: String): Try<String> =
        profileRepository.uploadAvatar(imageBytes, mimeType)
}
