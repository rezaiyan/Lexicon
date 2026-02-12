package data.word.sync

import data.core.network.error.AuthenticationException
import data.core.network.error.NetworkErrorHandler
import data.word.remote.WordRemoteDataSource
import data.word.remote.model.RemoteWord
import domain.auth.repository.IAuthRepository
import domain.word.model.Word
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IWordRemoteSyncHandler {
    suspend fun syncWordsToRemote(words: List<Word>): Result<Unit>
    suspend fun syncWordUpdateToRemote(id: Long, word: Word): Result<Unit>
    suspend fun syncWordDeletionToRemote(id: Long): Result<Unit>
    suspend fun syncWordsDeletionToRemote(ids: List<Long>): Result<Unit>
    suspend fun syncFromRemote(): Result<List<RemoteWord>>
}

class WordRemoteSyncHandler(
    private val wordRemoteDataSource: WordRemoteDataSource
) : IWordRemoteSyncHandler, KoinComponent {

    // Lazy injection to break circular dependency
    private val authRepository: IAuthRepository by inject()

    override suspend fun syncWordsToRemote(words: List<Word>): Result<Unit> {
        if (words.isEmpty()) return Result.success(Unit)

        val remoteWords = words.map { it.toRemote() }
        val result = wordRemoteDataSource.upsertWords(remoteWords)

        return NetworkErrorHandler.handleResult(
            result = result.map { Unit },
            authRepository = authRepository,
            onError = { error ->
                if (error !is AuthenticationException) {
                }
            }
        )
    }

    override suspend fun syncWordUpdateToRemote(id: Long, word: Word): Result<Unit> {
        val result = wordRemoteDataSource.updateWord(id, word.toRemote())

        return NetworkErrorHandler.handleResult(
            result = result.map { Unit },
            authRepository = authRepository,
            onError = { error ->
                if (error !is AuthenticationException) {
                }
            }
        )
    }

    override suspend fun syncWordDeletionToRemote(id: Long): Result<Unit> {
        val result = wordRemoteDataSource.deleteWord(id)

        return NetworkErrorHandler.handleResult(
            result = result.map { Unit },
            authRepository = authRepository,
            onError = { error ->
                if (error !is AuthenticationException) {
                }
            }
        )
    }

    override suspend fun syncWordsDeletionToRemote(ids: List<Long>): Result<Unit> {
        if (ids.isEmpty()) return Result.success(Unit)

        val result = wordRemoteDataSource.deleteWords(ids).first()

        return NetworkErrorHandler.handleResult(
            result = result,
            authRepository = authRepository,
            onError = { error ->
                if (error !is AuthenticationException) {
                }
            }
        )
    }

    override suspend fun syncFromRemote(): Result<List<RemoteWord>> {
        val result = wordRemoteDataSource.getWords()

        return NetworkErrorHandler.handleResult(
            result = result,
            authRepository = authRepository
        )
    }

    private fun Word.toRemote(): RemoteWord = RemoteWord(
        id = this.id.takeIf { it != 0 }?.toLong(),
        originalWord = originalWord,
        translation = translation,
        description = description,
        sourceLanguage = sourceLanguage,
        targetLanguage = targetLanguage,
        level = level,
        easeFactor = easeFactor,
        interval = interval,
        repetitions = repetitions,
        lastReviewDate = lastReviewDate,
        nextReviewDate = nextReviewDate,
        createdAt = dateAdded
    )
}

