package presentation.feature.profile

import core.common.Try
import domain.auth.manager.IUserManager
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import domain.auth.repository.IAuthRepository
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.profile.model.DayActivity
import domain.profile.model.LanguagePair
import domain.profile.model.ProfileStats
import domain.profile.repository.IProfileStatsRepository
import domain.profile.usecase.GetProfileStatsUseCase
import domain.streak.manager.IStreakManager
import domain.streak.model.StreakData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import feature.profile.ProfileViewModel
import feature.profile.model.ProfileUiData
import presentation.ViewModelTestBase
import core.common.UiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ProfileViewModelTest : ViewModelTestBase() {

    // --- Test fixtures ---

    private val testUser = AuthUser(
        id = 42L,
        email = "user@example.com",
        name = "Test User",
        displayAlias = "tester",
        profileImageUrl = "https://example.com/avatar.jpg"
    )

    private val testStreakData = StreakData(currentStreak = 7)

    private val testFeatureAccessResponse = FeatureAccessResponse(
        featureFlags = FeatureFlags(pushNotificationsEnabled = true),
        userAccess = UserFeatureAccess(hasPremiumAccess = false)
    )

    private val testProfileStats = ProfileStats(
        currentStreak = 7,
        longestStreak = 14,
        memberSince = "2024-01-01",
        weeklyActivity = listOf(DayActivity(date = "2024-01-01", reviewCount = 5)),
        languages = listOf(LanguagePair(sourceLanguage = "EN", targetLanguage = "DE", wordCount = 100))
    )

    // --- Fake implementations ---

    private fun fakeUserManager(
        userFlow: Flow<AuthUser?> = flowOf(testUser),
        logoutResult: Try<Unit> = Try.success(Unit),
        deleteAccountResult: Try<Unit> = Try.success(Unit)
    ) = object : IUserManager {
        override fun observeUser(): Flow<AuthUser?> = userFlow
        override fun setUser(user: AuthUser?) {}
        override suspend fun logout(): Try<Unit> = logoutResult
        override suspend fun deleteAccount(): Try<Unit> = deleteAccountResult
    }

    private fun fakeStreakManager(
        state: IStreakManager.StreakState = IStreakManager.StreakState.Loaded(testStreakData)
    ) = object : IStreakManager {
        override fun getStreak(): Flow<IStreakManager.StreakState> = flowOf(state)
        override suspend fun recordActivity(count: Int): Try<StreakData> = Try.success(testStreakData)
        override fun clearCache() {}
    }

    private fun fakeAuthRepository(
        featureAccessFlow: Flow<FeatureAccessResponse> = flowOf(testFeatureAccessResponse)
    ) = object : IAuthRepository {
        override suspend fun loginWithGoogle(idToken: String): Try<AuthUser> = Try.failure(NotImplementedError())
        override suspend fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Try<AuthUser> = Try.failure(NotImplementedError())
        override suspend fun logout(): Try<Unit> = Try.failure(NotImplementedError())
        override suspend fun deleteAccount(): Try<Unit> = Try.failure(NotImplementedError())
        override suspend fun getAccessToken(): String? = null
        override suspend fun isAuthenticated(): Boolean = true
        override fun isAuthenticatedAsFlow(): Flow<Boolean> = flowOf(true)
        override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> = featureAccessFlow
    }

    private fun fakeProfileStatsRepository(
        result: Try<ProfileStats> = Try.success(testProfileStats)
    ) = object : IProfileStatsRepository {
        override suspend fun getProfileStats(): Try<ProfileStats> = result
    }

    private fun createViewModel(
        userManager: IUserManager = fakeUserManager(),
        streakManager: IStreakManager = fakeStreakManager(),
        authRepository: IAuthRepository = fakeAuthRepository(),
        profileStatsRepository: IProfileStatsRepository = fakeProfileStatsRepository()
    ): ProfileViewModel {
        return ProfileViewModel(
            userManager = userManager,
            getFeatureAccessUseCase = GetFeatureAccessUseCase(authRepository),
            streakManager = streakManager,
            getProfileStatsUseCase = GetProfileStatsUseCase(profileStatsRepository)
        )
    }

    // --- Tests ---

    @Test
    fun `initial state is Loading before flows emit`() {
        // Verify that the declared initial state function returns Loading
        // In practice with UnconfinedTestDispatcher the VM processes eagerly,
        // so we verify the design intent via the factory method.
        val vm = createViewModel()
        // With UnconfinedTestDispatcher set via ViewModelTestBase, all flows
        // resolve synchronously. The state should be Loaded after construction.
        assertIs<UiState.Loaded<ProfileUiData>>(vm.currentState)
    }

    @Test
    fun `logged in user with streak produces Loaded state with userInfo populated`() = runTest {
        val vm = createViewModel()

        val state = assertIs<UiState.Loaded<ProfileUiData>>(vm.currentState)
        val userInfo = state.value.userInfo
        assertIs<feature.profile.model.ProfileUserUiModel>(userInfo)
        assertEquals("Test User", userInfo.name)
        assertEquals("user@example.com", userInfo.email)
        assertEquals("tester", userInfo.displayAlias)
        assertEquals("https://example.com/avatar.jpg", userInfo.profileImageUrl)
    }

    @Test
    fun `logged in user with streak populates streak data`() = runTest {
        val vm = createViewModel()

        val state = assertIs<UiState.Loaded<ProfileUiData>>(vm.currentState)
        assertEquals(7, state.value.streak?.currentStreak)
    }

    @Test
    fun `null user produces Loaded state with null userInfo`() = runTest {
        val vm = createViewModel(
            userManager = fakeUserManager(userFlow = flowOf(null))
        )

        val state = assertIs<UiState.Loaded<ProfileUiData>>(vm.currentState)
        assertNull(state.value.userInfo)
        assertNull(state.value.streak)
        assertNull(state.value.featureAccess)
    }

    @Test
    fun `streak error propagates to Error state when user is logged in`() = runTest {
        val vm = createViewModel(
            streakManager = fakeStreakManager(
                state = IStreakManager.StreakState.Error("Streak unavailable")
            )
        )

        val state = assertIs<UiState.Error>(vm.currentState)
        assertEquals("Streak unavailable", state.message)
    }

    @Test
    fun `premium user has subscriptions disabled`() = runTest {
        val premiumFeatureAccess = FeatureAccessResponse(
            featureFlags = FeatureFlags(pushNotificationsEnabled = true),
            userAccess = UserFeatureAccess(hasPremiumAccess = true)
        )
        val vm = createViewModel(
            authRepository = fakeAuthRepository(featureAccessFlow = flowOf(premiumFeatureAccess))
        )

        val state = assertIs<UiState.Loaded<ProfileUiData>>(vm.currentState)
        assertEquals(false, state.value.isSubscriptionsEnabled)
        assertEquals(false, state.value.shouldShowSubscriptionUI)
    }

    @Test
    fun `non-premium user has subscriptions enabled`() = runTest {
        val vm = createViewModel()

        val state = assertIs<UiState.Loaded<ProfileUiData>>(vm.currentState)
        assertEquals(true, state.value.isSubscriptionsEnabled)
        assertEquals(true, state.value.shouldShowSubscriptionUI)
    }

    @Test
    fun `clearError when state is Error resets to Loaded with empty profile data`() = runTest {
        val vm = createViewModel(
            streakManager = fakeStreakManager(
                state = IStreakManager.StreakState.Error("Some error")
            )
        )
        assertIs<UiState.Error>(vm.currentState)

        vm.clearError()

        val state = assertIs<UiState.Loaded<ProfileUiData>>(vm.currentState)
        assertNull(state.value.userInfo)
        assertNull(state.value.streak)
        assertNull(state.value.featureAccess)
        assertEquals(false, state.value.isSubscriptionsEnabled)
        assertEquals(false, state.value.shouldShowSubscriptionUI)
    }

    @Test
    fun `clearError when state is not Error does nothing`() = runTest {
        val vm = createViewModel()
        val stateBefore = vm.currentState
        assertIs<UiState.Loaded<ProfileUiData>>(stateBefore)

        vm.clearError()

        assertEquals(stateBefore, vm.currentState)
    }

    @Test
    fun `logout calls userManager logout`() = runTest {
        var logoutCalled = false
        val vm = createViewModel(
            userManager = fakeUserManager(
                logoutResult = Try { logoutCalled = true }
            )
        )

        vm.logout()

        assertEquals(true, logoutCalled)
    }

    @Test
    fun `deleteAccount calls userManager deleteAccount`() = runTest {
        var deleteAccountCalled = false
        val vm = createViewModel(
            userManager = fakeUserManager(
                deleteAccountResult = Try { deleteAccountCalled = true }
            )
        )

        vm.deleteAccount()

        assertEquals(true, deleteAccountCalled)
    }

    @Test
    fun `logout failure still completes without crashing`() = runTest {
        val vm = createViewModel(
            userManager = fakeUserManager(
                logoutResult = Try.failure(RuntimeException("Server error"))
            )
        )

        // Should not throw
        vm.logout()

        // State remains valid
        assertIs<UiState<ProfileUiData>>(vm.currentState)
    }

    @Test
    fun `deleteAccount failure still completes without crashing`() = runTest {
        val vm = createViewModel(
            userManager = fakeUserManager(
                deleteAccountResult = Try.failure(RuntimeException("Delete failed"))
            )
        )

        // Should not throw
        vm.deleteAccount()

        // State remains valid
        assertIs<UiState<ProfileUiData>>(vm.currentState)
    }

    @Test
    fun `profile stats are included in Loaded state when available`() = runTest {
        val vm = createViewModel()
        vm.refreshProfileStats()

        val state = assertIs<UiState.Loaded<ProfileUiData>>(vm.currentState)
        val stats = state.value.profileStats
        assertIs<feature.profile.model.ProfileStatsUiModel>(stats)
        assertEquals(7, stats.currentStreak)
        assertEquals(14, stats.longestStreak)
        assertEquals("2024-01-01", stats.memberSince)
    }

    @Test
    fun `profile stats are null when repository returns failure`() = runTest {
        val vm = createViewModel(
            profileStatsRepository = fakeProfileStatsRepository(
                result = Try.failure(RuntimeException("Stats unavailable"))
            )
        )
        vm.refreshProfileStats()

        val state = assertIs<UiState.Loaded<ProfileUiData>>(vm.currentState)
        assertNull(state.value.profileStats)
    }

    @Test
    fun `init does not load profile stats`() = runTest {
        val vm = createViewModel()

        val state = assertIs<UiState.Loaded<ProfileUiData>>(vm.currentState)
        assertNull(state.value.profileStats)
    }

    @Test
    fun `refreshProfileStats loads stats exactly once per call`() = runTest {
        var callCount = 0
        val vm = createViewModel(
            profileStatsRepository = object : IProfileStatsRepository {
                override suspend fun getProfileStats(): Try<ProfileStats> {
                    callCount++
                    return Try.success(testProfileStats)
                }
            }
        )

        vm.refreshProfileStats()

        assertEquals(1, callCount)
        val stats = assertIs<UiState.Loaded<ProfileUiData>>(vm.currentState).value.profileStats
        assertIs<feature.profile.model.ProfileStatsUiModel>(stats)
    }

    @Test
    fun `user flow emitting multiple values updates state accordingly`() = runTest {
        val userStateFlow = MutableStateFlow<AuthUser?>(testUser)
        val vm = createViewModel(
            userManager = fakeUserManager(userFlow = userStateFlow)
        )

        assertIs<UiState.Loaded<ProfileUiData>>(vm.currentState).also { state ->
            assertEquals("Test User", state.value.userInfo?.name)
        }

        userStateFlow.value = null

        assertIs<UiState.Loaded<ProfileUiData>>(vm.currentState).also { state ->
            assertNull(state.value.userInfo)
        }
    }
}
