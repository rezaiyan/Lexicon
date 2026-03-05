package data.word.sync

import data.word.remote.WordRemoteDataSource
import data.word.remote.model.BatchUpdateLanguagesRequest
import data.word.remote.model.RemoteWord
import core.common.Try
import domain.word.model.Word
import org.koin.core.component.KoinComponent

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

    override suspend fun syncWordsToRemote(words: List<Word>): Try<Unit> {
        if (words.isEmpty()) return Try.success(Unit)

        val remoteWords = words.map { it.toRemote() }

        return wordRemoteDataSource.upsertWords(remoteWords)
    }

    override suspend fun syncWordUpdateToRemote(id: Long, word: Word): Try<Unit> {
        return wordRemoteDataSource.updateWord(id, word.toRemote())
    }

    override suspend fun syncWordDeletionToRemote(id: Long): Try<Unit> {
        return wordRemoteDataSource.deleteWord(id)
    }

    override suspend fun syncWordsDeletionToRemote(ids: List<Long>): Try<Unit> {
        if (ids.isEmpty()) return Try.success(Unit)

        return wordRemoteDataSource.deleteWords(ids)
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
        return wordRemoteDataSource.batchUpdateLanguages(request)
    }

    override suspend fun syncFromRemote(): Try<List<RemoteWord>> {
        return wordRemoteDataSource.getWords()
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
