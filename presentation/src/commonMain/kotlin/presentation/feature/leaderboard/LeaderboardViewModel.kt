package presentation.feature.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.common.fold
import domain.leaderboard.usecase.GetLeaderboardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import presentation.model.LeaderboardEntryUiModel
import presentation.model.LeaderboardUiData
import presentation.model.UiState

class LeaderboardViewModel(
    private val getLeaderboardUseCase: GetLeaderboardUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<LeaderboardUiData>>(UiState.Loading)
    val state: StateFlow<UiState<LeaderboardUiData>> = _state.asStateFlow()

    init {
        loadLeaderboard()
    }

    fun refresh() {
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            getLeaderboardUseCase().fold(
                onSuccess = { leaderboard ->
                    _state.value = UiState.Loaded(
                        LeaderboardUiData(
                            entries = leaderboard.entries.map { it.toUiModel() },
                            userEntry = leaderboard.userEntry?.toUiModel()
                        )
                    )
                },
                onFailure = { error ->
                    _state.value = UiState.Error(
                        message = error.message ?: "Failed to load leaderboard"
                    )
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
