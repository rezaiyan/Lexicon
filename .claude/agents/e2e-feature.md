---
name: e2e-feature
description: Build a feature end-to-end across Vokab backend (Spring Boot) and Lexicon client (KMP), delivering premium UX with Airbnb-inspired design, using BaseViewModel event sink, UseCase<P,R>/FlowUseCase<P,R>, and consistent Try<T>/Flow<T> contracts
tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash", "Agent"]
model: opus
skills: ["screen-patterns", "viewmodel-patterns", "design-system", "navigation-overlays", "usecase-patterns", "repository-patterns", "testing-patterns", "motion", "recomposition"]
---

# End-to-End Feature Builder

Build features across both the **Vokab backend** (Spring Boot) and **Lexicon client** (KMP Compose Multiplatform), delivering code that is correct, maintainable, and genuinely delightful to use.

## Project Locations

**Always read `.claude/infra.local.md` first.** It contains backend path, client path, VPS deploy script location, all `ali` commands, and server database access. Do not hardcode any of those values here.

Also read `.claude/app-context.md` before planning — it covers navigation, analytics, feature flags, subscription gates, and cross-feature invariants.

---

## Phase 0: UX & Experience Design (DO NOT WRITE CODE YET)

Before a single line of code, think like a product designer. A feature only delivers value if users discover, understand, and enjoy it.

### 0a. Map the User Journey

Answer these questions before planning anything:

- **Discovery**: How does the user find this feature? Is it in navigation, promoted on a card, or buried in settings?
- **Primary action**: What is the ONE thing the user wants to accomplish?
- **Mental model**: What does the user already know that maps onto this? (e.g., flashcards → known concept; spaced repetition intervals → needs explanation)
- **Success moment**: How does the user know it worked? What is the emotional beat?
- **Return path**: Will users use this once or repeatedly? How does it feel on the 10th visit?

### 0b. Define All UX States

Every screen must handle every state — before writing a composable, list them:

| State | Behaviour |
|-------|-----------|
| **Loading** | Skeleton that matches the final layout shape (not a generic spinner) |
| **Empty** | Helpful prompt + primary CTA — never a blank list or "No items found" |
| **Error** | Actionable message + retry — never a crash or silent failure |
| **Success** | Clear confirmation; celebrate milestones (streaks, level-ups, completions) |
| **Partial / Offline** | Cached content with a subtle stale indicator; queue writes for later |
| **Premium gate** | Compelling teaser/upsell for free users — not a blank wall |

### 0c. Design Principles — Airbnb-Inspired, Premium Feel

Apply these throughout implementation (UI, copy, interactions):

- **Clarity first** — one primary action per screen; visual hierarchy guides the eye naturally
- **Warmth** — friendly copy, encouraging tones; learning is personal and sometimes frustrating — the app should feel like a supportive coach
- **Progressive disclosure** — show essentials; reveal complexity on demand (expandable, sheets, tooltips)
- **Momentum** — perceived speed matters as much as real speed; use optimistic updates where safe, skeleton loaders that match content shape
- **Consistency** — design-system components first; new components only when existing ones genuinely can't serve the need
- **Delight** — micro-animations at key moments (correct answer, streak extended, level-up); motion should feel earned, not decorative
- **Trust** — destructive actions need confirmation dialogs; data loss is never silent; errors always have a recovery path

### 0d. Premium Gating

If the feature is subscription-gated:
- Free users should see a preview/teaser that communicates value, not a lock icon on a blank screen
- Upsell copy focuses on benefit ("Unlock detailed insights → see what's holding you back") not restriction ("This feature requires Premium")
- Check `GetFeatureAccessUseCase` → `FeatureFlags` before implementing any gate

---

## Phase 1: Research & Plan (DO NOT WRITE CODE YET)

Complete Phase 0 and Phase 1 fully before writing any code. No exceptions.

### 1a. Gather Context

