# UI Components

Components in `presentation/src/commonMain/kotlin/presentation/ui/`.

## Layout Components

| Component | File | Purpose |
|-----------|------|---------|
| `LexiconColumn` | `components/LexiconScaffold.kt` | Standard screen layout with top bar, nav icon, action icons |
| `FlashCard` | `components/FlashCard.kt` | Animated front/back flip card for vocabulary |
| `LevelBucketCard` | `components/LevelBucketCard.kt` | Display learning stage card with count |
| `SettingsCard` | `components/SettingsCard.kt` | Generic settings option card |
| `BasicAlertDialog` | `components/BasicAlertDialog.kt` | Standard alert dialog |
| `AnimatedNavIcon` | `components/AnimatedNavIcon.kt` | Bottom nav icon with animation |

## Import Components

| Component | File | Purpose |
|-----------|------|---------|
| `ImportBottomSheet` | `components/imports/ImportBottomSheet.kt` | Multi-tab import (Text/File/Image) |
| `AiWordImportBottomSheet` | `components/imports/AiWordImportBottomSheet.kt` | AI vocabulary generation wizard |
| `ImportMethodSelectorContent` | `components/imports/ImportMethodSelectorContent.kt` | Choose manual or AI import |
| `ImportTabV2.Text` | `components/imports/ImportTabV2.kt` | Manual text input tab |
| `ImportTabV2.File` | `components/imports/ImportTabV2.kt` | File upload tab |
| `ImportTabV2.Image` | `components/imports/ImportTabV2.kt` | Image OCR tab (premium) |

## Profile Components

| Component | File |
|-----------|------|
| `UserInfoSection` | `components/profile/UserInfoSection.kt` |
| `StreakSection` | `components/profile/StreakSection.kt` |
| `LogoutDialogContent` | `components/profile/LogoutDialogContent.kt` |
| `DeleteAccountDialogContent` | `components/profile/DeleteAccountDialogContent.kt` |

## Settings Components

| Component | File |
|-----------|------|
| `LanguageSettingsCard` | `components/settings/LanguageSettingsCard.kt` |
| `ThemeSettingsCard` | `components/settings/ThemeSettingsCard.kt` |
| `NotificationSettingsCard` | `components/settings/NotificationSettingsCard.kt` |
| `SubscriptionCard` | `components/settings/SubscriptionCard.kt` |
| `AboutSettingsCard` | `components/settings/AboutSettingsCard.kt` |
| `WordManagerCard` | `components/settings/WordManagerCard.kt` |

## Review Components

| Component | File |
|-----------|------|
| `ReviewBottomSheetContent` | `screens/review/ReviewBottomSheetContent.kt` |
| `ReviewBottomSheet` | `screens/review/ReviewBottomSheet.kt` |
| `ReviewComponents` | `screens/review/ReviewComponents.kt` |
| `DeckStackingAnimation` | `screens/review/DeckStackingAnimation.kt` |
| `DeleteWordConfirmationDialog` | `screens/review/DeleteWordConfirmationDialog.kt` |
| `EditWordDialog` | `screens/review/EditWordDialog.kt` |

## Study Components

| Component | File |
|-----------|------|
| `StatsSection` | `screens/study/StatsSection.kt` |
| `LearningStagesSection` | `screens/study/LearningStagesSection.kt` |
| `ProgressComponents` | `screens/study/ProgressComponents.kt` |
| `ReviewActionSection` | `screens/study/ReviewActionSection.kt` |
| `StudyAnimations` | `screens/study/StudyAnimations.kt` |

## Subscription Components

| Component | File |
|-----------|------|
| `SubscriptionActiveContent` | `screens/subscription/SubscriptionActiveContent.kt` |
| `SubscriptionNotSubscribedContent` | `screens/subscription/SubscriptionNotSubscribedContent.kt` |
| `SubscriptionLoadingContent` | `screens/subscription/SubscriptionLoadingContent.kt` |
| `SubscriptionErrorContent` | `screens/subscription/SubscriptionErrorContent.kt` |
| `PlanCard` | `screens/subscription/PlanCard.kt` |
| `PremiumFeaturesGrid` | `screens/subscription/PremiumFeaturesGrid.kt` |
| `PremiumHeroSection` | `screens/subscription/PremiumHeroSection.kt` |
| `ComparisonTable` | `screens/subscription/ComparisonTable.kt` |

## Dialogs

| Dialog | File |
|--------|------|
| `LanguageSelectionDialog` | `components/LanguageSelectionDialog.kt` |
| `ThemeModeDialog` | `components/ThemeModeDialog.kt` |
| `NotificationPermissionDialog` | `components/NotificationDialogs.kt` |
| `NotificationSettingsDialog` | `components/NotificationDialogs.kt` |
| `CloseConfirmationDialog` | `components/CloseConfirmationDialog.kt` |
| `LexiconDialogContent` | `components/LexiconDialogContent.kt` |

## Auth Components

| Component | File | Notes |
|-----------|------|-------|
| `GoogleSignInContainer` | `components/GoogleSignInContainer.kt` | Platform-specific (expect/actual) |
| `AppleSignInButton` | `components/AppleSignInButton.kt` | Platform-specific (expect/actual) |

## Overlay System (`ui/overlay/`)

### OverlayHost
```kotlin
interface OverlayHost {
    fun show(overlay: Overlay, destination: NavDestination, tag: String?)
    fun dismiss(tag: String)
    fun dismissAll()
}
// Access via CompositionLocal: LocalOverlayHost
```

### Dialog Overlay (`overlay/dialog/DialogOverlay.kt`)
```kotlin
overlayHost.showDialog(tag = "myDialog") { nav ->
    MyDialogContent(
        onConfirm = { nav.dismiss(); doAction() },
        onDismiss = { nav.dismiss() }
    )
}
```

### Bottom Sheet Overlay (`overlay/bottomsheet/BottomSheetOverlay.kt`)
Two variants:
- `showFullscreenBottomSheet(tag, properties, content)` - Full screen
- `showSizeToFitBottomSheet(tag, content)` - Fit to content

Properties: `dismissOnTouchOutside`, `dismissOnBackPress`, `isNavigationBarsPaddingEnabled`, `sheetGesturesEnabled`

```kotlin
overlayHost.showFullscreenBottomSheet(
    tag = "import",
    properties = BottomSheetProperties(
        dismissOnTouchOutside = false,
        dismissOnBackPress = false,
        sheetGesturesEnabled = false,
    )
) { navigator ->
    ImportBottomSheet(onDismiss = { navigator.dismiss() })
}
```

## Platform-Specific Components (expect/actual)
- `AppleSignInButton` (android, ios, wasmJs)
- `GoogleSignInContainer` (mobile, wasmJs)
- `NavigationSuiteHelper` (android, ios, wasmJs)
- `NotificationsPermission` (android, ios, wasmJs)
- `SplashHost` (android, ios, wasmJs)
