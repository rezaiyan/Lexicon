package data.analytics.remote

import core.common.Try
import data.analytics.remote.model.SyncAnalyticsRequest
import data.analytics.remote.model.SyncReviewEventRequest
import data.analytics.remote.model.SyncSessionRequest
import data.core.network.client.ApiClient
import data.core.network.mapper.ApiResponseMapper
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

class AnalyticsRemoteDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun buildApiClient(mockEngine: MockEngine): ApiClient {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        return ApiClient("https://api.test", httpClient, ApiResponseMapper())
    }

    private fun buildDataSource(mockEngine: MockEngine) =
        AnalyticsRemoteDataSource(buildApiClient(mockEngine))

    private fun successEnvelope(data: String) = """{"success":true,"data":$data}"""
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun testSyncRequest() = SyncAnalyticsRequest(
        sessions = listOf(
            SyncSessionRequest(
                clientSessionId = "s-1",
                startedAt = 1000,
                endedAt = 2000,
                durationMs = 1000,
                totalCards = 5,
                correctCount = 4,
                incorrectCount = 1,
                reviewType = "flashcard",
                completedNormally = true,
                events = listOf(
                    SyncReviewEventRequest(
                        wordId = 10,
                        wordText = "hello",
                        wordTranslation = "hola",
                        sourceLanguage = "EN",
                        targetLanguage = "ES",
                        rating = 4,
                        previousLevel = 1,
                        newLevel = 2,
                        responseTimeMs = 1200,
                        reviewedAt = 1500,
                    )
                ),
            )
        ),
    )

    @Test
    fun `syncSessions sends POST to analytics sync endpoint`() = runTest {
        var capturedPath: String? = null
        var capturedMethod: HttpMethod? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedMethod = request.method
            respond(
                successEnvelope("""{"syncedSessionIds":["s-1"]}"""),
                HttpStatusCode.OK,
                jsonHeaders(),
            )
        }

        buildDataSource(mockEngine).syncSessions(testSyncRequest())

        assertEquals("/analytics/sync", capturedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
    }

    @Test
    fun `syncSessions returns synced IDs on success`() = runTest {
        val mockEngine = MockEngine {
            respond(
                successEnvelope("""{"syncedSessionIds":["s-1","s-2"]}"""),
                HttpStatusCode.OK,
                jsonHeaders(),
            )
        }

        val result = buildDataSource(mockEngine).syncSessions(testSyncRequest())

        assertTrue(result is Try.Success)
        assertEquals(listOf("s-1", "s-2"), result.value.syncedSessionIds)
    }

    @Test
    fun `syncSessions returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Internal Server Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }

        val result = buildDataSource(mockEngine).syncSessions(testSyncRequest())

        assertTrue(result is Try.Failure)
    }

    @Test
    fun `syncSessions returns failure on bad request`() = runTest {
        val mockEngine = MockEngine {
            respond("Bad Request", HttpStatusCode.BadRequest, jsonHeaders())
        }

        val result = buildDataSource(mockEngine).syncSessions(testSyncRequest())

        assertTrue(result is Try.Failure)
    }
}
