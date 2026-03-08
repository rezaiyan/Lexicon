package presentation.ui.components.imports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import components.dialog.LexiconDialogContent
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.confirm_languages
import lexicon.resources.generated.resources.import_text
import lexicon.resources.generated.resources.original_language
import lexicon.resources.generated.resources.translation_language
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.LanguageSelectionContent
import theme.Theme
import utils.Language

@Composable
internal fun ImportInfoCard(
    title: String,
    description: String,
    icon: ImageVector,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(Theme.shapes.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.md),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Theme.dimensions.iconSize)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(Theme.spacing.xxs))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
internal fun ImportLanguageConfirmationContent(
    sourceLanguage: Language,
    targetLanguage: Language,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onShowSourceLanguage: () -> Unit,
    onShowTargetLanguage: () -> Unit,
) {
    LexiconDialogContent(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = Theme.spacing.lg)
            .padding(bottom = Theme.spacing.lg),
        icon = Icons.Default.Language,
        title = stringResource(Res.string.confirm_languages),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
            ) {
                LanguageRow(
                    label = stringResource(Res.string.original_language),
                    language = sourceLanguage,
                    onClick = onShowSourceLanguage,
                )
                LanguageRow(
                    label = stringResource(Res.string.translation_language),
                    language = targetLanguage,
                    onClick = onShowTargetLanguage,
                )
            }
        },
        primaryButtonText = stringResource(Res.string.import_text),
        primaryButtonOnClick = onConfirm,
        secondaryButtonText = stringResource(Res.string.cancel),
        secondaryButtonOnClick = onDismiss,
    )
}

@Composable
private fun LanguageRow(
    label: String,
    language: Language,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.small))
            .clickable(onClick = onClick)
            .padding(vertical = Theme.spacing.xs, horizontal = Theme.spacing.sm)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
        ) {
            Text(
                text = "${language.nativeName} (${language.displayName})",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun LanguagePickerPage(
    currentLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.md)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
            )
        }
        LanguageSelectionContent(
            currentLanguage = currentLanguage,
            onLanguageSelected = onLanguageSelected,
        )
    }
}
