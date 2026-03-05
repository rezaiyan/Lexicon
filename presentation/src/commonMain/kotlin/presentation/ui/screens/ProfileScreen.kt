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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Leaderboard
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.feature.profile.ProfileViewModel
import presentation.model.ProfileUiData
import presentation.model.UiState
import presentation.ui.LocalSnackbarHostState
import presentation.ui.components.ActionIconConfig
import presentation.ui.components.LexiconColumn
import presentation.ui.components.profile.DeleteAccountCoolingDialogContent
import presentation.ui.components.profile.DeleteAccountHiddenDialogContent
import presentation.ui.components.profile.LogoutDialogContent
import presentation.ui.components.profile.MemberSinceSection
import presentation.ui.components.profile.StreakSection
import presentation.ui.components.profile.UserInfoSection
import presentation.ui.components.profile.WeeklyActivitySection
import presentation.ui.overlay.LocalOverlayHost
import presentation.ui.overlay.OverlayHost
import presentation.ui.overlay.bottomsheet.showFullscreenBottomSheet
import presentation.ui.overlay.dialog.showDialog
import presentation.ui.screens.study.staggeredFadeSlide
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.delete_account
import lexicon.resources.generated.resources.leaderboard
import lexicon.resources.generated.resources.logout
import lexicon.resources.generated.resources.edit_profile
import lexicon.resources.generated.resources.more_options
import lexicon.resources.generated.resources.profile

@Composable
fun ProfileScreen(
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {}
) {
    val profileViewModel = koinViewModel<ProfileViewModel>()
    val snackbarHostState = LocalSnackbarHostState.current
    val overlayHost = LocalOverlayHost.current

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
                    overlayHost.showFullscreenBottomSheet(tag = "more-options") { navigator ->
                        ProfileMoreOptionsSheet(
                            onEditProfile = {
                                navigator.dismiss()
                                onNavigateToEditProfile()
                            },
                            onDeleteAccount = {
                                navigator.dismiss()
                                showDeleteAccountFlow(overlayHost) {
                                    profileViewModel.deleteAccount()
                                }
                            }
                        )
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
                        onNavigateToLeaderboard = onNavigateToLeaderboard,
                        onLogout = {
                            overlayHost.showDialog(tag = "logout") { nav ->
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

@Composable
private fun ProfileContent(
    profileData: ProfileUiData,
    onNavigateToLeaderboard: () -> Unit,
    onLogout: () -> Unit,
) {
    val userInfo = profileData.userInfo ?: return

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var sectionIndex = 0

        Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))

        // 1. User Info
        UserInfoSection(
            userInfo = userInfo,
            modifier = Modifier.staggeredFadeSlide(sectionIndex++)
        )

        // 2. Streak Section (with optional longest streak from server)
        if (profileData.streak != null) {
            Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))
            StreakSection(
                streak = profileData.streak,
                longestStreak = profileData.profileStats?.longestStreak,
                modifier = Modifier.staggeredFadeSlide(sectionIndex++)
            )
        }

        // 3. Leaderboard Button
        Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))
        OutlinedButton(
            onClick = onNavigateToLeaderboard,
            modifier = Modifier
                .fillMaxWidth()
                .staggeredFadeSlide(sectionIndex++),
            border = BorderStroke(Theme.dimensions.borderWidth, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Leaderboard,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Theme.spacing.xs))
            Text(stringResource(Res.string.leaderboard))
        }

        // 4. Weekly Activity (server data — loads async)
        val weeklyActivity = profileData.profileStats?.weeklyActivity
        if (weeklyActivity != null) {
            Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))
            WeeklyActivitySection(
                weeklyActivity = weeklyActivity,
                modifier = Modifier
                    .staggeredFadeSlide(sectionIndex++)
            )
        }

        // 6. Member Since (server data — loads async)
        val memberSince = profileData.profileStats?.memberSince
        if (memberSince != null) {
            Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))
            MemberSinceSection(
                memberSince = memberSince,
                modifier = Modifier
                    .staggeredFadeSlide(sectionIndex++)
            )
        }

        // 7. Logout Button
        Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .staggeredFadeSlide(sectionIndex),
            border = BorderStroke(Theme.dimensions.borderWidth, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Theme.spacing.xs))
            Text(stringResource(Res.string.logout))
        }

        Spacer(modifier = Modifier.height(Theme.spacing.cardPadding))
    }
}

@Composable
private fun ProfileMoreOptionsSheet(
    onEditProfile: () -> Unit,
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
                .clickable(onClick = onEditProfile)
                .padding(
                    horizontal = Theme.spacing.sectionSpacing,
                    vertical = Theme.spacing.cardPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSizeXLarge),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(Theme.spacing.cardPadding))
            Text(
                text = stringResource(Res.string.edit_profile),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

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
