package theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AppColors {
    val normalBackground = Color(0xFFF0EDFF)
    val normalSurface = Color(0xFFFAF9FF)

    val novice = Color(0xFF7F5AF0)
    val apprentice = Color(0xFF2CB67D)
    val adept = Color(0xFFFF8906)
    val master = Color(0xFFE53170)

    val subscriptionRecommended = Color(0xFF9F7AEA)
    val subscriptionStandard = Color(0xFF10B981)
}

@Immutable
data class AppSpacing(
    val none: Dp = 0.dp,
    val extraSmall4: Dp = 2.dp,
    val extraSmall3: Dp = 4.dp,
    val extraSmall2: Dp = 8.dp,
    val extraSmall: Dp = 12.dp,
    val small: Dp = 16.dp,
    val medium: Dp = 24.dp,
    val large: Dp = 32.dp,
    val extraLarge: Dp = 40.dp,
    val extraLarge2: Dp = 48.dp,
    val extraLarge3: Dp = 56.dp,
    val extraLarge4: Dp = 64.dp,
    val extraLarge5: Dp = 72.dp,

    @Deprecated("Use new spacings")
    val cardPadding: Dp = 16.dp,
    @Deprecated("Use new spacings")
    val cardSpacing: Dp = 12.dp,
    @Deprecated("Use new spacings")
    val cardSpacingLarge: Dp = 16.dp,
    @Deprecated("Use new spacings")
    val sectionSpacing: Dp = 24.dp,
)

@Immutable
data class AppDimensions(

    val iconSizeMedium: Dp = 20.dp,
    val iconSize: Dp = 24.dp,
    val iconSizeLarge: Dp = 28.dp,
    val iconSizeXLarge: Dp = 32.dp,
    val iconSizeHuge: Dp = 48.dp,
    val iconSizeMassive: Dp = 60.dp,

    val cardCornerRadius: Dp = 20.dp,

    val profilePictureSize: Dp = 120.dp,

    val progressBarHeight: Dp = 6.dp,
    val borderWidth: Dp = 1.dp,
    val borderWidthThick: Dp = 4.dp
)

val LocalSpacing = staticCompositionLocalOf { AppSpacing() }
val LocalDimensions = staticCompositionLocalOf { AppDimensions() }

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

    background = Color(0xFFF0EDFF),
    onBackground = Color(0xFF1E1E1E),

    surface = Color(0xFFFAF9FF),
    onSurface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFFE8E4FF),
    onSurfaceVariant = Color(0xFF666666),

    error = Color(0xFFE53170),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFD9E3),
    onErrorContainer = Color(0xFF3E0019),

    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFE0E0E0),
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

    background = Color(0xFF0F0F1E),
    onBackground = Color(0xFFFFFFFE),

    surface = Color(0xFF1A1A2E),
    onSurface = Color(0xFFFFFFFE),
    surfaceVariant = Color(0xFF22223A),
    onSurfaceVariant = Color(0xFFCCCCCC),

    error = Color(0xFFE53170),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF93002A),
    onErrorContainer = Color(0xFFFFD9E3),

    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2A2A2A),
)

private fun appTypography(emojiFontFamily: FontFamily? = null): Typography {
    val ff = emojiFontFamily
    return Typography(
        displayLarge = TextStyle(
            fontFamily = ff,
            fontSize = 57.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = ff,
            fontSize = 45.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 52.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = ff,
            fontSize = 36.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = ff,
            fontSize = 32.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = ff,
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = ff,
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = ff,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = ff,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = ff,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = ff,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = ff,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = ff,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = ff,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = ff,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = ff,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

@Composable
fun LexiconTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val emojiFontFamily = platformFontFamily()
    val typography = appTypography(emojiFontFamily)

    CompositionLocalProvider(
        LocalSpacing provides AppSpacing(),
        LocalDimensions provides AppDimensions()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
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
}