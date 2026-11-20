package com.fithnitek.routes

import com.fithnitek.models.MessageResponse
import com.fithnitek.models.TrajetListResponse
import com.fithnitek.models.TrajetRequest
import com.fithnitek.models.TrajetResponse
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
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject?.toIntOrNull()

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

            get("/mes") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject?.toIntOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, TrajetListResponse(false, "Non autorisé"))
                    return@get
                }

                val trajets = withContext(Dispatchers.IO) {
                    TrajetRepository.findByConducteur(userId)
                }
                call.respond(HttpStatusCode.OK, TrajetListResponse(true, "Liste de vos trajets", trajets))
            }

            get("/search") {
                val trajets = withContext(Dispatchers.IO) {
                    TrajetRepository.searchAll()
                }
                call.respond(HttpStatusCode.OK, TrajetListResponse(true, "Résultats de recherche", trajets))
            }

            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject?.toIntOrNull()
                val trajetId = call.parameters["id"]?.toIntOrNull()

                if (userId == null || trajetId == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse(false, "Paramètres invalides"))
                    return@delete
                }

                val deleted = withContext(Dispatchers.IO) {
                    TrajetRepository.delete(trajetId, userId)
                }

                if (deleted) {
                    call.respond(HttpStatusCode.OK, MessageResponse(true, "Trajet supprimé"))
                } else {
                    call.respond(HttpStatusCode.NotFound, MessageResponse(false, "Trajet introuvable"))
                }
            }
        }
    }
}
