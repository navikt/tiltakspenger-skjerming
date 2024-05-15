package no.nav.tiltakspenger.skjerming.auth

import com.nimbusds.jwt.SignedJWT
import configureTestApplication
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltakspenger.libs.skjerming.SkjermingDTO
import no.nav.tiltakspenger.libs.skjerming.SkjermingPersonDTO
import no.nav.tiltakspenger.libs.skjerming.SkjermingResponsDTO
import no.nav.tiltakspenger.skjerming.service.SkjermingService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class TokenValidationTest {

    fun validTestToken(expiry: Long = 3600): SignedJWT {
        return mockOAuth2Server.issueToken(
            issuerId = "azure",
            audience = "audience",
            expiry = expiry,
        )
    }

    fun tokenMedFeilAudience(expiry: Long = 3600): SignedJWT {
        return mockOAuth2Server.issueToken(
            issuerId = "azure",
            audience = "tull",
            expiry = expiry,
        )
    }

    val mockedSkjermingService: SkjermingService = mockk<SkjermingService>().also {
        coEvery { it.hentSkjermingInfoMedAzure(any(), any(), any()) } returns SkjermingResponsDTO(
            skjermingForPersoner = SkjermingDTO(
                søker = SkjermingPersonDTO(
                    ident = "123",
                    skjerming = false,
                ),
                barn = listOf(
                    SkjermingPersonDTO(
                        ident = "456",
                        skjerming = false,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `post med ugyldig token skal gi 401`() {
        testApplication {
            configureTestApplication(mockedSkjermingService)
            val response = client.post("/azure/skjermet") {
                contentType(type = ContentType.Application.Json)
                header("Authorization", "Bearer ugyldigtoken")
            }
            Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `post med token med feil audience skal gi 401`() {
        val token = tokenMedFeilAudience()
        testApplication {
            configureTestApplication(mockedSkjermingService)
            val response = client.post("/azure/skjermet") {
                contentType(type = ContentType.Application.Json)
                header("Authorization", "Bearer ${token.serialize()}")
            }
            Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `post med gyldig token skal gi 200`() {
        val token = validTestToken()
        testApplication {
            configureTestApplication(mockedSkjermingService)
            val response = client.post("/azure/skjermet") {
                contentType(type = ContentType.Application.Json)
                header("Authorization", "Bearer ${token.serialize()}")
                setBody("""{"ident":"123","barn":["456"]}""")
            }
            Assertions.assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    companion object {

        val mockOAuth2Server = MockOAuth2Server()

        @JvmStatic
        @BeforeAll
        fun setup(): Unit = mockOAuth2Server.start(8080)

        @JvmStatic
        @AfterAll
        fun after(): Unit = mockOAuth2Server.shutdown()
    }
}
