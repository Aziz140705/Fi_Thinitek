
package com

import com.fithnitek.routes.authRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {

    routing {
        get("/") {
            call.respondText("Hello, World!")
        }

        authRoutes() // Activer les routes d'authentification
    }
}
