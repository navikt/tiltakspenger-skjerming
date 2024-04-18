package no.nav.tiltakspenger.skjerming.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.callid.callId
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import mu.KotlinLogging
import no.nav.tiltakspenger.skjerming.auth.getFnrForAzureToken
import no.nav.tiltakspenger.skjerming.service.SkjermingService

private val LOG = KotlinLogging.logger {}
const val AZURE_SKJERMING_PATH = "/azure/skjerming"
fun Route.AzureRoutes(skjermingService: SkjermingService) {
    get(AZURE_SKJERMING_PATH) {
        LOG.info { "Mottatt forespørsel på $AZURE_SKJERMING_PATH for å hente data om bruker skljermet" }

        val ident = call.getFnrForAzureToken() ?: throw IllegalStateException("Mangler fødselsnummer")
        val response = skjermingService.hentSkjermingInfoMedAzure(ident, emptyList(), callId = call.callId!!)

        call.respond(status = HttpStatusCode.OK, message = response)
    }
}
