package data.analytics.mapper

import data.analytics.remote.model.BestDayRemoteResponse
import data.analytics.remote.model.DailyStatsRemoteResponse
import data.analytics.remote.model.WeeklyReportRemoteResponse
import domain.analytics.model.BestDay
import domain.analytics.model.DailyStudyStats
import domain.analytics.model.WeeklyReport

fun WeeklyReportRemoteResponse.toDomain(): WeeklyReport = WeeklyReport(
    cardsReviewed = cardsReviewed,
    previousWeekCardsReviewed = previousWeekCardsReviewed,
    changePercent = changePercent,
    accuracyPercent = accuracyPercent,
    wordsMastered = wordsMastered,
    totalStudyTimeMs = totalStudyTimeMs,
    sessionsCount = sessionsCount,
    bestDay = bestDay?.toDomain(),
    weekStartDate = weekStartDate,
    weekEndDate = weekEndDate,
)

fun BestDayRemoteResponse.toDomain(): BestDay = BestDay(
    dayName = dayName,
    cardsReviewed = cardsReviewed,
    accuracyPercent = accuracyPercent,
)

fun DailyStatsRemoteResponse.toDomain(): DailyStudyStats = DailyStudyStats(
    date = date,
    sessionsCount = sessionsCount,
    cardsReviewed = cardsReviewed,
    correctCount = correctCount,
    incorrectCount = incorrectCount,
    studyTimeMs = studyTimeMs,
    uniqueWordsReviewed = 0,
    wordsLeveledUp = 0,
    wordsLeveledDown = 0,
)
