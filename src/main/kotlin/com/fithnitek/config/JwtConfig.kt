
package com.fithnitek.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtConfig {

    private val secret: String =
        System.getenv("JWT_SECRET")
            ?: error("JWT_SECRET doit être configuré")

    private const val issuer = "fi-thnitek"
    private const val audience = "fi-thnitek-mobile"
    private const val validity = 24 * 60 * 60 * 1000L

    // Générer un token après connexion réussie
    fun generateToken(idUser: Int, role: String): String {
        val now = System.currentTimeMillis()

        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(idUser.toString())
            .withClaim("role", role)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + validity))
            .sign(Algorithm.HMAC256(secret))
    }
}
