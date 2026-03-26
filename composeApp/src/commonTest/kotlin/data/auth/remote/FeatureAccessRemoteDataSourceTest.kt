package data.auth.remote

import data.core.network.client.ApiClient
import data.core.network.mapper.ApiResponseMapper
import fakes.FakeFeatureFlagProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureAccessRemoteDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun buildApiClient(mockEngine: MockEngine): ApiClient {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        return ApiClient("https://api.test", httpClient, ApiResponseMapper())
    }

    private val featureFlagProvider = FakeFeatureFlagProvider()

    private fun buildDataSource(mockEngine: MockEngine) =
        FeatureAccessRemoteDataSource(buildApiClient(mockEngine), featureFlagProvider)

    private fun successEnvelope(data: String) = """{"success":true,"data":$data}"""
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test
    fun `getFeatureAccessAsFlow emits feature access data on success`() = runTest {
        val mockEngine = MockEngine {
            respond(
                successEnvelope(
                    """{"featureFlags":{"pushNotificationsEnabled":false},"userAccess":{"hasPremiumAccess":true}}"""
                ),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }
        val result = buildDataSource(mockEngine).getFeatureAccessAsFlow().first()

        assertFalse(result.featureFlags.pushNotificationsEnabled)
        assertTrue(result.userAccess.hasPremiumAccess)
    }

    @Test
    fun `getFeatureAccessAsFlow sends GET to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                successEnvelope(
                    """{"featureFlags":{"pushNotificationsEnabled":true},"userAccess":{"hasPremiumAccess":false}}"""
                ),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }
        buildDataSource(mockEngine).getFeatureAccessAsFlow().first()

        assertEquals("/users/feature-access", capturedPath)
    }

    @Test
    fun `getFeatureAccessAsFlow emits default feature access on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).getFeatureAccessAsFlow().first()

        assertTrue(result.featureFlags.pushNotificationsEnabled)
        assertFalse(result.userAccess.hasPremiumAccess)
    }

    @Test
    fun `getFeatureAccessAsFlow emits default when API returns success false`() = runTest {
        val mockEngine = MockEngine {
            respond(
                """{"success":false,"data":null}""",
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }
        val result = buildDataSource(mockEngine).getFeatureAccessAsFlow().first()

        assertTrue(result.featureFlags.pushNotificationsEnabled)
        assertFalse(result.userAccess.hasPremiumAccess)
    }

    @Test
    fun `default feature access has pushNotificationsEnabled true and hasPremiumAccess false`() = runTest {
        val mockEngine = MockEngine {
            respond("Bad Gateway", HttpStatusCode.BadGateway, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).getFeatureAccessAsFlow().first()

        assertEquals(true, result.featureFlags.pushNotificationsEnabled)
        assertEquals(false, result.userAccess.hasPremiumAccess)
    }

    // --- Cache behavior (BUG-3) ---

    @Test
    fun `getFeatureAccessAsFlow second call returns cached response without network request`() = runTest {
        var requestCount = 0
        val mockEngine = MockEngine {
            requestCount++
            respond(
                successEnvelope("""{"featureFlags":{"pushNotificationsEnabled":true},"userAccess":{"hasPremiumAccess":false}}"""),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        dataSource.getFeatureAccessAsFlow().first()  // cache miss — hits network
        dataSource.getFeatureAccessAsFlow().first()  // cache hit — no network

        assertEquals(1, requestCount)
    }

    @Test
    fun `clearCache forces re-fetch on next call`() = runTest {
        var requestCount = 0
        val mockEngine = MockEngine {
            requestCount++
            respond(
                successEnvelope("""{"featureFlags":{"pushNotificationsEnabled":true},"userAccess":{"hasPremiumAccess":false}}"""),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        dataSource.getFeatureAccessAsFlow().first()  // first fetch
        assertEquals(1, requestCount)

        dataSource.clearCache()

        dataSource.getFeatureAccessAsFlow().first()  // second fetch after cache cleared
        assertEquals(2, requestCount)
    }

    @Test
    fun `cached response has same values as original fetch`() = runTest {
        val mockEngine = MockEngine {
            respond(
                successEnvelope("""{"featureFlags":{"pushNotificationsEnabled":false},"userAccess":{"hasPremiumAccess":true}}"""),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val firstResult = dataSource.getFeatureAccessAsFlow().first()
        val cachedResult = dataSource.getFeatureAccessAsFlow().first()

        assertEquals(firstResult.featureFlags.pushNotificationsEnabled, cachedResult.featureFlags.pushNotificationsEnabled)
        assertEquals(firstResult.userAccess.hasPremiumAccess, cachedResult.userAccess.hasPremiumAccess)
    }
}
