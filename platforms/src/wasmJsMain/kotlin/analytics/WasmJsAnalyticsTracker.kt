package analytics

/**
 * WasmJs implementation of IAnalyticsTracker
 * Logs events to browser console for development visibility
 */
class WasmJsAnalyticsTracker : IAnalyticsTracker {

    override fun logScreenView(screenName: String) {
        println("[Analytics] Screen: $screenName")
    }

    override fun logEvent(eventName: String, parameters: Map<String, Any>?) {
        println("[Analytics] Event: $eventName, params: $parameters")
    }

    override fun logWordReviewed(rating: Int, wordLevel: Int, wasCorrect: Boolean) {
        println("[Analytics] Word reviewed: rating=$rating, level=$wordLevel, correct=$wasCorrect")
    }

    override fun logReviewSessionStart(cardCount: Int) {
        println("[Analytics] Review session start: $cardCount cards")
    }

    override fun logReviewSessionComplete(cardsReviewed: Int, durationMs: Long, perfectCount: Int) {
        println("[Analytics] Review session complete: reviewed=$cardsReviewed, duration=${durationMs}ms, perfect=$perfectCount")
    }

    override fun logWordsImported(count: Int, method: String) {
        println("[Analytics] Words imported: count=$count, method=$method")
    }

    override fun logWordMastered(level: Int) {
        println("[Analytics] Word mastered: level=$level")
    }

    override fun logStreakUpdated(days: Int, isNewRecord: Boolean) {
        println("[Analytics] Streak updated: days=$days, newRecord=$isNewRecord")
    }

    override fun logDailyGoalCompleted(cardsTarget: Int, cardsActual: Int) {
        println("[Analytics] Daily goal completed: target=$cardsTarget, actual=$cardsActual")
    }

    fun logAiInsightGenerated(usedLocal: Boolean, totalWords: Int) {
        println("[Analytics] AI insight generated: usedLocal=$usedLocal, totalWords=$totalWords")
    }

    override fun logThemeChanged(themeMode: String, isDark: Boolean) {
        println("[Analytics] Theme changed: mode=$themeMode, dark=$isDark")
    }

    override fun logLanguageChanged(language: String) {
        println("[Analytics] Language changed: $language")
    }

    override fun setUserProperty(name: String, value: String) {
        println("[Analytics] User property: $name=$value")
    }

    override fun updateUserProgress(totalWords: Int, matureWords: Int, currentStreak: Int) {
        println("[Analytics] User progress: total=$totalWords, mature=$matureWords, streak=$currentStreak")
    }

    override fun logError(error: Throwable, context: String?) {
        println("[Analytics] Error: ${error.message}, context=$context")
    }

    override fun logNonFatalError(message: String, additionalInfo: Map<String, Any>?) {
        println("[Analytics] Non-fatal error: $message, info=$additionalInfo")
    }
}

/**
 * WasmJs factory function for analytics tracker
 */
actual fun createAnalyticsTracker(): IAnalyticsTracker {
    return WasmJsAnalyticsTracker()
}
