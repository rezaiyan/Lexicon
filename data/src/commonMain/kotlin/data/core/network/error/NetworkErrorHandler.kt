package data.core.network.error

import domain.auth.repository.IAuthRepository
import domain.common.Try
import domain.common.fold
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Centralized error handler for network operations.
 *
 * NOTE: This handler does NOT auto-logout on AuthenticationException.
 * Token refresh and session invalidation are handled by the HTTP interceptor chain
 * (RefreshAndRetryInterceptor → TokenRefreshManager). Calling logout() here would
 * cause duplicate logout cascades and could race with the refresh flow.
 */
object NetworkErrorHandler {

    fun <T> handleErrors(
        flow: Flow<T>,
        authRepository: IAuthRepository,
        onError: (Exception) -> Unit = {}
    ): Flow<T> = flow.catch { exception ->
        val mappedException = HttpErrorMapper.mapException(exception)
        onError(mappedException)
        throw mappedException
    }

    suspend fun <T> handleResult(
        result: Try<T>,
        authRepository: IAuthRepository,
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
