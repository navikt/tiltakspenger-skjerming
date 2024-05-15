package no.nav.tiltakspenger.skjerming.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.skjerming.service.SkjermingService

private val LOG = KotlinLogging.logger {}
const val AZURE_SKJERMING_PATH = "/azure/skjermet"

data class RequestBody(
    val ident: String,
    val barn: List<String>,
)

fun Route.AzureRoutes(skjermingService: SkjermingService) {
    post(AZURE_SKJERMING_PATH) {
        LOG.info { "Mottatt forespørsel på $AZURE_SKJERMING_PATH for å hente data om bruker skljermet" }

        val res = call.receive<RequestBody>()
        val ident = res.ident
        val barn = res.barn
        val response = skjermingService.hentSkjermingInfoMedAzure(ident, barn, call.callId!!)

        call.respond(status = HttpStatusCode.OK, message = response)
    }
}