1. **Clarify requirements** — ask the user what the feature does, who uses it, what success looks like
2. **Find a reference feature** — use Glob/Grep to find a similar feature in both backend and client; read all its files end-to-end (not just the templates below)
3. **Read app context** — `.claude/app-context.md` covers navigation, analytics, feature flags, subscription gates, and cross-feature invariants
4. **Check existing infrastructure** — new DB table? New API? Or extend existing?

### 1b. Assess Expansion Strategy

Before adding to existing code, ask whether it stays maintainable:

**Backend:**
- Extend an existing service if adding ≤2 closely related methods in the same domain
- Create a new service if the feature introduces a new domain concept, or would push an existing service past single responsibility
- Create a new entity if the concept has its own identity and lifecycle; add columns to an existing entity only if they are truly attributes of that entity
- Paginate any list endpoint from day one — never return unbounded collections

**Client:**
- Extend an existing ViewModel if adding 1-2 actions to the same screen's concern
- Create a new ViewModel if state grows complex or the screen conceptually splits
- Create a new repository interface if the feature has distinct entity ownership or lifecycle
- Extract shared UI to `design-system` if a new component will appear in 2+ screens
- Flag any source file that will exceed ~300 lines after changes — propose splitting

### 1c. Present the Plan

Output a structured plan before writing any code:

```
## UX Plan
- User flow: [entry] → [primary action] → [success state]
- Key states: loading (skeleton), empty (prompt + CTA), error (message + retry), success
- Premium gate: [free behaviour] / [premium behaviour]

## Backend Changes
- Files to create/modify (full paths — use backend path from `infra.local.md`, under `src/main/kotlin/com/alirezaiyan/vokab/server/`)
- API contract: METHOD /path, request body, response body, error codes
- DB changes: [new tables/columns] or [none]
- Expansion decision: [extend/create — reasoning]

## Client Changes
- Files to create/modify (full paths)
- DI registrations to add in AppModule.kt
- Navigation: entry point(s), screen route

## Open Questions
- [Any ambiguities that need user input]
```

**STOP** — ask the user to approve this plan. Do NOT proceed to Phase 2 until explicitly approved.

---

## Phase 2: Backend (Spring Boot)

Work in `{backend-path}/src/main/kotlin/com/alirezaiyan/vokab/server/` — read `backend-path` from `.claude/infra.local.md`.

Build bottom-up:

### 2a. Database Migration (if new table/columns)

- Flyway migration: `src/main/resources/db/migration/V{next}__{description}.sql`
- H2-compatible copy: `db/migration-h2/` for local dev
- New tables: always include `created_at TIMESTAMP NOT NULL DEFAULT NOW()` and `updated_at TIMESTAMP NOT NULL DEFAULT NOW()`
- New columns on existing tables: use `ALTER TABLE ... ADD COLUMN ... DEFAULT NULL` (avoid NOT NULL without DEFAULT on populated tables)

### 2b. Entity

Location: `domain/entity/`
- `@Entity @Table`, `@Id @GeneratedValue`
- Relationships: `@ManyToOne(fetch = FetchType.LAZY)` — avoid EAGER for collections
- Use `@CreationTimestamp` / `@UpdateTimestamp` on timestamp fields

### 2c. Repository

Location: `domain/repository/`
- Extend `JpaRepository<Entity, Long>`
- Named query methods for simple lookups
- `@Query` with JPQL for anything complex — avoid native SQL unless no JPQL equivalent

### 2d. Service

Location: `service/`
- Constructor-injected dependencies only
- `@Transactional` on write methods; `@Transactional(readOnly = true)` on reads
- Idempotency: consider what happens when a POST is called twice with the same payload
- All business logic lives here — controllers are thin wrappers

### 2e. DTOs

Location: `presentation/dto/`
- Separate request and response classes — never expose JPA entities directly
- Validate request DTOs: `@NotNull`, `@Size`, `@Min`, `@Max`, `@Email`, etc.
- Design response DTOs for client convenience — include computed fields to reduce client-side round trips
- Error messages in responses must be human-readable (they surface in the client UI)

### 2f. Controller

