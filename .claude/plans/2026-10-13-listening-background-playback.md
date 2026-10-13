<!-- plan-type: feature -->
# Listening Mode: Background Playback + Live Media Notification

## Goal
Listening mode keeps playing when the app goes to the background or the screen locks, on Android and iOS. While a session plays, a sticky media notification (Android) / Now Playing card (iOS lock screen, Control Center, Dynamic Island) shows the current word and its translation, updates live, and offers play/pause, previous, next and stop. When the session ends, the notification becomes dismissable and offers **Repeat** and **Close**.

## Background

### Current state (verified in code)
- **The sequencer lives in the ViewModel.** `ListeningViewModel.play()` loops `performStep → ListeningReducer.reduce` inside `viewModelScope`. `ListeningViewModel` is a `koinViewModel` of `StudyScreen` (`presentation/.../StudyScreen.kt:121`), so playback dies when the activity/VM is cleared (`onCleared` stops speech).
- **Android:** no foreground service, no `FOREGROUND_SERVICE*` / `WAKE_LOCK` permissions, no MediaSession, no audio focus. `AndroidTtsEngine` plays through a static `AudioTrack` (`USAGE_MEDIA` / `CONTENT_TYPE_SPEECH`). In the background the process gets frozen or killed (cached app, Doze), and with the screen off the CPU sleeps during synthesis and the `delay()` gaps.
- **iOS:** `Info.plist` `UIBackgroundModes` has only `remote-notification`, with no `audio`. `IosTtsEngine` calls `setCategory(Playback)` + `setActive(true)` on **every utterance**, writes a WAV to tmp and plays it through `AVAudioPlayer`. Even with the `audio` background mode, iOS suspends an app that isn't actively outputting audio. Pause/gap steps (`delay(pauseMs)`, `delay(gapMs)`) and synthesis time are silent, so the app would be suspended mid-session.
- **Reusable pieces:** `ListeningReducer` is already a pure state machine, which is good. `platforms` already depends on `domain` and hosts notification code (`AndroidNotificationManager`, `NotificationTapIntent.openAppPendingIntent`). iOS registers notification categories in `iOSApp.swift`.

### Constraints
- **Hard:** Android 12+ can't start a foreground service from the background, so it must start while the user taps Play. Android 14 needs `foregroundServiceType="mediaPlayback"` + the `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission. iOS keeps an app alive in the background only while its audio session is active and producing output.
- **Hard:** iOS has no "sticky notification". The native equivalent is the Now Playing card (`MPNowPlayingInfoCenter` + `MPRemoteCommandCenter`), which also gives lock-screen and Dynamic Island controls.
- **Soft:** Module boundaries. `domain` stays pure. Platform APIs live in `platforms`. Feature orchestration lives in `feature/study`.

## Architecture

```
                 ┌───────────────────────────── feature/study ─────────────────────────────┐
 ListeningVM ──▶ │ ListeningPlaybackController (singleton, app-scoped CoroutineScope)      │
 (setup, voices, │   owns: ListeningSession, isPlaying, tracker, words, play loop          │
  renders state) │   exposes: StateFlow<ListeningPlayback>  (Idle | Playing | Finished)     │
                 │   commands: start/toggle/next/previous/repeat/close                      │
                 │                                                                          │
                 │ ListeningMediaBridge  ── maps state → NowPlayingInfo, commands → controller│
                 └──────────────┬───────────────────────────────────────────▲──────────────┘
                                │ IListeningMediaSession (platforms/commonMain)│ Flow<MediaCommand>
             ┌──────────────────┴────────────┬───────────────────────────────┴──────┐
   Android:  ListeningPlaybackService (FGS mediaPlayback) + MediaSessionCompat + MediaStyle
             + audio focus + BECOMING_NOISY + partial WakeLock
   iOS:      IosListeningMediaSession: AVAudioSession(playback, spokenAudio) held for session
             + silent keep-alive output + MPNowPlayingInfoCenter + MPRemoteCommandCenter
             + interruption/route-change observers + "finished" local notification
   Web:      no-op
```

