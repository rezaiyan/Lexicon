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
import data.ai.remote.model.ExtractWordsRequest
import data.ai.remote.model.SuggestWordsRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.content.TextContent
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

    private val extractRequest = ExtractWordsRequest(imageBase64 = "aGk=", learningLanguage = "German", nativeLanguage = "English")

    private fun HttpRequestData.bodyText() = (body as TextContent).text

    @Test
    fun `extractWords posts the language pair to extract-words and returns the items`() = runTest {
        var path: String? = null
        var body: String? = null
        val mockEngine = MockEngine { request ->
            path = request.url.encodedPath
            body = request.bodyText()
            respond(
                successEnvelope("""{"items":[{"term":"Hund","translation":"dog","note":"der"}]}"""),
                HttpStatusCode.OK, jsonHeaders(),
            )
        }

        val result = buildDataSource(mockEngine).extractWords(extractRequest)

        assertEquals("/ai/extract-words", path)
        assertTrue(body.orEmpty().contains(""""learningLanguage":"German""""))
        assertTrue(body.orEmpty().contains(""""nativeLanguage":"English""""))
        assertEquals(listOf("Hund"), (result as Try.Success).value.items.map { it.term })
    }

    @Test
    fun `extractWords returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine { respond("""{"success":false}""", HttpStatusCode.InternalServerError, jsonHeaders()) }

        assertTrue(buildDataSource(mockEngine).extractWords(extractRequest) is Try.Failure)
    }

    @Test
    fun `suggestWords posts to suggest-vocabulary and returns the items`() = runTest {
        var path: String? = null
        val mockEngine = MockEngine { request ->
            path = request.url.encodedPath
            respond(
                successEnvelope("""{"items":[{"originalWord":"Haus","translation":"house"}]}"""),
                HttpStatusCode.OK, jsonHeaders(),
            )
        }

        val result = buildDataSource(mockEngine).suggestWords(
            SuggestWordsRequest(targetLanguage = "German", nativeLanguage = "English", currentLevel = "beginner"),
        )

        assertEquals("/ai/suggest-vocabulary", path)
        assertEquals(listOf("Haus"), (result as Try.Success).value.items.map { it.originalWord })
    }
}
