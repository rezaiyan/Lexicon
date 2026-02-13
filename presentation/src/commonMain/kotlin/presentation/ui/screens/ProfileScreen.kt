package presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.feature.profile.ProfileEvent
import presentation.feature.profile.ProfileViewModel
import presentation.model.ProfileUiData
import presentation.model.UiState
import presentation.ui.LocalSnackbarHostState
import presentation.ui.components.ActionIconConfig
import presentation.ui.components.LexiconColumn
import presentation.ui.components.profile.AuthenticationSection
import presentation.ui.components.profile.DeleteAccountCoolingDialogContent
import presentation.ui.components.profile.DeleteAccountHiddenDialogContent
import presentation.ui.components.profile.LogoutDialogContent
import presentation.ui.components.profile.MoreOptionsBottomSheetContent
import presentation.ui.components.profile.StreakSection
import presentation.ui.components.profile.UserInfoSection
import presentation.ui.overlay.LocalOverlayHost
import presentation.ui.overlay.OverlayHost
import presentation.ui.overlay.bottomsheet.showFullscreenBottomSheet
import presentation.ui.overlay.dialog.showDialog
import theme.Theme
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.more_options
import vokab.resources.generated.resources.profile

/**
 * Profile Screen - Self-contained
 * Manages its own ViewModel and state internally
 */
@Composable
fun ProfileScreen() {
    val profileViewModel = koinViewModel<ProfileViewModel>()
    val snackbarHostState = LocalSnackbarHostState.current
    val overlayHost = LocalOverlayHost.current

    val uiState by profileViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as UiState.Error).message,
                withDismissAction = true
            )
            profileViewModel.onEvent(ProfileEvent.ClearError)
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
                    overlayHost.showFullscreenBottomSheet(tag = "more-options") { navigator ->
                        MoreOptionsBottomSheetContent(
                            onLogout = {
                                navigator.dismiss()
                                overlayHost.showDialog(tag = "logout") { nav ->
                                    LogoutDialogContent(
                                        onConfirm = {
                                            nav.dismiss()
                                            profileViewModel.onEvent(ProfileEvent.Logout)
                                        },
                                        onDismiss = { nav.dismiss() }
                                    )
                                }
                            },
                            onDeleteAccount = {
                                showDeleteAccountFlow(overlayHost) {
                                    profileViewModel.onEvent(ProfileEvent.DeleteAccount)
                                }
                            },
                            navigator = navigator
                        )
                    }
                },
                size = 24.dp
            )
        } else null,
        scrollable = true
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            when (uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    if (profileData == null) {
                        AuthenticationSection(
                            isLoading = isLoading,
                            onLoginWithGoogle = { idToken -> profileViewModel.onEvent(ProfileEvent.LoginWithGoogle(idToken)) },
                            onLoginWithApple = { idToken, fullName, appleUserId -> profileViewModel.onEvent(ProfileEvent.LoginWithApple(idToken, fullName, appleUserId)) }
                        )
                    }
                }

                is UiState.Loaded -> {
                    val loadedData = (uiState as UiState.Loaded<ProfileUiData>).value
                    ProfileLoadedContent(
                        profileData = loadedData,
                        isLoading = isLoading,
                        onLoginWithGoogle = { idToken -> profileViewModel.onEvent(ProfileEvent.LoginWithGoogle(idToken)) },
                        onLoginWithApple = { idToken, fullName, appleUserId -> profileViewModel.onEvent(ProfileEvent.LoginWithApple(idToken, fullName, appleUserId)) },
                        onDeleteAccount = { profileViewModel.onEvent(ProfileEvent.DeleteAccount) },
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

@Composable
private fun ProfileLoadedContent(
    profileData: ProfileUiData,
    isLoading: Boolean,
    onLoginWithGoogle: suspend (String) -> Unit,
    onLoginWithApple: (String, String?, String) -> Unit,
    onDeleteAccount: () -> Unit,
) {
    val overlayHost = LocalOverlayHost.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (profileData.userInfo != null) {
            UserInfoSection(
                userInfo = profileData.userInfo,
                onProfilePictureLongPress = {
                    showDeleteAccountFlow(overlayHost, onDeleteAccount)
                },
            )

            Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))

            if (profileData.streak != null) {
                StreakSection(streak = profileData.streak)
                Spacer(modifier = Modifier.height(Theme.spacing.cardSpacingLarge))
            }

            Spacer(modifier = Modifier.height(Theme.spacing.large))
        } else {
            AuthenticationSection(
                isLoading = isLoading,
                onLoginWithGoogle = onLoginWithGoogle,
                onLoginWithApple = onLoginWithApple,
            )
        }
    }
}

private fun showDeleteAccountFlow(
    overlayHost: OverlayHost,
    onDeleteAccount: () -> Unit,
) {
    overlayHost.showDialog(tag = "delete-account-hidden") { nav ->
        DeleteAccountHiddenDialogContent(
            onConfirm = {
                nav.dismiss()
                overlayHost.showDialog(tag = "delete-account-cooling") { coolingNav ->
                    DeleteAccountCoolingDialogContent(
                        onConfirm = {
                            coolingNav.dismiss()
                            onDeleteAccount()
                        },
                        onDismiss = { coolingNav.dismiss() }
                    )
                }
            },
            onDismiss = { nav.dismiss() }
        )
    }
}