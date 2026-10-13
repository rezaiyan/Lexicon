package presentation.ui.components.imports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import components.BrandFilterChip
import components.SnapStepPicker
import components.CounterPill
import components.sheet.IconTile
import components.sheet.SheetSectionLabel
import components.LabeledSwitch
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


/** One fixed, free option. [subtitle] and [icon] are shown where options are rows (toggles). */
@Immutable
internal data class QuickPick(
    val label: String,
    val selected: Boolean,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)

/** Quick picks shown together; the kind decides the control, so its rule reads at a glance. */
@Immutable
internal sealed interface QuickPickGroup {
    val picks: List<QuickPick>

    /** Any number apply: one scrolling row of chips. */
    data class Chips(val title: String, override val picks: List<QuickPick>) : QuickPickGroup

    /** Exactly one of a few ordered steps (level, tone): a snapping track, never empty. */
    data class Steps(val title: String, override val picks: List<QuickPick>) : QuickPickGroup

    /** Independent on/off options: switch rows that explain themselves. */
    data class Toggles(override val picks: List<QuickPick>) : QuickPickGroup
}

/** Heading of a panel that starts collapsed (photo); the suggestions page shows the panel open. */
@Immutable
internal data class GuidanceHeader(val title: String, val subtitle: String)

/**
 * Lets the learner steer the AI, each way easier than a blank box:
 * - **their own words** in a prompt card, with sentence starters to tap
 * - **fine-tuning**: free, fixed options; each kind uses the control that fits it
 *
 * A refusal shows inside the prompt card with the learner's text kept, so they can fix it in place.
 *
 * @param header set for a collapsible panel (photos); null shows the content open, without a frame.
 * @param ownWordsFirst the suggestions page leads with the description; photos with the options.
 * @param afterOwnWords shown right under the prompt card when it leads (the topics, another way to
 *   say what to learn), so the fine-tuning comes last.
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
    afterOwnWords: (@Composable () -> Unit)? = null,
) {
    val content = @Composable {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg)) {
            val ownWords = @Composable {
                PromptCard(
                    guidance, placeholder, starters, surcharge, enabled, actions, onContinueWithout,
                    // Inside the photo panel's card it stays flat: one raised surface at a time
                    raised = header == null,
                )
            }
            if (ownWordsFirst) {
                ownWords()
                afterOwnWords?.invoke()
            }
            // On the page it is a card like the prompt; inside the photo panel's card it stays flat
            FineTuneSection(quickPicks, enabled, framed = header == null)
            if (!ownWordsFirst) ownWords()
        }
    }
    if (header == null) {
        Box(modifier.fillMaxWidth()) { content() }
        return
    }

    val hasContent = guidance.text.isNotBlank() || !guidance.focus.isEmpty
    var expanded by rememberSaveable { mutableStateOf(hasContent) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.shapes.medium),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Theme.elevation.low,
    ) {
        // No padding here: the header's touch target and ripple span the card edge to edge
        Column(modifier = Modifier.animateContentSize()) {
            val activeCount = quickPicks.sumOf { group -> group.picks.count(QuickPick::selected) } +
                if (guidance.isGuided) 1 else 0
            CollapsibleHeader(header, activeCount, expanded, onToggle = { expanded = !expanded })
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(Theme.motion.durationMedium)) + expandVertically(),
                exit = fadeOut(tween(Theme.motion.durationShort)) + shrinkVertically(),
            ) {
                Box(
                    Modifier.padding(
                        start = Theme.spacing.cardPadding,
                        end = Theme.spacing.cardPadding,
                        bottom = Theme.spacing.cardPadding,
                    ),
                ) { content() }
            }
        }
    }
}

@Composable
private fun CollapsibleHeader(header: GuidanceHeader, activeCount: Int, expanded: Boolean, onToggle: () -> Unit) {
    val chevron by animateFloatAsState(if (expanded) HALF_TURN_DEGREES else 0f)
    val toggleLabel = stringResource(if (expanded) Res.string.ai_guidance_collapse else Res.string.ai_guidance_expand)
    // Clickable before padding: the whole strip (full width, at least a touch target tall) reacts
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Theme.dimensions.touchTarget)
            .clickable(onClickLabel = toggleLabel, role = Role.Button, onClick = onToggle)
            .padding(Theme.spacing.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        IconTile(icon = Icons.Default.AutoAwesome, tinted = true)
        Column(modifier = Modifier.weight(1f)) {
            Text(header.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                header.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (activeCount > 0) {
            CounterPill(text = stringResource(Res.string.ai_guidance_active, activeCount))
        }
        Icon(
            Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(chevron),
        )
    }
}

/** The one accent on the page besides the CTA: a premium-gradient tile marking where to talk to the AI. */
@Composable
private fun SparkleBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Theme.dimensions.iconSizeXLarge)
            .clip(RoundedCornerShape(Theme.shapes.small))
            .background(Theme.gradients.premiumHero),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
        )
    }
}

/**
 * The learner's own words: a card whose quiet outline turns into the premium gradient once there is
 * text, a borderless field, and starters in one scrolling row so the card never grows with them.
 */
