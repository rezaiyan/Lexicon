package presentation.ui.components.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import components.GeneratingProgress
import components.sheet.IconTile
import components.sheet.LevelBars
import components.sheet.RadioDot
import components.sheet.SelectableCard
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import domain.onboarding.model.ProficiencyLevel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.advanced
import lexicon.resources.generated.resources.ai_wizard_continue
import lexicon.resources.generated.resources.ai_wizard_generate_words
import lexicon.resources.generated.resources.ai_wizard_level_highlight
import lexicon.resources.generated.resources.ai_wizard_level_subtitle
import lexicon.resources.generated.resources.ai_wizard_level_title
import lexicon.resources.generated.resources.ai_wizard_topics_highlight
import lexicon.resources.generated.resources.ai_wizard_topics_subtitle
import lexicon.resources.generated.resources.ai_wizard_topics_title
import lexicon.resources.generated.resources.beginner
import lexicon.resources.generated.resources.generating_step_1
import lexicon.resources.generated.resources.generating_step_2
import lexicon.resources.generated.resources.generating_step_3
import lexicon.resources.generated.resources.generating_step_4
import lexicon.resources.generated.resources.generating_title
import lexicon.resources.generated.resources.intermediate
import lexicon.resources.generated.resources.onboarding_advanced_desc
import lexicon.resources.generated.resources.onboarding_beginner_desc
import lexicon.resources.generated.resources.onboarding_intermediate_desc
import lexicon.resources.generated.resources.topics_selected
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.Language

// Font-scaled width below which topic cards stack in one column
private val TwoColumnMinWidth = 300.dp

internal val ProficiencyLevel.title: StringResource
    get() = when (this) {
        ProficiencyLevel.BEGINNER -> Res.string.beginner
        ProficiencyLevel.INTERMEDIATE -> Res.string.intermediate
        ProficiencyLevel.ADVANCED -> Res.string.advanced
    }

private val ProficiencyLevel.description: StringResource
    get() = when (this) {
        ProficiencyLevel.BEGINNER -> Res.string.onboarding_beginner_desc
        ProficiencyLevel.INTERMEDIATE -> Res.string.onboarding_intermediate_desc
        ProficiencyLevel.ADVANCED -> Res.string.onboarding_advanced_desc
    }

@Composable
internal fun AiLevelStep(
    selectedLevel: ProficiencyLevel?,
    error: String?,
    onLevelSelected: (ProficiencyLevel) -> Unit,
    onContinue: () -> Unit,
) {
    SheetPage(
        title = stringResource(Res.string.ai_wizard_level_title),
        highlight = stringResource(Res.string.ai_wizard_level_highlight),
        subtitle = stringResource(Res.string.ai_wizard_level_subtitle),
        footer = {
            SheetPrimaryButton(
                text = stringResource(Res.string.ai_wizard_continue),
                onClick = onContinue,
                enabled = selectedLevel != null,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            ProficiencyLevel.entries.forEach { level ->
                val selected = level == selectedLevel
                SelectableCard(
                    selected = selected,
                    onClick = { onLevelSelected(level) },
                    role = Role.RadioButton,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LevelBars(filled = level.ordinal + 1, selected = selected)
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
                    RadioDot(selected = selected)
                }
            }
        }
        ErrorMessage(error)
    }
}

private val TopicIcons: Map<String, ImageVector> = mapOf(
    "Daily Life" to Icons.Default.Home,
    "Travel" to Icons.Default.Flight,
    "Business" to Icons.Default.Work,
    "Food" to Icons.Default.Restaurant,
    "Technology" to Icons.Default.Memory,
    "Sports" to Icons.Default.SportsSoccer,
    "Health" to Icons.Default.Favorite,
    "Arts" to Icons.Default.Palette,
    "Nature" to Icons.Default.Eco,
    "Academic" to Icons.Default.School,
)

@Composable
internal fun AiTopicsStep(
    topics: List<String>,
    selectedTopics: Set<String>,
    error: String?,
    onToggleTopic: (String) -> Unit,
    onGenerate: () -> Unit,
    generateEnabled: Boolean = true,
) {
    SheetPage(
        title = stringResource(Res.string.ai_wizard_topics_title),
        highlight = stringResource(Res.string.ai_wizard_topics_highlight),
        subtitle = stringResource(Res.string.ai_wizard_topics_subtitle),
        footer = {
            if (selectedTopics.isNotEmpty()) {
                Text(
                    stringResource(Res.string.topics_selected, selectedTopics.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SheetPrimaryButton(
                text = stringResource(Res.string.ai_wizard_generate_words),
                onClick = onGenerate,
                enabled = generateEnabled,
                icon = Icons.Default.AutoAwesome,
            )
        },
    ) {
        BoxWithConstraints {
            // Single column when the font-scaled width can't fit two labels side by side
            val columns = if (maxWidth / LocalDensity.current.fontScale < TwoColumnMinWidth) 1 else 2
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
                topics.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
                        row.forEach { topic ->
                            val selected = topic in selectedTopics
                            SelectableCard(
                                selected = selected,
                                onClick = { onToggleTopic(topic) },
                                role = Role.Checkbox,
                                modifier = Modifier.weight(1f),
                            ) {
                                IconTile(icon = TopicIcons[topic] ?: Icons.Default.AutoAwesome, selected = selected)
                                Text(
                                    topic,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(Theme.dimensions.iconSizeSmall + Theme.spacing.xxxs),
                                    )
                                }
                            }
                        }
                        if (row.size < columns) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        ErrorMessage(error)
    }
}

private val GeneratingSteps = listOf(
    Res.string.generating_step_1,
    Res.string.generating_step_2,
    Res.string.generating_step_3,
    Res.string.generating_step_4,
)

/** Shown while suggestions are generated. */
@Composable
internal fun AiGeneratingContent(learning: Language?, level: ProficiencyLevel?, topics: Set<String>) {
    val summary = listOfNotNull(
        learning?.displayName,
        level?.let { stringResource(it.title) },
        topics.takeIf { it.isNotEmpty() }?.joinToString(", "),
    ).joinToString(" · ")

    GeneratingProgress(
        title = stringResource(Res.string.generating_title),
        steps = GeneratingSteps.map { stringResource(it) },
        summary = summary,
        modifier = Modifier.padding(horizontal = Theme.spacing.xl, vertical = Theme.spacing.lg),
    )
}
