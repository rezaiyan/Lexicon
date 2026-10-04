package feature.onboarding.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import components.sheet.IconTile
import components.sheet.LanguageCodeTile
import components.sheet.LanguageListRow
import components.sheet.LevelBars
import components.sheet.RadioDot
import components.sheet.SelectableCard
import components.sheet.SheetSectionLabel
import feature.onboarding.model.DailyGoalOption
import feature.onboarding.model.OnboardingStep
import feature.onboarding.model.ProficiencyLevel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.advanced
import lexicon.resources.generated.resources.beginner
import lexicon.resources.generated.resources.intermediate
import lexicon.resources.generated.resources.onboarding_advanced_desc
import lexicon.resources.generated.resources.onboarding_are_you_learning
import lexicon.resources.generated.resources.onboarding_beginner_desc
import lexicon.resources.generated.resources.onboarding_current_level
import lexicon.resources.generated.resources.onboarding_daily_goal_subtitle
import lexicon.resources.generated.resources.onboarding_daily_goal_title1
import lexicon.resources.generated.resources.onboarding_daily_goal_title2
import lexicon.resources.generated.resources.onboarding_goal_casual
import lexicon.resources.generated.resources.onboarding_goal_intense
import lexicon.resources.generated.resources.onboarding_goal_regular
import lexicon.resources.generated.resources.onboarding_goal_serious
import lexicon.resources.generated.resources.onboarding_intermediate_desc
import lexicon.resources.generated.resources.onboarding_level_subtitle
import lexicon.resources.generated.resources.onboarding_monthly_words
import lexicon.resources.generated.resources.onboarding_native_language_question
import lexicon.resources.generated.resources.onboarding_native_language_subtitle
import lexicon.resources.generated.resources.onboarding_other_languages
import lexicon.resources.generated.resources.onboarding_phone_language
import lexicon.resources.generated.resources.onboarding_target_language_subtitle
import lexicon.resources.generated.resources.onboarding_whats_your
import lexicon.resources.generated.resources.onboarding_which_language
import lexicon.resources.generated.resources.onboarding_words_per_day
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.Language

// Font-scaled width below which two-column grids collapse to one column
private val TwoColumnMinWidth = 300.dp

