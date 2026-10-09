package data.core.network.error

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Machine-readable `code` values the server puts in error bodies (see the server's ApiErrorCode). */
internal object ApiErrorCode {
    const val INSUFFICIENT_CREDITS = "INSUFFICIENT_CREDITS"
}

object HttpErrorMapper {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun mapHttpResponse(response: HttpResponse): Exception {
        val statusCode = response.status
        val message = messageFor(statusCode)

        return when (statusCode) {
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden -> AuthenticationException(message, statusCode.value)

            // Both "needs premium" and "out of credits" are 402; the body's code tells them apart
            HttpStatusCode.PaymentRequired -> when (response.errorCode()) {
                ApiErrorCode.INSUFFICIENT_CREDITS -> InsufficientCreditsException("Not enough credits.")
                else -> PremiumRequiredException(message)
            }

            HttpStatusCode.TooManyRequests -> RateLimitedException(message)

            // The server answered, so this is never a connectivity problem: keep the status for callers.
            else -> ServerException(message, statusCode.value)
        }
    }

    private fun messageFor(statusCode: HttpStatusCode): String = when (statusCode) {
        HttpStatusCode.Unauthorized -> "Authentication failed. Please sign in again."
        HttpStatusCode.Forbidden -> "Authentication failed. Account may be deleted or deactivated. Please sign in again."
        HttpStatusCode.PaymentRequired -> "This feature requires a subscription."
        HttpStatusCode.NotFound -> "Resource not found."
        HttpStatusCode.BadRequest -> "Invalid request. Please check your input."
        HttpStatusCode.TooManyRequests -> "Too many requests. Please try again in a few minutes."
        else -> when (statusCode.value) {
            in 400..499 -> "Client error: ${statusCode.value}"
            in 500..599 -> "Server error: ${statusCode.value}. Please try again later."
            else -> "Unexpected error: ${statusCode.value}"
        }
    }

    fun mapException(exception: Throwable): Exception {
        return when (exception) {
            is AuthenticationException,
            is PremiumRequiredException,
            is InsufficientCreditsException,
            is RateLimitedException,
            is ServerException,
            is NetworkException -> exception

            else -> {
                val message = exception.message ?: "An unexpected error occurred"
                // Check for timeout-related exceptions by message
                if (
                    message.contains("timeout", ignoreCase = true) ||
                    message.contains("connect", ignoreCase = true) ||
                    exception::class.simpleName?.contains("Timeout", ignoreCase = true) == true
                ) {
                    TimeoutException("Connection timeout. Please check your internet connection and try again.")
                } else {
                    NetworkException(message)
                }
            }
        }
    }

    /**
     * The `code` field of an error body, or null when there is none. The body is untrusted (it may be
     * empty, HTML from a proxy, or truncated), so a parse failure must never replace the real error.
     */
    private suspend fun HttpResponse.errorCode(): String? = runCatching {
        json.parseToJsonElement(bodyAsText()).jsonObject["code"]?.jsonPrimitive?.content
    }.onFailure { if (it is CancellationException) throw it }.getOrNull()
}
