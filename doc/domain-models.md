# Domain Models

All models live in `domain/src/commonMain/kotlin/domain/`.

## Common

### Try<T> (`domain/common/Try.kt`)
Custom Result type that rethrows `CancellationException` and `Error`.
```kotlin
sealed class Try<out T> {
    data class Success<T>(val value: T) : Try<T>()
    data class Failure(val throwable: Throwable) : Try<Nothing>()
}
// Extensions: getOrNull(), getOrDefault(), getOrThrow(), map(), flatMap(),
// fold(), recover(), doOnSuccess(), doOnFailure(), zipUnit()
```

## Auth Models (`domain/auth/model/`)

### AuthUser
```kotlin
data class AuthUser(
    val id: Long,
    val email: String,
    val name: String,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.FREE,
    val subscriptionExpiresAt: String? = null,
    val currentStreak: Int = 0
)
```

### SubscriptionStatus
```kotlin
enum class SubscriptionStatus { FREE, TRIAL, ACTIVE, EXPIRED, CANCELLED }
```

### AuthState
```kotlin
data class AuthState(
    val isAuthenticated: Boolean = false,
    val user: AuthUser? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### FeatureAccess (`domain/auth/model/FeatureAccess.kt`)
```kotlin
data class FeatureFlags(val pushNotificationsEnabled: Boolean = true)
data class UserFeatureAccess(val hasPremiumAccess: Boolean = false)
data class FeatureAccessResponse(val featureFlags: FeatureFlags, val userAccess: UserFeatureAccess)
```

## Word Models (`domain/word/model/`)

### Word
```kotlin
data class Word(
    val id: Int,
    val originalWord: String,
    val translation: String,
    val description: String,
    val sourceLanguage: Language,
    val targetLanguage: Language,
    val level: Int = 0,              // 0-6 (spaced repetition bucket)
    val easeFactor: Float = 2.5f,    // SM-2 ease factor (1.3-2.5)
    val interval: Int = 0,           // days until next review
    val repetitions: Int = 0,        // successful reviews count
    val lastReviewDate: Long = 0L,
    val nextReviewDate: Long,        // epoch ms when card is due
    val dateAdded: Long = now()
)
// Method: isSameContent(other) - compares originalWord+translation (case-insensitive, trimmed)
```

### ProgressStats
```kotlin
data class ProgressStats(
    val level0Count: Int = 0,   // Fresh
    val level1Count: Int = 0,   // Learning
    val level2Count: Int = 0,   // Familiar
    val level3Count: Int = 0,   // Building
    val level4Count: Int = 0,   // Almost there
    val level5Count: Int = 0,   // Strong
    val level6Count: Int = 0,   // Mastered
    val totalWords: Int = 0,
    val dueCards: Int = 0
)
// Computed: learningWords, matureWords
```

### LearningStage
```kotlin
enum class LearningStage(val level: Int) {
    LEVEL_0_FRESH(0),       // Brand new
    LEVEL_1_LEARNING(1),    // 10 min interval
    LEVEL_2_FAMILIAR(2),    // 1 day
    LEVEL_3_BUILDING(3),    // 3 days
    LEVEL_4_ALMOST(4),      // 7 days
    LEVEL_5_STRONG(5),      // 14 days
    LEVEL_6_MASTERED(6);    // 30+ days
    companion object { fun fromLevel(level: Int): LearningStage }
}
```

## Settings Models (`domain/settings/model/`)

### ReviewSettings
```kotlin
data class ReviewSettings(
    val successesToAdvance: Int = 1,  // 1-3
    val forgotPenalty: Int = 2        // 1-3
)
// Presets: EASY(1,1), BALANCED(1,2), RIGOROUS(2,2), EXPERT(2,3)
```

### ThemeMode
```kotlin
enum class ThemeMode { AUTO, LIGHT, DARK }
```

## Streak (`domain/streak/model/`)
```kotlin
data class StreakData(val currentStreak: Int)
```

## Subscription (`domain/subscription/model/`)
```kotlin
enum class PackagePeriod { MONTHLY, ANNUAL, LIFETIME, UNKNOWN }
data class SubscriptionProduct(val title: String, val description: String, val priceFormatted: String)
data class SubscriptionPackage(val identifier: String, val packagePeriod: PackagePeriod, val product: SubscriptionProduct)
data class SubscriptionOffering(val availablePackages: List<SubscriptionPackage>)
data class SubscriptionEntitlement(val identifier: String, val isActive: Boolean, val expirationDateMillis: Long?, val productIdentifier: String)
data class SubscriptionCustomerInfo(val activeEntitlements: Map<String, SubscriptionEntitlement>, val managementUrlString: String? = null)
```

## TTS (`domain/tts/model/`)
```kotlin
sealed class TtsState {
    data object Idle : TtsState()
    data class Downloading(val languageCode: String, val progress: Float) : TtsState()
    data object Loading : TtsState()
    data object Speaking : TtsState()
    data class Error(val message: String) : TtsState()
}
```

## Onboarding (`domain/onboarding/model/`)
```kotlin
data class OnboardingPreferences(
    val targetLanguage: String, val nativeLanguage: String,
    val level: String, val interests: List<String> = emptyList()
)

data class SuggestedVocabulary(
    val originalWord: String, val translation: String, val description: String,
    val sourceLanguage: String, val targetLanguage: String
)

data class SuggestedVocabularyResponse(
    val suggestedVocabulary: List<SuggestedVocabulary>,
    val targetLanguage: String, val nativeLanguage: String, val currentLevel: String
)
```

## Session Verification
```kotlin
sealed class SessionVerificationResult {
    data class Valid(val user: AuthUser) : SessionVerificationResult()
    data object Expired : SessionVerificationResult()
    data object NotAuthenticated : SessionVerificationResult()
    data object ServerError : SessionVerificationResult()
}
```

## Delete Progress
```kotlin
sealed class DeleteWordsProgress {
    data class DeletingFromBackend(val count: Int) : DeleteWordsProgress()
    data class DeletingFromLocal(val count: Int) : DeleteWordsProgress()
    data class Completed(val count: Int) : DeleteWordsProgress()
    data class Failed(val error: String) : DeleteWordsProgress()
}
```

## Language Enum (`utils/src/commonMain/kotlin/utils/Language.kt`)
14 languages: ENGLISH, GERMAN, FRENCH, SPANISH, ITALIAN, PORTUGUESE, RUSSIAN, CHINESE, JAPANESE, KOREAN, ARABIC, TURKISH, DUTCH, PERSIAN (HINDI)
Each has: `code` (2-letter), `displayName`, `nativeName`, `aiPromptName`
