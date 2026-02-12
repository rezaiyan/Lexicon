package data.core.network.interceptor

import data.auth.token.ITokenManager
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders

class AuthInterceptor(
    private val tokenManager: ITokenManager
) {
    fun createPlugin(): ClientPlugin<Unit> = createClientPlugin("AuthInterceptor") {
        onRequest { request, _ ->
            val token = tokenManager.getAccessToken()
            if (token != null && !request.headers.contains(HttpHeaders.Authorization)) {
                request.headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
}

