package data.notification.repository

import core.common.Try
import data.notification.remote.IPushNotificationDataSource
import data.notification.remote.model.Platform
import data.notification.remote.model.RegisterPushTokenRequest
import domain.notifications.repository.IPushTokenRepository
import kotlinx.coroutines.test.runTest
import pushnotification.IPushTokenManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PushTokenRepositoryImplTest {

    private fun buildRepo(
        tokenManager: FakePushTokenManager = FakePushTokenManager(),
        dataSource: FakePushNotificationDataSource = FakePushNotificationDataSource(),
        platform: Platform = Platform.ANDROID
    ): Pair<IPushTokenRepository, FakePushNotificationDataSource> {
        val repo = PushTokenRepositoryImpl(tokenManager, dataSource, platform)
        return repo to dataSource
    }

    // --- registerToken ---

    @Test
    fun `registerToken sends correct request to data source`() = runTest {
        val dataSource = FakePushNotificationDataSource()
        val (repo, _) = buildRepo(dataSource = dataSource, platform = Platform.IOS)

        repo.registerToken("test-fcm-token")

        assertEquals(1, dataSource.registerCalls.size)
        val request = dataSource.registerCalls.first()
        assertEquals("test-fcm-token", request.token)
        assertEquals(Platform.IOS, request.platform)
        assertEquals(null, request.deviceId)
    }

    @Test
    fun `registerToken returns success when data source succeeds`() = runTest {
        val (repo, _) = buildRepo()

        val result = repo.registerToken("token-123")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `registerToken returns failure when data source fails`() = runTest {
        val dataSource = FakePushNotificationDataSource(registerResult = Try.failure(RuntimeException("Network error")))
        val (repo, _) = buildRepo(dataSource = dataSource)

        val result = repo.registerToken("token-123")

        assertTrue(result.isFailure)
    }

    @Test
    fun `registerToken uses ANDROID platform by default`() = runTest {
        val dataSource = FakePushNotificationDataSource()
        val (repo, _) = buildRepo(dataSource = dataSource, platform = Platform.ANDROID)

        repo.registerToken("token")

        assertEquals(Platform.ANDROID, dataSource.registerCalls.first().platform)
    }

    // --- deactivateAllTokens ---

    @Test
    fun `deactivateAllTokens delegates to data source`() = runTest {
        val dataSource = FakePushNotificationDataSource()
        val (repo, _) = buildRepo(dataSource = dataSource)

        val result = repo.deactivateAllTokens()

        assertTrue(result.isSuccess)
        assertTrue(dataSource.deactivateAllCalled)
    }

    @Test
    fun `deactivateAllTokens returns failure when data source fails`() = runTest {
        val dataSource = FakePushNotificationDataSource(
            deactivateResult = Try.failure(RuntimeException("Server error"))
        )
        val (repo, _) = buildRepo(dataSource = dataSource)

        val result = repo.deactivateAllTokens()

        assertTrue(result.isFailure)
    }

    // --- Fakes ---

    private class FakePushTokenManager(
        private val currentToken: String? = null
    ) : IPushTokenManager {
        var initializeCalled = false
        var lastCallback: ((String) -> Unit)? = null

        override fun initialize(onTokenReceived: (String) -> Unit) {
            initializeCalled = true
            lastCallback = onTokenReceived
        }

        override suspend fun getCurrentToken(): String? = currentToken
    }

    private class FakePushNotificationDataSource(
        private val registerResult: Try<Unit> = Try.success(Unit),
        private val deactivateResult: Try<Unit> = Try.success(Unit)
    ) : IPushNotificationDataSource {
        val registerCalls = mutableListOf<RegisterPushTokenRequest>()
        var deactivateAllCalled = false

        override suspend fun registerPushToken(request: RegisterPushTokenRequest): Try<Unit> {
            registerCalls.add(request)
            return registerResult
        }

        override suspend fun deactivateAllTokens(): Try<Unit> {
            deactivateAllCalled = true
            return deactivateResult
        }
    }
}
