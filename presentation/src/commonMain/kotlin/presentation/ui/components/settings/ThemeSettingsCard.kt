package presentation.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.SettingsCard
import domain.settings.model.ThemeMode
import theme.AppColors
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.customize_appearance
import lexicon.resources.generated.resources.theme

@Composable
fun ThemeSettingsCard(
    themeMode: ThemeMode,
    onShowThemeDialog: () -> Unit
) {
    SettingsCard(
        icon = Icons.Default.DarkMode,
        title = stringResource(Res.string.theme),
        subtitle = stringResource(Res.string.customize_appearance),
        iconBackgroundColor = AppColors.settingsThemeIcon,
        onClick = onShowThemeDialog
    )
}
