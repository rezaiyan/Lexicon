package data.word.sync

import data.core.network.error.AuthenticationException
import data.core.network.error.NetworkErrorHandler
import data.word.remote.WordRemoteDataSource
import data.word.remote.model.BatchUpdateLanguagesRequest
import data.word.remote.model.RemoteWord
import domain.auth.repository.IAuthRepository
import domain.common.Try
import domain.common.map
import domain.word.model.Word
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IWordRemoteSyncHandler {
    suspend fun syncWordsToRemote(words: List<Word>): Try<Unit>
    suspend fun syncWordUpdateToRemote(id: Long, word: Word): Try<Unit>
    suspend fun syncWordDeletionToRemote(id: Long): Try<Unit>
    suspend fun syncWordsDeletionToRemote(ids: List<Long>): Try<Unit>
    suspend fun syncBatchLanguageUpdateToRemote(
        ids: List<Long>,
        sourceLanguage: String?,
        targetLanguage: String?
    ): Try<Unit>
    suspend fun syncFromRemote(): Try<List<RemoteWord>>
}

class WordRemoteSyncHandler(
    private val wordRemoteDataSource: WordRemoteDataSource
) : IWordRemoteSyncHandler, KoinComponent {

    // Lazy injection to break circular dependency
    private val authRepository: IAuthRepository by inject()

    override suspend fun syncWordsToRemote(words: List<Word>): Try<Unit> {
        if (words.isEmpty()) return Try.success(Unit)

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

    override suspend fun syncWordUpdateToRemote(id: Long, word: Word): Try<Unit> {
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

    override suspend fun syncWordDeletionToRemote(id: Long): Try<Unit> {
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

    override suspend fun syncWordsDeletionToRemote(ids: List<Long>): Try<Unit> {
        if (ids.isEmpty()) return Try.success(Unit)

        val result = wordRemoteDataSource.deleteWords(ids)

        return NetworkErrorHandler.handleResult(
            result = result,
            authRepository = authRepository,
            onError = { error ->
                if (error !is AuthenticationException) {
                }
            }
        )
    }

    override suspend fun syncBatchLanguageUpdateToRemote(
        ids: List<Long>,
        sourceLanguage: String?,
        targetLanguage: String?
    ): Try<Unit> {
        if (ids.isEmpty()) return Try.success(Unit)

        val request = BatchUpdateLanguagesRequest(
            ids = ids,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )
        val result = wordRemoteDataSource.batchUpdateLanguages(request)

        return NetworkErrorHandler.handleResult(
            result = result,
            authRepository = authRepository,
            onError = { error ->
                if (error !is AuthenticationException) {
                }
            }
        )
    }

    override suspend fun syncFromRemote(): Try<List<RemoteWord>> {
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
        sourceLanguage = sourceLanguage.code,
        targetLanguage = targetLanguage.code,
        level = level,
        easeFactor = easeFactor,
        interval = interval,
        repetitions = repetitions,
        lastReviewDate = lastReviewDate,
        nextReviewDate = nextReviewDate,
        createdAt = dateAdded
    )
}
