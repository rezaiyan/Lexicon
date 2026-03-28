package feature.profile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import components.dialog.LexiconDialogContent
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.change_photo
import lexicon.resources.generated.resources.choose_from_gallery
import lexicon.resources.generated.resources.delete_account
import lexicon.resources.generated.resources.edit_profile
import lexicon.resources.generated.resources.remove_photo

@Composable
internal fun ProfileMoreOptionsSheet(
    onEditProfile: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    LexiconDialogContent(
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

@Composable
internal fun AvatarOptionsPage(
    hasExistingAvatar: Boolean,
    onChooseFromGallery: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    LexiconDialogContent(
        icon = Icons.Default.CameraAlt,
        title = stringResource(Res.string.change_photo),
        primaryButtonText = stringResource(Res.string.choose_from_gallery),
        primaryButtonOnClick = onChooseFromGallery,
        secondaryButtonText = if (hasExistingAvatar) stringResource(Res.string.remove_photo) else null,
        secondaryButtonOnClick = if (hasExistingAvatar) onRemovePhoto else null,
    )
}

internal sealed interface ProfileSheetPage {
    data object Profile : ProfileSheetPage
    data object Options : ProfileSheetPage
    data object EditProfile : ProfileSheetPage
    data object AvatarOptions : ProfileSheetPage
    data object DeleteConfirm : ProfileSheetPage
    data object DeleteCooling : ProfileSheetPage
    data object Logout : ProfileSheetPage
}
