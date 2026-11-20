package com.fithnitek.models

import kotlinx.serialization.Serializable

// Profil renvoyé au mobile. Il ne contient JAMAIS le mot de passe (ni son hash).
@Serializable
data class ProfilDto(
    val idUser: Int,
    val nom: String,
    val prenom: String,
    val email: String,
    val telephone: String? = null,
    val role: String
)

@Serializable
data class ProfilUpdateRequest(
    val nom: String,
    val prenom: String,
    val telephone: String? = null
)

@Serializable
data class ProfilResponse(
    val success: Boolean,
    val message: String,
    val profil: ProfilDto? = null
)