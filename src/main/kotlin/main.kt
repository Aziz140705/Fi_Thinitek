
package com

import com.fithnitek.config.DatabaseFactory
import com.fithnitek.config.JwtConfig
import com.fithnitek.models.MessageResponse
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init() // Connexion MySQL

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = false
        })
    }

    // Authentification JWT : les routes dans authenticate("auth-jwt") exigent
    // l'en-tête  Authorization: Bearer <token>
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "fi-thnitek"
            verifier(JwtConfig.verifier)
            validate { credential ->
                if (credential.payload.subject?.toIntOrNull() != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    MessageResponse(false, "Session expirée ou token invalide. Reconnectez-vous.")
                )
            }
        }
    }

    configureRouting() // Routes API
}
