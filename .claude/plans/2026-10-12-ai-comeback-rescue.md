<!-- plan-type: feature -->
# AI Comeback Rescue

## Goal
Win back learners in the first days of a lapse, before the habit dies. When a user who had a study habit skips 2+ days, the AI reads their word-level analytics, picks a **3-minute "quick win" session of 8 words** (~6 near-mastery confidence words + ~2 weak words they are close to cracking), and writes a personal push that names real words. Tapping the push opens a review of exactly those words via deep link — no backlog, no chore, just a win that restarts the habit.

## Background

### What exists today
- **Server (`lexicon.server`)**
  - `EngagementSegmentScheduler` (01:00 UTC nightly) — computes COLD/DORMANT segments, calls `NotificationAiAdvisor`.
  - `NotificationAiAdvisor` — OpenRouter (`anthropic/claude-haiku-4.5`) via `AiClient` + `PromptTemplates`. Picks only `send|pause|motivate` + 1 of 5 generic content angles. Sees engagement stats only (open rates, streak, due count) — **never word-level data**.
  - `NotificationTypeSelector.selectType` — one push per day; priority: streak risk → milestone → weekly recap → add words → `DUE_CARDS` → `COMEBACK_ALERT` (single difficult word) → practice nudge → insight.
  - `SmartNotificationDispatcher` (every 30 min) — sends at user's usual study hour.
  - `NotificationContentBuilder` — template copy + `deep_link`.
  - `LearnerSignals` / `WordProgressQueries` — `getDifficultWords`, `getComebackWords`, `getWordsMastered`, accuracy, practice history.
  - Last Flyway migration: `V45__add_support_chat.sql`.
- **Client (`Lexicon`)**
  - `NotificationTapHandler` — maps `deep_link` to navigation. `vokab://review/*` → due review; `vokab://word/{id}` → `ReviewSource.ByWords(listOf(id))`.
  - `ReviewSource.ByWords(wordIds)` — review of a hand-picked word set, not capped by daily goal. **Session engine needs no changes.**
  - `ReportNotificationOpenedUseCase` — reports push opens.

### Gap this feature fills
1. AI never sees which words the user is weak/strong on.
2. Copy is template, not personal.
3. Push lands in the full due backlog — the exact thing a lapsed user avoids.

### Decisions made
- **Session mix:** 70/30 — confidence words + a few weak words (option A).
- **Trigger:** early lapse (2+ days off) for users who had a habit; later stages keep the existing flow.
- **Approach:** hybrid — code builds candidate pool from analytics, AI picks final words + writes copy, code validates with deterministic fallback.
- **Delivery:** push with deep link carrying the word IDs + rescue ID.

## Design

### 1. Trigger & scheduling
- New `NotificationType.COMEBACK_RESCUE` (separate from `COMEBACK_ALERT`, which stays the single-word rematch).
- **`ComebackRescuePolicy`** (pure rules, unit-tested). Eligible when all hold:
  - last `DailyActivity` ≤ today − 2 (2+ full days off);
  - ≥ 3 study days in last 14 (had a habit);
  - ≥ 10 words in deck;
  - no rescue already `SENT` in this lapse (since last study day).
- **Nightly planning:** new step in the 01:00 UTC job, after segment refresh. For each user eligible today → build plan (1 AI call) → persist as `PLANNED`. AI runs outside any transaction (same as `NotificationAiAdvisor`).
- **Dispatch:** `NotificationTypeSelector.selectType` checks rescue **right after `studiedToday`**, before the 3-ignores re-engagement branch and before `DUE_CARDS`. Picks `COMEBACK_RESCUE` when a `PLANNED` plan exists for today and the user has not studied since planning. Sent at the user's usual study hour by the existing dispatcher.
- Plans for users who studied before send time → `EXPIRED`. Plans older than today → `EXPIRED` by the nightly job.

### 2. Candidate pool, prompt, validation

