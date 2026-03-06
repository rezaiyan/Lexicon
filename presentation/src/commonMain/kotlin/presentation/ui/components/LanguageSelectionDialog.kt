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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
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
import utils.Language
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.translation_language

@Composable
fun LanguageSelectionDialog(
    currentLanguage: Language,
    onDismiss: () -> Unit,
    onLanguageSelected: (Language) -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Default.Language,
        title = stringResource(Res.string.translation_language),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Language.entries.forEach { language ->
                    LanguageOption(
                        language = language,
                        isSelected = currentLanguage == language,
                        onClick = {
                            onLanguageSelected(language)
                            onDismiss()
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun LanguageOption(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Theme.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = language.nativeName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = language.displayName,
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

