package fakes

import core.common.Try
import domain.auth.model.AuthUser
import domain.profile.repository.IProfileRepository

class FakeProfileRepository : IProfileRepository {
    var updateResult: Try<AuthUser> = Try.success(AuthUser(1L, "test@test.com", "Test"))
    var uploadResult: Try<String> = Try.success("https://avatar.url")
    var deleteAvatarResult: Try<Unit> = Try.success(Unit)
    var lastUpdateName: String? = null
    var lastUpdateAlias: String? = null

    override suspend fun updateProfile(name: String?, displayAlias: String?): Try<AuthUser> {
        lastUpdateName = name
        lastUpdateAlias = displayAlias
        return updateResult
    }

    override suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Try<String> = uploadResult
    override suspend fun deleteAvatar(): Try<Unit> = deleteAvatarResult
}
