package feature.study.ui.listening

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import components.sheet.SheetGroup
import components.sheet.SheetPrimaryButton
import components.sheet.SheetSectionLabel
import domain.listening.model.ListeningOptions
import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningSelection
import domain.listening.model.ListeningSettings
import domain.listening.model.ListeningSource
import domain.word.model.LearningStage
import feature.study.listening.ListeningState
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.level_0_fresh
import lexicon.resources.generated.resources.level_1_learning
import lexicon.resources.generated.resources.level_2_familiar
import lexicon.resources.generated.resources.level_3_building
import lexicon.resources.generated.resources.level_4_almost
import lexicon.resources.generated.resources.level_5_strong
import lexicon.resources.generated.resources.level_6_mastered
import lexicon.resources.generated.resources.listening_all_summary
import lexicon.resources.generated.resources.listening_due_summary
import lexicon.resources.generated.resources.listening_estimate
import lexicon.resources.generated.resources.listening_limit_all
import lexicon.resources.generated.resources.listening_order_translation_first
import lexicon.resources.generated.resources.listening_order_word_first
import lexicon.resources.generated.resources.listening_pause_summary
import lexicon.resources.generated.resources.listening_setup_how_many
import lexicon.resources.generated.resources.listening_setup_intro
import lexicon.resources.generated.resources.listening_setup_playback
import lexicon.resources.generated.resources.listening_setup_words
import lexicon.resources.generated.resources.listening_shuffle
import lexicon.resources.generated.resources.listening_source_all
import lexicon.resources.generated.resources.listening_source_due
import lexicon.resources.generated.resources.listening_source_level
import lexicon.resources.generated.resources.listening_source_tag
import lexicon.resources.generated.resources.listening_start
import lexicon.resources.generated.resources.listening_times
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private const val MS_PER_MINUTE = 60_000L
private const val MS_PER_SECOND = 1_000L
private const val CHEVRON_EXPANDED_DEGREES = 180f

/** The four kinds of source, one segment each; the specific level or tag is picked with chips below. */
private enum class SourceKind(val label: StringResource) {
    DUE(Res.string.listening_source_due),
    ALL(Res.string.listening_source_all),
    LEVEL(Res.string.listening_source_level),
    TAG(Res.string.listening_source_tag),
}

private val ListeningSource.kind: SourceKind
    get() = when (this) {
        ListeningSource.Due -> SourceKind.DUE
        ListeningSource.All -> SourceKind.ALL
        is ListeningSource.Level -> SourceKind.LEVEL
        is ListeningSource.Tag -> SourceKind.TAG
    }

/** Pre-session step: which words, how many, and how they are played. */
@Composable
internal fun SetupContent(state: ListeningState, actions: ListeningActions) {
    val selection = state.selection
    val available = state.options.count(selection.source)
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Theme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
        ) {
            Text(
                text = stringResource(Res.string.listening_setup_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            SetupSection(stringResource(Res.string.listening_setup_words)) {
                SourcePicker(state.options, selection.source, actions.onSourceSelected)
            }
            SetupSection(
                title = stringResource(Res.string.listening_setup_how_many),
                trailing = {
                    ShuffleToggle(checked = selection.shuffle, onCheckedChange = actions.onShuffleChanged)
                },
            ) {
                LimitPicker(selection, available, actions.onLimitSelected)
            }
            PlaybackSection(state.settings, state.speechRate, actions)
        }

        SetupFooter(state, actions)
    }
}

// ---------------------------------------------------------------------------
// Words
// ---------------------------------------------------------------------------

@Composable
private fun SourcePicker(
    options: ListeningOptions,
    source: ListeningSource,
    onSourceSelected: (ListeningSource) -> Unit,
) {
    val kinds = SourceKind.entries.filter { kind ->
        when (kind) {
            SourceKind.DUE, SourceKind.ALL -> true
            SourceKind.LEVEL -> options.levels.isNotEmpty()
            SourceKind.TAG -> options.tags.isNotEmpty()
        }
    }
    BrandSegmentedRow(
        labels = kinds.map { stringResource(it.label) },
        selectedIndex = kinds.indexOf(source.kind),
        enabled = { index -> kinds[index] != SourceKind.DUE || options.dueCount > 0 },
        onSelect = { index ->
            // Switching kind lands on its first option (levels lowest first, tags alphabetical).
            when (kinds[index]) {
                SourceKind.DUE -> onSourceSelected(ListeningSource.Due)
                SourceKind.ALL -> onSourceSelected(ListeningSource.All)
                SourceKind.LEVEL -> if (source !is ListeningSource.Level) {
                    options.levels.firstOrNull()?.let { onSourceSelected(ListeningSource.Level(it.stage)) }
                }
                SourceKind.TAG -> if (source !is ListeningSource.Tag) {
                    options.tags.firstOrNull()?.let { onSourceSelected(ListeningSource.Tag(it.tagId)) }
                }
            }
        },
    )

    when (source) {
        ListeningSource.Due -> SourceHint(stringResource(Res.string.listening_due_summary, options.dueCount))
        ListeningSource.All -> SourceHint(stringResource(Res.string.listening_all_summary, options.allCount))
        is ListeningSource.Level -> ChipFlow {
            options.levels.forEach { level ->
                CountChip(
                    text = stringResource(level.stage.label),
                    count = level.count,
                    selected = source.stage == level.stage,
                    onClick = { onSourceSelected(ListeningSource.Level(level.stage)) },
                )
            }
        }
        is ListeningSource.Tag -> ChipFlow {
            options.tags.forEach { tag ->
                CountChip(
                    text = tag.name,
                    count = tag.count,
                    selected = source.tagId == tag.tagId,
                    onClick = { onSourceSelected(ListeningSource.Tag(tag.tagId)) },
                )
            }
        }
    }
}