**Candidate pool** (`RescueCandidateQueries`, SQL, single language pair = `primaryTargetLanguage`):
| Bucket | Size | Criteria | Purpose |
|---|---|---|---|
| Near-mastery | up to 15 | level 3–5, recent accuracy ≥ 75%, due or overdue | confidence wins |
| Weak | up to 8 | `getDifficultWords` + `getComebackWords`, ≥ 1 correct answer ever | the "crack it" moment |

Each candidate carries: `wordId`, text, translation, level, totalReviews, errorRate, last-5 results, days since last review.

If pool has < 6 near-mastery or < 1 weak → skip AI, use fallback directly.

**Prompt** — new `Prompt.COMEBACK_RESCUE` template in `PromptTemplates`. Inputs: user profile (days away, longest streak, total words, level distribution, language pair) + candidate list (JSON). Instructions:
- choose exactly 8 IDs: 5–6 near-mastery, 2–3 weak;
- avoid putting easily confusable words (look-alikes, same-root, near-synonyms) in one session;
- prefer weak words with recent partial success over hopeless ones;
- vary topic/part of speech;
- **order** the 8 for the session arc (see 2b): open with 2 easy wins, weak words in the middle, close on an easy win;
- write copy per section 2b (3 candidates, grounded in facts);
- return one-line `reason` per pick.

Response schema: see 2b.

**Validation** (`RescuePlanValidator`, pure): every ID ∈ pool; 6–10 IDs, unique; ≥ 1 weak, ≥ 4 near-mastery; first and last ID are near-mastery. Copy validated separately by `RescueCopyLinter` (2b). Word-pick failure → full fallback; copy-only failure → keep AI picks, use template copy.

### 2b. Content craft — copy users like, not "AI-generated"

**What makes AI copy feel AI (and gets ignored):** generic hype ("Ready to crush it? 🚀"), vague praise, guilt ("We miss you!", "Don't lose your progress"), invented stats, em dashes, emoji confetti, the same shape every time, "journey/unlock/level up/boost".

**What users actually like in a comeback push:** one *true, specific* thing about *them*, their own word on screen, a small honest ask, and no pressure. It should read like a note from a friend who happens to know their deck.

#### Principles (encoded in prompt + linter)
1. **Grounded, never invented.** The AI only gets a numbered **fact sheet** built by code, and every candidate must cite the fact IDs it uses. Any number or word in the copy must come from a cited fact.
2. **The word is the hero.** Show at least 1 real target-language word from the session, written in the target language. Learners like seeing their own words; a word they recognise gets the tap.
3. **Honest, small ask.** State the real cost: "8 words, about 3 min". No "quick!" without the number.
4. **Zero guilt.** No streak shame (the streak is usually already broken), no "we miss you", no loss framing. At most a light nod to the time away, or none at all.
5. **One idea per push.** The title hooks; the body adds 1 detail + the ask. No lists.
6. **Consistent Lexicon voice.** Warm, dry, brief, a little playful. Plain words. Sounds like a person.
7. **Never the same angle twice in a row** for a user.

#### Fact sheet (built by `RescueFactSheetBuilder`, deterministic)
Only true, checkable facts, each with an ID. Example for a German learner:
```
F1 daysAway=3
F2 weakWord="Gewitter" (thunderstorm) missed 3 of last 5, correct last attempt
F3 nearMasteryWord="Schmetterling" level 5, one review from mastered
F4 nearMasteryCount=5 words at level 5
F5 firstWord="Apfel" added 2026-03-02
F6 totalWords=212 language=German
F7 longestStreak=19
F8 sessionSize=8 estMinutes=3
```
Facts outside the session's 8 words (e.g. F5 first word) are allowed only as "callback" hooks and only when the word is in the deck.

