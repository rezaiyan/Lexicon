# Plan: Streak-at-Risk Notification

**Status:** PENDING
**Type:** Feature
**Worktree:** No
**Approved:** No

## Goal

Send a push notification when a user hasn't studied today and their streak is at risk of breaking (e.g., 8pm local time if no session recorded that day). This is one of the highest-leverage retention features.

## Context

- `RecordStreakActivityUseCase` — already records streak activity
- `GetProfileStatsUseCase` — returns `currentStreak`
- Notification infrastructure: `platforms/` contains platform-specific notification code — check `NotificationService` or similar
- `feature/study/src/.../formatter/NotificationStringHelper.kt` — already exists, has streak-related strings
- `domain/streak/` — check streak domain models and if "last study date" is tracked
- Background worker / `WorkManager` (Android) or `BGTaskScheduler` (iOS) — check if any scheduling exists
- Daily goal study time from plan 08 (or fallback to 8pm)

## Implementation Tasks

- [ ] **T1** Audit notification infrastructure
  - Find notification service interfaces in `domain/notifications/` or `platforms/`
  - Check if `WorkManager` / background task scheduling is set up
  - Check `NotificationStringHelper` for existing streak strings

- [ ] **T2** Add streak check to `IStreakRepository` or domain: `suspend fun getLastStudyDate(): Try<LocalDate?>`

- [ ] **T3** Create `CheckAndScheduleStreakReminderUseCase`
  - Logic: if `lastStudyDate != today` AND `currentStreak > 0` → schedule notification
  - Notification content: "Don't break your X-day streak! Study now."

- [ ] **T4** Schedule the check
  - Android: `WorkManager` periodic work once per day, fire at 8pm (or user's preferred hour from plan 08)
  - iOS: `BGAppRefreshTask` + `UNUserNotificationCenter` local notification
  - Trigger from app foreground entry or login if no session today

- [ ] **T5** Permission handling
  - Request notification permission on first streak ≥ 3 (don't ask on day 1)
  - Show rationale: "Get reminders before your streak breaks"

- [ ] **T6** Tests
  - Unit test: `CheckAndScheduleStreakReminderUseCase` — no streak = no notification, streak + studied today = no notification, streak + not studied = notification
  - Fake `IStreakRepository` and `INotificationService`

## Files to Modify

| File | Change |
|------|--------|
| `domain/src/.../streak/` | Add `getLastStudyDate()` if missing |
| `domain/src/.../notifications/` | Add `ScheduleStreakReminderUseCase` or similar |
| `platforms/src/androidMain/` | `WorkManager` job implementation |
| `platforms/src/iosMain/` | `BGTaskScheduler` implementation |
| `composeApp/src/commonTest/...` | Streak reminder use case tests |

## Done: 0 / Left: 6
