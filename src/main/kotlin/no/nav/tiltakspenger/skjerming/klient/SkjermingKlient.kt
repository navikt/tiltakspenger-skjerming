package no.nav.tiltakspenger.skjerming.klient

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.tiltakspenger.person.auth.TokenProvider
import no.nav.tiltakspenger.skjerming.defaultHttpClient
import no.nav.tiltakspenger.skjerming.defaultObjectMapper
import no.nav.tiltakspenger.skjerming.auth.Configuration as SkjermingConfiguration

class SkjermingKlient(
    private val skjermingConfig: SkjermingKlientConfig = SkjermingConfiguration.skjermingKlientConfig(),
    private val objectMapper: ObjectMapper = defaultObjectMapper(),
    engine: HttpClientEngine? = null,
    private val tokenProvider: TokenProvider,
    private val httpClient: HttpClient = defaultHttpClient(
        objectMapper = objectMapper,
        engine = engine,
    ) {},
) {
    companion object {
        const val navCallIdHeader = "Nav-Call-Id"
    }

    suspend fun erSkjermetPerson(fødselsnummer: String, callId: String, token: String): Boolean {
        val httpResponse = httpClient.preparePost("${skjermingConfig.baseUrl}/skjermet") {
            header(navCallIdHeader, callId)
            bearerAuth(token)
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

    suspend fun hentSkjermingInfoMedAzure(ident: String, callId: String): Boolean {
        val token = tokenProvider.getAzureToken()
        return erSkjermetPerson(
            fødselsnummer = ident,
            callId = callId,
            token = token,
        )
    }

    suspend fun hentSkjermingInfoMedTokenx(ident: String, callId: String, subjectToken: String): Boolean {
        val token = tokenProvider.getTokenxToken(subjectToken)
        return erSkjermetPerson(
            fødselsnummer = ident,
            callId = callId,
            token = token,
        )
    }

    data class SkjermingKlientConfig(
        val baseUrl: String,
    )
}
