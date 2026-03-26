package domain.profile.model

data class EnrichedDayActivity(
    val date: String,
    val dayOfMonth: Int,
    val dayOfWeekLabel: String,
    val reviewCount: Int,
    val isToday: Boolean,
)
