package presentation.ui.components.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import presentation.ui.overlay.OverlayNavigator
import theme.Theme
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.delete_account
import vokab.resources.generated.resources.logout
import vokab.resources.generated.resources.more
import vokab.resources.generated.resources.more_options

@Composable
fun MoreOptionsBottomSheetContent(
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    navigator: OverlayNavigator
) {
    var showDeleteAccountOption by remember { mutableStateOf(false) }

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

        OptionItem(
            icon = Icons.AutoMirrored.Filled.Logout,
            text = stringResource(Res.string.logout),
            onClick = {
                navigator.dismiss()
                onLogout()
            },
            iconTint = MaterialTheme.colorScheme.primary
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        AnimatedVisibility(
            visible = !showDeleteAccountOption,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            OptionItem(
                icon = Icons.Default.MoreVert,
                text = stringResource(Res.string.more),
                onClick = { showDeleteAccountOption = true },
                iconTint = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(
            visible = showDeleteAccountOption,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            OptionItem(
                icon = Icons.Default.Delete,
                text = stringResource(Res.string.delete_account),
                onClick = {
                    navigator.dismiss()
                    onDeleteAccount()
                },
                iconTint = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun OptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = Theme.spacing.sectionSpacing,
                vertical = Theme.spacing.cardPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Theme.dimensions.iconSizeXLarge),
            tint = iconTint
        )
        Spacer(modifier = Modifier.width(Theme.spacing.cardPadding))
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

