@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)

package presentation.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.auth.manager.IUserManager
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.common.fold
import domain.common.getOrNull
import domain.profile.model.DayActivity
import domain.profile.model.ProfileStats
import domain.profile.usecase.GetProfileStatsUseCase
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import presentation.model.DayActivityUiModel
import presentation.model.LanguagePairUiModel
import presentation.model.ProfileStatsUiModel
import presentation.model.ProfileUiData
import presentation.model.UiState
import presentation.util.stateInEagerly
import presentation.util.stateInWhileSubscribed

class ProfileViewModel(
    private val userManager: IUserManager,
    getFeatureAccessUseCase: GetFeatureAccessUseCase,
    streakManager: IStreakManager,
    private val getProfileStatsUseCase: GetProfileStatsUseCase
) : ViewModel() {

    private val streakRefreshTrigger = MutableStateFlow(0)
    private val profileStatsRefreshTrigger = MutableStateFlow(0)

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

    private val profileStatsFlow: StateFlow<ProfileStatsUiModel?> = profileStatsRefreshTrigger
        .flatMapLatest {
            flow {
                emit(null)
                val result = getProfileStatsUseCase()
                emit(result.getOrNull()?.toUiModel())
            }
        }
        .catch { error ->
            error.printStackTrace()
            emit(null)
        }
        .stateInWhileSubscribed(viewModelScope, initialValue = null)

    init {
        observeAndCombineState()
    }

    private fun observeAndCombineState() {
        viewModelScope.launch {
            combine(
                userFlow,
                streakFlow,
                featureAccessFlow,
                profileStatsFlow
            ) { user, streak, featureAccessState, profileStats ->
                ProfileStateBuilder.createUiState(
                    user, streak, featureAccessState, profileStats
                )
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
                    profileStatsRefreshTrigger.value++
                },
                onFailure = { error ->
                    error.printStackTrace()
                    streakRefreshTrigger.value++
                    profileStatsRefreshTrigger.value++
                }
            )
        }
    }

    private fun deleteAccount() {
        viewModelScope.launch {
            userManager.deleteAccount().fold(
                onSuccess = {
                    streakRefreshTrigger.value++
                    profileStatsRefreshTrigger.value++
                },
                onFailure = { error ->
                    error.printStackTrace()
                    streakRefreshTrigger.value++
                    profileStatsRefreshTrigger.value++
                }
            )
        }
    }
}

private fun ProfileStats.toUiModel(): ProfileStatsUiModel {
    val todayStr = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date.toString()

    return ProfileStatsUiModel(
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        memberSince = memberSince,
        weeklyActivity = weeklyActivity.map { it.toUiModel(todayStr) }.sortedBy { it.date },
        languages = languages.map { lang ->
            LanguagePairUiModel(
                sourceLanguage = lang.sourceLanguage,
                targetLanguage = lang.targetLanguage,
                wordCount = lang.wordCount
            )
        }
    )
}

private fun DayActivity.toUiModel(todayStr: String): DayActivityUiModel {
    val localDate = LocalDate.parse(date)
    val dayOfWeekLabel = when (localDate.dayOfWeek) {
        DayOfWeek.MONDAY -> "MON"
        DayOfWeek.TUESDAY -> "TUE"
        DayOfWeek.WEDNESDAY -> "WED"
        DayOfWeek.THURSDAY -> "THU"
        DayOfWeek.FRIDAY -> "FRI"
        DayOfWeek.SATURDAY -> "SAT"
        DayOfWeek.SUNDAY -> "SUN"
    }
    return DayActivityUiModel(
        date = date,
        dayOfMonth = localDate.day,
        dayOfWeekLabel = dayOfWeekLabel,
        reviewCount = reviewCount,
        isToday = date == todayStr
    )
}
