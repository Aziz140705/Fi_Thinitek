package com.example.fithnitekmobile.repository

import com.example.fithnitekmobile.models.ForgotPasswordRequest
import com.example.fithnitekmobile.models.ForgotPasswordResponse
import com.example.fithnitekmobile.models.LoginRequest
import com.example.fithnitekmobile.models.LoginResponse
import com.example.fithnitekmobile.models.RegisterRequest
import com.example.fithnitekmobile.models.RegisterResponse
import com.example.fithnitekmobile.models.ResetPasswordRequest
import com.example.fithnitekmobile.models.ResetPasswordResponse
import com.example.fithnitekmobile.models.UserDto
import com.example.fithnitekmobile.models.VerifyCodeRequest
import com.example.fithnitekmobile.models.VerifyCodeResponse
import com.example.fithnitekmobile.network.ApiService
import com.example.fithnitekmobile.session.SessionManager
import com.example.fithnitekmobile.util.Resource

class AuthRepository(
    private val api: ApiService,
    private val session: SessionManager
) : BaseRepository() {

    fun getUser(): UserDto? = session.getUser()

    fun clearSession() = session.clearSession()

    fun saveFrom(loginData: LoginResponse, email: String? = null) {
        if (loginData.token != null && loginData.idUser != null) {
            session.saveSession(
                token = loginData.token,
                idUser = loginData.idUser,
                role = loginData.role ?: "PASSAGER",
                email = email
            )
        }
    }

    suspend fun login(req: LoginRequest): Resource<LoginResponse> {
        val result = safeApiCall<LoginResponse>(
            businessError = { if (it.success) null else it.message }
        ) { api.login(req) }

        if (result is Resource.Success) {
            saveFrom(result.data, req.email)
        }
        return result
    }

    suspend fun register(req: RegisterRequest): Resource<RegisterResponse> =
        safeApiCall(businessError = { if (it.success) null else it.message }) { api.register(req) }

    suspend fun forgotPassword(req: ForgotPasswordRequest): Resource<ForgotPasswordResponse> =
        safeApiCall(businessError = { if (it.success) null else it.message }) { api.forgotPassword(req) }

    suspend fun verifyCode(req: VerifyCodeRequest): Resource<VerifyCodeResponse> =
        safeApiCall(businessError = { if (it.success) null else it.message }) { api.verifyCode(req) }

    suspend fun resetPassword(req: ResetPasswordRequest): Resource<ResetPasswordResponse> =
        safeApiCall(businessError = { if (it.success) null else it.message }) { api.resetPassword(req) }
}
