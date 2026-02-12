package presentation.feature.auth

import analytics.IAnalyticsTracker
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.streak.remote.StreakRemoteDataSource
import domain.auth.model.AuthState
import domain.auth.usecase.DeleteAccountUseCase
import domain.auth.usecase.IsAuthenticatedUseCase
import domain.auth.usecase.LoginWithAppleUseCase
import domain.auth.usecase.LoginWithGoogleUseCase
import domain.auth.usecase.LogoutUseCase
import domain.auth.usecase.VerifySessionUseCase
import domain.notifications.usecase.RegisterPushTokenUseCase
import domain.settings.repository.ISettingsRepository
import domain.word.usecase.SyncRemoteToLocalUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val loginWithAppleUseCase: LoginWithAppleUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val isAuthenticatedUseCase: IsAuthenticatedUseCase,
    private val verifySessionUseCase: VerifySessionUseCase,
    private val registerPushTokenUseCase: RegisterPushTokenUseCase,
    private val syncRemoteToLocalUseCase: SyncRemoteToLocalUseCase,
    private val streakRemoteDataSource: StreakRemoteDataSource,
    private val analyticsTracker: IAnalyticsTracker,
    private val settingsRepository: ISettingsRepository,
) : ViewModel() {

    private val intents = MutableSharedFlow<AuthIntent>(extraBufferCapacity = 64)

    private val _authState = MutableStateFlow(AuthState())
    val authState = _authState.asStateFlow()

    private val intentProcessor = AuthIntentProcessor(
        loginWithGoogleUseCase = loginWithGoogleUseCase,
        loginWithAppleUseCase = loginWithAppleUseCase,
        logoutUseCase = logoutUseCase,
        deleteAccountUseCase = deleteAccountUseCase,
        verifySessionUseCase = verifySessionUseCase,
        registerPushTokenUseCase = registerPushTokenUseCase,
        syncRemoteToLocalUseCase = syncRemoteToLocalUseCase,
        streakRemoteDataSource = streakRemoteDataSource,
        analyticsTracker = analyticsTracker,
        settingsRepository = settingsRepository,
        authState = _authState,
        onPushNotificationInit = { initializePushNotifications() }
    )

    init {
        startIntentProcessor()
        verifyAndRestoreSession { }
    }

    private fun startIntentProcessor() {
        viewModelScope.launch {
            intents.collect { intent ->
                intentProcessor.process(intent)
            }
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

    fun recordActivityForStreak() {
        viewModelScope.launch {
            intents.emit(AuthIntent.RecordStreak)
        }
    }

    private fun initializePushNotifications() {
        viewModelScope.launch {
            try {
                if (isAuthenticatedUseCase()) {
                    registerPushTokenUseCase.initializeAndRegister()
                }
            } catch (e: Exception) {
                analyticsTracker.logNonFatalError(
                    message = "Push notification initialization failed",
                    additionalInfo = mapOf("error" to (e.message ?: "unknown"))
                )
            }
        }
    }

}