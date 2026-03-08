package data.profile.remote

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

class ProfileStatsRemoteDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun buildApiClient(mockEngine: MockEngine): ApiClient {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        return ApiClient("https://api.test", httpClient, ApiResponseMapper())
    }

    private fun buildDataSource(mockEngine: MockEngine) =
        ProfileStatsRemoteDataSource(buildApiClient(mockEngine))

    private fun successEnvelope(data: String) = """{"success":true,"data":$data}"""
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val statsJson = """{
        "currentStreak":5,
        "longestStreak":10,
        "memberSince":"2024-01-01",
        "weeklyActivity":[{"date":"2024-03-01","reviewCount":20}],
        "languages":[{"sourceLanguage":"en","targetLanguage":"de","wordCount":50}]
    }"""

    @Test
    fun `getProfileStats returns stats on success`() = runTest {
        val mockEngine = MockEngine {
            respond(successEnvelope(statsJson), HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).getProfileStats()

        assertTrue(result is Try.Success)
        assertEquals(5, result.value.currentStreak)
        assertEquals(10, result.value.longestStreak)
        assertEquals("2024-01-01", result.value.memberSince)
        assertEquals(1, result.value.weeklyActivity.size)
        assertEquals(20, result.value.weeklyActivity[0].reviewCount)
        assertEquals(1, result.value.languages.size)
        assertEquals("de", result.value.languages[0].targetLanguage)
    }

    @Test
    fun `getProfileStats sends GET to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(successEnvelope(statsJson), HttpStatusCode.OK, jsonHeaders())
        }
        buildDataSource(mockEngine).getProfileStats()

        assertEquals("/users/profile-stats", capturedPath)
    }

    @Test
    fun `getProfileStats returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).getProfileStats()

        assertTrue(result is Try.Failure)
    }
}
