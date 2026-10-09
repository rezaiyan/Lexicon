package data.ai.repository

import core.common.Try
import core.common.exceptionOrNull
import core.error.DomainError
import data.ai.remote.AiRemoteDataSource
import data.core.network.client.ApiClient
import data.core.network.mapper.ApiResponseMapper
import domain.word.add.model.LanguagePair
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [AiRepositoryImpl] over the real [ApiClient] and HTTP error mapping, so status codes are mapped
 * exactly as in production (a faked data source hid that a 404 surfaced as "no connection").
 */
class AiRepositoryHttpTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val languages = LanguagePair(Language.GERMAN, Language.ENGLISH)
    private val image = ByteArray(256) { it.toByte() }
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun repository(engine: MockEngine) = AiRepositoryImpl(
        AiRemoteDataSource(
            ApiClient(
                "https://api.test",
                HttpClient(engine) { install(ContentNegotiation) { json(json) } },
                ApiResponseMapper(),
            ),
        ),
    )

    @Test
    fun `extractWords calls only the v2 endpoint and reports a missing one as a server error`() = runTest {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths += request.url.encodedPath
            respond("""{"success":false,"message":"Not found"}""", HttpStatusCode.NotFound, jsonHeaders)
        }

        val error = (repository(engine).extractWords(image, languages) as Try.Failure).exceptionOrNull()

        assertEquals(DomainError.Network.ServerError(404), error)
        assertEquals(listOf("/ai/extract-words"), paths)
    }

    @Test
    fun `extractWords reports a client error as a server error not as offline`() = runTest {
        val engine = MockEngine {
            respond("""{"success":false,"message":"Too many requests"}""", HttpStatusCode.TooManyRequests, jsonHeaders)
        }

        val error = (repository(engine).extractWords(image, languages) as Try.Failure).exceptionOrNull()

        assertEquals(DomainError.Network.ServerError(429), error)
    }
}
