@file:OptIn(ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package data.word.repository

import data.word.local.IWordLocalDataSource
import data.word.mapper.WordMapper
import data.word.sync.IWordConflictResolver
import data.word.sync.IWordRemoteSyncHandler
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class WordRepositoryImpl(
    private val localDataSource: IWordLocalDataSource,
    private val remoteSyncHandler: IWordRemoteSyncHandler,
    private val conflictResolver: IWordConflictResolver
) : IWordRepository {

    override suspend fun getAllWordsAsync(): List<Word> {
        return localDataSource.getAllWordsAsync()
    }

    override fun getAllWords(): Flow<List<Word>> {
        return localDataSource.getAllWords()
    }

    override fun getDueCards(): Flow<List<Word>> {
        return localDataSource.getDueCards()
    }

    override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> {
        return localDataSource.getWordsByStage(stage)
    }

    override suspend fun insertWords(words: List<Word>): Int {
        if (words.isEmpty()) return 0

        val existingWords = getAllWordsAsync()
        val newWords = words.filter { newWord ->
            existingWords.none { it.isSameContent(newWord) }
        }

        if (newWords.isEmpty()) return 0

        remoteSyncHandler.syncWordsToRemote(newWords)
        localDataSource.insertWords(newWords)
        return newWords.size
    }

    override suspend fun getWordById(id: Int): Word? {
        return localDataSource.getWordById(id)
    }

    override suspend fun updateWord(word: Word) {
        remoteSyncHandler.syncWordUpdateToRemote(word.id.toLong(), word)
        localDataSource.updateWord(word)
    }

    override suspend fun deleteWord(id: Int) {
        remoteSyncHandler.syncWordDeletionToRemote(id.toLong())
        localDataSource.deleteWord(id)
    }

    override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> {
        if (ids.isEmpty()) {
            return flow { emit(DeleteWordsProgress.Failed("No words to delete")) }
        }

        return flow {
            emit(DeleteWordsProgress.DeletingFromBackend(ids.size))
        }
            .flatMapConcat {
                flow {
                    val result =
                        remoteSyncHandler.syncWordsDeletionToRemote(ids.map { it.toLong() })
                    result.fold(
                        onSuccess = { emit(DeleteWordsProgress.DeletingFromLocal(ids.size)) },
                        onFailure = { emit(DeleteWordsProgress.DeletingFromLocal(ids.size)) }
                    )
                }
            }
            .flatMapConcat { deletingLocalState ->
                flow {
                    emit(deletingLocalState)
                    val deletedCount = localDataSource.deleteWords(ids)
                    emit(DeleteWordsProgress.Completed(deletedCount))
                }
            }
            .catch { error ->
                kotlin.runCatching {
                    val deletedCount = localDataSource.deleteWords(ids)
                    emit(DeleteWordsProgress.Completed(deletedCount))
                }.onFailure {
                    emit(DeleteWordsProgress.Failed(error.message ?: "Failed to delete words"))
                }
            }
    }

    override suspend fun syncWithRemote(): Result<Unit> {
        return kotlin.runCatching {
            val remoteWordsResult = remoteSyncHandler.syncFromRemote()

            remoteWordsResult.fold(
                onSuccess = { remoteWords ->
                    val localWords = localDataSource.getAllWordsOnce()

                    val resolvedEntities = conflictResolver.resolveConflicts(
                        localWords = localWords,
                        remoteWords = remoteWords
                    )

                    if (resolvedEntities.isNotEmpty()) {
                        val resolvedWords = resolvedEntities.map { entity ->
                            WordMapper.toDomain(entity)
                        }
                        localDataSource.insertWords(resolvedWords)
                    }
                },
                onFailure = { error ->
                    throw error
                }
            )
        }
    }

    override suspend fun syncRemoteToLocal(clearFirst: Boolean): Result<Unit> {
        return syncWithRemote()
    }

    override fun getProgressStats(): Flow<ProgressStats> {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        return flow {
            emitAll(localDataSource.getProgressStats(currentTime))
        }
            .onStart { syncWithRemote() }
            .catch { emit(ProgressStats()) }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getTotalCount(): Int {
        return localDataSource.getTotalCount()
    }

    override suspend fun getDueCount(): Int {
        return localDataSource.getDueCount()
    }

    override suspend fun deleteAllWords(): Result<Unit> {
        return kotlin.runCatching {
            localDataSource.deleteAllWords()
        }
    }
}

