package data.core.network.interceptor

import data.connectivity.ServerReachability
import data.core.network.error.AuthenticationException
import data.core.network.error.InstructionsRefusedException
import data.core.network.error.InsufficientCreditsException
import data.core.network.error.PremiumRequiredException
import data.core.network.error.RateLimitedException
import data.core.network.error.ServerException
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException

/** Request attribute for the health check, which records its own verdict. */
val SkipReachabilityReport = AttributeKey<Unit>("SkipReachabilityReport")

/**
 * Tells [ServerReachability] how every request went, so the server status follows real traffic
 * without extra polling. Observes only: the call's result or error reaches the caller unchanged.
 */
class ReachabilityInterceptor(private val reachability: ServerReachability) {
    fun createPlugin(): ClientPlugin<Unit> = createClientPlugin("ReachabilityInterceptor") {
        on(Send) { request ->
            if (request.attributes.contains(SkipReachabilityReport)) return@on proceed(request)
            // ErrorInterceptor throws non-2xx answers from inside proceed(), so the error is the only
            // place to learn that the server did answer; it's rethrown untouched.
            @Suppress("TooGenericExceptionCaught")
            val call = try {
                proceed(request)
            } catch (error: Throwable) {
                reportError(error)
                throw error
            }
            reportStatus(call.response.status.value)
            call
        }
    }

    private fun reportStatus(statusCode: Int) {
        if (statusCode >= SERVER_ERROR) reachability.onRequestFailed() else reachability.onServerAnswered()
    }

    private fun reportError(error: Throwable) {
        when {
            // The caller went away; says nothing about the server
            error is CancellationException -> Unit
            error.isServerAnswer() -> reachability.onServerAnswered()
            else -> reachability.onRequestFailed()
        }
    }

    /** Errors the server itself answered with, as opposed to no answer at all. */
    private fun Throwable.isServerAnswer(): Boolean = when (this) {
        is ServerException -> statusCode < SERVER_ERROR
        is AuthenticationException,
        is RateLimitedException,
        is PremiumRequiredException,
        is InsufficientCreditsException,
        is InstructionsRefusedException -> true
        else -> false
    }

    private companion object {
        const val SERVER_ERROR = 500
    }
}
