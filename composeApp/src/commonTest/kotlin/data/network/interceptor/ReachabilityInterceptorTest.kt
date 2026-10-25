package data.network.interceptor

import data.connectivity.Reachability
import data.connectivity.ServerReachability
import data.core.network.interceptor.ErrorInterceptor
import data.core.network.interceptor.ReachabilityInterceptor
import data.core.network.interceptor.SkipReachabilityReport
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReachabilityInterceptorTest {

    private val reachability = ServerReachability()

    private fun client(engine: MockEngine) = HttpClient(engine) {
        install(ErrorInterceptor().createPlugin())
        install(ReachabilityInterceptor(reachability).createPlugin())
    }

    private fun answering(status: HttpStatusCode) = client(MockEngine { respond("", status) })

    private suspend fun checkRequested(): Boolean =
        withTimeoutOrNull(1) { reachability.checkRequests.first() } != null

    @Test
    fun `a 2xx answer marks the server reachable`() = runTest {
        answering(HttpStatusCode.OK).get("https://api/words")

        assertEquals(Reachability.Reachable, reachability.reachability.value)
        assertEquals(false, checkRequested())
    }

    @Test
    fun `a 4xx answer still proves the server is up, and the error reaches the caller`() = runTest {
        assertFailsWith<Exception> { answering(HttpStatusCode.Unauthorized).get("https://api/words") }
        assertFailsWith<Exception> { answering(HttpStatusCode.NotFound).get("https://api/words") }

        assertEquals(Reachability.Reachable, reachability.reachability.value)
        assertEquals(false, checkRequested())
    }

    @Test
    fun `a 5xx answer asks for a health check instead of declaring the server down`() = runTest {
        assertFailsWith<Exception> { answering(HttpStatusCode.BadGateway).get("https://api/words") }

        assertEquals(Reachability.Unknown, reachability.reachability.value)
        assertEquals(true, checkRequested())
    }

    @Test
    fun `no answer at all asks for a health check`() = runTest {
        val failing = client(MockEngine { throw IllegalStateException("connection refused") })

        val error = assertFailsWith<IllegalStateException> { failing.get("https://api/words") }

        assertEquals("connection refused", error.message)
        assertEquals(true, checkRequested())
    }

    @Test
    fun `the health check's own requests are left alone`() = runTest {
        val failing = client(MockEngine { throw IllegalStateException("down") })

        assertFailsWith<IllegalStateException> {
            failing.get("https://api/health") { attributes.put(SkipReachabilityReport, Unit) }
        }

        assertEquals(Reachability.Unknown, reachability.reachability.value)
        assertEquals(false, checkRequested())
    }

    @Test
    fun `the response passes through unchanged`() = runTest {
        val response = answering(HttpStatusCode.NoContent).get("https://api/words")

        assertEquals(HttpStatusCode.NoContent, response.status)
    }
}
