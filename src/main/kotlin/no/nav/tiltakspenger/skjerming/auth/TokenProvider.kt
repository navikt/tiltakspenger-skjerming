package no.nav.tiltakspenger.skjerming.auth

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import com.nimbusds.oauth2.sdk.auth.JWTAuthentication
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.Parameters
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties.TokenExchangeProperties.Companion.SUBJECT_TOKEN_TYPE_VALUE
import no.nav.security.token.support.client.core.auth.ClientAssertion
import no.nav.tiltakspenger.skjerming.defaultHttpClient
import no.nav.tiltakspenger.skjerming.defaultObjectMapper
import java.net.URI
import java.time.LocalDateTime
import no.nav.tiltakspenger.skjerming.auth.Configuration as SkjermingConfig

class TokenProvider(
    objectMapper: ObjectMapper = defaultObjectMapper(),
    engine: HttpClientEngine? = null,
    private val azureconfig: OauthAzureConfig = SkjermingConfig.oauthzureConfig(),
    private val tokenxConfig: OauthTokenxConfig = SkjermingConfig.oauthTokenxConfig(),
) {

    private val httpClient = defaultHttpClient(
        objectMapper = objectMapper,
        engine = engine,
    )

    private val tokenCache = TokenCache()

    suspend fun getAzureToken(): String {
        try {
            val currentToken = tokenCache.token
            return if (currentToken != null && !tokenCache.isExpired()) {
                currentToken
            } else {
                clientCredentials()
            }
        } catch (e: Exception) {
            throw AzureAuthException(e)
        }
    }

    suspend fun getTokenxToken(subjectToken: String): String {
        try {
            return tokenExchange(subjectToken)
        } catch (e: Exception) {
            throw TokenxAuthException(e)
        }
    }

    private suspend fun wellknown(): WellKnown {
        return httpClient.get(azureconfig.wellknownUrl).body()
    }

    private suspend fun tokenxEndpointUrl(): String {
        return httpClient.get(tokenxConfig.wellknownUrl).body<WellKnown>().tokenEndpoint
    }

    private suspend fun clientCredentials(): String {
        return httpClient.submitForm(
            url = wellknown().tokenEndpoint,
            formParameters = Parameters.build {
                append("grant_type", "client_credentials")
                append("client_id", azureconfig.clientId)
                append("client_secret", azureconfig.clientSecret)
                append("scope", azureconfig.scope)
            },
        ).body<OAuth2AccessTokenResponse>().let {
            tokenCache.update(
                it.accessToken,
                it.expiresIn.toLong(),
            )
            return@let it.accessToken
        }
    }

    private suspend fun tokenExchange(subjectToken: String): String {
        val clientAuthProperties = ClientAuthenticationProperties(
            clientId = tokenxConfig.clientId,
            clientAuthMethod = ClientAuthenticationMethod.PRIVATE_KEY_JWT,
            clientSecret = azureconfig.clientSecret,
            clientJwk = tokenxConfig.clientJwk,
        )

        val clientAssertion = ClientAssertion(URI.create(tokenxEndpointUrl()), clientAuthProperties)

        return httpClient.submitForm(
            url = wellknown().tokenEndpoint,
            formParameters = Parameters.build {
                append("grant_type", GrantType.TOKEN_EXCHANGE.value)
                append("subject_token_type", SUBJECT_TOKEN_TYPE_VALUE)
                append("subject_token", subjectToken)
                append("audience", tokenxConfig.audience)
                append("client_id", tokenxConfig.clientId)
                append("client_assertion_type", JWTAuthentication.CLIENT_ASSERTION_TYPE)
                append("client_assertion", clientAssertion.assertion())
            },
        ).body<OAuth2AccessTokenResponse>().let {
            return@let it.accessToken
        }
    }

    class TokenCache {
        var token: String? = null
            private set
        private var expires: LocalDateTime? = null

        fun isExpired(): Boolean = expires?.isBefore(LocalDateTime.now()) ?: true

        fun update(accessToken: String, expiresIn: Long) {
            token = accessToken
            expires = LocalDateTime.now().plusSeconds(expiresIn).minusSeconds(SAFETYMARGIN)
        }

        companion object {
            const val SAFETYMARGIN: Long = 60
        }
    }

    data class OauthAzureConfig(
        val scope: String,
        val clientId: String,
        val clientSecret: String,
        val wellknownUrl: String,
    )

    data class OauthTokenxConfig(
        val clientId: String,
        val clientJwk: String,
        val wellknownUrl: String,
        val audience: String,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class WellKnown(
        @JsonProperty("token_endpoint")
        val tokenEndpoint: String,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OAuth2AccessTokenResponse(
        @JsonProperty("token_type")
        val tokenType: String,
        @JsonProperty("access_token")
        var accessToken: String,
        @JsonProperty("ext_expires_in")
        val extExpiresIn: Int,
        @JsonProperty("expires_in")
        val expiresIn: Int,
    )

    class AzureAuthException(e: Exception) : RuntimeException(e)
    class TokenxAuthException(e: Exception) : RuntimeException(e)
}
