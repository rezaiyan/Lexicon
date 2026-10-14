package feature.words

import analytics.IAnalyticsTracker
import domain.tag.model.Tag
import domain.word.model.Word
import core.common.fold
import domain.word.usecase.ExportWordsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import feature.words.model.WordManagerEffect
import kotlin.time.Clock

class WordExportHandler(
    private val exportWordsUseCase: ExportWordsUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val events: SendChannel<WordManagerEffect>,
    private val scope: CoroutineScope
) {

    /** [tags] non-null exports each word's tags with it, so a re-import restores them. */
    fun shareWords(words: List<Word>, selectedWordIds: Set<Int>, tags: List<Tag>?) {
        scope.launch {
            val wordsToExport = if (selectedWordIds.isEmpty()) {
                words
            } else {
                words.filter { selectedWordIds.contains(it.id) }
            }

            if (wordsToExport.isEmpty()) {
                events.send(WordManagerEffect.ShareFailed)
                return@launch
            }

            val tagNames = tags?.associate { it.id to it.name }
            exportWordsUseCase(ExportWordsUseCase.Params(wordsToExport, tagNames)).fold(
                onSuccess = { exportText ->
                    val timestamp = Clock.System.now().toEpochMilliseconds()

                    events.send(WordManagerEffect.WordsShared(
                        count = wordsToExport.size,
                        text = exportText,
                        timestamp = timestamp
                    ))

                    analyticsTracker.logEvent(
                        "word_manager_share",
                        mapOf(
                            "count" to wordsToExport.size.toString(),
                            "type" to if (selectedWordIds.isEmpty()) "all" else "selected",
                            "with_tags" to (tagNames != null).toString(),
                        )
                    )
                },
                onFailure = {
                    events.send(WordManagerEffect.ShareFailed)
                }
            )
        }
    }
}
