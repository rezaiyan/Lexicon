package presentation.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.SettingsCard
import theme.AppColors
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.tag_manager
import lexicon.resources.generated.resources.tag_manager_subtitle

@Composable
fun TagManagerCard(onClick: () -> Unit) {
    SettingsCard(
        icon = Icons.Default.LocalOffer,
        title = stringResource(Res.string.tag_manager),
        subtitle = stringResource(Res.string.tag_manager_subtitle),
        iconBackgroundColor = AppColors.settingsTagManagerIcon,
        onClick = onClick
    )
}
