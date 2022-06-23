package no.nav.tiltakspenger.skjerming.oauth

interface TokenProvider {

    suspend fun getToken(): String
}