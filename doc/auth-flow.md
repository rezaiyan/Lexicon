# Authentication Flow

## Login Flow

```
User taps "Sign in with Google/Apple"
    │
    ├── Google: KMPAuth → Firebase Auth → ID Token
    │    └── AuthViewModel.loginWithGoogle(idToken)
    │
    └── Apple: AppleSignInHelper → NSNotification bridge → ID Token
         └── AuthViewModel.loginWithApple(idToken, fullName, appleUserId)

    ↓

AuthViewModel processes intent:
    │
    ├── LoginWithGoogleUseCase(idToken) → AuthenticationService → AuthRepository
    │    └── POST /auth/google {idToken}
    │
    └── LoginWithAppleUseCase(idToken, fullName, appleUserId) → AuthenticationService → AuthRepository
         └── POST /auth/apple {idToken, fullName?, appleUserId?}

    ↓

Backend returns AuthResponse:
    {accessToken, refreshToken, tokenType, expiresIn, user: UserDto}

    ↓

TokenManager.saveTokens(accessToken, refreshToken, expiresInMs)
SessionManager.setAuthenticated(true)
UserManager.setUser(authUser)
SubscriptionManager.logIn(userId)
InitializePushNotificationsUseCase()
AnalyticsTracker.logEvent("login")
```

## Session Verification (App Launch)

```
AuthViewModel.verifyAndRestoreSession()
    │
    ├── Check: tokenManager.hasTokens()?
    │    ├── No → setAuthenticated(false) → AuthGate
    │    └── Yes → VerifySessionUseCase()
    │              │
    │              ├── GET /users/me (with current access token)
    │              │
    │              ├── Valid → setAuthenticated(true), setUser(user) → Ready
    │              ├── Expired → clearTokens(), setAuthenticated(false) → AuthGate
    │              ├── NotAuthenticated → setAuthenticated(false) → AuthGate
    │              └── ServerError → keep tokens, assume valid → Ready
    │                   (session survives transient errors)
```

## Token Refresh

### Proactive Refresh (AuthInterceptor)
- Before each request, check if token expires within **5 minutes**
- If yes: call `TokenRefreshManager.refresh()` before making request
- Skips auth endpoints (paths containing `/auth/`)

### Reactive Refresh (RefreshAndRetryInterceptor)
- Triggers on 401/403 response
- Must have Authorization header, not be a retry, not be auth endpoint
- Flow:
  1. Mutex lock (single-flight: only one refresh at a time)
  2. Check if another caller already refreshed → reuse new token
  3. POST /auth/refresh {refreshToken}
  4. Success: save new tokens, retry original request
  5. AuthenticationException: clear tokens, set authenticated = false
  6. Other errors: keep tokens, return failure
- Edge case: refresh succeeds but retry still 401/403 → clear session (account deleted/banned)

## Logout Flow

```
ProfileViewModel.onEvent(Logout)
    │
    └── LogoutUseCase()
         │
         ├── 1. wordRepository.deleteAllWords()  (local)
         ├── 2. settingsRepository.clearSettings() (local)
         └── 3. authenticationService.logout()
              │
              ├── POST /auth/logout {refreshToken}  (best effort)
              ├── Push: deactivateAllTokens()
              ├── Google: signOutFromGoogle()
              ├── Apple: signOutFromApple()
              ├── Subscription: logOut()
              ├── TokenManager.clearTokens()
              └── SessionManager.setAuthenticated(false)

         Note: Succeeds even if API call fails
```

## Account Deletion Flow

```
ProfileViewModel.onEvent(DeleteAccount)
    │
    └── DeleteAccountUseCase()
         │
         ├── 1. DELETE /auth/delete-account  (MUST succeed)
         ├── 2. wordRepository.deleteAllWords()
         ├── 3. settingsRepository.clearSettings()
         └── 4. authenticationService.logout() (cleanup)
```

## Remote Account Deletion (Push Notification)
When server deletes account:
1. Push notification with type "account_deleted" arrives
2. `AccountDeletionHandler.handle()` triggered
3. `ClearAllUserDataUseCase()`:
   - deleteAllWords()
   - clearSettings()
   - clearTokens()
   - setAuthenticated(false)

## Token Storage
- **Android**: EncryptedSharedPreferences (MasterKey AES256_GCM)
- **iOS**: Keychain (Security framework) with migration from NSUserDefaults
- **Web**: localStorage (not truly secure)

Keys: `KEY_ACCESS_TOKEN`, `KEY_REFRESH_TOKEN`, `KEY_TOKEN_EXPIRES_AT`
