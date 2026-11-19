package com.fithnitek.routes

import com.fithnitek.models.ReservationListResponse
import com.fithnitek.models.ReservationRequest
import com.fithnitek.models.ReservationResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.reservationRoutes() {
    authenticate("auth-jwt") {
        route("/api/reservations") {
            post {
                val req = call.receive<ReservationRequest>()
                call.respond(HttpStatusCode.Created, ReservationResponse(true, "Réservation effectuée"))
            }

            get("/mes") {
                call.respond(HttpStatusCode.OK, ReservationListResponse(true, "Vos réservations", emptyList()))
            }

            get("/recues") {
                call.respond(HttpStatusCode.OK, ReservationListResponse(true, "Demandes reçues", emptyList()))
            }

            put("/{id}/accepter") {
                call.respond(HttpStatusCode.OK, ReservationResponse(true, "Réservation acceptée"))
            }

            put("/{id}/refuser") {
                call.respond(HttpStatusCode.OK, ReservationResponse(true, "Réservation refusée"))
            }

            put("/{id}/annuler") {
                call.respond(HttpStatusCode.OK, ReservationResponse(true, "Réservation annulée"))
            }
        }
    }
}
