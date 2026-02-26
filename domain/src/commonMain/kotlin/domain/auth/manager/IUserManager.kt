package domain.auth.manager

import domain.auth.model.AuthUser
import domain.common.Try
import kotlinx.coroutines.flow.Flow

interface IUserManager {
    fun observeUser(): Flow<AuthUser?>
    fun setUser(user: AuthUser?)
    suspend fun logout(): Try<Unit>
    suspend fun deleteAccount(): Try<Unit>
}
