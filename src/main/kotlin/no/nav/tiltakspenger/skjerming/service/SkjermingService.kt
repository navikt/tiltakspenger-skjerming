package no.nav.tiltakspenger.skjerming.service

import mu.KotlinLogging
import no.nav.tiltakspenger.skjerming.klient.SkjermingKlient

private val LOG = KotlinLogging.logger {}

class SkjermingService(
    private val skjermingKlient: SkjermingKlient,
) {
    suspend fun hentSkjermingInfoMedAzure(ident: String, callId: String): Boolean =
        skjermingKlient.erSkjermetPerson(ident, callId)
}
