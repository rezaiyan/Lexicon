package feature.profile.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import feature.profile.ui.EditProfileScreen
import feature.profile.ui.ProfileScreen
import kotlinx.serialization.Serializable
import overlay.LocalOverlayHost

@Serializable
data object ProfileRoute

@Serializable
data object EditProfileRoute

fun NavGraphBuilder.profileGraph(
    snackbarHostState: SnackbarHostState,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    composable<ProfileRoute> {
        ProfileScreen(
            snackbarHostState = snackbarHostState,
            overlayHost = LocalOverlayHost.current,
            onNavigateToLeaderboard = onNavigateToLeaderboard,
            onNavigateToEditProfile = onNavigateToEditProfile,
        )
    }

    composable<EditProfileRoute> {
        EditProfileScreen(
            snackbarHostState = snackbarHostState,
            overlayHost = LocalOverlayHost.current,
            onNavigateBack = onNavigateBack,
        )
    }
}
