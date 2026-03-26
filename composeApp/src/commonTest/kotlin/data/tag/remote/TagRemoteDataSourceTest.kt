package data.tag.remote

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TagRemoteDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun buildApiClient(mockEngine: MockEngine): ApiClient {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        return ApiClient("https://api.test", httpClient, ApiResponseMapper())
    }

    private fun buildDataSource(mockEngine: MockEngine) =
        TagRemoteDataSource(buildApiClient(mockEngine))

    private fun successEnvelope(data: String) = """{"success":true,"data":$data}"""
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun remoteTagJson(
        id: Long = 1L,
        name: String = "Spanish",
        wordCount: Long = 0L,
        createdAt: Long = 1000L,
        updatedAt: Long = 2000L
    ) = """{"id":$id,"name":"$name","wordCount":$wordCount,"createdAt":$createdAt,"updatedAt":$updatedAt}"""

    // -------------------------------------------------------------------------
    // getTags
    // -------------------------------------------------------------------------

    @Test
    fun `getTags - success returns list of tags`() = runTest {
        val mockEngine = MockEngine {
            respond(
                successEnvelope("""[${remoteTagJson(id = 1L, name = "Spanish", wordCount = 5L)}]"""),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }

        val result = buildDataSource(mockEngine).getTags()

        assertTrue(result is Try.Success)
        assertEquals(1, result.value.size)
        assertEquals("Spanish", result.value.first().name)
        assertEquals(1L, result.value.first().id)
    }

    @Test
    fun `getTags - returns empty list when data is null`() = runTest {
        val mockEngine = MockEngine {
            respond(
                successEnvelope("null"),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }

        val result = buildDataSource(mockEngine).getTags()

        assertTrue(result is Try.Success)
        assertTrue(result.value.isEmpty())
    }

    @Test
    fun `getTags - sends GET to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(successEnvelope("[]"), HttpStatusCode.OK, jsonHeaders())
        }

        buildDataSource(mockEngine).getTags()

        assertEquals("/tags", capturedPath)
    }

    @Test
    fun `getTags - returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Internal Server Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }

        val result = buildDataSource(mockEngine).getTags()

        assertTrue(result is Try.Failure)
    }

    // -------------------------------------------------------------------------
    // createTag
    // -------------------------------------------------------------------------

    @Test
    fun `createTag - success returns created tag`() = runTest {
        val mockEngine = MockEngine {
            respond(
                successEnvelope(remoteTagJson(id = 10L, name = "Greetings", wordCount = 0L)),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }

        val result = buildDataSource(mockEngine).createTag("Greetings")

        assertTrue(result is Try.Success)
        assertEquals(10L, result.value.id)
        assertEquals("Greetings", result.value.name)
    }

    @Test
    fun `createTag - sends POST to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                successEnvelope(remoteTagJson(id = 1L, name = "Test")),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }

        buildDataSource(mockEngine).createTag("Test")

        assertEquals("/tags", capturedPath)
    }

    @Test
    fun `createTag - returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Bad Request", HttpStatusCode.BadRequest, jsonHeaders())
        }

        val result = buildDataSource(mockEngine).createTag("Invalid")

        assertTrue(result is Try.Failure)
    }

    // -------------------------------------------------------------------------
    // renameTag
    // -------------------------------------------------------------------------

    @Test
    fun `renameTag - success returns renamed tag`() = runTest {
        val mockEngine = MockEngine {
            respond(
                successEnvelope(remoteTagJson(id = 5L, name = "Renamed", wordCount = 3L)),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }

        val result = buildDataSource(mockEngine).renameTag(5L, "Renamed")

        assertTrue(result is Try.Success)
        assertEquals(5L, result.value.id)
        assertEquals("Renamed", result.value.name)
    }

    @Test
    fun `renameTag - sends PUT to correct path with tag id`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                successEnvelope(remoteTagJson(id = 5L, name = "Renamed")),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }

        buildDataSource(mockEngine).renameTag(5L, "Renamed")

        assertEquals("/tags/5", capturedPath)
    }

    // -------------------------------------------------------------------------
    // deleteTag
    // -------------------------------------------------------------------------

    @Test
    fun `deleteTag - success returns Unit`() = runTest {
        val mockEngine = MockEngine {
            respond(
                successEnvelope("null"),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }

        val result = buildDataSource(mockEngine).deleteTag(3L)

        assertTrue(result is Try.Success)
    }

    @Test
    fun `deleteTag - sends DELETE to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(successEnvelope("null"), HttpStatusCode.OK, jsonHeaders())
        }

        buildDataSource(mockEngine).deleteTag(3L)

        assertEquals("/tags/3", capturedPath)
    }

    @Test
    fun `deleteTag - returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Not Found", HttpStatusCode.NotFound, jsonHeaders())
        }

        val result = buildDataSource(mockEngine).deleteTag(999L)

        assertTrue(result is Try.Failure)
    }

    // -------------------------------------------------------------------------
    // updateWordTags
    // -------------------------------------------------------------------------

    @Test
    fun `updateWordTags - success returns Unit`() = runTest {
        val mockEngine = MockEngine {
            respond(
                successEnvelope("null"),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }

        val result = buildDataSource(mockEngine).updateWordTags(42L, listOf(1L, 2L, 3L))

        assertTrue(result is Try.Success)
    }

    @Test
    fun `updateWordTags - sends PUT to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(successEnvelope("null"), HttpStatusCode.OK, jsonHeaders())
        }

        buildDataSource(mockEngine).updateWordTags(42L, listOf(1L, 2L))

        assertEquals("/words/42/tags", capturedPath)
    }

    @Test
    fun `updateWordTags - returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Unprocessable Entity", HttpStatusCode.UnprocessableEntity, jsonHeaders())
        }

        val result = buildDataSource(mockEngine).updateWordTags(42L, listOf(99L))

        assertTrue(result is Try.Failure)
    }

    // -------------------------------------------------------------------------
    // batchUpdateWordTags
    // -------------------------------------------------------------------------

    @Test
    fun `batchUpdateWordTags - success returns Unit`() = runTest {
        val mockEngine = MockEngine {
            respond(
                successEnvelope("null"),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }

        val result = buildDataSource(mockEngine).batchUpdateWordTags(
            wordIds = listOf(1L, 2L, 3L),
            tagIds = listOf(10L, 20L)
        )

        assertTrue(result is Try.Success)
    }

    @Test
    fun `batchUpdateWordTags - sends POST to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(successEnvelope("null"), HttpStatusCode.OK, jsonHeaders())
        }

        buildDataSource(mockEngine).batchUpdateWordTags(
            wordIds = listOf(1L, 2L),
            tagIds = listOf(5L)
        )

        assertEquals("/words/batch-assign-tags", capturedPath)
    }

    @Test
    fun `batchUpdateWordTags - returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Service Unavailable", HttpStatusCode.ServiceUnavailable, jsonHeaders())
        }

        val result = buildDataSource(mockEngine).batchUpdateWordTags(
            wordIds = listOf(1L),
            tagIds = listOf(1L)
        )

        assertTrue(result is Try.Failure)
    }
}
