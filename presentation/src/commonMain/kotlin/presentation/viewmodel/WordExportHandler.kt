@file:OptIn(ExperimentalTime::class)

package presentation.viewmodel

import analytics.IAnalyticsTracker
import domain.word.model.Word
import domain.word.usecase.ExportWordsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import presentation.model.WordManagerEffect
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class WordExportHandler(
    private val exportWordsUseCase: ExportWordsUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val events: SendChannel<WordManagerEffect>,
    private val scope: CoroutineScope
) {
    
    fun shareWords(words: List<Word>, selectedWordIds: Set<Int>) {
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
            
            val exportText = exportWordsUseCase(wordsToExport)
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
                    "type" to if (selectedWordIds.isEmpty()) "all" else "selected"
                )
            )
        }
    }
}



