package domain.auth.usecase

import core.common.Try
import domain.auth.session.ISessionManager
import domain.auth.storage.ISecureStorage
import domain.settings.repository.ISettingsRepository
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import utils.Language
import domain.settings.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClearAllUserDataUseCaseTest {

    private val wordRepository = FakeWordRepo()
    private val settingsRepository = FakeSettingsRepo()
    private val secureStorage = FakeSecureStorage()
    private val sessionManager = FakeSessionManager()
    private val useCase = ClearAllUserDataUseCase(wordRepository, settingsRepository, secureStorage, sessionManager)

    @Test
    fun `clears all user data successfully`() = runTest {
        val result = useCase()

        assertTrue(result.isSuccess)
        assertTrue(wordRepository.deleteAllWordsCalled)
        assertTrue(settingsRepository.clearSettingsCalled)
        assertTrue(secureStorage.clearTokensCalled)
        assertFalse(sessionManager.isAuthenticatedValue)
    }

    @Test
    fun `invoke with Unit params delegates correctly`() = runTest {
        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertTrue(wordRepository.deleteAllWordsCalled)
    }

    @Test
    fun `sets session to unauthenticated`() = runTest {
        sessionManager.isAuthenticatedValue = true

        useCase()

        assertFalse(sessionManager.isAuthenticatedValue)
    }

    @Test
    fun `returns failure when word repository throws`() = runTest {
        wordRepository.shouldThrow = true

        val result = useCase()

        assertTrue(result.isFailure)
    }

    private class FakeWordRepo : IWordRepository {
        var deleteAllWordsCalled = false
        var shouldThrow = false

        override suspend fun deleteAllWords(): Try<Unit> {
            if (shouldThrow) throw RuntimeException("Delete failed")
            deleteAllWordsCalled = true
            return Try.success(Unit)
        }

        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(0)
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf(DeleteWordsProgress.Completed(0))
        override fun updateWordsLanguages(
            ids: List<Int>,
            sourceLanguage: String,
            targetLanguage: String,
        ): Flow<UpdateWordsLanguagesProgress> =
            flow { emit(UpdateWordsLanguagesProgress.Completed(ids.size)) }

        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
    }

    private class FakeSettingsRepo : ISettingsRepository {
        var clearSettingsCalled = false
        override suspend fun clearSettings(): Try<Unit> {
            clearSettingsCalled = true
            return Try.success(Unit)
        }
        override fun getLanguage(): Flow<Language> = flowOf(Language.ENGLISH)
        override suspend fun setLanguage(language: Language): Try<Unit> = Try.success(Unit)
        override fun getThemeMode(): Flow<ThemeMode> = flowOf(ThemeMode.AUTO)
        override suspend fun setThemeMode(mode: ThemeMode): Try<Unit> = Try.success(Unit)
        override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setNotificationsEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setReviewRemindersEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setMotivationalMessagesEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override suspend fun getDailyReminderTime(): Try<String> = Try.success("09:00")
        override suspend fun setDailyReminderTime(time: String): Try<Unit> = Try.success(Unit)
        override suspend fun getMinimumDueCards(): Try<Int> = Try.success(5)
        override suspend fun setMinimumDueCards(count: Int): Try<Unit> = Try.success(Unit)
    }

    private class FakeSecureStorage : ISecureStorage {
        var clearTokensCalled = false

        override suspend fun clearTokens() { clearTokensCalled = true }
    }

    private class FakeSessionManager : ISessionManager {
        var isAuthenticatedValue = true
        override val isAuthenticatedFlow: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun setAuthenticated(isAuthenticated: Boolean) {
            isAuthenticatedValue = isAuthenticated
        }
        override suspend fun isAuthenticated(): Boolean = isAuthenticatedValue
        override fun initialize(scope: CoroutineScope) {}
    }
}
