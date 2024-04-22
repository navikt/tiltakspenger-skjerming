package no.nav.tiltakspenger.skjerming

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tiltakspenger.skjerming.klient.SkjermingKlient
import no.nav.tiltakspenger.skjerming.service.SkjermingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

internal class SkjermingServiceTest {
    private val testRapid = TestRapid()

    private val ident = "04927799109"
    private val barn1 = "07081512345"
    private val barn2 = "01010512345"

    private val skjermingKlient = mockk<SkjermingKlient>()

    val service = SkjermingService(
        rapidsConnection = testRapid,
        skjermingKlient = skjermingKlient,
    )

    @BeforeEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `Sjekk happy case`() {
        coEvery { skjermingKlient.erSkjermetPerson(any(), any()) } returns true

        testRapid.sendTestMessage(behovMelding)

        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "ident").asText() shouldBe ident

            JSONAssert.assertEquals(
                svar,
                message(0).toPrettyString(),
                JSONCompareMode.LENIENT,
            )
        }
    }

    private val svar = """
            {
              "@løsning": {
                "skjerming": {
                  "skjermingForPersoner": {
                      "søker": {
                        "ident": "$ident",
                        "skjerming": true
                      },
                      "barn": [
                        {
                          "ident": "$barn1",
                          "skjerming": true
                        },
                        {
                          "ident": "$barn2",
                          "skjerming": true
                        }
                      ]
                    },
                    "feil": null
                  }
              }
            }
    """.trimIndent()

    private val behovMelding = """
        {
            "@event_name": "behov",
            "@opprettet": "2023-01-17T12:50:54.875468981",
            "@id": "f51435b1-c993-4ca8-92ff-f62f3d4f2ebc",
            "@behovId": "dfe8e0cc-83ab-4182-96f8-6b5a49ce5b8b",
            "@behov": [
            "skjerming"
            ],
            "journalpostId": "foobar3",
            "tilstandtype": "AvventerPersonopplysninger",
            "ident": "$ident",
            "barn": [
                "$barn1",
                "$barn2"
            ],
            "system_read_count": 0,
            "system_participating_services": [
            {
                "id": "f51435b1-c993-4ca8-92ff-f62f3d4f2ebc",
                "time": "2023-01-17T12:50:54.895176586"
            }
            ]
        }
    """.trimIndent()
}
