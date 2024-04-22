package no.nav.tiltakspenger.skjerming.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tiltakspenger.libs.skjerming.SkjermingDTO
import no.nav.tiltakspenger.libs.skjerming.SkjermingPersonDTO
import no.nav.tiltakspenger.libs.skjerming.SkjermingResponsDTO
import no.nav.tiltakspenger.skjerming.klient.SkjermingKlient

private val LOG = KotlinLogging.logger {}
private val SECURELOG = KotlinLogging.logger("tjenestekall")

class SkjermingService(
    rapidsConnection: RapidsConnection?,
    private val skjermingKlient: SkjermingKlient,
) : River.PacketListener {
    companion object {
        internal object BEHOV {
            const val SKJERMING = "skjerming"
        }
    }

    init {
        River(rapidsConnection!!).apply {
            validate {
                it.demandAllOrAny("@behov", listOf(BEHOV.SKJERMING))
                it.forbid("@løsning")
                it.requireKey("@id", "@behovId")
                it.requireKey("ident")
                it.interestedIn("barn")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        try {
            loggVedInngang(packet)

            withLoggingContext(
                "id" to packet["@id"].asText(),
                "behovId" to packet["@behovId"].asText(),
            ) {
                val ident = packet["ident"].asText()
                val barn = packet["barn"].map { it.asText() }
                val behovId = packet["@behovId"].asText()
                SECURELOG.debug { "mottok ident $ident og barn $barn" }

                val respons = runBlocking(MDCContext()) {
                    SkjermingResponsDTO(
                        skjermingForPersoner = SkjermingDTO(
                            søker = SkjermingPersonDTO(
                                ident = ident,
                                skjerming = skjermingKlient.hentSkjermingInfoMedAzure(
                                    ident = ident,
                                    callId = behovId,
                                ),
                            ),
                            barn = barn.map {
                                SkjermingPersonDTO(
                                    ident = it,
                                    skjerming = skjermingKlient.hentSkjermingInfoMedAzure(
                                        ident = ident,
                                        callId = behovId,
                                    ),
                                )
                            },
                        ),
                        feil = null,
                    )
                }

                packet["@løsning"] = mapOf(
                    BEHOV.SKJERMING to respons,
                )
                loggVedUtgang(packet)
                context.publish(ident, packet.toJson())
            }
        } catch (e: Exception) {
            loggVedFeil(e, packet)
            throw e
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        LOG.info { "meldingen validerte ikke: $problems" }
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
    }

    fun loggVedInngang(packet: JsonMessage) {
        LOG.info(
            "løser behov med {} og {}",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("behovId", packet["@behovId"].asText()),
        )
        SECURELOG.info(
            "løser behov med {} og {}",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("behovId", packet["@behovId"].asText()),
        )
        SECURELOG.debug { "mottok melding: ${packet.toJson()}" }
    }

    private fun loggVedUtgang(packet: JsonMessage) {
        LOG.info(
            "har løst behov med {} og {}",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("behovId", packet["@behovId"].asText()),
        )
        SECURELOG.info(
            "har løst behov med {} og {}",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("behovId", packet["@behovId"].asText()),
        )
        SECURELOG.debug { "publiserer melding: ${packet.toJson()}" }
    }

    private fun loggVedFeil(ex: Throwable, packet: JsonMessage) {
        LOG.error(
            "feil ved behandling av behov med {}, se securelogs for detaljer",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("behovId", packet["@behovId"].asText()),
        )
        SECURELOG.error(
            "feil ${ex.message} ved behandling av behov med {} og {}",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("behovId", packet["@behovId"].asText()),
            ex,
        )
    }

    suspend fun hentSkjermingInfoMedAzure(ident: String, barn: List<String>, callId: String): SkjermingResponsDTO {
        return SkjermingResponsDTO(
            skjermingForPersoner = SkjermingDTO(
                søker = SkjermingPersonDTO(
                    ident = ident,
                    skjerming = skjermingKlient.hentSkjermingInfoMedAzure(ident, callId),
                ),
                barn = barn.map {
                    SkjermingPersonDTO(
                        ident = it,
                        skjerming = skjermingKlient.hentSkjermingInfoMedAzure(
                            ident = ident,
                            callId = callId,
                        ),
                    )
                },
            ),
        )
    }
}
