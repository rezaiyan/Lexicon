package domain.word.usecase

import core.common.Try
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import domain.word.service.IImportValidationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertTrue

class ImportViaFileUseCaseTest {

    private val wordRepo = FakeWordRepo()
    private val validationService = FakeImportValidationService()
    private val getCurrentLanguageUseCase = GetCurrentLanguageUseCase(FakeSettingsRepo())
    private val importWordsUseCase = ImportWordsUseCase(wordRepo, validationService, getCurrentLanguageUseCase)
    private val useCase = ImportViaFileUseCase(importWordsUseCase)

    @Test
    fun `returns failure for blank file content`() = runTest {
        val result = useCase("", "words.txt")

        assertTrue(result.isFailure)
    }

    @Test
    fun `returns failure for whitespace-only file content`() = runTest {
        val result = useCase("   ", "words.txt")

        assertTrue(result.isFailure)
    }

    @Test
    fun `returns failure for unsupported file extension`() = runTest {
        val result = useCase("hello - hola", "words.csv")

        assertTrue(result.isFailure)
    }

    @Test
    fun `returns failure for pdf file`() = runTest {
        val result = useCase("content", "words.pdf")

        assertTrue(result.isFailure)
    }

    @Test
    fun `accepts txt file extension`() = runTest {
        val result = useCase("hello - hola", "words.txt")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `accepts text file extension`() = runTest {
        val result = useCase("hello - hola", "words.text")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `accepts null filename`() = runTest {
        val result = useCase("hello - hola", null)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `passes language params to import use case`() = runTest {
        val result = useCase("hello - hola", "words.txt", Language.ENGLISH, Language.SPANISH)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke with Params delegates correctly`() = runTest {
        val result = useCase(ImportViaFileUseCase.Params("hello - hola", "words.txt"))

        assertTrue(result.isSuccess)
    }

    private class FakeWordRepo : IWordRepository {
        override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(words.size)
        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf(DeleteWordsProgress.Completed(0))
        override fun updateWordsLanguages(
            ids: List<Int>,
            sourceLanguage: String,
            targetLanguage: String,
        ): Flow<UpdateWordsLanguagesProgress> =
            flow { emit(UpdateWordsLanguagesProgress.Completed(ids.size)) }
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
    }

    private class FakeImportValidationService : IImportValidationService {
        override fun validateAndParse(
            text: String,
            sourceLanguage: Language,
            targetLanguage: Language,
        ): Try<List<Word>> {
            val words = text.split("\n").mapNotNull { line ->
                val parts = line.split(" - ")
                if (parts.size == 2) {
                    Word(
                        id = 0,
                        originalWord = parts[0].trim(),
                        translation = parts[1].trim(),
                        description = "",
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage,
                        nextReviewDate = 0L
                    )
                } else null
            }
            return if (words.isEmpty()) Try.failure(Exception("No valid words")) else Try.success(words)
        }
    }

    private class FakeSettingsRepo : domain.settings.repository.ISettingsRepository {
        override fun getLanguage(): Flow<Language> = flowOf(Language.ENGLISH)
        override suspend fun setLanguage(language: Language) {}
        override fun getThemeMode(): Flow<domain.settings.model.ThemeMode> =
            flowOf(domain.settings.model.ThemeMode.AUTO)

        override suspend fun setThemeMode(mode: domain.settings.model.ThemeMode) {}
        override suspend fun getLastInsightDate(): String? = null
        override suspend fun getCachedInsight(): String? = null
        override suspend fun updateDailyInsight(date: String, insight: String) {}
        override suspend fun getLastInsightDismissedTime(): Long = 0L
        override suspend fun setLastInsightDismissedTime(timestamp: Long) {}
        override suspend fun clearInsightData() {}
        override suspend fun clearSettings() {}
        override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(false)
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(false)
        override suspend fun setReviewRemindersEnabled(enabled: Boolean) {}
        override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(false)
        override suspend fun setMotivationalMessagesEnabled(enabled: Boolean) {}
        override suspend fun getDailyReminderTime(): String = "09:00"
        override suspend fun setDailyReminderTime(time: String) {}
        override suspend fun getMinimumDueCards(): Int = 5
        override suspend fun setMinimumDueCards(count: Int) {}
    }
}
