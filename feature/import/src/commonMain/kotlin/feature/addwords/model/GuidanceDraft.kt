package feature.addwords.model

import core.error.DomainError
import core.error.DomainError.AddWords.InstructionsRefused.Reason
import domain.ai.model.AiGuidance
import domain.ai.model.AiInstruction
import domain.ai.model.Register
import domain.ai.model.VocabularyFocus
import domain.ai.model.WordType

/** Why the learner's instructions weren't used; shown next to the instructions field, never as a page error. */
enum class InstructionProblem { OffTopic, Unsafe, PersonalData, Paused, Unavailable }

/** The instruction problem behind [this] failure, or null when the failure is about something else. */
fun Throwable.toInstructionProblem(): InstructionProblem? =
    (this as? DomainError.AddWords.InstructionsRefused)?.let {
        when (it.reason) {
            Reason.OffTopic -> InstructionProblem.OffTopic
            Reason.Unsafe -> InstructionProblem.Unsafe
            Reason.PersonalData -> InstructionProblem.PersonalData
            Reason.Paused -> InstructionProblem.Paused
            Reason.Unavailable -> InstructionProblem.Unavailable
        }
    }

/**
 * What the learner is composing for the AI: quick picks ([focus], free) and their own words
 * ([text], guided). Pure: every edit returns a new draft, and editing clears a shown [problem].
 */
data class GuidanceDraft(
    val text: String = "",
    val focus: VocabularyFocus = VocabularyFocus(),
    val problem: InstructionProblem? = null,
) {
    val instruction: AiInstruction? get() = AiInstruction.of(text)

    /** Guided requests cost the guided price. */
    val isGuided: Boolean get() = instruction != null

    /** While paused the field can't be used; quick picks still work. */
    val canWrite: Boolean get() = problem != InstructionProblem.Paused

    fun toGuidance() = AiGuidance(focus, instruction)

    fun withText(value: String) = copy(text = value.take(AiInstruction.MAX_LENGTH), problem = null)

    /** A sentence starter ("I'm preparing for …"): fills an empty field, or continues what is there. */
    fun withStarter(starter: String): GuidanceDraft {
        val joined = if (text.isBlank()) starter else "${text.trimEnd()} $starter"
        return withText("$joined ")
    }

    fun toggleWordType(type: WordType) = withFocus {
        copy(wordTypes = if (type in wordTypes) wordTypes - type else wordTypes + type)
    }

    /** One register at most; picking the selected one again clears it. */
    fun toggleRegister(value: Register) = withFocus { copy(register = value.takeIf { it != register }) }

    fun toggleGrammarNotes() = withFocus { copy(grammarNotes = !grammarNotes) }

    fun toggleMarkedOnly() = withFocus { copy(markedOnly = !markedOnly) }

    fun toggleSkipNames() = withFocus { copy(skipNames = !skipNames) }

    private fun withFocus(edit: VocabularyFocus.() -> VocabularyFocus) =
        // A pause outlives quick-pick edits: only time lifts it
        copy(focus = focus.edit(), problem = problem.takeIf { it == InstructionProblem.Paused })
}
