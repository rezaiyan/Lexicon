package data.streak.remote

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

class StreakRemoteDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun buildApiClient(mockEngine: MockEngine): ApiClient {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        return ApiClient("https://api.test", httpClient, ApiResponseMapper())
    }

    private fun buildDataSource(mockEngine: MockEngine) =
        StreakRemoteDataSource(buildApiClient(mockEngine))

    private fun successEnvelope(data: String) = """{"success":true,"data":$data}"""
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test
    fun `getStreak returns streak on success`() = runTest {
        val mockEngine = MockEngine {
            respond(successEnvelope("""{"currentStreak":7}"""), HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).getStreak()

        assertTrue(result is Try.Success)
        assertEquals(7, result.value.currentStreak)
    }

    @Test
    fun `getStreak sends GET to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(successEnvelope("""{"currentStreak":0}"""), HttpStatusCode.OK, jsonHeaders())
        }
        buildDataSource(mockEngine).getStreak()

        assertEquals("/streak", capturedPath)
    }

    @Test
    fun `getStreak returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).getStreak()

        assertTrue(result is Try.Failure)
    }

    @Test
    fun `recordActivity returns updated streak on success`() = runTest {
        val mockEngine = MockEngine {
            respond(successEnvelope("""{"currentStreak":8}"""), HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).recordActivity(5)

        assertTrue(result is Try.Success)
        assertEquals(8, result.value.currentStreak)
    }

    @Test
    fun `recordActivity sends POST to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(successEnvelope("""{"currentStreak":1}"""), HttpStatusCode.OK, jsonHeaders())
        }
        buildDataSource(mockEngine).recordActivity(1)

        assertEquals("/streak/record", capturedPath)
    }

    @Test
    fun `recordActivity returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Error", HttpStatusCode.BadRequest, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).recordActivity(1)

        assertTrue(result is Try.Failure)
    }
}
