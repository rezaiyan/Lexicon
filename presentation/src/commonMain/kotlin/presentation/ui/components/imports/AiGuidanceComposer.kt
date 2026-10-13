package presentation.ui.components.imports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
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
 * Lets the learner steer the AI three ways, each easier than a blank box:
 * - **quick picks**: free, fixed options (word types, register, …)
 * - **starters**: tap "I'm preparing for" to begin a sentence, then finish it
 * - **own words**: free text, screened server-side and priced at [surcharge] more credits
 *
 * A refusal shows inline with the learner's text kept, so they can fix it in place.
 *
 * @param title shown only when [collapsible]: the row that opens the panel.
 * @param onContinueWithout offered when the words couldn't be checked: clears them and retries.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AiGuidanceComposer(
    guidance: GuidanceDraft,
    placeholder: String,
    starters: List<String>,
    quickPicks: List<QuickPick>,
    surcharge: Int,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onStarter: (String) -> Unit,
    onClear: () -> Unit,
    onContinueWithout: () -> Unit,
    modifier: Modifier = Modifier,
    collapsible: Boolean = false,
    title: String? = null,
    subtitle: String? = null,
    ownWordsFirst: Boolean = true,
) {
    val hasContent = guidance.text.isNotBlank() || !guidance.focus.isEmpty
    var expanded by rememberSaveable { mutableStateOf(!collapsible || hasContent) }
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(MaterialTheme.colorScheme.surface)
            .animateContentSize()
            .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        if (collapsible && title != null) {
            val activeCount = quickPicks.count { it.selected } + if (guidance.isGuided) 1 else 0
            CollapsibleHeader(
                title = title,
                subtitle = subtitle,
                activeCount = activeCount,
                expanded = expanded,
                onToggle = { expanded = !expanded },
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(androidx.compose.animation.core.tween(Theme.motion.durationMedium)) + expandVertically(),
            exit = fadeOut(androidx.compose.animation.core.tween(Theme.motion.durationShort)) + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
                val ownWords: @Composable () -> Unit = {
                    OwnWordsSection(
                        guidance = guidance,
                        placeholder = placeholder,
                        starters = starters,
                        surcharge = surcharge,
                        enabled = enabled,
                        focusRequester = focusRequester,
                        onTextChange = onTextChange,
                        onStarter = { starter ->
                            onStarter(starter)
                            focusRequester.requestFocus()
                        },
                        onClear = onClear,
                        onContinueWithout = onContinueWithout,
                    )
                }
                if (ownWordsFirst) ownWords()
                if (quickPicks.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
                        SheetSectionLabel(stringResource(Res.string.ai_guidance_quick_picks))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
                        ) {
                            quickPicks.forEach { pick -> QuickPickChip(pick, enabled) }
                        }
                    }
                }
                if (!ownWordsFirst) ownWords()
            }
        }
    }
}

@Composable
private fun CollapsibleHeader(
    title: String,
    subtitle: String?,
    activeCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
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
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            val detail = if (activeCount > 0) stringResource(Res.string.ai_guidance_active, activeCount) else subtitle
            detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
@Suppress("LongParameterList") // a section of the composer: data + its callbacks
private fun OwnWordsSection(
    guidance: GuidanceDraft,
    placeholder: String,
    starters: List<String>,
    surcharge: Int,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onTextChange: (String) -> Unit,
    onStarter: (String) -> Unit,
    onClear: () -> Unit,
    onContinueWithout: () -> Unit,
) {
    val canWrite = enabled && guidance.canWrite
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
        SheetSectionLabel(stringResource(Res.string.ai_guidance_own_words))
        InstructionField(
            text = guidance.text,
            placeholder = placeholder,
            enabled = canWrite,
            isError = guidance.problem != null && guidance.problem != InstructionProblem.Paused,
            focusRequester = focusRequester,
            onTextChange = onTextChange,
            onClear = onClear,
        )
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
        guidance.problem?.let { problem ->
            InstructionProblemNote(problem, onContinueWithout = onContinueWithout.takeIf {
                problem == InstructionProblem.Unavailable
            })
        }
        if (canWrite && starters.isNotEmpty()) {
            SheetSectionLabel(stringResource(Res.string.ai_guidance_start_with))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
            ) {
                starters.forEach { starter ->
                    SuggestionChip(
                        onClick = { onStarter(starter) },
                        label = { Text("$starter …", style = MaterialTheme.typography.labelLarge) },
                        shape = RoundedCornerShape(Theme.shapes.pill),
                    )
                }
            }
        }
    }
}

/**
 * Text-field-value based so a starter inserted from outside puts the cursor at the end, ready to
 * finish the sentence (a plain String field would leave it where it was).
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
    var field by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    if (field.text != text) field = TextFieldValue(text, TextRange(text.length))
    val focusManager = LocalFocusManager.current
    val outline = MaterialTheme.colorScheme.outlineVariant
    val container = MaterialTheme.colorScheme.surfaceContainerLow
    val ownWords = stringResource(Res.string.ai_guidance_own_words)

    OutlinedTextField(
        value = field,
        onValueChange = { value ->
            field = value.copy(text = value.text.take(AiInstruction.MAX_LENGTH))
            if (field.text != text) onTextChange(field.text)
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
private fun QuickPickChip(pick: QuickPick, enabled: Boolean) {
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
    )
}

/** Why the words weren't used, that nothing was charged, and (when they couldn't be checked) a way on. */
@Composable
private fun InstructionProblemNote(problem: InstructionProblem, onContinueWithout: (() -> Unit)?) {
    val paused = problem == InstructionProblem.Paused
    val colors = MaterialTheme.colorScheme
    val container = if (paused) colors.surfaceContainerHigh else colors.errorContainer
    val content = if (paused) colors.onSurfaceVariant else colors.onErrorContainer
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
        if (!paused) {
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
