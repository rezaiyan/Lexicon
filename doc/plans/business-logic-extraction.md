# Business Logic Extraction — Initiative Overview

Audit of ViewModels and screens found domain logic living in the presentation layer.
This initiative extracts that logic into proper `UseCase<P,R>` objects in the domain layer,
updates ViewModels to delegate, and covers each extraction with unit tests.

**Constraint:** `AppNavigationViewModel` state machine (Wave 6) is deferred — see critical-risks.md §4.

---

## Findings Summary

| ViewModel | Severity | Leaked Logic |
|---|---|---|
| `ImportViewModel` | Critical | CSV parsing, CSV generation, error string-matching classifier |
| `AppNavigationViewModel` | Critical | App init state machine, "skip onboarding if has data" rule — **Deferred** |
| `ProfileViewModel` | High | `isToday` computation, day-of-week label, weekly activity sort |
| `ReviewViewModel` | High | Session ID generation, language-code-from-frequency resolution |
| `EditProfileViewModel` | Medium | Alias length + character-set validation |
| `SubscriptionViewModel` | Medium | Hardcoded month-name date formatter |

---

## Tasks

| Task | Status | File |
|---|---|---|
| REFACTOR-1 — Import CSV & Error Logic | Open | [refactor-1-import-csv-logic.md](tasks/refactor-1-import-csv-logic.md) |
| REFACTOR-2 — Alias Validation | Open | [refactor-2-alias-validation.md](tasks/refactor-2-alias-validation.md) |
| REFACTOR-3 — Profile Stats Enrichment | Open | [refactor-3-profile-enrichment.md](tasks/refactor-3-profile-enrichment.md) |
| REFACTOR-4 — Review Session Helpers | Open | [refactor-4-review-session-helpers.md](tasks/refactor-4-review-session-helpers.md) |
| REFACTOR-5 — Epoch Date Formatter | Open | [refactor-5-date-formatter.md](tasks/refactor-5-date-formatter.md) |
| REFACTOR-6 — App Init State Machine | Deferred | [refactor-6-app-init-state-machine.md](tasks/refactor-6-app-init-state-machine.md) |

---

## Execution Order

Work bottom-up within each wave. Compile after each:

```bash
./gradlew composeApp:compileKotlinMetadata
```

Full test run after all waves:

```bash
./gradlew composeApp:cleanAllTests composeApp:allTests
```

**Wave order:** REFACTOR-1 → REFACTOR-2 → REFACTOR-3 → REFACTOR-4 → REFACTOR-5 → REFACTOR-6 (deferred)

Waves 1–5 are independent and can be worked in parallel by separate branches.
Wave 6 requires its own planning session before implementation begins.
