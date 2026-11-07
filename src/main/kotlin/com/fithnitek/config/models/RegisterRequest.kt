
package com.fithnitek.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    val password: String,
    val telephone: String? = null
)
