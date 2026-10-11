package feature.leaderboard

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.UiState
import core.common.fold
import core.error.toUserMessage
import domain.leaderboard.model.Leaderboard
import domain.leaderboard.model.LeaderboardEntry
import domain.leaderboard.usecase.GetLeaderboardUseCase
import feature.leaderboard.model.LeaderboardEntryUiModel
import feature.leaderboard.model.LeaderboardState
import feature.leaderboard.model.LeaderboardUiData
import feature.leaderboard.model.PODIUM_SIZE
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LeaderboardViewModel(
    private val getLeaderboardUseCase: GetLeaderboardUseCase,
    private val analyticsTracker: IAnalyticsTracker,
) : BaseViewModel<LeaderboardState, Nothing>() {

    private var loadJob: Job? = null

    override fun initialState(): LeaderboardState = LeaderboardState()

    init {
        loadLeaderboard()
    }

    /** Reloads the leaderboard, keeping the current content visible while loading. */
    fun refresh() {
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        if (loadJob?.isActive == true) return
        val hasContent = currentState.content is UiState.Loaded
        updateState {
            if (hasContent) copy(isRefreshing = true) else copy(content = UiState.Loading)
        }
        loadJob = viewModelScope.launch {
            getLeaderboardUseCase().fold(
                onSuccess = { leaderboard ->
                    updateState {
                        copy(
                            content = UiState.Loaded(leaderboard.toUiData()),
                            isRefreshing = false,
                            refreshFailed = false,
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
                        if (hasContent) {
                            copy(isRefreshing = false, refreshFailed = true)
                        } else {
                            copy(content = UiState.Error(message = error.toUserMessage()))
                        }
                    }
                }
            )
        }
    }

    private fun Leaderboard.toUiData(): LeaderboardUiData {
        val all = entries.sortedBy { it.rank }.map { it.toUiModel() }
        val hasPodium = all.size >= PODIUM_SIZE
        val podium = if (hasPodium) all.take(PODIUM_SIZE) else emptyList()
        val rows = if (hasPodium) all.drop(PODIUM_SIZE) else all
        val userRowIndex = rows.indexOfFirst { it.isCurrentUser }.takeIf { it >= 0 }
        return LeaderboardUiData(
            podium = podium,
            rows = rows,
            userEntry = userEntry?.toUiModel() ?: all.firstOrNull { it.isCurrentUser },
            userRowIndex = userRowIndex,
        )
    }

    private fun LeaderboardEntry.toUiModel() = LeaderboardEntryUiModel(
        rank = rank,
        displayName = displayName,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        masteredWords = masteredWords,
        isCurrentUser = isCurrentUser,
        profileImageUrl = profileImageUrl
    )
}
