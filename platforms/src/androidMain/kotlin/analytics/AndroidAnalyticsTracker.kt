package analytics

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Android implementation of analytics tracker using Firebase
 */
class AndroidAnalyticsTracker : IAnalyticsTracker {
    
    private val analytics: FirebaseAnalytics = Firebase.analytics
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance()
    
    override fun logScreenView(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
    
    override fun logEvent(eventName: String, parameters: Map<String, Any>?) {
        val bundle = Bundle()
        parameters?.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        analytics.logEvent(eventName, bundle)
    }
    
    override fun logWordReviewed(rating: Int, wordLevel: Int, wasCorrect: Boolean) {
        val bundle = Bundle().apply {
            putInt("rating", rating)
            putInt("word_level", wordLevel)
            putBoolean("was_correct", wasCorrect)
            putString("review_quality", getRatingName(rating))
        }
        analytics.logEvent("word_reviewed", bundle)
    }
    
    override fun logReviewSessionStart(cardCount: Int) {
        val bundle = Bundle().apply {
            putInt("card_count", cardCount)
        }
        analytics.logEvent("review_session_start", bundle)
    }
    
    override fun logReviewSessionComplete(
        cardsReviewed: Int,
        durationMs: Long,
        perfectCount: Int
    ) {
        val accuracy = if (cardsReviewed > 0) perfectCount.toDouble() / cardsReviewed else 0.0
        
        val bundle = Bundle().apply {
            putInt("cards_reviewed", cardsReviewed)
            putLong("duration_ms", durationMs)
            putInt("perfect_count", perfectCount)
            putDouble("accuracy", accuracy)
        }
        analytics.logEvent("review_session_complete", bundle)
    }
    
    override fun logWordsImported(count: Int, method: String) {
        val bundle = Bundle().apply {
            putInt(FirebaseAnalytics.Param.QUANTITY, count)
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        analytics.logEvent("words_imported", bundle)
    }
    
    override fun logWordMastered(level: Int) {
        val bundle = Bundle().apply {
            putInt("mastery_level", level)
        }
        analytics.logEvent("word_mastered", bundle)
    }
    
    override fun logStreakUpdated(days: Int, isNewRecord: Boolean) {
        val bundle = Bundle().apply {
            putInt("streak_days", days)
            putBoolean("is_new_record", isNewRecord)
        }
        analytics.logEvent("streak_updated", bundle)
        
        // Log milestones
        if (days in listOf(7, 30, 100, 365)) {
            val milestoneBundle = Bundle().apply {
                putInt("milestone_days", days)
            }
            analytics.logEvent("streak_milestone", milestoneBundle)
        }
    }
    
    override fun logDailyGoalCompleted(cardsTarget: Int, cardsActual: Int) {
        val bundle = Bundle().apply {
            putInt("target", cardsTarget)
            putInt("actual", cardsActual)
            putBoolean("exceeded", cardsActual > cardsTarget)
        }
        analytics.logEvent("daily_goal_completed", bundle)
    }
    
    fun logAiInsightGenerated(usedLocal: Boolean, totalWords: Int) {
        val bundle = Bundle().apply {
            putBoolean("used_local_phrase", usedLocal)
            putInt("total_words", totalWords)
        }
        analytics.logEvent("ai_insight_generated", bundle)
    }

    override fun logThemeChanged(themeMode: String, isDark: Boolean) {
        val bundle = Bundle().apply {
            putString("theme_mode", themeMode)
            putBoolean("is_dark", isDark)
        }
        analytics.logEvent("theme_changed", bundle)
    }
    
    override fun logLanguageChanged(language: String) {
        val bundle = Bundle().apply {
            putString("target_language", language)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }
    
    override fun setUserProperty(name: String, value: String) {
        analytics.setUserProperty(name, value)
    }
    
    override fun updateUserProgress(
        totalWords: Int,
        matureWords: Int,
        currentStreak: Int
    ) {
        analytics.setUserProperty("total_words", totalWords.toString())
        analytics.setUserProperty("mature_words", matureWords.toString())
        analytics.setUserProperty("current_streak", currentStreak.toString())
    }
    
    override fun logError(error: Throwable, context: String?) {
        crashlytics.recordException(error)
        context?.let {
            crashlytics.setCustomKey("error_context", it)
        }
    }
    
    override fun logNonFatalError(message: String, additionalInfo: Map<String, Any>?) {
        val exception = Exception(message)
        crashlytics.recordException(exception)
        
        additionalInfo?.forEach { (key, value) ->
            crashlytics.setCustomKey(key, value.toString())
        }
    }
    
    private fun getRatingName(rating: Int): String {
        return when (rating) {
            0 -> "again"
            1 -> "hard"
            2 -> "good"
            3 -> "easy"
            else -> "unknown"
        }
    }
}

/**
 * Android factory function
 */
actual fun createAnalyticsTracker(): IAnalyticsTracker {
    return AndroidAnalyticsTracker()
}




