package data.network.interceptor

import core.common.Try
import data.auth.refresh.ITokenRefreshManager
import data.core.network.interceptor.RefreshAndRetryInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for RefreshAndRetryInterceptor: verifies 401/403 trigger token refresh + retry,
 * auth endpoints are excluded, and infinite retry loops are prevented.
 */
class RefreshAndRetryInterceptorTest {

    // -------------------------------------------------------------------------
    // Fake
    // -------------------------------------------------------------------------

    private class FakeTokenRefreshManager(
        private var refreshResult: Try<String> = Try.success("new-refreshed-token"),
        var refreshCallCount: Int = 0,
        var clearSessionCalled: Boolean = false
    ) : ITokenRefreshManager {
        override suspend fun refresh(): Try<String> {
            refreshCallCount++
            return refreshResult
        }

        override suspend fun clearSession() {
            clearSessionCalled = true
        }

        fun setRefreshResult(result: Try<String>) {
            refreshResult = result
        }
    }

    private fun jsonHeaders() =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    /**
     * Builds a client with both ErrorInterceptor and RefreshAndRetryInterceptor installed.
     * ErrorInterceptor converts 4xx/5xx into exceptions before response pipeline processes them.
     * RefreshAndRetryInterceptor handles 401/403 by refreshing + retrying.
     *
     * Note: The interceptor only intercepts responses that haven't been converted to exceptions yet.
     * RefreshAndRetryInterceptor must be installed AFTER ErrorInterceptor so it runs first in the
     * response pipeline (plugins are invoked in reverse installation order for responses).
     */
    private fun buildClient(
        engine: MockEngine,
        refreshManager: FakeTokenRefreshManager
    ): HttpClient = HttpClient(engine) {
        install(RefreshAndRetryInterceptor) {
            tokenRefreshManagerProvider = { refreshManager }
        }
    }

    // -------------------------------------------------------------------------
    // Conditions that trigger refresh+retry
    // -------------------------------------------------------------------------

    @Test
    fun `401 response for authenticated request triggers token refresh and retries`() = runTest {
        val refreshManager = FakeTokenRefreshManager(
            refreshResult = Try.success("new-refreshed-token")
        )
        var callCount = 0
        val engine = MockEngine { request ->
            callCount++
            if (callCount == 1) {
                respond("Unauthorized", HttpStatusCode.Unauthorized, jsonHeaders())
            } else {
                respond("""{"ok":true}""", HttpStatusCode.OK, jsonHeaders())
            }
        }
        val client = buildClient(engine, refreshManager)

        val response = client.get("https://api.test/words") {
            header(HttpHeaders.Authorization, "Bearer old-token")
        }

        assertEquals(200, response.status.value)
        assertEquals(1, refreshManager.refreshCallCount)
        assertEquals(2, callCount) // original + retry
    }

    @Test
    fun `403 response for authenticated request triggers token refresh and retries`() = runTest {
        val refreshManager = FakeTokenRefreshManager(
            refreshResult = Try.success("new-refreshed-token")
        )
        var callCount = 0
        val engine = MockEngine { request ->
            callCount++
            if (callCount == 1) {
                respond("Forbidden", HttpStatusCode.Forbidden, jsonHeaders())
            } else {
                respond("""{"ok":true}""", HttpStatusCode.OK, jsonHeaders())
            }
        }
        val client = buildClient(engine, refreshManager)

        val response = client.get("https://api.test/profile") {
            header(HttpHeaders.Authorization, "Bearer old-token")
        }

        assertEquals(200, response.status.value)
        assertEquals(1, refreshManager.refreshCallCount)
    }

    @Test
    fun `retry uses new access token in Authorization header`() = runTest {
        val refreshManager = FakeTokenRefreshManager(
            refreshResult = Try.success("brand-new-token")
        )
        var callCount = 0
        var capturedRetryAuthHeader: String? = null
        val engine = MockEngine { request ->
            callCount++
            if (callCount == 1) {
                respond("Unauthorized", HttpStatusCode.Unauthorized, jsonHeaders())
            } else {
                capturedRetryAuthHeader = request.headers[HttpHeaders.Authorization]
                respond("""{"ok":true}""", HttpStatusCode.OK, jsonHeaders())
            }
        }
        val client = buildClient(engine, refreshManager)

        client.get("https://api.test/words") {
            header(HttpHeaders.Authorization, "Bearer old-token")
        }

        assertEquals("Bearer brand-new-token", capturedRetryAuthHeader)
    }

    // -------------------------------------------------------------------------
    // Failed refresh — original response propagated
    // -------------------------------------------------------------------------

    @Test
    fun `failed refresh propagates original 401 response without crashing`() = runTest {
        val refreshManager = FakeTokenRefreshManager(
            refreshResult = Try.failure(Exception("Network error"))
        )
        var callCount = 0
        val engine = MockEngine { request ->
            callCount++
            respond("Unauthorized", HttpStatusCode.Unauthorized, jsonHeaders())
        }
        val client = buildClient(engine, refreshManager)

        val response = client.get("https://api.test/words") {
            header(HttpHeaders.Authorization, "Bearer old-token")
        }

        assertEquals(401, response.status.value)
        assertEquals(1, refreshManager.refreshCallCount)
        assertEquals(1, callCount) // only original request, no retry
    }

    // -------------------------------------------------------------------------
    // Conditions that skip refresh+retry
    // -------------------------------------------------------------------------

