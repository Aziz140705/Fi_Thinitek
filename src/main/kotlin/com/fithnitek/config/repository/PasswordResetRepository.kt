
package com.fithnitek.repository

import com.fithnitek.config.DatabaseFactory
import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object PasswordResetRepository {

    private val random = SecureRandom()

    // Données retournées si le code OTP est correct
    data class VerifiedCode(
        val userId: Int,
        val resetId: Int
    )

    private data class ResetRecord(
        val id: Int,
        val tokenHash: String,
        val expiresAt: Timestamp,
        val usedAt: Timestamp?,
        val failedAttempts: Int
    )

    // Générer un code aléatoire de 6 chiffres
    private fun generateCode(): String =
        random.nextInt(1_000_000).toString().padStart(6, '0')

    // Protéger le code avec une clé secrète
    private fun hashCode(email: String, code: String): String {

        val secret = System.getenv("JWT_SECRET")
            ?.takeIf { it.isNotBlank() }
            ?: error("JWT_SECRET non configuré")

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(
            SecretKeySpec(
                secret.toByteArray(Charsets.UTF_8),
                "HmacSHA256"
            )
        )

        return mac.doFinal(
            "password-reset:$email:$code"
                .toByteArray(Charsets.UTF_8)
        ).joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }

    // Lire le dernier code OTP et verrouiller sa ligne
    private fun getLatestReset(
        conn: java.sql.Connection,
        userId: Int
    ): ResetRecord? {

        val sql = """
            SELECT id, token_hash, expires_at,
                   used_at, failed_attempts
            FROM password_reset_tokens
            WHERE idUser = ?
            ORDER BY id DESC
            LIMIT 1
            FOR UPDATE
        """.trimIndent()

        return conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, userId)

            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    ResetRecord(
                        id = rs.getInt("id"),
                        tokenHash = rs.getString("token_hash"),
                        expiresAt = rs.getTimestamp("expires_at"),
                        usedAt = rs.getTimestamp("used_at"),
                        failedAttempts = rs.getInt("failed_attempts")
                    )
                } else null
            }
        }
    }

    private fun isActive(reset: ResetRecord?): Boolean {
        return reset != null &&
                reset.usedAt == null &&
                reset.failedAttempts < 5 &&
                reset.expiresAt.after(
                    Timestamp.valueOf(LocalDateTime.now())
                )
    }

    // 1. Créer un code, invalider les anciens et enregistrer son hash
    fun createResetCode(email: String): String? {

        val normalizedEmail = email.trim().lowercase()

        DatabaseFactory.getConnection().use { conn ->
            conn.autoCommit = false

            try {
                val userId = conn.prepareStatement(
                    "SELECT idUser FROM `user` WHERE email = ?"
                ).use { stmt ->
                    stmt.setString(1, normalizedEmail)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) rs.getInt("idUser")
                        else null
                    }
                }

                if (userId == null) {
                    conn.rollback()
                    return null
                }

                // Les anciens codes ne sont plus utilisables
                conn.prepareStatement(
                    """
                    UPDATE password_reset_tokens
                    SET used_at = NOW()
                    WHERE idUser = ? AND used_at IS NULL
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setInt(1, userId)
                    stmt.executeUpdate()
                }

                val code = generateCode()
                val codeHash = hashCode(normalizedEmail, code)

                conn.prepareStatement(
                    """
                    INSERT INTO password_reset_tokens
                        (idUser, token_hash, expires_at, failed_attempts)
                    VALUES (?, ?, ?, 0)
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setInt(1, userId)
                    stmt.setString(2, codeHash)
                    stmt.setTimestamp(
                        3,
                        Timestamp.valueOf(
                            LocalDateTime.now().plusMinutes(15)
                        )
                    )
                    stmt.executeUpdate()
                }

                conn.commit()
                return code

            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    // 2. Vérifier l'email et le code OTP
    fun verifyCode(email: String, code: String): VerifiedCode? {

        if (!code.matches(Regex("^[0-9]{6}$"))) return null

        val normalizedEmail = email.trim().lowercase()

        DatabaseFactory.getConnection().use { conn ->
            conn.autoCommit = false

            try {
                val userId = conn.prepareStatement(
                    "SELECT idUser FROM `user` WHERE email = ?"
                ).use { stmt ->
                    stmt.setString(1, normalizedEmail)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) rs.getInt("idUser")
                        else null
                    }
                }

                if (userId == null) {
                    conn.rollback()
                    return null
                }

                val reset = getLatestReset(conn, userId)

                if (!isActive(reset)) {
                    conn.rollback()
                    return null
                }

                // reset est non null après isActive, mais on le vérifie
                val activeReset = reset ?: return null

                val expectedHash = hashCode(normalizedEmail, code)

                val valid = MessageDigest.isEqual(
                    expectedHash.toByteArray(Charsets.US_ASCII),
                    activeReset.tokenHash.toByteArray(Charsets.US_ASCII)
                )

                if (!valid) {
                    // 5 tentatives incorrectes maximum
                    conn.prepareStatement(
                        """
                        UPDATE password_reset_tokens
                        SET failed_attempts = failed_attempts + 1
                        WHERE id = ?
                        """.trimIndent()
                    ).use { stmt ->
                        stmt.setInt(1, activeReset.id)
                        stmt.executeUpdate()
                    }

                    conn.commit()
                    return null
                }

                // Le code est correct.
                // Ne pas le consommer avant le changement du mot de passe.
                conn.commit()

                return VerifiedCode(
                    userId = userId,
                    resetId = activeReset.id
                )

            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    // 3. Changer le mot de passe avec une autorisation vérifiée
    fun resetPassword(
        userId: Int,
        resetId: Int,
        newPassword: String
    ): Boolean {

        if (newPassword.length < 8 ||
            newPassword.toByteArray(Charsets.UTF_8).size > 72
        ) return false

        DatabaseFactory.getConnection().use { conn ->
            conn.autoCommit = false

            try {
                // Bloquer le token pendant la transaction
                val reset = conn.prepareStatement(
                    """
                    SELECT id, token_hash, expires_at,
                           used_at, failed_attempts
                    FROM password_reset_tokens
                    WHERE id = ? AND idUser = ?
                    FOR UPDATE
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setInt(1, resetId)
                    stmt.setInt(2, userId)

                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            ResetRecord(
                                id = rs.getInt("id"),
                                tokenHash = rs.getString("token_hash"),
                                expiresAt = rs.getTimestamp("expires_at"),
                                usedAt = rs.getTimestamp("used_at"),
                                failedAttempts = rs.getInt("failed_attempts")
                            )
                        } else null
                    }
                }

                if (!isActive(reset)) {
                    conn.rollback()
                    return false
                }

                // BCrypt pour sécuriser le mot de passe
                val passwordHash = BCrypt.hashpw(
                    newPassword,
                    BCrypt.gensalt(12)
                )

                conn.prepareStatement(
                    """
                    UPDATE `user`
                    SET password_hash = ?
                    WHERE idUser = ?
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setString(1, passwordHash)
                    stmt.setInt(2, userId)
                    stmt.executeUpdate()
                }

                // Invalider toutes les demandes de réinitialisation
                conn.prepareStatement(
                    """
                    UPDATE password_reset_tokens
                    SET used_at = NOW()
                    WHERE idUser = ? AND used_at IS NULL
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setInt(1, userId)
                    stmt.executeUpdate()
                }

                conn.commit()
                return true

            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }
}
