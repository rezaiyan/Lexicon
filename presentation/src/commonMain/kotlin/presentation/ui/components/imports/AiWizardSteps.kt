package presentation.ui.components.imports

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import components.sheet.IconTile
import components.sheet.SelectableCard
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import feature.aiimport.model.AiWordImportStep
import feature.aiimport.model.AiWordImportUiState
import kotlinx.coroutines.delay
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.advanced
import lexicon.resources.generated.resources.ai_wizard_continue
import lexicon.resources.generated.resources.ai_wizard_generate_words
import lexicon.resources.generated.resources.ai_wizard_level_highlight
import lexicon.resources.generated.resources.ai_wizard_level_subtitle
import lexicon.resources.generated.resources.ai_wizard_level_title
import lexicon.resources.generated.resources.ai_wizard_native_highlight
import lexicon.resources.generated.resources.ai_wizard_native_subtitle
import lexicon.resources.generated.resources.ai_wizard_native_title
import lexicon.resources.generated.resources.ai_wizard_target_highlight
import lexicon.resources.generated.resources.ai_wizard_target_subtitle
import lexicon.resources.generated.resources.ai_wizard_target_title
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
import lexicon.resources.generated.resources.step_of
import lexicon.resources.generated.resources.topics_selected
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.Language

private const val GeneratingStepMillis = 2500L
private val LevelBarWidth = 6.dp
private val GeneratingRingSize = 120.dp

// Font-scaled width below which topic cards stack in one column
private val TwoColumnMinWidth = 300.dp

@Composable
private fun stepEyebrow(step: AiWordImportStep): String =
    stringResource(Res.string.step_of, step.ordinal + 1, AiWizardTotalSteps)

/** The wizard keeps languages as display names; map them onto [Language] for code tiles and search. */
private fun languagesFor(names: List<String>): List<Language> =
    names.mapNotNull { name -> Language.entries.firstOrNull { it.displayName == name } }

@Composable
internal fun AiTargetLanguageStep(
    languages: List<String>,
    selected: String?,
    onSelected: (String) -> Unit,
) {
    val options = remember(languages) { languagesFor(languages) }
    ImportLanguageListPage(
        eyebrow = stepEyebrow(AiWordImportStep.TARGET_LANG),
        title = stringResource(Res.string.ai_wizard_target_title),
        highlight = stringResource(Res.string.ai_wizard_target_highlight),
        subtitle = stringResource(Res.string.ai_wizard_target_subtitle),
        languages = options,
        selected = options.firstOrNull { it.displayName == selected },
        onLanguageSelected = { onSelected(it.displayName) },
    )
}

@Composable
internal fun AiNativeLanguageStep(
    languages: List<String>,
    selected: String?,
    onSelected: (String) -> Unit,
) {
    val options = remember(languages) { languagesFor(languages) }
    ImportLanguageListPage(
        eyebrow = stepEyebrow(AiWordImportStep.NATIVE_LANG),
        title = stringResource(Res.string.ai_wizard_native_title),
        highlight = stringResource(Res.string.ai_wizard_native_highlight),
        subtitle = stringResource(Res.string.ai_wizard_native_subtitle),
        languages = options,
        selected = options.firstOrNull { it.displayName == selected },
        onLanguageSelected = { onSelected(it.displayName) },
    )
}

private data class LevelOption(
    val key: String,
    val name: StringResource,
    val description: StringResource,
    val bars: Int,
)

private val LevelOptions = listOf(
    LevelOption("beginner", Res.string.beginner, Res.string.onboarding_beginner_desc, 1),
    LevelOption("intermediate", Res.string.intermediate, Res.string.onboarding_intermediate_desc, 2),
    LevelOption("advanced", Res.string.advanced, Res.string.onboarding_advanced_desc, 3),
)

