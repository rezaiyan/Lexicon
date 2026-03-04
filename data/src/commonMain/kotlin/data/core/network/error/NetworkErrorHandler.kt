package data.core.network.error

import core.common.Try
import core.common.fold

/**
 * Centralized error handler for network operations.
 *
 * NOTE: This handler does NOT auto-logout on AuthenticationException.
 * Token refresh and session invalidation are handled by the HTTP interceptor chain
 * (RefreshAndRetryInterceptor → TokenRefreshManager). Calling logout() here would
 * cause duplicate logout cascades and could race with the refresh flow.
 */
object NetworkErrorHandler {

    fun <T> handleResult(
        result: Try<T>,
        onSuccess: (T) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ): Try<T> {
        return result.fold(
            onSuccess = { value ->
                onSuccess(value)
                Try.success(value)
            },
            onFailure = { exception ->
                val mappedException = HttpErrorMapper.mapException(exception)
                onError(mappedException)
                Try.failure(mappedException)
            }
        )
    }
}
