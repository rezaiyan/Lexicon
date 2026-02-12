package domain.word.service

import domain.word.model.Word
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

interface IWordSyncService {
    fun syncWords(remoteWords: List<Word>): Flow<SyncResult>
    
    sealed interface SyncResult {
        data class Success(val syncedCount: Int) : SyncResult
        data class Error(val message: String) : SyncResult
    }
}

class WordSyncService(
    private val wordRepository: IWordRepository
) : IWordSyncService {
    
    override fun syncWords(remoteWords: List<Word>): Flow<IWordSyncService.SyncResult> = 
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
                        emit(IWordSyncService.SyncResult.Success(newWords.size))
                    } else {
                        emit(IWordSyncService.SyncResult.Success(0))
                    }
                }
            }
    
    private data class WordKey(val originalWord: String, val translation: String)
}
