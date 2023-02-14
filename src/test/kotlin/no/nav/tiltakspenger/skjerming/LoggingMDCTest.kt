package no.nav.tiltakspenger.skjerming

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.rapids_rivers.withMDC
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.MDC

private val LOG = KotlinLogging.logger {}

@Disabled
class LoggingMDCTest {

    @Test
    fun `manuell test av mdc`() {
        LOG.info("Sjekk 1")
        MDC.put("foo", "bar")
        withMDC("behovId" to "123") {
            LOG.info("Sjekk 2")
            runCatching {
                LOG.info("Sjekk 3")
                runBlocking(MDCContext()) {
                    LOG.info("Sjekk 4")
                    MDC.put("one", "more")
                    launch(MDCContext()) {
                        // Uten MDCContext() så blir ikke "one":"more" med i 4.1, bare i 4.2
                        LOG.info("Sjekk 4.1")
                        LOG.info(
                            "Sjekk 4.3",
                            StructuredArguments.keyValue("woop", "woop"),
                            StructuredArguments.keyValue("foo", "bar"),
                        )
                    }
                    LOG.info("Sjekk 4.2")
                }
                LOG.info("Sjekk 5")
            }
            LOG.info("Sjekk 6")
        }
        LOG.info("Sjekk 7")
    }

    @Test
    fun `manuell test av structured arguments kombinert med mdc`() {
        // Structured arguments loggges på samme måte som MDC verdier, så å ha begge deler blir smør på flesk..
        withLoggingContext("foo" to "bar") {
            runBlocking(MDCContext()) {
                LOG.info(
                    "Sjekk her {}", // Logges som "Sjekk her foo=bar"
                    StructuredArguments.keyValue("foo", "bar"),
                )
            }
        }
        withLoggingContext() {
            runBlocking(MDCContext()) {
                LOG.info(
                    "Sjekk her {}", // Logges som "Sjekk her foo=bar"
                    StructuredArguments.keyValue("foo", "bar"),
                )
            }
        }
    }
}
