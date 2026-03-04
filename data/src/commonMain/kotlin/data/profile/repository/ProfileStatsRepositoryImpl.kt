package data.profile.repository

import data.profile.remote.ProfileStatsRemoteDataSource
import core.common.Try
import core.common.map
import domain.profile.model.DayActivity
import domain.profile.model.LanguagePair
import domain.profile.model.ProfileStats
import domain.profile.repository.IProfileStatsRepository

class ProfileStatsRepositoryImpl(
    private val remoteDataSource: ProfileStatsRemoteDataSource
) : IProfileStatsRepository {

    override suspend fun getProfileStats(): Try<ProfileStats> {
        return remoteDataSource.getProfileStats().map { response ->
            ProfileStats(
                currentStreak = response.currentStreak,
                longestStreak = response.longestStreak,
                memberSince = response.memberSince,
                weeklyActivity = response.weeklyActivity.map { day ->
                    DayActivity(
                        date = day.date,
                        reviewCount = day.reviewCount
                    )
                },
                languages = response.languages.map { lang ->
                    LanguagePair(
                        sourceLanguage = lang.sourceLanguage,
                        targetLanguage = lang.targetLanguage,
                        wordCount = lang.wordCount
                    )
                }
            )
        }
    }
}
