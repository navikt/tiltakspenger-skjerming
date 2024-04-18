package no.nav.tiltakspenger.skjerming.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import io.ktor.server.auth.principal
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

internal fun ApplicationCall.getClaim(issuer: String, name: String): String? =
    this.authentication.principal<TokenValidationContextPrincipal>()
        ?.context
        ?.getClaims(issuer)
        ?.getStringClaim(name)

fun ApplicationCall.getFnrForTokenx(): String? = this.getClaim("tokendings", "pid")

fun ApplicationCall.getFnrForAzureToken(): String? = this.getClaim("azure", "ident")

fun ApplicationCall.token(): String = this.principal<TokenValidationContextPrincipal>().asTokenString()

internal fun TokenValidationContextPrincipal?.asTokenString(): String =
    this?.context?.firstValidToken?.encodedToken
        ?: throw RuntimeException("no token found in call context")
