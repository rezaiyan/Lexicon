package analytics

import platform.Foundation.NSLog

/**
 * iOS implementation of analytics tracker
 * Logs to console - actual Firebase tracking is handled by Swift layer (FirebaseAnalyticsHelper)
 */
class IOSAnalyticsTracker : IAnalyticsTracker {

    private fun log(message: String) {
        NSLog("[Analytics] $message")
    }

    override fun logScreenView(screenName: String) {
        log("Screen View: $screenName")
    }

    override fun logEvent(eventName: String, parameters: Map<String, Any>?) {
        log("Event: $eventName${parameters?.let { " - $it" } ?: ""}")
    }

    override fun logWordReviewed(rating: Int, wordLevel: Int, wasCorrect: Boolean) {
        log("Word Reviewed: rating=$rating, level=$wordLevel, correct=$wasCorrect")
    }

    override fun logReviewSessionStart(cardCount: Int) {
        log("Review Session Started: $cardCount cards")
    }

    override fun logReviewSessionComplete(
        cardsReviewed: Int,
        durationMs: Long,
        perfectCount: Int
    ) {
        val accuracy = if (cardsReviewed > 0) perfectCount.toDouble() / cardsReviewed else 0.0
        log("Review Session Complete: $cardsReviewed cards, ${durationMs}ms, ${(accuracy * 100).toInt()}% accuracy")
    }

    override fun logWordsImported(count: Int, method: String) {
        log("Words Imported: $count words via $method")
    }

    override fun logWordMastered(level: Int) {
        log("Word Mastered: level $level")
    }

    override fun logStreakUpdated(days: Int, isNewRecord: Boolean) {
        log("Streak Updated: $days days${if (isNewRecord) " (NEW RECORD!)" else ""}")
    }

    override fun logDailyGoalCompleted(cardsTarget: Int, cardsActual: Int) {
        log("Daily Goal Complete: $cardsActual/$cardsTarget cards")
    }

    override fun logThemeChanged(themeMode: String, isDark: Boolean) {
        log("Theme Changed: $themeMode (dark=$isDark)")
    }

    override fun logLanguageChanged(language: String) {
        log("Language Changed: $language")
    }

    override fun setUserProperty(name: String, value: String) {
        log("User Property Set: $name = $value")
    }

    override fun updateUserProgress(
        totalWords: Int,
        matureWords: Int,
        currentStreak: Int
    ) {
        log("User Progress: $totalWords total, $matureWords mature, $currentStreak streak")
    }

    override fun logError(error: Throwable, context: String?) {
        NSLog("[Analytics] ERROR${context?.let { " ($it)" } ?: ""}: ${error.message}")
        error.printStackTrace()
    }

    override fun logNonFatalError(message: String, additionalInfo: Map<String, Any>?) {
        NSLog("[Analytics] ERROR (Non-Fatal): $message")
        additionalInfo?.forEach { (key, value) ->
            NSLog("[Analytics]   $key: $value")
        }
    }
}
