package com.fithnitek.routes

import com.fithnitek.models.MessageResponse
import com.fithnitek.models.ProfilResponse
import com.fithnitek.models.ProfilUpdateRequest
import com.fithnitek.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TELEPHONE_REGEX = Regex("^\\+?[0-9 ]{8,15}$")

private fun ApplicationCall.currentUserId(): Int =
    principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull() ?: 0

private suspend inline fun ApplicationCall.guarded(crossinline block: suspend () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        respond(HttpStatusCode.InternalServerError, MessageResponse(false, e.localizedMessage ?: "Erreur serveur"))
    }
}

fun Route.profilRoutes() {

    authenticate("auth-jwt") {
        route("/api/users/me") {

            // Mon profil
            get {
                call.guarded {
                    val userId = call.currentUserId()
                    val profil = withContext(Dispatchers.IO) { UserRepository.findProfil(userId) }
                    if (profil == null) {
                        call.respond(HttpStatusCode.NotFound, MessageResponse(false, "Utilisateur introuvable"))
                    } else {
                        call.respond(HttpStatusCode.OK, ProfilResponse(true, "OK", profil))
                    }
                }
            }

            // Modifier mon profil
            put {
                call.guarded {
                    val request = try {
                        call.receive<ProfilUpdateRequest>()
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, MessageResponse(false, "Données invalides"))
                        return@guarded
                    }

                    val nom = request.nom.trim()
                    val prenom = request.prenom.trim()
                    val telephone = request.telephone?.trim()?.takeIf { it.isNotEmpty() }

                    if (nom.length !in 2..100 || prenom.length !in 2..100) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            MessageResponse(false, "Nom et prénom : 2 caractères minimum")
                        )
                        return@guarded
                    }
                    if (telephone != null && !TELEPHONE_REGEX.matches(telephone)) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            MessageResponse(false, "Téléphone invalide (8 à 15 chiffres)")
                        )
                        return@guarded
                    }

                    val idUser = call.currentUserId()
                    val updated = withContext(Dispatchers.IO) {
                        UserRepository.updateProfil(idUser, nom, prenom, telephone)
                    }
                    if (!updated) {
                        call.respond(HttpStatusCode.NotFound, MessageResponse(false, "Utilisateur introuvable"))
                        return@guarded
                    }

                    val profil = withContext(Dispatchers.IO) { UserRepository.findProfil(idUser) }
                    call.respond(HttpStatusCode.OK, ProfilResponse(true, "Profil mis à jour", profil))
                }
            }
        }
    }
}
