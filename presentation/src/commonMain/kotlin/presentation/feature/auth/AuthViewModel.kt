package presentation.feature.auth

import analytics.IAnalyticsTracker
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.auth.manager.IUserManager
import domain.auth.model.AuthState
import domain.auth.repository.SessionVerificationResult
import domain.auth.usecase.IsAuthenticatedUseCase
import domain.auth.usecase.LoginWithAppleUseCase
import domain.auth.usecase.LoginWithGoogleUseCase
import domain.auth.usecase.LogoutUseCase
import domain.auth.usecase.VerifySessionUseCase
import core.common.getOrElse
import core.common.onFailure
import domain.notifications.usecase.InitializePushNotificationsUseCase
import domain.notifications.usecase.RegisterPushTokenUseCase
import domain.subscription.ISubscriptionManager
import domain.word.usecase.SyncRemoteToLocalUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class AuthIntent {
    data class VerifyAndRestore(val onComplete: () -> Unit) : AuthIntent()
    data class LoginWithIdToken(val idToken: String) : AuthIntent()
    data class LoginWithApple(val idToken: String, val fullName: String?, val appleUserId: String) : AuthIntent()
    data object Logout : AuthIntent()
}

class AuthViewModel(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val loginWithAppleUseCase: LoginWithAppleUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val isAuthenticatedUseCase: IsAuthenticatedUseCase,
    private val verifySessionUseCase: VerifySessionUseCase,
    private val syncRemoteToLocalUseCase: SyncRemoteToLocalUseCase,
    private val initializePushNotificationsUseCase: InitializePushNotificationsUseCase,
    private val registerPushTokenUseCase: RegisterPushTokenUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val userManager: IUserManager,
    private val subscriptionManager: ISubscriptionManager,
) : ViewModel() {

    private val intents = MutableSharedFlow<AuthIntent>(extraBufferCapacity = 64)
    private val authMutex = Mutex()

    private val _authState = MutableStateFlow(AuthState())
    val authState = _authState.asStateFlow()

    init {
        startIntentProcessor()
        verifyAndRestoreSession { }
        observeAuthenticationState()
    }

    private fun observeAuthenticationState() {
        viewModelScope.launch {
            isAuthenticatedUseCase.asFlow().collect { isAuthenticated ->
                if (!isAuthenticated && _authState.value.isAuthenticated) {
                    // User has been logged out (either manually or automatically)
                    userManager.setUser(null)
                    _authState.value = AuthState(
                        isAuthenticated = false,
                        isLoading = false,
                        user = null,
                        error = null
                    )
                }
            }
        }
    }

    private fun startIntentProcessor() {
        viewModelScope.launch {
            intents.collect { intent ->
                processIntent(intent)
            }
        }
    }

    private suspend fun processIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.VerifyAndRestore -> processVerifyAndRestore(intent.onComplete)
            is AuthIntent.LoginWithIdToken -> processLogin(intent.idToken)
            is AuthIntent.LoginWithApple -> processLoginWithApple(intent.idToken, intent.fullName, intent.appleUserId)
            AuthIntent.Logout -> processLogout()
        }
    }

    private suspend fun processVerifyAndRestore(onComplete: () -> Unit) {
        _authState.value = _authState.value.copy(isLoading = true)
        val result = verifySessionUseCase().getOrElse { error ->
            // Treat verification failure like a server error — keep user authenticated
            _authState.value = _authState.value.copy(
                isAuthenticated = true,
                isLoading = false
            )
            onComplete()
            return
        }
        when (result) {
            is SessionVerificationResult.Valid -> {
                _authState.value = AuthState(
                    isAuthenticated = true,
                    isLoading = false,
                    user = result.user,
                    error = null
                )
                userManager.setUser(result.user)
                subscriptionManager.logIn(result.user.id.toString())
                initializePushNotifications()
                onComplete()
            }
            is SessionVerificationResult.Expired -> {
                _authState.value = _authState.value.copy(isLoading = false)
                onComplete()
            }
            is SessionVerificationResult.NotAuthenticated -> {
                _authState.value = AuthState(isAuthenticated = false, isLoading = false)
                onComplete()
            }
            is SessionVerificationResult.ServerError -> {
                _authState.value = _authState.value.copy(
                    isAuthenticated = true,
                    isLoading = false
                )
                onComplete()
            }
        }
    }

    private suspend fun processLogin(idToken: String) {
        analyticsTracker.logEvent(
            "login_google_token_received",
            mapOf("token_length" to idToken.length.toString())
        )
        _authState.value = _authState.value.copy(isLoading = true)
        authMutex.withLock {
            loginWithGoogleUseCase.invoke(idToken)
                .catch { error ->
                    analyticsTracker.logEvent(
                        "login_failed",
                        mapOf(
                            "provider" to "google",
                            "stage" to "backend_error",
                            "error_type" to (error::class.simpleName ?: "unknown"),
                            "error_message" to (error.message ?: "no_message")
                        )
                    )
                    analyticsTracker.logError(error, "google_login_backend_error")
                    _authState.value = AuthState(isAuthenticated = false, isLoading = false, error = error.message)
                }
                .collect { user ->
                    analyticsTracker.logEvent(
                        "login_success",
                        mapOf("user_id" to user.id.toString(), "provider" to "google")
                    )
                    _authState.value = AuthState(
                        isAuthenticated = true,
                        isLoading = true,
                        user = user,
                        error = null
                    )
                    userManager.setUser(user)
                    subscriptionManager.logIn(user.id.toString())
                    syncRemoteToLocalUseCase(clearFirst = false)
                    initializePushNotifications()
                    _authState.value = _authState.value.copy(isLoading = false)
                }
        }
    }

    private suspend fun processLoginWithApple(idToken: String, fullName: String?, appleUserId: String) {
        authMutex.withLock {
            loginWithAppleUseCase.invoke(idToken, fullName, appleUserId)
                .catch { error ->
                    analyticsTracker.logEvent("login_failed", mapOf("provider" to "apple"))
                    _authState.value = AuthState(isAuthenticated = false, isLoading = false, error = error.message)
                }
                .collect { user ->
                    analyticsTracker.logEvent(
                        "login_success",
                        mapOf("user_id" to user.id.toString(), "provider" to "apple")
                    )
                    _authState.value = AuthState(
                        isAuthenticated = true,
                        isLoading = true,
                        user = user,
                        error = null
                    )
                    userManager.setUser(user)
                    subscriptionManager.logIn(user.id.toString())
                    syncRemoteToLocalUseCase(clearFirst = false)
                    initializePushNotifications()
                    _authState.value = _authState.value.copy(isLoading = false)
                }
        }
    }

    private suspend fun processLogout() {
        analyticsTracker.logEvent("logout")
        registerPushTokenUseCase.deactivateAllTokens()
        userManager.setUser(null)
        logoutUseCase.invoke()
            .catch { _ ->
                _authState.value = AuthState(isAuthenticated = false, isLoading = false)
            }
            .collect { _ ->
                _authState.value = AuthState(isAuthenticated = false, isLoading = false)
            }
    }

    fun verifyAndRestoreSession(onComplete: () -> Unit) {
        intents.tryEmit(AuthIntent.VerifyAndRestore(onComplete))
    }

    fun loginWithGoogle(idToken: String) {
        intents.tryEmit(AuthIntent.LoginWithIdToken(idToken))
    }

    fun loginWithApple(idToken: String, fullName: String?, appleUserId: String) {
        intents.tryEmit(AuthIntent.LoginWithApple(idToken, fullName, appleUserId))
    }

    fun logout() {
        intents.tryEmit(AuthIntent.Logout)
    }

    private fun initializePushNotifications() {
        viewModelScope.launch {
            initializePushNotificationsUseCase().onFailure { error ->
                analyticsTracker.logNonFatalError(
                    message = "Push notification initialization failed",
                    additionalInfo = mapOf("error" to (error.message ?: "unknown"))
                )
            }
        }
    }

}
