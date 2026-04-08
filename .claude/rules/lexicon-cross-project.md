---
description: Cross-project workflow for features spanning Lexicon KMP client and lexicon.server Spring Boot backend — contract-first design, deploy sequence, agent usage
---

# Cross-Project Workflow

Features that touch both `~/projects/Lexicon` (KMP client) and `~/projects/lexicon.server` (Spring Boot backend).

**For full E2E feature implementation, use the `e2e-feature` agent** — it handles all phases with detailed checklists.

---

## When You Need Cross-Project Work

A change is cross-project when it requires:
- A new or modified API endpoint (backend DTO + client model + Ktor call)
- A new database column or table (Flyway migration + entity + DTO + sync)
- A new analytics event tracked on both sides
- A backend business rule change that affects client UX

---

## Contract-First Design

**Define the API contract BEFORE writing code on either side.**

```
1. New endpoint? → Define URL, method, request/response DTOs first
2. New DB column? → Write Flyway migration first, agree on field name
3. New event? → Define event name and payload fields first
```

Put the contract in a plan or comment before touching either repo. This prevents client/backend drifting apart.

---

## Implementation Order

```
Backend first → Client second → Verify together
```

1. **Backend:** Migration → Entity → Repository → Service → Controller → Tests
2. **Client:** Domain model → DataSource (Ktor) → Repository → UseCase → ViewModel → Screen
3. **Verify:** Run both locally, call the endpoint from the client

**Why backend first:** Client can be built against the real API. Avoids building a client against a mock that doesn't match reality.

---

## Running Both Locally

```bash
# Terminal 1: Backend (H2 dev mode)
cd ~/projects/lexicon.server && ./scripts/start-dev.sh
# Starts on http://localhost:8080

# Terminal 2: Client (Android emulator)
cd ~/projects/Lexicon && ./gradlew composeApp:assembleDebug
# Point local.properties → BACKEND_URL=http://10.0.2.2:8080
```

For iOS simulator, use `http://localhost:8080` (not 10.0.2.2).

---

## Migration Numbering

**Check before creating a migration:**
```bash
ls ~/projects/lexicon.server/src/main/resources/migration/ | sort | tail -5
# Last file: V31__some_name.sql → next is V32__your_name.sql
```

Also add an H2-compatible version in `migration-h2/` if the SQL uses PostgreSQL-specific syntax.

---

## API Contract Verification

Before shipping the client change, verify the contract matches:

```bash
# Hit the real local backend and inspect response shape
curl -s http://localhost:8080/api/v1/health | jq .

# Check a protected endpoint with JWT
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{"idToken":"..."}' | jq -r '.data.accessToken')
curl -s http://localhost:8080/api/v1/words \
  -H "Authorization: Bearer $TOKEN" | jq .

# Production URL is in .claude/infra.local.md (gitignored)
```

---

## Deploy Sequence

```
1. Deploy backend first:    ali server
2. Verify health:           curl $PRODUCTION_URL/api/v1/health  (URL in .claude/infra.local.md)
3. Ship client build:       fastlane android beta  (or iOS equivalent)
```

Never ship a client that requires a backend feature before the backend is deployed.

---

## Cross-Project Checklist

- [ ] API contract defined before coding (URL, request/response shape)
- [ ] Flyway migration sequential (check last V-number)
- [ ] H2 migration added if using PostgreSQL-specific SQL
- [ ] Backend tests pass (`./gradlew test`)
- [ ] Client compiles (`./gradlew composeApp:compileKotlinMetadata`)
- [ ] Both run locally and the client calls the endpoint successfully
- [ ] Backend deployed before client ships

---

## e2e-feature Agent

For building a complete feature end-to-end, delegate to the `e2e-feature` agent:

```
Use agent: e2e-feature
Prompt: "Build [feature description]"
```

The agent: reads `infra.local.md` + `app-context.md`, designs UX, implements backend (migration → entity → service → controller → tests), implements client (domain → data → useCase → viewModel → screen), verifies both build, deploys to VPS, and runs a manual smoke test.

**Use the agent for new features.** Use this rule for quick cross-project changes and checklists.