#### Angles (AI picks the one that fits the facts best, excluding the user's last angle)
| Angle | Fits when | Gold example (title / body) |
|---|---|---|
| `rematch` | weak word with recent partial success | **Gewitter, round 3** / You got it right last time. 8 words, about 3 min, mostly ones you already know. |
| `almost_there` | near-mastery word one review from top level | **Schmetterling is one review from mastered** / 4 more are right behind it. About 3 min. |
| `callback` | first word / long-held word still in deck | **Remember Apfel?** / Your first German word. It's still yours, along with 7 others worth 3 minutes today. |
| `progress_fact` | meaningful total or level count | **You know 212 German words** / Keep them warm: 8 short ones, about 3 min. |
| `curiosity` | strong weak word with clear translation | **Still know what Gewitter means?** / One way to find out. 8 words, 3 min. |

The gold examples are **hand-written by Ali** and go into the prompt as few-shot examples, together with 3 **anti-examples** each labelled with what's wrong:
- ❌ "Ready to crush your German goals? 🚀" — generic, hype, no fact
- ❌ "Don't let your progress slip away!" — guilt
- ❌ "You've mastered 500 words!" — invented number

These hand-written examples are the biggest lever on quality. Prompt wording matters less than these few-shots.

#### Generate 3, select 1
One AI call returns **3 candidates with different angles**, each with `angle`, `title`, `body`, `factIds`, and the AI's own ranking. Then code (`RescueCopyLinter`, pure) drops any candidate that breaks a rule, and the highest-ranked survivor wins. If all 3 fail, template copy for the best-fitting angle is used. Cost stays at 1 Haiku call; quality comes from selection, not a second call.

**Linter rules (hard fails):**
- title ≤ 40 chars, body ≤ 110 chars (measured in graphemes, so non-Latin scripts work)
- contains ≥ 1 session word in target language
- every digit sequence in copy appears in a cited fact; every quoted/target word is in the fact sheet
- banned phrases (configurable list): journey, unlock, crush, level up, boost, supercharge, don't lose, don't break, we miss you, ready to, let's go, hey there, oops
- no em dash (—), ≤ 1 exclamation mark, ≤ 1 emoji and only from an allowlist (default: none)
- angle ≠ user's last rescue angle
- not a near-duplicate (normalized-text match) of the user's last 3 rescue copies

#### Language & locale
- Copy is in the user's **app UI language** (user locale from settings, falling back to English). Target words stay in the target language and script.
- Few-shot gold examples exist in English; the prompt asks the AI to write naturally in the UI language, not translate the examples word for word.
- RTL languages (e.g. Persian UI): grapheme-length check; no Latin punctuation tricks.

#### Session arc — content doesn't stop at the push
- **Order (peak-end rule):** 2 easy wins first (momentum), weak words in the middle, finish on an easy win (ends on a high).
- **Outro lines**, generated in the same call and grounded in the same facts. There are 2 variants and the client picks one by result:
  - `outroWin` (weak word answered right): "Gewitter: cracked. That's 8 for 8 on a 3-minute comeback."
  - `outroSoft` (weak word missed): "8 words back in rotation. Gewitter will come round again soon."
- Delivered **inside the push data payload** (`outro_win`, `outro_soft`), so no extra endpoint and it works offline. If they're absent, the client shows the normal session-end screen.
- Outros go through the same linter (≤ 90 chars, no guilt, no invented numbers).

**Response schema (final):**
```json
{
  "wordIds": [45, 12, 78, 90, 162, 101, 133, 150],
  "reasons": { "78": "weak, 2/3 recent correct", "45": "level 5, easy opener" },
  "candidates": [
    { "angle": "rematch", "title": "Gewitter, round 3", "body": "You got it right last time. 8 words, about 3 min, mostly ones you already know.", "factIds": ["F2","F8"], "rank": 1 },
    { "angle": "almost_there", "title": "Schmetterling is one review from mastered", "body": "4 more are right behind it. About 3 min.", "factIds": ["F3","F4","F8"], "rank": 2 },
    { "angle": "curiosity", "title": "Still know what Gewitter means?", "body": "One way to find out. 8 words, 3 min.", "factIds": ["F2","F8"], "rank": 3 }
  ],
  "outroWin": "Gewitter: cracked. That's 8 for 8 on a 3-minute comeback.",
  "outroSoft": "8 words back in rotation. Gewitter will come round again soon."
}
```

