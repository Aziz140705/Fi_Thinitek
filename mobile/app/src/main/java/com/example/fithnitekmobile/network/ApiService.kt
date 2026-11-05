
package com.example.fithnitekmobile.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// US01 : Inscription
data class RegisterRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    val password: String,
    val telephone: String?
)

data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val idUser: Int? = null
)

// US02 : Connexion
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

// US04 : Demande de code OTP
data class ForgotPasswordRequest(
    val email: String
)

data class ForgotPasswordResponse(
    val success: Boolean,
    val message: String
)

// US04 : Vérification du code
data class VerifyCodeRequest(
    val email: String,
    val code: String
)

data class VerifyCodeResponse(
    val success: Boolean,
    val message: String,
    val resetTicket: String? = null
)

// US04 : Nouveau mot de passe
data class ResetPasswordRequest(
    val resetTicket: String,
    val newPassword: String
)

data class ResetPasswordResponse(
    val success: Boolean,
    val message: String
)

// Routes du backend Ktor
interface ApiService {

    @POST("api/auth/register")
    fun register(
        @Body request: RegisterRequest
    ): Call<RegisterResponse>

    @POST("api/auth/login")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>

    @POST("api/auth/forgot-password")
    fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Call<ForgotPasswordResponse>

    @POST("api/auth/verify-code")
    fun verifyCode(
        @Body request: VerifyCodeRequest
    ): Call<VerifyCodeResponse>

    @POST("api/auth/reset-password")
    fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Call<ResetPasswordResponse>
}
