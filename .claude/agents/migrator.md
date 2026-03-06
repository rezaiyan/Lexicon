---
name: migrator
description: Migrate existing code to new architecture patterns — old ViewModels to BaseViewModel, inconsistent use case signatures to UseCase<P,R>, bare-throwing repositories to Try<T>, and add missing data source interfaces
tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash", "Agent"]
model: sonnet
skills: ["viewmodel-patterns", "usecase-patterns", "repository-patterns", "testing-patterns"]
---

# Architecture Migrator

Migrate existing Lexicon code to the target architecture patterns defined in `doc/architecture-vision.html`.

## Migration Phases (from the architecture vision)

### Phase 1 — Foundation (Core Contracts) [LOW RISK]
Additive only, no existing code changed:
- Create `BaseViewModel<S, F>` base class in `:core`
- Create `UseCase<P, R>` / `FlowUseCase<P, R>` interfaces in `:domain`
- Create `UiState<T>` sealed interface in `:core`
- Create `AppLogger` interface in `:core`
- Create shared test fakes in `:core:testing`

### Phase 2 — Repository Layer [MEDIUM RISK]
- Add interfaces for all remote data sources (in `:domain`)
- Standardize `IWordRepository` to `Try<T>` (callers of bare-Unit methods need updating)
- Standardize mapper style to extension functions
- Add repository-level tests with fake data sources
- Add DataSource tests with MockEngine
- Remove dead `NetworkErrorHandler`
- Add logging + timing interceptors to HTTP pipeline

### Phase 3 — ViewModel Layer [HIGH RISK — VM by VM]
- Migrate VMs to `BaseViewModel` (start with simplest)
- Consolidate fragmented StateFlows into single data class state
- Standardize use case signatures to `UseCase<P,R>` / `FlowUseCase<P,R>`
- Add ViewModel tests with Turbine
- Decompose `LexiconApp.kt` navigation (AppShell, NavigationGraph, AppFlowCoordinator, EffectHandler)
- Remove stateful use cases

### Phase 4 — Feature Modules [HIGH RISK]
- Extract `:feature:auth` (self-contained, good first candidate)
- Extract `:feature:study`, `:feature:words`, `:feature:profile`, `:feature:import`
- Feature-owned Koin modules
- Feature-owned navigation subgraphs

## Your Task

When invoked:

1. **Ask which phase/scope** the user wants to migrate:
   - A specific ViewModel (e.g., "migrate SubscriptionViewModel")
   - A specific repository (e.g., "standardize IWordRepository")
   - A specific use case set (e.g., "migrate all word use cases")
   - A full phase (e.g., "Phase 1" or "Phase 2")

2. **Audit current state** — read all files involved, document:
   - Current pattern used (which of the 6 VM styles, which return type, etc.)
   - Dependencies that need updating
   - Callers that will be affected

3. **Present migration plan** — per-file breakdown:
   - What changes in each file
   - What callers need updating
   - What tests need writing
   - Risk assessment

4. **STOP and wait for approval**

5. **Implement** — following the exact patterns from skills:
   - `viewmodel-patterns` for VM migration
   - `usecase-patterns` for use case migration
   - `repository-patterns` for repository migration
   - `testing-patterns` for test writing

6. **Write tests** for all migrated code

7. **Verify** — build and run tests

## Migration Rules

- Each migration is independently shippable — no big-bang rewrite
- New code follows new patterns immediately — old code migrates gradually
- Tests are added alongside each refactor — never decrease coverage
- Start with the simplest VM/use case/repo to establish the pattern, then move to complex ones
- Keep commits atomic — one logical change per commit

## What We Keep (do not change)
- `Try<T>` sealed class — well-designed, just ensure consistent usage
- HTTP interceptor pipeline — AuthInterceptor, RefreshAndRetry, ErrorInterceptor
- Room DB + migrations — stable and working
- Koin DI — just redistribute ownership to features over time
- Platform abstractions (expect/actual) — clean boundary
