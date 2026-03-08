package data.word.remote

import data.core.network.client.ApiClient
import data.core.network.mapper.ApiResponseMapper
import data.word.remote.model.RemoteWord
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val testJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal fun buildApiClient(mockEngine: MockEngine): ApiClient {
    val httpClient = HttpClient(mockEngine) {
        install(ContentNegotiation) {
            json(testJson)
        }
    }
    return ApiClient(
        baseUrl = "https://api.test",
        httpClient = httpClient,
        apiResponseMapper = ApiResponseMapper()
    )
}

internal fun buildDataSource(mockEngine: MockEngine): WordRemoteDataSource =
    WordRemoteDataSource(buildApiClient(mockEngine))

internal fun successEnvelope(data: String): String =
    """{"success":true,"data":$data}"""

internal fun failureEnvelope(message: String): String =
    """{"success":false,"message":"$message"}"""

internal fun jsonHeaders() =
    headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

internal fun remoteWord(
    id: Long = 1L,
    original: String = "hello",
    translation: String = "hola"
) = RemoteWord(
    id = id,
    originalWord = original,
    translation = translation,
    description = "a greeting",
    sourceLanguage = "en",
    targetLanguage = "es",
    level = 1,
    easeFactor = 2.5f,
    interval = 1,
    repetitions = 0,
    lastReviewDate = 1000L,
    nextReviewDate = 2000L,
    createdAt = null
)

internal fun remoteWordJson(id: Long = 1L, original: String = "hello"): String =
    """{"id":$id,"originalWord":"$original","translation":"hola","description":"a greeting",""" +
        """"sourceLanguage":"en","targetLanguage":"es","level":1,"easeFactor":2.5,""" +
        """"interval":1,"repetitions":0,"lastReviewDate":1000,"nextReviewDate":2000}"""
