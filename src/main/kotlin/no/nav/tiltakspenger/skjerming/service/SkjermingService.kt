package no.nav.tiltakspenger.skjerming.service

import mu.KotlinLogging
import no.nav.tiltakspenger.libs.skjerming.SkjermingDTO
import no.nav.tiltakspenger.libs.skjerming.SkjermingPersonDTO
import no.nav.tiltakspenger.libs.skjerming.SkjermingResponsDTO
import no.nav.tiltakspenger.skjerming.klient.SkjermingKlient

private val LOG = KotlinLogging.logger {}

class SkjermingService(
    private val skjermingKlient: SkjermingKlient,
) {
    suspend fun hentSkjermingInfoMedAzure(ident: String, barn: List<String>, callId: String): SkjermingResponsDTO {
        LOG.info { "Start mapping av skjerming respons dto" }
        return SkjermingResponsDTO(
            skjermingForPersoner = SkjermingDTO(
                søker = SkjermingPersonDTO(
                    ident = ident,
                    skjerming = skjermingKlient.erSkjermetPerson(ident, callId),
                ),
                barn = barn.map {
                    SkjermingPersonDTO(
                        ident = it,
                        skjerming = skjermingKlient.erSkjermetPerson(it, callId),
                    )
                },
            ),
        )
    }
}
