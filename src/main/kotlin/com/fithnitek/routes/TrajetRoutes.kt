package com.fithnitek.routes

import com.fithnitek.models.MessageResponse
import com.fithnitek.models.TrajetListResponse
import com.fithnitek.models.TrajetRequest
import com.fithnitek.models.TrajetResponse
import com.fithnitek.repository.OpResult
import com.fithnitek.repository.TrajetRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Route.trajetRoutes() {
    authenticate("auth-jwt") {
        route("/api/trajets") {

            // Publier un trajet
            post {
                val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, MessageResponse(false, "Non autorisé"))
                    return@post
                }

                val req = try {
                    call.receive<TrajetRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, TrajetResponse(false, "Données invalides"))
                    return@post
                }

                val trajet = withContext(Dispatchers.IO) {
                    TrajetRepository.create(userId, req)
                }

                if (trajet != null) {
                    call.respond(HttpStatusCode.Created, TrajetResponse(true, "Trajet créé avec succès", trajet))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, TrajetResponse(false, "Erreur lors de la création du trajet"))
                }
            }

            // Vos trajets créés (conducteur)
            get("/mes") {
                val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, TrajetListResponse(false, "Non autorisé"))
                    return@get
                }

                val trajets = withContext(Dispatchers.IO) { TrajetRepository.findByConducteur(userId) }
                call.respond(HttpStatusCode.OK, TrajetListResponse(true, "Liste de vos trajets", trajets))
            }

            // Trajets disponibles pour un passager (sans les siens)
            get("/search") {
                val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, TrajetListResponse(false, "Non autorisé"))
                    return@get
                }

                val trajets = withContext(Dispatchers.IO) { TrajetRepository.searchAll(userId) }
                call.respond(HttpStatusCode.OK, TrajetListResponse(true, "Résultats de recherche", trajets))
            }

            // Annuler la publication
            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
                val trajetId = call.parameters["id"]?.toIntOrNull()

                if (userId == null || trajetId == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse(false, "Paramètres invalides"))
                    return@delete
                }

                when (val result = withContext(Dispatchers.IO) { TrajetRepository.annuler(trajetId, userId) }) {
                    is OpResult.Fail -> call.respond(
                        HttpStatusCode.fromValue(result.httpStatus),
                        MessageResponse(false, result.message)
                    )
                    is OpResult.Ok<*> -> call.respond(
                        HttpStatusCode.OK,
                        MessageResponse(true, "Publication annulée. Les demandes en cours sont annulées.")
                    )
                }
            }
        }
    }
}
