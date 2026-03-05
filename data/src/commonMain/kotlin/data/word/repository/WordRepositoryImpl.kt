@file:OptIn(ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package data.word.repository

import data.word.local.IWordLocalDataSource
import data.word.mapper.toDomain
import data.word.sync.IWordConflictResolver
import data.word.sync.IWordRemoteSyncHandler
import core.common.Try
import core.common.fold
import core.common.onFailure
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.Dispatchers
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

    override suspend fun getAllWordsAsync(): Try<List<Word>> {
        return Try { localDataSource.getAllWordsAsync() }
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

    override suspend fun insertWords(words: List<Word>): Try<Int> {
        if (words.isEmpty()) return Try.success(0)

        return Try {
            val existingWords = localDataSource.getAllWordsAsync()
            val newWords = words.filter { newWord ->
                existingWords.none { it.isSameContent(newWord) }
            }

            if (newWords.isEmpty()) return Try.success(0)

            remoteSyncHandler.syncWordsToRemote(newWords)
            localDataSource.insertWords(newWords)
            newWords.size
        }
    }

    override suspend fun getWordById(id: Int): Word? {
        return localDataSource.getWordById(id)
    }

    override suspend fun updateWord(word: Word): Try<Unit> {
        return Try {
            remoteSyncHandler.syncWordUpdateToRemote(word.id.toLong(), word)
            localDataSource.updateWord(word)
        }
    }

    override suspend fun deleteWord(id: Int): Try<Unit> {
        return Try {
            remoteSyncHandler.syncWordDeletionToRemote(id.toLong())
            localDataSource.deleteWord(id)
        }
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
                Try {
                    val deletedCount = localDataSource.deleteWords(ids)
                    emit(DeleteWordsProgress.Completed(deletedCount))
                }.onFailure {
                    emit(DeleteWordsProgress.Failed(error.message ?: "Failed to delete words"))
                }
            }
    }

    override fun updateWordsLanguages(
        ids: List<Int>,
        sourceLanguage: String,
        targetLanguage: String
    ): Flow<UpdateWordsLanguagesProgress> {
        if (ids.isEmpty()) {
            return flow { emit(UpdateWordsLanguagesProgress.Failed("No words to update")) }
        }

        return flow {
            emit(UpdateWordsLanguagesProgress.UpdatingBackend(ids.size))
        }
            .flatMapConcat {
                flow {
                    val result = remoteSyncHandler.syncBatchLanguageUpdateToRemote(
                        ids = ids.map { it.toLong() },
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage
                    )
                    result.fold(
                        onSuccess = { emit(UpdateWordsLanguagesProgress.UpdatingLocal(ids.size)) },
                        onFailure = { emit(UpdateWordsLanguagesProgress.UpdatingLocal(ids.size)) }
                    )
                }
            }
            .flatMapConcat { updatingLocalState ->
                flow {
                    emit(updatingLocalState)
                    val updatedCount = localDataSource.updateWordsLanguages(
                        ids = ids,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage
                    )
                    emit(UpdateWordsLanguagesProgress.Completed(updatedCount))
                }
            }
            .catch { error ->
                Try {
                    val updatedCount = localDataSource.updateWordsLanguages(
                        ids = ids,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage
                    )
                    emit(UpdateWordsLanguagesProgress.Completed(updatedCount))
                }.onFailure {
                    emit(
                        UpdateWordsLanguagesProgress.Failed(
                            error.message ?: "Failed to update languages"
                        )
                    )
                }
            }
    }

    override suspend fun syncWithRemote(): Try<Unit> {
        return Try {
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
                            entity.toDomain()
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

    override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> {
        return syncWithRemote()
    }

    override fun getProgressStats(): Flow<ProgressStats> {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        return flow {
            emitAll(localDataSource.getProgressStats(currentTime))
        }
            .onStart { syncWithRemote() }
            .catch { emit(ProgressStats()) }
            .flowOn(Dispatchers.Default)
    }

    override suspend fun getTotalCount(): Try<Int> {
        return Try { localDataSource.getTotalCount() }
    }

    override suspend fun getDueCount(): Try<Int> {
        return Try { localDataSource.getDueCount() }
    }

    override suspend fun deleteAllWords(): Try<Unit> {
        return Try {
            localDataSource.deleteAllWords()
        }
    }
}