#### Making sure users actually like it (not just "passes lint")
1. **Pre-launch taste test.** A script generates rescue plans for 50 real users (word data only, no PII) and shows them on an admin review page with 👍/👎 + note per candidate. Iterate the gold examples and prompt until **≥ 80% 👍 by Ali**. The rated set is saved as a regression eval (`rescue_copy_eval.jsonl`).
2. **Shadow week.** Plans are generated nightly but not sent. Ali skims a daily sample on the admin page.
3. **Live learning.** Every sent rescue logs `angle` + `source`. The admin funnel shows **open rate and completion rate per angle**. v1.1: weight angle choice per user by their past opens (simple, no bandit framework).
4. **Regression gate.** Any prompt or gold-example change reruns the eval set; at most a 5-point drop in the 👍 rate (re-rated by Ali on the diff only).

**Fallback** (`RescueFallbackPicker`, deterministic): top 6 near-mastery by recent accuracy, top 2 weak by recent success; copy from a template in `NotificationContentBuilder` that names the top weak word. `source = FALLBACK`.

AI errors (`UpstreamServiceException`, null, parse fail) → fallback. Every rescue that goes out has either AI or fallback content — no silent skips due to AI.

Tracked with `AiCallTracker` under new `AiOperation.COMEBACK_RESCUE` (cost visibility).

### 3. Push & deep link
- Content built from the plan: `title`, `body`, `type = comeback_rescue`, `deep_link = vokab://review/rescue?ids=12,45,78,...&rid=<rescueId>` (IDs in session order), `rid`, `outro_win`, `outro_soft` as data fields.
- On send: plan → `SENT`, `sent_at`; write `NotificationLog` as today (open tracking reuses existing path).
- Existing opened-report flow sets plan → `OPENED` when push data carries `rid`.
- **Backward compatibility:** old clients match `startsWith("vokab://review/")` → open due review. Acceptable degradation; no version gate needed.

### 4. Client (Lexicon)
- `NotificationTapHandler.openDeepLink`: new branch for `vokab://review/rescue` **placed before** the generic `vokab://review/` branch.
  - Parse `ids` → `List<Long>`, drop invalid, cap at 20; parse `rid`.
  - Non-empty → `navigator.openReview(ReviewSource.ComebackRescue(rescueId, wordIds))`; empty → `openDueReview()`.
- New `ReviewSource.ComebackRescue(rescueId: Long, wordIds: List<Long>, outroWin: String?, outroSoft: String?)` — behaves as `ByWords` for word loading but **preserves the given order** (session arc), `sessionTypeLabel = "RESCUE"` so analytics sessions are distinguishable. Words missing locally are skipped; if none remain → due review.
- On session completion of a `ComebackRescue` source → `ReportRescueCompletedUseCase(rescueId)` → `POST /api/v1/notifications/rescue/{rid}/complete`. Fire-and-forget, `Try<Unit>`, failure ignored (same contract as open reporting).
- Layers: domain use case + repository interface; data Ktor data source; DI registration in `AppModule.kt`.
- No new screen. Optional polish (v1.1): session-end message "Welcome back — 8/8" when source is rescue.

### 5. Backend API & data
- **Migration `V46__add_comeback_rescue.sql`:**
  - `comeback_rescue(id bigserial pk, user_id fk, planned_for date, word_ids jsonb (ordered), title, body, angle varchar, outro_win, outro_soft, candidates jsonb (all 3 + lint results, for tuning), picks_reasoning jsonb, source varchar (AI | AI_PICKS_TEMPLATE_COPY | FALLBACK), status varchar, created_at, sent_at, opened_at, completed_at)`
  - `rescue_copy_rating(id, rescue_id fk, candidate_index, rating, note, rated_at)` — admin taste-test ratings
  - unique `(user_id, planned_for)`; index `(status, planned_for)`.
