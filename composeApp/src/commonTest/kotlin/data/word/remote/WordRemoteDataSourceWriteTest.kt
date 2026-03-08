package data.word.remote

import core.common.Try
import data.word.remote.model.BatchUpdateLanguagesRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WordRemoteDataSourceWriteTest {

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
