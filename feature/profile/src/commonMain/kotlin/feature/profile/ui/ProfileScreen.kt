package feature.profile.ui

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
import components.dialog.LexiconDialogContent
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
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
import feature.profile.ProfileViewModel
import feature.profile.model.ProfileUiData
import core.common.UiState
import components.scaffold.ActionIconConfig
import components.scaffold.LexiconColumn
import feature.profile.ui.components.DeleteAccountCoolingContent
import feature.profile.ui.components.DeleteAccountHiddenContent
import feature.profile.ui.components.EditProfileSheetContent
import feature.profile.ui.components.LogoutDialogContent
import feature.profile.ui.components.MemberSinceSection
import feature.profile.ui.components.StreakSection
import feature.profile.ui.components.UserInfoSection
import feature.profile.ui.components.WeeklyActivitySection
import overlay.OverlayHost
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.BottomSheetProperties
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import overlay.bottomsheet.showSizeToFitBottomSheet
import overlay.bottomsheet.showSizeToFitBottomSheet
import components.animation.staggeredFadeSlide
import feature.leaderboard.navigation.showLeaderboard
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

                        LaunchedEffect(pages.currentPage) {
                            properties = when (pages.currentPage) {
                                is MoreOptionsPage.EditProfile -> BottomSheetProperties(
                                    dismissOnTouchOutside = false,
                                    sheetGesturesEnabled = false
                                )
                                is MoreOptionsPage.DeleteCooling -> BottomSheetProperties(
                                    dismissOnTouchOutside = false,
                                    sheetGesturesEnabled = false
                                )
                                else -> BottomSheetProperties(showCloseButton = false)
                            }
                        }

                        BottomSheetPages(navigator = pages) { currentPage ->
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
    LexiconDialogContent(
        title = stringResource(Res.string.more_options),
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onEditProfile)
                        .padding(vertical = Theme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(Theme.dimensions.iconSizeXLarge),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(Theme.spacing.md))
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
                        .padding(vertical = Theme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(Theme.dimensions.iconSizeXLarge),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(Theme.spacing.md))
                    Text(
                        text = stringResource(Res.string.delete_account),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

private sealed interface MoreOptionsPage {
    data object Options : MoreOptionsPage
    data object EditProfile : MoreOptionsPage
    data object DeleteConfirm : MoreOptionsPage
    data object DeleteCooling : MoreOptionsPage
}
