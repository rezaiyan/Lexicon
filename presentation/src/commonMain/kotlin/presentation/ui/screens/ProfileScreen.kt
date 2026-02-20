package presentation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import presentation.ui.components.profile.DeleteAccountCoolingDialogContent
import presentation.ui.components.profile.DeleteAccountHiddenDialogContent
import presentation.ui.components.profile.LogoutDialogContent
import presentation.ui.components.profile.StreakSection
import presentation.ui.components.profile.UserInfoSection
import presentation.ui.overlay.LocalOverlayHost
import presentation.ui.overlay.OverlayHost
import presentation.ui.overlay.bottomsheet.showFullscreenBottomSheet
import presentation.ui.overlay.dialog.showDialog
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.delete_account
import lexicon.resources.generated.resources.logout
import lexicon.resources.generated.resources.more_options
import lexicon.resources.generated.resources.profile

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
                        ProfileMoreOptionsSheet(
                            onDeleteAccount = {
                                navigator.dismiss()
                                showDeleteAccountFlow(overlayHost) {
                                    profileViewModel.onEvent(ProfileEvent.DeleteAccount)
                                }
                            }
                        )
                    }
                },
                size = 24.dp
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
                        onLogout = {
                            overlayHost.showDialog(tag = "logout") { nav ->
                                LogoutDialogContent(
                                    onConfirm = {
                                        nav.dismiss()
                                        profileViewModel.onEvent(ProfileEvent.Logout)
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

@Composable
private fun ProfileContent(
    profileData: ProfileUiData,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))

        UserInfoSection(
            userInfo = profileData.userInfo!!
        )

        if (profileData.streak != null) {
            Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))
            StreakSection(streak = profileData.streak)
        }

        Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing * 2))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.logout))
        }

        Spacer(modifier = Modifier.height(Theme.spacing.cardPadding))
    }
}

@Composable
private fun ProfileMoreOptionsSheet(
    onDeleteAccount: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Theme.spacing.sectionSpacing)
    ) {
        Text(
            text = stringResource(Res.string.more_options),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(Theme.spacing.sectionSpacing)
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDeleteAccount)
                .padding(
                    horizontal = Theme.spacing.sectionSpacing,
                    vertical = Theme.spacing.cardPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSizeXLarge),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(Theme.spacing.cardPadding))
            Text(
                text = stringResource(Res.string.delete_account),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error
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
