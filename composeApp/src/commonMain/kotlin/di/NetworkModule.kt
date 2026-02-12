package di

import data.auth.refresh.ITokenRefreshManager
import data.core.network.HttpClientProvider
import data.core.network.client.ApiClient
import data.core.network.interceptor.AuthInterceptor
import data.core.network.interceptor.ErrorInterceptor
import data.core.network.mapper.ApiResponseMapper
import io.ktor.client.HttpClient
import org.koin.dsl.module

fun networkModule(backendUrl: String) = module {

    // HTTP Interceptors
    single<AuthInterceptor> { AuthInterceptor(tokenManager = get()) }
    single<ErrorInterceptor> { ErrorInterceptor() }

    // HttpClient Singleton with interceptors
    single<HttpClient> {
        val scope = this
        HttpClientProvider.createHttpClient(
            authInterceptor = get(),
            tokenRefreshManagerProvider = { scope.get<ITokenRefreshManager>() },
            errorInterceptor = get()
        )
    }

    // API Response Mapper
    single { ApiResponseMapper() }

    // API Client
    single {
        ApiClient(
            baseUrl = backendUrl,
            httpClient = get<HttpClient>(),
            apiResponseMapper = get()
        )
    }
}
