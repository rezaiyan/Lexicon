package presentation.model

import androidx.compose.runtime.Immutable

@Immutable
data class LeaderboardUiData(
    val entries: List<LeaderboardEntryUiModel>,
    val userEntry: LeaderboardEntryUiModel?
)

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