**Why this split scales:**
- One source of truth, the controller. The screen, the notification, the lock screen and Bluetooth/headset buttons all read the same `StateFlow` and send the same commands. Adding a widget, Wear or CarPlay later means adding another consumer, with no sequencer changes.
- The platform port is a dumb renderer and input. It has no knowledge of words, the reducer or the tracker, so it unit-tests with a fake.
- The ViewModel shrinks to setup and voice pre-flight. It survives activity death because playback no longer depends on it.

### Contracts

```kotlin
// platforms/commonMain/kotlin/media/IListeningMediaSession.kt
data class NowPlayingInfo(
    val title: String,          // current prompt (word or translation, per order)
    val subtitle: String?,      // answer once revealed, else null
    val position: Int,          // 1-based word index
    val total: Int,
    val isPlaying: Boolean,
)

sealed interface MediaSessionPhase {
    data class Playing(val info: NowPlayingInfo) : MediaSessionPhase
    data class Finished(val wordsHeard: Int, val total: Int) : MediaSessionPhase  // dismissable, Repeat/Close
}

enum class MediaCommand { Play, Pause, TogglePlayPause, Next, Previous, Repeat, Close }

interface IListeningMediaSession {
    val commands: Flow<MediaCommand>
    fun render(phase: MediaSessionPhase)   // idempotent; first Playing render starts service/session
    fun release()                          // tear down notification/Now Playing + audio session
}
```

```kotlin
// feature/study/.../listening/playback/ListeningPlayback.kt
sealed interface ListeningPlayback {
    data object Idle : ListeningPlayback
    data class Playing(
        val session: ListeningSession,
        val isPlaying: Boolean,
        val speakableLanguages: Set<String>,
        val heardWordIds: Set<Int>,
    ) : ListeningPlayback
    data class Finished(val words: List<Word>, val wordsHeard: Int, val durationMs: Long) : ListeningPlayback
}
```

`NowPlayingInfo` mapping is a pure function (`ListeningPlayback.toMediaPhase(order)`), tested in isolation.

## UX Behavior Spec

Principle: **behave like a podcast app.** Users already know Spotify/Podcasts conventions; match them and nothing feels new or surprising. Every surface (screen, mini player, notification, lock screen, headset) shows the same state within one frame, because they all read one `StateFlow`.

### Leaving and returning
| Situation | Behavior |
|---|---|
| Home button / app switch / lock screen while playing | Keeps playing. No toast, no prompt. |
| Close (X) or back on the listening overlay **while playing** | Overlay collapses into a **mini player** above the bottom bar (word · translation · play/pause · ✕). Playback continues while the user browses the app. ✕ on the mini player stops and ends the session. |
| Close (X) while paused, on setup, or on the summary | Closes as today. Nothing keeps running. |
| Tap notification / Now Playing / mini player | Opens the overlay at the live position with no restart and no phase animation replay (it lands directly on the player). |
| Session finished while the app was in the background | Reopening shows the summary screen ("18 words · 7 min") with Listen again. The "finished" notification is cleared automatically once the user has seen the summary. |
| Android: app swiped from recents mid-session | Keeps playing (service owns playback). |
| iOS: app swiped away | iOS kills the app. Audio stops, Now Playing clears. Platform rule, accepted. |

### Notification / Now Playing content
- **Title:** the prompt word. **Subtitle:** the answer, appearing the moment it's spoken (same reveal as the screen, no spoilers). **Meta line:** "3 of 20 · German → English".
- **Artwork:** branded artwork (Lexicon mark on a theme gradient, bundled asset) so the lock screen and Android media carousel never show a blank grey tile. Small icon: monochrome app glyph. Android tint: brand primary.
- **Updates only when something visible changes** (new word, answer revealed, play↔pause), not on every pause/gap step. No flicker, no re-alert (`setOnlyAlertOnce`), silent channel.
- RTL-safe: Persian/Arabic text wrapped with `BidiFormatter` (Android) so mixed-direction pairs render correctly.
- Actions while playing: ⏮ Previous · ⏯ Play/Pause · ⏭ Next · ✕ Stop (compact view: prev/play/next).

