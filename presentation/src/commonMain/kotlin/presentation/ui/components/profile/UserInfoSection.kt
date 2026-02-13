package presentation.ui.components.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import presentation.model.ProfileUserUiModel
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.profile
import lexicon.resources.generated.resources.profile_picture

@Composable
fun UserInfoSection(
    userInfo: ProfileUserUiModel,
    onProfilePictureLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProfilePicture(
            onLongPress = onProfilePictureLongPress
        )

        Spacer(modifier = Modifier.height(Theme.spacing.cardSpacingLarge))

        Text(
            text = userInfo.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall3))

        Text(
            text = userInfo.email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ProfilePicture(
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(Theme.dimensions.profilePictureSize)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongPress() })
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = stringResource(Res.string.profile),
            modifier = Modifier.size(Theme.dimensions.iconSizeMassive),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

