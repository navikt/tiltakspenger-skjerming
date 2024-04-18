package no.nav.tiltakspenger.skjerming.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.callid.callId
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import mu.KotlinLogging
import no.nav.tiltakspenger.skjerming.auth.getFnrForTokenx
import no.nav.tiltakspenger.skjerming.auth.token
import no.nav.tiltakspenger.skjerming.service.SkjermingService

private val LOG = KotlinLogging.logger {}
const val TOKENX_SKJERMING_PATH = "/tokenx/skjerming"
fun Route.TokenxRoutes(skjermingService: SkjermingService) {
    get(TOKENX_SKJERMING_PATH) {
        LOG.info { "Mottatt forespørsel på $TOKENX_SKJERMING_PATH for å hente data om bruker skjermet" }
        val ident = call.getFnrForTokenx() ?: throw IllegalStateException("Mangler fødselsnummer")
        val response = skjermingService.hentSkjermingInfoMedTokenx(ident, emptyList(), call.callId!!, call.token())

        call.respond(status = HttpStatusCode.OK, message = response)
    }
}
