package data.network.interceptor

import core.common.Try
import data.auth.refresh.ITokenRefreshManager
import data.auth.token.ITokenManager
import data.core.network.interceptor.AuthInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock

/**
 * Tests for AuthInterceptor: verifies that it adds Bearer tokens to private endpoints,
 * skips public endpoints, and proactively refreshes tokens about to expire.
 */
class AuthInterceptorTest {

    // -------------------------------------------------------------------------
    // Fakes
    // -------------------------------------------------------------------------

    private class FakeTokenManager(
        var accessToken: String? = "test-access-token",
        var expiresAt: Long = 0L
    ) : ITokenManager {
        override suspend fun saveTokens(accessToken: String, refreshToken: String, expiresInMs: Long) {}
        override suspend fun getAccessToken(): String? = accessToken
        override suspend fun getRefreshToken(): String? = null
        override suspend fun clearTokens() { accessToken = null }
        override suspend fun hasTokens(): Boolean = accessToken != null
        override fun getTokenExpiresAt(): Long = expiresAt
    }

    private class FakeTokenRefreshManager(
        private var refreshResult: Try<String> = Try.success("new-access-token"),
        var refreshCallCount: Int = 0,
        var onRefreshCalled: (() -> Unit)? = null
    ) : ITokenRefreshManager {
        override suspend fun refresh(): Try<String> {
            refreshCallCount++
            onRefreshCalled?.invoke()
            return refreshResult
        }
        override suspend fun clearSession() {}

        fun setRefreshResult(result: Try<String>) { refreshResult = result }
    }

    private fun jsonHeaders() =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun buildClient(
        engine: MockEngine,
        tokenManager: ITokenManager,
        refreshManagerProvider: (() -> ITokenRefreshManager)? = null
    ): HttpClient = HttpClient(engine) {
        install(
            AuthInterceptor(
                tokenManager = tokenManager,
                tokenRefreshManagerProvider = refreshManagerProvider
            ).createPlugin()
        )
    }

    // -------------------------------------------------------------------------
    // Public endpoints — no Authorization header
    // -------------------------------------------------------------------------

    @Test
    fun `public endpoint auth google does not receive Authorization header`() = runTest {
        var capturedAuthHeader: String? = "SENTINEL"
        val engine = MockEngine { request ->
            capturedAuthHeader = request.headers[HttpHeaders.Authorization]
            respond("""{"ok":true}""", HttpStatusCode.OK, jsonHeaders())
        }
        val client = buildClient(engine, FakeTokenManager())

        client.get("https://api.test/auth/google")

        assertNull(capturedAuthHeader)
    }

    @Test
    fun `public endpoint auth apple does not receive Authorization header`() = runTest {
        var capturedAuthHeader: String? = "SENTINEL"
        val engine = MockEngine { request ->
            capturedAuthHeader = request.headers[HttpHeaders.Authorization]
            respond("""{"ok":true}""", HttpStatusCode.OK, jsonHeaders())
        }
        val client = buildClient(engine, FakeTokenManager())

        client.get("https://api.test/auth/apple")

        assertNull(capturedAuthHeader)
    }

    @Test
    fun `public endpoint auth refresh does not receive Authorization header`() = runTest {
        var capturedAuthHeader: String? = "SENTINEL"
        val engine = MockEngine { request ->
            capturedAuthHeader = request.headers[HttpHeaders.Authorization]
            respond("""{"ok":true}""", HttpStatusCode.OK, jsonHeaders())
        }
        val client = buildClient(engine, FakeTokenManager())

        client.get("https://api.test/auth/refresh")

        assertNull(capturedAuthHeader)
    }

    // -------------------------------------------------------------------------
    // Private endpoints — Bearer token added
    // -------------------------------------------------------------------------

    @Test
    fun `private endpoint receives Authorization Bearer token`() = runTest {
        var capturedAuthHeader: String? = null
        val engine = MockEngine { request ->
            capturedAuthHeader = request.headers[HttpHeaders.Authorization]
            respond("""{"data":"ok"}""", HttpStatusCode.OK, jsonHeaders())
        }
        val client = buildClient(engine, FakeTokenManager(accessToken = "my-secret-token"))

        client.get("https://api.test/words")

        assertEquals("Bearer my-secret-token", capturedAuthHeader)
    }

    @Test
    fun `private endpoint with null token does not add Authorization header`() = runTest {
        var capturedAuthHeader: String? = "SENTINEL"
        val engine = MockEngine { request ->
            capturedAuthHeader = request.headers[HttpHeaders.Authorization]
            respond("""{}""", HttpStatusCode.OK, jsonHeaders())
        }
        val client = buildClient(engine, FakeTokenManager(accessToken = null))

        client.get("https://api.test/profile")

        assertNull(capturedAuthHeader)
    }

    // -------------------------------------------------------------------------
    // No duplicate Authorization headers
    // -------------------------------------------------------------------------

    @Test
    fun `no duplicate Authorization headers added to private endpoint`() = runTest {
        val capturedHeaders = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedHeaders.addAll(request.headers.getAll(HttpHeaders.Authorization) ?: emptyList())
            respond("""{}""", HttpStatusCode.OK, jsonHeaders())
        }
        val client = buildClient(engine, FakeTokenManager(accessToken = "token-abc"))

        client.get("https://api.test/profile")

