package no.nav.tiltakspenger.skjerming

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tiltakspenger.skjerming.klient.SkjermingKlient

class SkjermingService(
    rapidsConnection: RapidsConnection,
    private val skjermingKlient: SkjermingKlient,
) :
    River.PacketListener {

    companion object {
        private val logg = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")


        internal object BEHOV {
            const val SKJERMING = "skjerming"
        }
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAllOrAny("@behov", listOf(BEHOV.SKJERMING))
                it.forbid("@løsning")
                it.requireKey("@id", "@behovId")
                it.requireKey("ident")
            }
        }.register(this)
    }

    fun loggVedInngang(packet: JsonMessage) {
        logg.info(
            "løser behov med id {} og korrelasjonsid {}",
            keyValue("id", packet["@id"].asText()),
            keyValue("behovId", packet["@behovId"].asText())
        )
        sikkerlogg.info(
            "løser behov med id {} og korrelasjonsid {}",
            keyValue("id", packet["@id"].asText()),
            keyValue("behovId", packet["@behovId"].asText())
        )
        sikkerlogg.debug { "mottok melding: ${packet.toJson()}" }
        sikkerlogg.debug { "mottok melding: ${packet.toJson()}" }
    }

    private fun loggVedUtgang(packet: JsonMessage, løsning: () -> String) {
        logg.info(
            "har løst behov med id {} og korrelasjonsid {}",
            keyValue("id", packet["@id"].asText()),
            keyValue("behovId", packet["@behovId"].asText())
        )
        sikkerlogg.info(
            "har løst behov med id {} og korrelasjonsid {}",
            keyValue("id", packet["@id"].asText()),
            keyValue("behovId", packet["@behovId"].asText())
        )
        sikkerlogg.info { "publiserer løsning: $løsning" }
    }

    private fun loggVedFeil(ex: Throwable, packet: JsonMessage) {
        logg.error(
            "feil: ${ex.message} ved behandling av behov {}",
            keyValue("id", packet["@id"].asText()),
            ex
        )
        sikkerlogg.error(
            "feil: ${ex.message} ved behandling av behov {}",
            keyValue("id", packet["@id"].asText()),
            ex
        )
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        runCatching {
            loggVedInngang(packet)
            val ident = packet["ident"].asText()
            val behovId = packet["@behovId"].asText()
            sikkerlogg.debug { "mottok ident $ident" }

            val erSkjermet = runBlocking {
                skjermingKlient.erSkjermetPerson(
                    fødselsnummer = ident,
                    behovId = behovId,
                )
            }

            packet["@løsning"] = mapOf(
                BEHOV.SKJERMING to erSkjermet
            )
            loggVedUtgang(packet) { "$erSkjermet" }
            context.publish(packet.toJson())
        }.onFailure {
            loggVedFeil(it, packet)
        }.getOrThrow()
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        logg.info { "meldingen validerte ikke: $problems" }
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {}
}
