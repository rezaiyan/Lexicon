package fakes

import analytics.IAnalyticsTracker

class FakeAnalyticsTracker : IAnalyticsTracker {
    override fun logScreenView(screenName: String) = Unit
    override fun logEvent(eventName: String, parameters: Map<String, Any>?) = Unit
    override fun logWordReviewed(rating: Int, wordLevel: Int, wasCorrect: Boolean) = Unit
    override fun logReviewSessionStart(cardCount: Int) = Unit
    override fun logReviewSessionComplete(cardsReviewed: Int, durationMs: Long, perfectCount: Int) = Unit
    override fun logWordsImported(count: Int, method: String) = Unit
    override fun logWordMastered(level: Int) = Unit
    override fun logStreakUpdated(days: Int, isNewRecord: Boolean) = Unit
    override fun logDailyGoalCompleted(cardsTarget: Int, cardsActual: Int) = Unit
    override fun logThemeChanged(themeMode: String, isDark: Boolean) = Unit
    override fun logLanguageChanged(language: String) = Unit
    override fun setUserProperty(name: String, value: String) = Unit
    override fun updateUserProgress(totalWords: Int, matureWords: Int, currentStreak: Int) = Unit
    override fun logError(error: Throwable, context: String?) = Unit
    override fun logNonFatalError(message: String, additionalInfo: Map<String, Any>?) = Unit
}
