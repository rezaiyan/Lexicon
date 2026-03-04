---
name: e2e-feature
description: Build a feature end-to-end across Vokab backend (Spring Boot) and Lexicon client (KMP), including API endpoint, service, entity, DTO, client data source, repository, use case, ViewModel, and screen
tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash", "Agent"]
model: opus
skills: ["screen-patterns", "viewmodel-patterns", "design-system", "navigation-overlays"]
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
- Pattern:
```kotlin
@Entity
@Table(name = "table_name", indexes = [...])
data class Feature(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    // fields...

    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
```

### 2c. Repository
- Location: `domain/repository/`
- Extend `JpaRepository<Entity, Long>`
- Custom queries via `@Query` annotation

### 2d. Service
- Location: `service/`
- Constructor-injected dependencies
- Business logic lives here, not in controllers
- Use `kotlin-logging`: `private val logger = KotlinLogging.logger {}`

### 2e. DTOs
- Location: `presentation/dto/`
- Request and response data classes
- Keep separate from entities

### 2f. Controller
- Location: `presentation/controller/`
- Pattern:
```kotlin
@RestController
@RequestMapping("/api/v1/feature")
class FeatureController(private val featureService: FeatureService) {

    @GetMapping
    fun get(@AuthenticationPrincipal user: User): ResponseEntity<ApiResponse<FeatureDto>> {
        val data = featureService.get(user)
        return ResponseEntity.ok(ApiResponse(success = true, data = data))
    }

    @PostMapping
    fun create(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: CreateFeatureRequest
    ): ResponseEntity<ApiResponse<FeatureDto>> {
        val data = featureService.create(user, request)
        return ResponseEntity.ok(ApiResponse(success = true, data = data))
    }
}
```

### Backend Rules
- All endpoints return `ApiResponse<T>` wrapper (`success`, `data`, `message`)
- Auth via `@AuthenticationPrincipal user: User` — Spring Security handles JWT
- Premium-gated features: check `featureAccessService.hasActivePremiumAccess(user)`
- Rate limiting: use `rateLimitConfig.getBucket(user.id.toString())`
- Validate with `@Valid` on request bodies
- No business logic in controllers — delegate to services

## Step 3: Client (KMP Lexicon)

Work in `~/AndroidStudioProjects/Vokab/Lexicon`

Build bottom-up:

### 3a. API DTO (shared)
- Location: `data/src/commonMain/kotlin/data/remote/dto/`
- Kotlinx Serialization `@Serializable` data classes
- Must match backend DTOs

### 3b. Remote Data Source
- Location: `data/src/commonMain/kotlin/data/remote/`
- Uses Ktor HttpClient (injected via Koin)
- Pattern:
```kotlin
class FeatureRemoteDataSource(private val client: HttpClient) {
    suspend fun getFeature(): FeatureResponse {
        return client.get("feature").body()
    }
    suspend fun createFeature(request: CreateFeatureRequest): FeatureResponse {
        return client.post("feature") { setBody(request) }.body()
    }
}
```
- Base URL and auth headers are handled by Ktor interceptors automatically

### 3c. Domain Model
- Location: `domain/src/commonMain/kotlin/domain/model/`
- Pure Kotlin data class — no serialization annotations

### 3d. Repository Interface
- Location: `domain/src/commonMain/kotlin/domain/repository/`
- Returns `Flow<T>` or `suspend` functions

### 3e. Repository Implementation
- Location: `data/src/commonMain/kotlin/data/repository/`
- Maps DTOs to domain models
- Combines local (Room) + remote data sources if needed

### 3f. Use Case
- Location: `domain/src/commonMain/kotlin/domain/usecase/`
- One class per business operation
- Pattern:
```kotlin
class GetFeatureUseCase(private val repository: FeatureRepository) {
    operator fun invoke(): Flow<Feature> = repository.getFeature()
}
```

### 3g. ViewModel
- Follow `viewmodel-patterns` skill
- `StateFlow<UiState<T>>` + `Channel<Event>`

### 3h. Screen
- Follow `screen-patterns` skill
- `koinViewModel` + `LexiconColumn` + `UiState` handling

### 3i. DI Registration
- Register ALL new classes in `composeApp/src/commonMain/kotlin/di/AppModule.kt`:
```kotlin
singleOf(::FeatureRemoteDataSource)
singleOf(::FeatureRepositoryImpl) { bind<FeatureRepository>() }
factoryOf(::GetFeatureUseCase)
viewModelOf(::FeatureViewModel)
```

### 3j. Navigation
- Add route in `LexiconApp.kt` NavHost block
- Type-safe `@Serializable` destination

### Client Rules
- Domain module: pure Kotlin only — no Ktor, Room, Compose, platform imports
- No `!!` — handle nullability explicitly
- No try-catch for control flow — use Flow `.catch {}` operator
- No unnecessary `runCatching`
- Design-system first — check existing components before creating new ones
- Shared components go to `design-system/` module

## Step 4: Verify

1. Build backend: `cd ~/AndroidStudioProjects/Vokab/vokab.server && ./gradlew build`
2. Build client: `cd ~/AndroidStudioProjects/Vokab/Lexicon && ./gradlew composeApp:compileKotlinMetadata`
3. Run client tests: `./gradlew composeApp:cleanAllTests composeApp:allTests`
4. Suggest deployment: remind user to check `.claude/infra.local.md` for deploy commands

## Step 5: Summary

After implementation, provide:
- List of all files created/modified (both projects)
- API endpoint(s) added with method + path
- How to test manually
- What tests should be written
