package com.fithnitek.config

object PasswordPolicy {
    fun validate(password: String): String? {
        if (password.length < 8) return "Le mot de passe doit contenir au moins 8 caractères"
        if (!password.any { it.isUpperCase() }) return "Le mot de passe doit contenir au moins une lettre majuscule"
        if (!password.any { it.isLowerCase() }) return "Le mot de passe doit contenir au moins une lettre minuscule"
        if (!password.any { it.isDigit() }) return "Le mot de passe doit contenir au moins un chiffre"
        if (!password.any { !it.isLetterOrDigit() }) return "Le mot de passe doit contenir au moins un caractère spécial"
        return null
    }
}
