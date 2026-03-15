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

    // Leaderboard medal colors
    val leaderboardGold = Color(0xFFFFD700)
    val leaderboardSilver = Color(0xFFC0C0C0)
    val leaderboardBronze = Color(0xFFCD7F32)

    // Accent palette — gradient endpoints for study completion, confetti, etc.
    val accentEmerald = Color(0xFF34D399)
    val accentLavender = Color(0xFFA78BFA)
    val accentSkyBlue = Color(0xFF60A5FA)
    val accentAmber = Color(0xFFFBBF24)
    val accentPink = Color(0xFFEC4899)
    val accentIndigo = Color(0xFF818CF8)

    // Settings icon colors — fixed across light/dark themes
    val settingsLanguageIcon = Color(0xFF9C27B0)
    val settingsThemeIcon = Color(0xFFE91E63)
    val settingsSubscriptionIcon = Color(0xFFE91E63)
    val settingsAboutIcon = Color(0xFF78909C)
    val settingsWordManagerIcon = Color(0xFFFF9800)
    val settingsNotificationIcon = Color(0xFF5C6BC0)
    val settingsTtsIcon = Color(0xFF00897B)

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

    // AI topic tile palette — 10 category tints
    val topicDailyLife: Color,
    val topicTravel: Color,
    val topicBusiness: Color,
    val topicFood: Color,
    val topicTechnology: Color,
    val topicSports: Color,
    val topicHealth: Color,
    val topicArts: Color,
    val topicNature: Color,
    val topicAcademic: Color,
)

/**
 * Language tile background colors, split by light/dark mode.
 * Keyed by English language name (e.g. "English", "German").
 */
object LanguageColors {
    val light: Map<String, Color> = mapOf(
        "English" to Color(0xFFE8EEF4),
        "German" to Color(0xFFFFF8E1),
        "French" to Color(0xFFE8F0FE),
        "Spanish" to Color(0xFFFFF0E8),
        "Italian" to Color(0xFFE8F5E9),
        "Portuguese" to Color(0xFFE0F2E9),
        "Dutch" to Color(0xFFFFF3E0),
        "Russian" to Color(0xFFE3E8F0),
        "Chinese" to Color(0xFFFFEBEE),
        "Japanese" to Color(0xFFFCE4EC),
        "Korean" to Color(0xFFEDE7F6),
        "Arabic" to Color(0xFFFAF3E0),
        "Turkish" to Color(0xFFFBE9E7),
        "Persian" to Color(0xFFE0F2F1),
    )

    val dark: Map<String, Color> = mapOf(
        "English" to Color(0xFF1F2A38),
        "German" to Color(0xFF3A3420),
        "French" to Color(0xFF1A2840),
        "Spanish" to Color(0xFF3A2820),
        "Italian" to Color(0xFF1B3A25),
        "Portuguese" to Color(0xFF1A3828),
        "Dutch" to Color(0xFF3A3020),
        "Russian" to Color(0xFF282838),
        "Chinese" to Color(0xFF3A1F1F),
        "Japanese" to Color(0xFF3A1E2A),
        "Korean" to Color(0xFF2D1F4E),
        "Arabic" to Color(0xFF38321A),
        "Turkish" to Color(0xFF3A2420),
        "Persian" to Color(0xFF1A3836),
    )
}

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

    topicDailyLife = Color(0xFFFFF3E0),
    topicTravel = Color(0xFFE3F2FD),
    topicBusiness = Color(0xFFECEFF1),
    topicFood = Color(0xFFFBE9E7),
    topicTechnology = Color(0xFFEDE7F6),
    topicSports = Color(0xFFE8F5E9),
    topicHealth = Color(0xFFFCE4EC),
    topicArts = Color(0xFFF3E5F5),
    topicNature = Color(0xFFE0F2F1),
    topicAcademic = Color(0xFFE8EAF6),
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

    topicDailyLife = Color(0xFF4E3B24),
    topicTravel = Color(0xFF1A3A5C),
    topicBusiness = Color(0xFF2C3440),
    topicFood = Color(0xFF4A2C22),
    topicTechnology = Color(0xFF2D1F4E),
    topicSports = Color(0xFF1B3A25),
    topicHealth = Color(0xFF3E1F2A),
    topicArts = Color(0xFF3A1E42),
    topicNature = Color(0xFF1A3836),
    topicAcademic = Color(0xFF1F2346),
)
