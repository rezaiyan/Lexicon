package data.word.remote

import core.common.Try
import data.core.network.client.ApiClient
import data.core.network.mapper.ApiResponseMapper
import data.word.remote.model.BatchUpdateLanguagesRequest
import data.word.remote.model.RemoteWord
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// ktor-client-mock is declared in gradle/libs.versions.toml as:
//   ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
// and added to composeApp/build.gradle.kts commonTest.dependencies.

class WordRemoteDataSourceTest {

    // ---------- helpers ----------

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Creates an [ApiClient] backed by [mockEngine] so no real network is involved.
     * The client is configured with ContentNegotiation + kotlinx-json, matching the
     * production setup, so [ApiResponseMapper] can deserialize the body with body<T>().
     */
    private fun buildApiClient(mockEngine: MockEngine): ApiClient {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        return ApiClient(
            baseUrl = "https://api.test",
            httpClient = httpClient,
            apiResponseMapper = ApiResponseMapper()
        )
    }

    private fun buildDataSource(mockEngine: MockEngine): WordRemoteDataSource =
        WordRemoteDataSource(buildApiClient(mockEngine))

    /** Wraps a payload in the standard ApiResponse envelope the server returns. */
    private fun successEnvelope(data: String): String =
        """{"success":true,"data":$data}"""

    private fun failureEnvelope(message: String): String =
        """{"success":false,"message":"$message"}"""

    private fun jsonHeaders() =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    // ---------- sample data ----------

    private fun remoteWord(
        id: Long = 1L,
        original: String = "hello",
        translation: String = "hola"
    ) = RemoteWord(
        id = id,
        originalWord = original,
        translation = translation,
        description = "a greeting",
        sourceLanguage = "en",
        targetLanguage = "es",
        level = 1,
        easeFactor = 2.5f,
        interval = 1,
        repetitions = 0,
        lastReviewDate = 1000L,
        nextReviewDate = 2000L,
        createdAt = null
    )

    private fun remoteWordJson(id: Long = 1L, original: String = "hello"): String =
        """{"id":$id,"originalWord":"$original","translation":"hola","description":"a greeting",""" +
            """"sourceLanguage":"en","targetLanguage":"es","level":1,"easeFactor":2.5,""" +
            """"interval":1,"repetitions":0,"lastReviewDate":1000,"nextReviewDate":2000}"""

    // =========================================================================
    // getWords
    // =========================================================================

    @Test
    fun `getWords - successful response returns Try Success with list of RemoteWord`() = runTest {
        val wordJson = remoteWordJson(id = 1L, original = "hello")
        val word2Json = remoteWordJson(id = 2L, original = "world")
        val mockEngine = MockEngine {
            respond(
                content = successEnvelope("[$wordJson,$word2Json]"),
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.getWords()

        assertTrue(result is Try.Success, "Expected Try.Success but got $result")
        val words = result.value
        assertEquals(2, words.size)
        assertEquals(1L, words[0].id)
        assertEquals("hello", words[0].originalWord)
        assertEquals(2L, words[1].id)
        assertEquals("world", words[1].originalWord)
    }

    @Test
    fun `getWords - empty list in response returns Try Success with empty list`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = successEnvelope("[]"),
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.getWords()

        assertTrue(result is Try.Success, "Expected Try.Success but got $result")
        assertTrue(result.value.isEmpty())
    }

    @Test
    fun `getWords - null data in success envelope returns Try Success with empty list`() = runTest {
        // ApiResponseMapper returns Try.success(null) when data is null,
        // then WordRemoteDataSource.map { it ?: emptyList() } converts null to empty list.
        val mockEngine = MockEngine {
            respond(
                content = """{"success":true,"data":null}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.getWords()

        assertTrue(result is Try.Success, "Expected Try.Success but got $result")
        assertTrue(result.value.isEmpty())
    }

    @Test
    fun `getWords - HTTP 500 response returns Try Failure`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.getWords()

        assertTrue(result is Try.Failure, "Expected Try.Failure but got $result")
        assertNotNull(result.throwable.message)
    }

    @Test
    fun `getWords - HTTP 401 response returns Try Failure`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "Unauthorized",
                status = HttpStatusCode.Unauthorized,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.getWords()

        assertTrue(result is Try.Failure, "Expected Try.Failure but got $result")
    }

    @Test
    fun `getWords - API success false returns Try Failure with message`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = failureEnvelope("Something went wrong"),
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.getWords()

        assertTrue(result is Try.Failure, "Expected Try.Failure but got $result")
        assertEquals("Something went wrong", result.throwable.message)
    }

    @Test
    fun `getWords - single word in list is returned correctly`() = runTest {
        val word = remoteWord(id = 42L, original = "library", translation = "biblioteca")
        val wordJson = """{"id":42,"originalWord":"library","translation":"biblioteca",""" +
            """"description":"a greeting","sourceLanguage":"en","targetLanguage":"es",""" +
            """"level":1,"easeFactor":2.5,"interval":1,"repetitions":0,""" +
            """"lastReviewDate":1000,"nextReviewDate":2000}"""
        val mockEngine = MockEngine {
            respond(
                content = successEnvelope("[$wordJson]"),
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.getWords()

        assertTrue(result is Try.Success, "Expected Try.Success but got $result")
        val words = result.value
        assertEquals(1, words.size)
        val returnedWord = words[0]
        assertEquals(42L, returnedWord.id)
        assertEquals("library", returnedWord.originalWord)
        assertEquals("biblioteca", returnedWord.translation)
        assertEquals("en", returnedWord.sourceLanguage)
        assertEquals("es", returnedWord.targetLanguage)
        assertFalse(result.isFailure)
    }

    // =========================================================================
    // upsertWords
    // =========================================================================

    @Test
    fun `upsertWords - successful response returns Try Success`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"success":true}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)
        val words = listOf(remoteWord())

        val result = dataSource.upsertWords(words)

        assertTrue(result is Try.Success, "Expected Try.Success but got $result")
    }

    @Test
    fun `upsertWords - HTTP 400 response returns Try Failure`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "Bad Request",
                status = HttpStatusCode.BadRequest,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)
        val words = listOf(remoteWord())

        val result = dataSource.upsertWords(words)

        assertTrue(result is Try.Failure, "Expected Try.Failure but got $result")
    }

    @Test
    fun `upsertWords - empty list body still sends request and returns Try Success`() = runTest {
        var requestMade = false
        val mockEngine = MockEngine {
            requestMade = true
            respond(
                content = """{"success":true}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.upsertWords(emptyList())

        assertTrue(result is Try.Success, "Expected Try.Success but got $result")
        assertTrue(requestMade, "Expected a network request to be made")
    }

    @Test
    fun `upsertWords - HTTP error returns Try Failure`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = failureEnvelope("Upsert failed"),
                status = HttpStatusCode.InternalServerError,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.upsertWords(listOf(remoteWord()))

        assertTrue(result is Try.Failure, "Expected Try.Failure but got $result")
    }

    // =========================================================================
    // deleteWord
    // =========================================================================

    @Test
    fun `deleteWord - successful response returns Try Success`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"success":true}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.deleteWord(id = 99L)

        assertTrue(result is Try.Success, "Expected Try.Success but got $result")
    }

