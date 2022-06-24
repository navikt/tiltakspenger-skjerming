package no.nav.tiltakspenger.skjerming.oauth

fun interface TokenProvider {

    fun getToken(): String
}
