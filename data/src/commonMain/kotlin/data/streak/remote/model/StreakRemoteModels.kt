package data.streak.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class StreakResponse(
    val currentStreak: Int
)

