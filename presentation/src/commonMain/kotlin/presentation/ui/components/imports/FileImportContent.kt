package presentation.ui.components.imports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import components.sheet.SheetSectionLabel
import domain.tag.model.Tag
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.add_words_file_title
import lexicon.resources.generated.resources.choose_a_file
import lexicon.resources.generated.resources.choose_file
import lexicon.resources.generated.resources.file_hint
import lexicon.resources.generated.resources.file_import_subtitle
import lexicon.resources.generated.resources.format_example_1
import lexicon.resources.generated.resources.format_example_2
import lexicon.resources.generated.resources.format_example_3
import lexicon.resources.generated.resources.how_to_format
import lexicon.resources.generated.resources.processing_file
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme
import utils.Language
import utils.rememberTextFilePickerLauncher

private val SampleLines = listOf(
    "die Wohnung, apartment",
    "vereinbaren, to arrange",
    "der Vertrag; contract; legal agreement",
)

@Composable
internal fun FileImportContent(
    isLoading: Boolean,
    sourceLanguage: Language,
    targetLanguage: Language,
    tags: List<Tag>,
    selectedTagId: Long?,
    onTagSelected: (Long?) -> Unit,
    onCreateTag: () -> Unit,
    onChangeLanguage: () -> Unit,
    importFile: (String, String?) -> Unit,
) {
    val filePickerLauncher = rememberTextFilePickerLauncher { fileContent, fileName ->
        if (fileContent != null) {
            importFile(fileContent, fileName)
        } else if (fileName != null) {
            importFile("", fileName)
        }
    }

    SheetPage(
        title = stringResource(Res.string.add_words_file_title),
        subtitle = stringResource(Res.string.file_import_subtitle),
        headerAccessory = {
            LanguagePairChip(source = sourceLanguage, target = targetLanguage, onClick = onChangeLanguage)
        },
        footer = {
            SheetPrimaryButton(
                text = stringResource(Res.string.choose_file),
                onClick = filePickerLauncher,
                isLoading = isLoading,
            )
        },
    ) {
        FileDropZone(onClick = filePickerLauncher, isLoading = isLoading)

        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            SheetSectionLabel(stringResource(Res.string.how_to_format))
            FormatSample()
            listOf(Res.string.format_example_1, Res.string.format_example_2, Res.string.format_example_3).forEach {
                FormatRule(stringResource(it))
            }
        }

        TagSelectorRow(
            tags = tags,
            selectedTagId = selectedTagId,
            onTagSelected = onTagSelected,
            onCreateTag = onCreateTag,
        )
    }
}

@Composable
private fun FileDropZone(onClick: () -> Unit, isLoading: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(Theme.shapes.extraLarge - Theme.spacing.xxs)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = Theme.opacity.hover / 2))
            .border(Theme.dimensions.borderWidth * 2, accent.copy(alpha = Theme.opacity.dimming), shape)
            .clickable(enabled = !isLoading, role = Role.Button, onClick = onClick)
            .padding(vertical = Theme.spacing.xl, horizontal = Theme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(Theme.dimensions.iconSizeMassive - Theme.spacing.xxs)
                .clip(CircleShape)
                .background(accent.copy(alpha = Theme.opacity.focus)),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Theme.dimensions.iconSize),
                    strokeWidth = Theme.spacing.xxxs,
                )
            } else {
                Icon(
                    Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(Theme.dimensions.iconSizeLarge),
                )
            }
        }
        Text(
            text = stringResource(if (isLoading) Res.string.processing_file else Res.string.choose_a_file),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(Res.string.file_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FormatSample() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
    ) {
        SampleLines.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}

@Composable
private fun FormatRule(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs + Theme.spacing.xxxs),
    ) {
        Box(
            modifier = Modifier
                .size(Theme.dimensions.iconSizeMedium)
                .clip(CircleShape)
                .background(AppColors.secondary.copy(alpha = Theme.opacity.focus)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = AppColors.secondary,
                modifier = Modifier.size(Theme.dimensions.iconSizeSmall - Theme.spacing.xxs),
            )
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
