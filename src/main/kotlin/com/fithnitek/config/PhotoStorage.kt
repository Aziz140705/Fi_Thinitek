package com.fithnitek.config

import java.io.File

// Enregistre les photos du dossier conducteur sur le disque du serveur.
// Elles ne sont JAMAIS servies publiquement (CIN et permis = données privées).
object PhotoStorage {

    const val MAX_BYTES = 3L * 1024 * 1024   // 3 Mo par photo

    // Dossier racine (modifiable avec la variable UPLOADS_DIR)
    private val root = File(System.getenv("UPLOADS_DIR")?.takeIf { it.isNotBlank() } ?: "uploads")

    // Vérifie le CONTENU du fichier (pas son nom) : retourne "jpg" ou "png", sinon null
    fun detectExtension(bytes: ByteArray): String? = when {
        bytes.size > 3 &&
                bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "jpg"
        bytes.size > 4 &&
                bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "png"
        else -> null
    }

    // Enregistre uploads/dossiers/<idUser>/<name>.<ext> en remplaçant l'ancienne photo.
    // `name` est toujours une valeur fixe du code (cin, permis, vehicule) : pas de nom venant du client.
    // Retourne le chemin RELATIF à stocker en base.
    fun save(idUser: Int, name: String, bytes: ByteArray): String {
        val ext = detectExtension(bytes) ?: throw IllegalArgumentException("Image invalide")
        val dir = File(root, "dossiers/$idUser").apply { mkdirs() }
        dir.listFiles { f -> f.nameWithoutExtension == name }?.forEach { it.delete() }
        File(dir, "$name.$ext").writeBytes(bytes)
        return "dossiers/$idUser/$name.$ext"
    }
}