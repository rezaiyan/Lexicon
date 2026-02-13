package presentation.manager

import domain.auth.manager.IUserManager
import domain.auth.model.AuthUser
import domain.auth.repository.IAuthRepository
import domain.auth.usecase.DeleteAccountUseCase
import domain.auth.usecase.LoginWithAppleUseCase
import domain.auth.usecase.LoginWithGoogleUseCase
import domain.auth.usecase.LogoutUseCase
import domain.common.Try
import domain.common.fold
import domain.common.onSuccess
import domain.subscription.ISubscriptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class UserManagerImpl(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val loginWithAppleUseCase: LoginWithAppleUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val authRepository: IAuthRepository,
    private val subscriptionManager: ISubscriptionManager,
) : IUserManager {

    private val _currentUser = MutableStateFlow<AuthUser?>(null)

    override fun isLogin(): Boolean {
        return false // Unused — callers should use suspend isAuthenticated() instead
    }

    override fun observeUser(): Flow<AuthUser?> {
        return flow {
            val token = authRepository.getAccessToken()
            if (token != null) {
                authRepository.getUserProfile().fold(
                    onSuccess = { user ->
                        _currentUser.value = user
                        emit(user)
                    },
                    onFailure = {
                        _currentUser.value = null
                        emit(null)
                    }
                )
            } else {
                _currentUser.value = null
                emit(null)
            }
        }
            .flatMapLatest { user ->
                if (user != null) {
                    _currentUser.asStateFlow()
                } else {
                    flow { emit(null) }
                }
            }
            .catch { emit(null) }
    }

    override suspend fun loginWithGoogle(idToken: String): Try<AuthUser> {
        val authResult = loginWithGoogleUseCase.invoke(idToken).first()
        return authResult.onSuccess { user ->
            _currentUser.value = user
            subscriptionManager.logIn(user.id.toString())
        }
    }

    override suspend fun loginWithApple(
        idToken: String,
        fullName: String?,
        appleUserId: String
    ): Try<AuthUser> {
        val authResult = loginWithAppleUseCase.invoke(idToken, fullName, appleUserId).first()
        return authResult.onSuccess { user ->
            _currentUser.value = user
            subscriptionManager.logIn(user.id.toString())
        }
    }

    override suspend fun logout(): Try<Unit> {
        val logoutResult = logoutUseCase.invoke().first()
        return logoutResult.onSuccess {
            subscriptionManager.logOut()
        }
    }

    override suspend fun deleteAccount(): Try<Unit> {
        return deleteAccountUseCase.invoke().first()
    }
}
