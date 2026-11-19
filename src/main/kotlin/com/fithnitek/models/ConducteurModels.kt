package com.fithnitek.models

import kotlinx.serialization.Serializable

// Dossier renvoyé au mobile. Il ne contient JAMAIS le CIN, le permis ni les chemins des photos.
@Serializable
data class DossierDto(
    val existe: Boolean,
    val statut: String? = null,        // PENDING = à payer ; VALIDATED = conducteur actif
    val marque: String? = null,
    val model: String? = null,
    val matricule: String? = null,
    val couleur: String? = null,
    val nbPlaces: Int? = null,
    val nbSiegesEnfant: Int? = null,
    val frais: Double                  // montant à payer, décidé par le serveur
)

@Serializable
data class DossierResponse(
    val success: Boolean,
    val message: String,
    val dossier: DossierDto? = null
)