@Composable
private fun SourceHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Theme.spacing.xxs),
    )
}

// ---------------------------------------------------------------------------
// How many
// ---------------------------------------------------------------------------

/** Preset sizes below what is available, then "All (n)"; a cap that plays everything reads as All. */
@Composable
private fun LimitPicker(selection: ListeningSelection, available: Int, onLimitSelected: (Int) -> Unit) {
    val presets = ListeningSelection.LIMIT_OPTIONS.filter { it < available }
    val playsAll = selection.sessionSize(available) == available
    val labels = presets.map(Int::toString) + stringResource(Res.string.listening_limit_all, available)
    BrandSegmentedRow(
        labels = labels,
        selectedIndex = if (playsAll) presets.size else presets.indexOf(selection.limit),
        onSelect = { index -> onLimitSelected(presets.getOrNull(index) ?: ListeningSelection.LIMIT_ALL) },
    )
}

@Composable
private fun ShuffleToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    FilterChip(
        selected = checked,
        onClick = { onCheckedChange(!checked) },
        label = { Text(stringResource(Res.string.listening_shuffle)) },
        leadingIcon = {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        shape = RoundedCornerShape(Theme.shapes.pill),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = primary.copy(alpha = Theme.opacity.focus),
            selectedLabelColor = primary,
            selectedLeadingIconColor = primary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = checked,
            selectedBorderColor = primary,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

// ---------------------------------------------------------------------------
// Playback
// ---------------------------------------------------------------------------

/** Collapsed to a one-line summary: these are set once and rarely touched again. */
@Composable
private fun PlaybackSection(settings: ListeningSettings, speechRate: Float, actions: ListeningActions) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (expanded) CHEVRON_EXPANDED_DEGREES else 0f)
    SheetGroup {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.listening_setup_playback),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = playbackSummary(settings, speechRate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(chevronRotation),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            ListeningSettingsRows(
                settings = settings,
                speechRate = speechRate,
                actions = actions,
                modifier = Modifier
                    .padding(horizontal = Theme.spacing.sm)
                    .padding(bottom = Theme.spacing.sm),
            )
        }
    }
}

@Composable
private fun playbackSummary(settings: ListeningSettings, speechRate: Float): String = listOf(
    stringResource(Res.string.listening_pause_summary, (settings.pauseMs / MS_PER_SECOND).toInt()),
    stringResource(
        when (settings.order) {
            ListeningOrder.WORD_FIRST -> Res.string.listening_order_word_first
            ListeningOrder.TRANSLATION_FIRST -> Res.string.listening_order_translation_first
        }
    ),
    stringResource(Res.string.listening_times, settings.repeatCount),
    "${speechRate}×",
).joinToString(" · ")

// ---------------------------------------------------------------------------
// Footer
// ---------------------------------------------------------------------------

@Composable
private fun SetupFooter(state: ListeningState, actions: ListeningActions) {
    val size = state.sessionSize
    val minutes = ((state.settings.estimatedDurationMs(size) + MS_PER_MINUTE / 2) / MS_PER_MINUTE)
        .toInt()
        .coerceAtLeast(1)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SheetPrimaryButton(
            text = stringResource(Res.string.listening_start, size),
            onClick = actions.onStart,
            enabled = size > 0,
            icon = Icons.Rounded.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(Res.string.listening_estimate, minutes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Building blocks
// ---------------------------------------------------------------------------

@Composable
private fun SetupSection(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SheetSectionLabel(title, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        content()
    }
}

/** Equal-width single choice in brand colours (the default active colour is the green secondary). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrandSegmentedRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    enabled: (Int) -> Boolean = { true },
) {
    val primary = MaterialTheme.colorScheme.primary
    val colors = SegmentedButtonDefaults.colors(
        activeContainerColor = primary.copy(alpha = Theme.opacity.focus),
        activeContentColor = primary,
        activeBorderColor = primary,
        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            SegmentedButton(
                selected = selected,
                onClick = { onSelect(index) },
                enabled = enabled(index),
                shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
                colors = colors,
                icon = {},
                label = {
                    Text(
                        text = label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun CountChip(text: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    OptionChip(text = "$text  $count", selected = selected, onClick = onClick)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
    ) {
        content()
    }
}

private val LearningStage.label: StringResource
    get() = when (this) {
        LearningStage.LEVEL_0_FRESH -> Res.string.level_0_fresh
        LearningStage.LEVEL_1_LEARNING -> Res.string.level_1_learning
        LearningStage.LEVEL_2_FAMILIAR -> Res.string.level_2_familiar
        LearningStage.LEVEL_3_BUILDING -> Res.string.level_3_building
        LearningStage.LEVEL_4_ALMOST -> Res.string.level_4_almost
        LearningStage.LEVEL_5_STRONG -> Res.string.level_5_strong
        LearningStage.LEVEL_6_MASTERED -> Res.string.level_6_mastered
    }
