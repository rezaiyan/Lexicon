package domain.auth.usecase

import core.common.Try
import domain.auth.model.AuthUser
import domain.auth.service.IAuthenticationService
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertTrue

class DeleteAccountUseCaseTest {

    private val authService = FakeAuthService()
    private val wordRepository = FakeWordRepo()
    private val settingsRepository = FakeSettingsRepo()
    private val useCase = DeleteAccountUseCase(authService, wordRepository, settingsRepository)

    @Test
    fun `delete account clears local data after successful deletion`() = runTest {
        val results = useCase.invoke().toList()

        assertTrue(results.isNotEmpty())
        assertTrue(wordRepository.deleteAllWordsCalled)
        assertTrue(settingsRepository.clearSettingsCalled)
    }

    @Test
    fun `invoke with Unit params delegates correctly`() = runTest {
        val results = useCase(Unit).toList()

        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `delete account emits Unit on success`() = runTest {
        val results = useCase.invoke().toList()

        assertTrue(results.size == 1)
    }

    private class FakeAuthService : IAuthenticationService {
        override fun loginWithGoogle(idToken: String): Flow<AuthUser> = flowOf()
        override fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Flow<AuthUser> = flowOf()
        override fun logout(): Flow<Unit> = flowOf(Unit)
        override fun deleteAccount(): Flow<Unit> = flowOf(Unit)
    }

    private class FakeWordRepo : IWordRepository {
        var deleteAllWordsCalled = false

        override suspend fun deleteAllWords(): Try<Unit> {
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
        override suspend fun clearSettings(): Try<Unit> { clearSettingsCalled = true; return Try.success(Unit) }
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
}