### Controls feel
| Control | Behavior |
|---|---|
| Previous | Music-player rule: within the first 2 s of a word → previous word, otherwise → restart the current word from its prompt. |
| Next | Next word immediately, mid-utterance speech is cut cleanly (no half syllable tail). |
| Pause → Play | Resumes the current word from its prompt, so the user always hears the pair in context. |
| Stop (✕ in notification) | Ends the session (recorded as abandoned), notification disappears immediately, mini player hides. |
| Headset/BT single press | Play/pause. Double/triple press → next/previous (system mapped via media session). |
| Settings changed mid-session (speed, pause, order, repeat) | Applies from the next step, no restart, no position loss. |

### Audio etiquette
| Event | Behavior |
|---|---|
| Phone/FaceTime call, alarm, Siri (transient) | Pause. **Auto-resume** after it ends if we were playing, restarting the current word. |
| Another app starts music/video (permanent loss) | Pause. No auto-resume. |
| Navigation prompt / short notification sound (duck request) | **Pause briefly instead of ducking** (speech over speech is unintelligible), resume after. Android `setWillPauseWhenDucked(true)`. iOS: interruption handling covers it. |
| Headphones / BT disconnected | Pause (never blast through the speaker). Reconnect does **not** auto-play. |
| Starting listening while music plays | Behaves like a podcast: other audio pauses (iOS `spokenAudio` mode, Android focus gain). |
| Tapping a speaker icon elsewhere in the app while listening plays | Listening pauses, the tapped word speaks, listening stays paused (mini player shows ⏯). No interleaving. |

### Ending
- On completion: a soft **end chime** (short bundled earcon, ~400 ms) so a user with the phone in their pocket knows it's done. Plays at media volume, like the words themselves (the iOS playback category ignores the silent switch, which is correct for media).
- Notification turns into: "Nice work, 18 words heard · 7 min", dismissable, actions **Repeat** and **Close**. Same notification id (replaces, never stacks).
- Android: finished notification auto-times out after 30 min (`setTimeoutAfter`). iOS: Now Playing "Session complete" card clears after 10 min idle; the local notification stays in Notification Center until dismissed or the app is opened.
- **Repeat** starts instantly from the notification/lock screen without opening the app (same words, fresh session).

### Long pause / battery
- Paused for 10 min while backgrounded → Android leaves foreground (notification becomes swipeable, "Paused · tap to resume", Play still works). iOS keep-alive stops when paused anyway; Now Playing remains so the lock-screen Play resumes.
- WakeLock and keep-alive held **only while playing**.

### Permissions (Android 13+)
- First time the user taps Start in listening and notification permission was never asked: show the system prompt right then, preceded by a one-line rationale in the setup screen ("Show controls on your lock screen"). If denied: playback still works in the background, no nagging. A subtle row in listening settings ("Lock-screen controls off · Enable") deep-links to system settings.

### Accessibility
- Notification actions and remote commands have localized content descriptions. Mini player buttons ≥ 48 dp with TalkBack/VoiceOver labels ("Pause listening", "Stop listening"). Mini player respects reduced motion (no slide, fade only).

## User Stories
- [ ] As a learner, I want listening to keep playing when I lock my phone or switch apps, so that I can review hands-free.
- [ ] As a learner, I want to see the current word and translation in the notification / lock screen, so that I can glance without opening the app.
- [ ] As a learner, I want play/pause/next/previous from the notification, lock screen and headphones, so that I can control playback without unlocking.
- [ ] As a learner, when the session ends I want to replay it or close it from the notification, so that I can keep going or clean up.
- [ ] As a learner, I want playback to pause on a phone call or when I unplug headphones, so that audio doesn't blast unexpectedly.

## Implementation Steps

