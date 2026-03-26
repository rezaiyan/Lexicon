# API Request Audit — Debug Plan

Track every API call made per screen/flow to catch over-fetching, redundant calls, and unexpected requests.

---

## How to Read Logs

All HTTP calls emit two log lines via `LoggingTimingInterceptor` under the tag `HTTP`:

```
→ GET  https://api.vokab.app/words          ← outgoing request
← 200  /words (142ms)                        ← response
```

**Android (Logcat filter):**
```
tag:HTTP | tag:Network
```

**iOS (Console.app / Xcode console filter):**
```
HTTP
```

Count `→` lines per scenario — that is the number of API calls made.

---

## Audit Scenarios

Each scenario is a row in the findings table below. Work through them in order.

### Legend
- **Expected** = minimum calls needed for this action (no redundancy)
- **Actual** = what you observe in logs
- **Status** = OK / OVER-FETCH / BUG

---

## Scenario Table

| # | Screen / Action | Expected calls | Actual calls | Endpoints seen | Status | Notes |
|---|---|---|---|---|---|---|
| 1 | **Cold launch — not logged in** | 0–1 (session check only) | | | | |
| 2 | **Cold launch — already logged in** | 1–3 (session verify + initial data) | | | | |
| 3+4 | **Login (Google) + land on main screen** | 4 (auth + words + notif-token + feature-access) | **6** | POST /auth/google, GET /words ×2, POST /notif-token ×2, GET /feature-access | **BUG** | Two known bugs — see Findings |
| 5 | **Settings tab — open** | 1 (GET /feature-access) | **1** | GET /feature-access | OK | But see BUG-3: cold flow issue |
| 5b | **Word Manager sheet — open** | 0 (words from local DB) | **1** | GET /feature-access | **BUG** | WordManagerVM re-fetches feature-access; words correctly served from local DB ✅ |
| 6 | **Profile screen — open** | 3 (streak + feature-access + profile-stats) | **4** | GET /streak, GET /feature-access, GET /profile-stats ×2 | **BUG** | profile-stats fires twice; feature-access BUG-3 confirmed again |
| 6 | **Words screen — scroll / paginate** | 1 per page | | | | |
| 7 | **Words screen — add new word** | 1 (POST /words) | | | | |
| 8 | **Words screen — edit word** | 1 (PATCH /words/{id}) | | | | |
| 9 | **Words screen — delete word** | 1 (DELETE /words/{id}) | | | | |
| 10 | **Study/Review screen — open** | 1–2 (load session + due cards) | **0** | — | OK | Words served from local DB ✅ |
| 11 | **Study/Review screen — answer card** | 0 (local only, batched) | **1 per card** | PATCH /words/{id} per answer | **BUG** | Should be local-only; batch and flush on session close — see BUG-5 |
| 12 | **Study/Review screen — finish session** | 1–2 (sync analytics session) | **1** | POST /analytics/sync | OK | Fires on session close ✅ |
| 13 | **Profile screen — open** | 1–2 (GET profile + stats) | | | | |
| 14 | **Profile screen — edit name/avatar** | 1 (PATCH profile) | | | | |
| 15 | **Leaderboard screen — open** | 1 (GET leaderboard) | **1** | GET /leaderboard | OK | |
| 16 | **Leaderboard screen — pull to refresh** | 1 | | | | |
| 17 | **Insights screen — open** | 6 (insights + daily-stats + difficult-words + accuracy-by-level + heatmap + accuracy-by-hour) | **12** | All 6 endpoints ×2 (70ms apart) | **BUG** | Every endpoint fires twice — same double-trigger pattern as BUG-4; see BUG-6 |
| 18 | **Insights screen — change time range** | 1–2 (refetch affected data) | | | | |
| 19 | **AI Import — open screen** | 0 (local) | **1** | GET /feature-access | **BUG** | Import ViewModel cold-subscribes feature-access on open — BUG-3 (subscriber #5) |
| 20 | **AI Import — submit image** | 1 (POST /ai/extract-vocabulary) | **1** | POST /ai/extract-vocabulary (117KB, 8.5s) | OK | ✅ Correct endpoint and single call |
| 21 | **AI Import — confirm import** | 1 (POST /words) | **2** | POST /words, GET /words (3ms later) | **BUG** | GET /words redundant re-sync after create — BUG-8 |
| 21b | **Text Import — open + process + confirm** | 2 (AI call + POST /words) | **3** | POST /onboarding/preferences (19s AI), POST /words (23KB), GET /words | **BUG** | GET /words redundant re-sync (BUG-8); odd endpoint name for AI processing |
| 22 | **Settings screen — open** | 0 (local prefs only) | **1** | GET /feature-access | **BUG** | BUG-3 confirmed again — SettingsViewModel cold-subscribes, fires a fresh network call |
| 23 | **App background → foreground (30s gap)** | 0–1 (token refresh if near expiry) | | | | |
| 24 | **App background → foreground (long gap)** | 1–3 (re-auth + refresh data) | | | | |
| 25 | **Logout** | 2 (DELETE /notif-token + POST /auth/logout) | **3** | DELETE /notifications/tokens, POST /auth/logout, GET /words → 401 | **BUG** | Spurious unauthenticated GET /words fires 165ms after logout — see BUG-7 |

---

## How We Work Through This

1. I share the scenario number and action.
2. You open logcat/Xcode console with filter `HTTP`, **clear the log**, perform the action.
3. Paste the log output (or just count of `→` lines + endpoints) back here.
4. I fill in the findings table and flag any anomalies.
5. Repeat for next scenario.

---

## Findings & Anomalies

### BUG-1 — `POST /notifications/register-token` fires twice (13ms apart)

**Observed:** Two identical calls at 09:33:17.864 and 09:33:17.877.

**Root cause:** `PushTokenRepositoryImpl.initializeAndRegister()` does two things simultaneously:
1. Immediately registers the current cached token → **call #1**
2. Registers a listener callback that fires when the token manager returns the same token → **call #2**

Both paths hit the same token, so call #2 is pure waste.

**Files:**
- `data/src/commonMain/kotlin/data/notification/repository/PushTokenRepositoryImpl.kt:22-39`
- `domain/src/commonMain/kotlin/domain/notifications/usecase/RegisterPushTokenUseCase.kt`

**Fix:** In `initializeAndRegister()`, skip the immediate register call and only register via the callback. Or deduplicate by checking whether the token in the callback matches the last-registered token — if it does, skip the network call.

---

### BUG-2 — `GET /words` fires twice (300ms apart)

**Observed:** First at 09:33:17.691 (right after auth), second at 09:33:17.997 (when home screen renders).

**Root cause:** Two independent code paths both call `syncWithRemote()` → `WordRemoteDataSource.getWords()`:

1. **Auth path:** `AuthViewModel.onLoginSuccess()` → `HandleLoginSuccessUseCase` → `SyncRemoteToLocalUseCase` → `syncWithRemote()` → `GET /words`
   - File: `domain/src/commonMain/kotlin/domain/auth/usecase/HandleLoginSuccessUseCase.kt:28`

2. **Screen path:** `StudyProgressViewModel.init` → `getProgressStats()` → `launch { syncWithRemote() }` → `GET /words`
   - File: `feature/study/src/commonMain/kotlin/feature/study/StudyProgressViewModel.kt:55-57`
   - File: `data/src/commonMain/kotlin/data/word/repository/WordRepositoryImpl.kt:207-210`

The post-login sync already fetches fresh words, so the StudyProgressViewModel background sync 300ms later is redundant.

**Fix:** After a fresh login sync completes, set a "just synced" flag or timestamp in the repository. `getProgressStats()` should skip `syncWithRemote()` if data was synced within the last N seconds.

---

---

### BUG-3 — `GET /users/feature-access` fires once per ViewModel that uses it

**Observed:** Fires when Settings tab opens (SettingsViewModel) and again when Word Manager sheet opens (WordManagerViewModel). Will fire again when Profile screen opens (ProfileViewModel).

**Root cause:** `GetFeatureAccessUseCase` calls `authRepository.getFeatureAccessAsFlow()` which calls `apiClient.getFlowNotNull(path)` — a cold `flow { emit(get(...)) }`. Every subscriber to this cold flow triggers a fresh HTTP request. There is no shared/cached layer.

**All 4 subscribers (each = one GET /feature-access):**
1. `StudyProgressViewModel` — on app launch
2. `SettingsViewModel` — on Settings tab open
3. `WordManagerViewModel` — on Word Manager sheet open
4. `ProfileViewModel` — on Profile screen open

**Files:**
- `data/src/commonMain/kotlin/data/core/network/client/ApiClient.kt:253-258` (`getFlowNotNull` — cold flow)
- `data/src/commonMain/kotlin/data/auth/repository/AuthRepositoryImpl.kt:122`
- `data/src/commonMain/kotlin/data/auth/remote/FeatureAccessRemoteDataSource.kt:21`

**Fix:** Add an in-memory cache in `AuthRepositoryImpl` (or `FeatureAccessRemoteDataSource`). On first call, fetch from the network and store the result. Subsequent calls return the cached value without hitting the network. Invalidate on logout or explicit refresh.

---

---

### BUG-4 — `GET /users/profile-stats` fires twice on Profile screen open

**Observed:** Two calls 44ms apart at 09:45:32.799 and 09:45:32.843.

**Root cause:** Double trigger:
1. `ProfileViewModel.init` line 61 → `viewModelScope.launch { loadProfileStats() }`
2. `ProfileScreen.kt:47` → `LaunchedEffect(Unit) { profileViewModel.refreshProfileStats() }` → `loadProfileStats()`

Both fire on first screen entry. The `ThrottledAction` (60s interval) does not block the first invocation, so both calls go through.

**Files:**
- `feature/profile/src/commonMain/kotlin/feature/profile/ProfileViewModel.kt:61`
- `feature/profile/src/commonMain/kotlin/feature/profile/ui/ProfileScreen.kt:47`

**Fix:** Remove `viewModelScope.launch { loadProfileStats() }` from `ProfileViewModel.init`. The `LaunchedEffect(Unit)` in the screen already handles the initial load on every visit. The ViewModel init does not need to pre-fetch.

---

### BUG-8 — `GET /words` fires immediately after AI import confirm (`POST /words`)

**Observed:** `POST /words` at 10:14:58.689 (23KB batch, 200 OK), then `GET /words` at 10:14:58.825 — 3ms later.

**Root cause:** After saving the imported words, the repository calls `syncWithRemote()` to refresh the local cache. But the `POST /words` response already contains the created words — there is no need to re-fetch the entire word list from the server immediately after.

**Fix:** After a successful `POST /words` batch, update the local DB directly from the response body. Skip `syncWithRemote()` — or set the "just synced" timestamp (same mechanism as the BUG-2 fix) so the follow-up sync is suppressed.

---

### BUG-7 — `GET /words` fires unauthenticated (401) immediately after logout

**Observed:** 165ms after `POST /auth/logout` completes, `GET /words` fires with no `Authorization` header → 401.

**Root cause:** Logout clears the auth token, which causes a state change that is observed by something that triggers `syncWithRemote()` — likely `StudyProgressViewModel` re-initializing when navigation returns to the home/study screen post-logout, or a Flow collector that reacts to the user state change and fires a background sync without checking auth state first.

**Files to investigate:**
- `feature/study/src/commonMain/kotlin/feature/study/StudyProgressViewModel.kt` — `init` calls `getProgressStats()` → `launch { syncWithRemote() }`
- `data/src/commonMain/kotlin/data/word/repository/WordRepositoryImpl.kt:207-210` — `syncWithRemote()` calls `GET /words`

**Fix:** `syncWithRemote()` (or its callers) must check that the user is authenticated before firing the network call. Skip sync if no valid auth token is present.

---

### BUG-6 — All 6 Insights endpoints fire twice on screen open

**Observed:** 12 requests total (6 endpoints × 2). First batch at 09:53:44.562–.596, second batch at 09:53:44.632–.643 (70ms later).

**Root cause:** Same double-trigger pattern as BUG-4:
1. `InsightsViewModel.init:66` → `loadAllData()` → fires all 6 requests
2. `InsightsScreen.kt:104` → `LaunchedEffect(Unit) { viewModel.refresh() }` → `loadAllData()` again → fires all 6 again

**Files:**
- `feature/insights/src/commonMain/kotlin/feature/insights/InsightsViewModel.kt:65-67`
- `feature/insights/src/commonMain/kotlin/feature/insights/ui/InsightsScreen.kt:104-106`

**Fix:** Remove `loadAllData()` from `InsightsViewModel.init`. The `LaunchedEffect(Unit)` in the screen already handles the initial load. This is the same fix as BUG-4.

---

### BUG-5 — `PATCH /words/{id}` fires once per card answer instead of batching

**Observed:** 3 PATCH calls, one per card reviewed, at 09:50:03.679, 09:50:07.162, 09:50:08.999. Then `POST /analytics/sync` at 09:50:13.586 on session close.

**Root cause:** Each card answer immediately triggers a network PATCH to sync SRS state. Card answers should be accumulated locally during the session and flushed in a single batch when the session closes — at the same time as `POST /analytics/sync`.

**Fix:** Buffer card answer results in memory during the review session. On session close, batch all pending PATCH /words updates (or a single POST /words/batch-update endpoint) and send together with the analytics sync.

---

### Over-fetch candidates
- `GET /users/feature-access` — up to 4× per session (BUG-3)
- `GET /words` — 2× on login (BUG-2)

### Redundant calls
- `GET /words` — second call is redundant (data just fetched 300ms prior)
- `POST /notifications/register-token` — second call is same token, no value
- `GET /users/feature-access` — same data fetched independently by each ViewModel

### Unexpected calls
_None so far_

---

## Action Items

| Priority | Issue | File | Fix |
|---|---|---|---|
| P1 | `GET /feature-access` once per ViewModel (up to 4×/session) | `AuthRepositoryImpl.kt:122`, `FeatureAccessRemoteDataSource.kt` | Add in-memory cache — fetch once, return cached value on subsequent calls, invalidate on logout |
| P1 | Duplicate `GET /words` on login | `WordRepositoryImpl.kt:210`, `HandleLoginSuccessUseCase.kt:28` | Skip `syncWithRemote()` in `getProgressStats()` if synced within last N seconds |
| P1 | Duplicate `GET /users/profile-stats` on Profile open | `ProfileViewModel.kt:61`, `ProfileScreen.kt:47` | Remove `loadProfileStats()` from `init`; let `LaunchedEffect` drive the initial load |
| P1 | `GET /words` fires immediately after AI import confirm | Import confirm use case / repository | Update local DB from POST response; skip `syncWithRemote()` after batch create |
| P1 | `GET /words` fires unauthenticated (401) on logout | `StudyProgressViewModel.kt`, `WordRepositoryImpl.kt:207-210` | Guard `syncWithRemote()` — skip if no valid auth token |
| P1 | All 6 Insights endpoints fire twice on screen open | `InsightsViewModel.kt:65-67`, `InsightsScreen.kt:104` | Remove `loadAllData()` from `init`; let `LaunchedEffect` drive initial load (same fix as BUG-4) |
| P1 | `PATCH /words/{id}` fires per card answer (N calls per session) | Review session ViewModel / use case | Buffer answers locally; batch-flush on session close alongside `POST /analytics/sync` |
| P2 | Duplicate `POST /notifications/register-token` | `PushTokenRepositoryImpl.kt:22-39` | Only register via callback; skip immediate call, or deduplicate by last-registered token |
