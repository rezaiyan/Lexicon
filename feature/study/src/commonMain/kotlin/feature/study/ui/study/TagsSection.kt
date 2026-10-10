package feature.study.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Label
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import components.SectionHeader
import components.animation.staggeredFadeSlide
import domain.tag.model.Tag
import feature.study.ui.components.LevelBucketCard
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.tags

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
            tags.forEachIndexed { index, tag ->
                LevelBucketCard(
                    modifier = Modifier.staggeredFadeSlide(index + 1),
                    level = tag.name,
                    description = null,
                    count = tag.wordCount.toInt(),
                    color = AppColors.adept,
                    icon = Icons.Rounded.Label,
                    onClick = { if (tag.wordCount > 0) onTagClick(tag) }
                )
            }
        }
    }
}
