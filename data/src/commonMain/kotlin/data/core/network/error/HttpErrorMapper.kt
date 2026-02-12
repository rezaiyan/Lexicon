package data.core.network.error

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

object HttpErrorMapper {

    fun mapHttpResponse(response: HttpResponse): Exception {
        val statusCode = response.status
        val message = when {
            statusCode.value in 400..499 -> {
                when (statusCode) {
                    HttpStatusCode.Unauthorized -> "Authentication failed. Please sign in again."
                    HttpStatusCode.Forbidden -> "Authentication failed. Account may be deleted or deactivated. Please sign in again."
                    HttpStatusCode.NotFound -> "Resource not found."
                    HttpStatusCode.BadRequest -> "Invalid request. Please check your input."
                    else -> "Client error: ${statusCode.value}"
                }
            }

            statusCode.value in 500..599 -> {
                "Server error: ${statusCode.value}. Please try again later."
            }

            else -> {
                "Unexpected error: ${statusCode.value}"
            }
        }

        return when (statusCode) {
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden -> AuthenticationException(message)

            HttpStatusCode.InternalServerError,
            HttpStatusCode.BadGateway,
            HttpStatusCode.ServiceUnavailable -> ServerException(message)

            else -> NetworkException(message)
        }
    }

    fun mapException(exception: Throwable): Exception {
        return when (exception) {
            is AuthenticationException,
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
                    NetworkException("Connection timeout. Please check your internet connection and try again.")
                } else {
                    NetworkException(message)
                }
            }
        }
    }
}

