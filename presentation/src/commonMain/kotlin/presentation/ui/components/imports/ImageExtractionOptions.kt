package presentation.ui.components.imports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import components.sheet.SheetSectionLabel
import components.sheet.SheetTonalButton
import domain.tag.model.Tag
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.add_words_photo_title
import lexicon.resources.generated.resources.ai_powered_extraction
import lexicon.resources.generated.resources.choose_from_gallery_cta
import lexicon.resources.generated.resources.photo_best_results
import lexicon.resources.generated.resources.photo_subtitle
import lexicon.resources.generated.resources.photo_tip_format
import lexicon.resources.generated.resources.photo_tip_light
import lexicon.resources.generated.resources.photo_tip_sharp
import lexicon.resources.generated.resources.take_photo
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.Language

/** "Scan a photo" — tips for a good capture plus camera / gallery actions. */
@Composable
internal fun PhotoSourcePage(
    isEnabled: Boolean,
    sourceLanguage: Language,
    targetLanguage: Language,
    tags: List<Tag>,
    selectedTagId: Long?,
    onTagSelected: (Long?) -> Unit,
    onCreateTag: () -> Unit,
    onChangeLanguage: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
) {
    SheetPage(
        title = stringResource(Res.string.add_words_photo_title),
        subtitle = stringResource(Res.string.photo_subtitle),
        eyebrow = stringResource(Res.string.ai_powered_extraction),
        headerAccessory = {
            LanguagePairChip(source = sourceLanguage, target = targetLanguage, onClick = onChangeLanguage)
        },
        footer = {
            SheetPrimaryButton(
                text = stringResource(Res.string.take_photo),
                onClick = onCameraClick,
                enabled = isEnabled,
                icon = Icons.Default.CameraAlt,
            )
            SheetTonalButton(
                text = stringResource(Res.string.choose_from_gallery_cta),
                onClick = onGalleryClick,
                enabled = isEnabled,
                icon = Icons.Default.PhotoLibrary,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Theme.shapes.large))
                .background(MaterialTheme.colorScheme.surface)
                .padding(Theme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            SheetSectionLabel(stringResource(Res.string.photo_best_results))
            PhotoTip(Icons.Default.WbSunny, stringResource(Res.string.photo_tip_light))
            PhotoTip(Icons.Default.CropFree, stringResource(Res.string.photo_tip_sharp))
            PhotoTip(Icons.Default.Image, stringResource(Res.string.photo_tip_format))
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
private fun PhotoTip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(Theme.spacing.xl)
                .clip(RoundedCornerShape(Theme.shapes.small + Theme.spacing.xxxs))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Theme.dimensions.iconSizeSmall + Theme.spacing.xxxs),
            )
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
