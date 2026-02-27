# Network Infrastructure

All networking code in `data/src/commonMain/kotlin/data/core/network/`.

## HTTP Client Stack

```
HttpClient (Ktor)
  ├── ContentNegotiation (Json: lenient, ignoreUnknownKeys)
  ├── Logging (HEADERS in debug, NONE in production)
  ├── AuthInterceptor (adds Bearer token)
  ├── RefreshAndRetryInterceptor (handles 401/403 with token refresh)
  └── ErrorInterceptor (maps non-2xx to exceptions)
```

**Order matters**: Auth → Refresh → Error

### HttpClientProvider (`HttpClientProvider.kt`)
Factory: `createHttpClient(authInterceptor?, tokenRefreshManagerProvider?, errorInterceptor?) -> HttpClient`

## ApiClient (`client/ApiClient.kt`)

Wrapper around Ktor HttpClient with `Try<T>` and `Flow<T>` support.

| Method | Return | Notes |
|--------|--------|-------|
| `get<T>(path)` | `Try<T?>` | GET |
| `getNotNull<T>(path)` | `Try<T>` | GET, fails if null |
| `post<T>(path, body?)` | `Try<T?>` | POST |
| `postUnit(path, body?)` | `Try<Unit>` | POST, no response body |
| `patch<T>(path, body?)` | `Try<T?>` | PATCH |
| `patchUnit(path, body?)` | `Try<Unit>` | PATCH, no response |
| `delete(path)` | `Try<Unit>` | DELETE |
| `getFlow<T>(path)` | `Flow<Try<T?>>` | Async GET |

All requests auto-set `Content-Type: application/json` for POST/PATCH.

## Interceptors

### AuthInterceptor (`interceptor/AuthInterceptor.kt`)
- Skips auth endpoints (paths containing `/auth/`) except `/logout`
- Proactively refreshes token if it expires within **5 minutes**
- Adds `Authorization: Bearer {token}` header

### RefreshAndRetryInterceptor (`interceptor/RefreshAndRetryInterceptor.kt`)
**Trigger**: Response status 401 or 403, has Authorization header, not already a retry, not auth endpoint.

**Flow**:
1. Intercept 401/403 response
2. Call `TokenRefreshManager.refresh()` (mutex-protected single-flight)
3. Success: retry original request with new token
4. Failure: pass original response through
5. Edge case: if refresh succeeds but retry still 401/403 -> clear session (account deleted/banned)

### ErrorInterceptor (`interceptor/ErrorInterceptor.kt`)
Maps non-2xx status codes to exceptions via HttpErrorMapper.

## Token Management

### TokenManager (`auth/token/TokenManager.kt`)
**Storage**: SecureStorage (Keychain on iOS, EncryptedSharedPreferences on Android)

| Method | Notes |
|--------|-------|
| `saveTokens(access, refresh, expiresInMs)` | Calculates expiry = now + expiresInMs |
| `getAccessToken()` | From secure storage |
| `getRefreshToken()` | From secure storage |
| `clearTokens()` | Wipe all |
| `hasTokens()` | Check access token exists |
| `getTokenExpiresAt()` | Expiry timestamp (0 if unknown) |

### TokenRefreshManager (`auth/refresh/TokenRefreshManager.kt`)
**Pattern**: Mutex-protected single-flight refresh

**Flow**:
1. Acquire mutex (prevents thundering herd)
2. Check if another caller refreshed while waiting -> reuse new token
3. Check refresh token exists -> fail if not
4. Call `authDataSource.refreshTokens(refreshToken)`
5. Success: save new tokens, set authenticated = true
6. AuthenticationException: clear tokens, set authenticated = false
7. Other errors: keep tokens (session survives transient errors)

## Error Handling

### Exception Types (`error/NetworkExceptions.kt`)
```kotlin
class AuthenticationException(message: String) : Exception  // 401/403
class ServerException(message: String) : Exception           // 500/502/503
class NetworkException(message: String) : Exception          // Others
```

### HttpErrorMapper (`error/HttpErrorMapper.kt`)
| Status | Exception |
|--------|-----------|
| 401 | AuthenticationException |
| 403 | AuthenticationException |
| 400, 404 | NetworkException |
| 500+ | ServerException |
| Timeout/Connection | NetworkException |

### ApiResponseMapper (`mapper/ApiResponseMapper.kt`)
- Parses `ApiResponse<T>` wrapper from HTTP body
- Checks `success` field
- Extracts `data` (may be null)
- Returns `Try.success(data)` or `Try.failure(error)`

## AuthenticationStateManager (`auth/state/`)
- In-memory `MutableStateFlow<Boolean>`
- Initialized by checking token existence
- Updated by auth operations
