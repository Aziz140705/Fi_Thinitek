package com.fithnitek.models

import kotlinx.serialization.Serializable

@Serializable
data class ReservationDto(
    val idReservation: Int = 0,
    val idTrajet: Int = 0,
    val idPassager: Int = 0,
    val placesReservees: Int = 1,
    val statut: String = "EN_ATTENTE",
    val depart: String? = null,
    val destination: String? = null,
    val dateDepart: String? = null,
    val heureDepart: String? = null,
    val prixTotal: Double? = null,
    val passagerNom: String? = null
)

@Serializable
data class ReservationRequest(
    val idTrajet: Int,
    val placesReservees: Int = 1
)

@Serializable
data class ReservationResponse(
    val success: Boolean,
    val message: String,
    val reservation: ReservationDto? = null
)

@Serializable
data class ReservationListResponse(
    val success: Boolean,
    val message: String,
    val reservations: List<ReservationDto> = emptyList()
)
