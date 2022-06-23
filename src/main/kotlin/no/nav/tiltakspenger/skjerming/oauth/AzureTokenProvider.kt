package no.nav.tiltakspenger.skjerming.oauth

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.Parameters
import no.nav.tiltakspenger.skjerming.Configuration
import no.nav.tiltakspenger.skjerming.defaultHttpClient
import no.nav.tiltakspenger.skjerming.defaultObjectMapper

class AzureTokenProvider(
    objectMapper: ObjectMapper = defaultObjectMapper(),
    engine: HttpClientEngine = CIO.create(),
    private val config: Configuration.OauthConfig = Configuration.OauthConfig(),
) : TokenProvider {
    private val azureHttpClient = defaultHttpClient(
        objectMapper = objectMapper, engine = engine
    )

    private val tokenCache = TokenCache()

    override suspend fun getToken(): String {
        val currentToken = tokenCache.token
        if (currentToken != null && !tokenCache.isExpired()) return currentToken
        return clientCredentials()
    }

    private suspend fun wellknown(): WellKnown {
        return azureHttpClient.get(config.wellknownUrl).body()
    }

    private suspend fun clientCredentials(): String {
        return azureHttpClient.submitForm(
            url = wellknown().tokenEndpoint,
            formParameters = Parameters.build {
                append("grant_type", "client_credentials")
                append("client_id", config.clientId)
                append("client_secret", config.clientSecret)
                append("scope", config.scope)
            }
        ).body<OAuth2AccessTokenResponse>().let {
            tokenCache.update(
                it.accessToken,
                it.expiresIn.toLong()
            )
            return@let it.accessToken
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class WellKnown(
    @JsonProperty("token_endpoint")
    val tokenEndpoint: String
)
