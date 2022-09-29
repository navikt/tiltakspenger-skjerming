package no.nav.tiltakspenger.skjerming.klient

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.tiltakspenger.skjerming.Configuration
import no.nav.tiltakspenger.skjerming.defaultHttpClient
import no.nav.tiltakspenger.skjerming.defaultObjectMapper

class SkjermingKlient(
    private val skjermingConfig: SkjermingKlientConfig = Configuration.skjermingKlientConfig(),
    private val objectMapper: ObjectMapper = defaultObjectMapper(),
    private val getToken: suspend () -> String,
    engine: HttpClientEngine = CIO.create(),
    private val httpClient: HttpClient = defaultHttpClient(
        objectMapper = objectMapper,
        engine = engine
    ) {}
) {
    companion object {
        const val navCallIdHeader = "Nav-Call-Id"
    }

    @Suppress("TooGenericExceptionThrown")
    suspend fun erSkjermetPerson(fødselsnummer: String, behovId: String): Boolean {
        val httpResponse = httpClient.preparePost("${skjermingConfig.baseUrl}/skjermet") {
            header(navCallIdHeader, behovId)
            bearerAuth(getToken())
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

    data class SkjermingKlientConfig(
        val baseUrl: String,
    )
}
