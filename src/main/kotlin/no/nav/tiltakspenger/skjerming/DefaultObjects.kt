package no.nav.tiltakspenger.skjerming

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import java.time.Duration
import mu.KotlinLogging

private val SECURELOG = KotlinLogging.logger("tjenestekall")

private object SecurelogWrapper : Logger {
    override fun log(message: String) {
        SECURELOG.info(message)
    }
}

@Suppress("MagicNumber")
fun defaultHttpClient(
    objectMapper: ObjectMapper,
    engine: HttpClientEngine = CIO.create(),
    configBlock: HttpClientConfig<*>.() -> Unit = {}
) = HttpClient(engine) {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    install(HttpTimeout) {
        connectTimeoutMillis = Duration.ofSeconds(60).toMillis()
        requestTimeoutMillis = Duration.ofSeconds(60).toMillis()
        socketTimeoutMillis = Duration.ofSeconds(60).toMillis()
    }

    this.install(Logging) {
        logger = SecurelogWrapper
        level = LogLevel.ALL
    }
    this.expectSuccess = true

    apply(configBlock)
}

fun defaultObjectMapper(): ObjectMapper = ObjectMapper()
    .registerModule(KotlinModule.Builder().build())
    .registerModule(JavaTimeModule())
    .setDefaultPrettyPrinter(
        DefaultPrettyPrinter().apply {
            indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
            indentObjectsWith(DefaultIndenter("  ", "\n"))
        }
    )
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
