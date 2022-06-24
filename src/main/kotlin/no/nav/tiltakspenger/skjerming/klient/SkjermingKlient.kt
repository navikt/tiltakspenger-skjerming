package no.nav.tiltakspenger.skjerming.klient

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.tiltakspenger.skjerming.Configuration
import no.nav.tiltakspenger.skjerming.defaultHttpClient
import no.nav.tiltakspenger.skjerming.defaultObjectMapper

class SkjermingKlient(
    private val skjermingConfig: Configuration.SkjermingKlientConfig = Configuration.SkjermingKlientConfig(),
    private val objectMapper: ObjectMapper = defaultObjectMapper(),
    private val tokenProviderBlock: suspend () -> String,
    engine: HttpClientEngine = CIO.create(),
    private val httpClient: HttpClient = defaultHttpClient(
        objectMapper = objectMapper,
        engine = engine
    ) {
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(
                        accessToken = tokenProviderBlock.invoke(),
                        // Refresh token are used in refreshToken method if client gets 401
                        // Should't need this if token expiry is checked first
                        refreshToken = emptyRefreshToken,
                    )
                }
            }
        }
    }
) {
    companion object {
        const val emptyRefreshToken = ""
        const val navCallIdHeader = "Nav-Call-Id"
    }

    @Suppress("TooGenericExceptionThrown")
    suspend fun erSkjermetPerson(fødselsnummer: String, behovId: String): Boolean {
        val httpResponse = httpClient.preparePost("${skjermingConfig.baseUrl}/skjermet") {
            header(navCallIdHeader, behovId)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(SkjermetDataRequestDTO(fødselsnummer))
        }.execute()
        return when (httpResponse.status) {
            HttpStatusCode.OK -> httpResponse.call.response.body()
            else -> throw RuntimeException("error (responseCode=${httpResponse.status.value}) from Skjerming")
        }
    }

    private data class SkjermetDataRequestDTO(val personident: String)
}
