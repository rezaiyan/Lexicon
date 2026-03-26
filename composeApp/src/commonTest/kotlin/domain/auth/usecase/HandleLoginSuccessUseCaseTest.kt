package domain.auth.usecase

import core.common.Try
import domain.auth.model.AuthUser
import domain.notifications.repository.IPushTokenRepository
import domain.notifications.usecase.InitializePushNotificationsUseCase
import domain.notifications.usecase.RegisterPushTokenUseCase
import domain.subscription.ISubscriptionManager
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionOffering
import domain.subscription.model.SubscriptionPackage
import domain.word.usecase.SyncRemoteToLocalUseCase
import fakes.FakeAuthRepository
import fakes.FakePushTokenRepository
import fakes.FakeWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HandleLoginSuccessUseCaseTest {

    private val testUser = AuthUser(id = 42L, email = "user@example.com", name = "Test User")

    private val fakeWordRepository = FakeWordRepository()
    // authenticated=true so InitializePushNotificationsUseCase actually calls initializeAndRegister
    private val fakeAuthRepository = FakeAuthRepository().also { it.authenticated = true }
    private val fakePushTokenRepository = FakePushTokenRepository()

    private var subscriptionLogInResult: Try<SubscriptionCustomerInfo> =
        Try.success(SubscriptionCustomerInfo(activeEntitlements = emptyMap()))
    private var lastSubscriptionUserId: String? = null

    private fun buildFakeSubscriptionManager(): ISubscriptionManager =
        object : ISubscriptionManager {
            override val customerInfo: StateFlow<SubscriptionCustomerInfo?> =
                MutableStateFlow(null)

            override suspend fun getOfferings(): Try<SubscriptionOffering> =
                throw NotImplementedError()

            override suspend fun purchase(packageToPurchase: SubscriptionPackage): Try<SubscriptionCustomerInfo> =
                throw NotImplementedError()

            override suspend fun restore(): Try<SubscriptionCustomerInfo> =
                throw NotImplementedError()

            override fun isSubscribed(): Flow<Boolean> = throw NotImplementedError()

            override suspend fun logIn(userId: String): Try<SubscriptionCustomerInfo> {
                lastSubscriptionUserId = userId
                return subscriptionLogInResult
            }

            override suspend fun logOut(): Try<SubscriptionCustomerInfo> =
                throw NotImplementedError()

            override fun getCurrentCustomerInfo(): SubscriptionCustomerInfo? = null

            override suspend fun manageSubscription(): Try<Unit> = throw NotImplementedError()

            override suspend fun cancelSubscription(): Try<Unit> = throw NotImplementedError()
        }

    private fun buildUseCase(
        pushTokenRepo: IPushTokenRepository = fakePushTokenRepository,
    ) = HandleLoginSuccessUseCase(
        subscriptionManager = buildFakeSubscriptionManager(),
        syncRemoteToLocalUseCase = SyncRemoteToLocalUseCase(fakeWordRepository),
        initializePushNotificationsUseCase = InitializePushNotificationsUseCase(
            IsAuthenticatedUseCase(fakeAuthRepository),
            RegisterPushTokenUseCase(pushTokenRepo),
        ),
    )

    @Test
    fun `success when all dependencies succeed`() = runTest {
        val useCase = buildUseCase()

        val result = useCase(HandleLoginSuccessUseCase.Params(user = testUser, syncData = true))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `success with syncData=false does not call sync`() = runTest {
        val useCase = buildUseCase()

        val result = useCase(HandleLoginSuccessUseCase.Params(user = testUser, syncData = false))

        assertTrue(result.isSuccess)
        assertFalse(fakeWordRepository.syncRemoteToLocalCalled, "sync should NOT be called when syncData=false")
        assertFalse(lastSubscriptionUserId == null, "subscription logIn should still be called")
        assertTrue(fakePushTokenRepository.initializeAndRegisterCalled, "notifications should still be initialized")
    }

    @Test
    fun `failure when subscriptionManager logIn fails propagates`() = runTest {
        subscriptionLogInResult = Try.failure(RuntimeException("Subscription login error"))
        val useCase = buildUseCase()

        val result = useCase(HandleLoginSuccessUseCase.Params(user = testUser))

        assertTrue(result.isFailure)
    }

    @Test
    fun `failure when sync fails propagates`() = runTest {
        fakeWordRepository.syncResult = Try.failure(RuntimeException("Sync error"))
        val useCase = buildUseCase()

        val result = useCase(HandleLoginSuccessUseCase.Params(user = testUser, syncData = true))

        assertTrue(result.isFailure)
    }

    @Test
    fun `failure when initializePush fails propagates`() = runTest {
        val throwingPushRepo = object : IPushTokenRepository {
            override fun initializeAndRegister() { throw RuntimeException("Push notification error") }
            override suspend fun registerToken(token: String): Try<Unit> = Try.success(Unit)
            override suspend fun deactivateAllTokens(): Try<Unit> = Try.success(Unit)
        }
        val useCase = buildUseCase(pushTokenRepo = throwingPushRepo)

        val result = useCase(HandleLoginSuccessUseCase.Params(user = testUser))

        assertTrue(result.isFailure)
    }

    @Test
    fun `user ID is passed to subscriptionManager as string`() = runTest {
        val user = AuthUser(id = 99L, email = "someone@example.com", name = "Someone")
        val useCase = buildUseCase()

        useCase(HandleLoginSuccessUseCase.Params(user = user))

        assertEquals("99", lastSubscriptionUserId)
    }

    @Test
    fun `sync is called when syncData=true`() = runTest {
        val useCase = buildUseCase()

        useCase(HandleLoginSuccessUseCase.Params(user = testUser, syncData = true))

        assertTrue(fakeWordRepository.syncRemoteToLocalCalled, "sync SHOULD be called when syncData=true")
    }
}
