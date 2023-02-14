package no.nav.tiltakspenger.skjerming.klient

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.skjerming.defaultObjectMapper
import no.nav.tiltakspenger.skjerming.oauth.TokenProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class SkjermingKlientTest {

    companion object {
        const val accessToken = "woopwoop"
    }

    @Test
    fun `skal inkludere Azure token i header`() {
        var actualAuthHeader: String? = null
        val mockEngine = MockEngine { request ->
            actualAuthHeader = request.headers["Authorization"]
            when (request.url.toString()) {
                "http://localhost:8080/skjermet" -> respond(
                    content = """true""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )

                else -> throw RuntimeException("Should not happen")
            }
        }
        val client = SkjermingKlient(
            skjermingConfig = SkjermingKlient.SkjermingKlientConfig(baseUrl = "http://localhost:8080"),
            objectMapper = defaultObjectMapper(),
            getToken = { accessToken },
            engine = mockEngine,
        )

        runBlocking {
            client.erSkjermetPerson(
                fødselsnummer = "x",
                behovId = "y",
            )
        }
        assertEquals("Bearer $accessToken", actualAuthHeader)
    }

    @Test
    fun `skal klare å deserialisere bodyen som returneres`() {
        val mockEngine = MockEngine { request ->
            println("URL er ${request.url}")
            when (request.url.toString()) {
                "http://localhost:8080/skjermet" -> respond(
                    content = "true",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )

                else -> throw RuntimeException("Should not happen")
            }
        }
        val client = SkjermingKlient(
            skjermingConfig = SkjermingKlient.SkjermingKlientConfig(baseUrl = "http://localhost:8080"),
            objectMapper = defaultObjectMapper(),
            getToken = { accessToken },
            engine = mockEngine,
        )

        val erSkjermet = runBlocking {
            client.erSkjermetPerson(
                fødselsnummer = "x",
                behovId = "y",
            )
        }
        assertEquals(true, erSkjermet)
    }

    @Test
    fun `skal håndtere InternalServerError`() {
        val tokenProvider = mockk<TokenProvider>()
        coEvery { tokenProvider.getToken() } returns "woopwoop"

        val mockEngine = MockEngine { request ->
            println("URL er ${request.url}")
            when (request.url.toString()) {
                "http://localhost:8080/skjermet" -> respondError(
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )

                else -> throw RuntimeException("Should not happen")
            }
        }
        val client = SkjermingKlient(
            skjermingConfig = SkjermingKlient.SkjermingKlientConfig(baseUrl = "http://localhost:8080"),
            objectMapper = defaultObjectMapper(),
            getToken = { accessToken },
            engine = mockEngine,
        )

        assertThrows(ServerResponseException::class.java) {
            runBlocking {
                client.erSkjermetPerson(
                    fødselsnummer = "x",
                    behovId = "y",
                )
            }
        }
    }
}
