package feature.leaderboard.model

import androidx.compose.runtime.Immutable
import core.common.UiState

/** Number of top entries highlighted on the podium. */
internal const val PODIUM_SIZE = 3

@Immutable
data class LeaderboardState(
    val content: UiState<LeaderboardUiData> = UiState.Loading,
    /** True while a refresh runs on top of already loaded content. */
    val isRefreshing: Boolean = false,
    /** True when the last refresh failed and stale content is shown. */
    val refreshFailed: Boolean = false,
)

@Immutable
data class LeaderboardUiData(
    /** Top three entries, shown on the podium. Empty when fewer than three people are ranked. */
    val podium: List<LeaderboardEntryUiModel>,
    /** Entries shown as rows below the podium. */
    val rows: List<LeaderboardEntryUiModel>,
    /** The current user's entry, or null when they are not ranked yet. */
    val userEntry: LeaderboardEntryUiModel?,
    /** Index of the current user's row in [rows], or null when the row is not in the list. */
    val userRowIndex: Int?,
    /** The closest entry ranked above the current user: the next person to overtake. */
    val nextUp: LeaderboardEntryUiModel?,
) {
    val isEmpty: Boolean get() = podium.isEmpty() && rows.isEmpty()
}

@Immutable
data class LeaderboardEntryUiModel(
    val rank: Int,
    val displayName: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val masteredWords: Int,
    val isCurrentUser: Boolean,
    val profileImageUrl: String? = null
)
