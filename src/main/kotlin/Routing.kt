
package com

import com.fithnitek.routes.authRoutes
import com.fithnitek.routes.passwordResetRoutes
import com.fithnitek.routes.reservationRoutes
import com.fithnitek.routes.trajetRoutes
import com.fithnitek.routes.userRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {

    routing {
        get("/") {
            call.respondText("Hello, World!")
        }

        authRoutes() // Activer les routes d'authentification
        passwordResetRoutes()

        // Sprint 2 : routes protégées par JWT
        userRoutes()
        trajetRoutes()
        reservationRoutes()
    }
}
