package no.nav.tiltakspenger.skjerming.oauth

import java.time.LocalDateTime

const val SAFETYMARGIN: Long = 60

class TokenCache {
    var token: String? = null
        private set
    private var expires: LocalDateTime? = null

    fun isExpired(): Boolean = expires?.isBefore(LocalDateTime.now()) ?: true

    fun update(accessToken: String, expiresIn: Long) {
        token = accessToken
        expires = LocalDateTime.now().plusSeconds(expiresIn).minusSeconds(SAFETYMARGIN)
    }
}
