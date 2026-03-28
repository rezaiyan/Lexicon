# Feature: Word Rush Analytics — Backend + Client + InsightsScreen
Created: 2026-03-28
Status: PENDING
Approved: No
Type: Feature

## Goal

Store Word Rush game results on a dedicated backend table (completely separate from `study_sessions`)
and expose aggregated insights via a new `/api/v1/word-rush` endpoint group. Wire the client to sync
results after each game, add a `GetWordRushInsightsUseCase`, extend `InsightsState` with Word Rush
data, and render a dedicated `WordRushInsightsCard` inside `InsightsScreen`.

---

## Architecture

```
WordRushViewModel
    └─ RecordWordRushGameUseCase  (domain)
         └─ IWordRushRecorder     (domain interface)
              └─ WordRushRecorderImpl  (data layer)
                   ├─ SQLDelight word_rush_pending_games  (offline queue)
                   └─ IWordRushDataSource  (data interface)
                        └─ WordRushRemoteDataSource (Ktor)
                             └─ POST /api/v1/word-rush/sync

InsightsViewModel
    └─ GetWordRushInsightsUseCase  (domain)
         └─ IWordRushStatsRepository  (domain interface)
              └─ WordRushStatsRepositoryImpl  (data layer)
                   └─ IWordRushDataSource
                        └─ WordRushRemoteDataSource
                             └─ GET /api/v1/word-rush/insights

InsightsScreen
    └─ WordRushInsightsCard  (shown when wordRushInsights is Loaded with totalGames > 0)
```

No per-question events. One record per game: score, totalQuestions, bestStreak, durationMs,
completedNormally, playedAt. Stats are aggregated server-side.

---

## Tasks

### Backend (lexicon.server)

- [ ] **B1** — `V27__add_word_rush_tables.sql`
  - `word_rush_games(id, user_id FK, client_game_id TEXT UNIQUE per user, score INT, total_questions INT, best_streak INT, duration_ms BIGINT, completed_normally BOOL, played_at BIGINT, created_at TIMESTAMP)`
  - Indexes: `(user_id)`, `(user_id, played_at)`, `(user_id, best_streak)`

- [ ] **B2** — `WordRushGame.kt` JPA entity

- [ ] **B3** — `WordRushGameRepository.kt` (Spring Data)
  - `findByUserAndClientGameId`
  - `countByUser`, `countCompletedByUser`
  - `findTopByUserOrderByBestStreakDesc`
  - `findSumScoreByUser`, `findSumDurationByUser`
  - `findAllByUserOrderByPlayedAtDesc(Pageable)`

- [ ] **B4** — `WordRushDto.kt`
  - `SyncWordRushRequest(games: List<SyncWordRushGameRequest>)`
  - `SyncWordRushGameRequest(clientGameId, score, totalQuestions, bestStreak, durationMs, completedNormally, playedAt)`
  - `SyncWordRushResponse(syncedGameIds: List<String>)`
  - `WordRushInsightsResponse(totalGames, totalCompleted, completionRatePercent, bestStreakEver, avgScore, avgAccuracyPercent, totalTimePlayedMs, avgDurationMs)`
  - `WordRushGameResponse(clientGameId, score, totalQuestions, bestStreak, durationMs, completedNormally, playedAt)`

- [ ] **B5** — `WordRushService.kt`
  - `syncGames(user, request)` — idempotent (skip by `clientGameId`)
  - `getInsights(user)` — aggregate query
  - `getHistory(user, limit)` — recent games ordered by `played_at DESC`

- [ ] **B6** — `WordRushController.kt` at `/api/v1/word-rush`
  - `POST /sync`
  - `GET /insights`
  - `GET /history?limit=20`

- [ ] **B7** — `WordRushControllerTest.kt` (MockMvc, mocked service)

- [ ] **B8** — Integration test in `AnalyticsIntegrationTest` or dedicated `WordRushIntegrationTest.kt`

---

### Client — Domain layer

- [ ] **C1** — `domain/wordrush/model/WordRushGameRecord.kt`
  - `data class WordRushGameRecord(clientGameId, score, totalQuestions, bestStreak, durationMs, completedNormally, playedAt)`

- [ ] **C2** — `domain/wordrush/model/WordRushInsights.kt`
  - `data class WordRushInsights(totalGames, totalCompleted, completionRatePercent, bestStreakEver, avgScore, avgAccuracyPercent, totalTimePlayedMs, avgDurationMs)`

- [ ] **C3** — `domain/wordrush/repository/IWordRushRecorder.kt`
  - `suspend fun recordGame(game: WordRushGameRecord): Try<Unit>`
  - `suspend fun retryPendingSync(): Try<Unit>`

