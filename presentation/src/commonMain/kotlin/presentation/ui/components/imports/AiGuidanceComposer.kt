package presentation.ui.components.imports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import components.sheet.SheetSectionLabel
import domain.ai.model.AiInstruction
import feature.addwords.model.GuidanceDraft
import feature.addwords.model.InstructionProblem
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.ai_guidance_active
import lexicon.resources.generated.resources.ai_guidance_clear
import lexicon.resources.generated.resources.ai_guidance_collapse
import lexicon.resources.generated.resources.ai_guidance_counter
import lexicon.resources.generated.resources.ai_guidance_expand
import lexicon.resources.generated.resources.ai_guidance_extra_cost
import lexicon.resources.generated.resources.ai_guidance_own_words
import lexicon.resources.generated.resources.ai_guidance_quick_picks
import lexicon.resources.generated.resources.ai_guidance_start_with
import lexicon.resources.generated.resources.ai_instructions_continue_without
import lexicon.resources.generated.resources.ai_instructions_not_charged
import lexicon.resources.generated.resources.ai_instructions_off_topic
import lexicon.resources.generated.resources.ai_instructions_paused
import lexicon.resources.generated.resources.ai_instructions_personal_data
import lexicon.resources.generated.resources.ai_instructions_unavailable
import lexicon.resources.generated.resources.ai_instructions_unsafe
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private const val HALF_TURN_DEGREES = 180f
private const val MAX_FIELD_LINES = 4

/** One quick-pick chip: a fixed, free option. */
@Immutable
internal data class QuickPick(val label: String, val selected: Boolean, val onClick: () -> Unit)

/**
 * Quick picks shown under one heading. [singleChoice] groups (tone) read as radio buttons to
 * assistive tech, so the "one at most" rule is announced, not discovered.
 */
@Immutable
internal data class QuickPickGroup(val title: String, val picks: List<QuickPick>, val singleChoice: Boolean = false)

/** Heading of a panel that starts collapsed (photo); the suggestions page shows the panel open. */
@Immutable
internal data class GuidanceHeader(val title: String, val subtitle: String)

/**
 * Lets the learner steer the AI three ways, each easier than a blank box:
 * - **quick picks**: free, fixed options, grouped (which words, kinds, tone, extras)
 * - **starters**: tap "I'm preparing for" to begin a sentence, then finish it
 * - **own words**: free text, screened server-side and priced at [surcharge] more credits
 *
 * A refusal shows inline with the learner's text kept, so they can fix it in place.
 *
 * @param header set for a collapsible panel; null shows it open, without a heading.
 * @param ownWordsFirst the suggestions page leads with the description; photos with the picks.
 * @param onContinueWithout offered when the words couldn't be checked: clears them and retries.
 */
@Composable
internal fun AiGuidanceComposer(
    guidance: GuidanceDraft,
    placeholder: String,
    starters: List<String>,
    quickPicks: List<QuickPickGroup>,
    surcharge: Int,
    enabled: Boolean,
    actions: GuidanceActions,
    onContinueWithout: () -> Unit,
    modifier: Modifier = Modifier,
    header: GuidanceHeader? = null,
    ownWordsFirst: Boolean = true,
) {
    val hasContent = guidance.text.isNotBlank() || !guidance.focus.isEmpty
    var expanded by rememberSaveable { mutableStateOf(header == null || hasContent) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(MaterialTheme.colorScheme.surface)
            .animateContentSize()
            .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        header?.let {
            val activeCount = quickPicks.sumOf { group -> group.picks.count(QuickPick::selected) } +
                if (guidance.isGuided) 1 else 0
            CollapsibleHeader(it, activeCount, expanded, onToggle = { expanded = !expanded })
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(Theme.motion.durationMedium)) + expandVertically(),
            exit = fadeOut(tween(Theme.motion.durationShort)) + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
                val ownWords = @Composable {
                    OwnWordsSection(guidance, placeholder, starters, surcharge, enabled, actions, onContinueWithout)
                }
                if (ownWordsFirst) ownWords()
                QuickPicksSection(quickPicks, enabled)
                if (!ownWordsFirst) ownWords()
            }
        }
    }
}

