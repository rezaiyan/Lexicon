# Design System

Design system in `design-system/src/commonMain/kotlin/theme/`.

## Theme (`AppTheme.kt`)

### LexiconTheme Composable
```kotlin
@Composable
fun LexiconTheme(
    isDarkTheme: Boolean,
    content: @Composable () -> Unit
)
```
Provides MaterialTheme with custom colors, typography, and CompositionLocal providers.

### Color Schemes
- **LightColorScheme**: Full Material 3 light color scheme
- **DarkColorScheme**: Full Material 3 dark color scheme

### AppColors
```kotlin
object AppColors {
    // Backgrounds
    val lightBackground, darkBackground, cardLight, cardDark

    // Skill levels (for SRS stages)
    val novice, apprentice, adept, master

    // Subscription tiers
    val freeTier, premiumTier
}
```

### AppSpacing
```kotlin
object AppSpacing {
    val none = 0.dp
    val extraSmall4 = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge32 = 32.dp
    val extraLarge48 = 48.dp
    val extraLarge56 = 56.dp
    val extraLarge72 = 72.dp
}
```

### AppDimensions
```kotlin
object AppDimensions {
    val iconSmall = 20.dp
    val iconMedium = 24.dp
    val iconLarge = 32.dp
    val iconExtraLarge = 48.dp
    val iconHuge = 60.dp
    val cardCornerRadius = 20.dp
    val profilePictureSize = 120.dp
    val progressBarHeight = 6.dp
}
```

### Typography
Material 3 typography with 13 text styles: display (large/medium/small), headline, title (large/medium/small), body (large/medium/small), label (large/medium/small).

### Access via Theme object
```kotlin
Theme.spacing.medium   // 16.dp
Theme.dimensions.iconLarge  // 32.dp
```

## Platform Fonts (`PlatformFont.kt`)
- Android/iOS: Returns null (use system fonts)
- WasmJs: Returns FontFamily with Noto Sans (regular, medium)
