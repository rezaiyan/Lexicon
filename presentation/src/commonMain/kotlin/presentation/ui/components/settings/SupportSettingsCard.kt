package presentation.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.runtime.Composable
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.support_settings_subtitle
import lexicon.resources.generated.resources.support_title
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.SettingsCard
import theme.AppColors

@Composable
fun SupportSettingsCard(onClick: () -> Unit) {
    SettingsCard(
        icon = Icons.Rounded.SupportAgent,
        title = stringResource(Res.string.support_title),
        subtitle = stringResource(Res.string.support_settings_subtitle),
        iconBackgroundColor = AppColors.settingsSupportIcon,
        onClick = onClick,
    )
}
