package presentation.feature.auth

import analytics.IAnalyticsTracker
import domain.streak.repository.IStreakRepository
import domain.auth.model.AuthState
import domain.auth.repository.SessionVerificationResult
import domain.auth.usecase.DeleteAccountUseCase
import domain.auth.usecase.LoginWithAppleUseCase
import domain.auth.usecase.LoginWithGoogleUseCase
import domain.auth.usecase.LogoutUseCase
import domain.auth.usecase.VerifySessionUseCase
import domain.notifications.usecase.RegisterPushTokenUseCase
import domain.settings.repository.ISettingsRepository
import domain.word.usecase.SyncRemoteToLocalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class AuthIntent {
    data class VerifyAndRestore(val onComplete: () -> Unit) : AuthIntent()
    data class LoginWithIdToken(val idToken: String) : AuthIntent()
    data class LoginWithApple(val idToken: String, val fullName: String?, val appleUserId: String) : AuthIntent()
    data object Logout : AuthIntent()
    data object DeleteAccount : AuthIntent()
    data object RecordStreak : AuthIntent()
}

class AuthIntentProcessor(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val loginWithAppleUseCase: LoginWithAppleUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val verifySessionUseCase: VerifySessionUseCase,
    private val registerPushTokenUseCase: RegisterPushTokenUseCase,
    private val syncRemoteToLocalUseCase: SyncRemoteToLocalUseCase,
    private val streakRepository: IStreakRepository,
    private val analyticsTracker: IAnalyticsTracker,
    private val settingsRepository: ISettingsRepository,
    private val authState: MutableStateFlow<AuthState>,
    private val onPushNotificationInit: () -> Unit
) {
    
    private val authMutex = Mutex()
    
    suspend fun process(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.VerifyAndRestore -> processVerifyAndRestore(intent.onComplete)
            is AuthIntent.LoginWithIdToken -> processLogin(intent.idToken)
            is AuthIntent.LoginWithApple -> processLoginWithApple(intent.idToken, intent.fullName, intent.appleUserId)
            AuthIntent.Logout -> processLogout()
            AuthIntent.DeleteAccount -> processDeleteAccount()
            AuthIntent.RecordStreak -> processRecordStreak()
        }
    }
    
    private suspend fun processVerifyAndRestore(onComplete: () -> Unit) {
        authState.value = authState.value.copy(isLoading = true)
        when (val result = verifySessionUseCase()) {
            is SessionVerificationResult.Valid -> {
                val newState = AuthState(
                    isAuthenticated = true,
                    isLoading = false,
                    user = result.user,
                    error = null
                )
                authState.value = newState
                onPushNotificationInit()
                onComplete()
            }
            is SessionVerificationResult.Expired -> {
                authState.value = authState.value.copy(isLoading = false)
                onComplete()
            }
            is SessionVerificationResult.NotAuthenticated -> {
                authState.value = AuthState(isAuthenticated = false, isLoading = false)
                onComplete()
            }
            is SessionVerificationResult.ServerError -> {
                authState.value = authState.value.copy(isLoading = false)
                onComplete()
            }
        }
    }
    
    private suspend fun processLogin(idToken: String) {
        authMutex.withLock {
            loginWithGoogleUseCase.invoke(idToken)
                .catch { error ->
                    analyticsTracker.logEvent("login_failed", mapOf("provider" to "google"))
                    val newState = AuthState(isAuthenticated = false, isLoading = false, error = error.message)
                    authState.value = newState
                }
                .collect { result ->
                    when (result) {
                        is LoginWithGoogleUseCase.AuthResult.Success -> {
                            val user = result.user
                            analyticsTracker.logEvent(
                                "login_success",
                                mapOf("user_id" to user.id.toString(), "provider" to "google")
                            )
                            
                            val loadingState = AuthState(
                                isAuthenticated = true,
                                isLoading = true,
                                user = user,
                                error = null
                            )
                            authState.value = loadingState
                            
                            syncRemoteToLocalUseCase(clearFirst = false)
                            onPushNotificationInit()
                            val finalState = authState.value.copy(isLoading = false)
                            authState.value = finalState
                        }
                        is LoginWithGoogleUseCase.AuthResult.Error -> {
                            analyticsTracker.logEvent("login_failed", mapOf("provider" to "google"))
                            val newState = AuthState(isAuthenticated = false, isLoading = false, error = result.message)
                            authState.value = newState
                        }
                    }
                }
        }
    }
    
    private suspend fun processLoginWithApple(idToken: String, fullName: String?, appleUserId: String) {
        authMutex.withLock {
            loginWithAppleUseCase.invoke(idToken, fullName, appleUserId)
                .catch { error ->
                    analyticsTracker.logEvent("login_failed", mapOf("provider" to "apple"))
                    val newState = AuthState(isAuthenticated = false, isLoading = false, error = error.message)
                    authState.value = newState
                }
                .collect { result ->
                    when (result) {
                        is LoginWithAppleUseCase.AuthResult.Success -> {
                            val user = result.user
                            analyticsTracker.logEvent(
                                "login_success",
                                mapOf("user_id" to user.id.toString(), "provider" to "apple")
                            )
                            
                            val loadingState = AuthState(
                                isAuthenticated = true,
                                isLoading = true,
                                user = user,
                                error = null
                            )
                            authState.value = loadingState
                            
                            syncRemoteToLocalUseCase(clearFirst = false)
                            onPushNotificationInit()
                            val finalState = authState.value.copy(isLoading = false)
                            authState.value = finalState
                        }
                        is LoginWithAppleUseCase.AuthResult.Error -> {
                            analyticsTracker.logEvent("login_failed", mapOf("provider" to "apple"))
                            val newState = AuthState(isAuthenticated = false, isLoading = false, error = result.message)
                            authState.value = newState
                        }
                    }
                }
        }
    }
    
    private suspend fun processLogout() {
        analyticsTracker.logEvent("logout")
        
        registerPushTokenUseCase.deactivateAllTokens()
        logoutUseCase.invoke()
            .catch { error ->
                val newState = AuthState(isAuthenticated = false, isLoading = true)
                authState.value = newState
                settingsRepository.clearInsightData()
                
                val finalState = authState.value.copy(isLoading = false)
                authState.value = finalState
            }
            .collect { result ->
                when (result) {
                    is LogoutUseCase.LogoutResult.Success -> {
                        val loadingState = AuthState(isAuthenticated = false, isLoading = true)
                        authState.value = loadingState
                        settingsRepository.clearInsightData()
                        
                        val finalState = authState.value.copy(isLoading = false)
                        authState.value = finalState
                    }
                    is LogoutUseCase.LogoutResult.Error -> {
                        val loadingState = AuthState(isAuthenticated = false, isLoading = true)
                        authState.value = loadingState
                        settingsRepository.clearInsightData()
                        val finalState = authState.value.copy(isLoading = false)
                        authState.value = finalState
                    }
                }
            }
    }
    
    private suspend fun processDeleteAccount() {
        analyticsTracker.logEvent("account_deleted")

        registerPushTokenUseCase.deactivateAllTokens()
        deleteAccountUseCase.invoke()
            .catch { error ->
                val newState = authState.value.copy(
                    isLoading = false,
                    error = "Failed to delete account: ${error.message}"
                )
                authState.value = newState
            }
            .collect { result ->
                when (result) {
                    is DeleteAccountUseCase.DeleteAccountResult.Success -> {
                        val loadingState = AuthState(isAuthenticated = false, isLoading = true)
                        authState.value = loadingState
                        settingsRepository.clearInsightData()

                        val finalState = authState.value.copy(isLoading = false)
                        authState.value = finalState
                    }
                    is DeleteAccountUseCase.DeleteAccountResult.Error -> {
                        val newState = authState.value.copy(
                            isLoading = false,
                            error = "Failed to delete account: ${result.message}"
                        )
                        authState.value = newState
                    }
                }
            }
    }

    private suspend fun processRecordStreak() {
        streakRepository.recordActivity().fold(
            onSuccess = { streakData ->
                val currentUser = authState.value.user
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(
                        currentStreak = streakData.currentStreak,
                        longestStreak = streakData.highestStreak
                    )
                    val newState = authState.value.copy(user = updatedUser)
                    authState.value = newState
                }
            },
            onFailure = { }
        )

    }
}
