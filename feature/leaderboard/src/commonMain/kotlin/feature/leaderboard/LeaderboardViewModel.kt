package feature.leaderboard

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.common.fold
import domain.leaderboard.usecase.GetLeaderboardUseCase
import kotlinx.coroutines.launch
import core.base.BaseViewModel
import feature.leaderboard.model.LeaderboardEntryUiModel
import feature.leaderboard.model.LeaderboardUiData
import core.common.UiState

class LeaderboardViewModel(
    private val getLeaderboardUseCase: GetLeaderboardUseCase,
    private val analyticsTracker: IAnalyticsTracker,
) : BaseViewModel<UiState<LeaderboardUiData>, Nothing>() {

    override fun initialState(): UiState<LeaderboardUiData> = UiState.Loading

    init {
        loadLeaderboard()
    }

    fun refresh() {
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            updateState { UiState.Loading }
            getLeaderboardUseCase().fold(
                onSuccess = { leaderboard ->
                    updateState {
                        UiState.Loaded(
                            LeaderboardUiData(
                                entries = leaderboard.entries.map { it.toUiModel() },
                                userEntry = leaderboard.userEntry?.toUiModel()
                            )
                        )
                    }
                    analyticsTracker.logEvent(
                        "leaderboard_viewed",
                        mapOf(
                            "user_rank" to (leaderboard.userEntry?.rank?.toString() ?: "unranked"),
                            "total_users" to leaderboard.entries.size.toString()
                        )
                    )
                },
                onFailure = { error ->
                    updateState {
                        UiState.Error(
                            message = error.message ?: "Failed to load leaderboard"
                        )
                    }
                }
            )
        }
    }

    private fun domain.leaderboard.model.LeaderboardEntry.toUiModel() = LeaderboardEntryUiModel(
        rank = rank,
        displayName = displayName,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        masteredWords = masteredWords,
        isCurrentUser = isCurrentUser,
        profileImageUrl = profileImageUrl
    )
}
