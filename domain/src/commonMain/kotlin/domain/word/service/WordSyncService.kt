package domain.word.service

import core.common.Try
import domain.word.model.Word
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

interface IWordSyncService {
    fun syncWords(remoteWords: List<Word>): Flow<Try<Int>>
}

class WordSyncService(
    private val wordRepository: IWordRepository
) : IWordSyncService {

    override fun syncWords(remoteWords: List<Word>): Flow<Try<Int>> =
        wordRepository.getAllWords()
            .flatMapLatest { existingWords ->
                flow {
                    val existingKeys = existingWords.map {
                        WordKey(it.originalWord.trim().lowercase(), it.translation.trim().lowercase())
                    }.toSet()

                    val newWords = remoteWords.filter { word ->
                        val key = WordKey(word.originalWord.trim().lowercase(), word.translation.trim().lowercase())
                        key !in existingKeys
                    }

                    if (newWords.isNotEmpty()) {
                        wordRepository.insertWords(newWords)
                        emit(Try.success(newWords.size))
                    } else {
                        emit(Try.success(0))
                    }
                }
            }

    private data class WordKey(val originalWord: String, val translation: String)
}
