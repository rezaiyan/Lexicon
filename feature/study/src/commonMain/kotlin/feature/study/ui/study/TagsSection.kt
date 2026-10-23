package feature.study.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import components.AccentCard
import components.AccentIconBadge
import components.SectionHeader
import components.animation.staggeredFadeSlide
import domain.tag.model.Tag
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.tags
import lexicon.resources.generated.resources.word_count_label
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

private const val TagColumns = 2

/** Tags as a two-column grid of compact tiles; a tap reviews that tag's words. */
@Composable
fun TagsSection(
    tags: List<Tag>,
    onTagClick: (Tag) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tags.isEmpty()) return

    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(Res.string.tags),
            modifier = Modifier
                .staggeredFadeSlide(index = 0, baseDelayMs = 0)
                .padding(top = Theme.spacing.sectionGap, bottom = Theme.spacing.sectionHeaderGap)
        )

        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.listGap)) {
            tags.chunked(TagColumns).forEachIndexed { rowIndex, rowTags ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .staggeredFadeSlide(rowIndex + 1),
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.listGap),
                ) {
                    rowTags.forEach { tag ->
                        TagTile(
                            tag = tag,
                            onClick = { onTagClick(tag) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    // Keep a lone last tile at half width
                    repeat(TagColumns - rowTags.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun TagTile(
    tag: Tag,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = tag.wordCount.toInt()
    val countLabel = pluralStringResource(Res.plurals.word_count_label, count, count)
    AccentCard(
        accent = AppColors.adept,
        enabled = count > 0,
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = "${tag.name}: $countLabel" },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.inlineGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIconBadge(
                icon = Icons.Rounded.Label,
                accent = AppColors.adept,
                size = Theme.dimensions.touchTargetSmall,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
