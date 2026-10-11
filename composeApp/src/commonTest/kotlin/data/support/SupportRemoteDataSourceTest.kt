package data.support

import core.common.Try
import data.core.network.client.ApiClient
import data.core.network.mapper.ApiResponseMapper
import data.support.remote.SendSupportMessageRequest
import data.support.remote.SupportRemoteDataSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupportRemoteDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true }
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun dataSource(engine: MockEngine) = SupportRemoteDataSource(
        ApiClient("https://api.test", HttpClient(engine) { install(ContentNegotiation) { json(json) } }, ApiResponseMapper())
    )

    @Test
    fun `getMessages asks for messages after the id and parses them`() = runTest {
        var url = ""
        val engine = MockEngine { request ->
            url = request.url.toString()
            respond(
                """{"success":true,"data":{"unreadCount":1,"messages":[
                    {"id":7,"sender":"ADMIN","body":"Hi!","clientMessageId":null,"createdAt":1000,"extra":true}]}}""",
                HttpStatusCode.OK,
                jsonHeaders(),
            )
        }

        val result = dataSource(engine).getMessages(afterId = 5)

        assertTrue(url.endsWith("/support/messages?after=5"), url)
        val message = (result as Try.Success).value.single()
        assertEquals(7L, message.id)
        assertEquals("ADMIN", message.sender)
        assertEquals("Hi!", message.body)
    }

    @Test
    fun `send posts body and client id`() = runTest {
        var method: HttpMethod? = null
        var sentBody = ""
        val engine = MockEngine { request ->
            method = request.method
            sentBody = (request.body as TextContent).text
            respond(
                """{"success":true,"data":{"id":9,"sender":"USER","body":"help","clientMessageId":"c-1","createdAt":5}}""",
                HttpStatusCode.OK,
                jsonHeaders(),
            )
        }

        val result = dataSource(engine).send(SendSupportMessageRequest(body = "help", clientMessageId = "c-1"))

        assertEquals(HttpMethod.Post, method)
        assertEquals("""{"body":"help","clientMessageId":"c-1"}""", sentBody)
        assertEquals("c-1", (result as Try.Success).value.clientMessageId)
    }

    @Test
    fun `server error is a failure`() = runTest {
        val engine = MockEngine {
            respond("""{"success":false,"message":"Rate limit exceeded"}""", HttpStatusCode.TooManyRequests, jsonHeaders())
        }

        assertTrue(dataSource(engine).send(SendSupportMessageRequest("x", "c")).isFailure)
    }
}
