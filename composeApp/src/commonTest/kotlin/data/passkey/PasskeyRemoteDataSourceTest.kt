package data.passkey

import core.common.getOrNull
import data.core.network.client.ApiClient
import data.core.network.mapper.ApiResponseMapper
import data.passkey.remote.PasskeyRemoteDataSource
import data.passkey.remote.PasskeyResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class PasskeyRemoteDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()
    private val bodies = mutableListOf<String>()

    private fun dataSource(responseBody: String) = PasskeyRemoteDataSource(
        ApiClient(
            "https://api.test",
            HttpClient(
                MockEngine { request ->
                    requests += request
                    bodies += request.body.toByteArray().decodeToString()
                    respond(responseBody, headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
                }
            ) { install(ContentNegotiation) { json(json) } },
            ApiResponseMapper(),
        )
    )

    @Test
    fun `sign-in options come back as the server's WebAuthn JSON, untouched`() = runTest {
        val options = """{"challenge":"abc","rpId":"alirezaiyan.com","timeout":300000,"allowCredentials":[]}"""

        val result = dataSource("""{"success":true,"data":$options}""").signInOptions().getOrNull()

        assertEquals("https://api.test/auth/passkeys/sign-in-options", requests.single().url.toString())
        assertEquals(HttpMethod.Post, requests.single().method)
        assertEquals(json.parseToJsonElement(options), json.parseToJsonElement(requireNotNull(result)))
    }

    @Test
    fun `register sends the device response as a string field`() = runTest {
        val response = """{"id":"cred","type":"public-key"}"""

        val passkey = dataSource("""{"success":true,"data":{"id":7,"createdAt":1000,"synced":true}}""")
            .register(response).getOrNull()

        assertEquals("https://api.test/auth/passkeys", requests.single().url.toString())
        val sent = json.parseToJsonElement(bodies.single()).jsonObject
        assertEquals(response, sent.getValue("responseJson").jsonPrimitive.content)
        assertEquals(PasskeyResponse(id = 7, createdAt = 1000, lastUsedAt = null, synced = true), passkey)
    }
}
