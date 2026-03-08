---
description: Review staged/recent changes against project conventions and architecture vision
allowed-tools: ["Bash", "Read", "Glob", "Grep"]
---

Review the current changes (staged + unstaged) against Lexicon's architecture conventions.

## Steps

1. Run `git diff` and `git diff --cached` to see all changes
2. Check each changed file against the rules below

## What to Check

### Architecture Boundaries
- Domain module: NO platform dependencies (no Android, iOS, Ktor, Room, Compose imports)
- Data flow direction: View -> ViewModel -> UseCase -> Repository -> DataSource
- `presentation` never imports from `data` directly
- New components registered in `AppModule.kt`

### Target Pattern Compliance
- **ViewModel**: extends `BaseViewModel<State, Effect>`, single data class state, event sink methods, `updateState`/`emitEffect`/`.reduce()`, `.catch {}` for Flows
- **Screen**: `viewModel.state()` (not collectAsState), `OnEvents`, `LexiconColumn`, content composable with data+lambdas
- **UseCase**: implements `UseCase<P,R>` or `FlowUseCase<P,R>`, suspend returns `Try<T>`, Flow returns `Flow<T>`, stateless
- **Repository**: interface in domain, impl in data, suspend returns `Try<T>`, stream returns `Flow<T>`

### Anti-patterns (flag these)
- `!!`, try-catch for control flow, unnecessary `runCatching`
- Sealed Event/Intent classes, `collectAsStateWithLifecycle()`, fragmented StateFlows
- Stateful use cases, bare-throwing suspend methods

### Testing
- New ViewModels should have Turbine tests
- New use cases should have tests
- New repositories should have tests with fakes

3. Report findings as:
   - **Violations**: Must-fix issues that break conventions
   - **Migration**: Code using old patterns that should be updated
   - **Warnings**: Style issues or potential improvements
   - **Good**: Notable well-written code or correct pattern usage
