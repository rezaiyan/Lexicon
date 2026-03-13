package data.notification.remote

import core.common.Try
import data.notification.remote.model.Platform
import data.notification.remote.model.RegisterPushTokenRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PushNotificationDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun buildHttpClient(mockEngine: MockEngine): HttpClient =
        HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }

    private fun buildDataSource(
        mockEngine: MockEngine,
        getAuthToken: suspend () -> String? = { "test-token" }
    ) = PushNotificationDataSource(
        baseUrl = "https://api.test",
        getAuthToken = getAuthToken,
        httpClient = buildHttpClient(mockEngine)
    )

    private fun successEnvelope(data: String) = """{"success":true,"data":$data}"""
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val registerRequest = RegisterPushTokenRequest(
        token = "fcm-device-token-abc123",
        platform = Platform.ANDROID,
        deviceId = "device-001"
    )

    // --- registerPushToken ---

    @Test
    fun `registerPushToken returns failure when not authenticated`() = runTest {
        val mockEngine = MockEngine {
            respond(successEnvelope("null"), HttpStatusCode.OK, jsonHeaders())
        }
        val dataSource = buildDataSource(mockEngine, getAuthToken = { null })

        val result = dataSource.registerPushToken(registerRequest)

        assertTrue(result is Try.Failure)
        assertTrue(result.throwable.message?.contains("not authenticated") == true)
    }

    @Test
    fun `registerPushToken sends POST to correct path when authenticated`() = runTest {
        var capturedPath: String? = null
        var capturedMethod: HttpMethod? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedMethod = request.method
            respond(successEnvelope("null"), HttpStatusCode.OK, jsonHeaders())
        }
        buildDataSource(mockEngine).registerPushToken(registerRequest)

        assertEquals("/notifications/register-token", capturedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
    }

    @Test
    fun `registerPushToken returns success on valid response`() = runTest {
        val mockEngine = MockEngine {
            respond(successEnvelope("null"), HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).registerPushToken(registerRequest)

        assertTrue(result is Try.Success)
    }

    @Test
    fun `registerPushToken returns failure when API response has success false`() = runTest {
        val mockEngine = MockEngine {
            respond(
                """{"success":false,"message":"Token already registered"}""",
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }
        val result = buildDataSource(mockEngine).registerPushToken(registerRequest)

        assertTrue(result is Try.Failure)
    }

    @Test
    fun `registerPushToken returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Internal Server Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).registerPushToken(registerRequest)

        assertTrue(result is Try.Failure)
    }

    // --- deactivateAllTokens ---

    @Test
    fun `deactivateAllTokens returns success when not authenticated`() = runTest {
        var networkCallMade = false
        val mockEngine = MockEngine {
            networkCallMade = true
            respond("", HttpStatusCode.OK, jsonHeaders())
        }
        val dataSource = buildDataSource(mockEngine, getAuthToken = { null })

        val result = dataSource.deactivateAllTokens()

        assertTrue(result is Try.Success)
        assertTrue(!networkCallMade, "No network call should be made when unauthenticated")
    }

    @Test
    fun `deactivateAllTokens sends DELETE to correct path when authenticated`() = runTest {
        var capturedPath: String? = null
        var capturedMethod: HttpMethod? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedMethod = request.method
            respond("", HttpStatusCode.OK, jsonHeaders())
        }
        buildDataSource(mockEngine).deactivateAllTokens()

        assertEquals("/notifications/tokens", capturedPath)
        assertEquals(HttpMethod.Delete, capturedMethod)
    }

    @Test
    fun `deactivateAllTokens returns success even on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Internal Server Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).deactivateAllTokens()

        assertTrue(result is Try.Success)
    }
}
