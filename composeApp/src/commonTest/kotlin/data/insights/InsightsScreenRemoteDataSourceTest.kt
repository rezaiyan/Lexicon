package data.insights

import core.common.Try
import data.core.network.client.ApiClient
import data.core.network.mapper.ApiResponseMapper
import data.insights.remote.InsightsScreenRemoteDataSource
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

class InsightsScreenRemoteDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true }
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun dataSource(engine: MockEngine) = InsightsScreenRemoteDataSource(
        ApiClient("https://api.test", HttpClient(engine) { install(ContentNegotiation) { json(json) } }, ApiResponseMapper())
    )

    private val payload = """{"success":true,"data":{
        "generatedAt":"2026-10-10T18:04:00Z","totalReviews":12,"futureField":true,
        "hero":{"headline":"Ahead of last week","reviews":{"value":12,"previous":5},"accuracyPct":null,
                "leveledUp":{"value":3,"previous":1},"currentStreak":2,"week":[{"date":"2026-10-05","reviews":12}]},
        "coach":[{"id":"NEW:2026-10-10","type":"NEW","priority":5,"title":"Hi","body":"b",
                  "action":{"kind":"SOMETHING_NEW"}}],
        "sections":{"mastery":null,"habits":null,"words":null,"wordRush":null},
        "locked":[{"section":"habits","reviewsNeeded":18}]}}"""

    @Test
    fun `sends tz and parses payload with unknown fields`() = runTest {
        var url = ""
        val engine = MockEngine { request ->
            url = request.url.toString()
            respond(payload, HttpStatusCode.OK, jsonHeaders())
        }

        val result = dataSource(engine).fetch("Europe/Berlin")

        assertTrue(url.endsWith("/analytics/insights-screen?tz=Europe%2FBerlin"), url)
        val dto = (result as Try.Success).value
        assertEquals(12, dto.totalReviews)
        assertEquals("SOMETHING_NEW", dto.coach.single().action.kind)
    }

    @Test
    fun `server error is a failure`() = runTest {
        val engine = MockEngine { respond("""{"success":false,"message":"boom"}""", HttpStatusCode.InternalServerError, jsonHeaders()) }
        assertTrue(dataSource(engine).fetch("UTC").isFailure)
    }
}
