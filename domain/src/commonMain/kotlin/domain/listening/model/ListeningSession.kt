package domain.listening.model

import domain.word.model.Word

/** One spoken line: text plus the language its voice model must speak it in. */
data class Utterance(val text: String, val languageCode: String)

/** The four beats of a listening item: prompt → pause → answer → gap. */
sealed interface ListeningStep {
    data object Prompt : ListeningStep
    data object Pause : ListeningStep
    data object Answer : ListeningStep
    data object Gap : ListeningStep
}

/** Position inside a listening queue. Pure data — the ViewModel performs the speech and delays. */
data class ListeningSession(
    val words: List<Word>,
    val index: Int = 0,
    val step: ListeningStep = ListeningStep.Prompt,
    val repetition: Int = 0,
) {
    val currentWord: Word get() = words[index]
    val isLastWord: Boolean get() = index == words.lastIndex

    /** Whether the answer side should be visible: once it has been (or is being) spoken. */
    val isAnswerRevealed: Boolean
        get() = step == ListeningStep.Answer || step == ListeningStep.Gap

    fun prompt(order: ListeningOrder): Utterance = when (order) {
        ListeningOrder.WORD_FIRST -> currentWord.wordUtterance()
        ListeningOrder.TRANSLATION_FIRST -> currentWord.translationUtterance()
    }

    fun answer(order: ListeningOrder): Utterance = when (order) {
        ListeningOrder.WORD_FIRST -> currentWord.translationUtterance()
        ListeningOrder.TRANSLATION_FIRST -> currentWord.wordUtterance()
    }

    // The learned word is in targetLanguage; its translation is in sourceLanguage.
    private fun Word.wordUtterance() = Utterance(originalWord, targetLanguage.code)
    private fun Word.translationUtterance() = Utterance(translation, sourceLanguage.code)
}

sealed interface ListeningCommand {
    /** The current step finished playing (speech ended or pause elapsed). */
    data object StepCompleted : ListeningCommand
    data object Next : ListeningCommand
    data object Previous : ListeningCommand
}

sealed interface ListeningTransition {
    data class Continue(val session: ListeningSession) : ListeningTransition
    data object Finished : ListeningTransition
}

/** Pure state machine for a listening session. No I/O, no clock. */
object ListeningReducer {

    fun reduce(
        session: ListeningSession,
        command: ListeningCommand,
        repeatCount: Int,
    ): ListeningTransition = when (command) {
        ListeningCommand.StepCompleted -> completeStep(session, repeatCount)
        ListeningCommand.Next -> advance(session)
        ListeningCommand.Previous -> ListeningTransition.Continue(
            session.copy(
                index = (session.index - 1).coerceAtLeast(0),
                step = ListeningStep.Prompt,
                repetition = 0,
            )
        )
    }

    private fun completeStep(session: ListeningSession, repeatCount: Int): ListeningTransition =
        when (session.step) {
            ListeningStep.Prompt -> ListeningTransition.Continue(session.copy(step = ListeningStep.Pause))
            ListeningStep.Pause -> ListeningTransition.Continue(session.copy(step = ListeningStep.Answer))
            ListeningStep.Answer -> ListeningTransition.Continue(session.copy(step = ListeningStep.Gap))
            ListeningStep.Gap -> if (session.repetition + 1 < repeatCount) {
                ListeningTransition.Continue(
                    session.copy(step = ListeningStep.Prompt, repetition = session.repetition + 1)
                )
            } else {
                advance(session)
            }
        }

    private fun advance(session: ListeningSession): ListeningTransition =
        if (session.isLastWord) {
            ListeningTransition.Finished
        } else {
            ListeningTransition.Continue(
                session.copy(index = session.index + 1, step = ListeningStep.Prompt, repetition = 0)
            )
        }
}
