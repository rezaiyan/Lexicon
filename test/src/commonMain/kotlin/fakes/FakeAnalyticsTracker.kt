package fakes

import analytics.IAnalyticsTracker

class FakeAnalyticsTracker : IAnalyticsTracker {
    override fun logScreenView(screenName: String) = Unit
    val events = mutableListOf<Pair<String, Map<String, Any>?>>()

    override fun logEvent(eventName: String, parameters: Map<String, Any>?) {
        events += eventName to parameters
    }
    override fun logWordReviewed(rating: Int, wordLevel: Int, wasCorrect: Boolean) = Unit
    override fun logReviewSessionStart(cardCount: Int) = Unit
    override fun logReviewSessionComplete(cardsReviewed: Int, durationMs: Long, perfectCount: Int) = Unit
    val wordsImported = mutableListOf<Pair<Int, String>>()

    override fun logWordsImported(count: Int, method: String) {
        wordsImported += count to method
    }
    override fun logWordMastered(level: Int) = Unit
    override fun logStreakUpdated(days: Int, isNewRecord: Boolean) = Unit
    override fun logDailyGoalCompleted(cardsTarget: Int, cardsActual: Int) = Unit
    override fun logThemeChanged(themeMode: String, isDark: Boolean) = Unit
    override fun setUserProperty(name: String, value: String) = Unit
    override fun updateUserProgress(totalWords: Int, matureWords: Int, currentStreak: Int) = Unit
    override fun logError(error: Throwable, context: String?) = Unit
    override fun logNonFatalError(message: String, additionalInfo: Map<String, Any>?) = Unit
}
