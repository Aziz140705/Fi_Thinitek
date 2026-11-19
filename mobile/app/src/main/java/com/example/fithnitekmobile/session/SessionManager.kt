package com.example.fithnitekmobile.session

import android.content.Context
import android.content.SharedPreferences
import com.example.fithnitekmobile.models.UserDto

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("fithnitek_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_NOM = "user_nom"
        private const val KEY_USER_PRENOM = "user_prenom"
        private const val KEY_USER_EMAIL = "user_email"
    }

    fun saveSession(token: String, idUser: Int, role: String, nom: String? = null, prenom: String? = null, email: String? = null) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, idUser)
            .putString(KEY_USER_ROLE, role)
            .putString(KEY_USER_NOM, nom)
            .putString(KEY_USER_PRENOM, prenom)
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUser(): UserDto? {
        val token = getToken() ?: return null
        val id = prefs.getInt(KEY_USER_ID, -1)
        if (id == -1) return null
        val role = prefs.getString(KEY_USER_ROLE, "PASSAGER") ?: "PASSAGER"
        val nom = prefs.getString(KEY_USER_NOM, null)
        val prenom = prefs.getString(KEY_USER_PRENOM, null)
        val email = prefs.getString(KEY_USER_EMAIL, null)
        return UserDto(
            idUser = id,
            nom = nom,
            prenom = prenom,
            email = email,
            role = role,
            token = token
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