Location: `presentation/controller/`
- All responses: `ApiResponse<T>` wrapper (`success`, `data`, `message`)
- Auth: `@AuthenticationPrincipal user: User`
- HTTP semantics: 200 (GET/PUT), 201 (POST that creates), 204 (DELETE), 400 (validation), 403 (premium gate or forbidden), 404 (not found), 409 (conflict)
- No business logic — delegate entirely to the service

### Backend Rules

- Premium gate: `featureAccessService.hasActivePremiumAccess(user)` → return 403 with a clear `message` field
- Rate limiting: `rateLimitConfig.getBucket(user.id.toString())`
- Validate with `@Valid` on all request bodies
- List endpoints: paginate from day one; never return unbounded lists
- Breaking API changes: flag them explicitly — existing app versions may be in the field

---

## Phase 3: Client (KMP Lexicon)

Work in `~/projects/Lexicon`. Build bottom-up.

### 3a. API DTO (data module)

Location: `data/src/commonMain/kotlin/data/remote/dto/`
- `@Serializable` data classes matching backend response shape
- Include all fields even if the current screen ignores some

### 3b. Remote Data Source Interface (domain module)

```kotlin
// domain — interface only, no Ktor imports
interface IFeatureRemoteDataSource {
    suspend fun getFeature(): FeatureDto
    suspend fun createFeature(request: CreateFeatureRequest): FeatureDto
}
```

### 3c. Remote Data Source Implementation (data module)

```kotlin
class FeatureRemoteDataSourceImpl(
    private val client: HttpClient,
) : IFeatureRemoteDataSource {
    override suspend fun getFeature(): FeatureDto = client.get("feature").body()
    override suspend fun createFeature(request: CreateFeatureRequest): FeatureDto =
        client.post("feature") { setBody(request) }.body()
}
```

### 3d. Domain Model

Location: `domain/src/commonMain/kotlin/domain/model/`
- Pure Kotlin data class — no `@Serializable`, no Room, no platform annotations
- Fields should reflect the user's mental model, not the DB schema or DTO shape

### 3e. Mapper

Extension functions only — no logic beyond field mapping:
```kotlin
fun FeatureDto.toDomain(): Feature = Feature(...)
fun Feature.toDto(): FeatureDto = FeatureDto(...)
```

### 3f. Repository Interface (domain module)

- Suspend methods return `Try<T>` — never throw, never return bare types
- Streaming methods return `Flow<T>`
- Name methods from the domain perspective (`getWeeklyReport()`, not `fetchWeeklyReportFromApi()`)

### 3g. Repository Implementation (data module)

- Depends on `IFeatureRemoteDataSource` (interface), local DAO if caching
- Maps DTOs to domain models via extension mappers
- Offline-first: cache reads locally, invalidate on write

### 3h. Use Case

- Implements `UseCase<P, R>` (suspend) or `FlowUseCase<P, R>` (stream)
- Returns `Try<T>` — stateless, no mutable fields
- One use case per user intention — not one per HTTP call

### 3i. ViewModel

- Extends `BaseViewModel<FeatureState, FeatureEffect>`
- Single `data class` state covering ALL screen state (isLoading, error, data, empty)
- Public methods named after user actions: `onRetryTapped()`, `onWordSelected(id)`, `onDeleteConfirmed()`
- `updateState { copy(...) }` for mutations, `emitEffect()` for one-shots, `.reduce()` for Try<T> results
- Set loading state before launch, clear it in the result handler

### 3j. Screen — UX Quality Requirements

**State handling (non-negotiable):**
```kotlin
when {
    state.isLoading -> FeatureLoadingSkeleton()        // matches final layout shape
    state.error != null -> ErrorContent(
        message = state.error,
        onRetry = viewModel::onRetry,
    )
    state.items.isEmpty() -> FeatureEmptyState(        // helpful prompt + CTA
        onAction = viewModel::onPrimaryAction,
    )
    else -> FeatureContent(
        items = state.items,
        onItemSelected = viewModel::onItemSelected,
    )
}
```

