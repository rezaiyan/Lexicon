# Data Layer

All data layer code in `data/src/commonMain/kotlin/data/`.

## Database (SQLDelight)

### Schema File
`data/src/commonMain/sqldelight/data/core/database/Lexicon.sq`

### Tables

**WordEntity**
| Column | Type | Default | Notes |
|--------|------|---------|-------|
| id | INTEGER | AUTOINCREMENT | Primary key |
| originalWord | TEXT | NOT NULL | The word in target language |
| translation | TEXT | NOT NULL | Translation in native language |
| description | TEXT | NOT NULL | Context/example sentence |
| sourceLanguage | TEXT | NOT NULL | 2-letter code |
| targetLanguage | TEXT | NOT NULL | 2-letter code |
| level | INTEGER | 0 | SRS bucket (0-6) |
| easeFactor | REAL | 2.5 | SM-2 ease factor (1.3-2.5) |
| interval | INTEGER | 0 | Days until next review |
| repetitions | INTEGER | 0 | Successful review count |
| lastReviewDate | INTEGER | 0 | Epoch ms |
| nextReviewDate | INTEGER | - | Epoch ms when due |
| dateAdded | INTEGER | - | Epoch ms creation time |

**SettingsEntity** (singleton, id=1)
| Column | Type | Default |
|--------|------|---------|
| id | INTEGER | 1 |
| languageCode | TEXT | 'en' |
| themeMode | TEXT | 'AUTO' |
| lastInsightDate | TEXT | null |
| cachedInsight | TEXT | null |
| lastInsightDismissedTime | INTEGER | 0 |
| notificationsEnabled | INTEGER | 1 |
| reviewReminders | INTEGER | 1 |
| motivationalMessages | INTEGER | 1 |
| dailyReminderTime | TEXT | '18:00' |
| minimumDueCards | INTEGER | 5 |

### Key SQLDelight Queries
- `getDueCards` - WHERE nextReviewDate <= :currentTime
- `getWordsByLevel` - Smart sorting: due cards first, then by nextReviewDate
- `findWordByContent` - Case-insensitive + trimmed match on originalWord AND translation
- `progressRow` - Complex aggregate: all level counts + due count in one query
- `upsertWord` - INSERT OR REPLACE by ID

## Local Data Source

**WordLocalDataSource** (`data/word/local/WordLocalDataSource.kt`)
- Wraps LexiconQueries with domain mapping via WordMapper
- All Flow-returning methods use `.asFlow().mapToList()`
- Batch insert uses transactions

## Remote Data Sources

### WordRemoteDataSource (`data/word/remote/`)
| Method | Endpoint | Body |
|--------|----------|------|
| `getWords()` | GET /words | - |
| `upsertWords(words)` | POST /words | `{words: RemoteWord[]}` |
| `updateWord(id, word)` | PATCH /words/{id} | RemoteWord |
| `deleteWord(id)` | DELETE /words/{id} | - |
| `deleteWords(ids)` | POST /words/batch-delete | `{ids: Long[]}` |

### AuthDataSource (`data/auth/remote/`)
| Method | Endpoint | Body |
|--------|----------|------|
| `authenticateWithGoogle(idToken)` | POST /auth/google | `{idToken}` |
| `authenticateWithApple(idToken, fullName?, appleUserId)` | POST /auth/apple | `{idToken, fullName?, appleUserId?}` |
| `refreshTokens(refreshToken)` | POST /auth/refresh | `{refreshToken}` |
| `logout(refreshToken)` | POST /auth/logout | `{refreshToken}` (best effort) |
| `getUserProfile()` | GET /users/me | - |
| `deleteAccount()` | DELETE /auth/delete-account | - |

### Other Remote Data Sources
- **AiRemoteDataSource**: POST /ai/extract-vocabulary (3MB max image, base64)
- **StreakRemoteDataSource**: GET /streak, POST /streak/record
- **OnboardingRemoteDataSource**: POST /onboarding/preferences
- **PushNotificationDataSource**: POST /notifications/register-token, DELETE /notifications/tokens
- **FeatureAccessRemoteDataSource**: GET /users/feature-access

## Mappers

### WordMapper (`data/word/mapper/`)
- `toDomain(WordEntity, fallbackLanguage) -> Word`
- `toEntityData(Word) -> WordEntityData`
- Language resolution via `Language.fromCode()` with ENGLISH fallback

### AuthMapper (`data/auth/mapper/`)
- `UserDto.toDomain() -> AuthUser`
- Maps subscriptionStatus string to SubscriptionStatus enum

## Sync System

### WordRemoteSyncHandler (`data/word/sync/`)
- Converts Word -> RemoteWord for API calls
- Methods: syncWordsToRemote, syncWordUpdateToRemote, syncWordDeletionToRemote, syncFromRemote

### WordConflictResolver (`data/word/sync/`)
Strategy:
1. Build local maps by ID and by content key (lowercase originalWord + translation)
2. For each remote word: match by content first, then by ID
3. Merge: use local ID if exists, preserve local dateAdded
4. Deduplicate by content key

## Remote Models

### RemoteWord
```kotlin
data class RemoteWord(
    val id: Long? = null,
    val originalWord: String, val translation: String, val description: String,
    val sourceLanguage: String, val targetLanguage: String,
    val level: Int, val easeFactor: Float, val interval: Int, val repetitions: Int,
    val lastReviewDate: Long, val nextReviewDate: Long, val createdAt: Long? = null
)
```

### AuthResponse
```kotlin
data class AuthResponse(
    val accessToken: String, val refreshToken: String,
    val tokenType: String, val expiresIn: Long, val user: UserDto
)
```

### ApiResponse Wrapper
All backend responses wrapped in:
```kotlin
data class ApiResponse<T>(val success: Boolean, val data: T? = null, val message: String? = null)
```
