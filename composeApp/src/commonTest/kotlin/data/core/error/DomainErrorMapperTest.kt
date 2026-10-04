package data.core.error

import core.error.DomainError
import core.error.toUserMessage
import data.core.network.error.NetworkException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DomainErrorMapperTest {

    @Test
    fun `toDomainError when the platform reports an IO failure returns NoConnection`() {
        val error = IOException("Unable to resolve host \"example.com\": No address associated with hostname")

        assertEquals(DomainError.Network.NoConnection, error.toDomainError())
    }

    @Test
    fun `toDomainError when the request times out returns Timeout`() {
        val error = HttpRequestTimeoutException("https://example.com/api", 30_000)

        assertEquals(DomainError.Network.Timeout, error.toDomainError())
    }

    @Test
    fun `toDomainError keeps the mapped NetworkException behaviour`() {
        assertEquals(DomainError.Network.NoConnection, NetworkException("offline").toDomainError())
    }

    @Test
    fun `toDomainError leaves unrelated errors untouched`() {
        val error = IllegalStateException("boom")

        assertSame(error, error.toDomainError())
    }

    @Test
    fun `an offline failure reaches the user without the host name`() {
        val message = IOException("Unable to resolve host \"example.com\"").toDomainError().toUserMessage()

        assertEquals("No internet connection", message)
    }
}
