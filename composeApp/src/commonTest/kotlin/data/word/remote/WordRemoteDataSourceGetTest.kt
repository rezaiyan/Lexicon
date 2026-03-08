package data.word.remote

import core.common.Try
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WordRemoteDataSourceGetTest {

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
}
