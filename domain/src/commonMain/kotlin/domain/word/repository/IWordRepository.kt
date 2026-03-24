package domain.word.repository

import core.common.Try
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import kotlinx.coroutines.flow.Flow

/**
 * Domain layer repository interface
 * Defines contract for word data operations
 */
interface IWordRepository {
    suspend fun getAllWordsAsync(): Try<List<Word>>
    fun getAllWords(): Flow<List<Word>>
    fun getDueCards(): Flow<List<Word>>
    fun getDueCardsByTag(tagId: Long): Flow<List<Word>>
    fun getWordsByStage(stage: LearningStage): Flow<List<Word>>
    suspend fun getWordById(id: Int): Word?
    suspend fun insertWords(words: List<Word>): Try<Int>
    suspend fun updateWord(word: Word): Try<Unit>
    suspend fun deleteWord(id: Int): Try<Unit>
    fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress>
    fun updateWordsLanguages(
        ids: List<Int>,
        sourceLanguage: String,
        targetLanguage: String
    ): Flow<UpdateWordsLanguagesProgress>
    suspend fun deleteAllWords(): Try<Unit>
    suspend fun syncWithRemote(): Try<Unit>
    suspend fun syncRemoteToLocal(clearFirst: Boolean = false): Try<Unit>
    fun getProgressStats(): Flow<ProgressStats>
    suspend fun getTotalCount(): Try<Int>
    suspend fun getDueCount(): Try<Int>
    suspend fun getMostCommonSourceLanguage(): Try<String?>
}

/**
 * Progress states for batch delete operation
 */
sealed class DeleteWordsProgress {
    data class DeletingFromBackend(val count: Int) : DeleteWordsProgress()
    data class DeletingFromLocal(val count: Int) : DeleteWordsProgress()
    data class Completed(val count: Int) : DeleteWordsProgress()
    data class Failed(val error: String) : DeleteWordsProgress()
}

/**
 * Progress states for batch language update operation
 */
sealed class UpdateWordsLanguagesProgress {
    data class UpdatingBackend(val count: Int) : UpdateWordsLanguagesProgress()
    data class UpdatingLocal(val count: Int) : UpdateWordsLanguagesProgress()
    data class Completed(val count: Int) : UpdateWordsLanguagesProgress()
    data class Failed(val error: String) : UpdateWordsLanguagesProgress()
}
