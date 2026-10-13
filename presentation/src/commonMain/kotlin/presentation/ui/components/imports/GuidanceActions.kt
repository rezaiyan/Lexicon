package presentation.ui.components.imports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import domain.ai.model.Register
import domain.ai.model.VocabularyFocus
import domain.ai.model.WordType
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.ai_action_with_cost
import lexicon.resources.generated.resources.ai_pick_grammar_notes
import lexicon.resources.generated.resources.ai_pick_marked_only
import lexicon.resources.generated.resources.ai_pick_skip_names
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
import org.jetbrains.compose.resources.stringResource

/** The guidance callbacks a source offers; method references from its ViewModel, so they stay stable. */
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

private val PhotoWordTypes = listOf(WordType.NOUN, WordType.VERB, WordType.ADJECTIVE, WordType.PHRASE)

/** Photo: which marked words, which kinds, and grammar notes. Most useful first. */
@Composable
internal fun photoQuickPicks(focus: VocabularyFocus, actions: GuidanceActions): List<QuickPick> {
    val markedOnly = stringResource(Res.string.ai_pick_marked_only)
    val skipNames = stringResource(Res.string.ai_pick_skip_names)
    val grammarNotes = stringResource(Res.string.ai_pick_grammar_notes)
    val typeLabels = PhotoWordTypes.map { it.label() }
    return remember(focus, actions, typeLabels) {
        buildList<QuickPick> {
            add(QuickPick(markedOnly, focus.markedOnly, actions.onToggleMarkedOnly))
            PhotoWordTypes.forEachIndexed { index, type ->
                add(QuickPick(typeLabels[index], type in focus.wordTypes) { actions.onToggleWordType(type) })
            }
            add(QuickPick(grammarNotes, focus.grammarNotes, actions.onToggleGrammarNotes))
            add(QuickPick(skipNames, focus.skipNames, actions.onToggleSkipNames))
        }
    }
}

/** Suggestions: kinds of words, tone, and grammar notes. */
@Composable
internal fun suggestQuickPicks(focus: VocabularyFocus, actions: GuidanceActions): List<QuickPick> {
    val grammarNotes = stringResource(Res.string.ai_pick_grammar_notes)
    val typeLabels = WordType.entries.map { it.label() }
    val registerLabels = Register.entries.map { it.label() }
    return remember(focus, actions, typeLabels, registerLabels) {
        buildList<QuickPick> {
            WordType.entries.forEachIndexed { index, type ->
                add(QuickPick(typeLabels[index], type in focus.wordTypes) { actions.onToggleWordType(type) })
            }
            Register.entries.forEachIndexed { index, register ->
                add(QuickPick(registerLabels[index], focus.register == register) { actions.onToggleRegister(register) })
            }
            add(QuickPick(grammarNotes, focus.grammarNotes, actions.onToggleGrammarNotes))
        }
    }
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
