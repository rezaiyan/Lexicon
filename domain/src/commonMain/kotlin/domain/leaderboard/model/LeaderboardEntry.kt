package domain.leaderboard.model

data class LeaderboardEntry(
    val rank: Int,
    val displayName: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val masteredWords: Int,
    val isCurrentUser: Boolean,
    val profileImageUrl: String? = null
)
