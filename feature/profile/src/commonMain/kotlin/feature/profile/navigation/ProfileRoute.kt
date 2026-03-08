package feature.profile.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import feature.profile.ui.ProfileScreen
import kotlinx.serialization.Serializable
import overlay.LocalOverlayHost

@Serializable
data object ProfileRoute

fun NavGraphBuilder.profileGraph(
    snackbarHostState: SnackbarHostState,
) {
    composable<ProfileRoute> {
        ProfileScreen(
            snackbarHostState = snackbarHostState,
            overlayHost = LocalOverlayHost.current,
        )
    }
}
