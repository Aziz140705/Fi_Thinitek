package com.example.fithnitekmobile.models

data class DossierDto(
    val existe: Boolean = false,
    val statut: String? = null,
    val marque: String? = null,
    val model: String? = null,
    val matricule: String? = null,
    val couleur: String? = null,
    val nbPlaces: Int? = null,
    val nbSiegesEnfant: Int? = null,
    val frais: Double = 20.0
)

data class DossierResponse(
    val success: Boolean,
    val message: String,
    val dossier: DossierDto? = null
)
