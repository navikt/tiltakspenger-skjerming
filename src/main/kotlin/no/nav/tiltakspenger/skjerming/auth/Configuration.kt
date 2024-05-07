package no.nav.tiltakspenger.skjerming.auth

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

object Configuration {

    private val defaultProperties = ConfigurationMap(
        mapOf(
            "application.httpPort" to 8080.toString(),
            "AZURE_APP_CLIENT_ID" to System.getenv("AZURE_APP_CLIENT_ID"),
            "AZURE_APP_CLIENT_SECRET" to System.getenv("AZURE_APP_CLIENT_SECRET"),
            "AZURE_APP_WELL_KNOWN_URL" to System.getenv("AZURE_APP_WELL_KNOWN_URL"),
        ),
    )

    private val localProperties = ConfigurationMap(
        mapOf(
            "stsUrl" to "",
            "application.profile" to Profile.LOCAL.toString(),
            "SKJERMING_SCOPE" to "api://dev-gcp.nom.skjermede-personer-pip/.default",
            "SKJERMING_BASE_URL" to "https://skjermede-personer-pip.intern.dev.nav.no",
            "AZURE_APP_CLIENT_ID" to "azure_test_client_id",
            "AZURE_APP_CLIENT_SECRET" to "Azure_test_client_secret",
            "AZURE_APP_WELL_KNOWN_URL" to "http://localhost:8080/default/.well-known/openid-configuration",
        ),
    )
    private val devProperties = ConfigurationMap(
        mapOf(
            "stsUrl" to "https://sts-q1.preprod.local/SecurityTokenServiceProvider/",
            "application.profile" to Profile.DEV.toString(),
            "SKJERMING_SCOPE" to "api://dev-gcp.nom.skjermede-personer-pip/.default",
            "SKJERMING_BASE_URL" to "https://skjermede-personer-pip.intern.dev.nav.no",
        ),
    )
    private val prodProperties = ConfigurationMap(
        mapOf(
            "stsUrl" to "https://sts.adeo.no/SecurityTokenServiceProvider/",
            "application.profile" to Profile.PROD.toString(),
            "SKJERMING_SCOPE" to "api://prod-gcp.nom.skjermede-personer-pip/.default",
            "SKJERMING_BASE_URL" to "https://skjermede-personer-pip.intern.nav.no",
        ),
    )

    private fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "dev-gcp" ->
            systemProperties() overriding EnvironmentVariables overriding devProperties overriding defaultProperties

        "prod-gcp" ->
            systemProperties() overriding EnvironmentVariables overriding prodProperties overriding defaultProperties

        else -> {
            systemProperties() overriding EnvironmentVariables overriding localProperties overriding defaultProperties
        }
    }

    fun httpPort() = config()[Key("application.httpPort", intType)]

    fun oauthAzureConfig(
        scope: String = config()[Key("SKJERMING_SCOPE", stringType)],
        clientId: String = config()[Key("AZURE_APP_CLIENT_ID", stringType)],
        clientSecret: String = config()[Key("AZURE_APP_CLIENT_SECRET", stringType)],
        wellknownUrl: String = config()[Key("AZURE_APP_WELL_KNOWN_URL", stringType)],
    ) = AzureTokenProvider.OauthAzureConfig(
        scope = scope,
        clientId = clientId,
        clientSecret = clientSecret,
        wellknownUrl = wellknownUrl,
    )

    fun skjermingKlientConfig(baseUrl: String = config()[Key("SKJERMING_BASE_URL", stringType)]) =
        SkjermingKlientConfig(baseUrl = baseUrl)

    data class SkjermingKlientConfig(
        val baseUrl: String,
    )
}

enum class Profile {
    LOCAL, DEV, PROD
}
