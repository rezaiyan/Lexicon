---
description: Scaffold an end-to-end feature across Lexicon client (KMP) and Vokab backend (Spring Boot)
argument-hint: "<feature-name>"
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash", "Agent"]
---

Scaffold the files for a new feature named "$ARGUMENTS" end-to-end across both the **Vokab backend** and **Lexicon client**.

## Project Locations

Read `.claude/infra.local.md` for project paths, build commands, and deployment details. Always read this file first.

## Phase 1: Research & Plan (DO NOT WRITE CODE YET)

You MUST complete this entire phase before writing any code.

1. **Clarify requirements** — ask the user what the feature needs to do
2. **Determine scope** — does this feature need:
   - Backend only? (new API endpoint, DB change, background job)
   - Client only? (UI feature using existing API)
   - Both? (new API + client integration)
3. **Find a reference feature** — use Glob/Grep to find a similar existing feature in both backend and client. Read all its files to understand the real patterns
4. **Check existing infrastructure** — can this extend existing endpoints, tables, services?
5. **Present the plan** as a structured list:
   - **Backend files** to create/modify (with full paths)
   - **Client files** to create/modify (with full paths)
   - **API contract** — HTTP method, path, request/response bodies
   - **DB changes** — new tables/columns/migrations or none
   - **DI registrations** — backend (Spring) and client (Koin)
   - **Navigation** — where the screen lives (if UI involved)
6. **STOP and wait for approval** before proceeding

## Phase 2: Backend (Spring Boot)

Work in the backend path from `infra.local.md`.

Build bottom-up:

### 2a. Database Migration (if new table/columns)
- Add Flyway migration: `src/main/resources/db/migration/V{next_number}__{description}.sql`
- Also add H2-compatible migration: `src/main/resources/db/migration-h2/` for local dev
- Check existing migrations to determine the next version number

### 2b. Entity
- Location: `domain/entity/`
- JPA `@Entity` with `@Table`, `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`
- Relationships via `@ManyToOne(fetch = FetchType.LAZY)` with `@JoinColumn`
- Audit fields: `createdAt`, `updatedAt` with `@Column(updatable = false)` / `@PreUpdate`

### 2c. Repository
- Location: `domain/repository/`
- Extend `JpaRepository<Entity, Long>`
- Custom query methods follow Spring Data naming conventions
- Complex queries: `@Query` annotation with JPQL

### 2d. Service
- Location: `service/`
- `@Service` with constructor injection
- Business logic lives here, not in controllers
- Throw appropriate exceptions — global handler maps them to HTTP status codes

### 2e. DTOs
- Location: `presentation/dto/`
- Separate request and response data classes
- Validation: `@field:NotBlank`, `@field:Size`, `@field:Valid` on request DTOs

### 2f. Controller
- Location: `presentation/controller/`
- `@RestController` with `@RequestMapping("/api/v1/{feature}")`
- All endpoints return `ApiResponse<T>` wrapper (`success`, `data`, `message`)
- Auth: `@AuthenticationPrincipal user: User`
- Validate: `@Valid @RequestBody`
- Premium gates: `featureAccessService.hasActivePremiumAccess(user)`
- Rate limiting: `rateLimitConfig.getBucket(user.id.toString())`
- No business logic — delegate to services

### Backend Patterns
```kotlin
// Controller pattern
@RestController
@RequestMapping("/api/v1/feature")
class FeatureController(
    private val featureService: FeatureService,
) {
    @GetMapping
    fun getFeature(@AuthenticationPrincipal user: User): ApiResponse<FeatureResponse> {
        val result = featureService.getFeature(user)
        return ApiResponse(success = true, data = result)
    }

    @PostMapping
    fun createFeature(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: CreateFeatureRequest,
    ): ApiResponse<FeatureResponse> {
        val result = featureService.createFeature(user, request)
        return ApiResponse(success = true, data = result)
    }
}

// Service pattern
@Service
class FeatureService(
    private val featureRepository: FeatureRepository,
) {
    fun getFeature(user: User): FeatureResponse { /* ... */ }
    fun createFeature(user: User, request: CreateFeatureRequest): FeatureResponse { /* ... */ }
}
```

