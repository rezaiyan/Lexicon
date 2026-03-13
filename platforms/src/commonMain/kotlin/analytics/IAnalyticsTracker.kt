package analytics

/**
 * Cross-platform analytics tracking interface
 * Provides a common API for tracking user behavior across all platforms
 */
interface IAnalyticsTracker {


    fun logScreenView(screenName: String)


    fun logEvent(eventName: String, parameters: Map<String, Any>? = null)


    fun logWordReviewed(rating: Int, wordLevel: Int, wasCorrect: Boolean)

    fun logReviewSessionStart(cardCount: Int)

    fun logReviewSessionComplete(
        cardsReviewed: Int,
        durationMs: Long,
        perfectCount: Int
    )


    fun logWordsImported(count: Int, method: String)

    fun logWordMastered(level: Int)


    fun logStreakUpdated(days: Int, isNewRecord: Boolean)

    fun logDailyGoalCompleted(cardsTarget: Int, cardsActual: Int)

    fun logThemeChanged(themeMode: String, isDark: Boolean)

    fun logLanguageChanged(language: String)

    fun setUserProperty(name: String, value: String)

    fun updateUserProgress(
        totalWords: Int,
        matureWords: Int,
        currentStreak: Int
    )


    fun logError(error: Throwable, context: String? = null)

    fun logNonFatalError(message: String, additionalInfo: Map<String, Any>? = null)
}

/**
 * Factory function to create platform-specific analytics tracker
 */
expect fun createAnalyticsTracker(): IAnalyticsTracker




