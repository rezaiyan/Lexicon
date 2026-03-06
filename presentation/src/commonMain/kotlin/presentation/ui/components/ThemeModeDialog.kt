@file:OptIn(ExperimentalMaterial3Api::class)

package presentation.ui.components

import components.dialog.BasicAlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import domain.settings.model.ThemeMode
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.theme
import lexicon.resources.generated.resources.theme_auto_desc
import lexicon.resources.generated.resources.theme_dark_desc
import lexicon.resources.generated.resources.theme_light_desc

@Composable
fun ThemeModeDialog(
    currentThemeMode: ThemeMode,
    onDismiss: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Default.DarkMode,
        title = stringResource(Res.string.theme),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                ThemeMode.entries.forEach { mode ->
                    ThemeOptionCard(
                        themeMode = mode,
                        isSelected = currentThemeMode == mode,
                        onClick = {
                            onThemeModeSelected(mode)
                            onDismiss()
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun ThemeOptionCard(
    themeMode: ThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (themeMode) {
        ThemeMode.LIGHT -> Icons.Default.LightMode
        ThemeMode.DARK -> Icons.Default.DarkMode
        ThemeMode.AUTO -> Icons.Default.Brightness4
    }

    val description = when (themeMode) {
        ThemeMode.LIGHT -> stringResource(Res.string.theme_light_desc)
        ThemeMode.DARK -> stringResource(Res.string.theme_dark_desc)
        ThemeMode.AUTO -> stringResource(Res.string.theme_auto_desc)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Theme.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(Theme.dimensions.iconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = themeMode.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
    }
}

