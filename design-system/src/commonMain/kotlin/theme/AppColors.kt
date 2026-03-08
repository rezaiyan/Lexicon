package theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

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
)

internal val LightSemanticColors = AppSemanticColors(
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
)

internal val DarkSemanticColors = AppSemanticColors(
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

    background = Color(0xFF16161A),
)
