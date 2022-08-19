package no.nav.tiltakspenger.skjerming

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tiltakspenger.skjerming.klient.SkjermingKlient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class SkjermingServiceTest {

    private val ident = "10101012345"
    private val testRapid = TestRapid()
    private val skjermingKlientMock = mockk<SkjermingKlient>()

    init {
        SkjermingService(
            rapidsConnection = testRapid,
            skjermingKlient = skjermingKlientMock
        )
    }

    @Test
    fun `Skal hente skjerming data og legge på kafka `() {

        coEvery { skjermingKlientMock.erSkjermetPerson(fødselsnummer = ident, behovId = any()) } returns true

        testRapid.sendTestMessage(behovsmelding())
        with(testRapid.inspektør) {
            // Kommenter inn denne for å se meldingen som publiseres
            // println(this.message(0))
            Assertions.assertEquals(1, size)
            Assertions.assertEquals("behov", field(0, "@event_name").asText())
            Assertions.assertEquals("skjerming", field(0, "@behov")[0].asText())
            Assertions.assertEquals(ident, field(0, "ident").asText())
            Assertions.assertEquals(true, field(0, "@løsning").get("skjerming").asBoolean())
        }
    }

    // language=JSON
    private fun behovsmelding(): String =
        """
        {
            "@event_name" : "behov",
            "@opprettet" : "2022-06-27T11:36:31.346814",
            "@id": "ed2fc977-2188-4bac-be16-7226dba5b9ea", 
            "@behovId": "ed2fc977-2188-4bac-be16-7226dba5b9ea", 
            "@behov" : [ "skjerming" ],
            "ident" : "$ident",
            "tilstand" : "WhateverType", 
            "system_read_count" : 0,
            "system_participating_services" : [ 
                { "id" : "ed2fc977-2188-4bac-be16-7226dba5b9ea", "time" : "2022-06-27T11:36:31.389679 "}
            ]
        }""".trimIndent()


    @Suppress("UnusedPrivateMember")
    // language=JSON
    private val publiseringLøsning: String =
        """
            {
              "@event_name": "behov",
              "@opprettet": "2022-08-19T13:50:43.520712",
              "@id": "2235e4d9-fe61-4db7-a171-ee4bdb68f484",
              "@behovId": "ed2fc977-2188-4bac-be16-7226dba5b9ea",
              "@behov": [
                "skjerming"
              ],
              "ident": "10101012345",
              "tilstand": "WhateverType",
              "system_read_count": 1,
              "system_participating_services": [
                {
                  "id": "ed2fc977-2188-4bac-be16-7226dba5b9ea",
                  "time": "2022-06-27T11:36:31.389679 "
                },
                {
                  "id": "ed2fc977-2188-4bac-be16-7226dba5b9ea",
                  "time": "2022-08-19T13:50:43.425617"
                },
                {
                  "id": "2235e4d9-fe61-4db7-a171-ee4bdb68f484",
                  "time": "2022-08-19T13:50:43.520712"
                }
              ],
              "@løsning": {
                "skjerming": true
              },
              "@forårsaket_av": {
                "id": "ed2fc977-2188-4bac-be16-7226dba5b9ea",
                "opprettet": "2022-06-27T11:36:31.346814",
                "event_name": "behov",
                "behov": [
                  "skjerming"
                ]
              }
            }
        """.trimIndent()
}
