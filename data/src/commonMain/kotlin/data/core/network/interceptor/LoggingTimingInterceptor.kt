package data.core.network.interceptor

import expects.logNetwork
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.util.date.getTimeMillis

/**
 * Ktor client plugin that logs request method + URL, response status + duration.
 */
val LoggingTimingInterceptor = createClientPlugin("LoggingTimingInterceptor") {
    onRequest { request, _ ->
        request.attributes.put(RequestStartTimeKey, getTimeMillis())
        logNetwork("HTTP", "→ ${request.method.value} ${request.url.buildString()}")
    }
    onResponse { response ->
        val startTime = response.call.request.attributes.getOrNull(RequestStartTimeKey)
        val duration = if (startTime != null) getTimeMillis() - startTime else -1
        logNetwork("HTTP", "← ${response.status.value} ${response.call.request.url.encodedPath} (${duration}ms)")
    }
}

private val RequestStartTimeKey = io.ktor.util.AttributeKey<Long>("RequestStartTime")
