package feature.profile.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import feature.profile.EditProfileViewModel
import feature.profile.ProfileViewModel
import feature.profile.ui.AvatarOptionsPage
import feature.profile.ui.ProfileMoreOptionsSheet
import feature.profile.ui.ProfileScreen
import feature.profile.ui.ProfileSheetPage
import feature.profile.ui.components.DeleteAccountCoolingContent
import feature.profile.ui.components.DeleteAccountHiddenContent
import feature.profile.ui.components.EditProfileSheetContent
import feature.profile.ui.components.LogoutDialogContent
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import overlay.OverlayHost
import overlay.bottomsheet.BottomSheetPageConfig
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.BottomSheetProperties
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import kotlinx.coroutines.launch
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.profile_updated
import org.jetbrains.compose.resources.stringResource
import overlay.bottomsheet.showSizeToFitBottomSheet
import utils.rememberImagePickerLauncher

@Serializable
data object ProfileRoute

fun NavGraphBuilder.profileGraph(
    snackbarHostState: SnackbarHostState,
) {
    composable<ProfileRoute> {
        ProfileScreen(
            snackbarHostState = snackbarHostState,
            onMoreOptions = {},
            onLogout = {},
        )
    }
}

fun OverlayHost.showProfileSheet(snackbarHostState: SnackbarHostState) {
    showSizeToFitBottomSheet(tag = "profile") { sheetNav ->
        val scope = rememberCoroutineScope()
        val profileUpdatedMessage = stringResource(Res.string.profile_updated)
        val pages = rememberBottomSheetPageNavigator<ProfileSheetPage>(ProfileSheetPage.Profile)
        val profileViewModel = koinViewModel<ProfileViewModel>()
        val editProfileViewModel = koinViewModel<EditProfileViewModel>()
        val editProfileState by editProfileViewModel.state()
        val imagePicker = rememberImagePickerLauncher { bytes ->
            if (bytes != null) editProfileViewModel.uploadAvatar(bytes, "image/jpeg")
        }

        BottomSheetPages(
            navigator = pages,
            onClose = { sheetNav.dismiss() },
            pageConfig = { page ->
                when (page) {
                    is ProfileSheetPage.Profile -> BottomSheetPageConfig(
                        showBackButton = false,
                        showCloseButton = false,
                        properties = BottomSheetProperties(),
                    )

                    is ProfileSheetPage.Options -> BottomSheetPageConfig(
                        showBackButton = true,
                        showCloseButton = false,
                        properties = BottomSheetProperties(),
                    )

                    is ProfileSheetPage.EditProfile -> BottomSheetPageConfig(
                        showBackButton = true,
                        showCloseButton = false,
                        properties = BottomSheetProperties(),
                    )

                    is ProfileSheetPage.AvatarOptions -> BottomSheetPageConfig(
                        showBackButton = true,
                        showCloseButton = false,
                        properties = BottomSheetProperties(),
                    )

                    is ProfileSheetPage.DeleteConfirm -> BottomSheetPageConfig(
                        showCloseButton = false,
                        properties = BottomSheetProperties(),
                    )

                    is ProfileSheetPage.DeleteCooling -> BottomSheetPageConfig(
                        showCloseButton = false,
                        properties = BottomSheetProperties(),
                    )

                    is ProfileSheetPage.Logout -> BottomSheetPageConfig(
                        showCloseButton = false,
                        properties = BottomSheetProperties(),
                    )
                }
            },
        ) { currentPage ->
            when (currentPage) {
                is ProfileSheetPage.Profile -> ProfileScreen(
                    snackbarHostState = snackbarHostState,
                    onMoreOptions = { pages.navigateTo(ProfileSheetPage.Options) },
                    onLogout = { pages.navigateTo(ProfileSheetPage.Logout) },
                )

                is ProfileSheetPage.Options -> ProfileMoreOptionsSheet(
                    onEditProfile = { pages.navigateTo(ProfileSheetPage.EditProfile) },
                    onDeleteAccount = { pages.navigateTo(ProfileSheetPage.DeleteConfirm) },
                )

                is ProfileSheetPage.EditProfile -> EditProfileSheetContent(
                    viewModel = editProfileViewModel,
                    onChangeAvatar = { pages.navigateTo(ProfileSheetPage.AvatarOptions) },
                    onSaved = {
                        pages.navigateBack()
                        scope.launch { snackbarHostState.showSnackbar(profileUpdatedMessage) }
                    },
                )

                is ProfileSheetPage.AvatarOptions -> AvatarOptionsPage(
                    hasExistingAvatar = editProfileState.profileImageUrl != null,
                    onChooseFromGallery = {
                        pages.navigateBack()
                        imagePicker()
                    },
                    onRemovePhoto = {
                        pages.navigateBack()
                        editProfileViewModel.deleteAvatar()
                    },
                )

                is ProfileSheetPage.DeleteConfirm -> DeleteAccountHiddenContent(
                    onConfirm = { pages.navigateTo(ProfileSheetPage.DeleteCooling) },
                    onDismiss = { sheetNav.dismiss() },
                )

                is ProfileSheetPage.DeleteCooling -> DeleteAccountCoolingContent(
                    onConfirm = {
                        sheetNav.dismiss()
                        profileViewModel.deleteAccount()
                    },
                    onDismiss = { sheetNav.dismiss() },
                )

                is ProfileSheetPage.Logout -> LogoutDialogContent(
                    onConfirm = {
                        sheetNav.dismiss()
                        profileViewModel.logout()
                    },
                    onDismiss = { pages.navigateBack() },
                )
            }
        }
    }
}
