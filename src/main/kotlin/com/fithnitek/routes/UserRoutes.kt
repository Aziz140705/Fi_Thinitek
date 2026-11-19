package com.fithnitek.routes

import com.fithnitek.config.JwtConfig
import com.fithnitek.models.LoginResponse
import com.fithnitek.models.MessageResponse
import com.fithnitek.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Route.userRoutes() {
    authenticate("auth-jwt") {
        route("/api/users") {
            post("/devenir-conducteur") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject?.toIntOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, MessageResponse(false, "Non autorisé"))
                    return@post
                }

                val success = withContext(Dispatchers.IO) {
                    UserRepository.updateRole(userId, "CONDUCTEUR")
                }

                if (success) {
                    val newToken = JwtConfig.generateToken(userId, "CONDUCTEUR")
                    call.respond(
                        HttpStatusCode.OK,
                        LoginResponse(true, "Rôle mis à jour avec succès !", token = newToken, idUser = userId, role = "CONDUCTEUR")
                    )
                } else {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse(false, "Impossible de devenir conducteur"))
                }
            }
        }
    }
}
