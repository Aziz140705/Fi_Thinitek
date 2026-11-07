
package com.fithnitek.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val idUser: Int? = null
)
