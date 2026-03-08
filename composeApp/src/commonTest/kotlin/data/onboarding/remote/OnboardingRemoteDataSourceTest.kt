package data.onboarding.remote

import core.common.Try
import data.core.network.client.ApiClient
import data.core.network.mapper.ApiResponseMapper
import data.onboarding.remote.model.OnboardingPreferencesRequest
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
import kotlin.test.assertTrue

class OnboardingRemoteDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun buildApiClient(mockEngine: MockEngine): ApiClient {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        return ApiClient("https://api.test", httpClient, ApiResponseMapper())
    }

    private fun buildDataSource(mockEngine: MockEngine) =
        OnboardingRemoteDataSource(buildApiClient(mockEngine))

    private fun successEnvelope(data: String) = """{"success":true,"data":$data}"""
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val request = OnboardingPreferencesRequest(
        targetLanguage = "de",
        nativeLanguage = "en",
        currentLevel = "beginner",
        interests = listOf("travel")
    )

    private val responseJson = """{
        "targetLanguage":"de",
        "nativeLanguage":"en",
        "currentLevel":"beginner",
        "items":[{"originalWord":"Hallo","translation":"hello","description":"a greeting"}]
    }"""

    @Test
    fun `submitPreferences returns response on success`() = runTest {
        val mockEngine = MockEngine {
            respond(successEnvelope(responseJson), HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).submitPreferences(request)

        assertTrue(result is Try.Success)
        assertEquals("de", result.value.targetLanguage)
        assertEquals(1, result.value.items.size)
        assertEquals("Hallo", result.value.items[0].originalWord)
    }

    @Test
    fun `submitPreferences sends POST to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { req ->
            capturedPath = req.url.encodedPath
            respond(successEnvelope(responseJson), HttpStatusCode.OK, jsonHeaders())
        }
        buildDataSource(mockEngine).submitPreferences(request)

        assertEquals("/onboarding/preferences", capturedPath)
    }

    @Test
    fun `submitPreferences returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).submitPreferences(request)

        assertTrue(result is Try.Failure)
    }

    @Test
    fun `submitPreferences returns failure on API error`() = runTest {
        val mockEngine = MockEngine {
            respond("""{"success":false,"message":"Invalid preferences"}""", HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).submitPreferences(request)

        assertTrue(result is Try.Failure)
    }
}