@Composable
internal fun AiLevelStep(
    selectedLevel: String?,
    error: String?,
    onLevelSelected: (String) -> Unit,
    onContinue: () -> Unit,
) {
    SheetPage(
        eyebrow = stepEyebrow(AiWordImportStep.LEVEL),
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
            LevelOptions.forEach { option ->
                val selected = option.key == selectedLevel
                SelectableCard(
                    selected = selected,
                    onClick = { onLevelSelected(option.key) },
                    role = Role.RadioButton,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LevelBars(filled = option.bars, selected = selected)
                    Column(
                        modifier = Modifier.weight(1f).padding(vertical = Theme.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
                    ) {
                        Text(
                            stringResource(option.name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(option.description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    RadioDot(selected = selected)
                }
            }
        }
        error?.let { ErrorMessage(it) }
    }
}

@Composable
private fun LevelBars(filled: Int, selected: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    val tile = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = Modifier
            .size(Theme.dimensions.touchTargetSmall + Theme.spacing.xxs)
            .clip(RoundedCornerShape(Theme.shapes.medium))
            .background(tile)
            .padding(bottom = Theme.spacing.sm - Theme.spacing.xxxs),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(10, 16, 22).forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(LevelBarWidth)
                    .height(height.dp)
                    .clip(CircleShape)
                    .background(if (index < filled) accent else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    val ring by animateColorAsState(if (selected) accent else MaterialTheme.colorScheme.outline, label = "radio")
    Box(
        modifier = Modifier
            .size(Theme.dimensions.iconSizeMedium + Theme.spacing.xxxs)
            .border(Theme.spacing.xxxs, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.size(Theme.spacing.sm - Theme.spacing.xxxs).clip(CircleShape).background(accent))
        }
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
) {
    SheetPage(
        eyebrow = stepEyebrow(AiWordImportStep.TOPICS),
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
        error?.let { ErrorMessage(friendlyGenerationError(it)) }
    }
}

private fun friendlyGenerationError(error: String): String {
    val isNetworkError = listOf("timeout", "connect", "network", "internet")
        .any { error.contains(it, ignoreCase = true) }
    return when {
        isNetworkError -> "You're offline -- check your connection and try again."
        error.contains("limit", ignoreCase = true) || error.contains("quota", ignoreCase = true) -> error
        else -> "Generation failed -- please try again or pick different topics."
    }
}

private val GeneratingSteps = listOf(
    Res.string.generating_step_1,
    Res.string.generating_step_2,
    Res.string.generating_step_3,
    Res.string.generating_step_4,
)

/**
 * Shown while suggestions are generated. The checklist advances on a timer (the request has no
 * progress signal) and holds on the last step until the result arrives.
 */
@Composable
internal fun AiGeneratingContent(state: AiWordImportUiState) {
    var activeIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (activeIndex < GeneratingSteps.lastIndex) {
            delay(GeneratingStepMillis)
            activeIndex++
        }
    }
    val summary = listOfNotNull(
        state.selectedTargetLanguage,
        state.selectedLevel?.replaceFirstChar { it.uppercase() },
        state.selectedTopics.takeIf { it.isNotEmpty() }?.joinToString(", "),
    ).joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.xl, vertical = Theme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
    ) {
        Box(modifier = Modifier.size(GeneratingRingSize), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(GeneratingRingSize),
                strokeWidth = Theme.spacing.xs,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Box(
                modifier = Modifier
                    .size(Theme.dimensions.iconSizeMassive + Theme.spacing.xxs)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Theme.dimensions.iconSizeXLarge),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            Text(
                stringResource(Res.string.generating_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (summary.isNotEmpty()) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
        ) {
            GeneratingSteps.forEachIndexed { index, label ->
                GeneratingStepRow(
                    text = stringResource(label),
                    done = index < activeIndex,
                    active = index == activeIndex,
                )
            }
        }
    }
}

@Composable
private fun GeneratingStepRow(text: String, done: Boolean, active: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.outlineVariant
    val labelColor = if (done || active) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Theme.spacing.xs + Theme.spacing.xxxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(Theme.dimensions.iconSize)
                .clip(CircleShape)
                .then(
                    when {
                        done -> Modifier.background(accent)
                        active -> Modifier.border(Theme.spacing.xxxs, accent, CircleShape)
                        else -> Modifier.border(Theme.spacing.xxxs, idle, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                done -> Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Theme.dimensions.iconSizeSmall - Theme.spacing.xxxs),
                )
                active -> Box(Modifier.size(Theme.spacing.xs).clip(CircleShape).background(accent))
            }
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = labelColor,
        )
    }
}
