package presentation.feature.auth

import analytics.IAnalyticsTracker
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import core.base.BaseViewModel

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
) : BaseViewModel<AuthState, Nothing>() {

    private val authMutex = Mutex()

    override fun initialState() = AuthState()

    init {
        verifyAndRestoreSession { }
        observeAuthenticationState()
    }

    private fun observeAuthenticationState() {
        viewModelScope.launch {
            isAuthenticatedUseCase.asFlow().collect { isAuthenticated ->
                if (!isAuthenticated && currentState.isAuthenticated) {
                    userManager.setUser(null)
                    updateState {
                        AuthState(
                            isAuthenticated = false,
                            isLoading = false,
                            user = null,
                            error = null
                        )
                    }
                }
            }
        }
    }

    private suspend fun processVerifyAndRestore(onComplete: () -> Unit) {
        updateState { copy(isLoading = true) }
        val result = verifySessionUseCase().getOrElse { _ ->
            updateState { copy(isAuthenticated = true, isLoading = false) }
            onComplete()
            return
        }
        when (result) {
            is SessionVerificationResult.Valid -> {
                updateState {
                    AuthState(
                        isAuthenticated = true,
                        isLoading = false,
                        user = result.user,
                        error = null
                    )
                }
                userManager.setUser(result.user)
                subscriptionManager.logIn(result.user.id.toString())
                initializePushNotifications()
                onComplete()
            }
            is SessionVerificationResult.Expired -> {
                updateState { copy(isLoading = false) }
                onComplete()
            }
            is SessionVerificationResult.NotAuthenticated -> {
                updateState { AuthState(isAuthenticated = false, isLoading = false) }
                onComplete()
            }
            is SessionVerificationResult.ServerError -> {
                updateState { copy(isAuthenticated = true, isLoading = false) }
                onComplete()
            }
        }
    }

    private suspend fun processLogin(idToken: String) {
        analyticsTracker.logEvent(
            "login_google_token_received",
            mapOf("token_length" to idToken.length.toString())
        )
        updateState { copy(isLoading = true) }
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
                    updateState { AuthState(isAuthenticated = false, isLoading = false, error = error.message) }
                }
                .collect { user ->
                    analyticsTracker.logEvent(
                        "login_success",
                        mapOf("user_id" to user.id.toString(), "provider" to "google")
                    )
                    updateState {
                        AuthState(
                            isAuthenticated = true,
                            isLoading = true,
                            user = user,
                            error = null
                        )
                    }
                    userManager.setUser(user)
                    subscriptionManager.logIn(user.id.toString())
                    syncRemoteToLocalUseCase(clearFirst = false)
                    initializePushNotifications()
                    updateState { copy(isLoading = false) }
                }
        }
    }

    private suspend fun processLoginWithApple(idToken: String, fullName: String?, appleUserId: String) {
        authMutex.withLock {
            loginWithAppleUseCase.invoke(idToken, fullName, appleUserId)
                .catch { error ->
                    analyticsTracker.logEvent("login_failed", mapOf("provider" to "apple"))
                    updateState { AuthState(isAuthenticated = false, isLoading = false, error = error.message) }
                }
                .collect { user ->
                    analyticsTracker.logEvent(
                        "login_success",
                        mapOf("user_id" to user.id.toString(), "provider" to "apple")
                    )
                    updateState {
                        AuthState(
                            isAuthenticated = true,
                            isLoading = true,
                            user = user,
                            error = null
                        )
                    }
                    userManager.setUser(user)
                    subscriptionManager.logIn(user.id.toString())
                    syncRemoteToLocalUseCase(clearFirst = false)
                    initializePushNotifications()
                    updateState { copy(isLoading = false) }
                }
        }
    }

    private suspend fun processLogout() {
        analyticsTracker.logEvent("logout")
        registerPushTokenUseCase.deactivateAllTokens()
        userManager.setUser(null)
        logoutUseCase.invoke()
            .catch { _ ->
                updateState { AuthState(isAuthenticated = false, isLoading = false) }
            }
            .collect { _ ->
                updateState { AuthState(isAuthenticated = false, isLoading = false) }
            }
    }

    fun verifyAndRestoreSession(onComplete: () -> Unit) {
        viewModelScope.launch { processVerifyAndRestore(onComplete) }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch { processLogin(idToken) }
    }

    fun loginWithApple(idToken: String, fullName: String?, appleUserId: String) {
        viewModelScope.launch { processLoginWithApple(idToken, fullName, appleUserId) }
    }

    fun logout() {
        viewModelScope.launch { processLogout() }
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
