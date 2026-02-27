package data.profile.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileStatsResponse(
    val currentStreak: Int,
    val longestStreak: Int,
    val memberSince: String,
    val weeklyActivity: List<DayActivityResponse>,
    val languages: List<LanguagePairResponse>
)

@Serializable
data class DayActivityResponse(
    val date: String,
    val reviewCount: Int
)

@Serializable
data class LanguagePairResponse(
    val sourceLanguage: String,
    val targetLanguage: String,
    val wordCount: Int
)
