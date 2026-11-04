
package com.fithnitek.routes

import com.fithnitek.models.RegisterRequest
import com.fithnitek.models.RegisterResponse
import com.fithnitek.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Route.authRoutes() {

    route("/api/auth") {

        post("/register") {

            // Recevoir les informations JSON
            val request = try {
                call.receive<RegisterRequest>()
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    RegisterResponse(false, "Données JSON invalides")
                )
                return@post
            }

            val nom = request.nom.trim()
            val prenom = request.prenom.trim()
            val email = request.email.trim().lowercase()

            // Vérifier les champs obligatoires
            if (nom.isBlank() || prenom.isBlank() ||
                email.isBlank() || request.password.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    RegisterResponse(false, "Tous les champs obligatoires doivent être remplis")
                )
                return@post
            }

            // Vérifier l'email
            val emailValide = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
            if (!emailValide.matches(email)) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    RegisterResponse(false, "Email invalide")
                )
                return@post
            }

            // Vérifier le mot de passe
            if (request.password.length < 8 ||
                request.password.toByteArray(Charsets.UTF_8).size > 72) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    RegisterResponse(false, "Mot de passe invalide (8 caractères minimum, 72 octets maximum)")
                )
                return@post
            }

            // Enregistrer dans MySQL
            val id = withContext(Dispatchers.IO) {
                UserRepository.register(
                    nom = nom,
                    prenom = prenom,
                    email = email,
                    password = request.password,
                    telephone = request.telephone?.trim()?.takeIf { it.isNotEmpty() }
                )
            }

            if (id != null) {
                call.respond(
                    HttpStatusCode.Created,
                    RegisterResponse(true, "Inscription réussie", id)
                )
            } else {
                call.respond(
                    HttpStatusCode.Conflict,
                    RegisterResponse(false, "Inscription impossible : email déjà utilisé ou contrainte SQL")
                )
            }
        }
    }
}
