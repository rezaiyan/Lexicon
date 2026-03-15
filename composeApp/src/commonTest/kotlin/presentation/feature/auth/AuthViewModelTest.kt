package presentation.feature.auth

import feature.auth.AuthViewModel
import analytics.IAnalyticsTracker
import core.common.Try
import domain.auth.manager.IUserManager
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import domain.auth.repository.IAuthRepository
import domain.auth.repository.ISessionRepository
import domain.auth.repository.SessionVerificationResult
import domain.auth.service.AuthenticationService
import domain.auth.service.IAuthenticationService
import domain.auth.usecase.IsAuthenticatedUseCase
import domain.auth.usecase.LoginWithAppleUseCase
import domain.auth.usecase.LoginWithGoogleUseCase
import domain.auth.usecase.LogoutUseCase
import domain.auth.usecase.VerifySessionUseCase
import domain.notifications.repository.IPushTokenRepository
import domain.notifications.usecase.InitializePushNotificationsUseCase
import domain.notifications.usecase.RegisterPushTokenUseCase
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.subscription.ISubscriptionManager
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionOffering
import domain.subscription.model.SubscriptionPackage
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.usecase.SyncRemoteToLocalUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest : ViewModelTestBase() {

    // --- Fakes ---

    private var isAuthenticatedFlow: Flow<Boolean> = flowOf(false)
    private var sessionResult: SessionVerificationResult = SessionVerificationResult.NotAuthenticated
    private var loginResult: Try<AuthUser> = Try.failure(RuntimeException("not configured"))

    private fun testUser(id: Long = 1L) = AuthUser(
        id = id,
        email = "test@example.com",
        name = "Test User"
    )

    private fun fakeAuthRepo() = object : IAuthRepository {
        override suspend fun loginWithGoogle(idToken: String): Try<AuthUser> = loginResult
        override suspend fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Try<AuthUser> = loginResult
        override suspend fun logout(): Try<Unit> = Try.success(Unit)
        override suspend fun deleteAccount(): Try<Unit> = Try.success(Unit)
        override suspend fun getAccessToken(): String? = null
        override suspend fun isAuthenticated(): Boolean = loginResult.isSuccess
        override fun isAuthenticatedAsFlow(): Flow<Boolean> = isAuthenticatedFlow
        override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> = flowOf(
            FeatureAccessResponse(FeatureFlags(), UserFeatureAccess(hasPremiumAccess = false))
        )
    }

    private fun fakeSessionRepo(result: SessionVerificationResult) = object : ISessionRepository {
        override suspend fun verifySession(): SessionVerificationResult = result
    }

    private fun fakeWordRepo() = object : IWordRepository {
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
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
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
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

    private fun fakePushTokenRepo() = object : IPushTokenRepository {
        override suspend fun registerToken(token: String): Try<Unit> = Try.success(Unit)
        override suspend fun deactivateAllTokens(): Try<Unit> = Try.success(Unit)
        override fun initializeAndRegister() {}
    }

    private val setUserCalls = mutableListOf<AuthUser?>()

    private fun fakeUserManager() = object : IUserManager {
        override fun observeUser(): Flow<AuthUser?> = flowOf(null)
        override fun setUser(user: AuthUser?) { setUserCalls += user }
        override suspend fun logout(): Try<Unit> = Try.success(Unit)
        override suspend fun deleteAccount(): Try<Unit> = Try.success(Unit)
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

    private fun fakeAnalytics() = object : IAnalyticsTracker {
        override fun logScreenView(screenName: String) {}
        override fun logEvent(eventName: String, parameters: Map<String, Any>?) {}
        override fun logWordReviewed(rating: Int, wordLevel: Int, wasCorrect: Boolean) {}
        override fun logReviewSessionStart(cardCount: Int) {}
        override fun logReviewSessionComplete(cardsReviewed: Int, durationMs: Long, perfectCount: Int) {}
        override fun logWordsImported(count: Int, method: String) {}
        override fun logWordMastered(level: Int) {}
        override fun logStreakUpdated(days: Int, isNewRecord: Boolean) {}
        override fun logDailyGoalCompleted(cardsTarget: Int, cardsActual: Int) {}
        override fun logThemeChanged(themeMode: String, isDark: Boolean) {}
        override fun logLanguageChanged(language: String) {}
        override fun setUserProperty(name: String, value: String) {}
        override fun updateUserProgress(totalWords: Int, matureWords: Int, currentStreak: Int) {}
        override fun logError(error: Throwable, context: String?) {}
        override fun logNonFatalError(message: String, additionalInfo: Map<String, Any>?) {}
    }

    private fun createViewModel(
        sessionVerificationResult: SessionVerificationResult = SessionVerificationResult.NotAuthenticated,
        authService: IAuthenticationService? = null,
    ): AuthViewModel {
        val authRepo = fakeAuthRepo()
        val service = authService ?: AuthenticationService(authRepo)
        val wordRepo = fakeWordRepo()
        val settingsRepo = fakeSettingsRepo()
        val pushTokenRepo = fakePushTokenRepo()
        val isAuthUseCase = IsAuthenticatedUseCase(authRepo)
        val registerPushTokenUseCase = RegisterPushTokenUseCase(pushTokenRepo)
        return AuthViewModel(
            loginWithGoogleUseCase = LoginWithGoogleUseCase(service),
            loginWithAppleUseCase = LoginWithAppleUseCase(service),
            logoutUseCase = LogoutUseCase(service, wordRepo, settingsRepo),
            isAuthenticatedUseCase = isAuthUseCase,
            verifySessionUseCase = VerifySessionUseCase(fakeSessionRepo(sessionVerificationResult)),
            syncRemoteToLocalUseCase = SyncRemoteToLocalUseCase(wordRepo),
            initializePushNotificationsUseCase = InitializePushNotificationsUseCase(isAuthUseCase, registerPushTokenUseCase),
            registerPushTokenUseCase = registerPushTokenUseCase,
            analyticsTracker = fakeAnalytics(),
            userManager = fakeUserManager(),
            subscriptionManager = fakeSubscriptionManager(),
        )
    }

    // --- Tests ---

    @Test
    fun `initial state has all defaults`() {
        val vm = createViewModel()
        val state = vm.currentState
        assertFalse(state.isAuthenticated)
        assertNull(state.user)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `verifyAndRestoreSession with Valid result sets authenticated user`() = runTest {
        val user = testUser()
        isAuthenticatedFlow = flowOf(true)
        val vm = createViewModel(sessionVerificationResult = SessionVerificationResult.Valid(user))
        // init block already called verifyAndRestoreSession; check state reflects valid session
        assertTrue(vm.currentState.isAuthenticated)
        assertEquals(user, vm.currentState.user)
        assertFalse(vm.currentState.isLoading)
        assertNull(vm.currentState.error)
    }

    @Test
    fun `verifyAndRestoreSession with NotAuthenticated clears authentication`() = runTest {
        val vm = createViewModel(sessionVerificationResult = SessionVerificationResult.NotAuthenticated)
        assertFalse(vm.currentState.isAuthenticated)
        assertNull(vm.currentState.user)
        assertFalse(vm.currentState.isLoading)
    }

    @Test
    fun `verifyAndRestoreSession with Expired keeps loading false and does not set user`() = runTest {
        val vm = createViewModel(sessionVerificationResult = SessionVerificationResult.Expired)
        assertFalse(vm.currentState.isLoading)
        assertNull(vm.currentState.user)
    }

    @Test
    fun `verifyAndRestoreSession with ServerError sets isAuthenticated true gracefully`() = runTest {
        isAuthenticatedFlow = flowOf(true)
        val vm = createViewModel(sessionVerificationResult = SessionVerificationResult.ServerError)
        assertTrue(vm.currentState.isAuthenticated)
        assertFalse(vm.currentState.isLoading)
        // No user is available from a server error, so user remains null
        assertNull(vm.currentState.user)
    }

    @Test
    fun `loginWithGoogle success sets authenticated user and clears loading`() = runTest {
        val user = testUser()
        loginResult = Try.success(user)
        val vm = createViewModel()
        vm.loginWithGoogle("valid-id-token")
        assertTrue(vm.currentState.isAuthenticated)
        assertEquals(user, vm.currentState.user)
        assertFalse(vm.currentState.isLoading)
        assertNull(vm.currentState.error)
    }

    @Test
    fun `loginWithGoogle failure sets error and clears authentication`() = runTest {
        loginResult = Try.failure(RuntimeException("network error"))
        val vm = createViewModel()
        vm.loginWithGoogle("bad-token")
        assertFalse(vm.currentState.isAuthenticated)
        assertFalse(vm.currentState.isLoading)
        assertNotNull(vm.currentState.error)
    }

    @Test
    fun `loginWithApple success sets authenticated user and clears loading`() = runTest {
        val user = testUser(id = 2L)
        loginResult = Try.success(user)
        val vm = createViewModel()
        vm.loginWithApple("apple-token", "Jane Doe", "apple-user-id")
        assertTrue(vm.currentState.isAuthenticated)
        assertEquals(user, vm.currentState.user)
        assertFalse(vm.currentState.isLoading)
        assertNull(vm.currentState.error)
    }

    @Test
    fun `loginWithApple failure sets error and clears authentication`() = runTest {
        loginResult = Try.failure(RuntimeException("apple auth failed"))
        val vm = createViewModel()
        vm.loginWithApple("bad-apple-token", null, "apple-user-id")
        assertFalse(vm.currentState.isAuthenticated)
        assertFalse(vm.currentState.isLoading)
        assertNotNull(vm.currentState.error)
    }

    @Test
    fun `logout resets state to not authenticated`() = runTest {
        // First get into an authenticated state
        val user = testUser()
        loginResult = Try.success(user)
        val vm = createViewModel()
        vm.loginWithGoogle("valid-id-token")
        assertTrue(vm.currentState.isAuthenticated)

        vm.logout()

        assertFalse(vm.currentState.isAuthenticated)
        assertFalse(vm.currentState.isLoading)
    }

    @Test
    fun `loginWithGoogle success calls setUser on userManager`() = runTest {
        val user = testUser()
        loginResult = Try.success(user)
        val vm = createViewModel()
        val initialSetUserCount = setUserCalls.size
        vm.loginWithGoogle("valid-id-token")
        val newCalls = setUserCalls.drop(initialSetUserCount)
        assertTrue(newCalls.contains(user))
    }

    @Test
    fun `verifyAndRestoreSession with Valid result calls setUser on userManager`() = runTest {
        val user = testUser()
        isAuthenticatedFlow = flowOf(true)
        createViewModel(sessionVerificationResult = SessionVerificationResult.Valid(user))
        assertTrue(setUserCalls.contains(user))
    }

    @Test
    fun `verifyAndRestoreSession with Valid result invokes onComplete callback`() = runTest {
        var callbackInvoked = false
        val user = testUser()
        isAuthenticatedFlow = flowOf(true)
        val vm = createViewModel(sessionVerificationResult = SessionVerificationResult.Valid(user))
        vm.verifyAndRestoreSession { callbackInvoked = true }
        assertTrue(callbackInvoked)
    }

    @Test
    fun `verifyAndRestoreSession with NotAuthenticated invokes onComplete callback`() = runTest {
        var callbackInvoked = false
        val vm = createViewModel(sessionVerificationResult = SessionVerificationResult.NotAuthenticated)
        vm.verifyAndRestoreSession { callbackInvoked = true }
        assertTrue(callbackInvoked)
    }
}