- **Endpoint:** `POST /api/v1/notifications/rescue/{id}/complete` — JWT auth, must own the rescue, idempotent, sets `COMPLETED` + `completed_at`. 404 for foreign/missing.
- Admin dashboard (`NotificationDashboardController`): add rescue funnel counts by `source`.

### 6. Metrics & success criteria
- **Funnel per source (AI vs FALLBACK):** planned → sent → opened → completed.
- **Return rate:** studied within 24h of send.
- **7-day retention after rescue:** ≥ 3 study days in the 7 days after send.
- **Holdout:** 10% of eligible users (stable hash of userId) get a plan built but **not sent** (`status = HOLDOUT`) and follow the existing flow — the only way to prove the rescue beats current pushes.
- Success target (initial): rescue cohort 24h return rate ≥ 1.5× holdout.

### 7. Testing
- **Server (JUnit 5 + MockK, Testcontainers PG):**
  - `ComebackRescuePolicyTest` — eligibility edges (1 vs 2 days off, 2 vs 3 habit days, < 10 words, already sent in lapse, studied today).
  - `RescuePlanValidatorTest` — foreign IDs, duplicates, count bounds, bucket mix, first/last near-mastery.
  - `RescueFactSheetBuilderTest` — only true facts, stable IDs, callback word only when in deck.
  - `RescueCopyLinterTest` — each rule: lengths (graphemes, RTL), target word present, ungrounded numbers/words, banned phrases, em dash, emoji, repeated angle, near-duplicate; best-ranked survivor chosen; all-fail → template.
  - `RescueFallbackPickerTest` — deterministic order, thin pools.
  - `ComebackRescuePlannerTest` — AI success, AI exception → fallback, unparsable JSON → fallback, thin pool → fallback without AI call.
  - `NotificationTypeSelectorTest` — rescue beats `DUE_CARDS` and re-engagement branch; studied-since-plan → not selected.
  - `RescueCandidateQueriesTest` (Testcontainers) — bucket criteria, single language pair.
  - Controller test — complete endpoint: owner OK, idempotent, foreign → 404.
- **Client (kotlin-test + Turbine, fakes):**
  - `NotificationTapHandlerTest` — rescue link parsed before generic review branch; malformed IDs; empty → due review.
  - `ReportRescueCompletedUseCaseTest`, data source test with `MockEngine`.
  - Review ViewModel test — completion of rescue source triggers report once; other sources don't.
- **E2E:** local backend + emulator — seed user with lapse, run planner manually, send push via admin, tap → review shows exactly the 8 words → completion marks row `COMPLETED`. Maestro flow opening the deep link via `adb shell am start -d`.

## User Stories
- [ ] As a learner who skipped 2 days, I get a push naming words I actually know/struggle with, so coming back feels personal, not generic.
- [ ] As that learner, tapping the push starts a 3-minute session of exactly those 8 words, so I get a quick win instead of a backlog.
- [ ] As a learner who already came back on my own, I don't get a rescue push for a lapse I already ended.
- [ ] As the product owner, I can see AI vs fallback vs holdout funnels, so I know whether AI picks actually bring users back.

## Implementation Steps

