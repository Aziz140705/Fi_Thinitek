package com.example.fithnitekmobile.util

enum class FormField {
    NOM, PRENOM, EMAIL, PASSWORD, CONFIRM, TELEPHONE, CODE, GENERAL
}

data class FormError(
    val field: FormField,
    val message: String
)
