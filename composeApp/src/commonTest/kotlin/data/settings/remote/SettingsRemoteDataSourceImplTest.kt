package data.settings.remote

import core.common.Try
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

class SettingsRemoteDataSourceImplTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun buildApiClient(mockEngine: MockEngine): ApiClient {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        return ApiClient("https://api.test", httpClient, ApiResponseMapper())
    }

    private fun buildDataSource(mockEngine: MockEngine) =
        SettingsRemoteDataSourceImpl(buildApiClient(mockEngine))

    private fun successEnvelope() = """{"success":true,"data":null}"""
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test
    fun `syncSettings sends PATCH to correct path`() = runTest {
        var capturedPath: String? = null
        var capturedMethod: HttpMethod? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedMethod = request.method
            respond(successEnvelope(), HttpStatusCode.OK, jsonHeaders())
        }
        val dto = SettingsSyncDto(
            languageCode = "en",
            themeMode = "AUTO",
            notificationsEnabled = true,
            dailyReminderTime = "18:00",
            reviewRemindersEnabled = true,
        )
        buildDataSource(mockEngine).syncSettings(dto)

        assertEquals("/settings", capturedPath)
        assertEquals(HttpMethod.Patch, capturedMethod)
    }

    @Test
    fun `syncSettings returns success on 200`() = runTest {
        val mockEngine = MockEngine {
            respond(successEnvelope(), HttpStatusCode.OK, jsonHeaders())
        }
        val dto = SettingsSyncDto("en", "AUTO", true, "18:00", false)
        val result = buildDataSource(mockEngine).syncSettings(dto)
        assertTrue(result is Try.Success)
    }

    @Test
    fun `syncSettings returns failure on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val dto = SettingsSyncDto("en", "AUTO", true, "18:00", true)
        val result = buildDataSource(mockEngine).syncSettings(dto)
        assertTrue(result is Try.Failure)
    }
}