### Backend first (`~/projects/lexicon.server`, JDK 21)
- [ ] `V46__add_comeback_rescue.sql` + `ComebackRescue` entity + repository
- [ ] `ComebackRescuePolicy` (eligibility) — TDD
- [ ] `RescueCandidateQueries` (near-mastery + weak buckets) — TDD with Testcontainers
- [ ] `RescueFactSheetBuilder` — TDD
- [ ] Gold examples + anti-examples (hand-written by Ali) + `Prompt.COMEBACK_RESCUE` template + `AiOperation.COMEBACK_RESCUE`
- [ ] `RescuePlanValidator` + `RescueCopyLinter` + `RescueFallbackPicker` + angle templates — TDD
- [ ] Admin taste-test page (generate for 50 users, 👍/👎 per candidate) + `rescue_copy_eval.jsonl`; iterate to ≥ 80% 👍
- [ ] Shadow week (plan, don't send) before enabling sends
- [ ] `ComebackRescuePlanner` (pool → AI → validate → fallback → persist, holdout assignment) — TDD
- [ ] Hook planner into nightly 01:00 job after segment refresh; expire stale plans
- [ ] `NotificationType.COMEBACK_RESCUE` + selector priority + content builder (deep link, `rid`)
- [ ] Mark `SENT` on dispatch; mark `OPENED` from existing open-report path when `rid` present
- [ ] `POST /api/v1/notifications/rescue/{id}/complete` + controller test
- [ ] Admin dashboard funnel
- [ ] `./gradlew build` green, coverage ≥ 80%

### Client (`~/projects/Lexicon`)
- [ ] `ReviewSource.ComebackRescue(rescueId, wordIds, outroWin?, outroSoft?)` — keeps given word order; handled wherever `ByWords` is; `sessionTypeLabel = "RESCUE"`
- [ ] `NotificationTapHandler` rescue branch (before `vokab://review/`) — TDD
- [ ] `ReportRescueCompletedUseCase` + repository/data source (Ktor) + DI in `AppModule.kt` — TDD
- [ ] Review ViewModel: on completion of rescue source → report (run `analytics-auditor` agent on the change)
- [ ] Session-end outro: pass `outro_win`/`outro_soft` through `ReviewSource.ComebackRescue`; show by weak-word result; absent → default end screen
- [ ] `./gradlew composeApp:cleanAllTests composeApp:allTests` green; `compileKotlinMetadata` OK

### Verify & ship
- [ ] Local E2E: lapsed seed user → planner → push → tap → exact 8 words → `COMPLETED`
- [ ] Maestro flow for rescue deep link
- [ ] Deploy backend (`ali server`), verify health, then ship client
- [ ] After 2 weeks: compare AI vs fallback vs holdout; tune prompt using `picks_reasoning`

## Open Questions
- **Premium gating?** Default: free for all — retention outweighs ~1 Haiku call per lapsed user per lapse. Gate later if cost shows up in `AiCallTracker`.
- **In-app comeback card** when the user returns on their own (no push tap) — reuse today's plan as a "Welcome back, 3-min win" card on the Study screen? Default: v1.1.
- **Second attempt** if rescue is ignored — one more at day 5 with a different mix, or hand over to existing flow? Default: hand over (one per lapse).
- **Word ID identity** — confirm server `wordId` from analytics equals the client local ID used by `ReviewSource.ByWords` (Insights coach cards and `vokab://word/{id}` already rely on this; verify before building).
- **Holdout size** — 10% default; lower if user base is small and results take too long.

## Risks
- **AI picks bad or invented words** → validator restricts to pool IDs; any failure falls back. Users never see invalid sessions.
- **Copy hallucinates a word or number** → fact sheet + cited fact IDs + linter grounding check; template copy if all candidates fail.
- **Copy feels generic/AI-ish** → hand-written gold examples, banned-phrase linter, 3-candidate selection, pre-launch taste test ≥ 80% 👍, per-angle open-rate tracking.
- **Push fatigue** → one rescue per lapse; it replaces (not adds to) that day's single push.
- **Words deleted locally since planning** → client skips missing IDs; empty → due review.
- **Old clients** → rescue link degrades to due review via existing prefix match.
- **Nightly job duration** → AI calls only for eligible users (small subset); calls sequential with existing `AiClient` timeout; failures isolated per user (`runCatching`, like `EngagementSegmentScheduler`).
- **Privacy** → only word text/translation + aggregate stats go to the AI; no email, name or tokens. Same exposure as existing `/ai/*` features.
- **Analytics session correctness** — new `ReviewSource` touches session lifecycle; run `analytics-auditor` before merge.
