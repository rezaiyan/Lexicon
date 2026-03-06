---
description: Review staged/recent changes against project conventions and architecture vision
allowed-tools: ["Bash", "Read", "Glob", "Grep"]
---

Review the current changes (staged + unstaged) against Lexicon's architecture conventions and target patterns.

## Steps

1. Run `git diff` and `git diff --cached` to see all changes
2. Check each changed file against these rules:

### Architecture Boundaries
- Domain module must have NO platform dependencies (no Android, iOS, Ktor, Room imports)
- Data flows: View -> ViewModel -> UseCase -> Repository -> DataSource
- New components must be registered in `composeApp/src/commonMain/kotlin/di/AppModule.kt`

### ViewModel Contract
- New VMs must extend `BaseViewModel<State, Effect>`
- Single `data class` state per screen — no fragmented StateFlows
- Public methods as event sink — no sealed Event/Intent classes
- `updateState { copy(...) }` for state, `emitEffect()` for effects
- `.reduce()` for Try<T> results, `.catch {}` for Flow errors
- Screen reads state via `viewModel.state()` — not `collectAsStateWithLifecycle()`

### UseCase Contract
- Must implement `UseCase<P, R>` or `FlowUseCase<P, R>`
- Suspend returns `Try<T>` — never bare types
- Flow returns `Flow<T>` — never `Flow<Try<T>>`
- Must be stateless — no mutable fields

### Repository Contract
- Suspend methods return `Try<T>` — never throw
- Stream methods return `Flow<T>`
- Data sources should have interfaces in `domain`
- Mappers are extension functions

### Kotlin Code Style
- No `!!` (non-null assertion) — use safe calls, Elvis, or requireNotNull with justification
- No try-catch for control flow — use Flow `catch` operator or Try/sealed types
- No unnecessary `runCatching` wrappers
- Use Kotlin Flow (not LiveData) for reactive state

### Testing
- New ViewModels should have Turbine tests
- New use cases should have tests
- New repositories should have tests with fakes

3. Report findings as:
   - **Violations**: Must-fix issues that break conventions or contracts
   - **Migration**: Code using old patterns that should be updated
   - **Warnings**: Style issues or potential improvements
   - **Good**: Notable well-written code or correct pattern usage
