package domain.profile.repository

import domain.auth.model.AuthUser
import core.common.Try

interface IProfileRepository {
    suspend fun updateProfile(name: String?, displayAlias: String?): Try<AuthUser>
}