### Phase 1: Extract the sequencer (pure refactor, no behavior change)
- [ ] Create `ListeningPlaybackController` in `feature/study/.../listening/playback/` and move `play`, `performStep`, `speak`, `jump`, `haltPlayback`, `finish`, `endSession`, the tracker and analytics out of `ListeningViewModel`. Inject `CoroutineScope` (app-scoped `SupervisorJob + Dispatchers.Main`) and a `Clock` so tests control time.
- [ ] Expose `val playback: StateFlow<ListeningPlayback>` plus commands `start(words, speakable, selection, settings)`, `togglePlayback()`, `next()`, `previous()`, `repeat()` (replays the stored `words`; new tracker and new analytics session), `close()`, `abandon()`.
- [ ] The controller reads `ListeningSettings`/speech rate from the observe use cases so the settings panel changes the live loop. Today the VM's `currentState.settings` does this.
- [ ] `ListeningViewModel` keeps Setup, Loading, NeedsVoices and Empty. It maps `Playing`/`Finished` from `controller.playback` into `ListeningScreenState.Active`/`Finished`. `onCleared` no longer stops playback. `abandon()` (closing the overlay) still stops it.
- [ ] Koin: `single { ListeningPlaybackController(...) }` in `listeningModule()`, plus an `ApplicationScope` qualifier if one doesn't exist yet.
- [ ] Tests (TDD, commonTest): move the existing play-loop assertions from `ListeningViewModelTest` to a new `ListeningPlaybackControllerTest` (FakeTtsRepository, `runTest`, virtual time). Add cases for repeat, close, and the controller outliving the VM. Keep `ListeningViewModelTest` green for setup/voices.

### Phase 2: Platform port + bridge
- [ ] Add `IListeningMediaSession`, `NowPlayingInfo`, `MediaSessionPhase` and `MediaCommand` to `platforms/commonMain/kotlin/media/`. Add a factory `expect`/`actual` like `NotificationManagerFactory`. The Web actual is a no-op.
- [ ] `ListeningMediaBridge` (feature/study): collect `controller.playback` → `distinctUntilChanged()` mapped phase → `session.render()`. Collect `session.commands` → controller. `Idle` → `release()`. Started once from the controller's scope, not from UI.
- [ ] `ListeningPlayback.toMediaPhase(order)` pure mapper. Title = prompt, subtitle = answer only once `session.isAnswerRevealed` (same as the screen; no spoiler before the answer is spoken).
- [ ] Tests: mapper tests (prompt-only, revealed, translation-first order, finished) and bridge tests with `FakeListeningMediaSession` (state → render calls; Repeat/Close/Next commands → controller state).

### Phase 3: Android
- [ ] Dependency: `androidx.media:media` (`MediaSessionCompat` + `NotificationCompat.MediaStyle`) in `libs.versions.toml`. **Why not media3:** media3 needs a `Player` facade over our TTS sequencer, and its auto-notification makes the custom "ended → dismissable with Repeat/Close" state hard to control. Revisit if we ever move to ExoPlayer.
- [ ] Manifest: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and `WAKE_LOCK` permissions. `<service android:name="media.ListeningPlaybackService" android:exported="false" android:foregroundServiceType="mediaPlayback"/>`.
- [ ] `ListeningPlaybackService` (platforms/androidMain):
  - Started by `AndroidListeningMediaSession.render(Playing)` via `ContextCompat.startForegroundService` on the first render, which happens while the user is tapping Play, so it's in the foreground. `startForeground(id, notif, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)`.
  - Channel `listening_playback`, `IMPORTANCE_LOW` (silent, no heads-up). `setOngoing(true)`, `setOnlyAlertOnce(true)`, `VISIBILITY_PUBLIC`.
  - `MediaSessionCompat` metadata (title/subtitle/"3 of 20") + `PlaybackStateCompat` actions → lock screen, Bluetooth, wired headset buttons, and the media carousel in quick settings. The session callback emits into `commands`.
  - Notification actions: Previous, Play/Pause, Next, Stop. Content intent → `openAppPendingIntent(type = "listening")` to reopen the overlay.
  - Audio focus (`AudioFocusRequest`, `AUDIOFOCUS_GAIN`): loss/transient loss → Pause, gain after transient → resume only if we paused it. Receiver for `ACTION_AUDIO_BECOMING_NOISY` → Pause.
  - Partial `WakeLock` held only while `isPlaying`, so synthesis and gaps run with the screen off. Released on pause/finish.
  - **Finished:** `stopForeground(STOP_FOREGROUND_DETACH)`, then post a non-ongoing notification "Session complete · 18/20 words" with actions **Repeat** and **Close**, plus a `deleteIntent` (swipe) → Close. **Repeat** re-enters the foreground and starts playback. That's allowed because it comes from a notification action PendingIntent, which is an FGS-start exemption.
  - Close/Idle → release session, cancel notification, `stopSelf()`.
