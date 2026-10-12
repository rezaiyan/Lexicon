# Comeback Rescue — P2 Client Implementation Plan

> **For agentic workers:** Use claude-kit:implement to execute this plan task-by-task.

**Goal:** Tapping a comeback-rescue push opens a review of exactly the AI-picked words in the planned order, shows the AI outro at the end, and tells the server the rescue was completed.

**Architecture:** One new `ReviewSource.ComebackRescue` carries rescue id, ordered word ids and the two outros from the push payload. `NotificationTapHandler` builds it from `vokab://review/rescue?ids=…&rid=…`. `LoadReviewQueueUseCase` loads those words in order (no focus filter, no daily-goal cap). `ReviewViewModel` picks the outro and fires `ReportRescueCompletedUseCase` once on completion. No new screen.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Koin, Ktor (MockEngine in tests), kotlin-test + Turbine, hand-written fakes.

**Design doc:** `.claude/plans/2026-10-12-ai-comeback-rescue.md` §4. Backend contract from P1 (`docs/plans/2026-10-12-comeback-rescue-p1-backend.md`):
- Push data: `type=comeback_rescue`, `deep_link=vokab://review/rescue?ids=5,1,20,3&rid=42`, `rid=42`, optional `outro_win`, `outro_soft`, plus `notification_log_id`.
- `POST /api/v1/notifications/rescue/{rid}/complete` → `ApiResponse<Unit>`; idempotent; 404 for unknown/foreign.

**Decisions made while reading the client:**
- `sessionTypeLabel = "REVIEW"` (not a new `"RESCUE"` label): the backend's session analytics expect the existing labels; the rescue is tracked by its own `complete` call.
- No learning-focus filter for rescue words: they were chosen for this learner from their dominant language pair; filtering by a different focus would empty the session.
- Outro choice: `outroWin` when every card was answered right (`unknownCount == 0`), else `outroSoft`. The client doesn't know which words were "weak", and "8 for 8"-style copy is only true for a clean run.
- Old clients receiving the new link already fall back to the due review (`startsWith("vokab://review/")`) — nothing to do.

---

### Task 1: `ReviewSource.ComebackRescue` + ordered loading

**Files:**
- Modify: `domain/src/commonMain/kotlin/domain/word/model/ReviewSource.kt`
- Modify: `domain/src/commonMain/kotlin/domain/word/usecase/LoadReviewQueueUseCase.kt`
- Modify: `feature/study/src/commonMain/kotlin/feature/study/ReviewViewModel.kt` (`toReviewType`)
- Test: `composeApp/src/commonTest/kotlin/domain/word/usecase/LoadReviewQueueUseCaseTest.kt`

- [ ] Failing test in `LoadReviewQueueUseCaseTest`:
```kotlin
@Test
fun `ComebackRescue keeps the planned order, skips missing words and ignores focus and daily goal`() = runTest {
    val words = (1..5).map { testWord(it) }
    val repo = ConfigurableWordRepository(allWords = words)
    val useCase = buildUseCase(repo, dailyGoal = 1)

    val result = useCase(ReviewSource.ComebackRescue(rescueId = 42L, wordIds = listOf(4L, 99L, 2L, 5L)))

    assertEquals(listOf(words[3], words[1], words[4]), result.getOrNull())
}
```
- [ ] Run `./gradlew composeApp:allTests --tests "*LoadReviewQueueUseCaseTest*"` (or `composeApp:testDebugUnitTest --tests ...`) → FAIL (unresolved `ComebackRescue`).
- [ ] Add to `ReviewSource`:
```kotlin
    /**
     * A comeback rescue from a push: the words a nightly plan picked for this learner, in session
     * order (easy wins first and last). Not filtered by learning focus or capped by the daily goal.
     */
    data class ComebackRescue(
        val rescueId: Long,
        val wordIds: List<Long>,
        val outroWin: String? = null,
        val outroSoft: String? = null,
    ) : ReviewSource() {
        override val sessionTypeLabel: String = "REVIEW"
    }
```
- [ ] Add branch in `LoadReviewQueueUseCase`:
```kotlin
            is ReviewSource.ComebackRescue -> {
                val byId = wordRepository.getAllWords().first().associateBy { it.id.toLong() }
                params.wordIds.mapNotNull { byId[it] }
            }
```
- [ ] Add `is ReviewSource.ComebackRescue` to the `REVIEW` group of `toReviewType()` in `ReviewViewModel`; fix any other non-exhaustive `when (source)` the compiler reports the same way as `ByWords`.
- [ ] Test → PASS. Commit `feat(rescue): comeback rescue review source with ordered words`.

### Task 2: Report completion (domain + data)

**Files:**
- Modify: `domain/src/commonMain/kotlin/domain/notifications/repository/INotificationEngagementRepository.kt`
- Create: `domain/src/commonMain/kotlin/domain/notifications/usecase/ReportRescueCompletedUseCase.kt`
- Modify: `data/src/commonMain/kotlin/data/notification/remote/IPushNotificationDataSource.kt`, `PushNotificationDataSource.kt`
- Modify: `data/src/commonMain/kotlin/data/notification/repository/NotificationEngagementRepositoryImpl.kt`
- Modify: `composeApp/src/commonMain/kotlin/di/NotificationModule.kt`
- Modify (fakes): `FakeNotificationEngagementRepository` in `ReportNotificationOpenedUseCaseTest.kt`, any `IPushNotificationDataSource` fake (e.g. in `PushTokenRepositoryImplTest.kt`)
- Test: `composeApp/src/commonTest/kotlin/domain/notifications/usecase/ReportRescueCompletedUseCaseTest.kt`, `PushNotificationDataSourceTest.kt`

