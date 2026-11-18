
package com.fithnitek.config

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtConfig {

    private val secret: String =
        System.getenv("JWT_SECRET")
            ?.takeIf { it.isNotBlank() }
            ?: "fi_thnitek_jwt_default_secret_key_2026"

    const val issuer = "fi-thnitek"
    const val audience = "fi-thnitek-mobile"
    private const val validity = 24 * 60 * 60 * 1000L

    // Vérificateur utilisé par Ktor pour protéger les routes (Authentication "auth-jwt").
    // Il refuse aussi le ticket de reset password (autre issuer / audience).
    val verifier: JWTVerifier = JWT
        .require(Algorithm.HMAC256(secret))
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

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
