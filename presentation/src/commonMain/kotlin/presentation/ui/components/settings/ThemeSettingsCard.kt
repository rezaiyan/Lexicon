package presentation.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.SettingsCard
import domain.settings.model.ThemeMode
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.customize_appearance
import lexicon.resources.generated.resources.theme

private val ThemeIconColor = Color(0xFFE91E63)

@Composable
fun ThemeSettingsCard(
    themeMode: ThemeMode,
    onShowThemeDialog: () -> Unit
) {
    SettingsCard(
        icon = Icons.Default.DarkMode,
        title = stringResource(Res.string.theme),
        subtitle = stringResource(Res.string.customize_appearance),
        iconBackgroundColor = ThemeIconColor,
        onClick = onShowThemeDialog
    )
}
