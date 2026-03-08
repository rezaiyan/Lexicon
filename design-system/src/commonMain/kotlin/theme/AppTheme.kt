package theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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
    onBackground = Color(0xFF16161A),

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
