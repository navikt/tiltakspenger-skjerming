package no.nav.tiltakspenger.skjerming.oauth

fun interface TokenProvider {

    suspend fun getToken(): String
}
