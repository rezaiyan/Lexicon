---
name: e2e-feature
description: Build a feature end-to-end across Vokab backend (Spring Boot) and Lexicon client (KMP), using BaseViewModel event sink, UseCase<P,R>/FlowUseCase<P,R>, and consistent Try<T>/Flow<T> contracts
tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash", "Agent"]
model: opus
skills: ["screen-patterns", "viewmodel-patterns", "design-system", "navigation-overlays", "usecase-patterns", "repository-patterns", "testing-patterns"]
---

# End-to-End Feature Builder

Build features across both the **Vokab backend** (Spring Boot) and **Lexicon client** (KMP Compose Multiplatform).

## Project Locations

Read `.claude/infra.local.md` for backend path, deployment script, and server query commands.

## Phase 1: Research & Plan (DO NOT WRITE CODE YET)

You MUST complete this entire phase before writing any code. No exceptions.

1. **Clarify requirements** — ask the user what the feature needs to do
2. **Find a reference feature** — use Glob/Grep to find a similar existing feature in both backend and client. Read all its files end-to-end to understand the real patterns (not just the templates below)
3. **Check existing infrastructure** — does this feature need a new DB table? New API endpoint? Or can it extend existing ones?
4. **Present the plan** as a structured list:
   - **Backend files** to create/modify (with full paths)
   - **Client files** to create/modify (with full paths)
   - **API contract** — endpoint method, path, request body, response body
   - **DB changes** — new tables/columns or none
   - **DI registrations** to add
   - **Navigation** — where the screen lives, how to reach it
5. **STOP and wait** — ask the user to approve the plan before proceeding to Phase 2
6. Do NOT proceed to Phase 2 until the user explicitly approves

## Phase 2: Implementation (only after plan approval)

## Step 2: Backend (Spring Boot)

Work in `~/AndroidStudioProjects/Vokab/vokab.server/src/main/kotlin/com/alirezaiyan/vokab/server/`

Build bottom-up:

### 2a. Database Migration (if new table/columns)
- Add Flyway migration in `src/main/resources/db/migration/`
- File naming: `V{next_number}__{description}.sql`
- Also add H2-compatible migration in `db/migration-h2/` for local dev

### 2b. Entity
- Location: `domain/entity/`
- JPA `@Entity` with `@Table`, `@Id @GeneratedValue`, relationships via `@ManyToOne`

### 2c. Repository
- Location: `domain/repository/`
- Extend `JpaRepository<Entity, Long>`

### 2d. Service
- Location: `service/`
- Constructor-injected dependencies, business logic here (not controllers)

### 2e. DTOs
- Location: `presentation/dto/`
- Request and response data classes, separate from entities

### 2f. Controller
- Location: `presentation/controller/`
- All endpoints return `ApiResponse<T>` wrapper
- Auth via `@AuthenticationPrincipal user: User`

### Backend Rules
- All endpoints return `ApiResponse<T>` wrapper (`success`, `data`, `message`)
- Premium-gated features: check `featureAccessService.hasActivePremiumAccess(user)`
- Rate limiting: use `rateLimitConfig.getBucket(user.id.toString())`
- Validate with `@Valid` on request bodies
- No business logic in controllers — delegate to services

## Step 3: Client (KMP Lexicon)

Work in `~/AndroidStudioProjects/Vokab/Lexicon`

Build bottom-up:

### 3a. API DTO (shared)
- Location: `data/src/commonMain/kotlin/data/remote/dto/`
- `@Serializable` data classes matching backend DTOs

### 3b. Remote Data Source Interface (in domain)
```kotlin
// In domain — interface
interface IFeatureRemoteDataSource {
    suspend fun getFeature(): FeatureDto
    suspend fun createFeature(request: CreateFeatureRequest): FeatureDto
}
```

### 3c. Remote Data Source Implementation (in data)
```kotlin
// In data — implementation
class FeatureRemoteDataSourceImpl(
    private val client: HttpClient,
) : IFeatureRemoteDataSource {
    override suspend fun getFeature(): FeatureDto = client.get("feature").body()
    override suspend fun createFeature(request: CreateFeatureRequest): FeatureDto =
        client.post("feature") { setBody(request) }.body()
}
```

### 3d. Domain Model
- Location: `domain/src/commonMain/kotlin/domain/model/`
- Pure Kotlin data class — no serialization annotations

### 3e. Mapper
- Extension functions: `fun FeatureDto.toDomain(): Feature`, `fun Feature.toDto(): FeatureDto`

### 3f. Repository Interface (in domain)
- Suspend methods return `Try<T>`, streaming methods return `Flow<T>`

### 3g. Repository Implementation (in data)
- Depends on `IFeatureRemoteDataSource` (interface), local DAO
- Maps DTOs to domain models via extension function mappers

### 3h. Use Case
- Implements `UseCase<P, R>` or `FlowUseCase<P, R>`
- Suspend use cases return `Try<T>`
- Stateless — no mutable fields

### 3i. ViewModel
- Extends `BaseViewModel<FeatureState, FeatureEffect>`
- Single `data class` state, event sink pattern (public methods)
- `updateState { copy(...) }`, `emitEffect()`, `.reduce()`

### 3j. Screen
- `koinViewModel` + `viewModel.state()` + `OnEvents(viewModel.effects)`
- `LexiconColumn` scaffold
- Content composable: data + lambdas, VM method references as event sink

### 3k. DI Registration
```kotlin
singleOf(::FeatureRemoteDataSourceImpl) { bind<IFeatureRemoteDataSource>() }
singleOf(::FeatureRepositoryImpl) { bind<IFeatureRepository>() }
factoryOf(::GetFeatureUseCase)
viewModelOf(::FeatureViewModel)
```

### 3l. Navigation
- Add route in NavHost block (or feature subgraph)
- Type-safe `@Serializable` destination

### Client Rules
- Domain module: pure Kotlin only — no Ktor, Room, Compose, platform imports
- No `!!`, no try-catch for control flow, no unnecessary `runCatching`
- All new code follows new patterns — BaseViewModel, UseCase<P,R>, Try<T> contracts
- Design-system first — check existing components before creating new ones

## Step 4: Tests

Delegate to `test-writer` agent for:
1. ViewModel tests with Turbine
2. Use case tests with fake repositories
3. Repository tests with fake data sources
4. DataSource tests with MockEngine (if complex serialization)

## Step 5: Verify

1. Build backend: `cd ~/AndroidStudioProjects/Vokab/vokab.server && ./gradlew build`
2. Build client: `cd ~/AndroidStudioProjects/Vokab/Lexicon && ./gradlew composeApp:compileKotlinMetadata`
3. Run client tests: `./gradlew composeApp:cleanAllTests composeApp:allTests`
4. Suggest deployment: remind user to check `.claude/infra.local.md` for deploy commands

## Step 6: Summary

After implementation, provide:
- List of all files created/modified (both projects)
- API endpoint(s) added with method + path
- How to test manually
- What tests were written
