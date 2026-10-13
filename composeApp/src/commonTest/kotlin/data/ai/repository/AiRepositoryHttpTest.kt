package data.ai.repository

import core.common.Try
import core.common.exceptionOrNull
import core.error.DomainError
import data.ai.remote.AiRemoteDataSource
import data.core.network.client.ApiClient
import data.core.network.mapper.ApiResponseMapper
import domain.ai.model.AiGuidance
import domain.ai.model.AiInstruction
import domain.ai.model.Register
import domain.ai.model.VocabularyFocus
import domain.ai.model.WordType
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.model.LanguagePair
import fakes.FakeCreditsRepository
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
import io.ktor.http.content.TextContent
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        FakeCreditsRepository(),
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
    fun `extractWords reports too many requests as rate limited`() = runTest {
        val engine = MockEngine {
            respond("""{"success":false,"message":"Too many requests"}""", HttpStatusCode.TooManyRequests, jsonHeaders)
        }

        val error = (repository(engine).extractWords(image, languages) as Try.Failure).exceptionOrNull()

        assertEquals(DomainError.Network.RateLimited, error)
    }

    // ── Learner guidance ──────────────────────────────────────────────────────

    private val okItems = """{"success":true,"data":{"items":[{"term":"gehen","translation":"to go"}]}}"""

    @Test
    fun `extractWords sends quick picks and the learner's words`() = runTest {
        var body = ""
        val engine = MockEngine { request ->
            body = (request.body as TextContent).text
            respond(okItems, HttpStatusCode.OK, jsonHeaders)
        }
        val guidance = AiGuidance(
            VocabularyFocus(wordTypes = setOf(WordType.VERB), markedOnly = true),
            AiInstruction.of("only the  blue box"),
        )

        repository(engine).extractWords(image, languages, guidance)

        assertTrue(body.contains(""""instructions":"only the blue box""""), body)
        assertTrue(body.contains(""""wordTypes":["VERB"]"""), body)
        assertTrue(body.contains(""""markedOnly":true"""), body)
    }

    @Test
    fun `a plain request sends no quick picks and no instructions`() = runTest {
        var body = ""
        val engine = MockEngine { request ->
            body = (request.body as TextContent).text
            respond(okItems, HttpStatusCode.OK, jsonHeaders)
        }

        repository(engine).extractWords(image, languages)

        assertFalse(body.contains(""""focus":{"""), body)
        assertFalse(body.contains(""""instructions":""""), body)
    }

    @Test
    fun `suggestWords sends the register pick and the description`() = runTest {
        var body = ""
        val engine = MockEngine { request ->
            body = (request.body as TextContent).text
            respond("""{"success":true,"data":{"items":[{"originalWord":"Bewerbung","translation":"application"}]}}""",
                HttpStatusCode.OK, jsonHeaders)
        }
        val guidance = AiGuidance(VocabularyFocus(register = Register.FORMAL), AiInstruction.of("job interview"))

        repository(engine).suggestWords(languages, ProficiencyLevel.entries.first(), emptyList(), guidance)

        assertTrue(body.contains(""""register":"FORMAL""""), body)
        assertTrue(body.contains(""""instructions":"job interview""""), body)
    }

    private suspend fun refusal(status: HttpStatusCode, code: String): Throwable? {
        val engine = MockEngine {
            respond("""{"success":false,"message":"no","code":"$code"}""", status, jsonHeaders)
        }
        val guidance = AiGuidance(instruction = AiInstruction.of("tell me a joke"))
        return (repository(engine).extractWords(image, languages, guidance) as Try.Failure).exceptionOrNull()
    }

    @Test
    fun `refused instructions map to their reason - whatever the status`() = runTest {
        assertEquals(
            DomainError.AddWords.InstructionsRefused(DomainError.AddWords.InstructionsRefused.Reason.OffTopic),
            refusal(HttpStatusCode.UnprocessableEntity, "INSTRUCTIONS_OFF_TOPIC"),
        )
        assertEquals(
            DomainError.AddWords.InstructionsRefused(DomainError.AddWords.InstructionsRefused.Reason.Unsafe),
            refusal(HttpStatusCode.UnprocessableEntity, "INSTRUCTIONS_UNSAFE"),
        )
        assertEquals(
            DomainError.AddWords.InstructionsRefused(DomainError.AddWords.InstructionsRefused.Reason.PersonalData),
            refusal(HttpStatusCode.UnprocessableEntity, "INSTRUCTIONS_PERSONAL_DATA"),
        )
        assertEquals(
            DomainError.AddWords.InstructionsRefused(DomainError.AddWords.InstructionsRefused.Reason.Paused),
            refusal(HttpStatusCode.TooManyRequests, "INSTRUCTIONS_PAUSED"),
        )
        assertEquals(
            DomainError.AddWords.InstructionsRefused(DomainError.AddWords.InstructionsRefused.Reason.Unavailable),
            refusal(HttpStatusCode.ServiceUnavailable, "INSTRUCTIONS_UNAVAILABLE"),
        )
    }

    @Test
    fun `an unknown refusal code from a newer server reads as off-topic`() = runTest {
        assertEquals(
            DomainError.AddWords.InstructionsRefused(DomainError.AddWords.InstructionsRefused.Reason.OffTopic),
            refusal(HttpStatusCode.UnprocessableEntity, "INSTRUCTIONS_SOMETHING_NEW"),
        )
    }

    @Test
    fun `a 503 without an instructions code stays a server error`() = runTest {
        assertEquals(DomainError.Network.ServerError(503), refusal(HttpStatusCode.ServiceUnavailable, "OTHER"))
    }
}
