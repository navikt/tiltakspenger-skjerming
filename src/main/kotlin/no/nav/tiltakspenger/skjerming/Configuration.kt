package no.nav.tiltakspenger.skjerming

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.tiltakspenger.skjerming.klient.SkjermingKlient
import no.nav.tiltakspenger.skjerming.oauth.AzureTokenProvider

object Configuration {

    val rapidsAndRivers = mapOf(
        "RAPID_APP_NAME" to "tiltakspenger-skjerming",
        "KAFKA_BROKERS" to System.getenv("KAFKA_BROKERS"),
        "KAFKA_CREDSTORE_PASSWORD" to System.getenv("KAFKA_CREDSTORE_PASSWORD"),
        "KAFKA_TRUSTSTORE_PATH" to System.getenv("KAFKA_TRUSTSTORE_PATH"),
        "KAFKA_KEYSTORE_PATH" to System.getenv("KAFKA_KEYSTORE_PATH"),
        "KAFKA_RAPID_TOPIC" to "tpts.rapid.v1",
        "KAFKA_RESET_POLICY" to "latest",
        "KAFKA_CONSUMER_GROUP_ID" to "tiltakspenger-skjerming-v1",
    )

    private val otherDefaultProperties = mapOf(
        "application.httpPort" to 8080.toString(),
        "SERVICEUSER_TPTS_USERNAME" to System.getenv("SERVICEUSER_TPTS_USERNAME"),
        "SERVICEUSER_TPTS_PASSWORD" to System.getenv("SERVICEUSER_TPTS_PASSWORD"),
        "AZURE_APP_CLIENT_ID" to System.getenv("AZURE_APP_CLIENT_ID"),
        "AZURE_APP_CLIENT_SECRET" to System.getenv("AZURE_APP_CLIENT_SECRET"),
        "AZURE_APP_WELL_KNOWN_URL" to System.getenv("AZURE_APP_WELL_KNOWN_URL"),
    )
    private val defaultProperties = ConfigurationMap(rapidsAndRivers + otherDefaultProperties)

    private val localProperties = ConfigurationMap(
        mapOf(
            "stsUrl" to "",
            "application.profile" to Profile.LOCAL.toString(),
            "skjermingScope" to "api://dev-gcp.nom.skjermede-personer-pip/.default",
            "skjermingBaseUrl" to "https://skjermede-personer-pip.intern.dev.nav.no",
        ),
    )
    private val devProperties = ConfigurationMap(
        mapOf(
            "stsUrl" to "https://sts-q1.preprod.local/SecurityTokenServiceProvider/",
            "application.profile" to Profile.DEV.toString(),
            "skjermingScope" to "api://dev-gcp.nom.skjermede-personer-pip/.default",
            "skjermingBaseUrl" to "https://skjermede-personer-pip.intern.dev.nav.no",
        ),
    )
    private val prodProperties = ConfigurationMap(
        mapOf(
            "stsUrl" to "",
            "application.profile" to Profile.PROD.toString(),
            "skjermingScope" to "api://prod-gcp.nom.skjermede-personer-pip/.default",
            "skjermingBaseUrl" to "https://skjermede-personer-pip.intern.nav.no",
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

    fun oauthConfig(
        scope: String = config()[Key("skjermingScope", stringType)],
        clientId: String = config()[Key("AZURE_APP_CLIENT_ID", stringType)],
        clientSecret: String = config()[Key("AZURE_APP_CLIENT_SECRET", stringType)],
        wellknownUrl: String = config()[Key("AZURE_APP_WELL_KNOWN_URL", stringType)],
    ) = AzureTokenProvider.OauthConfig(
        scope = scope,
        clientId = clientId,
        clientSecret = clientSecret,
        wellknownUrl = wellknownUrl,
    )

    fun skjermingKlientConfig(baseUrl: String = config()[Key("skjermingBaseUrl", stringType)]) =
        SkjermingKlient.SkjermingKlientConfig(baseUrl = baseUrl)
}

enum class Profile {
    LOCAL, DEV, PROD
}
