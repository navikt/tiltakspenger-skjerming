package no.nav.tiltakspenger.skjerming.klient

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.tiltakspenger.skjerming.Configuration
import no.nav.tiltakspenger.skjerming.felles.defaultHttpClient
import no.nav.tiltakspenger.skjerming.felles.defaultObjectMapper
import no.nav.tiltakspenger.skjerming.oauth.AzureTokenProvider
import no.nav.tiltakspenger.skjerming.oauth.TokenProvider

class SkjermingKlient(
    private val skjermingConfig: Configuration.SkjermingKlientConfig = Configuration.SkjermingKlientConfig(),
    private val objectMapper: ObjectMapper = defaultObjectMapper(),
    private val provider: TokenProvider = AzureTokenProvider(),
    engine: HttpClientEngine = CIO.create(),
    private val httpClient: HttpClient = defaultHttpClient(
        objectMapper = objectMapper,
        engine = engine
    ) {
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(
                        accessToken = provider.getToken(),
                        // Refresh token are used in refreshToken method if client gets 401
                        // Should't need this if token expiry is checked first
                        refreshToken = "",
                    )
                }
            }
        }
    }
) {
    suspend fun erSkjermetPerson(fødselsnummer: String, behovId: String): Boolean {
        val httpResponse = httpClient.preparePost("${skjermingConfig.baseUrl}/skjermet") {
            //header("Authorization", "Bearer ${tokenSupplier()}")
            header("Nav-Call-Id", behovId)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(SkjermetDataRequestDTO(fødselsnummer))
        }.execute()
        return when (httpResponse.status.value) {
            200 -> httpResponse.call.response.body()
            else -> throw RuntimeException("error (responseCode=${httpResponse.status.value}) from Skjerming")
        }
    }

    private data class SkjermetDataRequestDTO(val personident: String)
}

