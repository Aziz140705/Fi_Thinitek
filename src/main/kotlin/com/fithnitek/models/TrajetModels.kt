package com.fithnitek.models

import kotlinx.serialization.Serializable

@Serializable
data class TrajetDto(
    val idTrajet: Int = 0,
    val idConducteur: Int = 0,
    val depart: String = "",
    val destination: String = "",
    val dateDepart: String = "",
    val heureDepart: String = "",
    val prix: Double = 0.0,
    val placesDisponibles: Int = 0,
    val departLat: Double? = null,
    val departLng: Double? = null,
    val destLat: Double? = null,
    val destLng: Double? = null,
    val description: String? = null
)

@Serializable
data class TrajetRequest(
    val depart: String = "",
    val destination: String = "",
    val dateDepart: String = "",
    val heureDepart: String = "",
    val prix: Double = 0.0,
    val placesDisponibles: Int = 0,
    val departLat: Double? = null,
    val departLng: Double? = null,
    val destLat: Double? = null,
    val destLng: Double? = null,
    val description: String? = null
)

@Serializable
data class TrajetResponse(
    val success: Boolean,
    val message: String,
    val trajet: TrajetDto? = null
)

@Serializable
data class TrajetListResponse(
    val success: Boolean,
    val message: String,
    val trajets: List<TrajetDto> = emptyList()
)