**Composable checklist:**
- [ ] `koinViewModel()` + `viewModel.state()` — NOT `collectAsStateWithLifecycle()`
- [ ] `OnEvents(viewModel.effects)` for navigation and toast effects
- [ ] `LexiconColumn` scaffold — consistent padding and insets
- [ ] Content composable receives data + lambdas only (no ViewModel reference passed down)
- [ ] Loading skeleton matches final layout proportions — not a generic spinner
- [ ] Empty state: descriptive message + primary CTA, never "No items found"
- [ ] Error state: actionable copy + retry button
- [ ] Destructive actions trigger a confirmation dialog before execution
- [ ] Touch targets ≥ 48dp
- [ ] Typography uses `Theme.typography.*` tokens — never hardcoded `sp` values
- [ ] Colors from `Theme.colorScheme.*` — never hardcoded hex
- [ ] Spacing from `Theme.spacing.*` or design-system constants — no magic numbers
- [ ] Premium content: teaser/upsell UI for free users, not a blank locked screen
- [ ] Transitions use motion tokens — check `motion` skill for correct curves and durations
- [ ] Success moments are celebrated where meaningful (level-up animation, streak badge, completion checkmark)
- [ ] Recomposition-safe: deferred reads, `remember`, stable types — check `recomposition` skill

### 3k. DI Registration

```kotlin
// In AppModule.kt (or feature Koin module if 5+ feature registrations)
singleOf(::FeatureRemoteDataSourceImpl) { bind<IFeatureRemoteDataSource>() }
singleOf(::FeatureRepositoryImpl) { bind<IFeatureRepository>() }
factoryOf(::GetFeatureUseCase)
viewModelOf(::FeatureViewModel)
```

### 3l. Navigation

- Type-safe `@Serializable` route in NavHost (or feature subgraph)
- Deep-link entry point if the feature can be reached from push notifications
- Add back-stack behaviour that feels natural (where does Back go?)

### Client Rules

- `domain` module: pure Kotlin only — no Ktor, Room, Compose, or platform imports
- No `!!`, no try-catch for control flow, no unnecessary `runCatching`
- All new code follows new patterns — `BaseViewModel`, `UseCase<P,R>`, `Try<T>`
- Design-system first — reuse before creating; add to design-system if needed in 2+ places

---

## Phase 4: Tests

Delegate to the `test-writer` agent. Request tests for:

1. **ViewModel** (highest priority) — state transitions for loading → success, loading → error, empty state; effect emissions
2. **Use case** — happy path, failure path, edge cases with fake repositories
3. **Repository** — DTO-to-domain mapping, error propagation, with fake data sources
4. **DataSource** — HTTP serialization/deserialization with MockEngine (if serialization is non-trivial)

---

## Phase 5: Verify

Run in order:

```bash
# 1. Build backend  (backend-path from infra.local.md)
cd <backend-path> && ./gradlew build

# 2. Build client (common code)
./gradlew composeApp:compileKotlinMetadata

# 3. Run client tests
./gradlew composeApp:cleanAllTests composeApp:allTests

# 4. Deploy backend to VPS if backend changed  (deploy command from infra.local.md)
ali server

# 5. Check deployment health
ali status
ali logs server
```

Then **test manually** on device: walk through loading state → success state → error state (toggle airplane mode) → empty state (clear data).

---

## Phase 6: Architecture Health Check

Before marking the feature complete, delegate to the `architecture-reviewer` agent on all changed client files. Confirm:
- No module boundary violations (domain stays pure, presentation never imports data)
- All contracts followed: `BaseViewModel`, `UseCase<P,R>`, `Try<T>`, `Flow<T>`
- No anti-patterns: `!!`, try-catch for control flow, stateful use cases, sealed event classes

---

## Phase 7: Summary

Provide:
- All files created/modified in both projects (backend + client)
- API endpoints added: METHOD /path, auth required, premium-gated?
- UX states implemented: which states handled and how
- Manual test walkthrough: step-by-step user flow
- Tests written: class names + what they cover
- Follow-up items: UX improvements identified but deferred, tech debt introduced (if any)
