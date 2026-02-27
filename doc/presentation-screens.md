# Presentation Screens

All screens in `presentation/src/commonMain/kotlin/presentation/ui/screens/`.

## Screen Map

```
LexiconApp (root composable)
├── SplashScreen
├── OnboardingScreen (multi-step)
├── VocabularyPreviewScreen
├── AuthGateScreen (login)
└── AppContent (bottom nav)
     ├── ProfileScreen (tab 1)
     ├── StudyScreen (tab 2, default)
     │    ├── ImportBottomSheet (manual import)
     │    ├── AiWordImportBottomSheet (AI import)
     │    └── ReviewBottomSheet (flashcard review)
     └── SettingsScreen (tab 3)
          ├── WordManagerScreen (sub-screen)
          └── SubscriptionScreen (sub-screen)
```

## LexiconApp (`ui/LexiconApp.kt`)
- Main composable: theme setup, auth flow, overlay management
- `AppContent()`: Bottom nav with 3 tabs (Profile, Study, Settings)
- `NavigationGraph()`: NavHost with animated transitions (slide + fade)
- `HandleVocabularyEffects()`: Listens for snackbar events
- CompositionLocals: `LocalSnackbarHostState`, `LocalOverlayHost`

## SplashScreen (`ui/screens/SplashScreen.kt`)
- Shows splash animation
- Routes to onboarding/auth/ready based on state

## OnboardingScreen (`ui/screens/OnboardingScreen.kt`)
- Multi-step wizard (4 steps):
  1. Select target language
  2. Select native language
  3. Select proficiency level
  4. Select interests/topics (optional)
- **ViewModel**: `OnboardingViewModel`
- **Events**: NavigateToPreview, NavigateToMain

## VocabularyPreviewScreen (`ui/screens/VocabularyPreviewScreen.kt`)
- Review suggested vocabulary before login
- Toggle word selection, accept/deny
- **ViewModel**: `VocabularyPreviewViewModel`

## AuthGateScreen (`ui/screens/AuthGateScreen.kt`)
- Google Sign-In button
- Apple Sign-In button (iOS only)
- Error display, loading per provider
- **ViewModel**: `AuthViewModel`

## StudyScreen (`ui/screens/StudyScreen.kt`)
Main learning hub. Sections:
1. **StatsSection** - Progress overview (total, mature, due)
2. **LearningStagesSection** - 7 stage cards (Level 0-6)
3. **Review Buttons** - Start due review or browse by stage
4. **Import button** - Opens method selector (Manual/AI)

**ViewModel**: `StudyViewModel`
**Child sheets**: ImportBottomSheet, AiWordImportBottomSheet, ReviewBottomSheet

## ProfileScreen (`ui/screens/ProfileScreen.kt`)
- UserInfoSection (name, email, avatar)
- StreakSection (current streak)
- Logout button
- More menu: delete account (two-step confirmation)
- **ViewModel**: `ProfileViewModel`

## SettingsScreen (`ui/screens/SettingsScreen.kt`)
Setting cards:
1. LanguageSettingsCard - Select target language
2. ThemeSettingsCard - Auto/Light/Dark
3. NotificationSettingsCard - Enable/disable
4. WordManagerCard - Navigate to word manager
5. SubscriptionCard - View subscription
6. AboutSettingsCard - App version

Dialogs: Language selection, Theme mode, Notification permission, Notification settings
**ViewModel**: `SettingsViewModel`

## WordManagerScreen (`ui/screens/settings/WordManagerScreen.kt`)
- Search words
- Multi-select with checkboxes
- Edit individual words (dialog)
- Delete selected (with confirmation)
- Share words as text file
- **ViewModel**: `WordManagerViewModel`

## SubscriptionScreen (`ui/screens/SubscriptionScreen.kt`)
States: Loading, Error (retry), Subscribed (manage/cancel), Not Subscribed (purchase/restore)
Components: PlanCard, PremiumFeaturesGrid, PremiumHeroSection, ComparisonTable
**ViewModel**: `SubscriptionViewModel`

## Review System (`ui/screens/review/`)
- `ReviewBottomSheetContent` - Pure UI wrapper
- `ReviewBottomSheet` - Full review with card deck
- Two modes: REVIEW (active learning) / BROWSE (passive)
- Flashcard with front/back flip animation
- TTS integration
- Edit/Delete word options
- Progress tracking
- `DeckStackingAnimation` - Card flip/stacking animation
