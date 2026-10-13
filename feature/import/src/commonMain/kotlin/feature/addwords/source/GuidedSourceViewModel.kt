package feature.addwords.source

import analytics.IAnalyticsTracker
import core.base.BaseViewModel
import domain.ai.model.Register
import domain.ai.model.WordType
import domain.word.add.model.WordOrigin
import feature.addwords.model.GuidanceDraft
import feature.addwords.model.SourceEffect
import feature.addwords.model.logImportEvent
import feature.addwords.model.toInstructionProblem

/** A source's state that carries what the learner is telling the AI. */
interface GuidedSourceState<S> {
    val guidance: GuidanceDraft

    /** True while a request is in flight: the guidance is locked then. */
    val isBusy: Boolean

    fun withGuidance(guidance: GuidanceDraft): S
}

/**
 * The AI sources (photo, suggestions) share the same guidance controls: quick picks and the
 * learner's own words. This base holds those event handlers once.
 */
abstract class GuidedSourceViewModel<S : GuidedSourceState<S>>(
    private val origin: WordOrigin,
    private val analytics: IAnalyticsTracker,
) : BaseViewModel<S, SourceEffect>() {

    fun onInstructionChanged(text: String) = editGuidance { withText(text) }

    fun applyStarter(starter: String) = editGuidance { withStarter(starter) }

    fun clearInstruction() = editGuidance { withText("") }

    fun toggleWordType(type: WordType) = editGuidance { toggleWordType(type) }

    fun toggleRegister(register: Register) = editGuidance { toggleRegister(register) }

    fun toggleGrammarNotes() = editGuidance { toggleGrammarNotes() }

    fun toggleMarkedOnly() = editGuidance { toggleMarkedOnly() }

    fun toggleSkipNames() = editGuidance { toggleSkipNames() }

    private fun editGuidance(edit: GuidanceDraft.() -> GuidanceDraft) {
        if (currentState.isBusy) return
        updateState { withGuidance(guidance.edit()) }
    }

    /** Logs what was asked for. Never the text itself: only whether there was any, and how long. */
    protected fun logGuidanceSent() {
        val guidance = currentState.guidance
        if (!guidance.isGuided && guidance.focus.isEmpty) return
        analytics.logImportEvent(
            "import_ai_guidance",
            origin,
            "guided" to guidance.isGuided,
            "instruction_length" to (guidance.instruction?.text?.length ?: 0),
            "word_types" to guidance.focus.wordTypes.size,
            "register" to (guidance.focus.register?.name ?: "none"),
        )
    }

    /**
     * Shows a refusal of the learner's instructions next to the field (keeping their text so they can
     * fix it). Returns false when [error] is about something else, for the caller to handle.
     */
    protected fun showInstructionProblem(error: Throwable): Boolean {
        val problem = error.toInstructionProblem() ?: return false
        analytics.logImportEvent("import_instructions_refused", origin, "reason" to problem.name)
        updateState { withGuidance(guidance.withProblem(problem)) }
        return true
    }
}
