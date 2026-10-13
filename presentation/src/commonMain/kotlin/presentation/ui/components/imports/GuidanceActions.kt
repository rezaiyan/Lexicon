package presentation.ui.components.imports

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import domain.ai.model.Register
import domain.onboarding.model.ProficiencyLevel
import domain.ai.model.VocabularyFocus
import domain.ai.model.WordType
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.ai_action_with_cost
import lexicon.resources.generated.resources.ai_group_kinds
import lexicon.resources.generated.resources.ai_group_level
import lexicon.resources.generated.resources.ai_group_tone
import lexicon.resources.generated.resources.ai_pick_grammar_notes
import lexicon.resources.generated.resources.ai_pick_grammar_notes_hint
import lexicon.resources.generated.resources.ai_pick_marked_only
import lexicon.resources.generated.resources.ai_pick_marked_only_hint
import lexicon.resources.generated.resources.ai_pick_skip_names
import lexicon.resources.generated.resources.ai_pick_skip_names_hint
import lexicon.resources.generated.resources.ai_register_everyday
import lexicon.resources.generated.resources.ai_register_formal
import lexicon.resources.generated.resources.ai_register_slang
import lexicon.resources.generated.resources.ai_starter_only_in
import lexicon.resources.generated.resources.ai_starter_preparing
import lexicon.resources.generated.resources.ai_starter_skip
import lexicon.resources.generated.resources.ai_starter_talk_about
import lexicon.resources.generated.resources.ai_starter_trip
import lexicon.resources.generated.resources.ai_starter_work
import lexicon.resources.generated.resources.ai_word_type_adjective
import lexicon.resources.generated.resources.ai_word_type_idiom
import lexicon.resources.generated.resources.ai_word_type_noun
import lexicon.resources.generated.resources.ai_word_type_phrase
import lexicon.resources.generated.resources.ai_word_type_verb
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The guidance callbacks a source offers; method references from its ViewModel, so they stay stable. */
@Stable
internal class GuidanceActions(
    val onTextChange: (String) -> Unit,
    val onStarter: (String) -> Unit,
    val onClear: () -> Unit,
    val onToggleWordType: (WordType) -> Unit,
    val onToggleRegister: (Register) -> Unit,
    val onToggleGrammarNotes: () -> Unit,
    val onToggleMarkedOnly: () -> Unit = {},
    val onToggleSkipNames: () -> Unit = {},
)

@Composable
private fun WordType.label(): String = stringResource(
    when (this) {
        WordType.NOUN -> Res.string.ai_word_type_noun
        WordType.VERB -> Res.string.ai_word_type_verb
        WordType.ADJECTIVE -> Res.string.ai_word_type_adjective
        WordType.PHRASE -> Res.string.ai_word_type_phrase
        WordType.IDIOM -> Res.string.ai_word_type_idiom
    },
)

@Composable
private fun Register.label(): String = stringResource(
    when (this) {
        Register.FORMAL -> Res.string.ai_register_formal
        Register.EVERYDAY -> Res.string.ai_register_everyday
        Register.SLANG -> Res.string.ai_register_slang
    },
)

private val GrammarIcon = Icons.AutoMirrored.Rounded.MenuBook

private val PhotoWordTypes = listOf(WordType.NOUN, WordType.VERB, WordType.ADJECTIVE, WordType.PHRASE)

/** Photo: which words to read first (toggles), then the kinds of words. */
@Composable
internal fun photoQuickPicks(focus: VocabularyFocus, actions: GuidanceActions): List<QuickPickGroup> {
    val kinds = stringResource(Res.string.ai_group_kinds)
    val toggles = listOf(
        ToggleText(Res.string.ai_pick_marked_only, Res.string.ai_pick_marked_only_hint),
        ToggleText(Res.string.ai_pick_skip_names, Res.string.ai_pick_skip_names_hint),
        ToggleText(Res.string.ai_pick_grammar_notes, Res.string.ai_pick_grammar_notes_hint),
    ).map { it.resolve() }
    val typeLabels = PhotoWordTypes.map { it.label() }
    return remember(focus, actions, typeLabels, toggles) {
        listOf(
            QuickPickGroup.Toggles(
                listOf(
                    toggles[0].pick(focus.markedOnly, Icons.Rounded.BorderColor, actions.onToggleMarkedOnly),
                    toggles[1].pick(focus.skipNames, Icons.Rounded.PersonOff, actions.onToggleSkipNames),
                    toggles[2].pick(focus.grammarNotes, GrammarIcon, actions.onToggleGrammarNotes),
                ),
            ),
            QuickPickGroup.Chips(kinds, wordTypePicks(PhotoWordTypes, typeLabels, focus, actions)),
        )
    }
}