### 2g. Security Configuration (if needed)
- Public endpoints: add to `SecurityConfig.kt` permitAll list
- Webhook endpoints: verify signatures in controller/service

### 2h. Backend Build & Verify
- Build command: `./gradlew build` (from backend path in `infra.local.md`)

## Phase 3: Client (KMP Lexicon)

Work in the Lexicon client project.

Build bottom-up:

### 3a. API DTO
- Location: `data/src/commonMain/kotlin/data/remote/dto/`
- `@Serializable` data classes matching backend request/response DTOs

### 3b. Remote Data Source Interface (in domain)
```kotlin
interface IFeatureRemoteDataSource {
    suspend fun getFeature(): FeatureDto
    suspend fun createFeature(request: CreateFeatureRequest): FeatureDto
}
```

### 3c. Remote Data Source Implementation (in data)
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
- Location: `domain/src/commonMain/kotlin/domain/model/`
- Pure Kotlin data class — no serialization annotations, no framework imports

### 3e. Mapper
- Extension functions: `fun FeatureDto.toDomain(): Feature`
- Located with data source or repository implementation

### 3f. Repository Interface (in domain)
- Suspend methods return `Try<T>`, streaming methods return `Flow<T>`

### 3g. Repository Implementation (in data)
- Depends on `IFeatureRemoteDataSource` (interface, not impl)
- Maps DTOs to domain models via extension function mappers
- Wraps calls in Try

### 3h. Use Case
- Implements `UseCase<P, R>` (suspend -> `Try<T>`) or `FlowUseCase<P, R>` (-> `Flow<T>`)
- Stateless — no mutable fields

### 3i. ViewModel
- Extends `BaseViewModel<FeatureState, FeatureEffect>`
- Single `data class` state, event sink pattern (public methods)
- `updateState { copy(...) }`, `emitEffect()`, `.reduce()`

### 3j. Screen
- `koinViewModel` + `viewModel.state()` + `OnEvents(viewModel.effects)`
- `LexiconColumn` scaffold
- Content composable: data + lambdas, VM method references as event sink

### 3k. DI Registration (AppModule.kt)
```kotlin
singleOf(::FeatureRemoteDataSourceImpl) { bind<IFeatureRemoteDataSource>() }
singleOf(::FeatureRepositoryImpl) { bind<IFeatureRepository>() }
factoryOf(::GetFeatureUseCase)
viewModelOf(::FeatureViewModel)
```

### 3l. Navigation
- Add `@Serializable` route object/class
- Register in NavHost (or feature subgraph)

### 3m. Client Build & Verify
```bash
./gradlew composeApp:compileKotlinMetadata
```

## Phase 4: Tests

Delegate to `test-writer` agent for:
1. **Backend**: Service tests with MockK, integration tests with Spring Boot Test
2. **Client**: ViewModel tests with Turbine, UseCase tests with fake repositories, Repository tests with fake data sources, DataSource tests with MockEngine

Run all tests:
- Backend: `./gradlew test` (from backend path in `infra.local.md`)
- Client: `./gradlew composeApp:cleanAllTests composeApp:allTests`

## Phase 5: Deploy & Verify

Refer the user to `infra.local.md` for deployment commands (deploy backend, check status, tail logs, stream errors).

## Phase 6: Summary

After implementation, provide:
- All files created/modified (both projects, with full paths)
- API endpoint(s): HTTP method + path + request/response shape
- DB migrations applied (if any)
- How to test manually (curl examples or app flow)
- Tests written

## Rules

### Backend (Spring Boot)
- All endpoints return `ApiResponse<T>` wrapper
- No business logic in controllers — services only
- Validate request bodies with `@Valid`
- Use constructor injection (no `@Autowired`)
- Flyway migrations for all schema changes (both PostgreSQL and H2)

### Client (KMP Lexicon)
- Domain module: pure Kotlin — no Ktor, Compose, platform imports
- No `!!`, no try-catch for control flow, no `runCatching`
- All new code follows target patterns — BaseViewModel, UseCase<P,R>, Try<T>
- Design-system first — check existing components before creating new ones

### Both
- **Plan first, code second** — always get user approval
- **Reference feature** — read a similar existing feature before scaffolding
- **Commit often** — one logical unit per commit (backend commit, then client commit)
