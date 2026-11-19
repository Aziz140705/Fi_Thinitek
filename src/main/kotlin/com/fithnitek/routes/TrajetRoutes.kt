package com.fithnitek.routes

import com.fithnitek.models.MessageResponse
import com.fithnitek.models.TrajetListResponse
import com.fithnitek.models.TrajetRequest
import com.fithnitek.models.TrajetResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.trajetRoutes() {
    authenticate("auth-jwt") {
        route("/api/trajets") {
            post {
                val req = call.receive<TrajetRequest>()
                call.respond(HttpStatusCode.Created, TrajetResponse(true, "Trajet créé avec succès"))
            }

            get("/mes") {
                call.respond(HttpStatusCode.OK, TrajetListResponse(true, "Liste de vos trajets", emptyList()))
            }

            get("/search") {
                call.respond(HttpStatusCode.OK, TrajetListResponse(true, "Résultats de recherche", emptyList()))
            }

            get("/{id}") {
                call.respond(HttpStatusCode.OK, TrajetResponse(true, "Détails du trajet"))
            }

            put("/{id}") {
                call.respond(HttpStatusCode.OK, TrajetResponse(true, "Trajet mis à jour"))
            }

            delete("/{id}") {
                call.respond(HttpStatusCode.OK, MessageResponse(true, "Trajet supprimé"))
            }
        }
    }
}
