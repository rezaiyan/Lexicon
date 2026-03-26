package domain.profile.model

data class EnrichedProfileStats(
    val currentStreak: Int,
    val longestStreak: Int,
    val memberSince: String,
    val weeklyActivity: List<EnrichedDayActivity>,
    val languages: List<LanguagePair>,
)
