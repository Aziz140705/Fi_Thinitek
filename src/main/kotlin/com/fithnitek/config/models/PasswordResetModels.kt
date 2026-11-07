
package com.fithnitek.models

import kotlinx.serialization.Serializable

// Envoyer le code de vérification
@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class ForgotPasswordResponse(
    val success: Boolean,
    val message: String
)

// Vérifier le code OTP avant de changer le mot de passe
@Serializable
data class VerifyCodeRequest(
    val email: String,
    val code: String
)

@Serializable
data class VerifyCodeResponse(
    val success: Boolean,
    val message: String,
    val resetTicket: String? = null
)

// Modifier le mot de passe après vérification du code
@Serializable
data class ResetPasswordRequest(
    val resetTicket: String,
    val newPassword: String
)

@Serializable
data class ResetPasswordResponse(
    val success: Boolean,
    val message: String
)
