package data.core.network.mapper

import data.core.network.error.HttpErrorMapper
import data.core.network.model.ApiResponse
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse

/**
 * Mapper for handling API responses consistently
 * Extracts data from ApiResponse<T> and handles errors
 * Note: Token refresh and logout on 401/403 are handled by RefreshAndRetryInterceptor
 */
class ApiResponseMapper {

    /**
     * Maps an HTTP response to a Result containing the API response data
     * Handles non-success status codes and invalid API responses
     */
    suspend inline fun <reified T> mapResponse(
        httpResponse: HttpResponse
    ): Result<T?> {
        return try {
            if (httpResponse.status.value !in 200..299) {
                val exception = HttpErrorMapper.mapHttpResponse(httpResponse)
                return Result.failure(exception)
            }

            val apiResponse = httpResponse.body<ApiResponse<T>>()

            if (!apiResponse.success) {
                val errorMessage = apiResponse.message ?: "API request failed"
                return Result.failure(Exception(errorMessage))
            }

            Result.success(apiResponse.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun mapUnitResponse(
        httpResponse: HttpResponse
    ): Result<Unit> {
        return try {
            if (httpResponse.status.value !in 200..299) {
                val exception = HttpErrorMapper.mapHttpResponse(httpResponse)
                return Result.failure(exception)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

