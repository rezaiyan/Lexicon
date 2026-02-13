@file:OptIn(ExperimentalCoroutinesApi::class)

package presentation.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.auth.manager.IUserManager
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.streak.manager.IStreakManager
import domain.streak.model.StreakData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import presentation.model.ProfileUiData
import presentation.model.UiState
import presentation.util.stateInEagerly
import presentation.util.stateInWhileSubscribed

class ProfileViewModel(
    private val userManager: IUserManager,
    getFeatureAccessUseCase: GetFeatureAccessUseCase,
    streakManager: IStreakManager
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)
    private val streakRefreshTrigger = MutableStateFlow(0)
    private val featureAccessRefreshTrigger = MutableStateFlow(0)
    private val loginUserEvents = MutableSharedFlow<AuthUser?>(replay = 0, extraBufferCapacity = 1)
    private val _errorDismissed = MutableStateFlow(false)

    private val userFlow: StateFlow<AuthUser?> = merge(
        userManager.observeUser(),
        loginUserEvents,
        refreshTrigger.flatMapLatest { userManager.observeUser() }
    )
        .distinctUntilChanged()
        .catch { error ->
            error.printStackTrace()
            emit(null)
        }
        .stateInEagerly(viewModelScope, null)

    private val streakFlow: StateFlow<UiState<StreakData>> = streakRefreshTrigger
        .flatMapLatest { streakManager.getStreak() }
        .map { state ->
            when (state) {
                is IStreakManager.StreakState.Loading -> UiState.Loading
                is IStreakManager.StreakState.Error -> UiState.Error(state.message)
                is IStreakManager.StreakState.Loaded -> UiState.Loaded(state.data)
            }
        }
        .catch { error ->
            error.printStackTrace()
            emit(UiState.Error(error.message ?: "Failed to load streak"))
        }
        .stateInWhileSubscribed(viewModelScope, initialValue = UiState.Loading)

    private val featureAccessFlow: StateFlow<UiState<FeatureAccessResponse?>> =
        getFeatureAccessUseCase.invoke()
            .map {
                UiState.Loaded(it)
            }
            .stateInEagerly(viewModelScope, UiState.Loaded(null))

    val state: StateFlow<UiState<ProfileUiData>> = combine(
        userFlow,
        streakFlow,
        featureAccessFlow,
        _errorDismissed,
    ) { user, streak, featureAccessState, errorDismissed ->
        val result = ProfileStateBuilder.createUiState(user, streak, featureAccessState)
        if (errorDismissed && result is UiState.Error) {
            UiState.Loaded(ProfileUiData(userInfo = null, streak = null, featureAccess = null, isSubscriptionsEnabled = false, shouldShowSubscriptionUI = false))
        } else {
            _errorDismissed.value = false
            result
        }
    }
        .catch { error ->
            emit(UiState.Error(error.message ?: "An error occurred"))
        }
        .stateInWhileSubscribed(viewModelScope, initialValue = UiState.Loading)

    private val authHandler = ProfileAuthHandler(
        userManager = userManager,
        loginUserEvents = loginUserEvents,
        refreshTrigger = refreshTrigger,
        streakRefreshTrigger = streakRefreshTrigger,
        featureAccessRefreshTrigger = featureAccessRefreshTrigger,
        scope = viewModelScope
    )

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.LoginWithGoogle -> authHandler.loginWithGoogle(event.idToken)
            is ProfileEvent.LoginWithApple -> authHandler.loginWithApple(event.idToken, event.fullName, event.appleUserId)
            is ProfileEvent.Logout -> authHandler.logout()
            is ProfileEvent.DeleteAccount -> authHandler.deleteAccount()
            is ProfileEvent.ClearError -> { _errorDismissed.value = true }
        }
    }
}
