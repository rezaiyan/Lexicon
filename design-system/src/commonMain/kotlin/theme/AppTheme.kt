package theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// region Brand Colors (theme-independent)

object AppColors {
    // Brand palette — fixed across light/dark themes
    val primary = Color(0xFF7F5AF0)
    val secondary = Color(0xFF2CB67D)
    val tertiary = Color(0xFFFF8906)
    val error = Color(0xFFE53170)

    // Level badge colors
    val novice = Color(0xFF7F5AF0)
    val apprentice = Color(0xFF2CB67D)
    val adept = Color(0xFFFF8906)
    val master = Color(0xFFE53170)

    // Subscription colors
    val subscriptionRecommended = Color(0xFF9F7AEA)
    val subscriptionPremiumAccent = Color(0xFF7C3AED)
    val subscriptionStandard = Color(0xFF10B981)

    @Deprecated(
        "Use MaterialTheme.colorScheme.background",
        ReplaceWith(
            "MaterialTheme.colorScheme.background",
            "androidx.compose.material3.MaterialTheme",
        ),
    )
    val normalBackground = Color(0xFFFAF9F6)

    @Deprecated(
        "Use MaterialTheme.colorScheme.surface",
        ReplaceWith(
            "MaterialTheme.colorScheme.surface",
            "androidx.compose.material3.MaterialTheme",
        ),
    )
    val normalSurface = Color(0xFFFFFFFF)
}

// endregion

// region Semantic Colors (theme-aware)

@Immutable
data class AppSemanticColors(
    // Status — success
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,

    // Status — warning
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,

    // Status — info
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,

    // Surface layers (consistent regardless of M3 version)
    val surfaceDim: Color,
    val surfaceBright: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerLow: Color,

    // Scrim overlay color
    val scrim: Color,

    // Backgrounds
    val background: Color,

    // Card backgrounds
    val settingsCardBackground: Color,
)

private val LightSemanticColors = AppSemanticColors(
    success = Color(0xFF2CB67D),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFD4F5E4),
    onSuccessContainer = Color(0xFF0A3D24),

    warning = Color(0xFFFF8906),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFF3E0),
    onWarningContainer = Color(0xFF3D2000),

    info = Color(0xFF3B82F6),
    onInfo = Color(0xFFFFFFFF),
    infoContainer = Color(0xFFDBEAFE),
    onInfoContainer = Color(0xFF0C2D6B),

    surfaceDim = Color(0xFFF0EFEC),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF5F4F1),
    surfaceContainerHigh = Color(0xFFEDECE9),
    surfaceContainerLow = Color(0xFFFAF9F6),

    scrim = Color(0xFF000000),

    background = Color(0xFFF8F6F7),
    settingsCardBackground = Color(0xFFFFFFFF),
)

private val DarkSemanticColors = AppSemanticColors(
    success = Color(0xFF4ADE80),
    onSuccess = Color(0xFF0A3D24),
    successContainer = Color(0xFF1A5C42),
    onSuccessContainer = Color(0xFFD4F5E4),

    warning = Color(0xFFFBBF24),
    onWarning = Color(0xFF3D2000),
    warningContainer = Color(0xFF663604),
    onWarningContainer = Color(0xFFFFF3E0),

    info = Color(0xFF60A5FA),
    onInfo = Color(0xFF0C2D6B),
    infoContainer = Color(0xFF1E3A5F),
    onInfoContainer = Color(0xFFDBEAFE),

    surfaceDim = Color(0xFF0E0E0E),
    surfaceBright = Color(0xFF383838),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF2C2C2C),
    surfaceContainerLow = Color(0xFF151515),

    scrim = Color(0xFF000000),

    background = Color(0xFF221019),
    settingsCardBackground = Color(0xFF2D1A23),
)

// endregion

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
private val LightBackground = Color(0xFFF8F6F7)
private val LightSurface = Color(0xFFFFFFFF)
private val DarkBackground = Color(0xFF221019)
private val DarkSurface = Color(0xFF1E1E1E)

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

