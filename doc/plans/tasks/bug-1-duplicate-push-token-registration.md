# BUG-1 — Duplicate `POST /notifications/register-token` on Login

**Priority:** P2
**Status:** Open

## Observed Behaviour

Two identical `POST /notifications/register-token` calls fire 13ms apart immediately after login:

```
→ POST  https://api.vokab.app/notifications/register-token   (09:33:17.864)
→ POST  https://api.vokab.app/notifications/register-token   (09:33:17.877)
```

Both carry the same token — the second call has no effect and is pure waste.

## Root Cause

`PushTokenRepositoryImpl.initializeAndRegister()` opens two concurrent registration paths:

1. **Immediate path** — registers the current cached token right away → call #1
2. **Callback path** — registers a listener; when the token manager returns the same token, the callback fires and registers again → call #2

Both paths resolve to the same token, so call #2 is always redundant.

**Files:**
- `data/src/commonMain/kotlin/data/notification/repository/PushTokenRepositoryImpl.kt:22-39`
- `domain/src/commonMain/kotlin/domain/notifications/usecase/RegisterPushTokenUseCase.kt`

## Fix

Choose one:

**Option A (simplest):** Remove the immediate registration call. Only register inside the token-ready callback. This ensures a single call regardless of token availability timing.

**Option B (deduplication):** Keep both paths but track the last successfully registered token. Before each registration call, compare the pending token to `lastRegisteredToken`. Skip the network call if they match.

Option A is preferred — it removes the race condition entirely.

## Acceptance Criteria

- Exactly **1** `POST /notifications/register-token` fires per login
- No change to registration behaviour when the token changes (e.g. FCM token rotation)
