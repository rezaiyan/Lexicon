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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import components.animation.staggeredFadeSlide
import components.dialog.LexiconDialogContent
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.add_word_description
import lexicon.resources.generated.resources.capture_vocab_from_image
import lexicon.resources.generated.resources.choose_import_method
import lexicon.resources.generated.resources.from_file
import lexicon.resources.generated.resources.from_image
import lexicon.resources.generated.resources.import_words
import lexicon.resources.generated.resources.select_txt_file_description
import lexicon.resources.generated.resources.type_text
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
internal fun ImportMethodChooserContent(
    hasImageAccess: Boolean,
    onTextSelected: () -> Unit,
    onFileSelected: () -> Unit,
    onImageSelected: () -> Unit,
) {
    LexiconDialogContent(
        title = stringResource(Res.string.import_words),
        message = stringResource(Res.string.choose_import_method),
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
            ) {
                ImportMethodCard(
                    icon = Icons.Filled.Edit,
                    title = Res.string.type_text,
                    subtitle = Res.string.add_word_description,
                    onClick = onTextSelected,
                    modifier = Modifier.staggeredFadeSlide(0),
                )

                ImportMethodCard(
                    icon = Icons.Filled.AttachFile,
                    title = Res.string.from_file,
                    subtitle = Res.string.select_txt_file_description,
                    onClick = onFileSelected,
                    modifier = Modifier.staggeredFadeSlide(1),
                )

                if (hasImageAccess) {
                    ImportMethodCard(
                        icon = Icons.Filled.CameraAlt,
                        title = Res.string.from_image,
                        subtitle = Res.string.capture_vocab_from_image,
                        onClick = onImageSelected,
                        modifier = Modifier.staggeredFadeSlide(2),
                    )
                }
            }
        }
    )
}

@Composable
private fun ImportMethodCard(
    icon: ImageVector,
    title: StringResource,
    subtitle: StringResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.dimensions.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(Theme.dimensions.touchTargetSmall)
                    .clip(RoundedCornerShape(Theme.shapes.small))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Theme.dimensions.iconSizeSmall)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(Theme.dimensions.iconSizeSmall)
            )
        }
    }
}
