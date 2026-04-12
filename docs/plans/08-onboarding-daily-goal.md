# Plan: Onboarding Study Time & Daily Goal Setup

**Status:** PENDING
**Type:** Feature
**Worktree:** No
**Approved:** No

## Goal

Capture user intent during onboarding: preferred study time and daily word goal. This enables streak-at-risk notifications (plan 09) and personalizes the experience from day one.

## Context

- Onboarding feature: `feature/` or `presentation/` — find `OnboardingViewModel` and screen files
- `ISettingsRepository` — check if `studyTimeHour`, `dailyGoal` settings exist
- `SetStudyReminderUseCase` or notification scheduling — check if exists
- User settings stored where? — check `domain/settings/` for existing settings model
- Current onboarding flow — check how many steps exist, where to inject new step

## Implementation Tasks

- [ ] **T1** Audit existing onboarding flow
  - Find screen count and step structure
  - Identify settings domain models for study time/goal

- [ ] **T2** Add `studyTimeHour: Int?` and `dailyGoalWords: Int` to user settings domain model if not present
  - Add to `IUserSettingsRepository` interface
  - Add Koin binding

- [ ] **T3** Add "When do you want to study?" step to onboarding
  - Time picker (hour selection, e.g. Morning 7am / Afternoon 2pm / Evening 8pm)
  - Store in `IUserSettingsRepository`

- [ ] **T4** Add "Daily goal" step to onboarding
  - Options: 5 / 10 / 20 / 30 words per day
  - Store in `IUserSettingsRepository`

- [ ] **T5** Use `dailyGoalWords` to set review queue size in `LoadReviewQueueUseCase` if configurable

- [ ] **T6** Tests
  - `OnboardingViewModelTest`: verify settings are saved on step completion
  - Verify skipping keeps defaults

## Files to Modify

| File | Change |
|------|--------|
| `domain/src/.../settings/model/UserSettings.kt` | Add new fields if missing |
| `domain/src/.../settings/repository/IUserSettingsRepository.kt` | Add save methods |
| `data/src/.../settings/UserSettingsRepositoryImpl.kt` | Implement persistence |
| Onboarding screen/ViewModel | Add two new steps |
| `composeApp/src/commonTest/...` | New onboarding tests |

## Done: 0 / Left: 6
