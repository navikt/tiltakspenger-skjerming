package no.nav.tiltakspenger.skjerming

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

private val LOG = KotlinLogging.logger {}

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { _, e -> LOG.error(e) { e.message } }

    RapidApplication.create(Configuration.rapidsAndRivers).apply {

        SkjermingService(
            rapidsConnection = this,
        )

        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                LOG.info { "Starting tiltakspenger-arena" }
            }

            override fun onShutdown(rapidsConnection: RapidsConnection) {
                LOG.info { "Stopping tiltakspenger-arena" }
                super.onShutdown(rapidsConnection)
            }
        })
    }.start()
}
