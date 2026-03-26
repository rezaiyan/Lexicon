package feature.profile

import androidx.lifecycle.viewModelScope
import domain.auth.manager.IUserManager
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import core.common.fold
import core.common.getOrNull
import core.error.toUserMessage
import domain.profile.model.EnrichedProfileStats
import domain.profile.usecase.EnrichProfileStatsUseCase
import domain.profile.usecase.GetProfileStatsUseCase
import domain.streak.manager.IStreakManager
import domain.streak.model.StreakData
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import core.base.BaseViewModel
import feature.profile.model.DayActivityUiModel
import feature.profile.model.LanguagePairUiModel
import feature.profile.model.ProfileStatsUiModel
import feature.profile.model.ProfileUiData
import core.common.UiState
import core.common.ThrottledAction
import kotlin.time.Duration.Companion.seconds

class ProfileViewModel(
    private val userManager: IUserManager,
    private val getFeatureAccessUseCase: GetFeatureAccessUseCase,
    private val streakManager: IStreakManager,
    private val getProfileStatsUseCase: GetProfileStatsUseCase,
    private val enrichProfileStatsUseCase: EnrichProfileStatsUseCase,
) : BaseViewModel<UiState<ProfileUiData>, Nothing>() {

    override fun initialState(): UiState<ProfileUiData> = UiState.Loading

    private var currentUser: AuthUser? = null
    private var currentStreak: UiState<StreakData> = UiState.Loading
    private var currentFeatureAccess: UiState<FeatureAccessResponse?> = UiState.Loaded(null)
    private var currentProfileStats: ProfileStatsUiModel? = null

    private var streakJob: Job? = null

    private val throttledProfileStatsRefresh = ThrottledAction(
        scope = viewModelScope,
        interval = 60.seconds,
        action = { loadProfileStats() }
    )

    init {
        observeUser()
        observeStreak()
        observeFeatureAccess()
    }

    private fun observeUser() {
        viewModelScope.launch {
            userManager.observeUser()
                .distinctUntilChanged()
                .catch { emit(null) }
                .collect { user ->
                    currentUser = user
                    rebuildState()
                }
        }
    }

    private fun observeStreak() {
        streakJob?.cancel()
        streakJob = viewModelScope.launch {
            streakManager.getStreak()
                .map { state ->
                    when (state) {
                        is IStreakManager.StreakState.Loading -> UiState.Loading
                        is IStreakManager.StreakState.Error -> UiState.Error(state.message)
                        is IStreakManager.StreakState.Loaded -> UiState.Loaded(state.data)
                    }
                }
                .catch { error ->
                    emit(UiState.Error(error.toUserMessage()))
                }
                .collect { streak ->
                    currentStreak = streak
                    rebuildState()
                }
        }
    }

    private fun observeFeatureAccess() {
        viewModelScope.launch {
            getFeatureAccessUseCase.invoke()
                .map { UiState.Loaded(it) }
                .collect { featureAccess ->
                    currentFeatureAccess = featureAccess
                    rebuildState()
                }
        }
    }

    private suspend fun loadProfileStats() {
        val stats = getProfileStatsUseCase().getOrNull()
        currentProfileStats = stats?.let {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            enrichProfileStatsUseCase(it, today).toUiModel()
        }
        rebuildState()
    }

    private fun EnrichedProfileStats.toUiModel(): ProfileStatsUiModel =
        ProfileStatsUiModel(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            memberSince = memberSince,
            weeklyActivity = weeklyActivity.map { day ->
                DayActivityUiModel(
                    date = day.date,
                    dayOfMonth = day.dayOfMonth,
                    dayOfWeekLabel = day.dayOfWeekLabel,
                    reviewCount = day.reviewCount,
                    isToday = day.isToday,
                )
            },
            languages = languages.map { lang ->
                LanguagePairUiModel(
                    sourceLanguage = lang.sourceLanguage,
                    targetLanguage = lang.targetLanguage,
                    wordCount = lang.wordCount,
                )
            },
        )

    private fun rebuildState() {
        updateState {
            ProfileStateBuilder.createUiState(
                currentUser, currentStreak, currentFeatureAccess, currentProfileStats
            )
        }
    }

    fun refreshProfileStats() {
        throttledProfileStatsRefresh.request()
    }

    fun clearError() {
        if (currentState is UiState.Error) {
            updateState {
                UiState.Loaded(
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
    }

    fun logout() {
        viewModelScope.launch {
            userManager.logout().fold(
                onSuccess = { refreshAll() },
                onFailure = { refreshAll() }
            )
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            userManager.deleteAccount().fold(
                onSuccess = { refreshAll() },
                onFailure = { refreshAll() }
            )
        }
    }

    private fun refreshAll() {
        observeStreak()
        viewModelScope.launch { loadProfileStats() }
    }
}
