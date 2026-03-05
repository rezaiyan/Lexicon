package presentation.feature.leaderboard

import androidx.lifecycle.viewModelScope
import core.common.fold
import domain.leaderboard.usecase.GetLeaderboardUseCase
import kotlinx.coroutines.launch
import presentation.base.BaseViewModel
import presentation.model.LeaderboardEntryUiModel
import presentation.model.LeaderboardUiData
import presentation.model.UiState

class LeaderboardViewModel(
    private val getLeaderboardUseCase: GetLeaderboardUseCase
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
