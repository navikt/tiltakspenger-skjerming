package no.nav.tiltakspenger.skjerming

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tiltakspenger.skjerming.klient.SkjermingKlient

private val LOG = KotlinLogging.logger {}

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { _, e -> LOG.error(e) { e.message } }
    
    RapidApplication.create(Configuration.rapidsAndRivers).apply {

        SkjermingService(
            rapidsConnection = this,
            skjermingKlient = SkjermingKlient()
        )

        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                LOG.info { "Starting tiltakspenger-skjerming" }
            }

            override fun onShutdown(rapidsConnection: RapidsConnection) {
                LOG.info { "Stopping tiltakspenger-skjerming" }
                super.onShutdown(rapidsConnection)
            }
        })
    }.start()
}
