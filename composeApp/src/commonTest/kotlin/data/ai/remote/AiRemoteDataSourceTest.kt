package data.ai.remote

import core.common.Try
import data.core.network.client.ApiClient
import data.core.network.mapper.ApiResponseMapper
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
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiRemoteDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun buildApiClient(mockEngine: MockEngine): ApiClient {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        return ApiClient("https://api.test", httpClient, ApiResponseMapper())
    }

    private fun buildDataSource(mockEngine: MockEngine) =
        AiRemoteDataSource(buildApiClient(mockEngine))

    private fun successEnvelope(data: String) = """{"success":true,"data":$data}"""
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    // Valid image bytes (> 128 bytes, < 3MB)
    private val validImageBytes = ByteArray(256) { it.toByte() }

    @Test
    fun `extractVocabularyFromImage returns extracted text on success`() = runTest {
        val mockEngine = MockEngine {
            respond(
                successEnvelope("""{"extractedText":"hello,world","wordCount":2}"""),
                HttpStatusCode.OK, jsonHeaders()
            )
        }
        val result = buildDataSource(mockEngine).extractVocabularyFromImage(
            validImageBytes, Language.SPANISH, true, false
        )

        assertTrue(result is Try.Success)
        assertEquals("hello,world", result.value)
    }

    @Test
    fun `extractVocabularyFromImage sends POST to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                successEnvelope("""{"extractedText":"text","wordCount":1}"""),
                HttpStatusCode.OK, jsonHeaders()
            )
        }
        buildDataSource(mockEngine).extractVocabularyFromImage(
            validImageBytes, Language.ENGLISH, true, false
        )

        assertEquals("/ai/extract-vocabulary", capturedPath)
    }

    @Test
    fun `extractVocabularyFromImage returns failure when image too large`() = runTest {
        val largeImage = ByteArray(4 * 1024 * 1024) // 4MB
        val mockEngine = MockEngine {
            respond("should not be called", HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).extractVocabularyFromImage(
            largeImage, Language.ENGLISH, true, false
        )

        assertTrue(result is Try.Failure)
        val message = requireNotNull(result.throwable.message) { "Expected non-null error message" }
        assertTrue(message.contains("too large"))
    }

    @Test
    fun `extractVocabularyFromImage returns failure when image too small`() = runTest {
        val tinyImage = ByteArray(10)
        val mockEngine = MockEngine {
            respond("should not be called", HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).extractVocabularyFromImage(
            tinyImage, Language.ENGLISH, true, false
        )

        assertTrue(result is Try.Failure)
        val message = requireNotNull(result.throwable.message) { "Expected non-null error message" }
        assertTrue(message.contains("too small"))
    }

    @Test
    fun `extractVocabularyFromImage returns failure when extracted text is empty`() = runTest {
        val mockEngine = MockEngine {
            respond(
                successEnvelope("""{"extractedText":"","wordCount":0}"""),
                HttpStatusCode.OK, jsonHeaders()
            )
        }
        val result = buildDataSource(mockEngine).extractVocabularyFromImage(
            validImageBytes, Language.GERMAN, true, false
        )

        assertTrue(result is Try.Failure)
        val message = requireNotNull(result.throwable.message) { "Expected non-null error message" }
        assertTrue(message.contains("No vocabulary found"))
    }

    @Test
    fun `extractVocabularyFromImage returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).extractVocabularyFromImage(
            validImageBytes, Language.ENGLISH, true, false
        )

        assertTrue(result is Try.Failure)
    }
}
