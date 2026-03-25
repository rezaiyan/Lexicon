package data.core.network.client

import data.core.error.toDomainError
import data.core.network.mapper.ApiResponseMapper
import core.common.Try
import core.common.flatMap
import core.common.mapFailure
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.PublishedApi

/**
 * Base API client that handles common HTTP operations with consistent error handling
 * Eliminates try-catch repetition and provides a clean API for Try<T> and Flow<T>
 * Relies on HttpClient interceptors for authentication and error handling
 */
class ApiClient(
    @PublishedApi internal val baseUrl: String,
    @PublishedApi internal val httpClient: HttpClient,
    @PublishedApi internal val apiResponseMapper: ApiResponseMapper
) {
    /**
     * Executes a GET request and returns Try<T?>
     * Automatically handles errors via ApiResponseMapper
     */
    suspend inline fun <reified T> get(
        path: String,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): Try<T?> {
        return executeRequest {
            httpClient.get("$baseUrl$path") {
                block()
            }
        }
    }

    /**
     * Executes a GET request and returns Try<T> (non-null)
     * Returns failure if response data is null
     */
    suspend inline fun <reified T> getNotNull(
        path: String,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): Try<T> {
        return get<T>(path, block).flatMap { value ->
            if (value != null) Try.success(value)
            else Try.failure(Exception("Response data is null"))
        }
    }

    /**
     * Executes a POST request and returns Try<T?>
     */
    suspend inline fun <reified T> post(
        path: String,
        body: Any? = null,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): Try<T?> {
        return executeRequest {
            httpClient.post("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                body?.let { setBody(it) }
                block()
            }
        }
    }

    /**
     * Executes a POST request and returns Try<T> (non-null)
     */
    suspend inline fun <reified T> postNotNull(
        path: String,
        body: Any? = null,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): Try<T> {
        return post<T>(path, body, block).flatMap { value ->
            if (value != null) Try.success(value)
            else Try.failure(Exception("Response data is null"))
        }
    }

    /**
     * Executes a POST request and returns Try<Unit>
     */
    suspend inline fun postUnit(
        path: String,
        body: Any? = null,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): Try<Unit> {
        return executeUnitRequest {
            httpClient.post("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                body?.let { setBody(it) }
                block()
            }
        }
    }

    /**
     * Executes a PUT request and returns Try<T?>
     */
    suspend inline fun <reified T> put(
        path: String,
        body: Any? = null,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): Try<T?> {
        return executeRequest {
            httpClient.put("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                body?.let { setBody(it) }
                block()
            }
        }
    }

    /**
     * Executes a PUT request and returns Try<T> (non-null)
     */
    suspend inline fun <reified T> putNotNull(
        path: String,
        body: Any? = null,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): Try<T> {
        return put<T>(path, body, block).flatMap { value ->
            if (value != null) Try.success(value)
            else Try.failure(Exception("Response data is null"))
        }
    }

    /**
     * Executes a PUT request and returns Try<Unit>
     */
    suspend inline fun putUnit(
        path: String,
        body: Any? = null,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): Try<Unit> {
        return executeUnitRequest {
            httpClient.put("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                body?.let { setBody(it) }
                block()
            }
        }
    }

    /**
     * Executes a PATCH request and returns Try<T?>
     */
    suspend inline fun <reified T> patch(
        path: String,
        body: Any? = null,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): Try<T?> {
        return executeRequest {
            httpClient.patch("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                body?.let { setBody(it) }
                block()
            }
        }
    }

    /**
     * Executes a PATCH request and returns Try<T> (non-null)
     */
    suspend inline fun <reified T> patchNotNull(
        path: String,
        body: Any? = null,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): Try<T> {
        return patch<T>(path, body, block).flatMap { value ->
            if (value != null) Try.success(value)
            else Try.failure(Exception("Response data is null"))
        }
    }

    /**
     * Executes a PATCH request and returns Try<Unit>
     */
    suspend inline fun patchUnit(
        path: String,
        body: Any? = null,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): Try<Unit> {
        return executeUnitRequest {
            httpClient.patch("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                body?.let { setBody(it) }
                block()
            }
        }
    }

    /**
     * Executes a DELETE request and returns Try<Unit>
     */
    suspend inline fun delete(
        path: String,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): Try<Unit> {
        return executeUnitRequest {
            httpClient.delete("$baseUrl$path") {
                block()
            }
        }
    }

    /**
     * Core execution method that handles HTTP responses via ApiResponseMapper
     * Wraps exceptions in Try automatically
     */
    suspend inline fun <reified T> executeRequest(
        crossinline request: suspend () -> HttpResponse
    ): Try<T?> = Try {
        apiResponseMapper.mapResponse<T>(request())
    }.flatMap { it }.mapFailure { it.toDomainError() }

    /**
     * Core execution method for Unit responses
     */
    suspend inline fun executeUnitRequest(
        crossinline request: suspend () -> HttpResponse
    ): Try<Unit> = Try {
        apiResponseMapper.mapUnitResponse(request())
    }.flatMap { it }.mapFailure { it.toDomainError() }
}

/**
 * Executes a GET request and returns Flow<Try<T?>>
 */
inline fun <reified T> ApiClient.getFlow(
    path: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): Flow<Try<T?>> = flow {
    emit(get<T>(path, block))
}

/**
 * Executes a GET request and returns Flow<Try<T>> (non-null)
 */
inline fun <reified T> ApiClient.getFlowNotNull(
    path: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): Flow<Try<T>> = flow {
    emit(getNotNull<T>(path, block))
}
