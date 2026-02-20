@file:OptIn(ExperimentalCoroutinesApi::class)

package presentation.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.auth.manager.IUserManager
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.common.fold
import domain.streak.manager.IStreakManager
import domain.streak.model.StreakData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import presentation.model.ProfileUiData
import presentation.model.UiState
import presentation.util.stateInEagerly
import presentation.util.stateInWhileSubscribed

class ProfileViewModel(
    private val userManager: IUserManager,
    getFeatureAccessUseCase: GetFeatureAccessUseCase,
    streakManager: IStreakManager
) : ViewModel() {

    private val streakRefreshTrigger = MutableStateFlow(0)

    private val _state = MutableStateFlow<UiState<ProfileUiData>>(UiState.Loading)
    val state: StateFlow<UiState<ProfileUiData>> = _state.asStateFlow()

    private val userFlow: StateFlow<AuthUser?> = userManager.observeUser()
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

    init {
        observeAndCombineState()
    }

    private fun observeAndCombineState() {
        viewModelScope.launch {
            combine(
                userFlow,
                streakFlow,
                featureAccessFlow
            ) { user, streak, featureAccessState ->
                ProfileStateBuilder.createUiState(user, streak, featureAccessState)
            }
                .catch { error ->
                    emit(UiState.Error(error.message ?: "An error occurred"))
                }
                .collect { newState ->
                    _state.value = newState
                }
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.Logout -> logout()
            is ProfileEvent.DeleteAccount -> deleteAccount()
            is ProfileEvent.ClearError -> clearError()
        }
    }

    private fun clearError() {
        if (_state.value is UiState.Error) {
            _state.value = UiState.Loaded(
                ProfileUiData(
                    userInfo = null,
                    streak = null,
                    featureAccess = null,
                    isSubscriptionsEnabled = false,
                    shouldShowSubscriptionUI = false
                )
            )
        }
    }

    private fun logout() {
        viewModelScope.launch {
            userManager.logout().fold(
                onSuccess = {
                    streakRefreshTrigger.value++
                },
                onFailure = { error ->
                    error.printStackTrace()
                    streakRefreshTrigger.value++
                }
            )
        }
    }

    private fun deleteAccount() {
        viewModelScope.launch {
            userManager.deleteAccount().fold(
                onSuccess = {
                    streakRefreshTrigger.value++
                },
                onFailure = { error ->
                    error.printStackTrace()
                    streakRefreshTrigger.value++
                }
            )
        }
    }
}
