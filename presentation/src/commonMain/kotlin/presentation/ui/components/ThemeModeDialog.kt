package presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.Composable
import components.sheet.SheetGroup
import components.sheet.SheetPage
import components.sheet.SheetRadioRow
import domain.settings.model.ThemeMode
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.customize_appearance
import lexicon.resources.generated.resources.theme
import lexicon.resources.generated.resources.theme_auto_desc
import lexicon.resources.generated.resources.theme_dark_desc
import lexicon.resources.generated.resources.theme_light_desc
import org.jetbrains.compose.resources.stringResource

/** Theme picker; tapping a row applies it. */
@Composable
fun ThemeModeContent(
    currentThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onClose: (() -> Unit)? = null,
) {
    SheetPage(
        title = stringResource(Res.string.theme),
        subtitle = stringResource(Res.string.customize_appearance),
        onClose = onClose,
    ) {
        SheetGroup {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SheetRadioRow(
                    title = mode.displayName,
                    subtitle = mode.description(),
                    icon = mode.icon(),
                    selected = currentThemeMode == mode,
                    onClick = { onThemeModeSelected(mode) },
                    showDivider = index < ThemeMode.entries.lastIndex,
                )
            }
        }
    }
}

private fun ThemeMode.icon() = when (this) {
    ThemeMode.LIGHT -> Icons.Default.LightMode
    ThemeMode.DARK -> Icons.Default.DarkMode
    ThemeMode.AUTO -> Icons.Default.Brightness4
}

@Composable
private fun ThemeMode.description() = when (this) {
    ThemeMode.LIGHT -> stringResource(Res.string.theme_light_desc)
    ThemeMode.DARK -> stringResource(Res.string.theme_dark_desc)
    ThemeMode.AUTO -> stringResource(Res.string.theme_auto_desc)
}
