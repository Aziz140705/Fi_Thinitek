package com.example.fithnitekmobile.models

data class ProfilDto(
    val idUser: Int,
    val nom: String,
    val prenom: String,
    val email: String,
    val telephone: String? = null,
    val role: String
)

data class ProfilUpdateRequest(
    val nom: String,
    val prenom: String,
    val telephone: String? = null
)

data class ProfilResponse(
    val success: Boolean,
    val message: String,
    val profil: ProfilDto? = null
)
