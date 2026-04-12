package fakes

import core.common.Try
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class FakeWordRepository : IWordRepository {
    val insertedWords = mutableListOf<Word>()
    val updatedWords = mutableListOf<Word>()
    val deletedIds = mutableListOf<Int>()
    var storedWords = mutableListOf<Word>()

    var insertResult: Try<Int> = Try.success(0)
    var updateResult: Try<Unit> = Try.success(Unit)
    var updateLocalResult: Try<Unit> = Try.success(Unit)
    var batchSyncResult: Try<Unit> = Try.success(Unit)
    var deleteResult: Try<Unit> = Try.success(Unit)
    var syncResult: Try<Unit> = Try.success(Unit)
    var totalCountResult: Try<Int> = Try.success(0)
    var dueCountResult: Try<Int> = Try.success(0)

    var insertCallCount = 0
    var updateCallCount = 0
    var updateLocalCallCount = 0
    var batchSyncCallCount = 0
    var lastUpdatedWord: Word? = null
    var lastUpdatedLocalWord: Word? = null
    var lastBatchSyncedWords: List<Word> = emptyList()
    var syncWithRemoteCalled = false
    var syncRemoteToLocalCalled = false

    override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(storedWords.toList())
    override fun getAllWords(): Flow<List<Word>> = flowOf(storedWords.toList())
    override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
    override fun getDueCardsByTag(tagId: Long): Flow<List<Word>> = flowOf(emptyList())
    override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
    override suspend fun getWordById(id: Int): Word? = storedWords.find { it.id == id }

    override suspend fun insertWords(words: List<Word>): Try<Int> {
        insertCallCount++
        insertedWords.addAll(words)
        storedWords.addAll(words)
        return if (insertResult.isSuccess) Try.success(words.size) else insertResult
    }

    override suspend fun updateWord(word: Word): Try<Unit> {
        updateCallCount++
        lastUpdatedWord = word
        updatedWords.add(word)
        return updateResult
    }

    override suspend fun updateWordLocal(word: Word): Try<Unit> {
        updateLocalCallCount++
        lastUpdatedLocalWord = word
        return updateLocalResult
    }

    override suspend fun batchSyncWords(words: List<Word>): Try<Unit> {
        batchSyncCallCount++
        lastBatchSyncedWords = words
        return batchSyncResult
    }

    override suspend fun deleteWord(id: Int): Try<Unit> {
        deletedIds.add(id)
        return deleteResult
    }

    override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flow {
        deletedIds.addAll(ids)
        emit(DeleteWordsProgress.Completed(ids.size))
    }

    override fun updateWordsLanguages(
        ids: List<Int>,
        sourceLanguage: String,
        targetLanguage: String,
    ): Flow<UpdateWordsLanguagesProgress> = flow {
        emit(UpdateWordsLanguagesProgress.Completed(ids.size))
    }

    override suspend fun deleteAllWords(): Try<Unit> {
        storedWords.clear()
        return Try.success(Unit)
    }

    override suspend fun syncWithRemote(): Try<Unit> {
        syncWithRemoteCalled = true
        return syncResult
    }

    override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> {
        syncRemoteToLocalCalled = true
        return syncResult
    }

    override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
    override suspend fun getTotalCount(): Try<Int> = totalCountResult
    override suspend fun getDueCount(): Try<Int> = dueCountResult
    override suspend fun getNextDueAt(): Try<Long?> = Try.success(null)
    override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
}
