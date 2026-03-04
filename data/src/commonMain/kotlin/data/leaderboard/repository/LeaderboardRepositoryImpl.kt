package data.leaderboard.repository

import data.leaderboard.remote.LeaderboardRemoteDataSource
import core.common.Try
import core.common.map
import domain.leaderboard.model.Leaderboard
import domain.leaderboard.model.LeaderboardEntry
import domain.leaderboard.repository.ILeaderboardRepository

class LeaderboardRepositoryImpl(
    private val remoteDataSource: LeaderboardRemoteDataSource
) : ILeaderboardRepository {

    override suspend fun getLeaderboard(): Try<Leaderboard> {
        return remoteDataSource.getLeaderboard().map { response ->
            Leaderboard(
                entries = response.entries.map { entry ->
                    LeaderboardEntry(
                        rank = entry.rank,
                        displayName = entry.displayName,
                        currentStreak = entry.currentStreak,
                        longestStreak = entry.longestStreak,
                        masteredWords = entry.masteredWords,
                        isCurrentUser = entry.isCurrentUser,
                        profileImageUrl = entry.profileImageUrl
                    )
                },
                userEntry = response.userEntry?.let { entry ->
                    LeaderboardEntry(
                        rank = entry.rank,
                        displayName = entry.displayName,
                        currentStreak = entry.currentStreak,
                        longestStreak = entry.longestStreak,
                        masteredWords = entry.masteredWords,
                        isCurrentUser = entry.isCurrentUser,
                        profileImageUrl = entry.profileImageUrl
                    )
                }
            )
        }
    }
}
