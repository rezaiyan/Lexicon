package data.leaderboard.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntryResponse(
    val rank: Int,
    val displayName: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val masteredWords: Int,
    val isCurrentUser: Boolean,
    val profileImageUrl: String? = null
)

@Serializable
data class LeaderboardResponse(
    val entries: List<LeaderboardEntryResponse>,
    val userEntry: LeaderboardEntryResponse?
)
