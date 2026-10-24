---
name: ad-video
description: Make Lexicon promo / advertisement videos with motion design (Remotion + claude-motion toolkit) — brand palette and type, the app's selling points in plain words, how to get real UI reference, formats per platform, storyboard → timeline → review loop → sound → MP4. Use whenever the task is an ad, promo, trailer, social clip, store promo video or animated feature explainer for Lexicon.
argument-hint: "<format> <length> <message>"
user-invocable: true
---

# Lexicon ad video

Not to be confused with `motion` (in-app Compose animation). This skill is about **marketing video**.

## Where the work happens

| What | Path |
|---|---|
| Video project (Remotion + sfx engine, MIT, cloned from `whaleyxbt/claude-motion`) | `~/projects/lexicon-motion` |
| Toolkit taste rules — **read before building** | `~/projects/lexicon-motion/.claude/skills/{motion-design,review-loop,sound-design}/SKILL.md` |
| Toolkit agent guide | `~/projects/lexicon-motion/AGENTS.md` |
| Lexicon brand assets (DM Sans TTFs, 1024 app icon) | `~/projects/lexicon-motion/public/lexicon/` |
| Real UI reference frames | `~/projects/lexicon-motion/public/lexicon/ref/` |
| Creative brief + kickoff prompt | `docs/marketing/ad-video-brief.md` (this repo) |

Claude Motion (the claude.ai artifact type) is Team/Enterprise only and not on this account — the local toolkit is the same idea (animation as code, render to MP4) with more control: sound, frame review, exact timing.

Requirements already met on the MacBook: Node 21, Python 3.14, ffmpeg 7.1. Remotion license: free for individuals / small teams.

## Pipeline (follow in order, never skip a step)

1. **Brief locked** — audience, platform + aspect, length, one message, one hero moment, CTA. Missing answers → ask the user, don't guess. Defaults in the brief doc.
2. **Reference captured** — real current UI, never the stale `assets/Screenshot_202603*` (UI changed since: Insights coach, Words list). See "UI reference" below.
3. **Storyboard** — scene table (time, picture, on-screen words, sound). Show it to the user before writing components.
4. **Timeline** — every beat as named seconds in `timelines/<video>.json`. No hardcoded times in components or cue sheet.
5. **Build** — composition in `src/lexicon/<Video>.tsx`, scenes in `src/lexicon/scenes/`, palette + fonts in `src/lexicon/brand.ts` only. Reuse toolkit `Mask`, `Roll`, `Grain`, `Background`, `sp`, `prog`, `band`.
6. **Review loop** — draft render → contact sheets → checklist → fix → repeat until a full pass is clean (toolkit `review-loop`). Phone-width readability is the main test.
7. **Sound** — cue sheet `sfx/cues/<video>.py` against the same timeline; −14 LUFS, true peak ≤ −1 dBFS.
8. **Final** — render, mux, re-sheet the final MP4, `npm run wave`. Report what was checked with sheet paths.

Effort: storyboard low/medium, scene building medium, review loop + polish high.

## Brand layer (only these values)

```ts
// src/lexicon/brand.ts
export const C = {
  violet: '#7F5AF0',      // primary — brand, focus, the "Lexicon" moment
  violetSoft: '#A78BFA',  // primary on dark
  lavender: '#C0BFF2',    // icon left shard
  iconFrom: '#7B91F0',    // icon gradient, bottom-left (sampled from app-icon-1024.png)
  iconTo: '#6C21DC',      // icon gradient, top-right (sampled)
  green: '#2CB67D',       // remembered / correct / progress
  mint: '#3DDC84',        // level-up highlight
  orange: '#FF8906',      // due / streak / urgency
  pink: '#E53170',        // forgot / error — use sparingly
  ink: '#16161A',         // dark background
  surfaceDark: '#1E1E1E',
  paper: '#F8F6F7',       // light background
  white: '#FFFFFE',
  muted: '#6B7280',
};
export const F = { ui: 'DM Sans', mono: 'JetBrains Mono' }; // DM Sans 400/500/600/700 from public/lexicon/fonts
```

- **One accent per frame.** Violet means "Lexicon / the answer". Green = remembered. Orange = due/streak. Never all three in one frame.
- Background: `ink` (dark) by default — app's dark theme, better on social feeds. Light `paper` only for a deliberate contrast scene.
- Type: DM Sans 700 for display (tight `-0.02em`, lineHeight 1), 500 for UI, JetBrains Mono uppercase labels. Display serif from the toolkit (Instrument Serif) only if the user approves — off-brand by default.
- **Logo**: icon = violet gradient tile with two geometric shards (lavender left, white right). Natural hero/end-card move: the two shards fly in and lock together, tile scales in behind. Never redraw the mark differently.
- Name is **Lexicon**, never "vokab" (package id is legacy). Platforms: Android · iOS · Web.