        assertEquals(1, capturedHeaders.size)
        assertEquals("Bearer token-abc", capturedHeaders.first())
    }

    // -------------------------------------------------------------------------
    // Proactive refresh: token expiring within 5 minutes
    // -------------------------------------------------------------------------

    @Test
    fun `token expiring within 5 minutes triggers proactive refresh`() = runTest {
        val refreshManager = FakeTokenRefreshManager(
            refreshResult = Try.success("new-proactive-token")
        )
        val engine = MockEngine { respond("""{}""", HttpStatusCode.OK, jsonHeaders()) }
        val now = Clock.System.now().toEpochMilliseconds()
        val tokenManager = FakeTokenManager(
            accessToken = "old-token",
            expiresAt = now + 3 * 60 * 1000L  // 3 minutes — within 5-minute threshold
        )
        val client = buildClient(engine, tokenManager, refreshManagerProvider = { refreshManager })

        client.get("https://api.test/words")

        assertEquals(1, refreshManager.refreshCallCount)
    }

    @Test
    fun `token expiring in more than 5 minutes does not trigger proactive refresh`() = runTest {
        val refreshManager = FakeTokenRefreshManager()
        val engine = MockEngine { respond("""{}""", HttpStatusCode.OK, jsonHeaders()) }
        val now = Clock.System.now().toEpochMilliseconds()
        val tokenManager = FakeTokenManager(
            accessToken = "valid-token",
            expiresAt = now + 10 * 60 * 1000L  // 10 minutes — beyond threshold
        )
        val client = buildClient(engine, tokenManager, refreshManagerProvider = { refreshManager })

        client.get("https://api.test/words")

        assertEquals(0, refreshManager.refreshCallCount)
    }

    @Test
    fun `zero token expiry does not trigger proactive refresh`() = runTest {
        val refreshManager = FakeTokenRefreshManager()
        val engine = MockEngine { respond("""{}""", HttpStatusCode.OK, jsonHeaders()) }
        val tokenManager = FakeTokenManager(accessToken = "token", expiresAt = 0L)
        val client = buildClient(engine, tokenManager, refreshManagerProvider = { refreshManager })

        client.get("https://api.test/words")

        assertEquals(0, refreshManager.refreshCallCount)
    }

    @Test
    fun `null tokenRefreshManagerProvider skips proactive refresh without crash`() = runTest {
        val engine = MockEngine { respond("""{}""", HttpStatusCode.OK, jsonHeaders()) }
        val now = Clock.System.now().toEpochMilliseconds()
        val tokenManager = FakeTokenManager(
            accessToken = "token",
            expiresAt = now + 2 * 60 * 1000L  // within threshold but no refresh manager
        )
        val client = buildClient(engine, tokenManager, refreshManagerProvider = null)

        client.get("https://api.test/words")
        // Test passes if no exception thrown
    }

    @Test
    fun `proactive refresh success causes new token to be read from manager and used`() = runTest {
        val tokenManager = FakeTokenManager(
            accessToken = "old-token",
            expiresAt = 0L  // set after we have now
        )
        val now = Clock.System.now().toEpochMilliseconds()
        tokenManager.expiresAt = now + 2 * 60 * 1000L

        val refreshManager = FakeTokenRefreshManager(
            refreshResult = Try.success("new-proactive-token")
        )
        // When refresh is called, update the token manager so getAccessToken() returns the new token
        refreshManager.onRefreshCalled = { tokenManager.accessToken = "new-proactive-token" }

        var capturedAuthHeader: String? = null
        val engine = MockEngine { request ->
            capturedAuthHeader = request.headers[HttpHeaders.Authorization]
            respond("""{}""", HttpStatusCode.OK, jsonHeaders())
        }
        val client = buildClient(engine, tokenManager, refreshManagerProvider = { refreshManager })

        client.get("https://api.test/words")

        assertEquals("Bearer new-proactive-token", capturedAuthHeader)
    }

    @Test
    fun `proactive refresh failure still uses old token without crash`() = runTest {
        val refreshManager = FakeTokenRefreshManager(
            refreshResult = Try.failure(Exception("Network error during refresh"))
        )
        var capturedAuthHeader: String? = null
        val engine = MockEngine { request ->
            capturedAuthHeader = request.headers[HttpHeaders.Authorization]
            respond("""{}""", HttpStatusCode.OK, jsonHeaders())
        }
        val now = Clock.System.now().toEpochMilliseconds()
        val tokenManager = FakeTokenManager(
            accessToken = "still-valid-token",
            expiresAt = now + 2 * 60 * 1000L
        )
        val client = buildClient(engine, tokenManager, refreshManagerProvider = { refreshManager })

        client.get("https://api.test/words")

        // No crash — old token is used as fallback
        assertEquals("Bearer still-valid-token", capturedAuthHeader)
    }

    @Test
    fun `proactive refresh not triggered for public endpoint even when token expiring soon`() = runTest {
        val refreshManager = FakeTokenRefreshManager()
        val engine = MockEngine { respond("""{}""", HttpStatusCode.OK, jsonHeaders()) }
        val now = Clock.System.now().toEpochMilliseconds()
        val tokenManager = FakeTokenManager(
            accessToken = "token",
            expiresAt = now + 2 * 60 * 1000L
        )
        val client = buildClient(engine, tokenManager, refreshManagerProvider = { refreshManager })

        client.get("https://api.test/auth/google")

        // Public endpoint returns early before any refresh logic
        assertEquals(0, refreshManager.refreshCallCount)
    }
}
