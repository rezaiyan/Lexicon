package feature.study.ui.listening

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import components.sheet.SheetGroup
import components.sheet.SheetPrimaryButton
import components.sheet.SheetRadioRow
import components.sheet.SheetSectionLabel
import components.sheet.SheetSwitchRow
import domain.listening.model.ListeningSelection
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
import lexicon.resources.generated.resources.listening_estimate
import lexicon.resources.generated.resources.listening_limit_all
import lexicon.resources.generated.resources.listening_nothing_due
import lexicon.resources.generated.resources.listening_setup_how_many
import lexicon.resources.generated.resources.listening_setup_intro
import lexicon.resources.generated.resources.listening_setup_playback
import lexicon.resources.generated.resources.listening_setup_words
import lexicon.resources.generated.resources.listening_shuffle
import lexicon.resources.generated.resources.listening_shuffle_subtitle
import lexicon.resources.generated.resources.listening_source_all
import lexicon.resources.generated.resources.listening_source_due
import lexicon.resources.generated.resources.listening_source_level
import lexicon.resources.generated.resources.listening_source_tag
import lexicon.resources.generated.resources.listening_start
import lexicon.resources.generated.resources.listening_word_count
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private const val MS_PER_MINUTE = 60_000L

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
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            Text(
                text = stringResource(Res.string.listening_setup_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SetupSection(stringResource(Res.string.listening_setup_words)) {
                SourceRows(state, selection, actions)
            }
            if (state.options.levels.isNotEmpty()) {
                SetupSection(stringResource(Res.string.listening_source_level)) {
                    ChipFlow {
                        state.options.levels.forEach { level ->
                            val source = ListeningSource.Level(level.stage)
                            OptionChip(
                                text = "${stringResource(level.stage.label)} · ${level.count}",
                                selected = selection.source == source,
                                onClick = { actions.onSourceSelected(source) },
                            )
                        }
                    }
                }
            }
            if (state.options.tags.isNotEmpty()) {
                SetupSection(stringResource(Res.string.listening_source_tag)) {
                    ChipFlow {
                        state.options.tags.forEach { tag ->
                            val source = ListeningSource.Tag(tag.tagId)
                            OptionChip(
                                text = "${tag.name} · ${tag.count}",
                                selected = selection.source == source,
                                onClick = { actions.onSourceSelected(source) },
                            )
                        }
                    }
                }
            }

            SetupSection(stringResource(Res.string.listening_setup_how_many)) {
                LimitChips(selection, available, actions)
                SheetGroup {
                    SheetSwitchRow(
                        title = stringResource(Res.string.listening_shuffle),
                        subtitle = stringResource(Res.string.listening_shuffle_subtitle),
                        icon = Icons.Rounded.Shuffle,
                        checked = selection.shuffle,
                        onCheckedChange = actions.onShuffleChanged,
                        showDivider = false,
                    )
                }
            }

            SetupSection(stringResource(Res.string.listening_setup_playback)) {
                ListeningSettingsPanel(
                    settings = state.settings,
                    speechRate = state.speechRate,
                    actions = actions,
                )
            }
        }

        SetupFooter(state, actions)
    }
}

@Composable
private fun SourceRows(state: ListeningState, selection: ListeningSelection, actions: ListeningActions) {
    val dueCount = state.options.dueCount
    SheetGroup {
        SheetRadioRow(
            title = stringResource(Res.string.listening_source_due),
            subtitle = if (dueCount > 0) {
                stringResource(Res.string.listening_word_count, dueCount)
            } else {
                stringResource(Res.string.listening_nothing_due)
            },
            icon = Icons.Rounded.CalendarToday,
            selected = selection.source == ListeningSource.Due,
            // Nothing due: the row stays visible so the option is discoverable, but cannot be picked.
            onClick = { if (dueCount > 0) actions.onSourceSelected(ListeningSource.Due) },
            showDivider = true,
        )
        SheetRadioRow(
            title = stringResource(Res.string.listening_source_all),
            subtitle = stringResource(Res.string.listening_word_count, state.options.allCount),
            icon = Icons.AutoMirrored.Rounded.LibraryBooks,
            selected = selection.source == ListeningSource.All,
            onClick = { actions.onSourceSelected(ListeningSource.All) },
            showDivider = false,
        )
    }
}

/** Preset sizes smaller than what is available, then "All (n)"; the cap that would play everything reads as All. */
@Composable
private fun LimitChips(selection: ListeningSelection, available: Int, actions: ListeningActions) {
    val playsAll = selection.sessionSize(available) == available
    ChipFlow {
        ListeningSelection.LIMIT_OPTIONS.filter { it < available }.forEach { limit ->
            OptionChip(
                text = limit.toString(),
                selected = !playsAll && selection.limit == limit,
                onClick = { actions.onLimitSelected(limit) },
            )
        }
        OptionChip(
            text = stringResource(Res.string.listening_limit_all, available),
            selected = playsAll,
            onClick = { actions.onLimitSelected(ListeningSelection.LIMIT_ALL) },
        )
    }
}

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
        Text(
            text = stringResource(Res.string.listening_estimate, minutes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        SheetPrimaryButton(
            text = stringResource(Res.string.listening_start, size),
            onClick = actions.onStart,
            enabled = size > 0,
            icon = Icons.Rounded.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SetupSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Theme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        SheetSectionLabel(title)
        content()
    }
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
