package data.leaderboard.remote

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

class LeaderboardRemoteDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun buildApiClient(mockEngine: MockEngine): ApiClient {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        return ApiClient("https://api.test", httpClient, ApiResponseMapper())
    }

    private fun buildDataSource(mockEngine: MockEngine) =
        LeaderboardRemoteDataSource(buildApiClient(mockEngine))

    private fun successEnvelope(data: String) = """{"success":true,"data":$data}"""
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val leaderboardJson = """{
        "entries":[
            {"rank":1,"displayName":"Alice","currentStreak":10,"longestStreak":20,"masteredWords":100,"isCurrentUser":false,"profileImageUrl":"https://img.example.com/a.jpg"},
            {"rank":2,"displayName":"Bob","currentStreak":5,"longestStreak":15,"masteredWords":50,"isCurrentUser":true}
        ],
        "userEntry":{"rank":2,"displayName":"Bob","currentStreak":5,"longestStreak":15,"masteredWords":50,"isCurrentUser":true}
    }"""

    @Test
    fun `getLeaderboard returns entries on success`() = runTest {
        val mockEngine = MockEngine {
            respond(successEnvelope(leaderboardJson), HttpStatusCode.OK, jsonHeaders())
        }
        val ds = buildDataSource(mockEngine)

        val result = ds.getLeaderboard()

        assertTrue(result is Try.Success)
        assertEquals(2, result.value.entries.size)
        assertEquals("Alice", result.value.entries[0].displayName)
        assertEquals(true, result.value.entries[1].isCurrentUser)
        assertEquals("Bob", result.value.userEntry?.displayName)
    }

    @Test
    fun `getLeaderboard sends GET to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(successEnvelope("""{"entries":[],"userEntry":null}"""), HttpStatusCode.OK, jsonHeaders())
        }
        buildDataSource(mockEngine).getLeaderboard()

        assertEquals("/leaderboard", capturedPath)
    }

    @Test
    fun `getLeaderboard returns failure on HTTP 500`() = runTest {
        val mockEngine = MockEngine {
            respond("Internal Server Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).getLeaderboard()

        assertTrue(result is Try.Failure)
    }
}
