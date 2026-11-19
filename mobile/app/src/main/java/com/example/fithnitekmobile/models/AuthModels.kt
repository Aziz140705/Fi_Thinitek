package com.example.fithnitekmobile.models

data class UserDto(
    val idUser: Int,
    val nom: String? = null,
    val prenom: String? = null,
    val email: String? = null,
    val telephone: String? = null,
    val role: String = "PASSAGER",
    val token: String? = null
)

data class RegisterRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    val password: String,
    val telephone: String? = null
)

data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val idUser: Int? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val idUser: Int? = null,
    val role: String? = null
)

data class ForgotPasswordRequest(
    val email: String
)

data class ForgotPasswordResponse(
    val success: Boolean,
    val message: String
)

data class VerifyCodeRequest(
    val email: String,
    val code: String
)

data class VerifyCodeResponse(
    val success: Boolean,
    val message: String,
    val resetTicket: String? = null
)

data class ResetPasswordRequest(
    val resetTicket: String,
    val newPassword: String
)

data class ResetPasswordResponse(
    val success: Boolean,
    val message: String
)

data class MessageResponse(
    val success: Boolean,
    val message: String
)
