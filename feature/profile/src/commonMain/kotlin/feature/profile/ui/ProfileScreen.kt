package feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import feature.profile.ProfileViewModel
import feature.profile.model.ProfileUiData
import core.common.UiState
import components.scaffold.ActionIconConfig
import components.scaffold.LexiconColumn
import feature.profile.ui.components.DeleteAccountCoolingContent
import feature.profile.ui.components.DeleteAccountHiddenContent
import feature.profile.ui.components.EditProfileSheetContent
import feature.profile.ui.components.LogoutDialogContent
import overlay.OverlayHost
import overlay.bottomsheet.BottomSheetPageConfig
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.BottomSheetProperties
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import overlay.bottomsheet.showSizeToFitBottomSheet
import feature.leaderboard.navigation.showLeaderboard
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.more_options
import lexicon.resources.generated.resources.profile

@Composable
fun ProfileScreen(
    snackbarHostState: SnackbarHostState,
    overlayHost: OverlayHost,
) {
    val profileViewModel = koinViewModel<ProfileViewModel>()

    LaunchedEffect(Unit) { profileViewModel.refreshProfileStats() }

    val uiState by profileViewModel.state()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as UiState.Error).message,
                withDismissAction = true
            )
            profileViewModel.clearError()
        }
    }

    val profileData = (uiState as? UiState.Loaded<ProfileUiData>)?.value
    val isLoggedIn = profileData?.userInfo != null
    val isLoading = uiState is UiState.Loading

    LexiconColumn(
        title = stringResource(Res.string.profile),
        actionIcon1 = if (isLoggedIn) {
            ActionIconConfig(
                icon = Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.more_options),
                onClick = {
                    overlayHost.showSizeToFitBottomSheet(tag = "more-options") { sheetNav ->
                        val pages = rememberBottomSheetPageNavigator<MoreOptionsPage>(MoreOptionsPage.Options)

                        BottomSheetPages(
                            navigator = pages,
                            onClose = { sheetNav.dismiss() },
                            pageConfig = { page ->
                                when (page) {
                                    is MoreOptionsPage.Options -> BottomSheetPageConfig(
                                        showBackButton = false,
                                    )
                                    is MoreOptionsPage.EditProfile -> BottomSheetPageConfig(
                                        showBackButton = false,
                                        showCloseButton = false,
                                        properties = BottomSheetProperties(
                                            dismissOnTouchOutside = false,
                                            sheetGesturesEnabled = false,
                                        ),
                                    )
                                    is MoreOptionsPage.DeleteCooling -> BottomSheetPageConfig(
                                        properties = BottomSheetProperties(
                                            dismissOnTouchOutside = false,
                                            sheetGesturesEnabled = false,
                                        ),
                                    )
                                    else -> BottomSheetPageConfig()
                                }
                            },
                        ) { currentPage ->
                            when (currentPage) {
                                is MoreOptionsPage.Options -> ProfileMoreOptionsSheet(
                                    onEditProfile = { pages.navigateTo(MoreOptionsPage.EditProfile) },
                                    onDeleteAccount = { pages.navigateTo(MoreOptionsPage.DeleteConfirm) }
                                )
                                is MoreOptionsPage.EditProfile -> EditProfileSheetContent(
                                    snackbarHostState = snackbarHostState,
                                    overlayHost = overlayHost,
                                    onBack = { pages.navigateBack() },
                                    onDismiss = { sheetNav.dismiss() }
                                )
                                is MoreOptionsPage.DeleteConfirm -> DeleteAccountHiddenContent(
                                    onConfirm = { pages.navigateTo(MoreOptionsPage.DeleteCooling) },
                                    onDismiss = { sheetNav.dismiss() }
                                )
                                is MoreOptionsPage.DeleteCooling -> DeleteAccountCoolingContent(
                                    onConfirm = {
                                        sheetNav.dismiss()
                                        profileViewModel.deleteAccount()
                                    },
                                    onDismiss = { sheetNav.dismiss() }
                                )
                            }
                        }
                    }
                },
                size = Theme.dimensions.iconSize
            )
        } else null,
        scrollable = true
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                isLoggedIn -> {
                    ProfileContent(
                        profileData = profileData,
                        onNavigateToLeaderboard = { overlayHost.showLeaderboard() },
                        onLogout = {
                            overlayHost.showSizeToFitBottomSheet(tag = "logout") { nav ->
                                LogoutDialogContent(
                                    onConfirm = {
                                        nav.dismiss()
                                        profileViewModel.logout()
                                    },
                                    onDismiss = { nav.dismiss() }
                                )
                            }
                        }
                    )
                }
            }

            if (isLoggedIn && isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