@Composable
private fun CollapsibleHeader(header: GuidanceHeader, activeCount: Int, expanded: Boolean, onToggle: () -> Unit) {
    val chevron by animateFloatAsState(if (expanded) HALF_TURN_DEGREES else 0f)
    val toggleLabel = stringResource(if (expanded) Res.string.ai_guidance_collapse else Res.string.ai_guidance_expand)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.medium))
            .clickable(onClickLabel = toggleLabel, onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Theme.dimensions.iconSizeSmall + Theme.spacing.xxxs),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(header.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                if (activeCount > 0) stringResource(Res.string.ai_guidance_active, activeCount) else header.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(chevron),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickPicksSection(groups: List<QuickPickGroup>, enabled: Boolean) {
    if (groups.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
        SheetSectionLabel(stringResource(Res.string.ai_guidance_quick_picks))
        groups.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)) {
                Text(
                    group.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
                ) {
                    group.picks.forEach { pick -> QuickPickChip(pick, enabled, group.singleChoice) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OwnWordsSection(
    guidance: GuidanceDraft,
    placeholder: String,
    starters: List<String>,
    surcharge: Int,
    enabled: Boolean,
    actions: GuidanceActions,
    onContinueWithout: () -> Unit,
) {
    val canWrite = enabled && guidance.canWrite
    val focusRequester = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
        SheetSectionLabel(stringResource(Res.string.ai_guidance_own_words))
        InstructionField(
            text = guidance.text,
            placeholder = placeholder,
            enabled = canWrite,
            isError = guidance.problem?.isRefusal == true,
            focusRequester = focusRequester,
            onTextChange = actions.onTextChange,
            onClear = actions.onClear,
        )
        FieldFooter(guidance, surcharge)
        guidance.problem?.let { problem ->
            InstructionProblemNote(
                problem,
                onContinueWithout = onContinueWithout.takeIf { problem == InstructionProblem.Unavailable },
            )
        }
        if (canWrite && starters.isNotEmpty()) {
            SheetSectionLabel(stringResource(Res.string.ai_guidance_start_with))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
            ) {
                starters.forEach { starter ->
                    SuggestionChip(
                        onClick = {
                            actions.onStarter(starter)
                            focusRequester.requestFocus()
                        },
                        label = { Text("$starter …", style = MaterialTheme.typography.labelLarge) },
                        shape = RoundedCornerShape(Theme.shapes.pill),
                    )
                }
            }
        }
    }
}

/** What the words cost on the left (only once there are some), the length on the right. */
@Composable
private fun FieldFooter(guidance: GuidanceDraft, surcharge: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (guidance.isGuided && surcharge > 0) {
                stringResource(Res.string.ai_guidance_extra_cost, creditsText(surcharge))
            } else {
                ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (guidance.text.isNotEmpty()) {
            Text(
                stringResource(Res.string.ai_guidance_counter, guidance.text.length, AiInstruction.MAX_LENGTH),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/**
 * The ViewModel owns [text]; the field keeps only the cursor. When [text] changes from outside
 * (a starter, clear) the cursor goes to the end, ready to finish the sentence.
 */
@Composable
private fun InstructionField(
    text: String,
    placeholder: String,
    enabled: Boolean,
    isError: Boolean,
    focusRequester: FocusRequester,
    onTextChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    var editing by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    val shown = if (editing.text == text) editing else TextFieldValue(text, TextRange(text.length))
    val focusManager = LocalFocusManager.current
    val outline = MaterialTheme.colorScheme.outlineVariant
    val container = MaterialTheme.colorScheme.surfaceContainerLow
    val ownWords = stringResource(Res.string.ai_guidance_own_words)

    OutlinedTextField(
        value = shown,
        onValueChange = { value ->
            editing = value.copy(text = value.text.take(AiInstruction.MAX_LENGTH))
            if (editing.text != text) onTextChange(editing.text)
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .semantics { contentDescription = ownWords },
        enabled = enabled,
        isError = isError,
        minLines = 2,
        maxLines = MAX_FIELD_LINES,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingIcon = if (text.isNotEmpty() && enabled) {
            {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.ai_guidance_clear))
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(Theme.shapes.large - Theme.spacing.xxxs),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = outline,
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            disabledContainerColor = container,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
    )
}

@Composable
private fun QuickPickChip(pick: QuickPick, enabled: Boolean, singleChoice: Boolean) {
    FilterChip(
        selected = pick.selected,
        onClick = pick.onClick,
        enabled = enabled,
        label = { Text(pick.label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = if (pick.selected) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
        shape = RoundedCornerShape(Theme.shapes.pill),
        modifier = if (singleChoice) Modifier.semantics { role = Role.RadioButton } else Modifier,
    )
}

/** Refusals are the learner's to fix; a pause or an outage is not about what they wrote. */
private val InstructionProblem.isRefusal: Boolean
    get() = this == InstructionProblem.OffTopic || this == InstructionProblem.Unsafe ||
        this == InstructionProblem.PersonalData

/** Why the words weren't used, that nothing was charged, and (when they couldn't be checked) a way on. */
@Composable
private fun InstructionProblemNote(problem: InstructionProblem, onContinueWithout: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val container = if (problem.isRefusal) colors.errorContainer else colors.surfaceContainerHigh
    val content = if (problem.isRefusal) colors.onErrorContainer else colors.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.medium))
            .background(container)
            .semantics { liveRegion = LiveRegionMode.Assertive }
            .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
            )
            Text(problem.message(), style = MaterialTheme.typography.bodyMedium, color = content)
        }
        if (problem != InstructionProblem.Paused) {
            Text(
                stringResource(Res.string.ai_instructions_not_charged),
                style = MaterialTheme.typography.bodySmall,
                color = content,
            )
        }
        onContinueWithout?.let {
            TextButton(onClick = it) { Text(stringResource(Res.string.ai_instructions_continue_without)) }
        }
    }
}

@Composable
private fun InstructionProblem.message(): String = stringResource(
    when (this) {
        InstructionProblem.OffTopic -> Res.string.ai_instructions_off_topic
        InstructionProblem.Unsafe -> Res.string.ai_instructions_unsafe
        InstructionProblem.PersonalData -> Res.string.ai_instructions_personal_data
        InstructionProblem.Paused -> Res.string.ai_instructions_paused
        InstructionProblem.Unavailable -> Res.string.ai_instructions_unavailable
    },
)
