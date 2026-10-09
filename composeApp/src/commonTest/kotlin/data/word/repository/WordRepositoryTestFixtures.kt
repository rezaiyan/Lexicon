package data.word.repository

import core.common.Try
import data.core.database.WordEntity
import data.core.database.WordEntityData
import data.settings.local.ISettingsLocalDataSource
import data.word.local.IWordLocalDataSource
import data.word.remote.model.RemoteWord
import data.word.sync.IWordConflictResolver
import data.word.sync.ResolvedWords
import data.word.sync.IWordRemoteSyncHandler
import domain.auth.session.ISessionManager
import domain.word.add.model.AddWordsOutcome
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import utils.Language
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

    override fun getDueCardsByTag(tagId: Long): Flow<List<Word>> = flowOf(emptyList())

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

        override suspend fun getNextDueAt(): Long? = null

    override suspend fun deleteAllWords() {
        storedWords.clear()
    }

    override suspend fun getMostCommonSourceLanguage(): String? = null

    val pendingUploadIds = mutableListOf<Int>()
    private var nextId = 1000

    override suspend fun addNewWords(words: List<Word>): AddWordsOutcome {
        if (shouldThrowOnInsert) throw RuntimeException("Local insert failed")
        val known = storedWords.mapTo(mutableSetOf()) { it.identity }
        val fresh = words.filter { known.add(it.identity) }.map { it.copy(id = nextId++) }
        storedWords.addAll(fresh)
        pendingUploadIds.addAll(fresh.map { it.id })
        return AddWordsOutcome(fresh.size, words.size - fresh.size, fresh.map { it.originalWord })
    }

    override suspend fun getPendingUploads(): List<Word> = storedWords.filter { it.id in pendingUploadIds }

    val idMoves = mutableMapOf<Int, Int>()

    override suspend fun completeUpload(uploaded: List<Int>, serverIds: Map<Int, Int>) {
        pendingUploadIds.removeAll(uploaded)
        idMoves.putAll(serverIds)
        storedWords = storedWords.map { word -> serverIds[word.id]?.let { word.copy(id = it) } ?: word }.toMutableList()
    }
}

internal class FakeWordRemoteSyncHandler : IWordRemoteSyncHandler {
    var syncWordsToRemoteCallCount = 0
    var syncWordUpdateCallCount = 0
    var syncWordDeletionCallCount = 0
    var syncFromRemoteCallCount = 0
    var lastSyncedUpdateId: Long? = null
    var lastSyncedDeletionId: Long? = null
    var syncedWords = mutableListOf<Word>()
    var remoteWordsToReturn: List<RemoteWord> = emptyList()
    var shouldFailSyncWordsToRemote = false
    var shouldFailSyncWordUpdate = false
    var shouldFailSyncWordDeletion = false
    var shouldFailSyncFromRemote = false

    /** What the server answers with: the saved words with their server ids. */
    var savedWordsToReturn: List<RemoteWord> = emptyList()

    override suspend fun syncWordsToRemote(words: List<Word>): Try<List<RemoteWord>> {
        syncWordsToRemoteCallCount++
        syncedWords.addAll(words)
        return if (shouldFailSyncWordsToRemote) {
            Try.failure(RuntimeException("Remote sync failed"))
        } else {
            Try.success(savedWordsToReturn)
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
        return if (shouldFailSyncWordDeletion) {
            Try.failure(RuntimeException("Remote deletion sync failed"))
        } else {
            Try.success(Unit)
        }
    }

    override suspend fun syncWordsDeletionToRemote(ids: List<Long>): Try<Unit> =
        Try.success(Unit)

    override suspend fun syncBatchLanguageUpdateToRemote(
        ids: List<Long>,
        sourceLanguage: String?,
        targetLanguage: String?
    ): Try<Unit> = Try.success(Unit)

    override suspend fun syncFromRemote(updatedAfter: Long): Try<List<RemoteWord>> {
        syncFromRemoteCallCount++
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
    var localIdMoves: Map<Int, Int> = emptyMap()

    override fun resolveConflicts(
        localWords: List<WordEntity>,
        remoteWords: List<RemoteWord>
    ): ResolvedWords {
        lastLocalWords = localWords
        lastRemoteWords = remoteWords
        return ResolvedWords(resolvedEntities, localIdMoves)
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

internal class FakeSettingsLocalDataSource : ISettingsLocalDataSource {
    var wordSyncTimestamp: Long = 0L

    override suspend fun getSettings() = null
    override fun observeSettings() = flowOf(null)
    override suspend fun saveSettings(data: data.core.database.SettingsEntityData) {}
    override suspend fun clearSettings() {}
    override suspend fun getWordSyncTimestamp(): Long = wordSyncTimestamp
    override suspend fun setWordSyncTimestamp(timestamp: Long) { wordSyncTimestamp = timestamp }
}

internal class FakeSessionManager(
    authenticated: Boolean = true,
) : ISessionManager {
    private val _flow = MutableStateFlow(authenticated)
    override val isAuthenticatedFlow: StateFlow<Boolean> = _flow
    var authenticated: Boolean
        get() = _flow.value
        set(value) { _flow.value = value }

    override suspend fun isAuthenticated(): Boolean = _flow.value
    override suspend fun setAuthenticated(isAuthenticated: Boolean) { _flow.value = isAuthenticated }
    override fun initialize(scope: CoroutineScope) {}
}

internal fun makeRepository(
    local: FakeWordLocalDataSource = FakeWordLocalDataSource(),
    remote: FakeWordRemoteSyncHandler = FakeWordRemoteSyncHandler(),
    resolver: FakeWordConflictResolver = FakeWordConflictResolver(),
    session: FakeSessionManager = FakeSessionManager(authenticated = true),
    settings: FakeSettingsLocalDataSource = FakeSettingsLocalDataSource(),
    scope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
) = WordRepositoryImpl(local, remote, resolver, session, settings, scope)