- [ ] MainActivity: route the `type == "listening"` tap extra to open the listening overlay (StudyScreen shows it when `controller.playback != Idle`).
- [ ] Wire `POST_NOTIFICATIONS`. If denied, playback still runs (the FGS is legal without a visible notification) but no card shows. Don't block, and don't add a new permission prompt.

### Phase 4: iOS
- [ ] `Info.plist`: add `audio` to `UIBackgroundModes`.
- [ ] Audio session ownership moves to `IosListeningMediaSession`: `setCategory(Playback, mode = SpokenAudio)`, `setActive(true)` once at session start, `setActive(false, NotifyOthersOnDeactivation)` on release. Remove the per-utterance `setCategory`/`setActive` from `IosTtsEngine.playWavFile`. For one-off word taps outside listening, activate lazily through a small shared `IosAudioSessionManager` with a ref count, so the two don't fight.
- [ ] **Keep-alive:** while the session is `Playing && isPlaying`, run a looping silent buffer (`AVAudioEngine` + `AVAudioPlayerNode` with a zero-filled buffer, volume 0) so pause/gap/synthesis time counts as active output and iOS doesn't suspend the app. Stop it on pause and finish. Spike first (see Risks).
- [ ] `MPNowPlayingInfoCenter`: title = prompt, artist = answer (when revealed), album = "Lexicon · Listening", `MPNowPlayingInfoPropertyPlaybackQueueIndex/Count` = position/total, `PlaybackRate` 1/0. Updated on every render.
- [ ] `MPRemoteCommandCenter`: play, pause, togglePlayPause, nextTrack, previousTrack → `commands`. Disable seek/skip-interval commands.
- [ ] Observers: `AVAudioSessionInterruptionNotification` (began → Pause; ended + `shouldResume` → Play). `AVAudioSessionRouteChangeNotification` with `OldDeviceUnavailable` (headphones out) → Pause.
- [ ] **Finished:**
  - Now Playing stays with title "Session complete", subtitle "18 of 20 words", rate 0. Play = **Repeat** (remote commands reach the Now Playing app while backgrounded).
  - If the app is in the background, also post a local notification with category `LISTENING_FINISHED` and actions **Repeat** (`.foreground`, which relaunches into the overlay and repeats) and **Close** (destructive, background). It's dismissable by nature.
  - Close or swipe-dismiss → `release()`: clear `nowPlayingInfo`, disable the remote commands, deactivate the session.
- [ ] `iOSApp.swift`: add a `listeningFinished` case to `NotificationCategory` and route its actions to Kotlin (`ListeningNotificationActions.handle(actionId)` exported from ComposeApp), following the existing review-reminder routing.

