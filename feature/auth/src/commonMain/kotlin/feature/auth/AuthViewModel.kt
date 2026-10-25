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
import domain.passkey.usecase.IsPasskeyAvailableUseCase
import domain.passkey.usecase.SignInWithPasskeyUseCase
import core.common.fold
import core.common.getOrElse
import core.error.DomainError
import core.common.onFailure
import core.error.toUserMessage
import domain.notifications.usecase.DeactivatePushTokenUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import core.base.BaseViewModel

@Suppress(
    "LongParameterList", // one use case per sign-in method plus session plumbing
    "TooManyFunctions",  // each public method is a distinct event-sink entry point required by the screen
)
class AuthViewModel(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val loginWithAppleUseCase: LoginWithAppleUseCase,
    private val signInWithPasskeyUseCase: SignInWithPasskeyUseCase,
    private val isPasskeyAvailableUseCase: IsPasskeyAvailableUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val verifySessionUseCase: VerifySessionUseCase,
    private val handleLoginSuccessUseCase: HandleLoginSuccessUseCase,
    private val deactivatePushTokenUseCase: DeactivatePushTokenUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val userManager: IUserManager,
) : BaseViewModel<AuthState, AuthEffect>() {

    private val authMutex = Mutex()

    override fun initialState() = AuthState()

    init {
        verifyAndRestoreSession { }
        observeAuthenticationState()
        checkPasskeySupport()
    }

    private fun checkPasskeySupport() {
        viewModelScope.launch {
            val available = isPasskeyAvailableUseCase(Unit).getOrElse { false }
            updateState { copy(passkeySignInAvailable = available) }
        }
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
                    updateState { signedOut() }
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
                        passkeySignInAvailable = currentState.passkeySignInAvailable,
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
                updateState { signedOut() }
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
                    updateState { signedOut(error.toUserMessage()) }
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
                    updateState { signedOut(error.toUserMessage()) }
                }
                .collect { user -> onLoginSuccess(user, provider = "apple") }
        }
    }

    private suspend fun processLoginWithPasskey() {
        updateState { copy(isLoading = true, error = null) }
        authMutex.withLock {
            signInWithPasskeyUseCase(Unit).fold(
                onSuccess = { user -> onLoginSuccess(user, provider = "passkey") },
                onFailure = { error ->
                    // No passkey here, or one deleted from the account: hints, shown as a neutral notice
                    val notice = when (error) {
                        DomainError.Passkey.NoPasskeys -> AuthEffect.NoPasskeyOnDevice
                        DomainError.Passkey.Removed -> AuthEffect.PasskeyRemoved
                        else -> null
                    }
                    if (error is DomainError.Passkey.Cancelled || notice != null) {
                        updateState { copy(isLoading = false) }
                        notice?.let { emitEffect(it) }
                    } else {
                        analyticsTracker.logEvent(
                            "login_failed",
                            mapOf("provider" to "passkey", "error_type" to (error::class.simpleName ?: "unknown")),
                        )
                        updateState { copy(isAuthenticated = false, isLoading = false, error = error.toUserMessage()) }
                    }
                },
            )
        }
    }

    private suspend fun onLoginSuccess(user: AuthUser, provider: String) {
        analyticsTracker.logEvent(
            "login_success",
            mapOf("user_id" to user.id.toString(), "provider" to provider)
        )
        updateState {
            AuthState(
                isAuthenticated = true,
                isLoading = true,
                user = user,
                error = null,
                passkeySignInAvailable = currentState.passkeySignInAvailable,
            )
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
        updateState { signedOut() }
    }

    /** Signed out, keeping what the device supports (the sign-in screen shows it). */
    private fun signedOut(error: String? = null) = AuthState(
        isAuthenticated = false,
        isLoading = false,
        error = error,
        passkeySignInAvailable = currentState.passkeySignInAvailable,
    )

    fun verifyAndRestoreSession(onComplete: () -> Unit) {
        viewModelScope.launch { processVerifyAndRestore(onComplete) }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch { processLogin(idToken) }
    }

    fun loginWithApple(idToken: String, fullName: String?, appleUserId: String) {
        viewModelScope.launch { processLoginWithApple(idToken, fullName, appleUserId) }
    }

    fun loginWithPasskey() {
        viewModelScope.launch { processLoginWithPasskey() }
    }

    fun logout() {
        viewModelScope.launch { processLogout() }
    }

}
