package no.nav.tiltakspenger.skjerming

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
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
        private val LOG = KotlinLogging.logger {}

        internal object BEHOV {
            const val SKJERMING = "skjerming"
        }
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.requireAllOrAny("@behov", listOf(BEHOV.SKJERMING))
                it.forbid("@løsning")
                it.requireKey("@id", "@behovId")
                it.demandKey("ident")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        LOG.info { "Received packet: ${packet.toJson()}" }
        val ident = packet["ident"].asText()
        val behovId = packet["@behovId"].asText()

        val erSkjermet = runBlocking {
            skjermingKlient.erSkjermetPerson(
                fødselsnummer = ident,
                behovId = behovId,
            )
        }
        LOG.debug { "Received ident $ident" }
        packet["@løsning"] = mapOf(
            BEHOV.SKJERMING to erSkjermet
        )
        LOG.info { "Sending skjermet: $erSkjermet" }
        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        LOG.debug { problems }
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
        LOG.error { error }
    }
}
