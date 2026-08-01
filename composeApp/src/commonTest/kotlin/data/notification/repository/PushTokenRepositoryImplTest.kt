package data.notification.repository

import core.common.Try
import data.notification.remote.IPushNotificationDataSource
import data.notification.remote.model.Platform
import data.notification.remote.model.RegisterPushTokenRequest
import data.storage.SecureStorage
import domain.notifications.repository.IPushTokenRepository
import kotlinx.coroutines.test.runTest
import pushnotification.IPushTokenManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PushTokenRepositoryImplTest {

    private fun buildRepo(
        tokenManager: FakePushTokenManager = FakePushTokenManager(),
        dataSource: FakePushNotificationDataSource = FakePushNotificationDataSource(),
        secureStorage: FakeSecureStorage = FakeSecureStorage(),
        platform: Platform = Platform.ANDROID
    ): Triple<IPushTokenRepository, FakePushNotificationDataSource, FakeSecureStorage> {
        val repo = PushTokenRepositoryImpl(tokenManager, dataSource, secureStorage, platform)
        return Triple(repo, dataSource, secureStorage)
    }

    // --- initializeAndRegister (BUG-1) ---

    @Test
    fun `initializeAndRegister does not call registerToken before callback fires`() = runTest {
        val dataSource = FakePushNotificationDataSource()
        val tokenManager = FakePushTokenManager(currentToken = "existing-token")
        val (repo, _, _) = buildRepo(tokenManager = tokenManager, dataSource = dataSource)

        repo.initializeAndRegister()

        // Old buggy code had an immediate getCurrentToken() path; it's gone now.
        assertEquals(0, dataSource.registerCalls.size)
        assertTrue(tokenManager.initializeCalled)
    }

    @Test
    fun `initializeAndRegister sets up callback on token manager`() = runTest {
        val tokenManager = FakePushTokenManager()
        val (repo, _, _) = buildRepo(tokenManager = tokenManager)

        repo.initializeAndRegister()

        assertTrue(tokenManager.initializeCalled)
    }

    // --- registerToken ---

    @Test
    fun `registerToken sends correct request to data source`() = runTest {
        val dataSource = FakePushNotificationDataSource()
        val (repo, _, _) = buildRepo(dataSource = dataSource, platform = Platform.IOS)

        repo.registerToken("test-fcm-token")

        assertEquals(1, dataSource.registerCalls.size)
        val request = dataSource.registerCalls.first()
        assertEquals("test-fcm-token", request.token)
        assertEquals(Platform.IOS, request.platform)
        assertEquals(null, request.deviceId)
    }

    @Test
    fun `registerToken returns success when data source succeeds`() = runTest {
        val (repo, _, _) = buildRepo()

        val result = repo.registerToken("token-123")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `registerToken returns failure when data source fails`() = runTest {
        val dataSource = FakePushNotificationDataSource(registerResult = Try.failure(RuntimeException("Network error")))
        val (repo, _, _) = buildRepo(dataSource = dataSource)

        val result = repo.registerToken("token-123")

        assertTrue(result.isFailure)
    }

    @Test
    fun `registerToken uses ANDROID platform by default`() = runTest {
        val dataSource = FakePushNotificationDataSource()
        val (repo, _, _) = buildRepo(dataSource = dataSource, platform = Platform.ANDROID)

        repo.registerToken("token")

        assertEquals(Platform.ANDROID, dataSource.registerCalls.first().platform)
    }

    @Test
    fun `registerToken persists the token locally on success`() = runTest {
        val secureStorage = FakeSecureStorage()
        val (repo, _, _) = buildRepo(secureStorage = secureStorage)

        repo.registerToken("persisted-token")

        assertEquals("persisted-token", secureStorage.storedPushToken)
    }

    @Test
    fun `registerToken does not persist the token on failure`() = runTest {
        val secureStorage = FakeSecureStorage()
        val dataSource = FakePushNotificationDataSource(registerResult = Try.failure(RuntimeException("Network error")))
        val (repo, _, _) = buildRepo(dataSource = dataSource, secureStorage = secureStorage)

        repo.registerToken("token-123")

        assertNull(secureStorage.storedPushToken)
    }

    // --- deactivateAllTokens ---

    @Test
    fun `deactivateAllTokens delegates to data source`() = runTest {
        val dataSource = FakePushNotificationDataSource()
        val (repo, _, _) = buildRepo(dataSource = dataSource)

        val result = repo.deactivateAllTokens()

        assertTrue(result.isSuccess)
        assertTrue(dataSource.deactivateAllCalled)
    }

    @Test
    fun `deactivateAllTokens returns failure when data source fails`() = runTest {
        val dataSource = FakePushNotificationDataSource(
            deactivateResult = Try.failure(RuntimeException("Server error"))
        )
        val (repo, _, _) = buildRepo(dataSource = dataSource)

        val result = repo.deactivateAllTokens()

        assertTrue(result.isFailure)
    }

    @Test
    fun `deactivateAllTokens clears the persisted token on success`() = runTest {
        val secureStorage = FakeSecureStorage(storedPushToken = "stored-token")
        val (repo, _, _) = buildRepo(secureStorage = secureStorage)

        repo.deactivateAllTokens()

        assertNull(secureStorage.storedPushToken)
    }

    // --- deactivateCurrentToken ---

    @Test
    fun `deactivateCurrentToken prefers the persisted registered token over the live platform token`() = runTest {
        val dataSource = FakePushNotificationDataSource()
        val tokenManager = FakePushTokenManager(currentToken = "rotated-live-token")
        val secureStorage = FakeSecureStorage(storedPushToken = "actually-registered-token")
        val (repo, _, _) = buildRepo(tokenManager = tokenManager, dataSource = dataSource, secureStorage = secureStorage)

        val result = repo.deactivateCurrentToken()

        assertTrue(result.isSuccess)
        assertEquals("actually-registered-token", dataSource.deactivateTokenCalls.single())
    }

    @Test
    fun `deactivateCurrentToken falls back to the live platform token when nothing is persisted`() = runTest {
        val dataSource = FakePushNotificationDataSource()
        val tokenManager = FakePushTokenManager(currentToken = "device-token-xyz")
        val secureStorage = FakeSecureStorage(storedPushToken = null)
        val (repo, _, _) = buildRepo(tokenManager = tokenManager, dataSource = dataSource, secureStorage = secureStorage)

        val result = repo.deactivateCurrentToken()

        assertTrue(result.isSuccess)
        assertEquals("device-token-xyz", dataSource.deactivateTokenCalls.single())
        assertFalse(dataSource.deactivateAllCalled)
    }

    @Test
    fun `deactivateCurrentToken is a no-op when no token is available anywhere`() = runTest {
        val dataSource = FakePushNotificationDataSource()
        val tokenManager = FakePushTokenManager(currentToken = null)
        val secureStorage = FakeSecureStorage(storedPushToken = null)
        val (repo, _, _) = buildRepo(tokenManager = tokenManager, dataSource = dataSource, secureStorage = secureStorage)

        val result = repo.deactivateCurrentToken()

        assertTrue(result.isSuccess)
        assertTrue(dataSource.deactivateTokenCalls.isEmpty())
    }

    @Test
    fun `deactivateCurrentToken returns failure when data source fails`() = runTest {
        val dataSource = FakePushNotificationDataSource(
            deactivateTokenResult = Try.failure(RuntimeException("Server error"))
        )
        val tokenManager = FakePushTokenManager(currentToken = "device-token-xyz")
        val (repo, _, _) = buildRepo(tokenManager = tokenManager, dataSource = dataSource)

        val result = repo.deactivateCurrentToken()

        assertTrue(result.isFailure)
    }

    @Test
    fun `deactivateCurrentToken clears the persisted token on success`() = runTest {
        val secureStorage = FakeSecureStorage(storedPushToken = "stored-token")
        val (repo, _, _) = buildRepo(secureStorage = secureStorage)

        repo.deactivateCurrentToken()

        assertNull(secureStorage.storedPushToken)
    }

    @Test
    fun `deactivateCurrentToken does not clear the persisted token on failure`() = runTest {
        val dataSource = FakePushNotificationDataSource(
            deactivateTokenResult = Try.failure(RuntimeException("Server error"))
        )
        val secureStorage = FakeSecureStorage(storedPushToken = "stored-token")
        val (repo, _, _) = buildRepo(dataSource = dataSource, secureStorage = secureStorage)

        repo.deactivateCurrentToken()

        assertEquals("stored-token", secureStorage.storedPushToken)
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
        private val deactivateResult: Try<Unit> = Try.success(Unit),
        private val deactivateTokenResult: Try<Unit> = Try.success(Unit)
    ) : IPushNotificationDataSource {
        val registerCalls = mutableListOf<RegisterPushTokenRequest>()
        val deactivateTokenCalls = mutableListOf<String>()
        var deactivateAllCalled = false

        override suspend fun registerPushToken(request: RegisterPushTokenRequest): Try<Unit> {
            registerCalls.add(request)
            return registerResult
        }

        override suspend fun deactivateAllTokens(): Try<Unit> {
            deactivateAllCalled = true
            return deactivateResult
        }

        override suspend fun deactivateToken(token: String): Try<Unit> {
            deactivateTokenCalls.add(token)
            return deactivateTokenResult
        }
    }

    private class FakeSecureStorage(
        var storedPushToken: String? = null
    ) : SecureStorage {
        override suspend fun saveAccessToken(token: String) {}
        override suspend fun saveRefreshToken(token: String) {}
        override fun getAccessToken(): String? = null
        override suspend fun getRefreshToken(): String? = null
        override suspend fun clearTokens() {}
        override suspend fun saveTokenExpiresAt(expiresAtMs: Long) {}
        override fun getTokenExpiresAt(): Long = 0L
        override suspend fun hasCompletedOnboarding(): Boolean = false
        override suspend fun markOnboardingCompleted() {}
        override suspend fun savePushToken(token: String) { storedPushToken = token }
        override fun getPushToken(): String? = storedPushToken
        override suspend fun clearPushToken() { storedPushToken = null }
    }
}
