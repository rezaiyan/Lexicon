package fakes

import core.common.Try
import domain.auth.manager.IUserManager
import domain.auth.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserManager(user: AuthUser? = null) : IUserManager {
    val userFlow = MutableStateFlow(user)

    override fun observeUser(): Flow<AuthUser?> = userFlow
    override fun setUser(user: AuthUser?) { userFlow.value = user }
    override suspend fun logout(): Try<Unit> = Try.success(Unit)
    override suspend fun deleteAccount(): Try<Unit> = Try.success(Unit)
}
