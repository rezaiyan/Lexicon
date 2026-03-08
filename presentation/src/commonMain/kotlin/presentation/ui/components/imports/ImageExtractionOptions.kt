package presentation.ui.components.imports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.ai_powered_extraction
import lexicon.resources.generated.resources.camera
import lexicon.resources.generated.resources.capture_vocab_from_image
import lexicon.resources.generated.resources.choose_from_library
import lexicon.resources.generated.resources.extract_example_sentences
import lexicon.resources.generated.resources.extract_individual_words
import lexicon.resources.generated.resources.extraction_options
import lexicon.resources.generated.resources.gallery
import lexicon.resources.generated.resources.individual_words_hint
import lexicon.resources.generated.resources.select_at_least_one_option
import lexicon.resources.generated.resources.sentences_hint
import lexicon.resources.generated.resources.take_new_photo
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
internal fun AiExtractionInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(Theme.shapes.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSizeXLarge),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.ai_powered_extraction),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
                Text(
                    stringResource(Res.string.capture_vocab_from_image),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun ExtractionOptionsCard(
    extractWords: Boolean,
    extractSentences: Boolean,
    onExtractWordsChange: (Boolean) -> Unit,
    onExtractSentencesChange: (Boolean) -> Unit,
    isEnabled: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(Theme.shapes.medium),
    ) {
        Column(modifier = Modifier.padding(Theme.spacing.md)) {
            Text(
                stringResource(Res.string.extraction_options),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Theme.spacing.xs))

            ExtractionCheckbox(
                checked = extractWords,
                onCheckedChange = onExtractWordsChange,
                enabled = isEnabled,
                title = stringResource(Res.string.extract_individual_words),
                subtitle = stringResource(Res.string.individual_words_hint),
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Theme.spacing.xxxs),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            ExtractionCheckbox(
                checked = extractSentences,
                onCheckedChange = onExtractSentencesChange,
                enabled = isEnabled,
                title = stringResource(Res.string.extract_example_sentences),
                subtitle = stringResource(Res.string.sentences_hint),
            )
        }
    }

    AnimatedVisibility(
        visible = !extractWords && !extractSentences,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Theme.spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Theme.dimensions.iconSizeSmall)
            )
            Text(
                stringResource(Res.string.select_at_least_one_option),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ExtractionCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.small))
            .padding(Theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Theme.spacing.xs)
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun CaptureButtons(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    isEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
    ) {
        CaptureButton(
            onClick = onCameraClick,
            enabled = isEnabled,
            icon = Icons.Filled.CameraAlt,
            title = stringResource(Res.string.camera),
            subtitle = stringResource(Res.string.take_new_photo),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
        CaptureButton(
            onClick = onGalleryClick,
            enabled = isEnabled,
            icon = Icons.Filled.Photo,
            title = stringResource(Res.string.gallery),
            subtitle = stringResource(Res.string.choose_from_library),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CaptureButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(Theme.shapes.large),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
