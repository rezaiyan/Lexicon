@file:OptIn(ExperimentalTime::class)

package data.word.repository

import core.common.Try
import data.core.database.WordEntity
import data.core.database.WordEntityData
import data.word.local.IWordLocalDataSource
import data.word.remote.model.RemoteWord
import data.word.sync.IWordConflictResolver
import data.word.sync.IWordRemoteSyncHandler
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import utils.Language
import kotlin.time.ExperimentalTime

internal class FakeWordLocalDataSource : IWordLocalDataSource {
    val insertedWords = mutableListOf<Word>()
    val updatedWords = mutableListOf<Word>()
    val deletedIds = mutableListOf<Int>()
    var storedWords = mutableListOf<Word>()
    var storedEntities = mutableListOf<WordEntity>()
    var shouldThrowOnInsert = false
    var shouldThrowOnUpdate = false
    var shouldThrowOnDelete = false

    override suspend fun getAllWordsAsync(): List<Word> = storedWords.toList()

    override fun getAllWords(): Flow<List<Word>> = flowOf(storedWords.toList())

    override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())

    override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())

    override suspend fun getWordById(id: Int): Word? = storedWords.find { it.id == id }

    override suspend fun insertWords(words: List<Word>) {
        if (shouldThrowOnInsert) throw RuntimeException("Local insert failed")
        insertedWords.addAll(words)
        storedWords.addAll(words)
    }

    override suspend fun updateWord(word: Word) {
        if (shouldThrowOnUpdate) throw RuntimeException("Local update failed")
        updatedWords.add(word)
    }

    override suspend fun deleteWord(id: Int) {
        if (shouldThrowOnDelete) throw RuntimeException("Local delete failed")
        deletedIds.add(id)
    }

    override suspend fun deleteWords(ids: List<Int>): Int {
        deletedIds.addAll(ids)
        return ids.size
    }

    override suspend fun updateWordsLanguages(
        ids: List<Int>,
        sourceLanguage: String,
        targetLanguage: String
    ): Int = ids.size

    override suspend fun getAllWordsOnce(): List<WordEntity> = storedEntities.toList()

    override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())

    override suspend fun getTotalCount(): Int = storedWords.size

    override suspend fun getDueCount(): Int = 0

    override suspend fun deleteAllWords() {
        storedWords.clear()
    }

    override suspend fun getMostCommonSourceLanguage(): String? = null
}

internal class FakeWordRemoteSyncHandler : IWordRemoteSyncHandler {
    var syncWordsToRemoteCallCount = 0
    var syncWordUpdateCallCount = 0
    var syncWordDeletionCallCount = 0
    var lastSyncedUpdateId: Long? = null
    var lastSyncedDeletionId: Long? = null
    var syncedWords = mutableListOf<Word>()
    var remoteWordsToReturn: List<RemoteWord> = emptyList()
    var shouldFailSyncWordsToRemote = false
    var shouldFailSyncWordUpdate = false
    var shouldFailSyncWordDeletion = false
    var shouldFailSyncFromRemote = false

    override suspend fun syncWordsToRemote(words: List<Word>): Try<Unit> {
        syncWordsToRemoteCallCount++
        syncedWords.addAll(words)
        return if (shouldFailSyncWordsToRemote) {
            Try.failure(RuntimeException("Remote sync failed"))
        } else {
            Try.success(Unit)
        }
    }

    override suspend fun syncWordUpdateToRemote(id: Long, word: Word): Try<Unit> {
        syncWordUpdateCallCount++
        lastSyncedUpdateId = id
        if (shouldFailSyncWordUpdate) {
            throw RuntimeException("Remote update sync failed")
        }
        return Try.success(Unit)
    }

    override suspend fun syncWordDeletionToRemote(id: Long): Try<Unit> {
        syncWordDeletionCallCount++
        lastSyncedDeletionId = id
        if (shouldFailSyncWordDeletion) {
            throw RuntimeException("Remote deletion sync failed")
        }
        return Try.success(Unit)
    }

    override suspend fun syncWordsDeletionToRemote(ids: List<Long>): Try<Unit> =
        Try.success(Unit)

    override suspend fun syncBatchLanguageUpdateToRemote(
        ids: List<Long>,
        sourceLanguage: String?,
        targetLanguage: String?
    ): Try<Unit> = Try.success(Unit)

    override suspend fun syncFromRemote(): Try<List<RemoteWord>> {
        return if (shouldFailSyncFromRemote) {
            Try.failure(RuntimeException("Remote fetch failed"))
        } else {
            Try.success(remoteWordsToReturn)
        }
    }
}

internal class FakeWordConflictResolver : IWordConflictResolver {
    var resolvedEntities: List<WordEntityData> = emptyList()
    var lastLocalWords: List<WordEntity> = emptyList()
    var lastRemoteWords: List<RemoteWord> = emptyList()

    override fun resolveConflicts(
        localWords: List<WordEntity>,
        remoteWords: List<RemoteWord>
    ): List<WordEntityData> {
        lastLocalWords = localWords
        lastRemoteWords = remoteWords
        return resolvedEntities
    }
}

internal fun makeWord(
    id: Int = 1,
    originalWord: String = "hello",
    translation: String = "hola",
    sourceLanguage: Language = Language.ENGLISH,
    targetLanguage: Language = Language.SPANISH
) = Word(
    id = id,
    originalWord = originalWord,
    translation = translation,
    description = "",
    sourceLanguage = sourceLanguage,
    targetLanguage = targetLanguage,
    nextReviewDate = 0L
)

internal fun makeRemoteWord(
    id: Long = 10L,
    originalWord: String = "hello",
    translation: String = "hola",
    sourceLanguage: String = "en",
    targetLanguage: String = "es"
) = RemoteWord(
    id = id,
    originalWord = originalWord,
    translation = translation,
    description = "",
    sourceLanguage = sourceLanguage,
    targetLanguage = targetLanguage,
    level = 0,
    easeFactor = 2.5f,
    interval = 0,
    repetitions = 0,
    lastReviewDate = 0L,
    nextReviewDate = 0L,
    createdAt = null
)

internal fun makeWordEntityData(
    id: Int = 1,
    originalWord: String = "hello",
    translation: String = "hola"
) = WordEntityData(
    id = id,
    originalWord = originalWord,
    translation = translation,
    description = "",
    sourceLanguage = "en",
    targetLanguage = "es",
    level = 0,
    easeFactor = 2.5f,
    interval = 0,
    repetitions = 0,
    lastReviewDate = 0L,
    nextReviewDate = 0L,
    dateAdded = 0L
)

internal fun makeRepository(
    local: FakeWordLocalDataSource = FakeWordLocalDataSource(),
    remote: FakeWordRemoteSyncHandler = FakeWordRemoteSyncHandler(),
    resolver: FakeWordConflictResolver = FakeWordConflictResolver()
) = WordRepositoryImpl(local, remote, resolver)
