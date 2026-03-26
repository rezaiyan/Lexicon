package domain.profile.usecase

import domain.profile.model.DayActivity
import domain.profile.model.EnrichedDayActivity
import domain.profile.model.EnrichedProfileStats
import domain.profile.model.ProfileStats
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

class EnrichProfileStatsUseCase {

    operator fun invoke(stats: ProfileStats, today: LocalDate): EnrichedProfileStats {
        val todayStr = today.toString()
        val enrichedActivity = stats.weeklyActivity
            .map { it.enrich(todayStr) }
            .sortedBy { it.date }

        return EnrichedProfileStats(
            currentStreak = stats.currentStreak,
            longestStreak = stats.longestStreak,
            memberSince = stats.memberSince,
            weeklyActivity = enrichedActivity,
            languages = stats.languages,
        )
    }

    private fun DayActivity.enrich(todayStr: String): EnrichedDayActivity {
        val localDate = LocalDate.parse(date)
        val dayOfWeekLabel = when (localDate.dayOfWeek) {
            DayOfWeek.MONDAY -> "MON"
            DayOfWeek.TUESDAY -> "TUE"
            DayOfWeek.WEDNESDAY -> "WED"
            DayOfWeek.THURSDAY -> "THU"
            DayOfWeek.FRIDAY -> "FRI"
            DayOfWeek.SATURDAY -> "SAT"
            DayOfWeek.SUNDAY -> "SUN"
            else -> ""
        }
        return EnrichedDayActivity(
            date = date,
            dayOfMonth = localDate.dayOfMonth,
            dayOfWeekLabel = dayOfWeekLabel,
            reviewCount = reviewCount,
            isToday = date == todayStr,
        )
    }
}
