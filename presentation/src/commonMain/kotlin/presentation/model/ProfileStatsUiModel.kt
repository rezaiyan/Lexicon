package presentation.model

data class ProfileStatsUiModel(
    val currentStreak: Int,
    val longestStreak: Int,
    val memberSince: String,
    val weeklyActivity: List<DayActivityUiModel>,
    val languages: List<LanguagePairUiModel>
)

data class DayActivityUiModel(
    val date: String,
    val dayOfMonth: Int,
    val dayOfWeekLabel: String,
    val reviewCount: Int,
    val isToday: Boolean
)

data class LanguagePairUiModel(
    val sourceLanguage: String,
    val targetLanguage: String,
    val wordCount: Int
)
