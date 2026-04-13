package data.core.network.interceptor

import core.getAppVersion
import core.getPlatformName
import io.ktor.client.plugins.api.createClientPlugin

/**
 * Ktor client plugin that appends client metadata headers to every outgoing request:
 * - X-Platform: "Android", "iOS", or "Web"
 * - X-App-Version: the app version string (e.g. "1.29.0"), or "unknown" if unavailable
 */
val PlatformHeaderInterceptor = createClientPlugin("PlatformHeaderInterceptor") {
    onRequest { request, _ ->
        request.headers.append("X-Platform", getPlatformName())
        request.headers.append("X-App-Version", getAppVersion())
    }
}
