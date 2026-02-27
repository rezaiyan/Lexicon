package domain.profile.model

data class ProfileStats(
    val currentStreak: Int,
    val longestStreak: Int,
    val memberSince: String,
    val weeklyActivity: List<DayActivity>,
    val languages: List<LanguagePair>
)

data class DayActivity(
    val date: String,
    val reviewCount: Int
)

data class LanguagePair(
    val sourceLanguage: String,
    val targetLanguage: String,
    val wordCount: Int
)
