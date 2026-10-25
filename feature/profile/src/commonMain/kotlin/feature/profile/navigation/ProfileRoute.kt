package feature.profile.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import feature.profile.EditProfileViewModel
import feature.profile.ProfileViewModel
import feature.profile.ui.ProfileScreen
import feature.profile.ui.ProfileSheetPage
import feature.profile.ui.components.EditProfileSheetContent
import feature.profile.ui.components.LogoutDialogContent
import components.scaffold.LexiconColumn
import kotlinx.serialization.Serializable
import lexicon.resources.generated.resources.profile
import overlay.LocalOverlayHost
import org.koin.compose.viewmodel.koinViewModel
import overlay.OverlayHost
import overlay.OverlayNavigator
import overlay.bottomsheet.BottomSheetPageConfig
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.BottomSheetProperties
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import kotlinx.coroutines.launch
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.profile_updated
import org.jetbrains.compose.resources.stringResource
import overlay.bottomsheet.showSizeToFitBottomSheet
import components.snackbar.showSuccess

@Serializable
data object ProfileRoute

fun NavGraphBuilder.profileGraph(
    snackbarHostState: SnackbarHostState,
    onOpenSubscription: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable<ProfileRoute> {
        val overlayHost = LocalOverlayHost.current
        LexiconColumn(
            title = stringResource(Res.string.profile),
            scrollable = false,
        ) {
            ProfileScreen(
                snackbarHostState = snackbarHostState,
                onEditProfile = { overlayHost.showProfileSheet(snackbarHostState, ProfileSheetPage.EditProfile) },
                onLogout = { overlayHost.showProfileSheet(snackbarHostState, ProfileSheetPage.Logout) },
                onOpenSubscription = onOpenSubscription,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

private fun OverlayHost.showProfileSheet(
    snackbarHostState: SnackbarHostState,
    startPage: ProfileSheetPage,
) {
    showSizeToFitBottomSheet(tag = "profile") { sheetNav ->
        ProfileSheetContent(
            sheetNav = sheetNav,
            snackbarHostState = snackbarHostState,
            startPage = startPage,
        )
    }
}

@Composable
private fun ProfileSheetContent(
    sheetNav: OverlayNavigator,
    snackbarHostState: SnackbarHostState,
    startPage: ProfileSheetPage,
) {
    val scope = rememberCoroutineScope()
    val profileUpdatedMessage = stringResource(Res.string.profile_updated)
    val pages = rememberBottomSheetPageNavigator(startPage)
    val profileViewModel = koinViewModel<ProfileViewModel>()
    val editProfileViewModel = koinViewModel<EditProfileViewModel>()

    BottomSheetPages(
        navigator = pages,
        onClose = { sheetNav.dismiss() },
        pageConfig = { page ->
            val config = profileSheetPageConfig(page)
            if (page == startPage) config.copy(showBackButton = false) else config
        },
    ) { currentPage ->
        when (currentPage) {
            is ProfileSheetPage.EditProfile -> EditProfileSheetContent(
                viewModel = editProfileViewModel,
                onSaved = {
                    if (!pages.navigateBack()) sheetNav.dismiss()
                    scope.launch { snackbarHostState.showSuccess(profileUpdatedMessage) }
                },
            )

            is ProfileSheetPage.Logout -> LogoutDialogContent(
                onConfirm = {
                    sheetNav.dismiss()
                    profileViewModel.logout()
                },
                onDismiss = { if (!pages.navigateBack()) sheetNav.dismiss() },
            )
        }
    }
}

private fun profileSheetPageConfig(page: ProfileSheetPage): BottomSheetPageConfig = when (page) {
    is ProfileSheetPage.EditProfile -> BottomSheetPageConfig(
        showBackButton = true,
        showCloseButton = true,
        properties = BottomSheetProperties(),
    )
    is ProfileSheetPage.Logout -> BottomSheetPageConfig(
        showCloseButton = false,
        properties = BottomSheetProperties(),
    )
}
