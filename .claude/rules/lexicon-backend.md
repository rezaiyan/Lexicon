---
description: Spring Boot backend quick reference for Lexicon — path, stack, architecture, conventions, build commands
---

# Lexicon Backend (lexicon.server)

**Path:** `~/projects/lexicon.server`
**Full docs:** `~/projects/lexicon.server/.claude/` — `api.md`, `architecture.md`, `tech-stack.md`, `CLAUDE.md`

---

## Tech Stack

| Layer | Tool | Version |
|-------|------|---------|
| Framework | Spring Boot | 3.5.6 |
| Language | Kotlin | 1.9.25 |
| JVM | Java | 21 |
| DB | PostgreSQL (prod) / H2 (test) | Flyway 11.8.0 |
| Auth | JWT RS256 (JJWT 0.12.3) | stateless sessions |
| Push | Firebase Admin SDK | 9.2.0 |
| Rate limit | Bucket4j | 8.10.1 |
| Testing | JUnit 5 + MockK | 1.13.8 |
| Coverage | JaCoCo | 80% threshold |

---

## Architecture

**3-layer strict:** Controller → Service → Repository. No shortcuts.

```
Controllers (17)   → @RestController, @RequestMapping("/api/v1/...")
Services (27)      → @Service, @Transactional on writes
Repositories       → Spring Data JPA + custom JPQL/native SQL
```

**All responses wrapped:**
```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)
// 200 success, 400 validation, 401 auth, 404 not found, 500 error
```

**JWT filter:** `JwtAuthenticationFilter` — reads `Authorization: Bearer <token>`, validates RS256, looks up user. Admin bypass via `X-Admin-Key` header.

---

## Key Conventions

**Error handling:** Throw standard exceptions → `GlobalExceptionHandler` maps them:
```kotlin
IllegalArgumentException     → 400
NoSuchElementException       → 404
AuthenticationException      → 401
AccessDeniedException        → 403
```
Never return error details in body — use `message` field only.

**Transactions:** `@Transactional` on writes, `@Transactional(readOnly = true)` on reads. Avoid long transactions with external API calls.

**Database migrations:** Flyway manages schema — `ddl-auto: validate` (NEVER create/update). New schema change = new `.sql` file in `migration/` with sequential version number (check last V-number in `migration/` before creating).

**Analytics:** `EventService.track()` must never throw — fire-and-forget. Failures don't fail the primary operation.

**Security:** NEVER log sensitive data (passwords, tokens, emails). Validate webhook signatures. Rate-limit sensitive endpoints.

**Testing:** MockK (not Mockito). Backtick test names. Private factory functions for test data.

---

## Build Commands

```bash
cd ~/projects/lexicon.server

./gradlew build                  # Full build + tests
./gradlew bootRun                # Run locally (H2 database)
./scripts/start-dev.sh           # Dev server with hot reload

./gradlew test                   # All tests
./gradlew test --tests "*.WordServiceTest"  # Single class

./gradlew koverHtmlReport        # Coverage report (must be ≥80%)
```

**Deploy:** `ali server` (git push + Docker rebuild on VPS)

---

## API Base URL

- **Local:** `http://localhost:8080/api/v1`
- **Production:** see `.claude/infra.local.md` (gitignored, not committed)
- **Health:** `/api/v1/health`

---

## Test Profile

Tests use H2 + `schema.sql` (Flyway disabled). Config in `src/test/resources/application-test.yml`. Test security: `ControllerTestSecurityConfig` for controller tests (inject auth via `SecurityMockMvcRequestPostProcessors.authentication()`).
