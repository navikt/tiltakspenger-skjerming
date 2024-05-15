
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import no.nav.security.token.support.v2.RequiredClaims
import no.nav.security.token.support.v2.tokenValidationSupport
import no.nav.tiltakspenger.skjerming.ISSUER
import no.nav.tiltakspenger.skjerming.installJacksonFeature
import no.nav.tiltakspenger.skjerming.routes.AzureRoutes
import no.nav.tiltakspenger.skjerming.service.SkjermingService
import java.util.UUID

fun ApplicationTestBuilder.configureTestApplication(
    skjermingService: SkjermingService = mockk(),
) {
    application {
        install(CallId) {
            generate { UUID.randomUUID().toString() }
        }
        install(Authentication) {
            tokenValidationSupport(
                name = ISSUER.AZURE.value,
                config = ApplicationConfig("application.test.conf"),
                requiredClaims = RequiredClaims(
                    issuer = ISSUER.AZURE.value,
                    claimMap = arrayOf(),
                    combineWithOr = false,
                ),
            )
        }
        routing {
            authenticate(ISSUER.AZURE.value) {
                AzureRoutes(skjermingService)
            }
        }
        installJacksonFeature()
    }
}