    @Test
    fun `deleteWord - sends DELETE request to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                content = """{"success":true}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        dataSource.deleteWord(id = 7L)

        assertEquals("/words/7", capturedPath)
    }

    @Test
    fun `deleteWord - HTTP 404 response returns Try Failure`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "Not Found",
                status = HttpStatusCode.NotFound,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.deleteWord(id = 999L)

        assertTrue(result is Try.Failure, "Expected Try.Failure but got $result")
        assertNotNull(result.throwable.message)
    }

    @Test
    fun `deleteWord - HTTP 500 response returns Try Failure`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.deleteWord(id = 1L)

        assertTrue(result is Try.Failure, "Expected Try.Failure but got $result")
    }

    // =========================================================================
    // deleteWords
    // =========================================================================

    @Test
    fun `deleteWords - empty list returns Try Success without making network call`() = runTest {
        var networkCallMade = false
        val mockEngine = MockEngine {
            networkCallMade = true
            respond(
                content = """{"success":true}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.deleteWords(emptyList())

        assertTrue(result is Try.Success, "Expected Try.Success but got $result")
        assertFalse(networkCallMade, "No network call should be made for an empty list")
    }

    @Test
    fun `deleteWords - non-empty list sends batch-delete request and returns Try Success`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                content = """{"success":true}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.deleteWords(listOf(1L, 2L, 3L))

        assertTrue(result is Try.Success, "Expected Try.Success but got $result")
        assertEquals("/words/batch-delete", capturedPath)
    }

    @Test
    fun `deleteWords - HTTP error for non-empty list returns Try Failure`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "Service Unavailable",
                status = HttpStatusCode.ServiceUnavailable,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.deleteWords(listOf(10L, 20L))

        assertTrue(result is Try.Failure, "Expected Try.Failure but got $result")
    }

    @Test
    fun `deleteWords - single ID in list makes network call and returns Try Success`() = runTest {
        var networkCallMade = false
        val mockEngine = MockEngine {
            networkCallMade = true
            respond(
                content = """{"success":true}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.deleteWords(listOf(5L))

        assertTrue(result is Try.Success, "Expected Try.Success but got $result")
        assertTrue(networkCallMade, "Expected a network call for a non-empty list")
    }

    // =========================================================================
    // updateWord
    // =========================================================================

    @Test
    fun `updateWord - successful response returns Try Success`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"success":true}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.updateWord(id = 1L, word = remoteWord())

        assertTrue(result is Try.Success, "Expected Try.Success but got $result")
    }

    @Test
    fun `updateWord - sends PATCH request to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                content = """{"success":true}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        dataSource.updateWord(id = 42L, word = remoteWord())

        assertEquals("/words/42", capturedPath)
    }

    @Test
    fun `updateWord - HTTP 400 returns Try Failure`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "Bad Request",
                status = HttpStatusCode.BadRequest,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val result = dataSource.updateWord(id = 1L, word = remoteWord())

        assertTrue(result is Try.Failure, "Expected Try.Failure but got $result")
    }

    // =========================================================================
    // batchUpdateLanguages
    // =========================================================================

    @Test
    fun `batchUpdateLanguages - successful response returns Try Success`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"success":true}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)
        val request = BatchUpdateLanguagesRequest(
            ids = listOf(1L, 2L),
            sourceLanguage = "en",
            targetLanguage = "fr"
        )

        val result = dataSource.batchUpdateLanguages(request)

        assertTrue(result is Try.Success, "Expected Try.Success but got $result")
    }

    @Test
    fun `batchUpdateLanguages - sends request to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                content = """{"success":true}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)
        val request = BatchUpdateLanguagesRequest(ids = listOf(1L), sourceLanguage = "en")

        dataSource.batchUpdateLanguages(request)

        assertEquals("/words/batch-update", capturedPath)
    }

    @Test
    fun `batchUpdateLanguages - HTTP 500 returns Try Failure`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError,
                headers = jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)
        val request = BatchUpdateLanguagesRequest(ids = listOf(1L))

        val result = dataSource.batchUpdateLanguages(request)

        assertTrue(result is Try.Failure, "Expected Try.Failure but got $result")
    }
}
