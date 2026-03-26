---
description: Migrate a file or component from legacy patterns to current Lexicon architecture conventions
argument-hint: "<file path or component name>"
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash", "Agent"]
---

Migrate `$ARGUMENTS` to current Lexicon architecture patterns.

## Step 1: Read and Identify

Read the target file(s). Identify which legacy patterns are present:

| Legacy Pattern | Target Pattern | Skill |
|---|---|---|
| ViewModel extends ViewModel directly | `BaseViewModel<State, Effect>` | `viewmodel-patterns` |
| Sealed `Event`/`Intent` class | Event sink (public methods) | `viewmodel-patterns` |
| `collectAsStateWithLifecycle()` | `viewModel.state()` | `screen-patterns` |
| `LaunchedEffect` for effects | `OnEvents(viewModel.effects)` | `screen-patterns` |
| Multiple `StateFlow` fields | Single `data class` state | `viewmodel-patterns` |
| Bare-throwing suspend methods | `Try<T>` return type | `repository-patterns`, `error-handling` |
| `try-catch` for control flow | `.catch {}` / `Try<T>` | `error-handling` |
| `runCatching` | `Try<T>` | `error-handling` |
| `!!` operator | Safe call / `requireNotNull` | any |
| Use case implements `suspend fun` directly | `UseCase<P,R>` / `FlowUseCase<P,R>` | `usecase-patterns` |
| Repository throws exceptions | `Try<T>` return | `repository-patterns` |
| Stateful use case | Stateless use case | `usecase-patterns` |
| DI in ViewModel body | Constructor injection | `di-patterns` |

## Step 2: Plan the Migration

Before changing any code:

1. List every pattern violation in the file
2. Identify what changes are purely mechanical (rename, wrap in Try) vs. architectural (move logic)
3. Check if tests exist — if yes, they must remain passing throughout
4. Identify any callers of public methods that will change signature

Present the migration plan to the user if it touches more than 3 patterns or if signature changes affect other files.

## Step 3: Migrate Incrementally

Apply changes in this order (least to most disruptive):

1. **Anti-patterns first** (no `!!`, no `try-catch` for control flow) — safe, no API change
2. **State consolidation** (merge fragmented StateFlows → single data class state)
3. **Return types** (bare suspend → `Try<T>`, sealed Event → public methods)
4. **Base class** (ViewModel → BaseViewModel, UseCase interface)
5. **DI registration** (update AppModule.kt if constructor changed)

After each change group, verify the file compiles:
```bash
./gradlew composeApp:compileKotlinMetadata
```

## Step 4: Verify

After migration:
```bash
# Compile check
./gradlew composeApp:compileKotlinMetadata

# Run affected tests
./gradlew composeApp:cleanAllTests composeApp:allTests
```

Then use the `architecture-reviewer` agent to validate:
- No module boundary violations
- All contracts satisfied
- No remaining anti-patterns

## Step 5: Register (if DI changed)

If the constructor changed, update `AppModule.kt`:
- Use cases: `factoryOf(::UseCase)` (not single)
- Repositories: `singleOf(::Impl) { bind<IInterface>() }`
- ViewModels: `viewModelOf(::ViewModel)`

## Rules

- **Preserve behavior** — migration must not change what the app does, only how it's structured
- **One pattern at a time** — don't mix refactoring with new features
- **Keep tests green** — if tests break during migration, fix them before continuing
- **Don't over-migrate** — only fix what's in scope; don't cascade into adjacent files unless necessary
- **Read the skill** for each pattern before applying it — don't guess from memory
