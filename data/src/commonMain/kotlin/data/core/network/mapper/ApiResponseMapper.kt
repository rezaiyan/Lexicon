package data.core.network.mapper

import data.core.network.error.HttpErrorMapper
import data.core.network.model.ApiResponse
import domain.common.Try
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse

/**
 * Mapper for handling API responses consistently
 * Extracts data from ApiResponse<T> and handles errors
 * Note: Token refresh and logout on 401/403 are handled by RefreshAndRetryInterceptor
 */
class ApiResponseMapper {

    /**
     * Maps an HTTP response to a Try containing the API response data
     * Handles non-success status codes and invalid API responses
     */
    suspend inline fun <reified T> mapResponse(
        httpResponse: HttpResponse
    ): Try<T?> {
        if (httpResponse.status.value !in 200..299) {
            val exception = HttpErrorMapper.mapHttpResponse(httpResponse)
            return Try.failure(exception)
        }

        val apiResponse = httpResponse.body<ApiResponse<T>>()

        if (!apiResponse.success) {
            val errorMessage = apiResponse.message ?: "API request failed"
            return Try.failure(Exception(errorMessage))
        }

        return Try.success(apiResponse.data)
    }

    suspend fun mapUnitResponse(
        httpResponse: HttpResponse
    ): Try<Unit> {
        if (httpResponse.status.value !in 200..299) {
            val exception = HttpErrorMapper.mapHttpResponse(httpResponse)
            return Try.failure(exception)
        }

        return Try.success(Unit)
    }
}
