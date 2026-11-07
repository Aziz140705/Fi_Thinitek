
package com.fithnitek.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fithnitek.config.EmailService
import com.fithnitek.models.ForgotPasswordRequest
import com.fithnitek.models.ForgotPasswordResponse
import com.fithnitek.models.VerifyCodeRequest
import com.fithnitek.models.VerifyCodeResponse
import com.fithnitek.models.ResetPasswordRequest
import com.fithnitek.models.ResetPasswordResponse
import com.fithnitek.repository.PasswordResetRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

// Autorisation temporaire après vérification du code OTP
private object ResetTicketService {

    private const val ISSUER = "fi-thnitek-reset"
    private const val AUDIENCE = "fi-thnitek-reset-mobile"

    // Même clé secrète que le backend JWT
    private fun algorithm(): Algorithm {
        val secret = System.getenv("JWT_SECRET")
            ?.takeIf { it.isNotBlank() }
            ?: "fi_thnitek_jwt_default_secret_key_2026"

        return Algorithm.HMAC256(secret)
    }

    // Ticket temporaire valable 5 minutes
    fun create(userId: Int, resetId: Int): String {
        return JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withClaim("purpose", "password-reset")
            .withClaim("userId", userId)
            .withClaim("resetId", resetId)
            .withIssuedAt(Date())
            .withExpiresAt(
                Date(System.currentTimeMillis() + 5 * 60 * 1000)
            )
            .sign(algorithm())
    }

    // Vérifier la signature et l'expiration du ticket
    fun verify(ticket: String): Pair<Int, Int>? {
        return try {
            val jwt = JWT.require(algorithm())
                .withIssuer(ISSUER)
                .withAudience(AUDIENCE)
                .withClaim("purpose", "password-reset")
                .build()
                .verify(ticket)

            val userId = jwt.getClaim("userId").asInt()
            val resetId = jwt.getClaim("resetId").asInt()

            if (userId == null || resetId == null) {
                null
            } else {
                userId to resetId
            }
        } catch (e: Exception) {
            null
        }
    }
}

fun Route.passwordResetRoutes() {

    route("/api/auth") {

        // 1. Envoyer un code OTP par Gmail
        post("/forgot-password") {

            val request = try {
                call.receive<ForgotPasswordRequest>()
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ForgotPasswordResponse(false, "Requête invalide")
                )
                return@post
            }

            val email = request.email.trim().lowercase()

            if (email.isBlank() || !email.contains("@")) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ForgotPasswordResponse(false, "Email invalide")
                )
                return@post
            }

            // Ne pas révéler si l'email existe dans MySQL
            withContext(Dispatchers.IO) {
                try {
                    val code = PasswordResetRepository.createResetCode(email)

                    if (code != null) {
                        println("✅ Code OTP généré pour $email : $code")
                        EmailService.sendResetCode(email, code)
                        println("✅ E-mail envoyé avec succès à $email")
                    } else {
                        println("⚠️ L'adresse e-mail '$email' N'EXISTE PAS dans la table 'user' de MySQL !")
                    }
                } catch (e: Exception) {
                    println("❌ ERREUR LORS DE L'ENVOI DU CODE OTP : ${e.message}")
                    e.printStackTrace()
                    call.application.log.error(
                        "Erreur lors de l'envoi du code OTP",
                        e
                    )
                }
            }

            call.respond(
                HttpStatusCode.OK,
                ForgotPasswordResponse(
                    success = true,
                    message = "Si cet email existe, un code sera envoyé."
                )
            )
        }

        // 2. Vérifier le code reçu par Gmail
        post("/verify-code") {

            val request = try {
                call.receive<VerifyCodeRequest>()
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    VerifyCodeResponse(false, "Requête invalide")
                )
                return@post
            }

            val email = request.email.trim().lowercase()
            val code = request.code.trim()

            if (email.isBlank() ||
                !email.contains("@") ||
                !code.matches(Regex("^[0-9]{6}$"))
            ) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    VerifyCodeResponse(false, "Email ou code invalide")
                )
                return@post
            }

            val verified = try {
                withContext(Dispatchers.IO) {
                    PasswordResetRepository.verifyCode(email, code)
                }
            } catch (e: Exception) {
                call.application.log.error(
                    "Erreur vérification OTP",
                    e
                )

                call.respond(
                    HttpStatusCode.InternalServerError,
                    VerifyCodeResponse(false, "Erreur serveur")
                )
                return@post
            }

            if (verified == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    VerifyCodeResponse(
                        success = false,
                        message = "Code incorrect, expiré ou bloqué"
                    )
                )
                return@post
            }

            // Créer une autorisation après vérification du code
            val resetTicket = ResetTicketService.create(
                verified.userId,
                verified.resetId
            )

            call.respond(
                HttpStatusCode.OK,
                VerifyCodeResponse(
                    success = true,
                    message = "Code vérifié avec succès",
                    resetTicket = resetTicket
                )
            )
        }

        // 3. Modifier le mot de passe après vérification
        post("/reset-password") {

            val request = try {
                call.receive<ResetPasswordRequest>()
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ResetPasswordResponse(false, "Requête invalide")
                )
                return@post
            }

            val password = request.newPassword

            // Vérifier le ticket et le nouveau mot de passe
            if (request.resetTicket.isBlank() ||
                password.length < 8 ||
                password.toByteArray(Charsets.UTF_8).size > 72
            ) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ResetPasswordResponse(false, "Données invalides")
                )
                return@post
            }

            val authorization = ResetTicketService.verify(
                request.resetTicket
            )

            if (authorization == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ResetPasswordResponse(
                        false,
                        "Autorisation invalide ou expirée"
                    )
                )
                return@post
            }

            // Modifier le mot de passe dans MySQL
            val success = try {
                withContext(Dispatchers.IO) {
                    PasswordResetRepository.resetPassword(
                        userId = authorization.first,
                        resetId = authorization.second,
                        newPassword = password
                    )
                }
            } catch (e: Exception) {
                call.application.log.error(
                    "Erreur changement mot de passe",
                    e
                )

                call.respond(
                    HttpStatusCode.InternalServerError,
                    ResetPasswordResponse(false, "Erreur serveur")
                )
                return@post
            }

            if (success) {
                call.respond(
                    HttpStatusCode.OK,
                    ResetPasswordResponse(
                        true,
                        "Mot de passe modifié avec succès"
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ResetPasswordResponse(
                        false,
                        "Code expiré ou déjà utilisé"
                    )
                )
            }
        }
    }
}
