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

class ProfileRemoteDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun buildApiClient(mockEngine: MockEngine): ApiClient {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        return ApiClient("https://api.test", httpClient, ApiResponseMapper())
    }

    private fun buildDataSource(mockEngine: MockEngine) =
        ProfileRemoteDataSource(buildApiClient(mockEngine))

    private fun successEnvelope(data: String) = """{"success":true,"data":$data}"""
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val userJson = """{
        "id":1,"email":"test@test.com","name":"Updated","subscriptionStatus":"FREE","subscriptionExpiresAt":null
    }"""

    @Test
    fun `updateProfile returns user on success`() = runTest {
        val mockEngine = MockEngine {
            respond(successEnvelope(userJson), HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).updateProfile("Updated", "alias")

        assertTrue(result is Try.Success)
        assertEquals("Updated", result.value.name)
        assertEquals("test@test.com", result.value.email)
    }

    @Test
    fun `updateProfile sends PATCH to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(successEnvelope(userJson), HttpStatusCode.OK, jsonHeaders())
        }
        buildDataSource(mockEngine).updateProfile("Name", null)

        assertEquals("/users/me", capturedPath)
    }

    @Test
    fun `updateProfile returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).updateProfile("Name", null)

        assertTrue(result is Try.Failure)
    }

    @Test
    fun `deleteAvatar returns success`() = runTest {
        val mockEngine = MockEngine {
            respond("", HttpStatusCode.OK, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).deleteAvatar()

        assertTrue(result is Try.Success)
    }

    @Test
    fun `deleteAvatar sends DELETE to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond("", HttpStatusCode.OK, jsonHeaders())
        }
        buildDataSource(mockEngine).deleteAvatar()

        assertEquals("/users/me/avatar", capturedPath)
    }

    @Test
    fun `deleteAvatar returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).deleteAvatar()

        assertTrue(result is Try.Failure)
    }
}