- [ ] **C4** — `domain/wordrush/repository/IWordRushStatsRepository.kt`
  - `suspend fun getInsights(): Try<WordRushInsights>`

- [ ] **C5** — `domain/wordrush/usecase/RecordWordRushGameUseCase.kt` (`UseCase<WordRushGameRecord, Unit>`)

- [ ] **C6** — `domain/wordrush/usecase/GetWordRushInsightsUseCase.kt` (`UseCase<Unit, WordRushInsights>`)

---

### Client — Data layer

- [ ] **C7** — `data/wordrush/remote/IWordRushDataSource.kt`
  - `suspend fun syncGames(games: List<WordRushGameRecord>): Try<Unit>`
  - `suspend fun getInsights(): Try<WordRushInsights>`

- [ ] **C8** — `data/wordrush/remote/WordRushRemoteDataSource.kt` (Ktor)
  - Maps `WordRushGameRecord` → `SyncWordRushGameRequest`, calls backend
  - Maps `WordRushInsightsResponse` → `WordRushInsights`

- [ ] **C9** — SQLDelight: add `word_rush_pending_games.sq` table
  - Columns: `client_game_id TEXT PK, score INT, total_questions INT, best_streak INT, duration_ms INT, completed_normally INT, played_at INT`
  - Queries: `insert`, `selectAll`, `deleteByClientGameId`, `deleteAll`

- [ ] **C10** — `data/wordrush/repository/WordRushRecorderImpl.kt`
  - `recordGame()`: insert to SQLDelight queue → call `dataSource.syncGames()`; on success delete from queue; on failure leave for retry
  - `retryPendingSync()`: load queue → sync → clear successes

- [ ] **C11** — `data/wordrush/repository/WordRushStatsRepositoryImpl.kt`
  - Delegates to `IWordRushDataSource.getInsights()`

---

### Client — ViewModel integration

- [ ] **C12** — `WordRushViewModel`: after `finishGame()` and mid-game `dismiss()`, call `recordWordRushGameUseCase`
  - `finishGame()`: `completedNormally = true`, all fields from game state
  - `dismiss()` mid-game (when at least 1 question answered): `completedNormally = false`

- [ ] **C13** — `InsightsState`: add `wordRushInsights: UiState<WordRushInsights> = UiState.Loading`

- [ ] **C14** — `InsightsAvailability`: add `hasWordRush: Boolean` (true when `totalGames > 0`); include in `hasAnyContent`

- [ ] **C15** — `InsightsViewModel`: add `GetWordRushInsightsUseCase`, call `loadWordRushInsights()` in `loadAllData()`; update `isLoaded` and `isError` guards

- [ ] **C16** — `InsightsScreen.kt`: add `WordRushInsightsCard` composable
  - Placed after `OverviewTab`, before `TrendsTab` — only rendered when `availability.hasWordRush`
  - Shows: total games, best streak, avg accuracy %, avg score/10, total time played
  - Style matches existing `StatCard` + `MetricRow` components; uses `Icons.Rounded.Bolt` accent

---

### Client — DI + strings

- [ ] **C17** — DI: register all new components
  - `InsightsModule`: add `GetWordRushInsightsUseCase`, update `InsightsViewModel` factory
  - `StudyModule` (or new `WordRushModule`): register `RecordWordRushGameUseCase`, `IWordRushRecorder`, `WordRushRecorderImpl`, `IWordRushDataSource`, `WordRushRemoteDataSource`, `IWordRushStatsRepository`, `WordRushStatsRepositoryImpl`

- [ ] **C18** — String resources for the Word Rush card labels

- [ ] **C19** — Tests: `WordRushViewModelTest` additions (game recorded after finish and dismiss), `InsightsViewModelTest` addition

---

## Key Design Decisions

| Decision | Choice | Reason |
|---|---|---|
| Separate table | `word_rush_games` | Different domain — no per-word events, no SRS levels, no `review_type` trick |
| No per-question events | Single game record | Word Rush tracks game-level outcome only |
| Offline-first queue | SQLDelight pending table | Same pattern as `AnalyticsRecorderImpl`; survives app kill |
| Idempotent sync | `clientGameId` unique per user | Safe to retry; client generates UUID per game |
| Separate domain package | `domain/wordrush/` | Clean boundary; never import from `domain/analytics/` |
| Dedicated controller | `/api/v1/word-rush` | Independent lifecycle from `/api/v1/analytics` |
| Insights card position | After Overview, before Trends | Word Rush is a game mode; it sits between study overview and detailed trends |
