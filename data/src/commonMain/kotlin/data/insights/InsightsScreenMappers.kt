package data.insights

import data.insights.remote.*
import domain.insights.model.*
import kotlinx.datetime.LocalDate

fun InsightsScreenDto.toDomain() = InsightsScreen(
    totalReviews = totalReviews,
    hero = hero.toDomain(),
    coach = coach.map { it.toDomain() },
    mastery = sections.mastery?.toDomain(),
    habits = sections.habits?.toDomain(),
    words = sections.words?.toDomain(),
    wordRush = sections.wordRush?.toDomain(),
    locked = locked.mapNotNull { it.toDomainOrNull() },
)

private fun HeroDto.toDomain() = WeeklyHero(
    headline = headline,
    reviews = reviews.toDomain(),
    accuracyPct = accuracyPct?.toDomain(),
    leveledUp = leveledUp.toDomain(),
    currentStreak = currentStreak,
    week = week.mapNotNull { it.toDomainOrNull() },
)

private fun MetricDto.toDomain() = Metric(value, previous)

private fun DayCountDto.toDomainOrNull(): DayCount? =
    LocalDate.Formats.ISO.parseOrNull(date)?.let { DayCount(it, reviews) }

fun CoachCardDto.toDomain() = CoachCard(
    id = id,
    type = type,
    title = title,
    body = body,
    words = words.map { WordChip(it.id, it.text) },
    moreWordsCount = moreWordsCount,
    action = action.toDomain(),
)

private fun CoachActionDto.toDomain(): CoachAction {
    val text = label.orEmpty()
    return when (kind) {
        "REVIEW_WORDS" -> if (wordIds.isEmpty()) CoachAction.None else CoachAction.ReviewWords(text, wordIds)
        "START_REVIEW" -> CoachAction.StartReview(text)
        "ENABLE_REMINDER" -> hour?.let { CoachAction.EnableReminder(text, it) } ?: CoachAction.None
        "START_WORD_RUSH" -> CoachAction.StartWordRush(text)
        else -> CoachAction.None
    }
}

private fun MasterySectionDto.toDomain() =
    MasterySection(caption, levels.map { LevelCount(it.level, it.words) }, promotedThisWeek, demotedThisWeek)

private fun HabitsSectionDto.toDomain() = HabitsSection(
    caption = caption,
    heatmap = heatmap.mapNotNull { it.toDomainOrNull() },
    bestHour = bestHour?.let { BestHour(it.hour, it.accuracyPct) },
    weekdays = weekdays.map { WeekdayAccuracy(it.isoDay, it.accuracyPct, it.reviews) },
)

private fun WordsSectionDto.toDomain() = WordsSection(
    caption = caption,
    hardest = hardest.map { InsightWord(it.id, it.text, it.accuracyPct) },
    comebacks = comebacks.map { WordChip(it.id, it.text) },
)

private fun WordRushSectionDto.toDomain() = WordRushSection(caption, gamesPlayed, bestScore, recentScores)

private fun LockedSectionDto.toDomainOrNull(): LockedSection? {
    val key = when (section) {
        "mastery" -> InsightSectionKey.MASTERY
        "habits" -> InsightSectionKey.HABITS
        "words" -> InsightSectionKey.WORDS
        "wordRush" -> InsightSectionKey.WORD_RUSH
        else -> return null
    }
    return LockedSection(key, reviewsNeeded)
}