@Composable
internal fun TargetLanguageQuestion(
    languages: List<Language>,
    selected: Language?,
    onSelected: (Language) -> Unit,
) {
    OnboardingQuestionPage(
        step = OnboardingStep.TargetLanguage,
        title = stringResource(Res.string.onboarding_which_language),
        highlight = stringResource(Res.string.onboarding_are_you_learning),
        subtitle = stringResource(Res.string.onboarding_target_language_subtitle),
    ) {
        AdaptiveGrid(items = languages, spacing = Theme.spacing.xs) { language, modifier ->
            val isSelected = language == selected
            SelectableCard(
                selected = isSelected,
                onClick = { onSelected(language) },
                role = Role.RadioButton,
                modifier = modifier,
            ) {
                LanguageCodeTile(code = language.code, selected = isSelected)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        language.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        language.nativeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The phone's language is offered first (preselected); the rest follow as a plain list. */
@Composable
internal fun NativeLanguageQuestion(
    suggestion: Language?,
    others: List<Language>,
    selected: Language?,
    onSelected: (Language) -> Unit,
) {
    OnboardingQuestionPage(
        step = OnboardingStep.NativeLanguage,
        title = stringResource(Res.string.onboarding_whats_your),
        highlight = stringResource(Res.string.onboarding_native_language_question),
        subtitle = stringResource(Res.string.onboarding_native_language_subtitle),
    ) {
        if (suggestion != null) {
            val isSelected = suggestion == selected
            SelectableCard(
                selected = isSelected,
                onClick = { onSelected(suggestion) },
                role = Role.RadioButton,
                modifier = Modifier.fillMaxWidth(),
            ) {
                LanguageCodeTile(code = suggestion.code, selected = isSelected)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.onboarding_phone_language),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        suggestion.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                    )
                }
            }
        }
        Column {
            if (suggestion != null) {
                SheetSectionLabel(stringResource(Res.string.onboarding_other_languages))
            }
            others.forEachIndexed { index, language ->
                LanguageListRow(
                    code = language.code,
                    name = language.displayName,
                    nativeName = language.nativeName,
                    selected = language == selected,
                    showDivider = index < others.lastIndex,
                    onClick = { onSelected(language) },
                )
            }
        }
    }
}

private val ProficiencyLevel.title: StringResource
    get() = when (this) {
        ProficiencyLevel.Beginner -> Res.string.beginner
        ProficiencyLevel.Intermediate -> Res.string.intermediate
        ProficiencyLevel.Advanced -> Res.string.advanced
    }

private val ProficiencyLevel.description: StringResource
    get() = when (this) {
        ProficiencyLevel.Beginner -> Res.string.onboarding_beginner_desc
        ProficiencyLevel.Intermediate -> Res.string.onboarding_intermediate_desc
        ProficiencyLevel.Advanced -> Res.string.onboarding_advanced_desc
    }

@Composable
internal fun LevelQuestion(
    selected: ProficiencyLevel?,
    onSelected: (ProficiencyLevel) -> Unit,
) {
    OnboardingQuestionPage(
        step = OnboardingStep.Level,
        title = stringResource(Res.string.onboarding_whats_your),
        highlight = stringResource(Res.string.onboarding_current_level),
        subtitle = stringResource(Res.string.onboarding_level_subtitle),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            ProficiencyLevel.entries.forEach { level ->
                val isSelected = level == selected
                SelectableCard(
                    selected = isSelected,
                    onClick = { onSelected(level) },
                    role = Role.RadioButton,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LevelBars(filled = level.ordinal + 1, selected = isSelected)
                    Column(
                        modifier = Modifier.weight(1f).padding(vertical = Theme.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
                    ) {
                        Text(
                            stringResource(level.title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(level.description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    RadioDot(selected = isSelected)
                }
            }
        }
    }
}

private val DailyGoalOption.label: StringResource
    get() = when (this) {
        DailyGoalOption.Casual -> Res.string.onboarding_goal_casual
        DailyGoalOption.Regular -> Res.string.onboarding_goal_regular
        DailyGoalOption.Serious -> Res.string.onboarding_goal_serious
        DailyGoalOption.Intense -> Res.string.onboarding_goal_intense
    }

@Composable
internal fun DailyGoalQuestion(
    selected: DailyGoalOption,
    monthlyWords: Int,
    onSelected: (DailyGoalOption) -> Unit,
) {
    OnboardingQuestionPage(
        step = OnboardingStep.DailyGoal,
        title = stringResource(Res.string.onboarding_daily_goal_title1),
        highlight = stringResource(Res.string.onboarding_daily_goal_title2),
        subtitle = stringResource(Res.string.onboarding_daily_goal_subtitle),
    ) {
        val perDay = stringResource(Res.string.onboarding_words_per_day)
        AdaptiveGrid(items = DailyGoalOption.entries, spacing = Theme.spacing.sm) { goal, modifier ->
            val isSelected = goal == selected
            SelectableCard(
                selected = isSelected,
                onClick = { onSelected(goal) },
                role = Role.RadioButton,
                modifier = modifier,
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(vertical = Theme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
                ) {
                    Text(
                        goal.words.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Text(
                        "$perDay · ${stringResource(goal.label)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Theme.dimensions.iconSizeMedium).align(Alignment.Top),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(Theme.shapes.large))
                .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            IconTile(icon = Icons.Default.CalendarMonth, tinted = true)
            Text(
                stringResource(Res.string.onboarding_monthly_words, monthlyWords),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Two equal columns, or one when the font-scaled width can't fit two labels side by side. */
@Composable
private fun <T> AdaptiveGrid(
    items: List<T>,
    spacing: Dp,
    cell: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    BoxWithConstraints {
        val columns = if (maxWidth / LocalDensity.current.fontScale < TwoColumnMinWidth) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            items.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    row.forEach { item -> cell(item, Modifier.weight(1f)) }
                    if (row.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
