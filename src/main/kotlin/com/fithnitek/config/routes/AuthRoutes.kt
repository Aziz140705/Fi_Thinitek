package com.fithnitek.routes

import com.fithnitek.config.JwtConfig
import com.fithnitek.config.PasswordPolicy
import com.fithnitek.models.RegisterRequest
import com.fithnitek.models.RegisterResponse
import com.fithnitek.models.LoginRequest
import com.fithnitek.models.LoginResponse
import com.fithnitek.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Route.authRoutes() {

    route("/api/auth") {

        // US01 : Inscription
        post("/register") {

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

            if (nom.isBlank() || prenom.isBlank() ||
                email.isBlank() || request.password.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    RegisterResponse(false, "Tous les champs obligatoires doivent être remplis")
                )
                return@post
            }

            val emailValide = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

            if (!emailValide.matches(email)) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    RegisterResponse(false, "Email invalide")
                )
                return@post
            }

            // Mot de passe fort : 8 caractères, majuscule, minuscule, chiffre, caractère spécial
            val passwordError = PasswordPolicy.validate(request.password)
            if (passwordError != null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    RegisterResponse(false, passwordError)
                )
                return@post
            }

            val id = try {
                withContext(Dispatchers.IO) {
                    UserRepository.register(
                        nom = nom,
                        prenom = prenom,
                        email = email,
                        password = request.password,
                        telephone = request.telephone?.trim()?.takeIf { it.isNotEmpty() }
                    )
                }
            } catch (e: Exception) {
                call.application.environment.log.error("Erreur lors de l'inscription", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    RegisterResponse(false, "Erreur serveur ou de connexion à la base de données")
                )
                return@post
            }

            if (id != null) {
                call.respond(
                    HttpStatusCode.Created,
                    RegisterResponse(true, "Inscription réussie", id)
                )
            } else {
                call.respond(
                    HttpStatusCode.Conflict,
                    RegisterResponse(false, "Inscription impossible : email déjà utilisé")
                )
            }
        }

        // US02 : Connexion avec JWT
        post("/login") {

            val request = try {
                call.receive<LoginRequest>()
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    LoginResponse(false, "Données JSON invalides")
                )
                return@post
            }

            if (request.email.isBlank() || request.password.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    LoginResponse(false, "Email et mot de passe obligatoires")
                )
                return@post
            }

            // Vérifier email et mot de passe BCrypt
            val user = try {
                withContext(Dispatchers.IO) {
                    UserRepository.login(request.email, request.password)
                }
            } catch (e: Exception) {
                call.application.environment.log.error("Erreur lors de la connexion (login)", e)
                val errorMessage = if (e.message?.contains("base de données", ignoreCase = true) == true || e.message?.contains("MySQL", ignoreCase = true) == true) {
                    "Erreur de connexion MySQL : vérifiez que le service MySQL est démarré sur localhost:3306"
                } else {
                    "Erreur serveur : ${e.localizedMessage}"
                }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    LoginResponse(false, errorMessage)
                )
                return@post
            }

            if (user == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    LoginResponse(false, "Email ou mot de passe incorrect")
                )
                return@post
            }

            // Générer le token JWT
            val token = JwtConfig.generateToken(user.first, user.second)

            call.respond(
                HttpStatusCode.OK,
                LoginResponse(
                    success = true,
                    message = "Connexion réussie",
                    token = token,
                    idUser = user.first,
                    role = user.second
                )
            )
        }
    }
}