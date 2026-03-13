package data.auth.remote

import core.common.Try
import data.core.network.error.AuthenticationException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuthDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun buildDataSource(mockEngine: MockEngine): AuthDataSource {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        return AuthDataSource("https://api.test", httpClient)
    }

    private fun successEnvelope(data: String) = """{"success":true,"data":$data}"""
    private fun failureEnvelope(message: String) = """{"success":false,"message":"$message"}"""
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val userJson = """{
        "id":1,
        "email":"user@example.com",
        "name":"Test User",
        "subscriptionStatus":"free",
        "subscriptionExpiresAt":null,
        "currentStreak":3,
        "displayAlias":"tester",
        "profileImageUrl":null
    }"""

    private val authResponseJson = """{
        "accessToken":"access-token-abc",
        "refreshToken":"refresh-token-xyz",
        "tokenType":"Bearer",
        "expiresIn":3600,
        "user":$userJson
    }"""

    // --- authenticateWithGoogle ---

    @Test
    fun `authenticateWithGoogle returns AuthResponse on success`() = runTest {
        val mockEngine = MockEngine {
            respond(successEnvelope(authResponseJson), HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).authenticateWithGoogle("google-id-token")

        assertTrue(result is Try.Success)
        assertEquals("access-token-abc", result.value.accessToken)
        assertEquals("refresh-token-xyz", result.value.refreshToken)
        assertEquals("user@example.com", result.value.user.email)
    }

    @Test
    fun `authenticateWithGoogle sends POST to correct path`() = runTest {
        var capturedPath: String? = null
        var capturedMethod: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedMethod = request.method.value
            respond(successEnvelope(authResponseJson), HttpStatusCode.OK, jsonHeaders())
        }
        buildDataSource(mockEngine).authenticateWithGoogle("google-id-token")

        assertEquals("/auth/google", capturedPath)
        assertEquals("POST", capturedMethod)
    }

    @Test
    fun `authenticateWithGoogle returns failure when success is false`() = runTest {
        val mockEngine = MockEngine {
            respond(failureEnvelope("Invalid token"), HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).authenticateWithGoogle("bad-token")

        assertTrue(result is Try.Failure)
        assertIs<AuthenticationException>(result.throwable)
        assertEquals("Invalid token", result.throwable.message)
    }

    @Test
    fun `authenticateWithGoogle returns failure when data is null`() = runTest {
        val mockEngine = MockEngine {
            respond("""{"success":true,"data":null}""", HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).authenticateWithGoogle("token")

        assertTrue(result is Try.Failure)
        assertIs<AuthenticationException>(result.throwable)
    }

    @Test
    fun `authenticateWithGoogle returns failure with default message when server message is null`() = runTest {
        val mockEngine = MockEngine {
            respond("""{"success":false}""", HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).authenticateWithGoogle("token")

        assertTrue(result is Try.Failure)
        assertIs<AuthenticationException>(result.throwable)
        assertEquals("Authentication failed", result.throwable.message)
    }

    @Test
    fun `authenticateWithGoogle returns failure on HTTP 500`() = runTest {
        val mockEngine = MockEngine {
            respond("Internal Server Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).authenticateWithGoogle("token")

        assertTrue(result is Try.Failure)
    }

    // --- authenticateWithApple ---

    @Test
    fun `authenticateWithApple returns AuthResponse on success`() = runTest {
        val mockEngine = MockEngine {
            respond(successEnvelope(authResponseJson), HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).authenticateWithApple(
            idToken = "apple-id-token",
            fullName = "John Doe",
            appleUserId = "apple-user-001"
        )

        assertTrue(result is Try.Success)
        assertEquals("access-token-abc", result.value.accessToken)
        assertEquals("user@example.com", result.value.user.email)
    }

    @Test
    fun `authenticateWithApple sends POST to correct path`() = runTest {
        var capturedPath: String? = null
        var capturedMethod: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedMethod = request.method.value
            respond(successEnvelope(authResponseJson), HttpStatusCode.OK, jsonHeaders())
        }
        buildDataSource(mockEngine).authenticateWithApple(
            idToken = "apple-id-token",
            fullName = null,
            appleUserId = "apple-user-001"
        )

        assertEquals("/auth/apple", capturedPath)
        assertEquals("POST", capturedMethod)
    }

    @Test
    fun `authenticateWithApple returns failure when success is false`() = runTest {
        val mockEngine = MockEngine {
            respond(failureEnvelope("Apple authentication failed"), HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).authenticateWithApple(
            idToken = "bad-apple-token",
            fullName = null,
            appleUserId = "apple-user-001"
        )

        assertTrue(result is Try.Failure)
        assertIs<AuthenticationException>(result.throwable)
        assertEquals("Apple authentication failed", result.throwable.message)
    }

    @Test
    fun `authenticateWithApple returns failure when data is null`() = runTest {
        val mockEngine = MockEngine {
            respond("""{"success":true,"data":null}""", HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).authenticateWithApple(
            idToken = "token",
            fullName = null,
            appleUserId = "apple-user-001"
        )

        assertTrue(result is Try.Failure)
        assertIs<AuthenticationException>(result.throwable)
    }

    @Test
    fun `authenticateWithApple returns failure on HTTP 500`() = runTest {
        val mockEngine = MockEngine {
            respond("Server error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).authenticateWithApple(
            idToken = "token",
            fullName = null,
            appleUserId = "apple-user-001"
        )

        assertTrue(result is Try.Failure)
    }

    // --- refreshTokens ---

    @Test
    fun `refreshTokens returns AuthResponse on success`() = runTest {
        val mockEngine = MockEngine {
            respond(successEnvelope(authResponseJson), HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).refreshTokens("old-refresh-token")

        assertTrue(result is Try.Success)
        assertEquals("access-token-abc", result.value.accessToken)
        assertEquals("refresh-token-xyz", result.value.refreshToken)
        assertEquals(3600L, result.value.expiresIn)
    }

    @Test
    fun `refreshTokens sends POST to correct path`() = runTest {
        var capturedPath: String? = null
        var capturedMethod: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedMethod = request.method.value
            respond(successEnvelope(authResponseJson), HttpStatusCode.OK, jsonHeaders())
        }
        buildDataSource(mockEngine).refreshTokens("old-refresh-token")

        assertEquals("/auth/refresh", capturedPath)
        assertEquals("POST", capturedMethod)
    }

    @Test
    fun `refreshTokens returns failure when success is false`() = runTest {
        val mockEngine = MockEngine {
            respond(failureEnvelope("Token refresh failed"), HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).refreshTokens("expired-token")

        assertTrue(result is Try.Failure)
        assertIs<AuthenticationException>(result.throwable)
        assertEquals("Token refresh failed", result.throwable.message)
    }

    @Test
    fun `refreshTokens returns failure when data is null`() = runTest {
        val mockEngine = MockEngine {
            respond("""{"success":true,"data":null}""", HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).refreshTokens("token")

        assertTrue(result is Try.Failure)
        assertIs<AuthenticationException>(result.throwable)
    }

    @Test
    fun `refreshTokens returns failure with default message when server message is null`() = runTest {
        val mockEngine = MockEngine {
            respond("""{"success":false}""", HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).refreshTokens("token")

        assertTrue(result is Try.Failure)
        assertIs<AuthenticationException>(result.throwable)
        assertEquals("Token refresh failed", result.throwable.message)
    }

    @Test
    fun `refreshTokens returns failure on HTTP 401`() = runTest {
        val mockEngine = MockEngine {
            respond("Unauthorized", HttpStatusCode.Unauthorized, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).refreshTokens("token")

        assertTrue(result is Try.Failure)
    }

    @Test
    fun `refreshTokens returns failure on HTTP 500`() = runTest {
        val mockEngine = MockEngine {
            respond("Server error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).refreshTokens("token")

        assertTrue(result is Try.Failure)
    }

    // --- logout ---

    @Test
    fun `logout returns success on 200 response`() = runTest {
        val mockEngine = MockEngine {
            respond("""{"success":true}""", HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).logout("refresh-token")

        assertTrue(result is Try.Success)
    }

    @Test
    fun `logout sends POST to correct path`() = runTest {
        var capturedPath: String? = null
        var capturedMethod: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedMethod = request.method.value
            respond("""{"success":true}""", HttpStatusCode.OK, jsonHeaders())
        }
        buildDataSource(mockEngine).logout("refresh-token")

        assertEquals("/auth/logout", capturedPath)
        assertEquals("POST", capturedMethod)
    }

    @Test
    fun `logout returns success even on HTTP 500 (best-effort)`() = runTest {
        val mockEngine = MockEngine {
            respond("Server error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).logout("refresh-token")

        assertTrue(result is Try.Success)
    }

    @Test
    fun `logout returns success even on HTTP 401 (best-effort)`() = runTest {
        val mockEngine = MockEngine {
            respond("Unauthorized", HttpStatusCode.Unauthorized, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).logout("refresh-token")

        assertTrue(result is Try.Success)
    }

    // --- getUserProfile ---

    @Test
    fun `getUserProfile returns UserDto on success`() = runTest {
        val mockEngine = MockEngine {
            respond(successEnvelope(userJson), HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).getUserProfile()

        assertTrue(result is Try.Success)
        assertEquals(1L, result.value.id)
        assertEquals("user@example.com", result.value.email)
        assertEquals("Test User", result.value.name)
        assertEquals("free", result.value.subscriptionStatus)
        assertEquals(3, result.value.currentStreak)
    }

    @Test
    fun `getUserProfile sends GET to correct path`() = runTest {
        var capturedPath: String? = null
        var capturedMethod: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedMethod = request.method.value
            respond(successEnvelope(userJson), HttpStatusCode.OK, jsonHeaders())
        }
        buildDataSource(mockEngine).getUserProfile()

        assertEquals("/users/me", capturedPath)
        assertEquals("GET", capturedMethod)
    }

    @Test
    fun `getUserProfile returns failure when success is false`() = runTest {
        val mockEngine = MockEngine {
            respond(failureEnvelope("Failed to get user profile"), HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).getUserProfile()

        assertTrue(result is Try.Failure)
        assertIs<AuthenticationException>(result.throwable)
        assertEquals("Failed to get user profile", result.throwable.message)
    }

    @Test
    fun `getUserProfile returns failure when data is null`() = runTest {
        val mockEngine = MockEngine {
            respond("""{"success":true,"data":null}""", HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).getUserProfile()

        assertTrue(result is Try.Failure)
        assertIs<AuthenticationException>(result.throwable)
    }

    @Test
    fun `getUserProfile returns failure on HTTP 401`() = runTest {
        val mockEngine = MockEngine {
            respond("Unauthorized", HttpStatusCode.Unauthorized, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).getUserProfile()

        assertTrue(result is Try.Failure)
    }

    @Test
    fun `getUserProfile returns failure on HTTP 500`() = runTest {
        val mockEngine = MockEngine {
            respond("Internal Server Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).getUserProfile()

        assertTrue(result is Try.Failure)
    }

    @Test
    fun `getUserProfile returns failure on HTTP 403`() = runTest {
        val mockEngine = MockEngine {
            respond("Forbidden", HttpStatusCode.Forbidden, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).getUserProfile()

        assertTrue(result is Try.Failure)
    }

    // --- deleteAccount ---

    @Test
    fun `deleteAccount returns success on 200 response`() = runTest {
        val mockEngine = MockEngine {
            respond("", HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).deleteAccount()

        assertTrue(result is Try.Success)
    }

    @Test
    fun `deleteAccount sends DELETE to correct path`() = runTest {
        var capturedPath: String? = null
        var capturedMethod: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedMethod = request.method.value
            respond("", HttpStatusCode.OK, jsonHeaders())
        }
        buildDataSource(mockEngine).deleteAccount()

        assertEquals("/auth/delete-account", capturedPath)
        assertEquals("DELETE", capturedMethod)
    }

    @Test
    fun `deleteAccount succeeds even on HTTP error (no body parsing)`() = runTest {
        val mockEngine = MockEngine {
            respond("Internal Server Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).deleteAccount()

        // Without Ktor interceptors, delete() doesn't throw on non-2xx
        // Similar to logout's best-effort behavior
        assertTrue(result is Try.Success)
    }
}
