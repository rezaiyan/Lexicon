package feature.study.listening.playback

import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.listening_finished_title
import lexicon.resources.generated.resources.listening_media_close
import lexicon.resources.generated.resources.listening_media_finished_subtitle
import lexicon.resources.generated.resources.listening_media_position
import lexicon.resources.generated.resources.listening_media_repeat
import lexicon.resources.generated.resources.listening_media_stop
import media.MediaLabels
import org.jetbrains.compose.resources.getString

/** Localized pieces of the media notification / Now Playing; resolved once from resources. */
class ListeningMediaText(
    val labels: MediaLabels,
    /** e.g. "3 of 20" for a 1-based [position]. */
    val position: (position: Int, total: Int) -> String,
    val finishedTitle: String,
    /** e.g. "18 words heard · 7 min". */
    val finishedSubtitle: (wordsHeard: Int, minutes: Int) -> String,
)

/** Resolves the media notification's text in the app's language; formats are kept for live values. */
suspend fun loadListeningMediaText(): ListeningMediaText {
    val position = getString(Res.string.listening_media_position)
    val finishedSubtitle = getString(Res.string.listening_media_finished_subtitle)
    return ListeningMediaText(
        labels = MediaLabels(
            stop = getString(Res.string.listening_media_stop),
            repeat = getString(Res.string.listening_media_repeat),
            close = getString(Res.string.listening_media_close),
        ),
        position = { current, total -> position.fill(current, total) },
        finishedTitle = getString(Res.string.listening_finished_title),
        finishedSubtitle = { heard, minutes -> finishedSubtitle.fill(heard, minutes) },
    )
}

/** Fills `%1$d` / `%2$d` placeholders; `String.format` is JVM-only. */
private fun String.fill(first: Int, second: Int): String =
    replace("%1\$d", first.toString()).replace("%2\$d", second.toString())
