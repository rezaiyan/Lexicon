# BUG-3 — `GET /feature-access` Fires Once Per ViewModel (Up to 5×/Session)

**Priority:** P1
**Status:** Open

## Observed Behaviour

`GET /users/feature-access` fires separately each time a new screen opens:

| Trigger | Screen |
|---|---|
| App launch | `StudyProgressViewModel` |
| Settings tab open | `SettingsViewModel` |
| Word Manager sheet open | `WordManagerViewModel` |
| Profile screen open | `ProfileViewModel` |
| AI / Text Import screen open | `ImportViewModel` |

Up to **5 network calls** per session for data that never changes between screens.

## Root Cause

`GetFeatureAccessUseCase.invoke()` calls `authRepository.getFeatureAccessAsFlow()`, which calls:

```kotlin
// ApiClient.kt:253-258
fun <reified T> ApiClient.getFlowNotNull(path: String, ...): Flow<Try<T>> = flow {
    emit(getNotNull<T>(path, block))   // cold — network call on every collect
}
```

This is a **cold flow**. Every `collect` (i.e. every ViewModel that subscribes) fires a fresh HTTP request. There is no shared state or cache.

**Files:**
- `data/src/commonMain/kotlin/data/core/network/client/ApiClient.kt:253-258`
- `data/src/commonMain/kotlin/data/auth/repository/AuthRepositoryImpl.kt:122`
- `data/src/commonMain/kotlin/data/auth/remote/FeatureAccessRemoteDataSource.kt:21`

## Fix

Add an in-memory cache in `AuthRepositoryImpl` (or `FeatureAccessRemoteDataSource`).

```kotlin
// AuthRepositoryImpl
private var cachedFeatureAccess: FeatureAccessResponse? = null
private var featureAccessFetchedAt: Long = 0L
private val CACHE_TTL_MS = 5 * 60 * 1000L  // 5 minutes

fun getFeatureAccessAsFlow(): Flow<Try<FeatureAccessResponse>> = flow {
    val cached = cachedFeatureAccess
    val age = System.currentTimeMillis() - featureAccessFetchedAt
    if (cached != null && age < CACHE_TTL_MS) {
        emit(Try.success(cached))
        return@flow
    }
    val result = apiClient.getNotNull<FeatureAccessResponse>(path)
    result.onSuccess {
        cachedFeatureAccess = it
        featureAccessFetchedAt = System.currentTimeMillis()
    }
    emit(result)
}

fun clearFeatureAccessCache() {
    cachedFeatureAccess = null
    featureAccessFetchedAt = 0L
}
```

Call `clearFeatureAccessCache()` on logout and on explicit subscription purchase/change.

Alternatively, convert to a `SharedFlow` / `stateIn` so all subscribers share one upstream subscription.

## Acceptance Criteria

- `GET /feature-access` fires **at most once** per session (on first subscriber)
- Subsequent screen opens return the cached value with **0 network calls**
- Cache is cleared on logout — next session fetches fresh data
- A subscription purchase/upgrade triggers a cache invalidation and re-fetch
