package com.fithnitek.routes

import com.fithnitek.models.MessageResponse
import com.fithnitek.models.ReservationDto
import com.fithnitek.models.ReservationListResponse
import com.fithnitek.models.ReservationRequest
import com.fithnitek.models.ReservationResponse
import com.fithnitek.repository.OpResult
import com.fithnitek.repository.ReservationRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun ApplicationCall.userIdOrZero(): Int =
    principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull() ?: 0

private suspend fun ApplicationCall.replyReservation(
    result: OpResult<ReservationDto>,
    okMessage: String,
    okStatus: HttpStatusCode = HttpStatusCode.OK
) {
    when (result) {
        is OpResult.Fail -> respond(
            HttpStatusCode.fromValue(result.httpStatus),
            ReservationResponse(false, result.message)
        )
        is OpResult.Ok<ReservationDto> -> respond(okStatus, ReservationResponse(true, okMessage, result.value))
    }
}

fun Route.reservationRoutes() {
    authenticate("auth-jwt") {
        route("/api/reservations") {

            // Le passager réserve
            post {
                val userId = call.userIdOrZero()
                if (userId == 0) {
                    call.respond(HttpStatusCode.Unauthorized, MessageResponse(false, "Non autorisé"))
                    return@post
                }
                val req = try {
                    call.receive<ReservationRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ReservationResponse(false, "Données invalides"))
                    return@post
                }

                val result = withContext(Dispatchers.IO) { ReservationRepository.creer(userId, req) }
                call.replyReservation(result, "Réservation effectuée", HttpStatusCode.Created)
            }

            get("/mes") {
                val userId = call.userIdOrZero()
                if (userId == 0) {
                    call.respond(HttpStatusCode.Unauthorized, ReservationListResponse(false, "Non autorisé"))
                    return@get
                }
                val list = withContext(Dispatchers.IO) { ReservationRepository.findMesReservations(userId) }
                call.respond(HttpStatusCode.OK, ReservationListResponse(true, "Vos réservations", list))
            }

            get("/recues") {
                val userId = call.userIdOrZero()
                if (userId == 0) {
                    call.respond(HttpStatusCode.Unauthorized, ReservationListResponse(false, "Non autorisé"))
                    return@get
                }
                val list = withContext(Dispatchers.IO) { ReservationRepository.findReservationsRecues(userId) }
                call.respond(HttpStatusCode.OK, ReservationListResponse(true, "Demandes reçues", list))
            }

            put("/{id}/accepter") {
                val userId = call.userIdOrZero()
                val id = call.parameters["id"]?.toIntOrNull()
                if (userId == 0 || id == null) {
                    call.respond(HttpStatusCode.BadRequest, ReservationResponse(false, "Paramètres invalides"))
                    return@put
                }
                val result = withContext(Dispatchers.IO) { ReservationRepository.accepter(id, userId) }
                call.replyReservation(result, "Réservation acceptée")
            }

            put("/{id}/refuser") {
                val userId = call.userIdOrZero()
                val id = call.parameters["id"]?.toIntOrNull()
                if (userId == 0 || id == null) {
                    call.respond(HttpStatusCode.BadRequest, ReservationResponse(false, "Paramètres invalides"))
                    return@put
                }
                val result = withContext(Dispatchers.IO) { ReservationRepository.refuser(id, userId) }
                call.replyReservation(result, "Réservation refusée")
            }

            put("/{id}/annuler") {
                val userId = call.userIdOrZero()
                val id = call.parameters["id"]?.toIntOrNull()
                if (userId == 0 || id == null) {
                    call.respond(HttpStatusCode.BadRequest, ReservationResponse(false, "Paramètres invalides"))
                    return@put
                }
                val result = withContext(Dispatchers.IO) { ReservationRepository.annuler(id, userId) }
                call.replyReservation(result, "Réservation annulée")
            }
        }
    }
}