- [ ] Failing tests: use case delegates the id to the repository and returns its result; data source POSTs to `.../notifications/rescue/42/complete` and maps `success=false` to failure.
- [ ] `INotificationEngagementRepository.reportRescueCompleted(rescueId: Long): Try<Unit>` (KDoc: tells the backend the learner finished a comeback rescue session).
- [ ] `ReportRescueCompletedUseCase(repository) : UseCase<Long, Unit>` → `repository.reportRescueCompleted(params)`.
- [ ] `IPushNotificationDataSource.reportRescueCompleted(rescueId: Long): Try<Unit>`; impl mirrors `reportNotificationOpened` with `appendPathSegments("rescue", rescueId.toString(), "complete")`.
- [ ] Repository impl delegates. DI: `factoryOf(::ReportRescueCompletedUseCase)` next to `ReportNotificationOpenedUseCase` registration.
- [ ] Tests → PASS. Commit `feat(rescue): report finished comeback rescue sessions`.

### Task 3: Deep link → rescue review

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/notification/NotificationTapHandler.kt`
- Test: `composeApp/src/commonTest/kotlin/notification/NotificationTapHandlerTest.kt`

- [ ] Failing tests:
  - rescue link with `rid` + outros in data → `reviewRequests` emits `ComebackRescue(42, [5,1,20,3], "win", "soft")`, destination `Study`, open reported.
  - malformed ids (`ids=a,,7,-1`) → only `[7]`.
  - no valid ids or missing `rid` → due review (`ReviewSource.DueCards`).
  - more than 20 ids → first 20.
- [ ] Branch placed **before** the generic `vokab://review/` check:
```kotlin
link.startsWith(RESCUE_LINK_PREFIX) -> openRescue(link, data)
```
```kotlin
    /** A comeback rescue: review exactly the planned words; anything unusable falls back to due review. */
    private fun openRescue(link: String, data: Map<String, String>) {
        val query = link.substringAfter('?', "").split('&')
            .mapNotNull { it.split('=', limit = 2).takeIf { kv -> kv.size == 2 } }
            .associate { (k, v) -> k to v }
        val rescueId = (query["rid"] ?: data[RESCUE_ID_KEY])?.toLongOrNull()
        val wordIds = query["ids"].orEmpty().split(',')
            .mapNotNull { it.trim().toLongOrNull()?.takeIf { id -> id > 0 } }
            .take(MAX_RESCUE_WORDS)
        if (rescueId == null || wordIds.isEmpty()) {
            navigator.openDueReview()
            return
        }
        navigator.openReview(
            ReviewSource.ComebackRescue(rescueId, wordIds, data[OUTRO_WIN_KEY], data[OUTRO_SOFT_KEY])
        )
    }
```
  `openDeepLink` needs `data`: change its signature to `openDeepLink(link: String, data: Map<String, String>)`.
- [ ] Constants: `RESCUE_LINK_PREFIX = "vokab://review/rescue"`, `RESCUE_ID_KEY = "rid"`, `OUTRO_WIN_KEY = "outro_win"`, `OUTRO_SOFT_KEY = "outro_soft"`, `MAX_RESCUE_WORDS = 20`.
- [ ] Tests → PASS. Commit `feat(rescue): open comeback rescue reviews from the push deep link`.

### Task 4: Outro + completion report in the review

**Files:**
- Modify: `feature/study/src/commonMain/kotlin/feature/study/ReviewViewModelState.kt` (`Completed.outro: String? = null`)
- Modify: `feature/study/src/commonMain/kotlin/feature/study/ReviewViewModel.kt`
- Modify: `feature/study/src/commonMain/kotlin/feature/study/ui/review/ReviewScreen.kt`, `ReviewCompletionContent.kt`
- Modify: `composeApp/src/commonTest/kotlin/presentation/feature/study/ReviewViewModelTest.kt` (constructor + new tests)

- [ ] Failing ViewModel tests (Turbine/state):
  - rescue source, all cards known → `Completed.outro == outroWin`, `reportRescueCompleted` called once with 42.
  - rescue source, one unknown → `outro == outroSoft`.
  - rescue without outros → `outro == null`, still reported.
  - `ByWords` source → no report, `outro == null`.
  - abandoned rescue session → no report.
- [ ] `ReviewViewModel` gets `private val reportRescueCompleted: ReportRescueCompletedUseCase`; stores `private var rescue: ReviewSource.ComebackRescue? = null` set in `startSession` (`source as? ReviewSource.ComebackRescue`). In `completeSession`, after the streak:
```kotlin
        val outro = rescue?.let { if (unknownCount == 0) it.outroWin else it.outroSoft }
        rescue?.let { reportRescueCompleted(it.rescueId) } // best-effort: a lost report only skews the funnel
        rescue = null
```
  and pass `outro` into `ReviewState.Completed`.
- [ ] `ReviewCompletionContent(outro: String?, …)`: when non-null, show it under the tier message as `bodyLarge`, `onSurface`, centered; screen passes `reviewState.outro`.
- [ ] Run `analytics-auditor` agent on the `ReviewViewModel` change.
- [ ] Tests → PASS. Commit `feat(rescue): show the rescue outro and report completion`.

### Task 5: Verify

- [ ] `./gradlew composeApp:cleanAllTests composeApp:allTests` → 0 failures.
- [ ] `./gradlew composeApp:compileKotlinMetadata composeApp:assembleDebug` → BUILD SUCCESSFUL.
- [ ] On emulator: `adb shell am start -W -a android.intent.action.VIEW -d "vokab://review/rescue?ids=<3 real ids>&rid=1"` is not routed (deep links come via push data), so verify through the unit tests + a push from the admin manual-push endpoint with the rescue data payload once P1 is deployed (P3 Task covers the end-to-end send).
- [ ] Push to `main`.
