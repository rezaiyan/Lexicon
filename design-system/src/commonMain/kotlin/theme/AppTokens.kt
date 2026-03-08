package theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// region Spacing

@Immutable
data class AppSpacing(
    val none: Dp = 0.dp,

    val xxxs: Dp = 2.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 40.dp,
    val xxxl: Dp = 48.dp,

    // Legacy names — kept for backward compatibility
    @Deprecated("Use xxxs", ReplaceWith("xxxs"))
    val extraSmall4: Dp = 2.dp,
    @Deprecated("Use xxs", ReplaceWith("xxs"))
    val extraSmall3: Dp = 4.dp,
    @Deprecated("Use xs", ReplaceWith("xs"))
    val extraSmall2: Dp = 8.dp,
    @Deprecated("Use sm", ReplaceWith("sm"))
    val extraSmall: Dp = 12.dp,
    @Deprecated("Use md", ReplaceWith("md"))
    val small: Dp = 16.dp,
    @Deprecated("Use lg", ReplaceWith("lg"))
    val medium: Dp = 24.dp,
    @Deprecated("Use xl", ReplaceWith("xl"))
    val large: Dp = 32.dp,
    @Deprecated("Use xxl", ReplaceWith("xxl"))
    val extraLarge: Dp = 40.dp,
    @Deprecated("Use xxxl", ReplaceWith("xxxl"))
    val extraLarge2: Dp = 48.dp,
    @Deprecated("Use spacing values directly")
    val extraLarge3: Dp = 56.dp,
    @Deprecated("Use spacing values directly")
    val extraLarge4: Dp = 64.dp,
    @Deprecated("Use spacing values directly")
    val extraLarge5: Dp = 72.dp,

    @Deprecated("Use md", ReplaceWith("md"))
    val cardPadding: Dp = 16.dp,
    @Deprecated("Use sm", ReplaceWith("sm"))
    val cardSpacing: Dp = 12.dp,
    @Deprecated("Use md", ReplaceWith("md"))
    val cardSpacingLarge: Dp = 16.dp,
    @Deprecated("Use lg", ReplaceWith("lg"))
    val sectionSpacing: Dp = 24.dp,
)

// endregion

// region Dimensions

@Immutable
data class AppDimensions(
    // Icon sizes (ascending)
    val iconSizeSmall: Dp = 16.dp,
    val iconSizeMedium: Dp = 20.dp,
    val iconSize: Dp = 24.dp,
    val iconSizeLarge: Dp = 28.dp,
    val iconSizeXLarge: Dp = 32.dp,
    val iconSizeHuge: Dp = 48.dp,
    val iconSizeMassive: Dp = 60.dp,

    // Corner radii
    val cardCornerRadius: Dp = 12.dp,

    // Component heights
    val buttonHeight: Dp = 56.dp,
    val buttonHeightSmall: Dp = 40.dp,
    val inputFieldHeight: Dp = 56.dp,
    val bottomBarHeight: Dp = 80.dp,
    val topBarHeight: Dp = 64.dp,

    // Touch targets (accessibility)
    val touchTarget: Dp = 48.dp,
    val touchTargetSmall: Dp = 40.dp,

    // Content constraints
    val contentMaxWidth: Dp = 500.dp,
    val dialogMinWidth: Dp = 280.dp,
    val dialogMaxWidth: Dp = 480.dp,

    // Dividers & borders (ascending)
    val hairlineThickness: Dp = 0.5.dp,
    val dividerThickness: Dp = 1.dp,
    val borderWidth: Dp = 1.dp,
    val borderWidthThick: Dp = 4.dp,

    // Specific components
    val profilePictureSize: Dp = 120.dp,
    val progressBarHeight: Dp = 6.dp,
)

// endregion

// region Shapes

@Immutable
data class AppShapes(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val pill: Dp = 100.dp,
)

// endregion

// region Elevation

@Immutable
data class AppElevation(
    val none: Dp = 0.dp,
    val low: Dp = 1.dp,
    val medium: Dp = 2.dp,
    val high: Dp = 4.dp,
    val extraHigh: Dp = 6.dp,
    val overlay: Dp = 8.dp,
    val modal: Dp = 12.dp,
)

// endregion

// region Motion

@Immutable
data class AppMotion(
    // Durations (ms) — ascending scale
    val durationXShort: Int = 100,
    val durationShort: Int = 150,
    val durationShort2: Int = 200,
    val durationMedium: Int = 300,
    val durationMedium2: Int = 400,
    val durationLong: Int = 500,
    val durationXLong: Int = 800,
    val durationXXLong: Int = 1200,

    // Easing curves
    val easingStandard: Easing = FastOutSlowInEasing,
    val easingDecelerate: Easing = LinearOutSlowInEasing,
    val easingAccelerate: Easing = FastOutLinearInEasing,
    val easingLinear: Easing = LinearEasing,
    val easingEmphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
)

// endregion

// region Opacity

@Immutable
data class AppOpacity(
    // Interaction state layers (M3 spec)
    val hover: Float = 0.08f,
    val focus: Float = 0.12f,
    val pressed: Float = 0.12f,
    val dragged: Float = 0.16f,

    // Content emphasis (ascending)
    val disabled: Float = 0.38f,
    val hint: Float = 0.60f,
    val muted: Float = 0.80f,

    // Surface overlays (ascending)
    val dimming: Float = 0.32f,
    val overlay: Float = 0.50f,
)

// endregion

// region Breakpoints

@Immutable
data class AppBreakpoints(
    val compact: Dp = 0.dp,
    val medium: Dp = 600.dp,
    val expanded: Dp = 840.dp,
    val large: Dp = 1200.dp,
)

// endregion

// region Gradients

// Color constants shared between color schemes and gradients
internal val LightBackground = Color(0xFFF8F6F7)
internal val LightSurface = Color(0xFFFFFFFF)
internal val DarkBackground = Color(0xFF16161A)
internal val DarkSurface = Color(0xFF1E1E1E)

@Immutable
data class AppGradients(
    val premiumHero: Brush,
    val primaryWash: Brush,
    val surfaceFade: Brush,
)

private val PremiumHeroBrush = Brush.linearGradient(
    colors = listOf(
        AppColors.subscriptionRecommended,
        AppColors.subscriptionPremiumAccent,
    ),
)

internal val LightGradients = AppGradients(
    premiumHero = PremiumHeroBrush,
    primaryWash = Brush.linearGradient(
        colors = listOf(
            AppColors.primary.copy(alpha = 0.08f),
            AppColors.tertiary.copy(alpha = 0.05f),
        ),
    ),
    surfaceFade = Brush.verticalGradient(
        colors = listOf(
            LightBackground,
            LightSurface,
        ),
    ),
)

internal val DarkGradients = AppGradients(
    premiumHero = PremiumHeroBrush,
    primaryWash = Brush.linearGradient(
        colors = listOf(
            AppColors.primary.copy(alpha = 0.12f),
            AppColors.tertiary.copy(alpha = 0.08f),
        ),
    ),
    surfaceFade = Brush.verticalGradient(
        colors = listOf(
            DarkBackground,
            DarkSurface,
        ),
    ),
)

// endregion