## What to sell (true claims only)

Benefit first, mechanism second. All of these exist in the app today:

| Benefit (say this) | Mechanism (show this) |
|---|---|
| Words come back just before you'd forget them | 7 stages, names exactly as the Study tab shows them: First Encounter → Getting Familiar (every 10 min) → Starting to Stick (daily) → Building Confidence (every 3 days) → Nearly Mastered (weekly) → Well Mastered (bi-weekly) → Fully Mastered (monthly). A missed word comes back in the next review |
| Stop typing word lists | Add words: **Scan a photo** (AI found 22 words on one book page), **Generate with AI** from a sentence ("Words for my first week in Berlin" → 50 words), type a word, import a file |
| Know exactly what to do today | Insights coach: due today, 7-day forecast, next action |
| Learn by ear too | TTS pronunciation, listening mode, plays in background |
| Keeps you coming back | Streaks, review reminders (8-word bites), Word Rush game, leaderboard |
| Your words everywhere | Sync across Android, iPhone, web; works offline |

Landing-page voice (alirezaiyan.com/lexicon): short, plain, second person — "Review at the right time", "Your words on every device". Match it. No invented numbers, ratings, user counts or testimonials. Premium features (photo import, AI suggestions, Insights, Word Rush) may be shown; don't imply they're free.

## UI reference

Recreate UI as React components (crisp at any size, animatable per element) — screenshots are *reference*, not footage. Exception: an App Store app preview must be mostly real captured app footage (Apple rule) — that's a different deliverable.

**Already captured (2026-10-24, dark theme, showcase account "Alex"):** 30 stills in `public/lexicon/ref/` (`01`–`28` by screen, plus `buchseite.jpg`, the German book page used for the photo scan). Footage, 1080×2400, in `public/lexicon/rec/`:

| Clip | Shows |
|---|---|
| `review-session.mp4` (41 s) | 10 cards: flip tilt, Got it / Nope, progress 1→10 |
| `review-complete.mp4` | 5-card round ending on "Perfect Score! 100%" + 13-day streak (still: `13-review-complete.png`) |
| `photo-import-extract.mp4` (36 s) | book page → scan-line animation → "We found 22 words" → list scroll |
| `ai-generate.mp4` (30 s) | typed request → "Building your word list" step loader → 50 words |
| `listening.mp4` (27 s) | hands-free playback: German word → pause → English + example |
| `word-rush.mp4` (42 s) | 10/10 round → grade S, "Lightning Fast", "New Streak Record!" |
| `tabs-tour.mp4` (37 s) | Study → Words → Insights (best week in 12 weeks, forecast) → Profile |
| `words-search.mp4` (7 s) | typing "reis" → das Reiseziel, die Reise |

Recapture or extend with the tools in `~/projects/lexicon-motion/capture/` (README there): showcase account, seeder, emulator driver. The account is curated: discard AI/photo results instead of adding them. Don't film the profile email or the AI result row "das Flughafen" (wrong article).

For a fresh account or store screenshots: `./maestro/record_showcase.sh <flow>` signs in as the showcase account. Read a flow before running it, because some add test words.

## Formats

| Use | Size | Length |
|---|---|---|
| Reels / TikTok / Shorts / Stories ads | 1080×1920 (9:16), safe area: keep 250 px top / 340 px bottom clear | 15–30 s |
| X / LinkedIn / feed | 1080×1350 (4:5) or 1080×1080 | 15–25 s |
| Play Store promo (YouTube link), landing page hero | 1920×1080 (16:9) | 30–45 s |

60 fps. First frame must be a real image (autoplay muted = thumbnail). Assume sound off: every message readable without audio, captions are on-screen type, not a VO transcript.

## Ad structure (default for 20–30 s)

1. **Hook 0–2 s** — the pain, in ≤ 6 words, big ("You learned 50 words. You remember 5.").
2. **Turn 2–5 s** — Lexicon appears (logo shards lock in) or the first card flips.
3. **Proof 5–20 s** — 2–3 features max, one per beat, each a persistent object that transforms into the next (photo → words → cards → forecast). No feature tour.
4. **Hero moment** — one, e.g. a word climbing all 7 stages to Mastered.
5. **End card ≥ 2 s** — icon, "Lexicon", one-line promise, Android · iOS · Web, store badges only if user supplies official assets.

## Don'ts

- Stale screenshots, fake UI that doesn't exist in the app, made-up stats.
- More than 3 features in one ad.
- Text moving while it should be read; holds shorter than `motion-design` §4.
- New hex values outside `C`; fonts outside `F`.
- Saying "done" without contact sheets of the current render.
