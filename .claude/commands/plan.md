---
description: Plan-first workflow for tasks touching 3+ files or requiring architectural decisions — enters plan mode, checks boundaries, finds reference, gets approval before any code
argument-hint: "<what you want to build or change>"
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash", "Agent"]
---

Plan the implementation of: `$ARGUMENTS`

## Phase 1: Understand (read-only)

1. **Read the relevant domain** — check `app-context.md` for the feature area and existing components
2. **Find a reference implementation** — Glob/Grep for a similar existing feature, read all its files:
   - Domain model + repository interface
   - Data source interface + implementation + mapper
   - Repository implementation
   - Use case(s)
   - ViewModel + Screen
   - DI registration in AppModule.kt
   - Tests
3. **Check for existing infrastructure** — can this extend existing endpoints, tables, repositories?
4. **Check module boundaries** — which modules will be touched?

## Phase 2: Architecture Decision

Answer these questions before planning files:

| Question | Answer |
|---|---|
| Which layer(s) does this touch? | domain / data / presentation / all |
| Is there a new data model needed? | yes → domain/model/ ; no → reuse existing |
| Is there a new API endpoint needed? | yes → DTO + remote data source + backend; no → use existing |
| Does it need local persistence? | yes → SQLDelight entity + local data source |
| Does it need a new screen? | yes → ViewModel + Screen + navigation route |
| Does it change auth or subscription gating? | yes → check critical-risks.md sections 2, 3 |
| Does it touch analytics? | yes → check critical-risks.md section 2 |
| Does it touch the SRS algorithm? | YES → read critical-risks.md section 1 carefully |
| Does it batch remote operations? | yes → check critical-risks.md section 10 |

## Phase 3: File Plan

Present the implementation plan as a structured list:

### Domain layer (`:domain`)
- `domain/{feature}/model/NewModel.kt` (if new model)
- `domain/{feature}/repository/INewRepository.kt` (interface only)
- `domain/{feature}/datasource/INewRemoteDataSource.kt`
- `domain/{feature}/usecase/DoThingUseCase.kt`

### Data layer (`:data`)
- `data/remote/dto/NewDto.kt`
- `data/{feature}/datasource/NewRemoteDataSourceImpl.kt`
- `data/{feature}/mapper/NewMappers.kt`
- `data/{feature}/repository/NewRepositoryImpl.kt`

### Presentation layer
- `presentation/ui/screens/{Feature}Screen.kt`
- `presentation/viewmodel/{Feature}ViewModel.kt` (or `feature/{feature}/`)

### DI
- `di/AppModule.kt` — new registrations

### Navigation (if new screen)
- Route object/class
- NavHost registration

### Tests (required)
- `{Feature}ViewModelTest.kt`
- `{Feature}UseCaseTest.kt`
- `{Feature}RepositoryTest.kt` (if new repository)

## Phase 4: Present and Wait

Format the plan as:

```
## Plan: [Feature Name]

### What it does
[1-2 sentence description]

### Architecture decisions
- [key decision 1]
- [key decision 2]

### Files to create
- path/to/file.kt — [purpose]
...

### Files to modify
- path/to/existing.kt — [what changes]
...

### Risks / notes
- [any critical-risks.md sections that apply]
- [any non-obvious decisions]

Ready to implement? (yes/no)
```

**STOP HERE — do not write any code until the user approves the plan.**

## Phase 5: Implement (after approval)

Work bottom-up:
1. Domain model(s)
2. Repository interface + data source interface
3. DTOs + mappers
4. Data source implementation
5. Repository implementation
6. Use case(s)
7. ViewModel
8. Screen
9. DI registration
10. Navigation
11. Tests

Compile after each layer:
```bash
./gradlew composeApp:compileKotlinMetadata
```

## Phase 6: Verify

```bash
# Full test run
./gradlew composeApp:cleanAllTests composeApp:allTests

# Then use architecture-reviewer agent
```

## Rules

- Never write code before the plan is approved
- Always find a reference implementation first — don't scaffold from memory
- If the task touches the SRS algorithm, analytics, auth, or sync: read the relevant critical-risks.md section first, include risk notes in the plan
- If the plan grows to 15+ files, break into independent PRs and plan each separately
- If you discover the plan needs to change mid-implementation, stop and re-present the updated plan
