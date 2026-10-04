package data.core.error

import core.error.DomainError
import data.core.network.error.AuthenticationException
import data.core.network.error.NetworkException
import data.core.network.error.PremiumRequiredException
import data.core.network.error.ServerException
import data.core.network.error.TimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.io.IOException

fun Throwable.toDomainError(): Throwable = when (this) {
    is DomainError -> this
    is AuthenticationException -> if (statusCode == 403) DomainError.Auth.Unauthorized
                                  else DomainError.Auth.NotAuthenticated
    is PremiumRequiredException -> DomainError.Commerce.PremiumRequired
    is ServerException -> DomainError.Network.ServerError(statusCode)
    is TimeoutException -> DomainError.Network.Timeout
    is NetworkException -> DomainError.Network.NoConnection
    // Ktor's timeouts are IOExceptions too, so they must be matched first
    is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> DomainError.Network.Timeout
    // Platform I/O failures (no route, DNS lookup failed, connection reset) never reached the server
    is IOException -> DomainError.Network.NoConnection
    else -> this
}