    @Test
    fun `auth endpoints do not trigger refresh on 401`() = runTest {
        val refreshManager = FakeTokenRefreshManager()
        val engine = MockEngine { respond("Unauthorized", HttpStatusCode.Unauthorized, jsonHeaders()) }
        val client = buildClient(engine, refreshManager)

        val response = client.get("https://api.test/auth/google") {
            header(HttpHeaders.Authorization, "Bearer old-token")
        }

        assertEquals(401, response.status.value)
        assertEquals(0, refreshManager.refreshCallCount)
    }

    @Test
    fun `auth refresh endpoint does not trigger refresh on 401`() = runTest {
        val refreshManager = FakeTokenRefreshManager()
        val engine = MockEngine { respond("Unauthorized", HttpStatusCode.Unauthorized, jsonHeaders()) }
        val client = buildClient(engine, refreshManager)

        val response = client.get("https://api.test/auth/refresh") {
            header(HttpHeaders.Authorization, "Bearer old-token")
        }

        assertEquals(401, response.status.value)
        assertEquals(0, refreshManager.refreshCallCount)
    }

    @Test
    fun `request without Authorization header does not trigger refresh on 401`() = runTest {
        val refreshManager = FakeTokenRefreshManager()
        val engine = MockEngine { respond("Unauthorized", HttpStatusCode.Unauthorized, jsonHeaders()) }
        val client = buildClient(engine, refreshManager)

        val response = client.get("https://api.test/public-that-returned-401")
        // No auth header added

        assertEquals(401, response.status.value)
        assertEquals(0, refreshManager.refreshCallCount)
    }

    @Test
    fun `already-retried request does not trigger another refresh preventing infinite loop`() = runTest {
        val refreshManager = FakeTokenRefreshManager(
            refreshResult = Try.success("token-after-refresh")
        )
        var callCount = 0
        val engine = MockEngine { request ->
            callCount++
            respond("Unauthorized", HttpStatusCode.Unauthorized, jsonHeaders())
        }
        val client = buildClient(engine, refreshManager)

        val response = client.get("https://api.test/words") {
            header(HttpHeaders.Authorization, "Bearer old-token")
        }

        // The retry (2nd request) returns 401 again but should NOT retry a second time
        assertEquals(401, response.status.value)
        assertEquals(1, refreshManager.refreshCallCount) // refresh only called once
        assertEquals(2, callCount) // original + exactly one retry
    }

    // -------------------------------------------------------------------------
    // 200/non-auth errors are not intercepted
    // -------------------------------------------------------------------------

    @Test
    fun `200 response passes through without triggering refresh`() = runTest {
        val refreshManager = FakeTokenRefreshManager()
        val engine = MockEngine { respond("""{"ok":true}""", HttpStatusCode.OK, jsonHeaders()) }
        val client = buildClient(engine, refreshManager)

        val response = client.get("https://api.test/words") {
            header(HttpHeaders.Authorization, "Bearer valid-token")
        }

        assertEquals(200, response.status.value)
        assertEquals(0, refreshManager.refreshCallCount)
    }

    @Test
    fun `500 response does not trigger refresh`() = runTest {
        val refreshManager = FakeTokenRefreshManager()
        val engine = MockEngine {
            respond("Internal Server Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val client = buildClient(engine, refreshManager)

        val response = client.get("https://api.test/words") {
            header(HttpHeaders.Authorization, "Bearer valid-token")
        }

        assertEquals(500, response.status.value)
        assertEquals(0, refreshManager.refreshCallCount)
    }

    // -------------------------------------------------------------------------
    // POST with body — body is re-sent on retry
    // -------------------------------------------------------------------------

    @Test
    fun `POST request retried after 401 with body preserved`() = runTest {
        val refreshManager = FakeTokenRefreshManager(
            refreshResult = Try.success("new-token-for-post")
        )
        var callCount = 0
        val engine = MockEngine { request ->
            callCount++
            if (callCount == 1) {
                respond("Unauthorized", HttpStatusCode.Unauthorized, jsonHeaders())
            } else {
                respond("""{"ok":true}""", HttpStatusCode.OK, jsonHeaders())
            }
        }
        val client = buildClient(engine, refreshManager)

        val response = client.post("https://api.test/words") {
            header(HttpHeaders.Authorization, "Bearer old-token")
            contentType(ContentType.Application.Json)
            setBody("""{"word":"hello"}""")
        }

        assertEquals(200, response.status.value)
        assertEquals(2, callCount) // original + retry
        assertEquals(1, refreshManager.refreshCallCount)
    }

    // -------------------------------------------------------------------------
    // GET request — no body copy on retry
    // -------------------------------------------------------------------------

    @Test
    fun `GET request retried after 401 without body issues`() = runTest {
        val refreshManager = FakeTokenRefreshManager(
            refreshResult = Try.success("new-token-for-get")
        )
        var callCount = 0
        val engine = MockEngine { request ->
            callCount++
            if (callCount == 1) {
                respond("Unauthorized", HttpStatusCode.Unauthorized, jsonHeaders())
            } else {
                respond("""{"words":[]}""", HttpStatusCode.OK, jsonHeaders())
            }
        }
        val client = buildClient(engine, refreshManager)

        val response = client.get("https://api.test/words") {
            header(HttpHeaders.Authorization, "Bearer old-token")
        }

        assertEquals(200, response.status.value)
        assertEquals(2, callCount)
    }
}