/** Suggestions: the learner's level, kinds of words, one tone, and grammar notes. */
@Composable
internal fun suggestQuickPicks(
    focus: VocabularyFocus,
    actions: GuidanceActions,
    level: ProficiencyLevel?,
    onLevelSelected: (ProficiencyLevel) -> Unit,
): List<QuickPickGroup> {
    val levelTitle = stringResource(Res.string.ai_group_level)
    val levelLabels = ProficiencyLevel.entries.map { stringResource(it.title) }
    val kinds = stringResource(Res.string.ai_group_kinds)
    val tone = stringResource(Res.string.ai_group_tone)
    val grammar = ToggleText(Res.string.ai_pick_grammar_notes, Res.string.ai_pick_grammar_notes_hint).resolve()
    val typeLabels = WordType.entries.map { it.label() }
    val registerLabels = Register.entries.map { it.label() }
    return remember(focus, actions, typeLabels, registerLabels, kinds, grammar, level, levelLabels) {
        listOf(
            // Always one level: picking the selected one again keeps it
            QuickPickGroup.OneOf(
                levelTitle,
                ProficiencyLevel.entries.mapIndexed { index, entry ->
                    QuickPick(levelLabels[index], entry == level) { onLevelSelected(entry) }
                },
            ),
            QuickPickGroup.Chips(kinds, wordTypePicks(WordType.entries, typeLabels, focus, actions)),
            QuickPickGroup.OneOf(
                tone,
                Register.entries.mapIndexed { index, register ->
                    QuickPick(registerLabels[index], focus.register == register) { actions.onToggleRegister(register) }
                },
            ),
            QuickPickGroup.Toggles(
                listOf(grammar.pick(focus.grammarNotes, GrammarIcon, actions.onToggleGrammarNotes)),
            ),
        )
    }
}

/** An on/off option's label and the line that explains it. */
private data class ToggleText(val label: StringResource, val hint: StringResource)

private data class ResolvedToggle(val label: String, val hint: String) {
    fun pick(selected: Boolean, icon: ImageVector, onClick: () -> Unit) =
        QuickPick(label, selected, subtitle = hint, icon = icon, onClick = onClick)
}

@Composable
private fun ToggleText.resolve() = ResolvedToggle(stringResource(label), stringResource(hint))

private fun wordTypePicks(
    types: List<WordType>,
    labels: List<String>,
    focus: VocabularyFocus,
    actions: GuidanceActions,
): List<QuickPick> = types.mapIndexed { index, type ->
    QuickPick(labels[index], type in focus.wordTypes) { actions.onToggleWordType(type) }
}

@Composable
internal fun photoStarters(): List<String> {
    val onlyIn = stringResource(Res.string.ai_starter_only_in)
    val skip = stringResource(Res.string.ai_starter_skip)
    return remember(onlyIn, skip) { listOf(onlyIn, skip) }
}

@Composable
internal fun suggestStarters(): List<String> {
    val preparing = stringResource(Res.string.ai_starter_preparing)
    val work = stringResource(Res.string.ai_starter_work)
    val trip = stringResource(Res.string.ai_starter_trip)
    val talkAbout = stringResource(Res.string.ai_starter_talk_about)
    return remember(preparing, work, trip, talkAbout) { listOf(preparing, work, trip, talkAbout) }
}

/** "Extract words · 3 credits"; just [action] while the cost is unknown or free. */
@Composable
internal fun actionWithCost(action: String, cost: Int?): String =
    if (cost == null || cost <= 0) action else stringResource(Res.string.ai_action_with_cost, action, creditsText(cost))
