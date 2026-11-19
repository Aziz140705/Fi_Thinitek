package com.example.fithnitekmobile.models

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
) {
    fun hasCoordinates(): Boolean = departLat != null && departLng != null && destLat != null && destLng != null
}

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

data class TrajetResponse(
    val success: Boolean,
    val message: String,
    val trajet: TrajetDto? = null
)

data class TrajetListResponse(
    val success: Boolean,
    val message: String,
    val trajets: List<TrajetDto> = emptyList()
)
