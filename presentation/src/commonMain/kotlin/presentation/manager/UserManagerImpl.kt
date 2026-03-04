package presentation.manager

import domain.auth.manager.IUserManager
import domain.auth.model.AuthUser
import domain.auth.usecase.DeleteAccountUseCase
import domain.auth.usecase.LogoutUseCase
import core.common.Try
import core.common.onSuccess
import domain.notifications.usecase.RegisterPushTokenUseCase
import domain.streak.manager.IStreakManager
import domain.subscription.ISubscriptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class UserManagerImpl(
    private val logoutUseCase: LogoutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val subscriptionManager: ISubscriptionManager,
    private val streakManager: IStreakManager,
    private val registerPushTokenUseCase: RegisterPushTokenUseCase,
) : IUserManager {

    private val _currentUser = MutableStateFlow<AuthUser?>(null)

    override fun observeUser(): Flow<AuthUser?> = _currentUser.asStateFlow()

    override fun setUser(user: AuthUser?) {
        _currentUser.value = user
    }

    override suspend fun logout(): Try<Unit> {
        registerPushTokenUseCase.deactivateAllTokens()
        val logoutResult = logoutUseCase.invoke().first()
        return logoutResult.onSuccess {
            _currentUser.value = null
            subscriptionManager.logOut()
            streakManager.clearCache()
        }
    }

    override suspend fun deleteAccount(): Try<Unit> {
        registerPushTokenUseCase.deactivateAllTokens()
        val result = deleteAccountUseCase.invoke().first()
        return result.onSuccess {
            _currentUser.value = null
            subscriptionManager.logOut()
            streakManager.clearCache()
        }
    }
}
