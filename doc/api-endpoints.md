# API Endpoints

Base URL configured via `AppConfig.VOKAB_BACKEND_URL` (e.g., `https://host/api/v1`).
All responses wrapped in `ApiResponse<T> { success, data, message }`.

## Authentication (6 endpoints)

| Method | Path | Auth | Request | Response | Notes |
|--------|------|------|---------|----------|-------|
| POST | `/auth/google` | No | `{idToken}` | `AuthResponse` | Google OAuth |
| POST | `/auth/apple` | No | `{idToken, authorizationCode?, fullName?, appleUserId?}` | `AuthResponse` | Apple Sign-In |
| POST | `/auth/refresh` | No | `{refreshToken}` | `AuthResponse` | Token refresh |
| POST | `/auth/logout` | Yes | `{refreshToken}` | Unit | Best effort |
| GET | `/users/me` | Yes | - | `UserDto` | Get profile |
| DELETE | `/auth/delete-account` | Yes | - | Unit | Delete account |

### AuthResponse
```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": { "id": 1, "email": "...", "name": "...", "subscriptionStatus": "FREE", "subscriptionExpiresAt": null, "currentStreak": 0 }
}
```

## Words (5 endpoints)

| Method | Path | Auth | Request | Response | Notes |
|--------|------|------|---------|----------|-------|
| GET | `/words` | Yes | - | `List<RemoteWord>` | Fetch all |
| POST | `/words` | Yes | `{words: RemoteWord[]}` | Unit | Upsert (bulk) |
| PATCH | `/words/{id}` | Yes | `RemoteWord` | Unit | Update single |
| DELETE | `/words/{id}` | Yes | - | Unit | Delete single |
| POST | `/words/batch-delete` | Yes | `{ids: Long[]}` | Unit | Batch delete |

### RemoteWord
```json
{
  "id": 1,
  "originalWord": "Haus",
  "translation": "house",
  "description": "Das ist ein Haus",
  "sourceLanguage": "de",
  "targetLanguage": "en",
  "level": 3,
  "easeFactor": 2.5,
  "interval": 3,
  "repetitions": 5,
  "lastReviewDate": 1700000000000,
  "nextReviewDate": 1700259200000,
  "createdAt": 1699000000000
}
```

## Feature Access (1 endpoint)

| Method | Path | Auth | Response |
|--------|------|------|----------|
| GET | `/users/feature-access` | Yes | `FeatureAccessResponse` |

```json
{
  "featureFlags": { "pushNotificationsEnabled": true },
  "userAccess": { "hasPremiumAccess": false }
}
```

## Onboarding (1 endpoint)

| Method | Path | Auth | Request | Response |
|--------|------|------|---------|----------|
| POST | `/onboarding/preferences` | Yes | `OnboardingPreferencesRequest` | `SuggestedVocabularyResponseDto` |

### Request
```json
{
  "targetLanguage": "de",
  "nativeLanguage": "en",
  "currentLevel": "beginner",
  "interests": ["travel", "food"]
}
```

### Response
```json
{
  "targetLanguage": "de",
  "nativeLanguage": "en",
  "currentLevel": "beginner",
  "items": [
    { "originalWord": "Haus", "translation": "house", "description": "Das ist ein Haus" }
  ]
}
```

## Streak (2 endpoints)

| Method | Path | Auth | Response |
|--------|------|------|----------|
| GET | `/streak` | Yes | `{currentStreak: Int}` |
| POST | `/streak/record` | Yes | `{currentStreak: Int}` |

## AI (1 endpoint)

| Method | Path | Auth | Request | Response |
|--------|------|------|---------|----------|
| POST | `/ai/extract-vocabulary` | Yes | `ExtractVocabularyRequest` | `{extractedText, wordCount}` |

### Request
```json
{
  "imageBase64": "...",
  "targetLanguage": "de",
  "extractWords": true,
  "extractSentences": false
}
```
Constraints: image max 3MB, min 128 bytes.

## Push Notifications (2 endpoints)

| Method | Path | Auth | Request |
|--------|------|------|---------|
| POST | `/notifications/register-token` | Yes | `{token, platform: "ANDROID"/"IOS"/"WEB", deviceId?}` |
| DELETE | `/notifications/tokens` | Yes | - |

## Total: 18 endpoints across 6 feature areas
