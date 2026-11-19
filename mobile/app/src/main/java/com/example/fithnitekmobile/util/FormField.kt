package com.example.fithnitekmobile.util

enum class FormField {
    NOM, PRENOM, EMAIL, PASSWORD, CONFIRM, TELEPHONE, CODE, GENERAL,
    DEPART, DESTINATION, DATE, PRIX, PLACES, PRIX_MAX, PLACES_MIN, RAYON,
    CIN, PERMIS, MARQUE, MODELE, MATRICULE, PHOTOS
}

data class FormError(
    val field: FormField,
    val message: String
)
