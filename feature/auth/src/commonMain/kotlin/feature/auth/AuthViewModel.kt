package feature.auth

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import domain.auth.manager.IUserManager
import domain.auth.model.AuthState
import domain.auth.model.AuthUser
import domain.auth.repository.SessionVerificationResult
import domain.auth.usecase.HandleLoginSuccessUseCase
import domain.auth.usecase.LoginWithAppleUseCase
import domain.auth.usecase.LoginWithGoogleUseCase
import domain.auth.usecase.ObserveAuthStateUseCase
import domain.auth.usecase.VerifySessionUseCase
import core.common.getOrElse
import core.common.onFailure
import core.error.toUserMessage
import domain.notifications.usecase.DeactivatePushTokenUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import core.base.BaseViewModel

class AuthViewModel(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val loginWithAppleUseCase: LoginWithAppleUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val verifySessionUseCase: VerifySessionUseCase,
    private val handleLoginSuccessUseCase: HandleLoginSuccessUseCase,
    private val deactivatePushTokenUseCase: DeactivatePushTokenUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val userManager: IUserManager,
) : BaseViewModel<AuthState, Nothing>() {

    private val authMutex = Mutex()

    override fun initialState() = AuthState()

    init {
        verifyAndRestoreSession { }
        observeAuthenticationState()
    }

    private fun observeAuthenticationState() {
        viewModelScope.launch {
            observeAuthStateUseCase(Unit).collect { isAuthenticated ->
                if (!isAuthenticated && currentState.isAuthenticated) {
                    analyticsTracker.logEvent(
                        "auto_logout",
                        mapOf("reason" to "auth_state_revoked", "source" to "auth_viewmodel")
                    )
                    deactivatePushTokenUseCase.deactivateCurrentToken()
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
                handleLoginSuccessUseCase(HandleLoginSuccessUseCase.Params(result.user, syncData = false))
                    .onFailure { error ->
                        analyticsTracker.logNonFatalError(
                            message = "Post-session-restore setup failed",
                            additionalInfo = mapOf("error" to (error.message ?: "unknown"))
                        )
                    }
                onComplete()
            }
            is SessionVerificationResult.Expired -> {
                analyticsTracker.logEvent(
                    "auto_logout",
                    mapOf("reason" to "session_verify_expired", "source" to "auth_viewmodel")
                )
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
            loginWithGoogleUseCase(idToken)
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
                    updateState { AuthState(isAuthenticated = false, isLoading = false, error = error.toUserMessage()) }
                }
                .collect { user -> onLoginSuccess(user, provider = "google") }
        }
    }

    private suspend fun processLoginWithApple(idToken: String, fullName: String?, appleUserId: String) {
        updateState { copy(isLoading = true) }
        authMutex.withLock {
            loginWithAppleUseCase(LoginWithAppleUseCase.Params(idToken, fullName, appleUserId))
                .catch { error ->
                    analyticsTracker.logEvent("login_failed", mapOf("provider" to "apple"))
                    updateState { AuthState(isAuthenticated = false, isLoading = false, error = error.toUserMessage()) }
                }
                .collect { user -> onLoginSuccess(user, provider = "apple") }
        }
    }

    private suspend fun onLoginSuccess(user: AuthUser, provider: String) {
        analyticsTracker.logEvent(
            "login_success",
            mapOf("user_id" to user.id.toString(), "provider" to provider)
        )
        updateState {
            AuthState(isAuthenticated = true, isLoading = true, user = user, error = null)
        }
        userManager.setUser(user)
        handleLoginSuccessUseCase(HandleLoginSuccessUseCase.Params(user, syncData = true))
            .onFailure { error ->
                analyticsTracker.logNonFatalError(
                    message = "Post-login setup failed",
                    additionalInfo = mapOf("provider" to provider, "error" to (error.message ?: "unknown"))
                )
            }
        updateState { copy(isLoading = false) }
    }

    private suspend fun processLogout() {
        analyticsTracker.logEvent("logout")
        userManager.logout()
        updateState { AuthState(isAuthenticated = false, isLoading = false) }
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

}
