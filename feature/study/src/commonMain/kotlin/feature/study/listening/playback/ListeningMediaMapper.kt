package feature.study.listening.playback

import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningStep
import media.ListeningMediaState

/**
 * What the system media surface should show for this playback, or null when nothing is running.
 * Changes only when something visible changes (word, reveal, play/pause), so the notification
 * never flickers on the silent pause and gap steps.
 */
fun ListeningPlayback.toMediaState(order: ListeningOrder, text: ListeningMediaText): ListeningMediaState? =
    when (this) {
        ListeningPlayback.Idle -> null
        is ListeningPlayback.Active -> {
            val word = session.currentWord
            val (from, to) = when (order) {
                ListeningOrder.WORD_FIRST -> word.targetLanguage to word.sourceLanguage
                ListeningOrder.TRANSLATION_FIRST -> word.sourceLanguage to word.targetLanguage
            }
            ListeningMediaState.Playing(
                title = session.prompt(order).text,
                subtitle = session.answer(order).text.takeIf { session.isAnswerRevealed },
                detail = "${text.position(session.index + 1, session.words.size)} · " +
                    "${from.displayName} → ${to.displayName}",
                index = session.index,
                total = session.words.size,
                isPlaying = isPlaying,
                labels = text.labels,
                example = session.example()?.text?.takeIf {
                    session.step == ListeningStep.Example || session.step == ListeningStep.Gap
                },
            )
        }
        is ListeningPlayback.Finished -> ListeningMediaState.Finished(
            title = text.finishedTitle,
            subtitle = text.finishedSubtitle(wordsHeard, (durationMs / MS_PER_MINUTE).toInt().coerceAtLeast(1)),
            labels = text.labels,
        )
    }

private const val MS_PER_MINUTE = 60_000L
