package presentation.ui.components.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import components.animation.staggeredFadeSlide
import components.sheet.SheetBadge
import components.sheet.SheetGroup
import components.sheet.SheetOptionRow
import components.sheet.SheetPage
import components.sheet.SheetSectionLabel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.add_words_ai_subtitle
import lexicon.resources.generated.resources.add_words_ai_title
import lexicon.resources.generated.resources.add_words_file_subtitle
import lexicon.resources.generated.resources.add_words_file_title
import lexicon.resources.generated.resources.add_words_own_section
import lexicon.resources.generated.resources.add_words_photo_subtitle
import lexicon.resources.generated.resources.add_words_photo_title
import lexicon.resources.generated.resources.add_words_recommended
import lexicon.resources.generated.resources.add_words_subtitle
import lexicon.resources.generated.resources.add_words_title
import lexicon.resources.generated.resources.add_words_type_subtitle
import lexicon.resources.generated.resources.add_words_type_title
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/**
 * Single entry point of the add-words flow. Every source is offered to everyone; the AI ones show
 * their credit cost (the balance lives on the profile and under each spend button).
 *
 * @param aiCost / [photoCost] credits each costs; null while unknown.
 */
@Composable
internal fun AddWordsChooserContent(
    aiCost: Int?,
    photoCost: Int?,
    onAiAssistant: () -> Unit,
    onTypeWord: () -> Unit,
    onImportFile: () -> Unit,
    onScanPhoto: () -> Unit,
) {
    SheetPage(
        title = stringResource(Res.string.add_words_title),
        subtitle = stringResource(Res.string.add_words_subtitle),
    ) {
        AiAssistantCard(cost = aiCost, onClick = onAiAssistant, modifier = Modifier.staggeredFadeSlide(0))

        Column(
            modifier = Modifier.staggeredFadeSlide(1),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            SheetSectionLabel(stringResource(Res.string.add_words_own_section))
            SheetGroup {
                SheetOptionRow(
                    icon = Icons.Default.Edit,
                    title = stringResource(Res.string.add_words_type_title),
                    subtitle = stringResource(Res.string.add_words_type_subtitle),
                    onClick = onTypeWord,
                    showDivider = true,
                )
                SheetOptionRow(
                    icon = Icons.Default.Description,
                    title = stringResource(Res.string.add_words_file_title),
                    subtitle = stringResource(Res.string.add_words_file_subtitle),
                    onClick = onImportFile,
                    showDivider = true,
                )
                SheetOptionRow(
                    icon = Icons.Default.CameraAlt,
                    title = stringResource(Res.string.add_words_photo_title),
                    subtitle = stringResource(Res.string.add_words_photo_subtitle),
                    onClick = onScanPhoto,
                    showDivider = false,
                    trailingContent = { CreditCostBadge(photoCost) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiAssistantCard(cost: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.shapes.extraLarge - Theme.spacing.xxs),
        color = accent.copy(alpha = Theme.opacity.focus),
    ) {
        Row(
            modifier = Modifier.padding(Theme.spacing.md + Theme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.md),
        ) {
            Box(
                modifier = Modifier.size(Theme.dimensions.touchTarget),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = RoundedCornerShape(Theme.shapes.large), color = accent) {
                    Box(Modifier.size(Theme.dimensions.touchTarget), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(Theme.dimensions.iconSize),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.textGap),
            ) {
                // FlowRow: on narrow screens / large fonts the badge wraps under the title
                FlowRow(
                    itemVerticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
                ) {
                    Text(
                        stringResource(Res.string.add_words_ai_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    SheetBadge(
                        text = stringResource(Res.string.add_words_recommended),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = accent,
                    )
                    CreditCostBadge(cost)
                }
                Text(
                    stringResource(Res.string.add_words_ai_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
            )
        }
    }
}
