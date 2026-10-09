package feature.profile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import components.GroupedRow
import components.GroupedSection
import components.Pill
import components.icons.LexiconIcons
import components.animation.staggeredFadeSlide
import feature.profile.model.ProfileSubscriptionStatus
import feature.profile.model.ProfileUiData
import feature.profile.ui.components.UserInfoSection
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.account_section
import lexicon.resources.generated.resources.delete_account
import lexicon.resources.generated.resources.edit_profile
import lexicon.resources.generated.resources.logout
import lexicon.resources.generated.resources.subscription
import lexicon.resources.generated.resources.upgrade
import lexicon.resources.generated.resources.upgrade_to_premium
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

@Composable
internal fun ProfileContent(
    profileData: ProfileUiData,
    onEditProfile: () -> Unit,
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit,
    onOpenSubscription: () -> Unit,
) {
    val userInfo = profileData.userInfo ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Theme.spacing.xs, bottom = Theme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
    ) {
        UserInfoSection(
            userInfo = userInfo,
            memberSince = profileData.profileStats?.memberSince,
            subscriptionStatus = profileData.subscriptionStatus,
            modifier = Modifier.staggeredFadeSlide(0),
            creditBalance = profileData.credits?.balance,
            creditsRefillAtMillis = profileData.credits?.periodEndsAtMillis,
        )

        GroupedSection(title = stringResource(Res.string.account_section)) {
            val isFree = profileData.subscriptionStatus == ProfileSubscriptionStatus.Free
            GroupedRow(
                title = stringResource(Res.string.subscription),
                subtitle = if (isFree) stringResource(Res.string.upgrade_to_premium) else null,
                icon = LexiconIcons.Diamond,
                iconColor = AppColors.settingsSubscriptionIcon,
                onClick = onOpenSubscription,
                trailingContent = if (isFree) {
                    { Pill(text = stringResource(Res.string.upgrade), color = MaterialTheme.colorScheme.error) }
                } else {
                    null
                },
            )
            GroupedRow(
                title = stringResource(Res.string.edit_profile),
                icon = Icons.Default.Edit,
                onClick = onEditProfile,
            )
            GroupedRow(
                title = stringResource(Res.string.delete_account),
                icon = Icons.Default.Delete,
                // Deliberately muted so it doesn't draw taps; confirmation sheet still uses error styling.
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onDeleteAccount,
                showChevron = false,
                showDivider = false,
            )
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(Theme.dimensions.buttonHeight),
            shape = RoundedCornerShape(Theme.shapes.pill),
            border = BorderStroke(Theme.dimensions.borderWidth * 1.5f, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(
                text = stringResource(Res.string.logout),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
