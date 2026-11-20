package com

import com.fithnitek.routes.authRoutes
import com.fithnitek.routes.conducteurRoutes
import com.fithnitek.routes.passwordResetRoutes
import com.fithnitek.routes.profilRoutes
import com.fithnitek.routes.reservationRoutes
import com.fithnitek.routes.trajetRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {

    routing {
        get("/") {
            call.respondText("Hello, World!")
        }

        authRoutes()
        passwordResetRoutes()

        profilRoutes()       // US04 : profil
        conducteurRoutes()   // dossier + paiement conducteur

        trajetRoutes()
        reservationRoutes()
    }
}