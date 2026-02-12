import Foundation
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics

/// Helper class for Firebase Analytics logging on iOS
/// Provides a clean API for tracking user actions
class FirebaseAnalyticsHelper {
    
    static let shared = FirebaseAnalyticsHelper()
    private let crashlytics = Crashlytics.crashlytics()
    
    private init() {}

    
    func logScreenView(screenName: String) {
        Analytics.logEvent(AnalyticsEventScreenView, parameters: [
            AnalyticsParameterScreenName: screenName,
            AnalyticsParameterScreenClass: screenName
        ])
    }

    
    func logWordReviewed(rating: Int, wordLevel: Int, wasCorrect: Bool) {
        Analytics.logEvent("word_reviewed", parameters: [
            "rating": rating,
            "word_level": wordLevel,
            "was_correct": wasCorrect,
            "review_quality": getRatingName(rating: rating)
        ])
    }
    
    func logReviewSessionStart(cardCount: Int) {
        Analytics.logEvent("review_session_start", parameters: [
            "card_count": cardCount
        ])
    }
    
    func logReviewSessionComplete(cardsReviewed: Int, durationMs: Int64, perfectCount: Int) {
        let accuracy = cardsReviewed > 0 ? Double(perfectCount) / Double(cardsReviewed) : 0.0
        
        Analytics.logEvent("review_session_complete", parameters: [
            "cards_reviewed": cardsReviewed,
            "duration_ms": durationMs,
            "perfect_count": perfectCount,
            "accuracy": accuracy
        ])
    }

    
    func logWordsImported(count: Int, method: String) {
        Analytics.logEvent("words_imported", parameters: [
            AnalyticsParameterQuantity: count,
            AnalyticsParameterMethod: method // "text" or "image"
        ])
    }
    
    func logWordMastered(level: Int) {
        Analytics.logEvent("word_mastered", parameters: [
            "mastery_level": level
        ])
    }

    
    func logStreakUpdated(days: Int, isNewRecord: Bool) {
        Analytics.logEvent("streak_updated", parameters: [
            "streak_days": days,
            "is_new_record": isNewRecord
        ])
        
        // Log milestones
        if [7, 30, 100, 365].contains(days) {
            Analytics.logEvent("streak_milestone", parameters: [
                "milestone_days": days
            ])
        }
    }
    
    func logDailyGoalCompleted(cardsTarget: Int, cardsActual: Int) {
        Analytics.logEvent("daily_goal_completed", parameters: [
            "target": cardsTarget,
            "actual": cardsActual,
            "exceeded": cardsActual > cardsTarget
        ])
    }

    
    func logAiInsightGenerated(usedLocal: Bool, totalWords: Int) {
        Analytics.logEvent("ai_insight_generated", parameters: [
            "used_local_phrase": usedLocal,
            "total_words": totalWords
        ])
    }
    
    func logThemeChanged(themeMode: String, isDark: Bool) {
        Analytics.logEvent("theme_changed", parameters: [
            "theme_mode": themeMode,
            "is_dark": isDark
        ])
    }
    
    func logLanguageChanged(language: String) {
        Analytics.logEvent(AnalyticsEventSelectContent, parameters: [
            "target_language": language
        ])
    }

    
    func setUserProperty(name: String, value: String) {
        Analytics.setUserProperty(value, forName: name)
    }
    
    func updateUserProgress(totalWords: Int, matureWords: Int, currentStreak: Int) {
        Analytics.setUserProperty(String(totalWords), forName: "total_words")
        Analytics.setUserProperty(String(matureWords), forName: "mature_words")
        Analytics.setUserProperty(String(currentStreak), forName: "current_streak")
    }

    
    func logError(_ error: Error, additionalInfo: [String: Any]? = nil) {
        crashlytics.record(error: error)
        
        if let info = additionalInfo {
            for (key, value) in info {
                crashlytics.setCustomValue(value, forKey: key)
            }
        }
    }
    
    func logNonFatalError(message: String, additionalInfo: [String: Any]? = nil) {
        let error = NSError(
            domain: "com.alirezaiyan.vokab",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey: message]
        )
        
        crashlytics.record(error: error)
        
        if let info = additionalInfo {
            for (key, value) in info {
                crashlytics.setCustomValue(value, forKey: key)
            }
        }
    }
    
    func setUserId(_ userId: String) {
        crashlytics.setUserID(userId)
        Analytics.setUserID(userId)
    }

    
    private func getRatingName(rating: Int) -> String {
        switch rating {
        case 0: return "again"
        case 1: return "hard"
        case 2: return "good"
        case 3: return "easy"
        default: return "unknown"
        }
    }
}





