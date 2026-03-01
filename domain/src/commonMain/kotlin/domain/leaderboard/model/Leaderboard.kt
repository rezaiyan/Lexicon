package domain.leaderboard.model

data class Leaderboard(
    val entries: List<LeaderboardEntry>,
    val userEntry: LeaderboardEntry?
)