private val LightGradients = AppGradients(
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

private val DarkGradients = AppGradients(
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

// region CompositionLocals

val LocalSpacing = staticCompositionLocalOf { AppSpacing() }
val LocalDimensions = staticCompositionLocalOf { AppDimensions() }
val LocalShapes = staticCompositionLocalOf { AppShapes() }
val LocalElevation = staticCompositionLocalOf { AppElevation() }
val LocalMotion = staticCompositionLocalOf { AppMotion() }
val LocalOpacity = staticCompositionLocalOf { AppOpacity() }
val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }
val LocalBreakpoints = staticCompositionLocalOf { AppBreakpoints() }
val LocalGradients = staticCompositionLocalOf { LightGradients }

// endregion

// region Color Schemes

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7F5AF0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3DDC84),
    onPrimaryContainer = Color(0xFF1E1E1E),

    secondary = Color(0xFF2CB67D),
    onSecondary = Color(0xFF1E1E1E),
    secondaryContainer = Color(0xFF9EF7D3),
    onSecondaryContainer = Color(0xFF1E1E1E),

    tertiary = Color(0xFFFF8906),
    onTertiary = Color(0xFF1E1E1E),
    tertiaryContainer = Color(0xFFFFCE8A),
    onTertiaryContainer = Color(0xFF1E1E1E),

    background = LightBackground,
    onBackground = Color(0xFF1E1E1E),

    surface = LightSurface,
    onSurface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFFF3F2EF),
    onSurfaceVariant = Color(0xFF6B7280),

    error = Color(0xFFE53170),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFD9E3),
    onErrorContainer = Color(0xFF3E0019),

    outline = Color(0xFFD4D2CD),
    outlineVariant = Color(0xFFE8E6E1),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF16161A),
    primaryContainer = Color(0xFF3DDC84),
    onPrimaryContainer = Color(0xFF16161A),

    secondary = Color(0xFF2CB67D),
    onSecondary = Color(0xFF16161A),
    secondaryContainer = Color(0xFF1A5C42),
    onSecondaryContainer = Color(0xFF9EF7D3),

    tertiary = Color(0xFFFF8906),
    onTertiary = Color(0xFF16161A),
    tertiaryContainer = Color(0xFF663604),
    onTertiaryContainer = Color(0xFFFFCE8A),

    background = DarkBackground,
    onBackground = Color(0xFFFFFFFE),

    surface = DarkSurface,
    onSurface = Color(0xFFFFFFFE),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFCCCCCC),

    error = Color(0xFFE53170),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF93002A),
    onErrorContainer = Color(0xFFFFD9E3),

    outline = Color(0xFF3D3D3D),
    outlineVariant = Color(0xFF2E2E2E),
)

// endregion

// region Typography

private fun appTypography(fontFamily: FontFamily? = null): Typography {
    val ff = fontFamily
    return Typography(
        displayLarge = TextStyle(
            fontFamily = ff,
            fontSize = 57.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 64.sp,
            letterSpacing = (-0.5).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = ff,
            fontSize = 45.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 52.sp,
            letterSpacing = (-0.25).sp,
        ),
        displaySmall = TextStyle(
            fontFamily = ff,
            fontSize = 36.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 44.sp,
            letterSpacing = 0.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = ff,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 40.sp,
            letterSpacing = (-0.25).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = ff,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 36.sp,
            letterSpacing = 0.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = ff,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 32.sp,
            letterSpacing = 0.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = ff,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = ff,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = ff,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = ff,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 26.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = ff,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 22.sp,
            letterSpacing = 0.25.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = ff,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 18.sp,
            letterSpacing = 0.4.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = ff,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = ff,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = ff,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
    )
}

// endregion

// region Material3 Shapes

private fun appMaterialShapes(): Shapes {
    val shapes = AppShapes()
    return Shapes(
        extraSmall = RoundedCornerShape(shapes.extraSmall),
        small = RoundedCornerShape(shapes.small),
        medium = RoundedCornerShape(shapes.medium),
        large = RoundedCornerShape(shapes.large),
        extraLarge = RoundedCornerShape(shapes.extraLarge),
    )
}

// endregion

// region Theme

@Composable
fun LexiconTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors
    val gradients = if (darkTheme) DarkGradients else LightGradients
    val fontFamily = platformFontFamily()
    val typography = appTypography(fontFamily)

    CompositionLocalProvider(
        LocalSpacing provides AppSpacing(),
        LocalDimensions provides AppDimensions(),
        LocalShapes provides AppShapes(),
        LocalElevation provides AppElevation(),
        LocalMotion provides AppMotion(),
        LocalOpacity provides AppOpacity(),
        LocalSemanticColors provides semanticColors,
        LocalBreakpoints provides AppBreakpoints(),
        LocalGradients provides gradients,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = appMaterialShapes(),
            content = content,
        )
    }
}

object Theme {
    val spacing: AppSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val dimensions: AppDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalDimensions.current

    val shapes: AppShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalShapes.current

    val elevation: AppElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalElevation.current

    val motion: AppMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalMotion.current

    val opacity: AppOpacity
        @Composable
        @ReadOnlyComposable
        get() = LocalOpacity.current

    val colors: AppSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSemanticColors.current

    val breakpoints: AppBreakpoints
        @Composable
        @ReadOnlyComposable
        get() = LocalBreakpoints.current

    val gradients: AppGradients
        @Composable
        @ReadOnlyComposable
        get() = LocalGradients.current
}

// endregion
