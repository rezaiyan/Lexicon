package feature.profile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import components.animation.staggeredFadeSlide
import feature.profile.model.ProfileUiData
import feature.profile.ui.components.MemberSinceSection
import feature.profile.ui.components.UserInfoSection
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.logout
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
internal fun ProfileContent(
    profileData: ProfileUiData,
    onLogout: () -> Unit,
) {
    val userInfo = profileData.userInfo ?: return
    val stats = profileData.profileStats

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var idx = 0

        Spacer(Modifier.height(Theme.spacing.md))

        UserInfoSection(
            userInfo = userInfo,
            modifier = Modifier.staggeredFadeSlide(idx++),
        )

        val memberSince = stats?.memberSince
        if (memberSince != null) {
            Spacer(Modifier.height(Theme.spacing.md))
            MemberSinceSection(
                memberSince = memberSince,
                modifier = Modifier.staggeredFadeSlide(idx++),
            )
        }

        Spacer(Modifier.height(Theme.spacing.lg))
        TextButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().staggeredFadeSlide(idx),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(Theme.spacing.xs))
            Text(
                text = stringResource(Res.string.logout),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(Modifier.height(Theme.spacing.md))
    }
}