@Composable
private fun PromptCard(
    guidance: GuidanceDraft,
    placeholder: String,
    starters: List<String>,
    surcharge: Int,
    enabled: Boolean,
    actions: GuidanceActions,
    onContinueWithout: () -> Unit,
    raised: Boolean,
) {
    val canWrite = enabled && guidance.canWrite
    val focusRequester = remember { FocusRequester() }
    val edge = if (guidance.text.isNotEmpty()) {
        BorderStroke(Theme.dimensions.borderWidth, Theme.gradients.premiumHero)
    } else {
        BorderStroke(Theme.dimensions.borderWidth, MaterialTheme.colorScheme.outlineVariant)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.shapes.medium),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (raised) Theme.elevation.low else Theme.elevation.none,
        border = edge,
    ) {
        Column(
            modifier = Modifier.padding(Theme.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            PromptHeader(guidance.text, canWrite, actions.onClear)
            InstructionField(
                text = guidance.text,
                placeholder = placeholder,
                enabled = canWrite,
                focusRequester = focusRequester,
                onTextChange = actions.onTextChange,
            )
            if (canWrite && starters.isNotEmpty()) {
                StarterRow(starters) { starter ->
                    actions.onStarter(starter)
                    focusRequester.requestFocus()
                }
            }
            if (guidance.isGuided && surcharge > 0) {
                Text(
                    stringResource(Res.string.ai_guidance_extra_cost, creditsText(surcharge)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            guidance.problem?.let { problem ->
                InstructionProblemNote(
                    problem,
                    onContinueWithout = onContinueWithout.takeIf { problem == InstructionProblem.Unavailable },
                )
            }
        }
    }
}

/** Title on the left; once there is text, its length and a clear button on the right. */
@Composable
private fun PromptHeader(text: String, canClear: Boolean, onClear: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        SparkleBadge()
        Text(
            stringResource(Res.string.ai_guidance_own_words),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (text.isNotEmpty()) {
            Text(
                stringResource(Res.string.ai_guidance_counter, text.length, AiInstruction.MAX_LENGTH),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (canClear) {
                IconButton(onClick = onClear, modifier = Modifier.size(Theme.dimensions.touchTarget)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(Res.string.ai_guidance_clear),
                        modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                    )
                }
            }
        }
    }
}

/** Sentence starters as soft pills in one row; tapping one starts (or continues) the sentence. */
@Composable
private fun StarterRow(starters: List<String>, onStarter: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        starters.forEach { starter ->
            Surface(
                onClick = { onStarter(starter) },
                shape = RoundedCornerShape(Theme.shapes.pill),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
                    )
                    Text("$starter …", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
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
    focusRequester: FocusRequester,
    onTextChange: (String) -> Unit,
) {
    var editing by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    val shown = if (editing.text == text) editing else TextFieldValue(text, TextRange(text.length))
    val focusManager = LocalFocusManager.current
    val ownWords = stringResource(Res.string.ai_guidance_own_words)
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
    )

    BasicTextField(
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
        textStyle = textStyle,
        minLines = 2,
        maxLines = MAX_FIELD_LINES,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        decorationBox = { field ->
            Box {
                if (text.isEmpty()) {
                    Text(placeholder, style = textStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                field()
            }
        },
    )
}

/** Free options under one label, one row per group, split by hairlines so each reads on its own. */
@Composable
private fun FineTuneSection(groups: List<QuickPickGroup>, enabled: Boolean, framed: Boolean) {
    if (groups.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val rows = @Composable {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
            groups.forEachIndexed { index, group ->
                if (index > 0) {
                    HorizontalDivider(thickness = Theme.dimensions.hairlineThickness, color = colors.outlineVariant)
                }
                FineTuneGroup(group, enabled)
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
        SheetSectionLabel(stringResource(Res.string.ai_guidance_quick_picks))
        if (framed) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Theme.shapes.medium),
                color = colors.surface,
                shadowElevation = Theme.elevation.low,
                border = BorderStroke(Theme.dimensions.borderWidth, colors.outlineVariant),
            ) {
                Box(Modifier.padding(Theme.spacing.cardPadding)) { rows() }
            }
        } else {
            rows()
        }
    }
}

/** Each kind of group draws the control its rule needs. */
@Composable
private fun FineTuneGroup(group: QuickPickGroup, enabled: Boolean) {
    when (group) {
        is QuickPickGroup.Chips -> GroupWithTitle(group.title) {
            QuickPickFlow(group.picks) { pick ->
                BrandFilterChip(label = pick.label, selected = pick.selected, onClick = pick.onClick, enabled = enabled)
            }
        }
        is QuickPickGroup.Steps -> {
            val selected = group.picks.indexOfFirst(QuickPick::selected).takeIf { it >= 0 }
            GroupWithTitle(group.title) {
                SnapStepPicker(
                    labels = group.picks.map(QuickPick::label),
                    selectedIndex = selected,
                    onSelect = { index -> group.picks[index].onClick() },
                    enabled = enabled,
                )
            }
        }
        is QuickPickGroup.Toggles -> Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
            group.picks.forEach { pick ->
                LabeledSwitch(
                    title = pick.label,
                    subtitle = pick.subtitle,
                    checked = pick.selected,
                    onCheckedChange = { pick.onClick() },
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun GroupWithTitle(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}

/** Options that wrap onto a second line rather than scrolling out of sight. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> QuickPickFlow(items: List<T>, item: @Composable (T) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        items.forEach { item(it) }
    }
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