### Phase 5: In-app experience + polish
- [ ] Reopening the app mid-session (or tapping the notification) shows the Active player at the live position. The VM maps controller state on init. Skip the phase-enter animation when landing on a live session.
- [ ] **Mini player** (`ListeningMiniPlayer` in feature/study, reusing design-system tokens; check existing components first): shown in `StudyScreen`'s scaffold above the bottom bar whenever `controller.playback` is Playing/paused and the overlay is closed. Tap → reopen overlay. Uses the same controller commands. Fade in/out with `Theme.motion`.
- [ ] Overlay close logic: playing → collapse to mini player; otherwise → close as today.
- [ ] Previous 2-second rule in `ListeningReducer` (pure, takes `elapsedInWordMs`), plus tests.
- [ ] End chime asset in `resources`, played via the TTS engine output path (so it shares audio session/focus).
- [ ] Notification permission rationale row in setup + settings row when denied.
- [ ] Global TTS hook: word-tap speakers call `controller.pauseForExternalSpeech()` before speaking.
- [ ] Branded artwork asset (Android drawable + iOS bundle image) for media session/Now Playing.
- [ ] Strings in `resources/.../strings.xml`: channel name, "Session complete", "%1$d of %2$d words", Repeat, Close. Feed them into the platform session as resolved strings from the bridge, so `platforms` needs no Compose resources.
- [ ] Analytics: `listening_session_repeat {source: notification|lock_screen|app}` and `listening_background_minutes` (time played while the app was backgrounded) to measure adoption.
- [ ] Update `doc/tts-system.md` with the session ownership and background rules.

### Phase 6: Verification
- [ ] `./gradlew composeApp:cleanAllTests composeApp:allTests` green. `detekt` clean. `compileKotlinMetadata`, `assembleDebug` and `linkDebugFrameworkIosSimulatorArm64` pass.
- [ ] Android device (API 34+ and API 26): start 20 words → home → lock the screen for 5+ min, and audio continues with the word index advancing. Notification updates live. Lock-screen and BT headset buttons work. Phone call pauses. Unplugging headphones pauses. End → notification is swipeable, Repeat replays, Close clears. Swipe the app from recents mid-session and playback continues.
- [ ] iOS physical device (the simulator doesn't faithfully suspend apps): the same matrix via the lock screen, Control Center and Dynamic Island. Leave it locked for 5+ min including long gap settings (pause 3 s, gap 3 s, repeat 3).
- [ ] UX walkthrough on both platforms against every row of the **UX Behavior Spec** tables (checklist in PR description): call auto-resume, BT disconnect, previous 2 s rule, mini player collapse/expand, finished-while-backgrounded summary, Repeat from lock screen, permission denied path, RTL pair (fa↔en).
- [ ] Run the `ux-reviewer` agent on the overlay + mini player.
- [ ] Regression: single-word TTS taps in the review/words screens still play on both platforms with no audio session conflict.

## Open Questions
- **Repeat semantics:** replay the *same* words (works offline and from the background, no I/O), or rebuild the queue like the in-app "Listen again"? The plan assumes same words from the notification and keeps the in-app button as it is.
- Show the translation in the notification before it's spoken, or only after the reveal like the screen? The plan assumes after the reveal.
- **Overlay X while playing:** the plan now collapses to a mini player (podcast-style) instead of stopping. If that's too much scope for v1, fallback: X stops playback, only backgrounding keeps it alive.
- **End chime:** on by default; add a toggle in listening settings, or no toggle (YAGNI)? The plan assumes no toggle.

## Risks
- **iOS suspension during silence:** the core risk. Mitigation: the keep-alive silent output in Phase 4. Spike it on a real device **before** Phase 4 proper (lock for 10 min, gap 3 s). Fallback: render pause/gap as real silent PCM appended to each utterance buffer, so output never stops.
- **App Review (iOS):** the `audio` background mode needs genuine audio. We have that, and the silence is part of the content's pacing. Note it in the review notes.
- **Android 14+ dismissible FGS notifications:** MediaStyle notifications with an active media session are expected to stay non-dismissable while playing. Verify on device. If one is swiped, the service keeps running and the next render re-posts it.
- **Sherpa synthesis in the background:** CPU-heavy. The WakeLock (Android) and active audio (iOS) keep it running. Watch for thermal/battery issues on long sessions. The existing 2-engine LRU pool avoids reload churn.
- **Shared TTS engine contention:** another screen speaking while listening plays in the background. The controller is the only sequencer, and `TtsRepositoryImpl`'s `stop()` cancels in-flight work. Make sure word-tap TTS elsewhere pauses listening (a `controller.pause()` hook) rather than interleaving.
- **Refactor regression:** Phase 1 moves the loop. Mitigate with test-first migration of the existing VM tests before any platform work, and ship Phase 1 as its own commit.
