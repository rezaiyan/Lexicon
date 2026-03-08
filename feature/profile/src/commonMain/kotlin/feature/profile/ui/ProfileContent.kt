package feature.profile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import feature.profile.model.ProfileUiData
import feature.profile.ui.components.MemberSinceSection
import feature.profile.ui.components.StreakSection
import feature.profile.ui.components.UserInfoSection
import feature.profile.ui.components.WeeklyActivitySection
import components.animation.staggeredFadeSlide
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.leaderboard
import lexicon.resources.generated.resources.logout

@Composable
internal fun ProfileContent(
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
