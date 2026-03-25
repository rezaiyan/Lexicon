package data.core.error

import core.error.DomainError
import data.core.network.error.AuthenticationException
import data.core.network.error.NetworkException
import data.core.network.error.ServerException
import data.core.network.error.TimeoutException

fun Throwable.toDomainError(): Throwable = when (this) {
    is DomainError -> this
    is AuthenticationException -> if (statusCode == 403) DomainError.Auth.Unauthorized
                                  else DomainError.Auth.NotAuthenticated
    is ServerException -> DomainError.Network.ServerError(statusCode)
    is TimeoutException -> DomainError.Network.Timeout
    is NetworkException -> DomainError.Network.NoConnection
    else -> this
}
