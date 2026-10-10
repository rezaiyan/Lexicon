package feature.study.ui.focus

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import components.LanguageBadge
import domain.focus.model.LanguageSummary
import domain.focus.model.LearningFocus
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.focus_all_languages
import lexicon.resources.generated.resources.focus_all_languages_subtitle
import lexicon.resources.generated.resources.focus_dismiss
import lexicon.resources.generated.resources.focus_intro_got_it
import lexicon.resources.generated.resources.focus_intro_message
import lexicon.resources.generated.resources.focus_language_counts
import lexicon.resources.generated.resources.focus_nudge_message
import lexicon.resources.generated.resources.focus_nudge_switch
import lexicon.resources.generated.resources.focus_on
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.Language

/** Stable per-language tint from theme roles (no hardcoded colors). */
@Composable
private fun Language.badgeTint(): Color {
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
    )
    return palette[ordinal % palette.size]
}

@Composable
private fun LanguageCodeBadge(language: Language, size: Dp = Theme.dimensions.iconSizeLarge) {
    LanguageBadge(label = language.code.uppercase(), tint = language.badgeTint(), size = size)
}

/** App bar icon for the focus switcher; the sheet and content description name the current focus. */
val FocusLanguageIcon: ImageVector = Icons.Rounded.Language

@Composable
fun LanguageSwitcherSheetContent(
    focus: LearningFocus,
    languages: List<LanguageSummary>,
    onSelect: (LearningFocus) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.screenGutter, vertical = Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.listGap),
    ) {
        Text(stringResource(Res.string.focus_on), style = MaterialTheme.typography.titleMedium)
        languages.forEach { summary ->
            val option = LearningFocus.Single(summary.language)
            FocusOptionRow(
                leading = { LanguageCodeBadge(summary.language) },
                title = summary.language.nativeName,
                subtitle = stringResource(Res.string.focus_language_counts, summary.wordCount, summary.dueCount),
                selected = focus == option,
                hasDue = summary.dueCount > 0 && focus != option,
                onClick = { onSelect(option) },
            )
        }
        HorizontalDivider()
        FocusOptionRow(
            leading = {
                Icon(
                    Icons.Rounded.Language,
                    contentDescription = null,
                    modifier = Modifier.size(Theme.dimensions.iconSizeLarge),
                )
            },
            title = stringResource(Res.string.focus_all_languages),
            subtitle = stringResource(Res.string.focus_all_languages_subtitle),
            selected = focus == LearningFocus.All,
            hasDue = false,
            onClick = { onSelect(LearningFocus.All) },
        )
    }
}

@Composable
private fun FocusOptionRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String,
    selected: Boolean,
    hasDue: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.medium))
            .clickable(onClick = onClick)
            .padding(Theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        leading()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hasDue) {
            Box(
                Modifier
                    .size(Theme.spacing.xs)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun FocusNudgeCard(
    summary: LanguageSummary,
    onSwitch: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = Theme.spacing.cardPadding, top = Theme.spacing.xxs, bottom = Theme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        LanguageCodeBadge(summary.language, size = Theme.dimensions.iconSize)
        Text(
            text = stringResource(Res.string.focus_nudge_message, summary.dueCount, summary.language.displayName),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSwitch) { Text(stringResource(Res.string.focus_nudge_switch)) }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.focus_dismiss))
        }
    }
}

@Composable
fun FocusIntroCard(
    languageCount: Int,
    onGotIt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus))
            .padding(start = Theme.spacing.cardPadding, top = Theme.spacing.xs, bottom = Theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.focus_intro_message, languageCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onGotIt) { Text(stringResource(Res.string.focus_intro_got_it)) }
    }
}
