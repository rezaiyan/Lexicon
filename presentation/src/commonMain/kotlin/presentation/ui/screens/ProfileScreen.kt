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
import presentation.feature.profile.ProfileViewModel
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
fun ProfileScreen(profileViewModel: ProfileViewModel) {
    val snackbarHostState = LocalSnackbarHostState.current
    val overlayHost = LocalOverlayHost.current

    val uiState by profileViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as UiState.Error).message,
                withDismissAction = true
            )
            profileViewModel.clearError()
        }
    }

    val profileData = (uiState as? UiState.Loaded)?.value

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
                                            profileViewModel.logout()
                                        },
                                        onDismiss = { nav.dismiss() }
                                    )
                                }
                            },
                            onDeleteAccount = {
                                overlayHost.showDialog(tag = "delete-account-hidden") { nav ->
                                    DeleteAccountHiddenDialogContent(
                                        onConfirm = {
                                            nav.dismiss()
                                            overlayHost.showDialog(tag = "delete-account-cooling") { coolingNav ->
                                                DeleteAccountCoolingDialogContent(
                                                    onConfirm = {
                                                        coolingNav.dismiss()
                                                        profileViewModel.deleteAccount()
                                                    },
                                                    onDismiss = { coolingNav.dismiss() }
                                                )
                                            }
                                        },
                                        onDismiss = { nav.dismiss() }
                                    )
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
                            profileViewModel = profileViewModel,
                            isLoading = isLoading
                        )
                    }
                }

                is UiState.Loaded -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isLoggedIn) {
                            UserInfoSection(
                                userInfo = profileData.userInfo,
                                onProfilePictureLongPress = {
                                    overlayHost.showDialog(tag = "delete-account-hidden") { nav ->
                                        DeleteAccountHiddenDialogContent(
                                            onConfirm = {
                                                nav.dismiss()
                                                overlayHost.showDialog(tag = "delete-account-cooling") { coolingNav ->
                                                    DeleteAccountCoolingDialogContent(
                                                        onConfirm = {
                                                            coolingNav.dismiss()
                                                            profileViewModel.deleteAccount()
                                                        },
                                                        onDismiss = { coolingNav.dismiss() }
                                                    )
                                                }
                                            },
                                            onDismiss = { nav.dismiss() }
                                        )
                                    }
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
                                profileViewModel = profileViewModel,
                                isLoading = isLoading
                            )
                        }
                    }
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