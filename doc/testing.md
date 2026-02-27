# Testing

## Test Locations

| Type | Location | Framework | Command |
|------|----------|-----------|---------|
| Common unit tests | `composeApp/src/commonTest/kotlin/` | kotlin-test + coroutines-test | `./gradlew composeApp:cleanAllTests composeApp:allTests` |
| Android unit tests | `composeApp/src/androidTest/kotlin/` | JUnit 4 | `./gradlew composeApp:testDebugUnitTest` |
| Android instrumented | `composeApp/src/androidInstrumentedTest/kotlin/` | JUnit 4 + Room in-memory | Requires device/emulator |
| iOS tests | Currently disabled | GoogleSignIn dependency issue | CI verifies framework linking |

## Common Test Files

| File | Tests | Count |
|------|-------|-------|
| `ReviewWordUseCaseTest.kt` | SRS algorithm (forgot drops level, remembered advances, floor at 0, mastered exponential) | 7 |
| `ImportWordsUseCaseTest.kt` | CSV parsing, dedup, special chars, round-trip, languages | 25 |
| `ExportWordsUseCaseTest.kt` | Export format, UTF-8, special chars, round-trip, large datasets | 31 |
| `ImportValidationServiceTest.kt` | Parsing, validation, comments, blank lines, errors | 8 |
| `ReviewSettingsTest.kt` | Validation, presets, equality, copy, boundary | 25 |
| `GetProgressStatsUseCaseTest.kt` | Stats flow from repository | ~3 |
| `DeleteWordUseCaseTest.kt` | Successful delete, exception, negative ID | 3 |
| `DeleteWordsUseCaseTest.kt` | Batch delete, progress states, errors, edge cases | 9 |
| `GetDueWordsUseCaseTest.kt` | Due cards flow | ~2 |
| `WordTest.kt` | isSameContent (case-insensitive, trim, accents) | 5 |
| `LearningStageTest.kt` | Learning stage enum tests | ~5 |
| `ProgressStatsTest.kt` | Progress stats model tests | ~5 |
| `WordSyncServiceTest.kt` | Sync deduplication | ~5 |
| `GetReviewSettingsUseCaseTest.kt` | Settings retrieval | ~2 |
| `SubmitPreferencesUseCaseTest.kt` | Onboarding submission | ~3 |
| `ImportSuggestedVocabularyUseCaseTest.kt` | Suggested vocab import | ~3 |

## Android Instrumented Tests

| File | Tests | Count |
|------|-------|-------|
| `EndToEndReviewTest.kt` | Full review flow, due cards, progress stats, persistence | 17 |
| `LexiconDaoTest.kt` | CRUD, counts, levels, due cards, upsert | 12 |
| `ThreeWordsReviewScenarioTest.kt` | Bug regression: 3 words mixed review | ~3 |
| `FirstSuccessStaysInBucketTest.kt` | Advancement after first success | 3 |
| `WordOrderingTest.kt` | Word sorting by due date | ~5 |
| `BucketProgressionBugTest.kt` | Bucket progression regression | ~3 |

## Test Patterns

### Fake Repository Pattern
```kotlin
class FakeWordRepository : IWordRepository {
    val insertedWords = mutableListOf<Word>()
    val updatedWords = mutableListOf<Word>()
    var shouldThrowError = false
    // ... minimal implementations
}
```

### Test Data Builder
```kotlin
// In TestUtils.kt
fun createWord(
    id: Int = 1,
    originalWord: String = "hello",
    translation: String = "hola",
    level: Int = 0,
    // ... all parameters with defaults
): Word
```

### Review Settings for Tests
```kotlin
val DEFAULT_TEST_SETTINGS = ReviewSettings(successesToAdvance = 1, forgotPenalty = 2)
// = ReviewSettings.BALANCED
```

### In-Memory Database for Instrumented Tests
```kotlin
fun createWordRepository(): WordRepositoryImpl {
    val db = Room.inMemoryDatabaseBuilder(context, LexiconDatabase::class.java).build()
    val dao = db.lexiconDao()
    // Create with fake sync handler (no-op remote)
    return WordRepositoryImpl(localDataSource, fakeSyncHandler, fakeConflictResolver)
}
```

## Test Constants (`TestConstants.kt`)
```kotlin
const val DEFAULT_ORIGINAL_WORD = "hello"
const val DEFAULT_TRANSLATION = "hola"
const val DEFAULT_LEVEL = 0
const val DEFAULT_EASE_FACTOR = 2.5f
const val DEFAULT_INTERVAL = 0
const val DEFAULT_REPETITIONS = 0
const val DEFAULT_SOURCE_LANGUAGE = "en"
const val DEFAULT_TARGET_LANGUAGE = "es"
const val MILLIS_PER_DAY = 86_400_000L
const val MILLIS_PER_MINUTE = 60_000L
```

## CI/CD Test Workflows

**test.yml**: common tests -> Android unit tests -> iOS framework build
**build.yml**: common compilation -> Android APK -> iOS framework

All env secrets injected via `.github/actions/init-config/action.yml`.
