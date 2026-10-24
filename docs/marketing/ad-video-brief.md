# Lexicon ad video — creative brief

Status: **draft — open questions below must be answered before storyboard.**
Skill: `.claude/skills/ad-video/SKILL.md` · Project: `~/projects/lexicon-motion`

## 1. Lessons from the sources

| Source | Takeaway we use |
|---|---|
| Claude Motion (support.claude.com) | Animation = code, not a video model: no realistic footage or people; any word/number/timing editable. Say *who watches, where it plays, how long*; attach your own content. Motion artifact type is Team/Enterprise only → we use the local toolkit. |
| claude-motion repo | Taste rules with numbers, timeline JSON as single clock, contact-sheet self-review, procedural sound to −14 LUFS. 15 s cut of their example flashed text too fast → 22 s final. Hold longer, animate less. |
| Matt Loui prompts (Google Doc) | Every good result started from **real reference** (screen recording/screenshots of the actual UI) + "use the same design, animate it in Remotion" + **"ask me as many questions as you need"**. Praise + reuse what worked for the next piece. |
| Reddit guide | Not readable from here (reddit blocked in browser pane). Paste it if it has something specific. |

## 2. Defaults (change any)

| Item | Default |
|---|---|
| Goal | Installs from paid social + organic posts |
| Audience | Adults learning a 2nd language (B-level, e.g. German/English), already tried flashcard/Duolingo-style apps, forget words |
| Primary format | 1080×1920 9:16, 60 fps, 25 s |
| Cutdowns | 1080×1350 4:5 (feed), 15 s 9:16 (hook + hero + end card) |
| Sound | Procedural SFX + pad, no voiceover; works fully muted |
| Language on screen | English |
| Message | "Remember the words you learn." |
| Hero moment | One word climbs all 7 stages, Fresh → Mastered, its review dots spreading out over a time axis |
| Features (max 3) | Photo → words (import), spaced repetition (hero), today's plan (Insights coach) |
| CTA / end card | Icon shards lock in → "Lexicon" → "Remember the words you learn." → Android · iOS · Web |
| Theme | Dark (`ink` #16161A), violet accent |

## 3. Draft beat sheet (25 s, 9:16)

| t (s) | Picture | On-screen words |
|---|---|---|
| 0.0–2.2 | Word cards pile up fast, then fade to grey one by one | "You learned 50 words." → "You remember 5." |
| 2.2–4.5 | One grey card turns, violet edge; icon shards slice through | "Lexicon" |
| 4.5–9.0 | Phone frame: photo of a book page, words light up and lift off into a list | "Snap a page. Get the words." |
| 9.0–16.5 | **Hero**: one word card, Remembered taps; stage meter climbs 7 steps, review dots spread along a 1 min → 30 days axis | "Back right before you forget." |
| 16.5–21.0 | Cards collapse into the Insights coach: due today, 7-day forecast bars | "Know what to do today." |
| 21.0–25.0 | End card, holds ≥ 2.5 s | "Lexicon — Remember the words you learn." · Android · iOS · Web |

## 4. Open questions (answer before storyboard)

1. Where will it run first? (Instagram/TikTok ads, Play Store promo, landing page, X/LinkedIn)
2. Who exactly is it for, and which language pair should the demo words use?
3. One message to keep if everything else is cut?
4. Length: 15 / 25 / 30–45 s? Cutdowns needed?
5. Voiceover or text-only? Music bed vibe (calm/focused vs upbeat/punchy)?
6. CTA: store badges (need official badge assets), URL (alirezaiyan.com/lexicon), or none?
7. Show premium features (photo import, Insights)? Mention pricing/free trial?
8. Reference ads you love (links/recordings) — style to aim at?

## 5. Prep checklist

- [x] Toolkit cloned + installed, example renders, contact sheets work, sfx masters to −14.0 LUFS
- [x] Brand fonts (DM Sans) + 1024 icon copied to `public/lexicon/`
- [x] Brand palette + claims + formats written into the `ad-video` skill
- [x] Fresh UI reference captured (dark theme, showcase account): 30 stills + 8 clips, inventory in the `ad-video` skill
- [x] `src/lexicon/brand.ts` + DM Sans font loader (verified with the `LexiconBrandCheck` still)
- [ ] Brief answers locked
- [ ] Storyboard approved

## 6. What the real app gives us (from the capture)

Real moments worth building scenes on, all true on the showcase account:

- **Photo scan**: one book page → scan-line sweeps the page → "We found 22 words" with translations and genders. Strongest "wow" for the import beat.
- **Generate with AI**: one sentence ("Words for my first week in Berlin") → 4-step loader → 50 words.
- **Review**: big German word on a dark card, flip, Got it / Nope; round ends "Perfect Score! 100%" with "13-day streak".
- **Insights**: "Your best week in 12 weeks", 136 reviews, 93% accuracy, 7-day forecast bars.
- **Word Rush**: 10/10, grade S, "Lightning Fast", "New Streak Record!" Possible energetic end beat.

Copy to avoid: the in-app stage names differ between the Study tab ("Starting to Stick", "Building Confidence"…) and the card badges ("Familiar", "Confident", "Nearly There"…). Use the Study tab names on screen.

## 7. Kickoff prompt (paste in a session at `~/projects/lexicon-motion` once §4 is answered)

```
Read AGENTS.md, the skills in .claude/skills/, and
~/projects/Lexicon/.claude/skills/ad-video/SKILL.md plus
~/projects/Lexicon/docs/marketing/ad-video-brief.md.

Make a <LENGTH>-second <WIDTH>x<HEIGHT> 60 fps ad for Lexicon for <PLATFORM>,
aimed at <AUDIENCE>. One message: "<MESSAGE>". Hero moment: <HERO>.
Features (max 3): <F1>, <F2>, <F3>. End card: <CTA>.

Reference UI is in public/lexicon/ref/ — recreate it as React components in the
same design, don't paste screenshots. Brand values only from the ad-video skill.

Order: storyboard table → show me → timelines/<name>.json → scenes in
src/lexicon/ → review loop until a full contact-sheet pass is clean (phone width!)
→ cue sheet + sound → final render + mux + wave check.
Ask me as many questions as you need before the storyboard.
```
