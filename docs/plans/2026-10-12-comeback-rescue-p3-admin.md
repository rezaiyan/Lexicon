# Comeback Rescue — P3 Taste Test, Funnel & Send Switch Implementation Plan

> **For agentic workers:** Use claude-kit:implement to execute this plan task-by-task.

**Goal:** Give Ali an admin page to generate rescue copy for real learners without sending anything, rate every candidate 👍/👎, see the live funnel (AI vs fallback vs holdout, per angle), and switch sends on once the taste test passes.

**Architecture:** In `lexicon.server`. A `RescuePreviewService` reuses the P1 planner's draft step (pool → fact sheet → AI → validate → lint → fallback) without the lapse check and stores results in a separate `rescue_preview` table, so previews never touch `comeback_rescue` or block a real plan. Ratings live in `rescue_copy_rating`. `RescueAdminController` (`/admin/rescue/**`, admin session cookie, `ROLE_ADMIN` via existing `SecurityConfig`) serves previews, ratings, taste score, recent nightly plans, funnel and an eval export. A vanilla-JS page `admin-ui/rescue.html` follows the existing support/notifications pages (same CSS, CSP, sign-in flow). A new `NotificationJob.COMEBACK_RESCUE_PLANNING` lets the admin run nightly planning on demand.

**Tech Stack:** Spring Boot 3.5 / Kotlin, Flyway, `NamedParameterJdbcTemplate`, JUnit 5 + MockK, Testcontainers, vanilla JS admin UI.

**Depends on:** P1 merged (planner, entity, table). Design doc `.claude/plans/2026-10-12-ai-comeback-rescue.md` §2b "Making sure users actually like it", §6.

**Gate that stays human:** sends are enabled only after Ali's taste test reaches ≥ 80% 👍 on the chosen candidates. This plan builds everything and leaves `COMEBACK_RESCUE_SEND_ENABLED=false`.

---

### Task 1: Planner draft extraction (refactor, behavior unchanged)

- Split `ComebackRescuePlanner.planIfEligible` into eligibility + `fun draft(user: User, daysAway: Long): RescueDraft?` (public), where `RescueDraft` holds pool language pair, `wordIds`, `RescueCopy`, `source`, `candidatesJson`, `reasoningJson`.
- `planIfEligible` = eligibility → `draft` → persist. Existing `ComebackRescuePlannerTest` must pass unchanged.
- Commit `refactor(rescue): expose the planner's draft step`.

### Task 2: Preview + rating tables

- Migration `V47__add_rescue_previews.sql`:
  - `rescue_preview(id bigserial pk, user_id fk users on delete cascade, created_at, prompt_version text, source text, angle text, title text, body text, outro_win text, outro_soft text, word_ids jsonb, candidates jsonb, picks_reasoning jsonb)`
  - `rescue_copy_rating(id bigserial pk, preview_id fk rescue_preview on delete cascade, candidate_index int not null, rating text not null /* UP|DOWN */, note text, rated_at timestamp default now(), unique(preview_id, candidate_index))`
- Entities `RescuePreview`, `RescueCopyRating` (+ enum `CopyRating { UP, DOWN }`) and repositories. Repository test (Testcontainers): save/read, upsert-by-unique on re-rate.
- Add `rescue_copy_rating` (via preview subquery) and `rescue_preview` to `UserDataPurger.STATEMENTS` — `UserDataPurgerTest` fails on any uncovered `user_id` table.
- Commit `feat(rescue): tables for copy previews and ratings`.

### Task 3: Preview service

- `RescuePreviewService.generate(count: Int): List<RescuePreview>`: sample up to `count` active users (`findAllActiveUsersWithPushTokens`, shuffled, stop at `count` successful drafts, try at most `count * 5`), `daysAway = 3` for the fact sheet, call `planner.draft`, persist. Never writes `comeback_rescue`. Count capped at 50.
- `rate(previewId, candidateIndex, rating, note)`: validates index < candidates size, upserts.
- `tasteScore()`: over rated candidates that were the *chosen* one (index matches the candidate whose title equals preview title) → `{rated, up, upRate}`; plus all-candidate rate.
- Unit tests with mocked planner/repositories: count cap, skips users without a draft, never touches `ComebackRescueRepository`, rating index validation, score math.
- Commit `feat(rescue): generate and rate rescue copy previews`.

### Task 4: Funnel query

- `RescueFunnelQueries.funnel(days: Int): List<RescueFunnelRow>` — SQL over `comeback_rescue` for `planned_for >= today - days`, grouped by `arm (SENT|HOLDOUT|UNSENT), source, angle` so sent vs holdout return rates compare directly: `planned` (all), `holdout`, `sent`, `opened` (join `notification_log.opened_at` via `notification_log_id`), `completed`, `returned24h` (exists `daily_activities` for the user with `activity_date` between `sent_at::date` and `sent_at::date + 1`; for holdout rows use `planned_for` instead of `sent_at`).
- Testcontainers test seeding rescues, logs and activity.
- Commit `feat(rescue): rescue funnel by source and angle`.

### Task 5: Admin API + on-demand job

- `RescueAdminController` `/admin/rescue`:
  - `POST /previews?count=20` → generated previews
  - `GET /previews?limit=50` → newest previews with candidates (violations included) and ratings
  - `POST /previews/{id}/ratings` `{candidateIndex, rating, note?}`
  - `GET /taste` → taste score
  - `GET /plans?limit=50` → newest `comeback_rescue` rows
  - `GET /funnel?days=14`
  - `GET /eval.jsonl` → rated candidates as JSON lines (`text/plain`), the regression eval set
- `NotificationJob.COMEBACK_RESCUE_PLANNING` → `NotificationAdminService.runJob` calls `comebackRescueScheduler.planAll()` (it loads reachable learners itself since the review fixes).
- `@WebMvcTest` controller tests (with `ControllerTestSecurityConfig`): happy paths, 404 for unknown preview, 400 for bad index.
- Commit `feat(rescue): admin API for previews, ratings, plans and funnel`.

### Task 6: Admin page

- `admin-ui/rescue.html` + `admin-ui/rescue.js`, served by `NotificationDashboardController` (`/admin-ui/rescue`, `/admin-ui/rescue.js`), reusing `notifications.css`, the CSP and the session sign-in flow from `support.js` (copy `request`/`api`/`withBusy`/session helpers; all user text through `esc`/`textContent`, no `innerHTML` with data).
- Sections: **Taste score** (chosen-candidate 👍 rate vs 80% target), **Generate previews** (count input + button), **Previews** (per preview: user id, source, chosen angle/title/body, outros; each candidate with angle, title, body, violations list, 👍/👎 buttons + note), **Nightly plans** (table), **Funnel** (table, days selector).
- Link from the notifications and support headers.
- Manual browser check after deploy (sign in, generate 3 previews, rate, see score update).
- Commit `feat(rescue): admin page for rescue taste test and funnel`.

### Task 7: Verify & ship

- `./gradlew build` green (JaCoCo ≥ 80%).
- Deploy `ali server`; `ali health`.
- Generate previews on prod admin page; confirm copy quality manually; leave `COMEBACK_RESCUE_SEND_ENABLED` unset.
- **Enabling sends (after taste test ≥ 80% 👍):** set `COMEBACK_RESCUE_SEND_ENABLED=true` in the server env on the VPS and redeploy; watch `/admin-ui/rescue` funnel for a week; compare `returned24h` rate of sent vs holdout.
