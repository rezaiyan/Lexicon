package data.network.interceptor

import data.core.network.error.AuthenticationException
import data.core.network.error.NetworkException
import data.core.network.error.ServerException
import data.core.network.interceptor.ErrorInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Tests for ErrorInterceptor: verifies that it maps non-2xx status codes to typed domain
 * exceptions and passes 2xx responses through unchanged.
 */
class ErrorInterceptorTest {

    private fun jsonHeaders() =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun buildClient(mockEngine: MockEngine): HttpClient = HttpClient(mockEngine) {
        install(ErrorInterceptor().createPlugin())
    }

    // -------------------------------------------------------------------------
    // 2xx — pass-through
    // -------------------------------------------------------------------------

    @Test
    fun `200 response passes through without exception`() = runTest {
        val engine = MockEngine { respond("""{"ok":true}""", HttpStatusCode.OK, jsonHeaders()) }
        val client = buildClient(engine)

        val response = client.get("https://api.test/some/endpoint")

        assertEquals(200, response.status.value)
        assertEquals("""{"ok":true}""", response.bodyAsText())
    }

    @Test
    fun `201 Created response passes through without exception`() = runTest {
        val engine = MockEngine { respond("""{"id":1}""", HttpStatusCode.Created, jsonHeaders()) }
        val client = buildClient(engine)

        val response = client.get("https://api.test/resource")

        assertEquals(201, response.status.value)
    }

    @Test
    fun `204 No Content response passes through without exception`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.NoContent, jsonHeaders()) }
        val client = buildClient(engine)

        val response = client.get("https://api.test/resource")

        assertEquals(204, response.status.value)
    }

    // -------------------------------------------------------------------------
    // 4xx — client error
    // -------------------------------------------------------------------------

    @Test
    fun `401 Unauthorized throws AuthenticationException`() = runTest {
        val engine = MockEngine { respond("Unauthorized", HttpStatusCode.Unauthorized, jsonHeaders()) }
        val client = buildClient(engine)

        val ex = assertFailsWith<AuthenticationException> {
            client.get("https://api.test/private")
        }
        assertEquals(401, ex.statusCode)
    }

    @Test
    fun `403 Forbidden throws AuthenticationException`() = runTest {
        val engine = MockEngine { respond("Forbidden", HttpStatusCode.Forbidden, jsonHeaders()) }
        val client = buildClient(engine)

        val ex = assertFailsWith<AuthenticationException> {
            client.get("https://api.test/private")
        }
        assertEquals(403, ex.statusCode)
    }

    @Test
    fun `404 Not Found throws NetworkException`() = runTest {
        val engine = MockEngine { respond("Not Found", HttpStatusCode.NotFound, jsonHeaders()) }
        val client = buildClient(engine)

        val ex = assertFailsWith<NetworkException> {
            client.get("https://api.test/missing")
        }
        assertIs<NetworkException>(ex)
    }

    @Test
    fun `400 Bad Request throws NetworkException`() = runTest {
        val engine = MockEngine { respond("Bad Request", HttpStatusCode.BadRequest, jsonHeaders()) }
        val client = buildClient(engine)

        assertFailsWith<NetworkException> {
            client.get("https://api.test/resource")
        }
    }

    @Test
    fun `429 Too Many Requests throws NetworkException`() = runTest {
        val engine = MockEngine {
            respond("Too Many Requests", HttpStatusCode.TooManyRequests, jsonHeaders())
        }
        val client = buildClient(engine)

        assertFailsWith<NetworkException> {
            client.get("https://api.test/resource")
        }
    }

    // -------------------------------------------------------------------------
    // 5xx — server error
    // -------------------------------------------------------------------------

    @Test
    fun `500 Internal Server Error throws ServerException`() = runTest {
        val engine = MockEngine {
            respond("Internal Server Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val client = buildClient(engine)

        val ex = assertFailsWith<ServerException> {
            client.get("https://api.test/resource")
        }
        assertEquals(500, ex.statusCode)
    }

    @Test
    fun `502 Bad Gateway throws ServerException`() = runTest {
        val engine = MockEngine { respond("Bad Gateway", HttpStatusCode.BadGateway, jsonHeaders()) }
        val client = buildClient(engine)

        val ex = assertFailsWith<ServerException> {
            client.get("https://api.test/resource")
        }
        assertEquals(502, ex.statusCode)
    }

    @Test
    fun `503 Service Unavailable throws ServerException`() = runTest {
        val engine = MockEngine {
            respond("Service Unavailable", HttpStatusCode.ServiceUnavailable, jsonHeaders())
        }
        val client = buildClient(engine)

        val ex = assertFailsWith<ServerException> {
            client.get("https://api.test/resource")
        }
        assertEquals(503, ex.statusCode)
    }

    // -------------------------------------------------------------------------
    // Error message content
    // -------------------------------------------------------------------------

    @Test
    fun `401 exception message contains authentication context`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized, jsonHeaders()) }
        val client = buildClient(engine)

        val ex = assertFailsWith<AuthenticationException> {
            client.get("https://api.test/private")
        }
        assertEquals(true, ex.message?.isNotBlank())
    }

    @Test
    fun `500 exception message contains server error context`() = runTest {
        val engine = MockEngine {
            respond("", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val client = buildClient(engine)

        val ex = assertFailsWith<ServerException> {
            client.get("https://api.test/resource")
        }
        assertEquals(true, ex.message?.contains("500"))
    }
}
