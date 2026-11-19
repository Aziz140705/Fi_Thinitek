package com.fithnitek.models

import kotlinx.serialization.Serializable

@Serializable
data class MessageResponse(
    val success: Boolean,
    val message: String
)
