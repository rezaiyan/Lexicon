# Navigation

Navigation in `presentation/src/commonMain/kotlin/presentation/`.

## App Flow

```
SplashScreen
    │
    ├── (not onboarded) → OnboardingScreen
    │                        │
    │                        ├── (submit) → VocabularyPreviewScreen
    │                        │                │
    │                        │                └── AuthGateScreen → Ready
    │                        │
    │                        └── (skip) → AuthGateScreen → Ready
    │
    ├── (not authenticated) → AuthGateScreen → Ready
    │
    └── (authenticated) → Ready
```

## AppUiState (Navigation State Machine)
```kotlin
sealed interface AppUiState {
    data object Splash : AppUiState
    data object Onboarding : AppUiState
    data class VocabularyPreview(val words: List<SuggestedVocabulary>) : AppUiState
    data class AuthGate(val pendingVocabulary: List<SuggestedVocabulary>) : AppUiState
    data object Ready : AppUiState
}
```

Managed by `AppNavigationViewModel`.

## Bottom Navigation (Ready state)

```kotlin
sealed interface TabDestination {
    data object Profile : TabDestination      // Tab 1
    data object Study : TabDestination        // Tab 2 (default)
    data object Settings : TabDestination     // Tab 3
    data object WordManager : TabDestination  // Sub-screen of Settings
    data object Subscription : TabDestination // Sub-screen of Settings
}

enum class LexiconRoute(val route: String) {
    Profile("Profile"),
    Study("Study"),
    Settings("Settings")
}
```

3 bottom tabs with animated transitions (slide + fade).

## NavigationGraph (`ui/LexiconApp.kt`)
Uses `androidx.navigation.compose` NavHost.

**Routes**: Profile, Study, Settings, WordManager (nested), Subscription (nested)

**Transitions**: AnimatedContentTransitionScope with:
- Enter: slideIntoContainer + fadeIn
- Exit: slideOutOfContainer + fadeOut

## Overlay Navigation

Dialogs and bottom sheets are NOT part of the nav graph. They use the OverlayHost system:

```kotlin
// Show dialog
overlayHost.showDialog(tag = "logout") { nav ->
    LogoutDialogContent(onConfirm = { nav.dismiss() }, onDismiss = { nav.dismiss() })
}

// Show full-screen bottom sheet
overlayHost.showFullscreenBottomSheet(tag = "import", properties = ...) { nav ->
    ImportBottomSheet(onDismiss = { nav.dismiss() })
}
```

This means:
- No nav graph routes for dialogs/sheets
- Managed via CompositionLocal `LocalOverlayHost`
- Tags used for identification and dismissal
- `dismissAll()` available for clearing all overlays

## Deep Link / Notification Navigation
Notifications open `MainActivity` with data in extras. No deep link URI scheme defined in the nav graph.
