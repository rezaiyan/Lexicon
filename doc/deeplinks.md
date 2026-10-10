# Deep Links

Lexicon has **no URI deep links** (no `lexicon://` scheme, no Android App Links, no iOS Universal Links). External entry points are notification taps, the home-screen widget, and the OAuth callback.

## Entry Points

| Source | Platform | Lands on |
|--------|----------|----------|
| Notification tap (`type` extra / `userInfo`) | Android, iOS | Screen per `type` (below) |
| Daily Word widget | Android | App launch (`actionStartActivity<MainActivity>()`), no routing |
| Google Sign-In callback (`$(GID_URL_SCHEME)`) | iOS | `GIDSignIn.handle(url)` only, not app navigation |
| `CI_INJECT_TOKENS` broadcast | Android **debug only** | `CiTokenReceiver`, injects tokens for CI |

## Notification Routing

Push/local payload keys: `type`, `notification_log_id`.

| `type` | Action |
|--------|--------|
| `review_reminder` | Switch to Study tab, start due-cards review (skipped if a review is already active) |
| `billing_issue` | Open `SubscriptionRoute` |
| `subscription_updated` | Silent push, no navigation (refetch subscription) |
| anything else | App opens on last screen; open still reported |

## Flow

```
Android: MainActivity.onCreate / onNewIntent → reads extras → removes them (no replay on config change)
iOS:     AppDelegate didReceive response → MainViewControllerKt.notifyNotificationTapped(logId, type)
              ↓
NotificationTapHandler.onNotificationTapped(data)   composeApp/.../notification/NotificationTapHandler.kt
  ├─ NotificationNavigator.open(...) / openDueReview()
  └─ ReportNotificationOpenedUseCase (fire-and-forget)
              ↓
NotificationNavigator   presentation/.../navigation/NotificationNavigator.kt
  ├─ destinations    → NavigationGraph (OnEvents) → navigate
  └─ reviewRequests  → StudyScreen (OnEvents)     → openReviewScreen(DueCards)
```

Channels are `CONFLATED`: a cold-start tap is buffered until the UI collects it; only the latest tap wins.

## Adding a New Destination

1. Backend: send new `type` string in push data.
2. Add constant to `PushTypes`.
3. Add value to `NotificationDestination` (or a dedicated request channel if the target screen must act, like `reviewRequests`).
4. Map it in `NotificationTapHandler` and handle it in `NavigationGraph`.
5. Test in `NotificationTapHandlerTest`.

## Adding URI Deep Links (not implemented)

Would need: Android `<intent-filter>` with `VIEW` + `BROWSABLE` + `autoVerify` and `assetlinks.json`; iOS Associated Domains entitlement + `apple-app-site-association`; parse URL into the same `NotificationNavigator`-style channel. Validate all URL params before use (see `.claude/rules/lexicon-android-security.md`).
