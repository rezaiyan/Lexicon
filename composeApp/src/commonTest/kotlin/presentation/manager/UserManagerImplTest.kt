package presentation.manager

import core.common.Try
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import domain.auth.repository.IAuthRepository
import domain.auth.service.AuthenticationService
import domain.auth.usecase.DeleteAccountUseCase
import domain.auth.usecase.LogoutUseCase
import domain.notifications.repository.IPushTokenRepository
import domain.notifications.usecase.DeactivatePushTokenUseCase
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.streak.manager.IStreakManager
import domain.streak.model.StreakData
import domain.subscription.ISubscriptionManager
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionOffering
import domain.subscription.model.SubscriptionPackage
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserManagerImplTest {

    private fun fakeAuthRepo() = object : IAuthRepository {
        override suspend fun loginWithGoogle(idToken: String): Try<AuthUser> = Try.failure(RuntimeException("not used"))
        override suspend fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Try<AuthUser> = Try.failure(RuntimeException("not used"))
        override suspend fun logout(): Try<Unit> = Try.success(Unit)
        override suspend fun deleteAccount(): Try<Unit> = Try.success(Unit)
        override suspend fun getAccessToken(): String? = null
        override suspend fun isAuthenticated(): Boolean = false
        override fun isAuthenticatedAsFlow(): Flow<Boolean> = flowOf(false)
        override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> = flowOf(
            FeatureAccessResponse(FeatureFlags(), UserFeatureAccess(hasPremiumAccess = false))
        )
    }

    private fun fakeWordRepo() = object : IWordRepository {
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCardsByTag(tagId: Long): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(0)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf()
        override fun updateWordsLanguages(ids: List<Int>, sourceLanguage: String, targetLanguage: String): Flow<UpdateWordsLanguagesProgress> = flowOf()
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = emptyFlow()
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
        override suspend fun getNextDueAt(): Try<Long?> = Try.success(null)
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
        override suspend fun updateWordLocal(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun batchSyncWords(words: List<Word>): Try<Unit> = Try.success(Unit)
    }

    private fun fakeSettingsRepo() = object : ISettingsRepository {
        override fun getLanguage(): Flow<Language> = flowOf(Language.ENGLISH)
        override suspend fun setLanguage(language: Language): Try<Unit> = Try.success(Unit)
        override fun getThemeMode(): Flow<ThemeMode> = flowOf(ThemeMode.AUTO)
        override suspend fun setThemeMode(mode: ThemeMode): Try<Unit> = Try.success(Unit)
        override suspend fun clearSettings(): Try<Unit> = Try.success(Unit)
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

    private fun fakeSubscriptionManager() = object : ISubscriptionManager {
        override val customerInfo: StateFlow<SubscriptionCustomerInfo?> = MutableStateFlow(null)
        override suspend fun getOfferings(): Try<SubscriptionOffering> = Try.failure(RuntimeException("not used"))
        override suspend fun purchase(packageToPurchase: SubscriptionPackage): Try<SubscriptionCustomerInfo> = Try.failure(RuntimeException("not used"))
        override suspend fun restore(): Try<SubscriptionCustomerInfo> = Try.failure(RuntimeException("not used"))
        override fun isSubscribed(): Flow<Boolean> = flowOf(false)
        override suspend fun logIn(userId: String): Try<SubscriptionCustomerInfo> = Try.success(SubscriptionCustomerInfo(emptyMap()))
        override suspend fun logOut(): Try<SubscriptionCustomerInfo> = Try.success(SubscriptionCustomerInfo(emptyMap()))
        override fun getCurrentCustomerInfo(): SubscriptionCustomerInfo? = null
        override suspend fun manageSubscription(): Try<Unit> = Try.success(Unit)
        override suspend fun cancelSubscription(): Try<Unit> = Try.success(Unit)
    }

    private fun fakeStreakManager() = object : IStreakManager {
        override fun getStreak(): Flow<IStreakManager.StreakState> = flowOf(
            IStreakManager.StreakState.Loaded(StreakData(currentStreak = 0))
        )
        override suspend fun recordActivity(count: Int): Try<StreakData> =
            Try.success(StreakData(currentStreak = 0))
        override fun clearCache() {}
    }

    private var deactivateAllCalled = false
    private var deactivateCurrentCalled = false

    private fun fakePushTokenRepo() = object : IPushTokenRepository {
        override suspend fun registerToken(token: String): Try<Unit> = Try.success(Unit)
        override suspend fun deactivateAllTokens(): Try<Unit> {
            deactivateAllCalled = true
            return Try.success(Unit)
        }
        override suspend fun deactivateCurrentToken(): Try<Unit> {
            deactivateCurrentCalled = true
            return Try.success(Unit)
        }
        override fun initializeAndRegister() {}
    }

    private fun buildManager(): UserManagerImpl {
        val authRepo = fakeAuthRepo()
        val service = AuthenticationService(authRepo)
        val wordRepo = fakeWordRepo()
        val settingsRepo = fakeSettingsRepo()
        val deactivatePushTokenUseCase = DeactivatePushTokenUseCase(fakePushTokenRepo())
        return UserManagerImpl(
            logoutUseCase = LogoutUseCase(service, wordRepo, settingsRepo),
            deleteAccountUseCase = DeleteAccountUseCase(service, wordRepo, settingsRepo),
            subscriptionManager = fakeSubscriptionManager(),
            streakManager = fakeStreakManager(),
            deactivatePushTokenUseCase = deactivatePushTokenUseCase,
        )
    }

    @Test
    fun `logout deactivates only the current device push token not all devices`() = runTest {
        deactivateAllCalled = false
        deactivateCurrentCalled = false
        val manager = buildManager()

        manager.logout()

        assertTrue(deactivateCurrentCalled)
        assertFalse(deactivateAllCalled)
    }

    @Test
    fun `deleteAccount deactivates all device push tokens`() = runTest {
        deactivateAllCalled = false
        deactivateCurrentCalled = false
        val manager = buildManager()

        manager.deleteAccount()

        assertTrue(deactivateAllCalled)
        assertFalse(deactivateCurrentCalled)
    }
}
